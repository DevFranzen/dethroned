package com.toni.dethroned.backend.lobby.exception;

public class LobbyException extends RuntimeException {
    public LobbyException(String message) {
        super(message);
    }

    public LobbyException(String message, Throwable cause) {
        super(message, cause);
    }
}
