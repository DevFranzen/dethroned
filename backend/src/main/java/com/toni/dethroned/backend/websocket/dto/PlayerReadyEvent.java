package com.toni.dethroned.backend.websocket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlayerReadyEvent {
    @JsonProperty("type")
    private final String type = "PLAYER_READY";

    @JsonProperty("playerId")
    private String playerId;

    @JsonProperty("lobbyStatus")
    private Object lobbyStatus;

    @JsonProperty("timestamp")
    private long timestamp;

    public PlayerReadyEvent(String playerId, Object lobbyStatus) {
        this.playerId = playerId;
        this.lobbyStatus = lobbyStatus;
        this.timestamp = System.currentTimeMillis();
    }

    public String getType() {
        return type;
    }

    public String getPlayerId() {
        return playerId;
    }

    public Object getLobbyStatus() {
        return lobbyStatus;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
