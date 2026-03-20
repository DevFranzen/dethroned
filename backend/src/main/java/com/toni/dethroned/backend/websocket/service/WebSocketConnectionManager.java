package com.toni.dethroned.backend.websocket.service;

import com.toni.dethroned.backend.websocket.domain.ConnectionGroup;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service für zentrale Verwaltung von WebSocket ConnectionGroups.
 * Jede ConnectionGroup verwaltet alle WebSocket-Verbindungen einer Gruppe (z.B. Lobby).
 */
@Service
public class WebSocketConnectionManager {

    private final Map<String, ConnectionGroup> connectionGroups = new ConcurrentHashMap<>();

    /**
     * Gibt eine ConnectionGroup zurück oder erstellt sie wenn sie nicht existiert.
     *
     * @param groupId Gruppen ID (z.B. Lobby ID)
     * @return ConnectionGroup
     */
    public ConnectionGroup getConnectionGroup(String groupId) {
        return connectionGroups.computeIfAbsent(groupId, id -> new ConnectionGroup(id));
    }

    /**
     * Erstellt eine neue ConnectionGroup.
     * Falls bereits eine existiert, wird diese zurückgegeben.
     *
     * @param groupId Gruppen ID
     * @return ConnectionGroup
     */
    public ConnectionGroup createConnectionGroup(String groupId) {
        return getConnectionGroup(groupId);
    }

    /**
     * Entfernt eine ConnectionGroup.
     *
     * @param groupId Gruppen ID
     */
    public void removeConnectionGroup(String groupId) {
        connectionGroups.remove(groupId);
    }

    /**
     * Prüft ob eine ConnectionGroup existiert.
     *
     * @param groupId Gruppen ID
     * @return true wenn Gruppe existiert, false sonst
     */
    public boolean hasConnectionGroup(String groupId) {
        return connectionGroups.containsKey(groupId);
    }

    /**
     * Prüft ob ein Player in einer Gruppe verbunden ist.
     *
     * @param playerId Player ID
     * @param groupId Gruppen ID
     * @return true wenn Player verbunden ist, false sonst
     */
    public boolean isPlayerConnected(String playerId, String groupId) {
        ConnectionGroup group = connectionGroups.get(groupId);
        if (group == null) {
            return false;
        }
        return group.isPlayerConnected(playerId);
    }
}
