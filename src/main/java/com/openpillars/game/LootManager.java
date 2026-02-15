package com.openpillars.game;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.openpillars.OpenPillars;
import com.openpillars.util.FileHandler;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages loot tables and weighted random item selection.
 * Supports cross-version materials through XSeries.
 */
public class LootManager {

    private final OpenPillars plugin;
    private final Map<String, List<LootItem>> lootTables;
    private final Map<String, Integer> totalWeights;

    public LootManager(OpenPillars plugin) {
        this.plugin = plugin;
        this.lootTables = new ConcurrentHashMap<>();
        this.totalWeights = new ConcurrentHashMap<>();
        loadLootTables();
    }

    /**
     * Loads all loot tables from the config
     */
    public void loadLootTables() {
        lootTables.clear();
        totalWeights.clear();
        
        ConfigurationSection tables = plugin.getFileHandler().getLootConfig()
                .getConfigurationSection("loot-tables");
        
        if (tables == null) {
            plugin.getLogger().warning("No loot tables found in loot.yml!");
            return;
        }
        
        for (String tableName : tables.getKeys(false)) {
            List<Map<?, ?>> items = tables.getMapList(tableName);
            List<LootItem> lootItems = new ArrayList<>();
            int totalWeight = 0;
            
            for (Map<?, ?> itemData : items) {
                LootItem lootItem = parseLootItem(itemData);
                if (lootItem != null) {
                    lootItems.add(lootItem);
                    totalWeight += lootItem.getWeight();
                }
            }
            
            lootTables.put(tableName, lootItems);
            totalWeights.put(tableName, totalWeight);
            
            plugin.getLogger().info("Loaded loot table '" + tableName + "' with " 
                    + lootItems.size() + " items (total weight: " + totalWeight + ")");
        }
    }

    /**
     * Parses a loot item from config data
     * @param data The config data map
     * @return The parsed LootItem, or null if invalid
     */
    private LootItem parseLootItem(Map<?, ?> data) {
        String materialName = (String) data.get("material");
        if (materialName == null) return null;
        
        // Use XMaterial for cross-version support
        Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(materialName);
        if (!xMaterial.isPresent()) {
            plugin.getLogger().warning("Invalid material: " + materialName);
            return null;
        }
        
        int weight = getInt(data, "weight", 1);
        Object amountObj = data.get("amount");
        String amountStr = amountObj != null ? String.valueOf(amountObj) : "1";
        String name = (String) data.get("name");
        
        @SuppressWarnings("unchecked")
        List<String> lore = (List<String>) data.get("lore");
        
        @SuppressWarnings("unchecked")
        List<String> enchantments = (List<String>) data.get("enchantments");
        
        return new LootItem(xMaterial.get(), weight, amountStr, name, lore, enchantments);
    }

    /**
     * Gets an int from a map with default value
     */
    private int getInt(Map<?, ?> data, String key, int defaultValue) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    /**
     * Gets a random item from the specified loot table
     * @param tableName The name of the loot table
     * @return A random ItemStack, or null if table not found
     */
    public ItemStack getRandomItem(String tableName) {
        List<LootItem> items = lootTables.get(tableName);
        Integer totalWeight = totalWeights.get(tableName);
        
        if (items == null || items.isEmpty() || totalWeight == null || totalWeight <= 0) {
            return null;
        }
        
        // Check global drop chance
        double globalChance = plugin.getFileHandler().getLootConfig()
                .getDouble("global-drop-chance", 100);
        if (ThreadLocalRandom.current().nextDouble(100) > globalChance) {
            return null;
        }
        
        // Weighted random selection
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        int currentWeight = 0;
        
        for (LootItem item : items) {
            currentWeight += item.getWeight();
            if (roll < currentWeight) {
                return item.createItemStack();
            }
        }
        
        // Fallback (shouldn't happen)
        return items.get(items.size() - 1).createItemStack();
    }

    /**
     * Gets the current loot table based on game time
     * @param gameTimeMillis The current game time in milliseconds
     * @return The name of the loot table to use
     */
    public String getCurrentLootTable(long gameTimeMillis) {
        if (!plugin.getFileHandler().getLootConfig().getBoolean("dynamic-loot.enabled", false)) {
            return "standard-pillar";
        }
        
        List<Map<?, ?>> switches = plugin.getFileHandler().getLootConfig()
                .getMapList("dynamic-loot.switches");
        
        int gameTimeMinutes = (int) (gameTimeMillis / 60000);
        String currentTable = "standard-pillar";
        
        for (Map<?, ?> switchData : switches) {
            int time = getInt(switchData, "time", 0);
            if (gameTimeMinutes >= time) {
                Object tableObj = switchData.get("table");
                if (tableObj != null) {
                    currentTable = (String) tableObj;
                }
            }
        }
        
        return currentTable;
    }

    /**
     * Represents a single loot item with all its properties
     */
    public static class LootItem {
        private final XMaterial material;
        private final int weight;
        private final String amountStr;
        private final String name;
        private final List<String> lore;
        private final List<String> enchantments;

        public LootItem(XMaterial material, int weight, String amountStr, 
                       String name, List<String> lore, List<String> enchantments) {
            this.material = material;
            this.weight = weight;
            this.amountStr = amountStr;
            this.name = name;
            this.lore = lore;
            this.enchantments = enchantments;
        }

        public int getWeight() {
            return weight;
        }

        /**
         * Creates an ItemStack from this loot item
         * @return The created ItemStack
         */
        public ItemStack createItemStack() {
            ItemStack item = material.parseItem();
            if (item == null) return null;
            
            // Parse amount (can be "1" or "1-5" for range)
            item.setAmount(parseAmount());
            
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                // Apply custom name
                if (name != null) {
                    meta.setDisplayName(FileHandler.colorize(name));
                }
                
                // Apply lore
                if (lore != null && !lore.isEmpty()) {
                    List<String> coloredLore = new ArrayList<>();
                    for (String line : lore) {
                        coloredLore.add(FileHandler.colorize(line));
                    }
                    meta.setLore(coloredLore);
                }
                
                item.setItemMeta(meta);
            }
            
            // Apply enchantments
            if (enchantments != null) {
                for (String enchantStr : enchantments) {
                    applyEnchantment(item, enchantStr);
                }
            }
            
            return item;
        }

        /**
         * Parses the amount string to get the actual amount
         * @return The parsed amount
         */
        private int parseAmount() {
            if (amountStr.contains("-")) {
                String[] parts = amountStr.split("-");
                try {
                    int min = Integer.parseInt(parts[0].trim());
                    int max = Integer.parseInt(parts[1].trim());
                    return ThreadLocalRandom.current().nextInt(min, max + 1);
                } catch (NumberFormatException e) {
                    return 1;
                }
            }
            
            try {
                return Integer.parseInt(amountStr.trim());
            } catch (NumberFormatException e) {
                return 1;
            }
        }

        /**
         * Applies an enchantment string to an item
         * @param item The item to enchant
         * @param enchantStr The enchantment string (FORMAT: "ENCHANT_NAME:level")
         */
        private void applyEnchantment(ItemStack item, String enchantStr) {
            String[] parts = enchantStr.split(":");
            if (parts.length != 2) return;
            
            String enchantName = parts[0].trim();
            int level;
            try {
                level = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                return;
            }
            
            // Use XEnchantment for cross-version support
            Optional<XEnchantment> xEnchant = XEnchantment.matchXEnchantment(enchantName);
            if (xEnchant.isPresent()) {
                Enchantment enchantment = xEnchant.get().getEnchant();
                if (enchantment != null) {
                    item.addUnsafeEnchantment(enchantment, level);
                }
            }
        }
    }
}
