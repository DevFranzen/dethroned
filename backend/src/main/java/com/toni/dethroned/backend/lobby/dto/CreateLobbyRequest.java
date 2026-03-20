package com.toni.dethroned.backend.lobby.dto;

import com.toni.dethroned.backend.lobby.domain.GameSettings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public class CreateLobbyRequest {
    @NotBlank(message = "Player ID is required")
    private String playerId;

    @Valid
    private GameSettings gameSettings;

    public CreateLobbyRequest() {
    }

    public CreateLobbyRequest(String playerId, GameSettings gameSettings) {
        this.playerId = playerId;
        this.gameSettings = gameSettings;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public GameSettings getGameSettings() {
        return gameSettings;
    }

    public void setGameSettings(GameSettings gameSettings) {
        this.gameSettings = gameSettings;
    }
}
