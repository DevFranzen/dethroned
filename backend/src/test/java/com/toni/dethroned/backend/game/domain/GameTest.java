package com.toni.dethroned.backend.game.domain;

import com.toni.dethroned.backend.lobby.domain.GameSettings;
import com.toni.dethroned.backend.lobby.domain.Player;
import com.toni.dethroned.backend.lobby.domain.PlayerRole;
import com.toni.dethroned.backend.websocket.domain.ConnectionGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GameTest {
    private Game game;
    private ConnectionGroup connectionGroup;
    private GameSettings gameSettings;
    private Map<String, Player> players;

    @BeforeEach
    void setUp() {
        connectionGroup = new ConnectionGroup("lobby-123");
        gameSettings = new GameSettings(2, 4, "elimination", 0, 800, 600);

        players = new HashMap<>();
        players.put("player1", new Player("player1", "Player1", PlayerRole.PLAYER));
        players.put("player2", new Player("player2", "Player2", PlayerRole.PLAYER));

        game = new Game(connectionGroup, gameSettings, players);
    }

    @Test
    void testGameInitializationWithValidData() {
        assertNotNull(game.getGameId());
        assertEquals(connectionGroup, game.getConnectionGroup());
        assertEquals(gameSettings, game.getGameSettings());
        assertEquals(2, game.getPlayers().size());
    }

    @Test
    void testGameStatusIsWaitingOnCreation() {
        assertEquals(GameStatus.WAITING, game.getStatus());
    }

    @Test
    void testGameHasUniqueGameId() {
        Game game2 = new Game(connectionGroup, gameSettings, players);
        assertNotEquals(game.getGameId(), game2.getGameId());
    }

    @Test
    void testGameStartTransitionsToRunning() {
        game.start();
        assertEquals(GameStatus.RUNNING, game.getStatus());
        assertNotNull(game.getStartedAt());
    }

    @Test
    void testGamePauseTransitionsToPaused() {
        game.start();
        game.pause();
        assertEquals(GameStatus.PAUSED, game.getStatus());
    }

    @Test
    void testGameFinishTransitionsToFinished() {
        game.finish();
        assertEquals(GameStatus.FINISHED, game.getStatus());
    }

    @Test
    void testGameCreatedAtIsSet() {
        assertNotNull(game.getCreatedAt());
    }

    @Test
    void testGameStartedAtIsNullInitially() {
        assertNull(game.getStartedAt());
    }

    @Test
    void testGameStartedAtSetWhenStarted() {
        assertNull(game.getStartedAt());
        game.start();
        assertNotNull(game.getStartedAt());
    }

    @Test
    void testGameCannotTransitionFromRunningToWaiting() {
        game.start();
        assertEquals(GameStatus.RUNNING, game.getStatus());
        // Trying to call start again on RUNNING game should not change status
        game.start();
        assertEquals(GameStatus.RUNNING, game.getStatus());
    }

    @Test
    void testGameStatusCanBeSetManually() {
        game.setStatus(GameStatus.FINISHED);
        assertEquals(GameStatus.FINISHED, game.getStatus());
    }
}
