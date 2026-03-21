package com.toni.dethroned.backend.websocket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event when a player disconnects from a group (lobby, game)
 */
public class PlayerDisconnectedEvent {
    @JsonProperty("type")
    private final String type = "PLAYER_DISCONNECTED";

    @JsonProperty("playerId")
    private String playerId;

    @JsonProperty("timestamp")
    private long timestamp;

    public PlayerDisconnectedEvent() {
    }

    public PlayerDisconnectedEvent(String playerId, long timestamp) {
        this.playerId = playerId;
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
