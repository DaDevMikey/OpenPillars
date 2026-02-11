package com.openpillars.events;

import com.openpillars.game.GameState;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Called when the game state changes
 */
public class GameStateChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final GameState previousState;
    private final GameState newState;

    public GameStateChangeEvent(GameState previousState, GameState newState) {
        this.previousState = previousState;
        this.newState = newState;
    }

    /**
     * Gets the previous game state
     * @return The previous GameState
     */
    public GameState getPreviousState() {
        return previousState;
    }

    /**
     * Gets the new game state
     * @return The new GameState
     */
    public GameState getNewState() {
        return newState;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
