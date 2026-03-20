package com.toni.dethroned.backend.websocket.exception;

/**
 * Basis-Exception für WebSocket-Fehler
 */
public class WebSocketException extends RuntimeException {
    public WebSocketException(String message) {
        super(message);
    }

    public WebSocketException(String message, Throwable cause) {
        super(message, cause);
    }
}
