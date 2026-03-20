package com.toni.dethroned.backend.lobby.domain;

import java.time.Instant;
import java.time.LocalDateTime;

public class Player {
    private String id;
    private String username;
    private PlayerRole role;
    private PlayerStatus status;
    private String sessionId;
    private Instant joinedAt;
    private boolean isConnected;
    private LocalDateTime lastHeartbeat;

    public Player(String id, String username, PlayerRole role) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.status = PlayerStatus.CONNECTED;
        this.joinedAt = Instant.now();
        this.isConnected = false;
        this.lastHeartbeat = LocalDateTime.now();
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

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public LocalDateTime getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(LocalDateTime lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }
}
