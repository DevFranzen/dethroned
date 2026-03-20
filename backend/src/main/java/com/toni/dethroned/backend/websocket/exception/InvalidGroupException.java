package com.toni.dethroned.backend.websocket.exception;

/**
 * Exception wenn eine Gruppe nicht existiert oder nicht erreichbar ist
 */
public class InvalidGroupException extends WebSocketException {
    public InvalidGroupException(String groupId) {
        super("Group " + groupId + " not found or not accessible");
    }
}
