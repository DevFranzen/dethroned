package com.toni.dethroned.backend.websocket.service;

import com.toni.dethroned.backend.lobby.service.LobbyService;
import org.springframework.stereotype.Service;

/**
 * Service zur Verifikation ob ein Player Zugriff auf eine Session/Gruppe hat.
 * Wird verwendet um sicherzustellen dass nur autorisierte Players sich verbinden können.
 */
@Service
public class SessionVerificationService {

    private final LobbyService lobbyService;

    public SessionVerificationService(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    /**
     * Verifies ob ein Player Zugriff auf eine Lobby hat.
     * Der Player muss ein Member der Lobby sein.
     *
     * @param playerId Player ID
     * @param lobbyId Lobby ID
     * @return true wenn Player in der Lobby ist, false sonst
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
     * Generische Verifikation für Gruppen-Zugriff.
     * Für diese Phase wird nur Lobby-Zugriff verifikiert.
     *
     * @param playerId Player ID
     * @param groupId Gruppen ID (aktuell = Lobby ID)
     * @return true wenn Player Zugriff hat, false sonst
     */
    public boolean verifyGroupAccess(String playerId, String groupId) {
        // Aktuell wird groupId als lobbyId behandelt
        // Später könnte dies basierend auf Typ (LOBBY, GAME, etc.) unterschiedlich handled werden
        return verifyLobbyAccess(playerId, groupId);
    }
}
