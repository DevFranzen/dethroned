package com.toni.dethroned.backend.auth.dto;

public class AuthResponse {
    private String playerId;

    public AuthResponse(String playerId) {
        this.playerId = playerId;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }
}
