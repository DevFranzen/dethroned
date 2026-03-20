package com.toni.dethroned.backend.lobby.dto;

import com.toni.dethroned.backend.lobby.domain.GameSettings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public class CreateLobbyRequest {
    @NotBlank(message = "Host ID is required")
    private String hostId;

    @Valid
    private GameSettings gameSettings;

    public CreateLobbyRequest() {
    }

    public CreateLobbyRequest(String hostId, GameSettings gameSettings) {
        this.hostId = hostId;
        this.gameSettings = gameSettings;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public GameSettings getGameSettings() {
        return gameSettings;
    }

    public void setGameSettings(GameSettings gameSettings) {
        this.gameSettings = gameSettings;
    }
}
