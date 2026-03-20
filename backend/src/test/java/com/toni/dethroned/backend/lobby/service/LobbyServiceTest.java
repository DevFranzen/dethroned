package com.toni.dethroned.backend.lobby.service;

import com.toni.dethroned.backend.lobby.domain.GameSettings;
import com.toni.dethroned.backend.lobby.domain.Lobby;
import com.toni.dethroned.backend.lobby.domain.LobbyStatus;
import com.toni.dethroned.backend.lobby.domain.Player;
import com.toni.dethroned.backend.lobby.domain.PlayerRole;
import com.toni.dethroned.backend.lobby.domain.PlayerStatus;
import com.toni.dethroned.backend.lobby.exception.InvalidHostException;
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
        Lobby lobby = lobbyService.createLobby("host1", gameSettings);

        assertNotNull(lobby);
        assertNotNull(lobby.getId());
        assertNotNull(lobby.getCode());
        assertEquals("host1", lobby.getHostId());
        assertEquals(LobbyStatus.OPEN, lobby.getStatus());
        assertEquals(0, lobby.getPlayers().size());
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
    void testDeleteLobbyNotHost() {
        Lobby lobby = lobbyService.createLobby("host1", gameSettings);

        assertThrows(InvalidHostException.class, () -> {
            lobbyService.deleteLobby(lobby.getId(), "nothost");
        });
    }

    @Test
    void testAddPlayer() {
        Lobby lobby = lobbyService.createLobby("host1", gameSettings);
        Player player = lobbyService.addPlayer(lobby.getId(), "player1", PlayerRole.PLAYER);

        assertNotNull(player);
        assertEquals("player1", player.getUsername());
        assertEquals(PlayerRole.PLAYER, player.getRole());
        assertEquals(PlayerStatus.CONNECTED, player.getStatus());
    }

    @Test
    void testAddPlayerLobbyFull() {
        GameSettings smallSettings = new GameSettings(2, 2, "elimination", 0, 800, 600);
        Lobby lobby = lobbyService.createLobby("host1", smallSettings);
        lobbyService.addPlayer(lobby.getId(), "player1", PlayerRole.PLAYER);
        lobbyService.addPlayer(lobby.getId(), "player2", PlayerRole.PLAYER);

        assertThrows(LobbyFullException.class, () -> {
            lobbyService.addPlayer(lobby.getId(), "player3", PlayerRole.PLAYER);
        });
    }

    @Test
    void testRemovePlayer() {
        Lobby lobby = lobbyService.createLobby("host1", gameSettings);
        Player player = lobbyService.addPlayer(lobby.getId(), "player1", PlayerRole.PLAYER);

        lobbyService.removePlayer(lobby.getId(), player.getId());

        assertEquals(0, lobby.getPlayers().size());
    }

    @Test
    void testRemovePlayerNotFound() {
        Lobby lobby = lobbyService.createLobby("host1", gameSettings);

        assertThrows(PlayerNotFoundException.class, () -> {
            lobbyService.removePlayer(lobby.getId(), "nonexistent");
        });
    }

    @Test
    void testMarkPlayerReady() {
        Lobby lobby = lobbyService.createLobby("host1", gameSettings);
        Player player = lobbyService.addPlayer(lobby.getId(), "player1", PlayerRole.PLAYER);

        lobbyService.markPlayerReady(lobby.getId(), player.getId());

        assertEquals(PlayerStatus.READY, lobby.getPlayer(player.getId()).getStatus());
    }

    @Test
    void testLobbyTransitionsToReadyWhenAllPlayersReady() {
        Lobby lobby = lobbyService.createLobby("host1", gameSettings);
        Player player1 = lobbyService.addPlayer(lobby.getId(), "player1", PlayerRole.PLAYER);
        Player player2 = lobbyService.addPlayer(lobby.getId(), "player2", PlayerRole.PLAYER);

        lobbyService.markPlayerReady(lobby.getId(), player1.getId());
        assertEquals(LobbyStatus.OPEN, lobby.getStatus());

        lobbyService.markPlayerReady(lobby.getId(), player2.getId());
        assertEquals(LobbyStatus.READY, lobby.getStatus());
    }
}
