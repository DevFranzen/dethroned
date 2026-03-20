package com.toni.dethroned.backend.websocket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Generic WebSocket message for communication between client and server
 */
public class WebSocketMessage {
    @JsonProperty("type")
    private String type;

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("playerId")
    private String playerId;

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("payload")
    private Object payload;

    public WebSocketMessage() {
    }

    public WebSocketMessage(String type, String playerId, String sessionId, Object payload) {
        this.type = type;
        this.playerId = playerId;
        this.sessionId = sessionId;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }
}
