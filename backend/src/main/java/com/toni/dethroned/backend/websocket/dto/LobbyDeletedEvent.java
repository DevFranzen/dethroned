package com.toni.dethroned.backend.websocket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LobbyDeletedEvent {
    @JsonProperty("type")
    private final String type = "LOBBY_DELETED";

    @JsonProperty("lobbyId")
    private String lobbyId;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("timestamp")
    private long timestamp;

    public LobbyDeletedEvent(String lobbyId, String reason) {
        this.lobbyId = lobbyId;
        this.reason = reason;
        this.timestamp = System.currentTimeMillis();
    }

    public String getType() {
        return type;
    }

    public String getLobbyId() {
        return lobbyId;
    }

    public String getReason() {
        return reason;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
