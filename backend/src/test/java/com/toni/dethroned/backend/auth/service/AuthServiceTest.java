package com.toni.dethroned.backend.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService();
    }

    @Test
    void testGeneratePlayerId() {
        String playerId = authService.generatePlayerId();
        assertNotNull(playerId);
        assertFalse(playerId.isEmpty());
    }

    @Test
    void testGeneratePlayerIdUniqueness() {
        Set<String> playerIds = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            String playerId = authService.generatePlayerId();
            playerIds.add(playerId);
        }
        assertEquals(100, playerIds.size());
    }

    @Test
    void testIsValidPlayerId() {
        String playerId = authService.generatePlayerId();
        assertTrue(authService.isValidPlayerId(playerId));
    }

    @Test
    void testIsValidPlayerIdNotGenerated() {
        assertFalse(authService.isValidPlayerId("non-existent-id"));
    }
}
