package com.toni.dethroned.backend.websocket.exception;

/**
 * Exception when a group does not exist or is not accessible
 */
public class InvalidGroupException extends WebSocketException {
    public InvalidGroupException(String groupId) {
        super("Group " + groupId + " not found or not accessible");
    }
}
