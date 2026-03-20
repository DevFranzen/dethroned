package com.toni.dethroned.backend.lobby.controller;

import com.toni.dethroned.backend.lobby.domain.GameSettings;
import com.toni.dethroned.backend.lobby.domain.Lobby;
import com.toni.dethroned.backend.lobby.domain.PlayerRole;
import com.toni.dethroned.backend.lobby.domain.PlayerStatus;
import com.toni.dethroned.backend.lobby.dto.AddPlayerRequest;
import com.toni.dethroned.backend.lobby.dto.LobbyResponse;
import com.toni.dethroned.backend.lobby.service.LobbyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class LobbyControllerAddPlayerTest {
    private LobbyController controller;
    private LobbyService lobbyService;
    private Lobby testLobby;

    @BeforeEach
    void setUp() {
        lobbyService = new LobbyService();
        controller = new LobbyController(lobbyService);

        // Create test lobby
        GameSettings settings = new GameSettings(2, 4, "elimination", 0, 800, 600);
        testLobby = lobbyService.createLobby("admin-player", settings);
    }

    @Test
    void testAddPlayerReturnsLobbyResponse() {
        AddPlayerRequest request = new AddPlayerRequest("new-player", "NewPlayer", PlayerRole.PLAYER);

        ResponseEntity<LobbyResponse> response = controller.addPlayer(testLobby.getId(), request);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testAddPlayerResponseContainsLobbyData() {
        AddPlayerRequest request = new AddPlayerRequest("new-player", "NewPlayer", PlayerRole.PLAYER);

        ResponseEntity<LobbyResponse> response = controller.addPlayer(testLobby.getId(), request);
        LobbyResponse body = response.getBody();

        assertNotNull(body);
        assertEquals(testLobby.getId(), body.getId());
        assertEquals(testLobby.getCode(), body.getCode());
        assertEquals(testLobby.getAdminId(), body.getAdminId());
    }

    @Test
    void testAddPlayerResponseContainsAllPlayers() {
        // First player joins
        AddPlayerRequest request1 = new AddPlayerRequest("player1", "Player1", PlayerRole.PLAYER);
        ResponseEntity<LobbyResponse> response1 = controller.addPlayer(testLobby.getId(), request1);
        LobbyResponse body1 = response1.getBody();

        assertNotNull(body1);
        assertEquals(2, body1.getPlayers().size()); // Admin + new player

        // Second player joins
        AddPlayerRequest request2 = new AddPlayerRequest("player2", "Player2", PlayerRole.PLAYER);
        ResponseEntity<LobbyResponse> response2 = controller.addPlayer(testLobby.getId(), request2);
        LobbyResponse body2 = response2.getBody();

        assertNotNull(body2);
        assertEquals(3, body2.getPlayers().size()); // Admin + 2 players
    }

    @Test
    void testAddPlayerResponseContainsGameSettings() {
        AddPlayerRequest request = new AddPlayerRequest("new-player", "NewPlayer", PlayerRole.PLAYER);

        ResponseEntity<LobbyResponse> response = controller.addPlayer(testLobby.getId(), request);
        LobbyResponse body = response.getBody();

        assertNotNull(body);
        assertNotNull(body.getSettings());
        assertEquals(2, body.getSettings().getMinPlayers());
        assertEquals(4, body.getSettings().getMaxPlayers());
        assertEquals("elimination", body.getSettings().getGameMode());
    }

    @Test
    void testAddPlayerResponseContainsPlayerDetails() {
        AddPlayerRequest request = new AddPlayerRequest("player-123", "TestPlayer", PlayerRole.PLAYER);

        ResponseEntity<LobbyResponse> response = controller.addPlayer(testLobby.getId(), request);
        LobbyResponse body = response.getBody();

        assertNotNull(body);
        assertTrue(body.getPlayers().stream().anyMatch(p -> p.getId().equals("player-123")));
        assertTrue(body.getPlayers().stream().anyMatch(p -> p.getUsername().equals("TestPlayer")));
        assertTrue(body.getPlayers().stream().anyMatch(p -> p.getStatus() == PlayerStatus.CONNECTED));
        assertTrue(body.getPlayers().stream().anyMatch(p -> p.getRole() == PlayerRole.PLAYER));
    }

    @Test
    void testAddPlayerResponseHasTimestamps() {
        AddPlayerRequest request = new AddPlayerRequest("new-player", "NewPlayer", PlayerRole.PLAYER);

        ResponseEntity<LobbyResponse> response = controller.addPlayer(testLobby.getId(), request);
        LobbyResponse body = response.getBody();

        assertNotNull(body);
        assertNotNull(body.getCreatedAt());
        assertNotNull(body.getLastActivity());
    }

    @Test
    void testAddMultiplePlayersGetLatestLobbyState() {
        // Add first player
        AddPlayerRequest request1 = new AddPlayerRequest("player1", "Player1", PlayerRole.PLAYER);
        ResponseEntity<LobbyResponse> response1 = controller.addPlayer(testLobby.getId(), request1);
        LobbyResponse body1 = response1.getBody();
        assert body1 != null;
        int countAfterFirst = body1.getPlayers().size();

        // Add second player
        AddPlayerRequest request2 = new AddPlayerRequest("player2", "Player2", PlayerRole.PLAYER);
        ResponseEntity<LobbyResponse> response2 = controller.addPlayer(testLobby.getId(), request2);
        LobbyResponse body2 = response2.getBody();
        assert body2 != null;
        int countAfterSecond = body2.getPlayers().size();

        // Verify second response includes both new players
        assertEquals(countAfterFirst + 1, countAfterSecond);
        assertTrue(body2.getPlayers().stream().anyMatch(p -> p.getId().equals("player1")));
        assertTrue(body2.getPlayers().stream().anyMatch(p -> p.getId().equals("player2")));
    }

    @Test
    void testAddPlayerResponseIncludesAdminAndNewPlayer() {
        AddPlayerRequest request = new AddPlayerRequest("new-player", "NewPlayer", PlayerRole.PLAYER);

        ResponseEntity<LobbyResponse> response = controller.addPlayer(testLobby.getId(), request);
        LobbyResponse body = response.getBody();

        assertNotNull(body);
        assertEquals(2, body.getPlayers().size());

        // Verify admin is in the list
        assertTrue(body.getPlayers().stream().anyMatch(p -> p.getId().equals("admin-player")));

        // Verify new player is in the list
        assertTrue(body.getPlayers().stream().anyMatch(p -> p.getId().equals("new-player")));
    }
}
