package com.toni.dethroned.backend.websocket.domain;

/**
 * Enum für verschiedene Typen von WebSocket-Sessions.
 * Wird verwendet um den Kontext einer Verbindung zu identifizieren.
 */
public enum SessionType {
    /**
     * Session für eine Lobby
     */
    LOBBY,

    /**
     * Session für ein Spiel
     */
    GAME,

    /**
     * Session für Spectators
     */
    SPECTATOR
}
