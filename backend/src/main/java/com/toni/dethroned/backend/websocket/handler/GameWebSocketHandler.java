package com.toni.dethroned.backend.websocket.handler;

import com.toni.dethroned.backend.lobby.service.LobbyService;
import com.toni.dethroned.backend.websocket.domain.ConnectionGroup;
import com.toni.dethroned.backend.websocket.service.SessionVerificationService;
import com.toni.dethroned.backend.websocket.service.WebSocketConnectionManager;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

/**
 * WebSocket Handler für Game-Verbindungen.
 * Managed den Lifecycle von WebSocket-Verbindungen:
 * - Verbindungsaufbau mit Verifikation
 * - Message-Handling
 * - Disconnect-Cleanup
 * - Reconnect-Logik
 */
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketConnectionManager connectionManager;
    private final SessionVerificationService verificationService;
    private final LobbyService lobbyService;

    public GameWebSocketHandler(
            WebSocketConnectionManager connectionManager,
            SessionVerificationService verificationService,
            LobbyService lobbyService) {
        this.connectionManager = connectionManager;
        this.verificationService = verificationService;
        this.lobbyService = lobbyService;
    }

    /**
     * Wird aufgerufen wenn ein Client eine WebSocket-Verbindung aufbaut.
     * - Extrahiert playerId und sessionId aus URL-Parametern
     * - Verifikation des Players
     * - Verbindung zur ConnectionGroup hinzufügen
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String playerId = null;
        String sessionId = null;

        // Parse aus URI Path
        // Format: ws://host/ws/player/{playerId}/session/{sessionId}
        String uri = session.getUri().toString();
        String[] parts = uri.split("/");
        if (parts.length >= 5) {
            playerId = parts[parts.length - 3];
            sessionId = parts[parts.length - 1];
        }

        // Validierung
        if (playerId == null || sessionId == null) {
            sendConnectionResponse(session, "{\"status\":\"AUTHENTICATION_FAILED\",\"message\":\"Missing playerId or sessionId\"}");
            session.close();
            return;
        }

        // Verifikation: Prüfe ob Player in der Lobby ist
        if (!verificationService.verifyGroupAccess(playerId, sessionId)) {
            sendConnectionResponse(session, "{\"status\":\"UNAUTHORIZED\",\"message\":\"Player is not authorized to access this session\"}");
            session.close();
            return;
        }

        // Hole die ConnectionGroup (für Lobby = die Lobby selbst)
        ConnectionGroup connectionGroup;
        try {
            var lobby = lobbyService.getLobbyById(sessionId);
            connectionGroup = lobby.getConnectionGroup();
        } catch (Exception e) {
            sendConnectionResponse(session, "{\"status\":\"SESSION_INVALID\",\"message\":\"Session not found\"}");
            session.close();
            return;
        }

        // Füge Verbindung zur ConnectionGroup hinzu (oder Reconnect wenn schon vorhanden)
        connectionGroup.addConnection(playerId, session);

        // Sende Erfolgs-Response
        String successResponse = String.format(
            "{\"status\":\"CONNECTED\",\"message\":\"Connected successfully\",\"playerId\":\"%s\",\"sessionId\":\"%s\"}",
            playerId, sessionId
        );
        sendConnectionResponse(session, successResponse);
    }

    /**
     * Wird aufgerufen wenn der Client eine Nachricht sendet.
     * Aktuell nicht implementiert - kann später für Game-Commands erweitert werden.
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Messages vom Client könnten hier für Game-Commands verarbeitet werden
        // Für diese Phase nicht notwendig
    }

    /**
     * Wird aufgerufen wenn die WebSocket-Verbindung geschlossen wird.
     * - Findet die ConnectionGroup
     * - Entfernt den Player aus der Gruppe
     * - Falls Lobby leer: Kann später zur Auto-Löschung führen
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // Extrahiere playerId und sessionId (gleich wie in afterConnectionEstablished)
        String uri = session.getUri().toString();
        String[] parts = uri.split("/");
        String playerId = null;
        String sessionId = null;

        if (parts.length >= 5) {
            playerId = parts[parts.length - 3];
            sessionId = parts[parts.length - 1];
        }

        if (playerId == null || sessionId == null) {
            return;
        }

        // Entferne Connection aus ConnectionGroup
        if (connectionManager.hasConnectionGroup(sessionId)) {
            try {
                var lobby = lobbyService.getLobbyById(sessionId);
                ConnectionGroup connectionGroup = lobby.getConnectionGroup();
                connectionGroup.removeConnection(playerId);

                // Broadcast Disconnect an verbleibende Players
                String disconnectMessage = String.format(
                    "{\"type\":\"PLAYER_DISCONNECTED\",\"playerId\":\"%s\",\"timestamp\":%d}",
                    playerId,
                    System.currentTimeMillis()
                );
                connectionGroup.broadcastAll(disconnectMessage, playerId);
            } catch (Exception e) {
                // Lobby existiert nicht mehr oder andere Fehler
            }
        }
    }

    /**
     * Wird aufgerufen wenn ein Transport-Fehler auftritt.
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        // Fehler-Handling - Verbindung wird automatisch geschlossen
    }

    /**
     * Hilfsmethode zum Senden einer ConnectionResponse als JSON String
     */
    private void sendConnectionResponse(WebSocketSession session, String jsonResponse) {
        try {
            session.sendMessage(new TextMessage(jsonResponse));
        } catch (IOException e) {
            // Fehler beim Senden - Verbindung wird geschlossen
        }
    }
}
