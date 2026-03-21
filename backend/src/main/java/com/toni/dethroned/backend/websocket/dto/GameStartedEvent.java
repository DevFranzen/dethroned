package com.toni.dethroned.backend.websocket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.toni.dethroned.backend.lobby.domain.GameSettings;
import com.toni.dethroned.backend.lobby.dto.PlayerResponse;

import java.util.List;

/**
 * Event sent when a game starts in a lobby
 */
public class GameStartedEvent {
    @JsonProperty("type")
    private final String type = "GAME_STARTED";

    @JsonProperty("gameId")
    private String gameId;

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("gameSettings")
    private GameSettings gameSettings;

    @JsonProperty("players")
    private List<PlayerResponse> players;

    public GameStartedEvent() {
    }

    public GameStartedEvent(String gameId, long timestamp, GameSettings gameSettings, List<PlayerResponse> players) {
        this.gameId = gameId;
        this.timestamp = timestamp;
        this.gameSettings = gameSettings;
        this.players = players;
    }

    public String getType() {
        return type;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public GameSettings getGameSettings() {
        return gameSettings;
    }

    public void setGameSettings(GameSettings gameSettings) {
        this.gameSettings = gameSettings;
    }

    public List<PlayerResponse> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerResponse> players) {
        this.players = players;
    }
}
