package com.toni.dethroned.backend.websocket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ErrorEvent {
    @JsonProperty("type")
    private final String type = "ERROR";

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("timestamp")
    private long timestamp;

    public ErrorEvent(String status, String message) {
        this.status = status;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
