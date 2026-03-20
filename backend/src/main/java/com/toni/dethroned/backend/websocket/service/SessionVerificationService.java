package com.toni.dethroned.backend.websocket.service;

import com.toni.dethroned.backend.lobby.service.LobbyService;
import org.springframework.stereotype.Service;

/**
 * Service for verifying whether a player has access to a session/group.
 * Used to ensure that only authorized players can connect.
 */
@Service
public class SessionVerificationService {

    private final LobbyService lobbyService;

    public SessionVerificationService(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    /**
     * Verifies whether a player has access to a lobby.
     * The player must be a member of the lobby.
     *
     * @param playerId Player ID
     * @param lobbyId Lobby ID
     * @return true if player is in the lobby, false otherwise
     */
    public boolean verifyLobbyAccess(String playerId, String lobbyId) {
        try {
            var lobby = lobbyService.getLobbyById(lobbyId);
            return lobby.getPlayer(playerId) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generic verification for group access.
     * For this phase, only lobby access is verified.
     *
     * @param playerId Player ID
     * @param groupId Group ID (currently = Lobby ID)
     * @return true if player has access, false otherwise
     */
    public boolean verifyGroupAccess(String playerId, String groupId) {
        // Currently groupId is treated as lobbyId
        // Later this could be handled differently based on type (LOBBY, GAME, etc.)
        return verifyLobbyAccess(playerId, groupId);
    }
}
