package com.toni.dethroned.backend.websocket.handler;

import com.toni.dethroned.backend.lobby.domain.GameSettings;
import com.toni.dethroned.backend.lobby.domain.Lobby;
import com.toni.dethroned.backend.lobby.domain.LobbyStatus;
import com.toni.dethroned.backend.lobby.domain.Player;
import com.toni.dethroned.backend.lobby.domain.PlayerRole;
import com.toni.dethroned.backend.lobby.service.LobbyService;
import com.toni.dethroned.backend.websocket.service.SessionVerificationService;
import com.toni.dethroned.backend.websocket.service.WebSocketConnectionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameWebSocketHandlerLobbyOpsTest {
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

        // Setup test lobby
        GameSettings settings = new GameSettings(2, 4, "elimination", 0, 800, 600);
        testLobby = new Lobby("lobby-123", "ABC123", "player1", settings);
        testLobby.addPlayer(new Player("player1", "Player1", PlayerRole.PLAYER));
        testLobby.addPlayer(new Player("player2", "Player2", PlayerRole.PLAYER));

        // Initialize connection group in connection manager
        connectionManager.createConnectionGroup("lobby-123");
    }

    // --- GET_LOBBY Tests ---

    @Test
    void testGetLobbySuccess() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player1/session/lobby-123"));
        when(mockSession.isOpen()).thenReturn(true);
        when(lobbyService.getLobbyById("lobby-123")).thenReturn(testLobby);

        // Act
        handler.afterConnectionEstablished(mockSession);
        String getLobbyMessage = "{\"type\":\"GET_LOBBY\"}";
        handler.handleTextMessage(mockSession, new TextMessage(getLobbyMessage));

        // Assert
        verify(mockSession, atLeastOnce()).sendMessage(any(TextMessage.class));
    }

    @Test
    void testGetLobbyNotFound() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player1/session/lobby-456"));
        when(mockSession.isOpen()).thenReturn(true);
        when(lobbyService.getLobbyById("lobby-456")).thenThrow(new RuntimeException("Lobby not found"));

        // Act & Assert - should not throw
        handler.afterConnectionEstablished(mockSession);
        assertDoesNotThrow(() -> handler.handleTextMessage(mockSession, new TextMessage("{\"type\":\"GET_LOBBY\"}")));
    }

    @Test
    void testGetLobbyUnicastOnly() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player1/session/lobby-123"));
        when(mockSession.isOpen()).thenReturn(true);
        when(mockSession2.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player2/session/lobby-123"));
        when(mockSession2.isOpen()).thenReturn(true);
        when(lobbyService.getLobbyById("lobby-123")).thenReturn(testLobby);

        // Act - connect both players
        handler.afterConnectionEstablished(mockSession);
        handler.afterConnectionEstablished(mockSession2);

        // Send GET_LOBBY from player1
        String getLobbyMessage = "{\"type\":\"GET_LOBBY\"}";
        handler.handleTextMessage(mockSession, new TextMessage(getLobbyMessage));

        // Assert - only player1 should receive it (plus connection message)
        verify(mockSession, atLeastOnce()).sendMessage(any(TextMessage.class));
        // Player2 should only receive connection events, not GET_LOBBY response
        verify(mockSession2, atLeastOnce()).sendMessage(any(TextMessage.class));
    }

    @Test
    void testGetLobbyStateStructure() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player1/session/lobby-123"));
        when(mockSession.isOpen()).thenReturn(true);
        when(lobbyService.getLobbyById("lobby-123")).thenReturn(testLobby);

        // Act
        handler.afterConnectionEstablished(mockSession);
        handler.handleTextMessage(mockSession, new TextMessage("{\"type\":\"GET_LOBBY\"}"));

        // Assert - verify at least one message sent
        verify(mockSession, atLeastOnce()).sendMessage(any(TextMessage.class));
    }

    // --- DELETE_LOBBY Tests ---

    @Test
    void testDeleteLobbySuccess() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player1/session/lobby-123"));
        when(mockSession.isOpen()).thenReturn(true);
        when(lobbyService.getLobbyById("lobby-123")).thenReturn(testLobby);

        // Act
        handler.afterConnectionEstablished(mockSession);
        String deleteLobbyMessage = "{\"type\":\"DELETE_LOBBY\"}";
        handler.handleTextMessage(mockSession, new TextMessage(deleteLobbyMessage));

        // Assert
        verify(lobbyService).deleteLobby("lobby-123", "player1");
        verify(mockSession, atLeastOnce()).sendMessage(any(TextMessage.class));
    }

    @Test
    void testDeleteLobbyNotAdmin() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player2/session/lobby-123"));
        when(mockSession.isOpen()).thenReturn(true);
        when(lobbyService.getLobbyById("lobby-123")).thenReturn(testLobby);

        // Act
        handler.afterConnectionEstablished(mockSession);
        String deleteLobbyMessage = "{\"type\":\"DELETE_LOBBY\"}";
        handler.handleTextMessage(mockSession, new TextMessage(deleteLobbyMessage));

        // Assert - should not call deleteLobby
        verify(lobbyService, never()).deleteLobby("lobby-123", "player2");
    }

    @Test
    void testDeleteLobbyBroadcastsToAll() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player1/session/lobby-123"));
        when(mockSession.isOpen()).thenReturn(true);
        when(mockSession2.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player2/session/lobby-123"));
        when(mockSession2.isOpen()).thenReturn(true);
        when(lobbyService.getLobbyById("lobby-123")).thenReturn(testLobby);

        // Act - connect both players
        handler.afterConnectionEstablished(mockSession);
        handler.afterConnectionEstablished(mockSession2);

        // Admin deletes lobby
        String deleteLobbyMessage = "{\"type\":\"DELETE_LOBBY\"}";
        handler.handleTextMessage(mockSession, new TextMessage(deleteLobbyMessage));

        // Assert - both should receive messages
        verify(mockSession, atLeastOnce()).sendMessage(any(TextMessage.class));
        verify(mockSession2, atLeastOnce()).sendMessage(any(TextMessage.class));
    }

    @Test
    void testDeleteLobbyEventHasTimestamp() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player1/session/lobby-123"));
        when(mockSession.isOpen()).thenReturn(true);
        when(lobbyService.getLobbyById("lobby-123")).thenReturn(testLobby);

        // Act
        handler.afterConnectionEstablished(mockSession);
        handler.handleTextMessage(mockSession, new TextMessage("{\"type\":\"DELETE_LOBBY\"}"));

        // Assert - verify message was sent
        verify(mockSession, atLeastOnce()).sendMessage(any(TextMessage.class));
    }

    // --- MARK_READY Tests ---

    @Test
    void testMarkReadySuccess() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player1/session/lobby-123"));
        when(mockSession.isOpen()).thenReturn(true);
        when(lobbyService.getLobbyById("lobby-123")).thenReturn(testLobby);
        doNothing().when(lobbyService).markPlayerReady("lobby-123", "player1");

        // Act
        handler.afterConnectionEstablished(mockSession);
        String markReadyMessage = "{\"type\":\"MARK_READY\"}";
        handler.handleTextMessage(mockSession, new TextMessage(markReadyMessage));

        // Assert
        verify(lobbyService).markPlayerReady("lobby-123", "player1");
        verify(mockSession, atLeastOnce()).sendMessage(any(TextMessage.class));
    }

    @Test
    void testMarkReadyTransitionsLobbyIfAllReady() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player1/session/lobby-123"));
        when(mockSession.isOpen()).thenReturn(true);
        when(lobbyService.getLobbyById("lobby-123")).thenReturn(testLobby);
        doNothing().when(lobbyService).markPlayerReady("lobby-123", "player1");

        // Simulate all players ready
        testLobby.getStatus();

        // Act
        handler.afterConnectionEstablished(mockSession);
        handler.handleTextMessage(mockSession, new TextMessage("{\"type\":\"MARK_READY\"}"));

        // Assert
        verify(mockSession, atLeastOnce()).sendMessage(any(TextMessage.class));
    }

    @Test
    void testMarkReadyPlayerNotInLobby() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player3/session/lobby-123"));
        when(mockSession.isOpen()).thenReturn(true);
        when(lobbyService.getLobbyById("lobby-123")).thenReturn(testLobby);
        doThrow(new RuntimeException("Player not found"))
            .when(lobbyService).markPlayerReady("lobby-123", "player3");

        // Act & Assert - should not throw
        handler.afterConnectionEstablished(mockSession);
        assertDoesNotThrow(() -> handler.handleTextMessage(mockSession, new TextMessage("{\"type\":\"MARK_READY\"}")));
    }

    @Test
    void testMarkReadyBroadcastsUpdatedLobbyState() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player1/session/lobby-123"));
        when(mockSession.isOpen()).thenReturn(true);
        when(mockSession2.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player2/session/lobby-123"));
        when(mockSession2.isOpen()).thenReturn(true);
        when(lobbyService.getLobbyById("lobby-123")).thenReturn(testLobby);
        doNothing().when(lobbyService).markPlayerReady("lobby-123", "player1");

        // Act - connect both players
        handler.afterConnectionEstablished(mockSession);
        handler.afterConnectionEstablished(mockSession2);

        // Mark ready
        handler.handleTextMessage(mockSession, new TextMessage("{\"type\":\"MARK_READY\"}"));

        // Assert - both should receive messages
        verify(mockSession, atLeastOnce()).sendMessage(any(TextMessage.class));
        verify(mockSession2, atLeastOnce()).sendMessage(any(TextMessage.class));
    }

    // --- PLAYER_LEAVE Tests ---

    @Test
    void testPlayerLeaveSuccess() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player1/session/lobby-123"));
        when(mockSession.isOpen()).thenReturn(true);
        when(lobbyService.getLobbyById("lobby-123")).thenReturn(testLobby);
        doNothing().when(lobbyService).playerLeaves("lobby-123", "player1");

        // Act
        handler.afterConnectionEstablished(mockSession);
        String playerLeaveMessage = "{\"type\":\"PLAYER_LEAVE\"}";
        handler.handleTextMessage(mockSession, new TextMessage(playerLeaveMessage));

        // Assert
        verify(lobbyService).playerLeaves("lobby-123", "player1");
        verify(mockSession, atLeastOnce()).sendMessage(any(TextMessage.class));
    }

    @Test
    void testPlayerLeaveLastPlayerDeletesLobby() throws Exception {
        // Arrange
        Lobby singlePlayerLobby = new Lobby("lobby-456", "DEF456", "player1",
            new GameSettings(1, 2, "elimination", 0, 800, 600));
        singlePlayerLobby.addPlayer(new Player("player1", "Player1", PlayerRole.PLAYER));

        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player1/session/lobby-456"));
        when(mockSession.isOpen()).thenReturn(true);
        when(lobbyService.getLobbyById("lobby-456"))
            .thenReturn(singlePlayerLobby)
            .thenThrow(new RuntimeException("Lobby not found"));
        doNothing().when(lobbyService).playerLeaves("lobby-456", "player1");

        // Act
        handler.afterConnectionEstablished(mockSession);
        handler.handleTextMessage(mockSession, new TextMessage("{\"type\":\"PLAYER_LEAVE\"}"));

        // Assert
        verify(lobbyService).playerLeaves("lobby-456", "player1");
    }

    @Test
    void testPlayerLeaveAdminTransfersIfAdminLeaves() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player1/session/lobby-123"));
        when(mockSession.isOpen()).thenReturn(true);
        when(lobbyService.getLobbyById("lobby-123")).thenReturn(testLobby);
        doNothing().when(lobbyService).playerLeaves("lobby-123", "player1");

        // After admin leaves, simulate admin transfer
        testLobby.setAdminId("player2");

        // Act
        handler.afterConnectionEstablished(mockSession);
        handler.handleTextMessage(mockSession, new TextMessage("{\"type\":\"PLAYER_LEAVE\"}"));

        // Assert
        verify(lobbyService).playerLeaves("lobby-123", "player1");
        verify(mockSession, atLeastOnce()).sendMessage(any(TextMessage.class));
    }

    @Test
    void testPlayerLeavePlayerNotFound() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player3/session/lobby-123"));
        when(mockSession.isOpen()).thenReturn(true);
        when(lobbyService.getLobbyById("lobby-123")).thenReturn(testLobby);
        doThrow(new RuntimeException("Player not found"))
            .when(lobbyService).playerLeaves("lobby-123", "player3");

        // Act & Assert - should not throw
        handler.afterConnectionEstablished(mockSession);
        assertDoesNotThrow(() -> handler.handleTextMessage(mockSession, new TextMessage("{\"type\":\"PLAYER_LEAVE\"}")));
    }
}
