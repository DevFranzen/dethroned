package com.toni.dethroned.backend.auth.service;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {
    private final Set<String> generatedPlayerIds = ConcurrentHashMap.newKeySet();

    public String generatePlayerId() {
        String playerId = UUID.randomUUID().toString();
        generatedPlayerIds.add(playerId);
        return playerId;
    }

    public boolean isValidPlayerId(String playerId) {
        return generatedPlayerIds.contains(playerId);
    }
}
