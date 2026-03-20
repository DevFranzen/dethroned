package com.toni.dethroned.backend.lobby.dto;

import com.toni.dethroned.backend.lobby.domain.GameSettings;

public class GameSettingsResponse {
    private int minPlayers;
    private int maxPlayers;
    private String gameMode;
    private long timeLimit;
    private float fieldWidth;
    private float fieldHeight;

    public GameSettingsResponse(GameSettings settings) {
        this.minPlayers = settings.getMinPlayers();
        this.maxPlayers = settings.getMaxPlayers();
        this.gameMode = settings.getGameMode();
        this.timeLimit = settings.getTimeLimit();
        this.fieldWidth = settings.getFieldWidth();
        this.fieldHeight = settings.getFieldHeight();
    }

    // Getters
    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public String getGameMode() {
        return gameMode;
    }

    public long getTimeLimit() {
        return timeLimit;
    }

    public float getFieldWidth() {
        return fieldWidth;
    }

    public float getFieldHeight() {
        return fieldHeight;
    }
}
