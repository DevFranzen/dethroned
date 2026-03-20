package com.toni.dethroned.backend.lobby.dto;

import com.toni.dethroned.backend.lobby.domain.Player;
import com.toni.dethroned.backend.lobby.domain.PlayerRole;
import com.toni.dethroned.backend.lobby.domain.PlayerStatus;
import java.time.Instant;

public class PlayerResponse {
    private String id;
    private String username;
    private PlayerRole role;
    private PlayerStatus status;
    private Instant joinedAt;

    public PlayerResponse(Player player) {
        this.id = player.getId();
        this.username = player.getUsername();
        this.role = player.getRole();
        this.status = player.getStatus();
        this.joinedAt = player.getJoinedAt();
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

    public Instant getJoinedAt() {
        return joinedAt;
    }
}
