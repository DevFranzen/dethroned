package com.toni.dethroned.backend.websocket.exception;

/**
 * Exception wenn die Player ID ungültig ist
 */
public class PlayerNotAuthenticatedException extends WebSocketException {
    public PlayerNotAuthenticatedException(String playerId) {
        super("Player " + playerId + " is not authenticated");
    }
}
