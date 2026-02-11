package com.openpillars.events;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Called when a player breaks a pillar block.
 * Can be cancelled or loot can be modified by other plugins.
 */
public class PillarBlockBreakEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final Block block;
    private ItemStack loot;
    private final boolean ownPillar;
    private boolean cancelled;

    public PillarBlockBreakEvent(Player player, Block block, @Nullable ItemStack loot, boolean ownPillar) {
        this.player = player;
        this.block = block;
        this.loot = loot;
        this.ownPillar = ownPillar;
        this.cancelled = false;
    }

    /**
     * Gets the player who broke the block
     * @return The player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the block that was broken
     * @return The broken block
     */
    public Block getBlock() {
        return block;
    }

    /**
     * Gets the loot that will be given to the player
     * @return The loot ItemStack, or null if no loot
     */
    @Nullable
    public ItemStack getLoot() {
        return loot;
    }

    /**
     * Sets the loot to give to the player
     * @param loot The new loot, or null for no loot
     */
    public void setLoot(@Nullable ItemStack loot) {
        this.loot = loot;
    }

    /**
     * Checks if this is the player's own pillar
     * @return true if breaking their own pillar
     */
    public boolean isOwnPillar() {
        return ownPillar;
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
}
