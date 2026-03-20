package com.toni.dethroned.backend.websocket.exception;

/**
 * Exception when the player ID is invalid
 */
public class PlayerNotAuthenticatedException extends WebSocketException {
    public PlayerNotAuthenticatedException(String playerId) {
        super("Player " + playerId + " is not authenticated");
    }
}
