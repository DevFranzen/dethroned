package com.toni.dethroned.backend.lobby.dto;

import com.toni.dethroned.backend.lobby.domain.PlayerRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AddPlayerRequest {
    @NotBlank(message = "Player ID is required")
    private String playerId;

    @NotBlank(message = "Username is required")
    private String username;

    @NotNull(message = "Role is required")
    private PlayerRole role;

    public AddPlayerRequest() {
    }

    public AddPlayerRequest(String playerId, String username, PlayerRole role) {
        this.playerId = playerId;
        this.username = username;
        this.role = role;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public PlayerRole getRole() {
        return role;
    }

    public void setRole(PlayerRole role) {
        this.role = role;
    }
}
