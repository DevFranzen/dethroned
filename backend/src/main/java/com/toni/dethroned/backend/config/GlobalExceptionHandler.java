package com.toni.dethroned.backend.config;

import com.toni.dethroned.backend.lobby.exception.InvalidAdminException;
import com.toni.dethroned.backend.lobby.exception.LobbyFullException;
import com.toni.dethroned.backend.lobby.exception.LobbyNotFoundException;
import com.toni.dethroned.backend.lobby.exception.PlayerNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LobbyNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleLobbyNotFound(LobbyNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse(e.getMessage()));
    }

    @ExceptionHandler(PlayerNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePlayerNotFound(PlayerNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse(e.getMessage()));
    }

    @ExceptionHandler(LobbyFullException.class)
    public ResponseEntity<Map<String, Object>> handleLobbyFull(LobbyFullException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse(e.getMessage()));
    }

    @ExceptionHandler(InvalidAdminException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidAdmin(InvalidAdminException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse(e.getMessage()));
    }

    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}
