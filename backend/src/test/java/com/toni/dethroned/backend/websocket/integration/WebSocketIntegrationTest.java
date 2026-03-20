package com.toni.dethroned.backend.websocket.integration;

import com.toni.dethroned.backend.lobby.domain.GameSettings;
import com.toni.dethroned.backend.lobby.domain.Lobby;
import com.toni.dethroned.backend.lobby.domain.PlayerRole;
import com.toni.dethroned.backend.lobby.service.LobbyService;
import com.toni.dethroned.backend.websocket.handler.GameWebSocketHandler;
import com.toni.dethroned.backend.websocket.service.SessionVerificationService;
import com.toni.dethroned.backend.websocket.service.WebSocketConnectionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for complete WebSocket connection flow:
 * - Multiple players connecting to the same lobby
 * - Reconnection scenarios
 * - Broadcasting messages
 * - Disconnection cleanup
 */
class WebSocketIntegrationTest {
    private GameWebSocketHandler handler;
    private WebSocketConnectionManager connectionManager;
    private SessionVerificationService verificationService;
    private LobbyService lobbyService;
    private Lobby testLobby;
    private WebSocketSession playerSession1;
    private WebSocketSession playerSession2;
    private WebSocketSession playerSession3;

    @BeforeEach
    void setUp() {
        // Setup services
        lobbyService = new LobbyService();
        connectionManager = new WebSocketConnectionManager();
        verificationService = new SessionVerificationService(lobbyService);
        handler = new GameWebSocketHandler(connectionManager, verificationService, lobbyService);

        // Create lobby with players
        GameSettings settings = new GameSettings(2, 4, "elimination", 0, 800, 600);
        testLobby = lobbyService.createLobby("player1", settings);
        lobbyService.addPlayer(testLobby.getId(), "player2", "Player2", PlayerRole.PLAYER);
        lobbyService.addPlayer(testLobby.getId(), "player3", "Player3", PlayerRole.PLAYER);

        // Initialize connection group
        connectionManager.createConnectionGroup(testLobby.getId());

        // Now create mock sessions with proper URI
        playerSession1 = createMockSession("player1");
        playerSession2 = createMockSession("player2");
        playerSession3 = createMockSession("player3");
    }

    private WebSocketSession createMockSession(String playerId) {
        WebSocketSession session = mock(WebSocketSession.class);
        try {
            when(session.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/" + playerId + "/session/" + testLobby.getId()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    @Test
    void testScenarioThreePlayersConnectingSequentially() throws Exception {
        // Act - Player 1 connects
        handler.afterConnectionEstablished(playerSession1);
        assertEquals(1, testLobby.getConnectionGroup().getConnectionCount());
        assertTrue(testLobby.getConnectionGroup().isPlayerConnected("player1"));

        // Act - Player 2 connects
        handler.afterConnectionEstablished(playerSession2);
        assertEquals(2, testLobby.getConnectionGroup().getConnectionCount());
        assertTrue(testLobby.getConnectionGroup().isPlayerConnected("player1"));
        assertTrue(testLobby.getConnectionGroup().isPlayerConnected("player2"));

        // Act - Player 3 connects
        handler.afterConnectionEstablished(playerSession3);
        assertEquals(3, testLobby.getConnectionGroup().getConnectionCount());
        assertTrue(testLobby.getConnectionGroup().isPlayerConnected("player1"));
        assertTrue(testLobby.getConnectionGroup().isPlayerConnected("player2"));
        assertTrue(testLobby.getConnectionGroup().isPlayerConnected("player3"));
    }

    @Test
    void testScenarioPlayerReconnectsAfterDisconnection() throws Exception {
        // Arrange - Initial connection
        handler.afterConnectionEstablished(playerSession1);
        assertTrue(testLobby.getConnectionGroup().isPlayerConnected("player1"));
        assertSame(playerSession1, testLobby.getConnectionGroup().getPlayerConnection("player1").getWebSocketSession());

        // Act - Player disconnects
        handler.afterConnectionClosed(playerSession1, CloseStatus.NORMAL);
        assertFalse(testLobby.getConnectionGroup().isPlayerConnected("player1"));
        assertEquals(0, testLobby.getConnectionGroup().getConnectionCount());

        // Arrange - New session for reconnection
        WebSocketSession reconnectSession = createMockSession("player1");

        // Act - Player reconnects
        handler.afterConnectionEstablished(reconnectSession);
        assertTrue(testLobby.getConnectionGroup().isPlayerConnected("player1"));
        assertSame(reconnectSession, testLobby.getConnectionGroup().getPlayerConnection("player1").getWebSocketSession());
        assertEquals(1, testLobby.getConnectionGroup().getConnectionCount());
    }

    @Test
    void testScenarioOnePlayerDisconnectsWhileOthersStayConnected() throws Exception {
        // Arrange - All 3 players connect
        handler.afterConnectionEstablished(playerSession1);
        handler.afterConnectionEstablished(playerSession2);
        handler.afterConnectionEstablished(playerSession3);
        assertEquals(3, testLobby.getConnectionGroup().getConnectionCount());

        // Act - Player 2 disconnects
        handler.afterConnectionClosed(playerSession2, CloseStatus.NORMAL);

        // Assert - Remaining players still connected
        assertEquals(2, testLobby.getConnectionGroup().getConnectionCount());
        assertTrue(testLobby.getConnectionGroup().isPlayerConnected("player1"));
        assertFalse(testLobby.getConnectionGroup().isPlayerConnected("player2"));
        assertTrue(testLobby.getConnectionGroup().isPlayerConnected("player3"));

        // Verify broadcast was sent to remaining players
        verify(playerSession1, atLeastOnce()).sendMessage(any());
        verify(playerSession3, atLeastOnce()).sendMessage(any());
    }

    @Test
    void testScenarioSequentialDisconnectionsCloseToLastPlayer() throws Exception {
        // Arrange - All players connect
        handler.afterConnectionEstablished(playerSession1);
        handler.afterConnectionEstablished(playerSession2);
        handler.afterConnectionEstablished(playerSession3);
        assertEquals(3, testLobby.getConnectionGroup().getConnectionCount());

        // Act - First player disconnects
        handler.afterConnectionClosed(playerSession1, CloseStatus.NORMAL);
        assertEquals(2, testLobby.getConnectionGroup().getConnectionCount());

        // Act - Second player disconnects
        handler.afterConnectionClosed(playerSession2, CloseStatus.NORMAL);
        assertEquals(1, testLobby.getConnectionGroup().getConnectionCount());

        // Assert - Last player still connected
        assertTrue(testLobby.getConnectionGroup().isPlayerConnected("player3"));
        assertFalse(testLobby.getConnectionGroup().isPlayerConnected("player1"));
        assertFalse(testLobby.getConnectionGroup().isPlayerConnected("player2"));
    }

    @Test
    void testScenarioRapidReconnectionWithoutPriorDisconnection() throws Exception {
        // Arrange - Player 1 connects with original session
        handler.afterConnectionEstablished(playerSession1);
        assertEquals(1, testLobby.getConnectionGroup().getConnectionCount());

        // Act - Player 1 reconnects with new session (simulating network retry)
        WebSocketSession newSession = createMockSession("player1");
        handler.afterConnectionEstablished(newSession);

        // Assert - Old session replaced, count stays same, new session is active
        assertEquals(1, testLobby.getConnectionGroup().getConnectionCount());
        assertTrue(testLobby.getConnectionGroup().isPlayerConnected("player1"));
        assertSame(newSession, testLobby.getConnectionGroup().getPlayerConnection("player1").getWebSocketSession());
    }

    @Test
    void testScenarioInterleavedConnectDisconnectReconnect() throws Exception {
        // Player 1 connects
        handler.afterConnectionEstablished(playerSession1);
        assertEquals(1, testLobby.getConnectionGroup().getConnectionCount());

        // Player 2 connects
        handler.afterConnectionEstablished(playerSession2);
        assertEquals(2, testLobby.getConnectionGroup().getConnectionCount());

        // Player 1 disconnects
        handler.afterConnectionClosed(playerSession1, CloseStatus.NORMAL);
        assertEquals(1, testLobby.getConnectionGroup().getConnectionCount());
        assertTrue(testLobby.getConnectionGroup().isPlayerConnected("player2"));
        assertFalse(testLobby.getConnectionGroup().isPlayerConnected("player1"));

        // Player 3 connects
        handler.afterConnectionEstablished(playerSession3);
        assertEquals(2, testLobby.getConnectionGroup().getConnectionCount());
        assertTrue(testLobby.getConnectionGroup().isPlayerConnected("player2"));
        assertTrue(testLobby.getConnectionGroup().isPlayerConnected("player3"));

        // Player 1 reconnects
        WebSocketSession player1Reconnect = createMockSession("player1");
        handler.afterConnectionEstablished(player1Reconnect);
        assertEquals(3, testLobby.getConnectionGroup().getConnectionCount());
        assertTrue(testLobby.getConnectionGroup().isPlayerConnected("player1"));
        assertTrue(testLobby.getConnectionGroup().isPlayerConnected("player2"));
        assertTrue(testLobby.getConnectionGroup().isPlayerConnected("player3"));
    }

    @Test
    void testScenarioPlayerDisconnectsAndReconnectsMultipleTimes() throws Exception {
        // Initial connect
        handler.afterConnectionEstablished(playerSession1);
        assertTrue(testLobby.getConnectionGroup().isPlayerConnected("player1"));

        // First disconnect
        handler.afterConnectionClosed(playerSession1, CloseStatus.NORMAL);
        assertFalse(testLobby.getConnectionGroup().isPlayerConnected("player1"));

        // First reconnect
        WebSocketSession session2 = createMockSession("player1");
        handler.afterConnectionEstablished(session2);
        assertTrue(testLobby.getConnectionGroup().isPlayerConnected("player1"));

        // Second disconnect
        handler.afterConnectionClosed(session2, CloseStatus.NORMAL);
        assertFalse(testLobby.getConnectionGroup().isPlayerConnected("player1"));

        // Second reconnect
        WebSocketSession session3 = createMockSession("player1");
        handler.afterConnectionEstablished(session3);
        assertTrue(testLobby.getConnectionGroup().isPlayerConnected("player1"));
        assertSame(session3, testLobby.getConnectionGroup().getPlayerConnection("player1").getWebSocketSession());
    }

    @Test
    void testScenarioAllPlayersDisconnectAndReconnect() throws Exception {
        // All connect
        handler.afterConnectionEstablished(playerSession1);
        handler.afterConnectionEstablished(playerSession2);
        handler.afterConnectionEstablished(playerSession3);
        assertEquals(3, testLobby.getConnectionGroup().getConnectionCount());

        // All disconnect
        handler.afterConnectionClosed(playerSession1, CloseStatus.NORMAL);
        handler.afterConnectionClosed(playerSession2, CloseStatus.NORMAL);
        handler.afterConnectionClosed(playerSession3, CloseStatus.NORMAL);
        assertEquals(0, testLobby.getConnectionGroup().getConnectionCount());

        // All reconnect with new sessions
        WebSocketSession reconnect1 = createMockSession("player1");
        WebSocketSession reconnect2 = createMockSession("player2");
        WebSocketSession reconnect3 = createMockSession("player3");

        handler.afterConnectionEstablished(reconnect1);
        handler.afterConnectionEstablished(reconnect2);
        handler.afterConnectionEstablished(reconnect3);
        assertEquals(3, testLobby.getConnectionGroup().getConnectionCount());
        assertTrue(testLobby.getConnectionGroup().isPlayerConnected("player1"));
        assertTrue(testLobby.getConnectionGroup().isPlayerConnected("player2"));
        assertTrue(testLobby.getConnectionGroup().isPlayerConnected("player3"));
    }

    @Test
    void testScenarioConnectionCountAccuracy() throws Exception {
        assertEquals(0, testLobby.getConnectionGroup().getConnectionCount());

        // Sequential connections
        for (int i = 0; i < 3; i++) {
            handler.afterConnectionEstablished(switch (i) {
                case 0 -> playerSession1;
                case 1 -> playerSession2;
                case 2 -> playerSession3;
                default -> null;
            });
            assertEquals(i + 1, testLobby.getConnectionGroup().getConnectionCount());
        }

        // Sequential disconnections
        handler.afterConnectionClosed(playerSession1, CloseStatus.NORMAL);
        assertEquals(2, testLobby.getConnectionGroup().getConnectionCount());

        handler.afterConnectionClosed(playerSession2, CloseStatus.NORMAL);
        assertEquals(1, testLobby.getConnectionGroup().getConnectionCount());

        handler.afterConnectionClosed(playerSession3, CloseStatus.NORMAL);
        assertEquals(0, testLobby.getConnectionGroup().getConnectionCount());
    }
}
