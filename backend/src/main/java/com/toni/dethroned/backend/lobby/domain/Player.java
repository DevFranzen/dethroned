package com.toni.dethroned.backend.lobby.domain;

import java.time.Instant;

public class Player {
    private String id;
    private String username;
    private PlayerRole role;
    private PlayerStatus status;
    private String sessionId;
    private Instant joinedAt;

    public Player(String id, String username, PlayerRole role) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.status = PlayerStatus.CONNECTED;
        this.joinedAt = Instant.now();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public PlayerRole getRole() {
        return role;
    }

    public PlayerStatus getStatus() {
        return status;
    }

    public void setStatus(PlayerStatus status) {
        this.status = status;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }
}
