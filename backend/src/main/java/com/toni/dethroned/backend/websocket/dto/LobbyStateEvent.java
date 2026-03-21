package com.toni.dethroned.backend.websocket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LobbyStateEvent {
    @JsonProperty("type")
    private final String type = "LOBBY_STATE";

    @JsonProperty("lobbyId")
    private String lobbyId;

    @JsonProperty("code")
    private String code;

    @JsonProperty("status")
    private Object status;

    @JsonProperty("adminId")
    private String adminId;

    @JsonProperty("players")
    private Object players;

    @JsonProperty("settings")
    private Object settings;

    @JsonProperty("timestamp")
    private long timestamp;

    public LobbyStateEvent(String lobbyId, String code, Object status, String adminId,
                          Object players, Object settings) {
        this.lobbyId = lobbyId;
        this.code = code;
        this.status = status;
        this.adminId = adminId;
        this.players = players;
        this.settings = settings;
        this.timestamp = System.currentTimeMillis();
    }

    public String getType() {
        return type;
    }

    public String getLobbyId() {
        return lobbyId;
    }

    public String getCode() {
        return code;
    }

    public Object getStatus() {
        return status;
    }

    public String getAdminId() {
        return adminId;
    }

    public Object getPlayers() {
        return players;
    }

    public Object getSettings() {
        return settings;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
