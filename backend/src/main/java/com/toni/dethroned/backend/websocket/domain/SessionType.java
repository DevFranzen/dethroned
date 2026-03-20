package com.toni.dethroned.backend.websocket.domain;

/**
 * Enum for different types of WebSocket sessions.
 * Used to identify the context of a connection.
 */
public enum SessionType {
    /**
     * Session for a lobby
     */
    LOBBY,

    /**
     * Session for a game
     */
    GAME,

    /**
     * Session for spectators
     */
    SPECTATOR
}
