package com.openpillars.game;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Represents a player in the game with their stats and state.
 */
public class GamePlayer {

    private final UUID uuid;
    private final String name;
    
    private boolean alive;
    private boolean frozen;
    private Location spawnLocation;
    private Location pillarBase;
    
    private int kills;
    private int blocksPlaced;
    private int blocksBroken;
    private int itemsCollected;

    public GamePlayer(Player player) {
        this.uuid = player.getUniqueId();
        this.name = player.getName();
        this.alive = true;
        this.frozen = false;
        this.kills = 0;
        this.blocksPlaced = 0;
        this.blocksBroken = 0;
        this.itemsCollected = 0;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public void setSpawnLocation(Location spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    public Location getPillarBase() {
        return pillarBase;
    }

    public void setPillarBase(Location pillarBase) {
        this.pillarBase = pillarBase;
    }

    public int getKills() {
        return kills;
    }

    public void addKill() {
        this.kills++;
    }

    public int getBlocksPlaced() {
        return blocksPlaced;
    }

    public void addBlockPlaced() {
        this.blocksPlaced++;
    }

    public int getBlocksBroken() {
        return blocksBroken;
    }

    public void addBlockBroken() {
        this.blocksBroken++;
    }

    public int getItemsCollected() {
        return itemsCollected;
    }

    public void addItemCollected() {
        this.itemsCollected++;
    }

    public void addItemsCollected(int amount) {
        this.itemsCollected += amount;
    }
}
