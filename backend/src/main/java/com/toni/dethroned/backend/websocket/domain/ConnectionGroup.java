package com.toni.dethroned.backend.websocket.domain;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zentrale Klasse für die Verwaltung aller WebSocket-Verbindungen einer Gruppe.
 * Eine ConnectionGroup repräsentiert eine logische Gruppe (z.B. eine Lobby, ein Game),
 * die mehrere Players über WebSocket verbunden hat.
 *
 * Diese Klasse bietet generische Broadcasting- und Messaging-Funktionalität,
 * die von verschiedenen Services (Game, Lobby, etc.) genutzt werden kann.
 */
public class ConnectionGroup {
    private final String groupId;
    private final Map<String, PlayerConnection> playerConnections;

    public ConnectionGroup(String groupId) {
        this.groupId = groupId;
        this.playerConnections = new ConcurrentHashMap<>();
    }

    /**
     * Verbindung eines Players hinzufügen oder reconnecten.
     * Falls der Player bereits verbunden war, wird die alte Verbindung ersetzt (Reconnect).
     *
     * @param playerId Player ID
     * @param webSocketSession WebSocket Session
     * @return PlayerConnection Objekt
     */
    public PlayerConnection addConnection(String playerId, WebSocketSession webSocketSession) {
        LocalDateTime now = LocalDateTime.now();
        PlayerConnection connection = new PlayerConnection(playerId, webSocketSession, now);
        playerConnections.put(playerId, connection);
        return connection;
    }

    /**
     * Verbindung eines Players entfernen.
     * Der Player selbst bleibt in der Gruppe (z.B. Lobby), nur die WebSocket-Verbindung wird entfernt.
     *
     * @param playerId Player ID
     */
    public void removeConnection(String playerId) {
        playerConnections.remove(playerId);
    }

    /**
     * Prüft ob ein Player verbunden ist.
     *
     * @param playerId Player ID
     * @return true wenn Player verbunden ist, false sonst
     */
    public boolean isPlayerConnected(String playerId) {
        PlayerConnection connection = playerConnections.get(playerId);
        return connection != null && connection.isAlive();
    }

    /**
     * Gibt die Verbindung eines Players zurück.
     *
     * @param playerId Player ID
     * @return PlayerConnection oder null wenn nicht verbunden
     */
    public PlayerConnection getPlayerConnection(String playerId) {
        return playerConnections.get(playerId);
    }

    /**
     * Gibt alle aktiven Verbindungen zurück.
     *
     * @return Collection aller PlayerConnections
     */
    public Collection<PlayerConnection> getActiveConnections() {
        return playerConnections.values();
    }

    /**
     * Gibt die Anzahl aktiver Verbindungen zurück.
     *
     * @return Anzahl verbundener Players
     */
    public int getConnectionCount() {
        return playerConnections.size();
    }

    /**
     * Sendet eine Message an alle verbundenen Players in dieser Gruppe.
     *
     * @param message Message als JSON String
     */
    public void broadcastAll(String message) {
        broadcastAll(message, null);
    }

    /**
     * Sendet eine Message an alle verbundenen Players außer einem.
     *
     * @param message Message als JSON String
     * @param excludePlayerId Player ID die excludiert wird (null wenn niemand excludiert)
     */
    public void broadcastAll(String message, String excludePlayerId) {
        for (PlayerConnection connection : playerConnections.values()) {
            if (excludePlayerId != null && connection.getPlayerId().equals(excludePlayerId)) {
                continue;
            }

            if (connection.isAlive()) {
                try {
                    connection.getWebSocketSession().sendMessage(new TextMessage(message));
                    connection.updateActivity();
                } catch (IOException e) {
                    // Verbindung ist broken, wird bei nächstem Disconnect cleanup entfernt
                }
            }
        }
    }

    /**
     * Sendet eine Message an einen spezifischen Player.
     *
     * @param playerId Player ID
     * @param message Message als JSON String
     */
    public void sendTo(String playerId, String message) {
        PlayerConnection connection = playerConnections.get(playerId);
        if (connection == null || !connection.isAlive()) {
            return;
        }

        try {
            connection.getWebSocketSession().sendMessage(new TextMessage(message));
            connection.updateActivity();
        } catch (IOException e) {
            // Verbindung ist broken, wird bei nächstem Disconnect cleanup entfernt
        }
    }

    /**
     * Prüft ob die Gruppe leer ist (keine Verbindungen).
     *
     * @return true wenn keine Players verbunden sind
     */
    public boolean isEmpty() {
        return playerConnections.isEmpty();
    }

    /**
     * Gibt die Group ID zurück.
     *
     * @return Group ID
     */
    public String getGroupId() {
        return groupId;
    }
}
