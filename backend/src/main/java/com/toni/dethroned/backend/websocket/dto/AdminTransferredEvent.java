package com.toni.dethroned.backend.websocket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AdminTransferredEvent {
    @JsonProperty("type")
    private final String type = "ADMIN_TRANSFERRED";

    @JsonProperty("oldAdminId")
    private String oldAdminId;

    @JsonProperty("newAdminId")
    private String newAdminId;

    @JsonProperty("lobbyId")
    private String lobbyId;

    @JsonProperty("timestamp")
    private long timestamp;

    public AdminTransferredEvent(String oldAdminId, String newAdminId, String lobbyId) {
        this.oldAdminId = oldAdminId;
        this.newAdminId = newAdminId;
        this.lobbyId = lobbyId;
        this.timestamp = System.currentTimeMillis();
    }

    public String getType() {
        return type;
    }

    public String getOldAdminId() {
        return oldAdminId;
    }

    public String getNewAdminId() {
        return newAdminId;
    }

    public String getLobbyId() {
        return lobbyId;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
