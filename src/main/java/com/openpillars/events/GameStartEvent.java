package com.openpillars.events;

import com.openpillars.game.GameManager;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Called when a game starts (players unfrozen, gameplay begins)
 */
public class GameStartEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final GameManager gameManager;

    public GameStartEvent(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * Gets the game manager for this game
     * @return The GameManager instance
     */
    public GameManager getGameManager() {
        return gameManager;
    }

    /**
     * Gets the number of players in the game
     * @return The player count
     */
    public int getPlayerCount() {
        return gameManager.getPlayers().size();
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
