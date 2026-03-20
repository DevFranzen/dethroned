package com.toni.dethroned.backend.lobby.service;

import com.toni.dethroned.backend.lobby.domain.GameSettings;
import com.toni.dethroned.backend.lobby.domain.Lobby;
import com.toni.dethroned.backend.lobby.domain.Player;
import com.toni.dethroned.backend.lobby.domain.PlayerRole;
import com.toni.dethroned.backend.lobby.exception.InvalidAdminException;
import com.toni.dethroned.backend.lobby.exception.LobbyNotFoundException;
import com.toni.dethroned.backend.lobby.exception.PlayerNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LobbyServiceAuthTest {
    private LobbyService lobbyService;
    private GameSettings gameSettings;

    @BeforeEach
    void setUp() {
        lobbyService = new LobbyService();
        gameSettings = new GameSettings(2, 4, "elimination", 0, 800, 600);
    }

    @Test
    void testCreateLobbyWithPlayerId() {
        String playerId = "player-123";
        Lobby lobby = lobbyService.createLobby(playerId, gameSettings);

        assertNotNull(lobby);
        assertEquals(playerId, lobby.getAdminId());
        assertEquals(1, lobby.getPlayers().size());
        assertTrue(lobby.getPlayers().containsKey(playerId));
    }

    @Test
    void testCreatorBecomesAdminAndMember() {
        String playerId = "player-123";
        Lobby lobby = lobbyService.createLobby(playerId, gameSettings);

        Player creatorPlayer = lobby.getPlayer(playerId);
        assertNotNull(creatorPlayer);
        assertEquals("player-123", creatorPlayer.getId());
        assertEquals(PlayerRole.PLAYER, creatorPlayer.getRole());
    }

    @Test
    void testPlayerLeavesLobby() {
        String playerId = "player-123";
        Lobby lobby = lobbyService.createLobby(playerId, gameSettings);
        String player2Id = "player-456";
        Player player2 = lobbyService.addPlayer(lobby.getId(), player2Id, "Player 2", PlayerRole.PLAYER);

        lobbyService.playerLeaves(lobby.getId(), player2.getId());

        assertEquals(1, lobby.getPlayers().size());
        assertNull(lobby.getPlayer(player2.getId()));
    }

    @Test
    void testLastPlayerLeavesDeletesLobby() {
        String playerId = "player-123";
        Lobby lobby = lobbyService.createLobby(playerId, gameSettings);
        String lobbyId = lobby.getId();

        lobbyService.playerLeaves(lobbyId, playerId);

        assertThrows(LobbyNotFoundException.class, () -> {
            lobbyService.getLobbyById(lobbyId);
        });
    }

    @Test
    void testAdminTransferToOldestPlayerWhenAdminLeaves() throws InterruptedException {
        String admin = "admin-player";
        Lobby lobby = lobbyService.createLobby(admin, gameSettings);
        String lobbyId = lobby.getId();

        String player2Id = "player-456";
        lobbyService.addPlayer(lobbyId, player2Id, "Player 2", PlayerRole.PLAYER);

        Thread.sleep(10); // Ensure different timestamps

        String player3Id = "player-789";
        lobbyService.addPlayer(lobbyId, player3Id, "Player 3", PlayerRole.PLAYER);

        // Admin leaves
        lobbyService.playerLeaves(lobbyId, admin);

        // Player 2 should be admin now (oldest remaining)
        Lobby updatedLobby = lobbyService.getLobbyById(lobbyId);
        assertEquals(player2Id, updatedLobby.getAdminId());
    }

    @Test
    void testAdminTransfersToCorrectOldestPlayer() throws InterruptedException {
        String admin = "admin-player";
        Lobby lobby = lobbyService.createLobby(admin, gameSettings);
        String lobbyId = lobby.getId();

        // Add players with delays to ensure different join times
        Thread.sleep(5);
        String player2Id = "player-456";
        lobbyService.addPlayer(lobbyId, player2Id, "Player 2", PlayerRole.PLAYER);

        Thread.sleep(5);
        String player3Id = "player-789";
        lobbyService.addPlayer(lobbyId, player3Id, "Player 3", PlayerRole.PLAYER);

        Thread.sleep(5);
        String player4Id = "player-101";
        lobbyService.addPlayer(lobbyId, player4Id, "Player 4", PlayerRole.PLAYER);

        // Admin leaves
        lobbyService.playerLeaves(lobbyId, admin);

        // Player 2 should be admin (was second to join, first after admin)
        Lobby updatedLobby = lobbyService.getLobbyById(lobbyId);
        assertEquals(player2Id, updatedLobby.getAdminId());
    }

    @Test
    void testDeleteLobbyWithWrongAdmin() {
        String admin = "admin-123";
        Lobby lobby = lobbyService.createLobby(admin, gameSettings);

        assertThrows(InvalidAdminException.class, () -> {
            lobbyService.deleteLobby(lobby.getId(), "wrong-admin");
        });
    }

    @Test
    void testDeleteLobbyWithCorrectAdmin() {
        String admin = "admin-123";
        Lobby lobby = lobbyService.createLobby(admin, gameSettings);
        String lobbyId = lobby.getId();

        lobbyService.deleteLobby(lobbyId, admin);

        assertThrows(LobbyNotFoundException.class, () -> {
            lobbyService.getLobbyById(lobbyId);
        });
    }

    @Test
    void testPlayerLeavesWithInvalidPlayer() {
        String admin = "admin-123";
        Lobby lobby = lobbyService.createLobby(admin, gameSettings);

        assertThrows(PlayerNotFoundException.class, () -> {
            lobbyService.playerLeaves(lobby.getId(), "non-existent-player");
        });
    }
}
