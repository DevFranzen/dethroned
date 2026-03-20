package com.toni.dethroned.backend.lobby.domain;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class Lobby {
    private String id;
    private String code;
    private String hostId;
    private String displayClientId;
    private LobbyStatus status;
    private Map<String, Player> players;
    private GameSettings settings;
    private Instant createdAt;
    private Instant lastActivity;

    public Lobby(String id, String code, String hostId, GameSettings settings) {
        this.id = id;
        this.code = code;
        this.hostId = hostId;
        this.settings = settings;
        this.status = LobbyStatus.OPEN;
        this.players = new HashMap<>();
        this.createdAt = Instant.now();
        this.lastActivity = Instant.now();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getHostId() {
        return hostId;
    }

    public String getDisplayClientId() {
        return displayClientId;
    }

    public void setDisplayClientId(String displayClientId) {
        this.displayClientId = displayClientId;
    }

    public LobbyStatus getStatus() {
        return status;
    }

    public void setStatus(LobbyStatus status) {
        this.status = status;
        this.lastActivity = Instant.now();
    }

    public Map<String, Player> getPlayers() {
        return players;
    }

    public GameSettings getSettings() {
        return settings;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastActivity() {
        return lastActivity;
    }

    public void updateLastActivity() {
        this.lastActivity = Instant.now();
    }

    // Business Logic
    public void addPlayer(Player player) {
        players.put(player.getId(), player);
        updateLastActivity();
    }

    public void removePlayer(String playerId) {
        players.remove(playerId);
        updateLastActivity();
    }

    public Player getPlayer(String playerId) {
        return players.get(playerId);
    }

    public boolean isFull() {
        return players.size() >= settings.getMaxPlayers();
    }

    public boolean allPlayersReady() {
        return players.values().stream()
                .allMatch(p -> p.getStatus() == PlayerStatus.READY);
    }

    public boolean canStart() {
        return players.size() >= settings.getMinPlayers() && allPlayersReady();
    }
}
