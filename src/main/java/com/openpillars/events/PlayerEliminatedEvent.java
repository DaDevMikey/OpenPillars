package com.openpillars.events;

import com.openpillars.game.GamePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

/**
 * Called when a player dies or is eliminated from the game.
 * Can be cancelled to prevent the death.
 */
public class PlayerEliminatedEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final GamePlayer gamePlayer;
    private final EliminationCause cause;
    private final Player killer;
    private boolean cancelled;

    public PlayerEliminatedEvent(Player player, GamePlayer gamePlayer, 
                                  EliminationCause cause, @Nullable Player killer) {
        this.player = player;
        this.gamePlayer = gamePlayer;
        this.cause = cause;
        this.killer = killer;
        this.cancelled = false;
    }

    /**
     * Gets the eliminated player
     * @return The player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the game player data
     * @return The GamePlayer
     */
    public GamePlayer getGamePlayer() {
        return gamePlayer;
    }

    /**
     * Gets the cause of elimination
     * @return The elimination cause
     */
    public EliminationCause getCause() {
        return cause;
    }

    /**
     * Gets the killer if applicable
     * @return The killer, or null if not killed by a player
     */
    @Nullable
    public Player getKiller() {
        return killer;
    }

    /**
     * Checks if this was a PvP kill
     * @return true if killed by another player
     */
    public boolean isPvPKill() {
        return killer != null && cause == EliminationCause.KILLED;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    /**
     * Enum for different elimination causes
     */
    public enum EliminationCause {
        /** Player fell into the void */
        VOID,
        /** Player was killed by another player */
        KILLED,
        /** Player disconnected/left */
        DISCONNECT,
        /** Player died from other causes (fire, fall, etc.) */
        OTHER
    }
}
