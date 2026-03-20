package com.toni.dethroned.backend.lobby.dto;

import com.toni.dethroned.backend.lobby.domain.Lobby;
import com.toni.dethroned.backend.lobby.domain.LobbyStatus;
import com.toni.dethroned.backend.lobby.domain.Player;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class LobbyResponse {
    private String id;
    private String code;
    private String adminId;
    private String displayClientId;
    private LobbyStatus status;
    private List<PlayerResponse> players;
    private GameSettingsResponse settings;
    private Instant createdAt;
    private Instant lastActivity;

    public LobbyResponse(Lobby lobby) {
        this.id = lobby.getId();
        this.code = lobby.getCode();
        this.adminId = lobby.getAdminId();
        this.displayClientId = lobby.getDisplayClientId();
        this.status = lobby.getStatus();
        this.players = lobby.getPlayers().values().stream()
                .map(PlayerResponse::new)
                .toList();
        this.settings = new GameSettingsResponse(lobby.getSettings());
        this.createdAt = lobby.getCreatedAt();
        this.lastActivity = lobby.getLastActivity();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getAdminId() {
        return adminId;
    }

    public String getDisplayClientId() {
        return displayClientId;
    }

    public LobbyStatus getStatus() {
        return status;
    }

    public List<PlayerResponse> getPlayers() {
        return players;
    }

    public GameSettingsResponse getSettings() {
        return settings;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastActivity() {
        return lastActivity;
    }
}
