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
 * WebSocket Handler for game connections.
 * Manages the lifecycle of WebSocket connections:
 * - Connection establishment with verification
 * - Message handling
 * - Disconnect cleanup
 * - Reconnect logic
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
     * Called when a client establishes a WebSocket connection.
     * - Extracts playerId and sessionId from URL parameters
     * - Verifies the player
     * - Adds connection to the ConnectionGroup
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String playerId = null;
        String sessionId = null;

        // Parse from URI Path
        // Format: ws://host/ws/player/{playerId}/session/{sessionId}
        String uri = session.getUri().toString();
        String[] parts = uri.split("/");
        if (parts.length >= 5) {
            playerId = parts[parts.length - 3];
            sessionId = parts[parts.length - 1];
        }

        // Validation
        if (playerId == null || sessionId == null) {
            sendConnectionResponse(session, "{\"status\":\"AUTHENTICATION_FAILED\",\"message\":\"Missing playerId or sessionId\"}");
            session.close();
            return;
        }

        // Verification: Check if player is in the lobby
        if (!verificationService.verifyGroupAccess(playerId, sessionId)) {
            sendConnectionResponse(session, "{\"status\":\"UNAUTHORIZED\",\"message\":\"Player is not authorized to access this session\"}");
            session.close();
            return;
        }

        // Get the ConnectionGroup (for lobby = the lobby itself)
        ConnectionGroup connectionGroup;
        try {
            var lobby = lobbyService.getLobbyById(sessionId);
            connectionGroup = lobby.getConnectionGroup();
        } catch (Exception e) {
            sendConnectionResponse(session, "{\"status\":\"SESSION_INVALID\",\"message\":\"Session not found\"}");
            session.close();
            return;
        }

        // Add connection to ConnectionGroup (or reconnect if already present)
        connectionGroup.addConnection(playerId, session);

        // Send success response
        String successResponse = String.format(
            "{\"status\":\"CONNECTED\",\"message\":\"Connected successfully\",\"playerId\":\"%s\",\"sessionId\":\"%s\"}",
            playerId, sessionId
        );
        sendConnectionResponse(session, successResponse);

        // Broadcast player connected event to all other players in the group
        String connectedMessage = String.format(
            "{\"type\":\"PLAYER_CONNECTED\",\"playerId\":\"%s\",\"timestamp\":%d}",
            playerId,
            System.currentTimeMillis()
        );
        connectionGroup.broadcastAll(connectedMessage, playerId);
    }

    /**
     * Called when the client sends a message.
     * Currently not implemented - can be extended later for game commands.
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Messages from client could be processed here for game commands
        // Not necessary for this phase
    }

    /**
     * Called when the WebSocket connection is closed.
     * - Finds the ConnectionGroup
     * - Removes the player from the group
     * - If lobby is empty: Can lead to auto-deletion later
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // Extract playerId and sessionId (same as in afterConnectionEstablished)
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

        // Remove connection from ConnectionGroup
        if (connectionManager.hasConnectionGroup(sessionId)) {
            try {
                var lobby = lobbyService.getLobbyById(sessionId);
                ConnectionGroup connectionGroup = lobby.getConnectionGroup();
                connectionGroup.removeConnection(playerId);

                // Broadcast disconnect to remaining players
                String disconnectMessage = String.format(
                    "{\"type\":\"PLAYER_DISCONNECTED\",\"playerId\":\"%s\",\"timestamp\":%d}",
                    playerId,
                    System.currentTimeMillis()
                );
                connectionGroup.broadcastAll(disconnectMessage, playerId);
            } catch (Exception e) {
                // Lobby no longer exists or other error
            }
        }
    }

    /**
     * Called when a transport error occurs.
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        // Error handling - connection is closed automatically
    }

    /**
     * Helper method to send a ConnectionResponse as JSON string
     */
    private void sendConnectionResponse(WebSocketSession session, String jsonResponse) {
        try {
            session.sendMessage(new TextMessage(jsonResponse));
        } catch (IOException e) {
            // Error sending - connection will be closed
        }
    }
}
