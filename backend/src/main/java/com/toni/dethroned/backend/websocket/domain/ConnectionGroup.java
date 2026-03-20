package com.toni.dethroned.backend.websocket.domain;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central class for managing all WebSocket connections of a group.
 * A ConnectionGroup represents a logical group (e.g. a Lobby, a Game)
 * that has multiple players connected over WebSocket.
 *
 * This class provides generic broadcasting and messaging functionality
 * that can be used by various services (Game, Lobby, etc.).
 */
public class ConnectionGroup {
    private final String groupId;
    private final Map<String, PlayerConnection> playerConnections;

    public ConnectionGroup(String groupId) {
        this.groupId = groupId;
        this.playerConnections = new ConcurrentHashMap<>();
    }

    /**
     * Add a player's connection or reconnect.
     * If the player was already connected, the old connection is replaced (Reconnect).
     *
     * @param playerId Player ID
     * @param webSocketSession WebSocket Session
     * @return PlayerConnection object
     */
    public PlayerConnection addConnection(String playerId, WebSocketSession webSocketSession) {
        LocalDateTime now = LocalDateTime.now();
        PlayerConnection connection = new PlayerConnection(playerId, webSocketSession, now);
        playerConnections.put(playerId, connection);
        return connection;
    }

    /**
     * Remove a player's connection.
     * The player itself remains in the group (e.g. Lobby), only the WebSocket connection is removed.
     *
     * @param playerId Player ID
     */
    public void removeConnection(String playerId) {
        playerConnections.remove(playerId);
    }

    /**
     * Checks if a player is connected.
     *
     * @param playerId Player ID
     * @return true if player is connected, false otherwise
     */
    public boolean isPlayerConnected(String playerId) {
        PlayerConnection connection = playerConnections.get(playerId);
        return connection != null && connection.isAlive();
    }

    /**
     * Returns a player's connection.
     *
     * @param playerId Player ID
     * @return PlayerConnection or null if not connected
     */
    public PlayerConnection getPlayerConnection(String playerId) {
        return playerConnections.get(playerId);
    }

    /**
     * Returns all active connections.
     *
     * @return Collection of all PlayerConnections
     */
    public Collection<PlayerConnection> getActiveConnections() {
        return playerConnections.values();
    }

    /**
     * Returns the number of active connections.
     *
     * @return Number of connected players
     */
    public int getConnectionCount() {
        return playerConnections.size();
    }

    /**
     * Sends a message to all connected players in this group.
     *
     * @param message Message as JSON string
     */
    public void broadcastAll(String message) {
        broadcastAll(message, null);
    }

    /**
     * Sends a message to all connected players except one.
     *
     * @param message Message as JSON string
     * @param excludePlayerId Player ID to be excluded (null if no one is excluded)
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
                    // Connection is broken, will be cleaned up at next disconnect
                }
            }
        }
    }

    /**
     * Sends a message to a specific player.
     *
     * @param playerId Player ID
     * @param message Message as JSON string
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
            // Connection is broken, will be cleaned up at next disconnect
        }
    }

    /**
     * Checks if the group is empty (no connections).
     *
     * @return true if no players are connected
     */
    public boolean isEmpty() {
        return playerConnections.isEmpty();
    }

    /**
     * Returns the group ID.
     *
     * @return Group ID
     */
    public String getGroupId() {
        return groupId;
    }
}
