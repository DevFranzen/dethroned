package com.toni.dethroned.backend.websocket.domain;

import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;

/**
 * Repräsentiert eine WebSocket-Verbindung eines Players zu einer Gruppe.
 * Speichert Verbindungs-Metadaten wie Connect-Zeit und letzte Aktivität.
 */
public class PlayerConnection {
    private String playerId;
    private WebSocketSession webSocketSession;
    private LocalDateTime connectedAt;
    private LocalDateTime lastActivityAt;

    public PlayerConnection(String playerId, WebSocketSession webSocketSession, LocalDateTime connectedAt) {
        this.playerId = playerId;
        this.webSocketSession = webSocketSession;
        this.connectedAt = connectedAt;
        this.lastActivityAt = connectedAt;
    }

    public String getPlayerId() {
        return playerId;
    }

    public WebSocketSession getWebSocketSession() {
        return webSocketSession;
    }

    public LocalDateTime getConnectedAt() {
        return connectedAt;
    }

    public LocalDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(LocalDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public void updateActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }

    public boolean isAlive() {
        return webSocketSession != null && webSocketSession.isOpen();
    }
}
