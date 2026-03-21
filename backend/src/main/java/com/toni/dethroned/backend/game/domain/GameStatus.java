package com.toni.dethroned.backend.game.domain;

/**
 * Game status enumeration
 */
public enum GameStatus {
    WAITING,    // Game created, not started yet
    RUNNING,    // Game is active
    PAUSED,     // Game paused (optional)
    FINISHED    // Game completed
}
