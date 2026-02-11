package com.openpillars.game;

/**
 * Represents the different states a game can be in.
 * Follows the flow: LOBBY -> STARTING -> ACTIVE -> ENDING
 */
public enum GameState {
    
    /**
     * Waiting for players to join.
     * Players can move around the lobby freely.
     */
    LOBBY,
    
    /**
     * Countdown before game starts.
     * Players are teleported to pillars and frozen.
     */
    STARTING,
    
    /**
     * Game is actively running.
     * Players can break blocks and fight.
     */
    ACTIVE,
    
    /**
     * Game has ended, showing results.
     * Players cannot interact, winner is being announced.
     */
    ENDING,
    
    /**
     * Game is being reset/cleaned up.
     * No players should be in this state.
     */
    RESETTING
}
