package com.toni.dethroned.backend.websocket.exception;

/**
 * Exception wenn ein Player nicht berechtigt ist, auf eine Gruppe zuzugreifen
 */
public class UnauthorizedGroupAccessException extends WebSocketException {
    public UnauthorizedGroupAccessException(String playerId, String groupId) {
        super("Player " + playerId + " is not authorized to access group " + groupId);
    }
}
