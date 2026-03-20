package com.toni.dethroned.backend.websocket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response beim WebSocket-Verbindungsaufbau
 */
public class ConnectionResponse {
    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("playerId")
    private String playerId;

    @JsonProperty("sessionId")
    private String sessionId;

    public ConnectionResponse() {
    }

    public ConnectionResponse(String status, String message, String playerId, String sessionId) {
        this.status = status;
        this.message = message;
        this.playerId = playerId;
        this.sessionId = sessionId;
    }

    public static ConnectionResponse connected(String playerId, String sessionId) {
        return new ConnectionResponse("CONNECTED", "Connected successfully", playerId, sessionId);
    }

    public static ConnectionResponse authenticationFailed(String message) {
        return new ConnectionResponse("AUTHENTICATION_FAILED", message, null, null);
    }

    public static ConnectionResponse sessionInvalid(String message) {
        return new ConnectionResponse("SESSION_INVALID", message, null, null);
    }

    public static ConnectionResponse unauthorized(String message) {
        return new ConnectionResponse("UNAUTHORIZED", message, null, null);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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
}
