package com.openpillars.game;

import com.cryptomorin.xseries.XMaterial;
import com.openpillars.OpenPillars;
import com.openpillars.events.PillarBlockBreakEvent;
import com.openpillars.events.PillarBlockGenerateEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles pillar generation and block management.
 * All generation is done async where possible to maintain TPS.
 */
public class PillarGenerator {

    private final OpenPillars plugin;
    
    // Map of player UUID to their pillar data
    private final Map<UUID, PillarData> playerPillars;
    
    // Map of block locations to owning player
    private final Map<Location, UUID> blockOwners;

    public PillarGenerator(OpenPillars plugin) {
        this.plugin = plugin;
        this.playerPillars = new ConcurrentHashMap<>();
        this.blockOwners = new ConcurrentHashMap<>();
    }

    /**
     * Generates the initial pillar for a player
     * @param baseLocation The base location of the pillar
     * @param playerId The player's UUID
     */
    public void generateInitialPillar(Location baseLocation, UUID playerId) {
        int initialHeight = plugin.getConfig().getInt("pillar.pillar-initial-height", 5);
        World world = baseLocation.getWorld();
        
        if (world == null) return;
        
        PillarData pillarData = new PillarData(baseLocation, playerId);
        playerPillars.put(playerId, pillarData);
        
        // Get the base material (stone for initial pillar)
        Material baseMaterial = XMaterial.STONE.parseMaterial();
        if (baseMaterial == null) baseMaterial = Material.STONE;
        
        // Generate initial pillar blocks
        for (int y = 0; y < initialHeight; y++) {
            Location blockLoc = baseLocation.clone().add(0, y, 0);
            Block block = world.getBlockAt(blockLoc);
            block.setType(baseMaterial);
            
            pillarData.addBlock(blockLoc);
            blockOwners.put(normalizeLocation(blockLoc), playerId);
        }
        
        // Store the current top for future generation
        pillarData.setCurrentHeight(initialHeight);
        
        // Store spawn location for the game player
        GamePlayer gamePlayer = plugin.getGameManager().getGamePlayer(playerId);
        if (gamePlayer != null) {
            gamePlayer.setPillarBase(baseLocation);
        }
    }

    /**
     * Generates a new block on a player's pillar
     * @param playerId The player's UUID
     * @param lootManager The loot manager for item selection
     */
    public void generateBlock(UUID playerId, LootManager lootManager) {
        PillarData pillarData = playerPillars.get(playerId);
        if (pillarData == null) return;
        
        int maxHeight = plugin.getConfig().getInt("pillar.max-height", 50);
        if (pillarData.getCurrentHeight() >= maxHeight) return;
        
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return;
        
        Location baseLocation = pillarData.getBaseLocation();
        World world = baseLocation.getWorld();
        if (world == null) return;
        
        int blocksPerInterval = plugin.getConfig().getInt("pillar.blocks-per-interval", 1);
        
        for (int i = 0; i < blocksPerInterval && pillarData.getCurrentHeight() < maxHeight; i++) {
            Location blockLoc = baseLocation.clone().add(0, pillarData.getCurrentHeight(), 0);
            
            // Get random block material - using a selection of building blocks
            Material blockMaterial = getRandomPillarBlock();
            
            // Fire pre-generation event
            PillarBlockGenerateEvent event = new PillarBlockGenerateEvent(
                    player, blockLoc, blockMaterial);
            Bukkit.getPluginManager().callEvent(event);
            
            if (event.isCancelled()) continue;
            
            // Place the block
            Block block = world.getBlockAt(blockLoc);
            block.setType(event.getMaterial());
            
            pillarData.addBlock(blockLoc);
            blockOwners.put(normalizeLocation(blockLoc), playerId);
            pillarData.setCurrentHeight(pillarData.getCurrentHeight() + 1);
            
            // Store loot for this block
            String lootTable = lootManager.getCurrentLootTable(
                    plugin.getGameManager().getGameTime());
            ItemStack loot = lootManager.getRandomItem(lootTable);
            if (loot != null) {
                pillarData.setBlockLoot(blockLoc, loot);
            }
        }
    }

    /**
     * Gets a random block material for pillar generation
     * @return A random Material for the pillar block
     */
    private Material getRandomPillarBlock() {
        // List of possible pillar blocks
        XMaterial[] pillarBlocks = {
            XMaterial.STONE,
            XMaterial.COBBLESTONE,
            XMaterial.DIRT,
            XMaterial.OAK_PLANKS,
            XMaterial.GRAVEL,
            XMaterial.SAND,
            XMaterial.CLAY,
            XMaterial.SANDSTONE,
            XMaterial.MOSSY_COBBLESTONE,
            XMaterial.ANDESITE,
            XMaterial.DIORITE,
            XMaterial.GRANITE
        };
        
        XMaterial selected = pillarBlocks[new Random().nextInt(pillarBlocks.length)];
        Material material = selected.parseMaterial();
        return material != null ? material : Material.STONE;
    }

    /**
     * Handles a player breaking a pillar block
     * @param player The player breaking the block
     * @param block The block being broken
     * @return true if the block was a pillar block and handled
     */
    public boolean handleBlockBreak(Player player, Block block) {
        Location blockLoc = normalizeLocation(block.getLocation());
        UUID ownerId = blockOwners.get(blockLoc);
        
        if (ownerId == null) return false; // Not a pillar block
        
        PillarData pillarData = playerPillars.get(ownerId);
        if (pillarData == null) return false;
        
        // Get the stored loot for this block
        ItemStack loot = pillarData.getBlockLoot(blockLoc);
        
        // Fire event
        PillarBlockBreakEvent event = new PillarBlockBreakEvent(
                player, block, loot, ownerId.equals(player.getUniqueId()));
        Bukkit.getPluginManager().callEvent(event);
        
        if (event.isCancelled()) return true;
        
        // Remove the block from tracking
        pillarData.removeBlock(blockLoc);
        blockOwners.remove(blockLoc);
        
        // Give loot to player if configured
        if (plugin.getConfig().getBoolean("pillar.drop-items", true) && event.getLoot() != null) {
            player.getInventory().addItem(event.getLoot());
            
            GamePlayer gamePlayer = plugin.getGameManager().getGamePlayer(player);
            if (gamePlayer != null) {
                gamePlayer.addBlockBroken();
                gamePlayer.addItemsCollected(event.getLoot().getAmount());
            }
            
            // Send item received message
            String itemName = event.getLoot().hasItemMeta() && event.getLoot().getItemMeta().hasDisplayName()
                    ? event.getLoot().getItemMeta().getDisplayName()
                    : formatMaterialName(event.getLoot().getType().name());
            
            String message = plugin.getFileHandler().getMessage("game.item-received",
                    "%item%", itemName + " x" + event.getLoot().getAmount());
            player.sendMessage(message);
        }
        
        // Break the block (don't drop vanilla items)
        block.setType(Material.AIR);
        
        return true;
    }

    /**
     * Formats a material name to be more readable
     * @param name The material name
     * @return The formatted name
     */
    private String formatMaterialName(String name) {
        String[] words = name.toLowerCase().split("_");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (formatted.length() > 0) formatted.append(" ");
            formatted.append(Character.toUpperCase(word.charAt(0)));
            formatted.append(word.substring(1));
        }
        return formatted.toString();
    }

    /**
     * Gets the owner of a block
     * @param location The block location
     * @return The owner's UUID, or null if not a pillar block
     */
    public UUID getBlockOwner(Location location) {
        return blockOwners.get(normalizeLocation(location));
    }

    /**
     * Checks if a location is part of any pillar
     * @param location The location to check
     * @return true if it's a pillar block
     */
    public boolean isPillarBlock(Location location) {
        return blockOwners.containsKey(normalizeLocation(location));
    }

    /**
     * Gets a player's pillar data
     * @param playerId The player's UUID
     * @return The pillar data, or null if not found
     */
    public PillarData getPillarData(UUID playerId) {
        return playerPillars.get(playerId);
    }

    /**
     * Clears all pillars and resets the world
     */
    public void clearAllPillars() {
        // Clear all pillar blocks
        for (PillarData pillarData : playerPillars.values()) {
            for (Location blockLoc : pillarData.getBlocks()) {
                Block block = blockLoc.getBlock();
                if (block != null) {
                    block.setType(Material.AIR);
                }
            }
        }
        
        playerPillars.clear();
        blockOwners.clear();
    }

    /**
     * Normalizes a location for consistent map keys
     * @param location The location to normalize
     * @return The normalized location
     */
    private Location normalizeLocation(Location location) {
        return new Location(
                location.getWorld(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
    }

    /**
     * Represents data for a single pillar
     */
    public static class PillarData {
        private final Location baseLocation;
        private final UUID ownerId;
        private final Set<Location> blocks;
        private final Map<Location, ItemStack> blockLoot;
        private int currentHeight;

        public PillarData(Location baseLocation, UUID ownerId) {
            this.baseLocation = baseLocation;
            this.ownerId = ownerId;
            this.blocks = ConcurrentHashMap.newKeySet();
            this.blockLoot = new ConcurrentHashMap<>();
            this.currentHeight = 0;
        }

        public Location getBaseLocation() {
            return baseLocation;
        }

        public UUID getOwnerId() {
            return ownerId;
        }

        public Set<Location> getBlocks() {
            return blocks;
        }

        public void addBlock(Location location) {
            blocks.add(location);
        }

        public void removeBlock(Location location) {
            blocks.remove(location);
            blockLoot.remove(location);
        }

        public int getCurrentHeight() {
            return currentHeight;
        }

        public void setCurrentHeight(int currentHeight) {
            this.currentHeight = currentHeight;
        }

        public ItemStack getBlockLoot(Location location) {
            return blockLoot.get(location);
        }

        public void setBlockLoot(Location location, ItemStack loot) {
            blockLoot.put(location, loot);
        }
    }
}
