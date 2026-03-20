package com.toni.dethroned.backend.lobby.domain;

public class GameSettings {
    private int minPlayers;
    private int maxPlayers;
    private String gameMode;
    private long timeLimit;
    private float fieldWidth;
    private float fieldHeight;

    public GameSettings() {
    }

    public GameSettings(int minPlayers, int maxPlayers, String gameMode, long timeLimit, float fieldWidth, float fieldHeight) {
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.gameMode = gameMode;
        this.timeLimit = timeLimit;
        this.fieldWidth = fieldWidth;
        this.fieldHeight = fieldHeight;
    }

    // Getters and Setters
    public int getMinPlayers() {
        return minPlayers;
    }

    public void setMinPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public String getGameMode() {
        return gameMode;
    }

    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
    }

    public long getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(long timeLimit) {
        this.timeLimit = timeLimit;
    }

    public float getFieldWidth() {
        return fieldWidth;
    }

    public void setFieldWidth(float fieldWidth) {
        this.fieldWidth = fieldWidth;
    }

    public float getFieldHeight() {
        return fieldHeight;
    }

    public void setFieldHeight(float fieldHeight) {
        this.fieldHeight = fieldHeight;
    }
}
