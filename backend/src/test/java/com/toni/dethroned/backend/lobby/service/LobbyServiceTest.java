package com.toni.dethroned.backend.lobby.service;

import com.toni.dethroned.backend.lobby.domain.GameSettings;
import com.toni.dethroned.backend.lobby.domain.Lobby;
import com.toni.dethroned.backend.lobby.domain.LobbyStatus;
import com.toni.dethroned.backend.lobby.domain.Player;
import com.toni.dethroned.backend.lobby.domain.PlayerRole;
import com.toni.dethroned.backend.lobby.domain.PlayerStatus;
import com.toni.dethroned.backend.lobby.exception.InvalidAdminException;
import com.toni.dethroned.backend.lobby.exception.LobbyFullException;
import com.toni.dethroned.backend.lobby.exception.LobbyNotFoundException;
import com.toni.dethroned.backend.lobby.exception.PlayerNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LobbyServiceTest {
    private LobbyService lobbyService;
    private GameSettings gameSettings;

    @BeforeEach
    void setUp() {
        lobbyService = new LobbyService();
        gameSettings = new GameSettings(2, 4, "elimination", 0, 800, 600);
    }

    @Test
    void testCreateLobby() {
        Lobby lobby = lobbyService.createLobby("player1", gameSettings);

        assertNotNull(lobby);
        assertNotNull(lobby.getId());
        assertNotNull(lobby.getCode());
        assertEquals("player1", lobby.getAdminId());
        assertEquals(LobbyStatus.OPEN, lobby.getStatus());
        assertEquals(1, lobby.getPlayers().size()); // Creator is automatically member
    }

    @Test
    void testLobbyCodeIsUnique() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            Lobby lobby = lobbyService.createLobby("host" + i, gameSettings);
            codes.add(lobby.getCode());
        }
        assertEquals(10, codes.size());
    }

    @Test
    void testGetLobbyById() {
        Lobby created = lobbyService.createLobby("host1", gameSettings);
        Lobby retrieved = lobbyService.getLobbyById(created.getId());

        assertEquals(created.getId(), retrieved.getId());
    }

    @Test
    void testGetLobbyByIdNotFound() {
        assertThrows(LobbyNotFoundException.class, () -> {
            lobbyService.getLobbyById("nonexistent");
        });
    }

    @Test
    void testGetLobbyByCode() {
        Lobby created = lobbyService.createLobby("host1", gameSettings);
        Lobby retrieved = lobbyService.getLobbyByCode(created.getCode());

        assertEquals(created.getId(), retrieved.getId());
    }

    @Test
    void testGetOpenLobbies() {
        lobbyService.createLobby("host1", gameSettings);
        lobbyService.createLobby("host2", gameSettings);

        List<Lobby> lobbies = lobbyService.getOpenLobbies();
        assertEquals(2, lobbies.size());
    }

    @Test
    void testDeleteLobby() {
        Lobby lobby = lobbyService.createLobby("host1", gameSettings);
        lobbyService.deleteLobby(lobby.getId(), "host1");

        assertThrows(LobbyNotFoundException.class, () -> {
            lobbyService.getLobbyById(lobby.getId());
        });
    }

    @Test
    void testDeleteLobbyNotAdmin() {
        Lobby lobby = lobbyService.createLobby("admin1", gameSettings);

        assertThrows(InvalidAdminException.class, () -> {
            lobbyService.deleteLobby(lobby.getId(), "notadmin");
        });
    }

    @Test
    void testAddPlayer() {
        Lobby lobby = lobbyService.createLobby("host1", gameSettings);
        String playerId = "player-123";
        Player player = lobbyService.addPlayer(lobby.getId(), playerId, "player1", PlayerRole.PLAYER);

        assertNotNull(player);
        assertEquals(playerId, player.getId());
        assertEquals("player1", player.getUsername());
        assertEquals(PlayerRole.PLAYER, player.getRole());
        assertEquals(PlayerStatus.CONNECTED, player.getStatus());
    }

    @Test
    void testAddPlayerLobbyFull() {
        GameSettings smallSettings = new GameSettings(2, 2, "elimination", 0, 800, 600);
        Lobby lobby = lobbyService.createLobby("admin1", smallSettings);
        // Admin + 1 Player = 2 (full)
        lobbyService.addPlayer(lobby.getId(), "player-456", "player1", PlayerRole.PLAYER);

        assertThrows(LobbyFullException.class, () -> {
            lobbyService.addPlayer(lobby.getId(), "player-789", "player2", PlayerRole.PLAYER);
        });
    }

    @Test
    void testPlayerLeaves() {
        Lobby lobby = lobbyService.createLobby("admin1", gameSettings);
        String playerId = "player-123";
        Player player = lobbyService.addPlayer(lobby.getId(), playerId, "player1", PlayerRole.PLAYER);

        lobbyService.playerLeaves(lobby.getId(), player.getId());

        assertEquals(1, lobby.getPlayers().size()); // Admin remains
    }

    @Test
    void testPlayerLeavesNotFound() {
        Lobby lobby = lobbyService.createLobby("admin1", gameSettings);

        assertThrows(PlayerNotFoundException.class, () -> {
            lobbyService.playerLeaves(lobby.getId(), "nonexistent");
        });
    }

    @Test
    void testMarkPlayerReady() {
        Lobby lobby = lobbyService.createLobby("host1", gameSettings);
        String playerId = "player-123";
        Player player = lobbyService.addPlayer(lobby.getId(), playerId, "player1", PlayerRole.PLAYER);

        lobbyService.markPlayerReady(lobby.getId(), player.getId());

        assertEquals(PlayerStatus.READY, lobby.getPlayer(player.getId()).getStatus());
    }

    @Test
    void testLobbyTransitionsToReadyWhenAllPlayersReady() {
        Lobby lobby = lobbyService.createLobby("admin1", gameSettings);
        String adminId = "admin1";
        String playerId1 = "player-456";
        String playerId2 = "player-789";
        Player player1 = lobbyService.addPlayer(lobby.getId(), playerId1, "player1", PlayerRole.PLAYER);
        Player player2 = lobbyService.addPlayer(lobby.getId(), playerId2, "player2", PlayerRole.PLAYER);

        // Admin ready
        lobbyService.markPlayerReady(lobby.getId(), adminId);
        assertEquals(LobbyStatus.OPEN, lobby.getStatus());

        // Player 1 ready
        lobbyService.markPlayerReady(lobby.getId(), player1.getId());
        assertEquals(LobbyStatus.OPEN, lobby.getStatus());

        // Player 2 ready - all 3 players ready, lobby transitions to READY
        lobbyService.markPlayerReady(lobby.getId(), player2.getId());
        assertEquals(LobbyStatus.READY, lobby.getStatus());
    }
}
