package com.openpillars.util;

import com.openpillars.OpenPillars;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles all configuration file loading, saving, and management.
 * Supports async loading to maintain server TPS.
 */
public class FileHandler {

    private final OpenPillars plugin;
    private final Map<String, FileConfiguration> configs;
    private final Map<String, File> files;
    
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public FileHandler(OpenPillars plugin) {
        this.plugin = plugin;
        this.configs = new HashMap<>();
        this.files = new HashMap<>();
    }

    /**
     * Loads all configuration files synchronously
     */
    public void loadAll() {
        // Save defaults if they don't exist
        plugin.saveDefaultConfig();
        saveDefaultConfig("loot.yml");
        saveDefaultConfig("messages.yml");
        
        // Load configs
        plugin.reloadConfig();
        configs.put("config", plugin.getConfig());
        
        loadConfig("loot.yml");
        loadConfig("messages.yml");
    }

    /**
     * Loads all configuration files asynchronously
     * @return CompletableFuture that completes when all configs are loaded
     */
    public CompletableFuture<Void> loadAllAsync() {
        return CompletableFuture.runAsync(() -> {
            loadAll();
        }).exceptionally(ex -> {
            plugin.getLogger().log(Level.SEVERE, "Failed to load configurations asynchronously", ex);
            return null;
        });
    }

    /**
     * Loads a specific config file
     * @param fileName The name of the config file
     */
    public void loadConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        
        if (!file.exists()) {
            saveDefaultConfig(fileName);
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        // Look for defaults in the jar
        InputStream defConfigStream = plugin.getResource(fileName);
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
            config.setDefaults(defConfig);
        }
        
        configs.put(fileName.replace(".yml", ""), config);
        files.put(fileName.replace(".yml", ""), file);
    }

    /**
     * Saves the default config from resources if it doesn't exist
     * @param fileName The name of the config file
     */
    public void saveDefaultConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
    }

    /**
     * Saves a config to file
     * @param name The config name (without .yml)
     */
    public void saveConfig(String name) {
        FileConfiguration config = configs.get(name);
        File file = files.get(name);
        
        if (config != null && file != null) {
            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save " + name + ".yml", e);
            }
        }
    }

    /**
     * Saves a config to file asynchronously
     * @param name The config name (without .yml)
     * @return CompletableFuture that completes when the config is saved
     */
    public CompletableFuture<Void> saveConfigAsync(String name) {
        return CompletableFuture.runAsync(() -> saveConfig(name));
    }

    /**
     * Gets the main config
     * @return The main FileConfiguration
     */
    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    /**
     * Gets the loot config
     * @return The loot FileConfiguration
     */
    public FileConfiguration getLootConfig() {
        return configs.get("loot");
    }

    /**
     * Gets the messages config
     * @return The messages FileConfiguration
     */
    public FileConfiguration getMessagesConfig() {
        return configs.get("messages");
    }

    /**
     * Gets the plugin prefix from messages.yml
     * @return The colored prefix string
     */
    public String getPrefix() {
        return colorize(getMessagesConfig().getString("prefix", "&8[&6OpenPillars&8] &r"));
    }

    /**
     * Gets a message from messages.yml with the prefix
     * @param path The path to the message
     * @return The colored message with prefix
     */
    public String getMessage(String path) {
        String message = getMessagesConfig().getString(path, "Message not found: " + path);
        return getPrefix() + colorize(message);
    }

    /**
     * Gets a message from messages.yml without the prefix
     * @param path The path to the message
     * @return The colored message
     */
    public String getRawMessage(String path) {
        String message = getMessagesConfig().getString(path, "Message not found: " + path);
        return colorize(message);
    }

    /**
     * Gets a message with placeholders replaced
     * @param path The path to the message
     * @param placeholders Key-value pairs for replacement
     * @return The colored message with placeholders replaced
     */
    public String getMessage(String path, String... placeholders) {
        String message = getMessage(path);
        
        if (placeholders.length % 2 != 0) {
            plugin.getLogger().warning("Invalid placeholder pairs for message: " + path);
            return message;
        }
        
        for (int i = 0; i < placeholders.length; i += 2) {
            message = message.replace(placeholders[i], placeholders[i + 1]);
        }
        
        return message;
    }

    /**
     * Gets a raw message with placeholders replaced
     * @param path The path to the message
     * @param placeholders Key-value pairs for replacement
     * @return The colored message with placeholders replaced
     */
    public String getRawMessage(String path, String... placeholders) {
        String message = getRawMessage(path);
        
        if (placeholders.length % 2 != 0) {
            plugin.getLogger().warning("Invalid placeholder pairs for message: " + path);
            return message;
        }
        
        for (int i = 0; i < placeholders.length; i += 2) {
            message = message.replace(placeholders[i], placeholders[i + 1]);
        }
        
        return message;
    }

    /**
     * Colorizes a string with both standard color codes and hex colors
     * @param message The message to colorize
     * @return The colorized message
     */
    public static String colorize(String message) {
        if (message == null) return "";
        
        // Handle hex colors (&#RRGGBB format)
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String hexColor = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hexColor.toCharArray()) {
                replacement.append("§").append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        
        // Handle standard color codes
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    /**
     * Strips all color codes from a message
     * @param message The message to strip
     * @return The message without color codes
     */
    public static String stripColor(String message) {
        return ChatColor.stripColor(colorize(message));
    }

    /**
     * Sends a title to a player with cross-version support (1.8+).
     * On 1.8 the fade parameters are ignored (not supported).
     * @param player  The player
     * @param title   Title text (already colorized)
     * @param subtitle Subtitle text (already colorized)
     * @param fadeIn  Fade-in ticks
     * @param stay    Stay ticks
     * @param fadeOut Fade-out ticks
     */
    @SuppressWarnings("deprecation")
    public static void sendTitle(org.bukkit.entity.Player player, String title, String subtitle,
                                 int fadeIn, int stay, int fadeOut) {
        try {
            // Try the 1.9+ five-arg method first
            player.getClass().getMethod("sendTitle", String.class, String.class,
                    int.class, int.class, int.class)
                    .invoke(player, title, subtitle, fadeIn, stay, fadeOut);
        } catch (NoSuchMethodException e) {
            // Fall back to the 1.8 two-arg method
            try {
                player.sendTitle(title, subtitle);
            } catch (Exception ignored) { }
        } catch (Exception ignored) { }
    }

    /**
     * Sends an action bar message to a player, compatible with 1.8+.
     * Uses NMS packets on 1.8, spigot API on 1.9+.
     * @param player The player
     * @param message The message (already color-coded)
     */
    public static void sendActionBar(org.bukkit.entity.Player player, String message) {
        try {
            // Try the Spigot API approach first (1.9+)
            Class<?> chatMessageType = Class.forName("net.md_5.bungee.api.ChatMessageType");
            Object actionBar = chatMessageType.getField("ACTION_BAR").get(null);
            Class<?> textComponent = Class.forName("net.md_5.bungee.api.chat.TextComponent");
            Object component = textComponent.getConstructor(String.class).newInstance(message);
            Class<?> baseComponent = Class.forName("net.md_5.bungee.api.chat.BaseComponent");
            Object spigot = player.getClass().getMethod("spigot").invoke(player);
            // Try the sendMessage(ChatMessageType, BaseComponent) method
            spigot.getClass().getMethod("sendMessage", chatMessageType, baseComponent)
                    .invoke(spigot, actionBar, component);
        } catch (Exception e) {
            // Fallback: use NMS packets for 1.8
            try {
                String version = org.bukkit.Bukkit.getServer().getClass().getPackage().getName()
                        .split("\\.")[3];
                Class<?> chatSerializer = Class.forName(
                        "net.minecraft.server." + version + ".IChatBaseComponent$ChatSerializer");
                Object chatComponent = chatSerializer.getMethod("a", String.class)
                        .invoke(null, "{\"text\":\"" + message.replace("\"", "\\\"")
                        + "\"}");
                Class<?> iChatBaseComponent = Class.forName(
                        "net.minecraft.server." + version + ".IChatBaseComponent");
                Class<?> packetClass = Class.forName(
                        "net.minecraft.server." + version + ".PacketPlayOutChat");
                Object packet = packetClass.getConstructor(iChatBaseComponent, byte.class)
                        .newInstance(chatComponent, (byte) 2);
                Object handle = player.getClass().getMethod("getHandle").invoke(player);
                Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
                playerConnection.getClass().getMethod("sendPacket",
                        Class.forName("net.minecraft.server." + version + ".Packet"))
                        .invoke(playerConnection, packet);
            } catch (Exception ex) {
                // Last resort: just send as chat
                player.sendMessage(message);
            }
        }
    }
}
