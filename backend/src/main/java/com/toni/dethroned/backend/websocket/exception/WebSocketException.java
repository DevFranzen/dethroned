package com.toni.dethroned.backend.websocket.exception;

/**
 * Base exception for WebSocket errors
 */
public class WebSocketException extends RuntimeException {
    public WebSocketException(String message) {
        super(message);
    }

    public WebSocketException(String message, Throwable cause) {
        super(message, cause);
    }
}
