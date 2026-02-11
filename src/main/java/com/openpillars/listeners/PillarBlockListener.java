package com.openpillars.listeners;

import com.openpillars.OpenPillars;
import com.openpillars.game.GameManager;
import com.openpillars.game.GameState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Handles pillar block interactions (breaking and placing).
 */
public class PillarBlockListener implements Listener {

    private final OpenPillars plugin;

    public PillarBlockListener(OpenPillars plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        GameManager gameManager = plugin.getGameManager();
        
        // Only handle if player is in game and game is active
        if (!gameManager.isPlaying(player)) return;
        if (gameManager.getState() != GameState.ACTIVE) {
            event.setCancelled(true);
            return;
        }
        
        // Check if it's a pillar block
        if (gameManager.getPillarGenerator().isPillarBlock(event.getBlock().getLocation())) {
            // Cancel the default event, we handle it ourselves
            event.setCancelled(true);
            
            // Handle through pillar generator (gives loot, fires events)
            gameManager.getPillarGenerator().handleBlockBreak(player, event.getBlock());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        GameManager gameManager = plugin.getGameManager();
        
        // Only handle if player is in game
        if (!gameManager.isPlaying(player)) return;
        
        // Prevent block placement during non-active states
        if (gameManager.getState() != GameState.ACTIVE) {
            event.setCancelled(true);
            return;
        }
        
        // Allow block placement during active game
        // Could add restrictions here (e.g., only near their pillar)
    }
}
