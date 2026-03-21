package com.toni.dethroned.backend.websocket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlayerLeftEvent {
    @JsonProperty("type")
    private final String type = "PLAYER_LEFT";

    @JsonProperty("playerId")
    private String playerId;

    @JsonProperty("lobbyId")
    private String lobbyId;

    @JsonProperty("remainingPlayers")
    private Object remainingPlayers;

    @JsonProperty("timestamp")
    private long timestamp;

    public PlayerLeftEvent(String playerId, String lobbyId, Object remainingPlayers) {
        this.playerId = playerId;
        this.lobbyId = lobbyId;
        this.remainingPlayers = remainingPlayers;
        this.timestamp = System.currentTimeMillis();
    }

    public String getType() {
        return type;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getLobbyId() {
        return lobbyId;
    }

    public Object getRemainingPlayers() {
        return remainingPlayers;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
