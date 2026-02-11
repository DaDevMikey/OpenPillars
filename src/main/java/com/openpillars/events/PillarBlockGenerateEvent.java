package com.openpillars.events;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Called when a pillar block is about to be generated.
 * Can be cancelled or modified by other plugins.
 */
public class PillarBlockGenerateEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final Location location;
    private Material material;
    private boolean cancelled;

    public PillarBlockGenerateEvent(Player player, Location location, Material material) {
        this.player = player;
        this.location = location;
        this.material = material;
        this.cancelled = false;
    }

    /**
     * Gets the player whose pillar this block is being added to
     * @return The player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the location where the block will be placed
     * @return The block location
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Gets the material that will be placed
     * @return The block material
     */
    public Material getMaterial() {
        return material;
    }

    /**
     * Sets the material to be placed
     * @param material The new material
     */
    public void setMaterial(Material material) {
        this.material = material;
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
