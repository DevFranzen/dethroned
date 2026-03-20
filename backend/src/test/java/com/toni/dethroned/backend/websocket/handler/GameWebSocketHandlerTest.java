package com.toni.dethroned.backend.websocket.handler;

import com.toni.dethroned.backend.lobby.domain.GameSettings;
import com.toni.dethroned.backend.lobby.domain.Lobby;
import com.toni.dethroned.backend.lobby.domain.Player;
import com.toni.dethroned.backend.lobby.domain.PlayerRole;
import com.toni.dethroned.backend.lobby.service.LobbyService;
import com.toni.dethroned.backend.websocket.service.SessionVerificationService;
import com.toni.dethroned.backend.websocket.service.WebSocketConnectionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameWebSocketHandlerTest {
    private GameWebSocketHandler handler;
    private WebSocketConnectionManager connectionManager;
    private SessionVerificationService verificationService;
    private LobbyService lobbyService;
    private WebSocketSession mockSession;
    private WebSocketSession mockSession2;
    private Lobby testLobby;

    @BeforeEach
    void setUp() {
        connectionManager = new WebSocketConnectionManager();
        verificationService = mock(SessionVerificationService.class);
        lobbyService = mock(LobbyService.class);
        handler = new GameWebSocketHandler(connectionManager, verificationService, lobbyService);

        mockSession = mock(WebSocketSession.class);
        mockSession2 = mock(WebSocketSession.class);

        // Setup test lobby with game settings
        GameSettings settings = new GameSettings(2, 4, "elimination", 0, 800, 600);
        testLobby = new Lobby("lobby-123", "ABC123", "player1", settings);
        testLobby.addPlayer(new Player("player1", "Player1", PlayerRole.PLAYER));
        testLobby.addPlayer(new Player("player2", "Player2", PlayerRole.PLAYER));

        // Initialize connection group in connection manager
        connectionManager.createConnectionGroup("lobby-123");
    }

    @Test
    void testConnectionEstablishedWithValidPlayerId() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player1/session/lobby-123"));
        when(mockSession.isOpen()).thenReturn(true);
        when(verificationService.verifyGroupAccess("player1", "lobby-123")).thenReturn(true);
        when(lobbyService.getLobbyById("lobby-123")).thenReturn(testLobby);

        // Act
        handler.afterConnectionEstablished(mockSession);

        // Assert
        assertTrue(testLobby.getConnectionGroup().isPlayerConnected("player1"));
        verify(mockSession, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    void testConnectionFailsWithInvalidPlayerId() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player//session/lobby-123"));
        when(mockSession.isOpen()).thenReturn(true);

        // Act
        handler.afterConnectionEstablished(mockSession);

        // Assert
        verify(mockSession, times(1)).sendMessage(any(TextMessage.class));
        verify(mockSession, times(1)).close();
    }

    @Test
    void testConnectionFailsWithInvalidSessionId() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player1/session/"));
        when(mockSession.isOpen()).thenReturn(true);

        // Act
        handler.afterConnectionEstablished(mockSession);

        // Assert
        verify(mockSession, times(1)).close();
    }

    @Test
    void testConnectionFailsWhenPlayerNotAuthorized() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player3/session/lobby-123"));
        when(mockSession.isOpen()).thenReturn(true);
        when(verificationService.verifyGroupAccess("player3", "lobby-123")).thenReturn(false);

        // Act
        handler.afterConnectionEstablished(mockSession);

        // Assert
        verify(mockSession, times(1)).sendMessage(any(TextMessage.class));
        verify(mockSession, times(1)).close();
    }

    @Test
    void testConnectionFailsWhenLobbyNotFound() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player1/session/nonexistent"));
        when(mockSession.isOpen()).thenReturn(true);
        when(verificationService.verifyGroupAccess("player1", "nonexistent")).thenReturn(true);
        when(lobbyService.getLobbyById("nonexistent")).thenThrow(new RuntimeException("Lobby not found"));

        // Act
        handler.afterConnectionEstablished(mockSession);

        // Assert
        verify(mockSession, times(1)).sendMessage(any(TextMessage.class));
        verify(mockSession, times(1)).close();
    }

    @Test
    void testReconnectionReplacesPreviousSession() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player1/session/lobby-123"));
        when(mockSession.isOpen()).thenReturn(true);
        when(mockSession2.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player1/session/lobby-123"));
        when(mockSession2.isOpen()).thenReturn(true);
        when(verificationService.verifyGroupAccess("player1", "lobby-123")).thenReturn(true);
        when(lobbyService.getLobbyById("lobby-123")).thenReturn(testLobby);

        // Act - First connection
        handler.afterConnectionEstablished(mockSession);
        assertTrue(testLobby.getConnectionGroup().isPlayerConnected("player1"));
        assertSame(mockSession, testLobby.getConnectionGroup().getPlayerConnection("player1").getWebSocketSession());

        // Act - Reconnect with new session
        handler.afterConnectionEstablished(mockSession2);

        // Assert
        assertTrue(testLobby.getConnectionGroup().isPlayerConnected("player1"));
        assertSame(mockSession2, testLobby.getConnectionGroup().getPlayerConnection("player1").getWebSocketSession());
        assertEquals(1, testLobby.getConnectionGroup().getConnectionCount());
    }

    @Test
    void testMultiplePlayersCanConnect() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player1/session/lobby-123"));
        when(mockSession.isOpen()).thenReturn(true);
        when(mockSession2.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player2/session/lobby-123"));
        when(mockSession2.isOpen()).thenReturn(true);
        when(verificationService.verifyGroupAccess("player1", "lobby-123")).thenReturn(true);
        when(verificationService.verifyGroupAccess("player2", "lobby-123")).thenReturn(true);
        when(lobbyService.getLobbyById("lobby-123")).thenReturn(testLobby);

        // Act
        handler.afterConnectionEstablished(mockSession);
        handler.afterConnectionEstablished(mockSession2);

        // Assert
        assertTrue(testLobby.getConnectionGroup().isPlayerConnected("player1"));
        assertTrue(testLobby.getConnectionGroup().isPlayerConnected("player2"));
        assertEquals(2, testLobby.getConnectionGroup().getConnectionCount());
    }

    @Test
    void testDisconnectionRemovesPlayer() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player1/session/lobby-123"));
        when(mockSession.isOpen()).thenReturn(true);
        when(verificationService.verifyGroupAccess("player1", "lobby-123")).thenReturn(true);
        when(lobbyService.getLobbyById("lobby-123")).thenReturn(testLobby);

        handler.afterConnectionEstablished(mockSession);
        assertTrue(testLobby.getConnectionGroup().isPlayerConnected("player1"));

        // Act
        handler.afterConnectionClosed(mockSession, CloseStatus.NORMAL);

        // Assert
        assertFalse(testLobby.getConnectionGroup().isPlayerConnected("player1"));
    }

    @Test
    void testDisconnectionBroadcastsToRemainingPlayers() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player1/session/lobby-123"));
        when(mockSession.isOpen()).thenReturn(true);
        when(mockSession2.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player2/session/lobby-123"));
        when(mockSession2.isOpen()).thenReturn(true);
        when(verificationService.verifyGroupAccess("player1", "lobby-123")).thenReturn(true);
        when(verificationService.verifyGroupAccess("player2", "lobby-123")).thenReturn(true);
        when(lobbyService.getLobbyById("lobby-123")).thenReturn(testLobby);

        handler.afterConnectionEstablished(mockSession);
        handler.afterConnectionEstablished(mockSession2);

        // Act
        handler.afterConnectionClosed(mockSession, CloseStatus.NORMAL);

        // Assert
        assertFalse(testLobby.getConnectionGroup().isPlayerConnected("player1"));
        assertTrue(testLobby.getConnectionGroup().isPlayerConnected("player2"));
        // Verify broadcast message was sent to player2
        verify(mockSession2, atLeastOnce()).sendMessage(any(TextMessage.class));
    }

    @Test
    void testDisconnectionHandlesInvalidUri() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/invalid"));

        // Act - Should not throw exception
        assertDoesNotThrow(() -> handler.afterConnectionClosed(mockSession, CloseStatus.NORMAL));
    }

    @Test
    void testConnectionResponseContainsCorrectData() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player1/session/lobby-123"));
        when(mockSession.isOpen()).thenReturn(true);
        when(verificationService.verifyGroupAccess("player1", "lobby-123")).thenReturn(true);
        when(lobbyService.getLobbyById("lobby-123")).thenReturn(testLobby);

        // Act
        handler.afterConnectionEstablished(mockSession);

        // Assert
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(mockSession, atLeastOnce()).sendMessage(messageCaptor.capture());

        String payload = messageCaptor.getValue().getPayload();
        assertTrue(payload.contains("CONNECTED"));
        assertTrue(payload.contains("player1"));
        assertTrue(payload.contains("lobby-123"));
    }

    @Test
    void testSequentialConnectDisconnectReconnect() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player1/session/lobby-123"));
        when(mockSession.isOpen()).thenReturn(true);
        when(mockSession2.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player1/session/lobby-123"));
        when(mockSession2.isOpen()).thenReturn(true);
        when(verificationService.verifyGroupAccess("player1", "lobby-123")).thenReturn(true);
        when(lobbyService.getLobbyById("lobby-123")).thenReturn(testLobby);

        // Act - Connect
        handler.afterConnectionEstablished(mockSession);
        assertTrue(testLobby.getConnectionGroup().isPlayerConnected("player1"));
        assertEquals(1, testLobby.getConnectionGroup().getConnectionCount());

        // Act - Disconnect
        handler.afterConnectionClosed(mockSession, CloseStatus.NORMAL);
        assertFalse(testLobby.getConnectionGroup().isPlayerConnected("player1"));
        assertEquals(0, testLobby.getConnectionGroup().getConnectionCount());

        // Act - Reconnect with new session
        handler.afterConnectionEstablished(mockSession2);
        assertTrue(testLobby.getConnectionGroup().isPlayerConnected("player1"));
        assertEquals(1, testLobby.getConnectionGroup().getConnectionCount());
        assertSame(mockSession2, testLobby.getConnectionGroup().getPlayerConnection("player1").getWebSocketSession());
    }
}
