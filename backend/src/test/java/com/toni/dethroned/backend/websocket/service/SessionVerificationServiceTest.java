package com.toni.dethroned.backend.websocket.service;

import com.toni.dethroned.backend.lobby.domain.GameSettings;
import com.toni.dethroned.backend.lobby.domain.Lobby;
import com.toni.dethroned.backend.lobby.domain.PlayerRole;
import com.toni.dethroned.backend.lobby.service.LobbyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessionVerificationServiceTest {
    private SessionVerificationService verificationService;
    private LobbyService lobbyService;
    private Lobby testLobby;

    @BeforeEach
    void setUp() {
        lobbyService = new LobbyService();
        verificationService = new SessionVerificationService(lobbyService);

        // Create test lobby with players
        GameSettings settings = new GameSettings(2, 4, "elimination", 0, 800, 600);
        testLobby = lobbyService.createLobby("player1", settings);
        lobbyService.addPlayer(testLobby.getId(), "player2", "Player2", PlayerRole.PLAYER);
    }

    @Test
    void testVerifyLobbyAccessPlayerInLobby() {
        boolean result = verificationService.verifyLobbyAccess("player1", testLobby.getId());

        assertTrue(result);
    }

    @Test
    void testVerifyLobbyAccessPlayerNotInLobby() {
        boolean result = verificationService.verifyLobbyAccess("player-unknown", testLobby.getId());

        assertFalse(result);
    }

    @Test
    void testVerifyLobbyAccessInvalidLobby() {
        boolean result = verificationService.verifyLobbyAccess("player1", "nonexistent-lobby");

        assertFalse(result);
    }

    @Test
    void testVerifyLobbyAccessMultiplePlayers() {
        assertTrue(verificationService.verifyLobbyAccess("player1", testLobby.getId()));
        assertTrue(verificationService.verifyLobbyAccess("player2", testLobby.getId()));
        assertFalse(verificationService.verifyLobbyAccess("player3", testLobby.getId()));
    }

    @Test
    void testVerifyGroupAccessDelegatestoLobbyAccess() {
        boolean result = verificationService.verifyGroupAccess("player1", testLobby.getId());

        assertTrue(result);
    }

    @Test
    void testVerifyGroupAccessInvalidGroup() {
        boolean result = verificationService.verifyGroupAccess("player1", "nonexistent-group");

        assertFalse(result);
    }

    @Test
    void testVerifyGroupAccessMultiplePlayers() {
        assertTrue(verificationService.verifyGroupAccess("player1", testLobby.getId()));
        assertTrue(verificationService.verifyGroupAccess("player2", testLobby.getId()));
        assertFalse(verificationService.verifyGroupAccess("player3", testLobby.getId()));
    }

    @Test
    void testVerificationAfterPlayerAddsToLobby() {
        assertFalse(verificationService.verifyLobbyAccess("player3", testLobby.getId()));

        lobbyService.addPlayer(testLobby.getId(), "player3", "Player3", PlayerRole.PLAYER);

        assertTrue(verificationService.verifyLobbyAccess("player3", testLobby.getId()));
    }

    @Test
    void testVerificationAfterPlayerLeavesLobby() {
        assertTrue(verificationService.verifyLobbyAccess("player2", testLobby.getId()));

        lobbyService.playerLeaves(testLobby.getId(), "player2");

        assertFalse(verificationService.verifyLobbyAccess("player2", testLobby.getId()));
    }

    @Test
    void testVerificationWithAdminPlayer() {
        assertTrue(verificationService.verifyLobbyAccess(testLobby.getAdminId(), testLobby.getId()));
    }

    @Test
    void testVerificationNullPlayerIdReturnsNull() {
        assertDoesNotThrow(() -> {
            boolean result = verificationService.verifyLobbyAccess(null, testLobby.getId());
            assertFalse(result);
        });
    }

    @Test
    void testVerificationNullLobbyIdThrowsException() {
        assertFalse(verificationService.verifyLobbyAccess("player1", null));
    }
}
