package com.toni.dethroned.backend.websocket.exception;

/**
 * Exception when a player is not authorized to access a group
 */
public class UnauthorizedGroupAccessException extends WebSocketException {
    public UnauthorizedGroupAccessException(String playerId, String groupId) {
        super("Player " + playerId + " is not authorized to access group " + groupId);
    }
}
