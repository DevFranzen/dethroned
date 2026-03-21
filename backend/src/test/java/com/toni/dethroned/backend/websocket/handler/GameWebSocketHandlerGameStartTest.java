package com.toni.dethroned.backend.websocket.handler;

import com.toni.dethroned.backend.game.domain.Game;
import com.toni.dethroned.backend.lobby.domain.GameSettings;
import com.toni.dethroned.backend.lobby.domain.Lobby;
import com.toni.dethroned.backend.lobby.domain.Player;
import com.toni.dethroned.backend.lobby.domain.PlayerRole;
import com.toni.dethroned.backend.lobby.domain.LobbyStatus;
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

class GameWebSocketHandlerGameStartTest {
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

        // Mark all players as ready
        testLobby.getPlayers().get("player1").setStatus(com.toni.dethroned.backend.lobby.domain.PlayerStatus.READY);
        testLobby.getPlayers().get("player2").setStatus(com.toni.dethroned.backend.lobby.domain.PlayerStatus.READY);

        // Initialize connection group in connection manager
        connectionManager.createConnectionGroup("lobby-123");
    }

    @Test
    void testStartGameWithValidAdmin() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player1/session/lobby-123"));
        when(mockSession.isOpen()).thenReturn(true);
        when(lobbyService.getLobbyById("lobby-123")).thenReturn(testLobby);

        // Act - establish connection first
        handler.afterConnectionEstablished(mockSession);

        // Now send START_GAME message
        String startGameMessage = "{\"type\":\"START_GAME\"}";
        handler.handleTextMessage(mockSession, new TextMessage(startGameMessage));

        // Assert
        verify(mockSession, atLeastOnce()).sendMessage(any(TextMessage.class));
        assertTrue(testLobby.getStatus() == LobbyStatus.IN_PROGRESS);
        assertNotNull(testLobby.getGame());
        assertTrue(testLobby.getGame() instanceof Game);
    }

    @Test
    void testStartGameWithInvalidAdminFails() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player2/session/lobby-123"));
        when(mockSession.isOpen()).thenReturn(true);
        when(lobbyService.getLobbyById("lobby-123")).thenReturn(testLobby);

        // Act - establish connection first
        handler.afterConnectionEstablished(mockSession);

        // Now send START_GAME message from non-admin player
        String startGameMessage = "{\"type\":\"START_GAME\"}";
        handler.handleTextMessage(mockSession, new TextMessage(startGameMessage));

        // Assert - lobby should not be started (game not created)
        assertNull(testLobby.getGame());
    }

    @Test
    void testStartGameWhenLobbyNotReadyFails() throws Exception {
        // Arrange
        Lobby lobbyNotReady = new Lobby("lobby-456", "DEF456", "player1", new GameSettings(2, 4, "elimination", 0, 800, 600));
        lobbyNotReady.addPlayer(new Player("player1", "Player1", PlayerRole.PLAYER));
        // Don't add second player, so canStart() returns false

        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player1/session/lobby-456"));
        when(mockSession.isOpen()).thenReturn(true);
        when(lobbyService.getLobbyById("lobby-456")).thenReturn(lobbyNotReady);

        // Act
        handler.afterConnectionEstablished(mockSession);

        String startGameMessage = "{\"type\":\"START_GAME\"}";
        handler.handleTextMessage(mockSession, new TextMessage(startGameMessage));

        // Assert
        assertEquals(LobbyStatus.OPEN, lobbyNotReady.getStatus());
        assertNull(lobbyNotReady.getGame());
    }

    @Test
    void testGameStartedEventBroadcasted() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player1/session/lobby-123"));
        when(mockSession.isOpen()).thenReturn(true);
        when(mockSession2.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player2/session/lobby-123"));
        when(mockSession2.isOpen()).thenReturn(true);
        when(lobbyService.getLobbyById("lobby-123")).thenReturn(testLobby);

        // Act - connect both players
        handler.afterConnectionEstablished(mockSession);
        handler.afterConnectionEstablished(mockSession2);

        // Send START_GAME
        String startGameMessage = "{\"type\":\"START_GAME\"}";
        handler.handleTextMessage(mockSession, new TextMessage(startGameMessage));

        // Assert - both should receive messages (connection + broadcast)
        verify(mockSession, atLeastOnce()).sendMessage(any(TextMessage.class));
        verify(mockSession2, atLeastOnce()).sendMessage(any(TextMessage.class));
    }

    @Test
    void testGameObjectCreatedWithCorrectData() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player1/session/lobby-123"));
        when(mockSession.isOpen()).thenReturn(true);
        when(lobbyService.getLobbyById("lobby-123")).thenReturn(testLobby);

        // Act
        handler.afterConnectionEstablished(mockSession);
        String startGameMessage = "{\"type\":\"START_GAME\"}";
        handler.handleTextMessage(mockSession, new TextMessage(startGameMessage));

        // Assert
        Game game = (Game) testLobby.getGame();
        assertNotNull(game.getGameId());
        assertEquals(testLobby.getSettings(), game.getGameSettings());
        assertEquals(2, game.getPlayers().size());
    }

    @Test
    void testGameStatusIsWaiting() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player1/session/lobby-123"));
        when(mockSession.isOpen()).thenReturn(true);
        when(lobbyService.getLobbyById("lobby-123")).thenReturn(testLobby);

        // Act
        handler.afterConnectionEstablished(mockSession);
        String startGameMessage = "{\"type\":\"START_GAME\"}";
        handler.handleTextMessage(mockSession, new TextMessage(startGameMessage));

        // Assert
        Game game = (Game) testLobby.getGame();
        assertEquals(com.toni.dethroned.backend.game.domain.GameStatus.WAITING, game.getStatus());
    }

    @Test
    void testLobbyStatusTransitionsToInProgress() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player1/session/lobby-123"));
        when(mockSession.isOpen()).thenReturn(true);
        when(lobbyService.getLobbyById("lobby-123")).thenReturn(testLobby);

        // Act
        handler.afterConnectionEstablished(mockSession);
        String startGameMessage = "{\"type\":\"START_GAME\"}";
        handler.handleTextMessage(mockSession, new TextMessage(startGameMessage));

        // Assert
        assertEquals(LobbyStatus.IN_PROGRESS, testLobby.getStatus());
    }

    @Test
    void testGameHasConnectionGroupReference() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player1/session/lobby-123"));
        when(mockSession.isOpen()).thenReturn(true);
        when(lobbyService.getLobbyById("lobby-123")).thenReturn(testLobby);

        // Act
        handler.afterConnectionEstablished(mockSession);
        String startGameMessage = "{\"type\":\"START_GAME\"}";
        handler.handleTextMessage(mockSession, new TextMessage(startGameMessage));

        // Assert
        Game game = (Game) testLobby.getGame();
        assertNotNull(game.getConnectionGroup());
        assertEquals(testLobby.getConnectionGroup(), game.getConnectionGroup());
    }

    @Test
    void testInvalidMessageFormatHandledGracefully() throws Exception {
        // Arrange
        when(mockSession.getUri()).thenReturn(new URI("ws://localhost:8080/ws/player/player1/session/lobby-123"));
        when(mockSession.isOpen()).thenReturn(true);

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> handler.handleTextMessage(mockSession, new TextMessage("invalid json")));
    }
}
