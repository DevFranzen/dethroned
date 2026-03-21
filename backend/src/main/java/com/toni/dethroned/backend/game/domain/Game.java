package com.toni.dethroned.backend.game.domain;

import com.toni.dethroned.backend.lobby.domain.GameSettings;
import com.toni.dethroned.backend.lobby.domain.Player;
import com.toni.dethroned.backend.websocket.domain.ConnectionGroup;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Game domain class representing a game session
 */
public class Game {
    private final String gameId;
    private final ConnectionGroup connectionGroup;
    private final GameSettings gameSettings;
    private final Map<String, Player> players;
    private GameStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime startedAt;

    public Game(ConnectionGroup connectionGroup, GameSettings gameSettings, Map<String, Player> players) {
        this.gameId = UUID.randomUUID().toString();
        this.connectionGroup = connectionGroup;
        this.gameSettings = gameSettings;
        this.players = players;
        this.status = GameStatus.WAITING;
        this.createdAt = LocalDateTime.now();
        this.startedAt = null;
    }

    /**
     * Transition game to RUNNING state
     */
    public void start() {
        if (this.status == GameStatus.WAITING) {
            this.status = GameStatus.RUNNING;
            this.startedAt = LocalDateTime.now();
        }
    }

    /**
     * Transition game to PAUSED state
     */
    public void pause() {
        if (this.status == GameStatus.RUNNING) {
            this.status = GameStatus.PAUSED;
        }
    }

    /**
     * Transition game to FINISHED state
     */
    public void finish() {
        this.status = GameStatus.FINISHED;
    }

    public String getGameId() {
        return gameId;
    }

    public ConnectionGroup getConnectionGroup() {
        return connectionGroup;
    }

    public GameSettings getGameSettings() {
        return gameSettings;
    }

    public Map<String, Player> getPlayers() {
        return players;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }
}
