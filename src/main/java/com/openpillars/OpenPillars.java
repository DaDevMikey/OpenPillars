package com.openpillars;

import com.openpillars.commands.PillarsCommand;
import com.openpillars.game.GameManager;
import com.openpillars.listeners.MovementController;
import com.openpillars.listeners.PlayerListener;
import com.openpillars.listeners.PillarBlockListener;
import com.openpillars.placeholders.PillarsExpansion;
import com.openpillars.util.FileHandler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class OpenPillars extends JavaPlugin {

    private static OpenPillars instance;
    private FileHandler fileHandler;
    private GameManager gameManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize file handler and load configs
        this.fileHandler = new FileHandler(this);
        this.fileHandler.loadAll();
        
        // Initialize game manager
        this.gameManager = new GameManager(this);
        
        // Register listeners
        registerListeners();
        
        // Register commands
        registerCommands();
        
        // Hook into PlaceholderAPI if available
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PillarsExpansion(this).register();
            getLogger().info("Successfully hooked into PlaceholderAPI!");
        }
        
        getLogger().info("=================================");
        getLogger().info("OpenPillars has been enabled!");
        getLogger().info("Version: " + getDescription().getVersion());
        getLogger().info("=================================");
    }

    @Override
    public void onDisable() {
        // Stop any active games
        if (gameManager != null) {
            gameManager.stopAllGames();
        }
        
        getLogger().info("OpenPillars has been disabled!");
    }
    
    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new MovementController(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PillarBlockListener(this), this);
    }
    
    private void registerCommands() {
        PillarsCommand pillarsCommand = new PillarsCommand(this);
        getCommand("pillars").setExecutor(pillarsCommand);
        getCommand("pillars").setTabCompleter(pillarsCommand);
    }
    
    public void reload() {
        fileHandler.loadAll();
        getLogger().info("Configuration reloaded!");
    }

    public static OpenPillars getInstance() {
        return instance;
    }

    public FileHandler getFileHandler() {
        return fileHandler;
    }

    public GameManager getGameManager() {
        return gameManager;
    }
}
