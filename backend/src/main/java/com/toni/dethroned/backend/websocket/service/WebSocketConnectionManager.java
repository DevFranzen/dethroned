package com.toni.dethroned.backend.websocket.service;

import com.toni.dethroned.backend.websocket.domain.ConnectionGroup;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for central management of WebSocket ConnectionGroups.
 * Each ConnectionGroup manages all WebSocket connections of a group (e.g. Lobby).
 */
@Service
public class WebSocketConnectionManager {

    private final Map<String, ConnectionGroup> connectionGroups = new ConcurrentHashMap<>();

    /**
     * Returns a ConnectionGroup or creates it if it doesn't exist.
     *
     * @param groupId Group ID (e.g. Lobby ID)
     * @return ConnectionGroup
     */
    public ConnectionGroup getConnectionGroup(String groupId) {
        return connectionGroups.computeIfAbsent(groupId, id -> new ConnectionGroup(id));
    }

    /**
     * Creates a new ConnectionGroup.
     * If one already exists, it is returned.
     *
     * @param groupId Group ID
     * @return ConnectionGroup
     */
    public ConnectionGroup createConnectionGroup(String groupId) {
        return getConnectionGroup(groupId);
    }

    /**
     * Removes a ConnectionGroup.
     *
     * @param groupId Group ID
     */
    public void removeConnectionGroup(String groupId) {
        connectionGroups.remove(groupId);
    }

    /**
     * Checks if a ConnectionGroup exists.
     *
     * @param groupId Group ID
     * @return true if group exists, false otherwise
     */
    public boolean hasConnectionGroup(String groupId) {
        return connectionGroups.containsKey(groupId);
    }

    /**
     * Checks if a player is connected in a group.
     *
     * @param playerId Player ID
     * @param groupId Group ID
     * @return true if player is connected, false otherwise
     */
    public boolean isPlayerConnected(String playerId, String groupId) {
        ConnectionGroup group = connectionGroups.get(groupId);
        if (group == null) {
            return false;
        }
        return group.isPlayerConnected(playerId);
    }
}
