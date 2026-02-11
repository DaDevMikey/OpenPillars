package com.openpillars.listeners;

import com.openpillars.OpenPillars;
import com.openpillars.game.GameManager;
import com.openpillars.game.GamePlayer;
import com.openpillars.game.GameState;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Controls player movement, implementing the freeze mechanic
 * for the "Seamless Start" experience (no cages).
 */
public class MovementController implements Listener {

    private final OpenPillars plugin;

    public MovementController(OpenPillars plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        GameManager gameManager = plugin.getGameManager();
        
        // Check if player is in our game
        GamePlayer gamePlayer = gameManager.getGamePlayer(player);
        if (gamePlayer == null) return;
        
        // Check if freeze is enabled in config
        if (!plugin.getConfig().getBoolean("freeze.enabled", true)) return;
        
        // Only freeze during STARTING state
        if (gameManager.getState() != GameState.STARTING) return;
        
        // Check if player is frozen
        if (!gamePlayer.isFrozen()) return;
        
        Location from = event.getFrom();
        Location to = event.getTo();
        
        if (to == null) return;
        
        // Check if player actually moved (not just looked around)
        boolean moved = from.getX() != to.getX() || 
                       from.getY() != to.getY() || 
                       from.getZ() != to.getZ();
        
        if (moved) {
            boolean allowLook = plugin.getConfig().getBoolean("freeze.allow-look", true);
            boolean strictMode = plugin.getConfig().getBoolean("freeze.strict-mode", true);
            
            if (strictMode) {
                // Teleport player back to spawn location
                Location spawnLoc = gamePlayer.getSpawnLocation();
                if (spawnLoc != null) {
                    // Keep their head rotation but reset position
                    Location correctedLoc = spawnLoc.clone();
                    correctedLoc.setYaw(to.getYaw());
                    correctedLoc.setPitch(to.getPitch());
                    
                    // Use teleport to prevent any movement
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (player.isOnline() && gamePlayer.isFrozen()) {
                                player.teleport(correctedLoc);
                            }
                        }
                    }.runTask(plugin);
                }
            } else {
                // Just cancel the movement
                if (allowLook) {
                    // Allow looking but not moving
                    Location cancelLoc = from.clone();
                    cancelLoc.setYaw(to.getYaw());
                    cancelLoc.setPitch(to.getPitch());
                    event.setTo(cancelLoc);
                } else {
                    event.setCancelled(true);
                }
            }
            
            // Send action bar message
            sendFreezeActionBar(player);
        }
    }

    /**
     * Sends the freeze action bar message to a player
     * @param player The player to send to
     */
    private void sendFreezeActionBar(Player player) {
        GameManager gameManager = plugin.getGameManager();
        
        // Calculate remaining countdown
        // This is a rough estimate as we don't track exact countdown time here
        String message = plugin.getFileHandler().getRawMessage("actionbar.frozen",
                "%time%", "?"); // Countdown would need to be passed from GameManager
        
        try {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
                    new TextComponent(message));
        } catch (Exception e) {
            // Fallback for older versions
            player.sendMessage(message);
        }
    }

    /**
     * Checks if a player can move based on game state
     * @param player The player to check
     * @return true if the player can move
     */
    public boolean canMove(Player player) {
        GameManager gameManager = plugin.getGameManager();
        GamePlayer gamePlayer = gameManager.getGamePlayer(player);
        
        if (gamePlayer == null) return true;
        
        if (gameManager.getState() == GameState.STARTING && gamePlayer.isFrozen()) {
            return false;
        }
        
        return true;
    }
}
