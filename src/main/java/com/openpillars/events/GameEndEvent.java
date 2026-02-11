package com.openpillars.events;

import com.openpillars.game.GameManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

/**
 * Called when a game ends
 */
public class GameEndEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final GameManager gameManager;
    private final Player winner;

    public GameEndEvent(GameManager gameManager, @Nullable Player winner) {
        this.gameManager = gameManager;
        this.winner = winner;
    }

    /**
     * Gets the game manager for this game
     * @return The GameManager instance
     */
    public GameManager getGameManager() {
        return gameManager;
    }

    /**
     * Gets the winner of the game
     * @return The winning player, or null if no winner (draw/timeout)
     */
    @Nullable
    public Player getWinner() {
        return winner;
    }

    /**
     * Checks if there was a winner
     * @return true if there was a winner
     */
    public boolean hasWinner() {
        return winner != null;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
