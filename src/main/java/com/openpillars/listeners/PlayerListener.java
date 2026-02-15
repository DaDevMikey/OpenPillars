package com.openpillars.listeners;

import com.openpillars.OpenPillars;
import com.openpillars.events.PlayerEliminatedEvent;
import com.openpillars.game.GameManager;
import com.openpillars.game.GamePlayer;
import com.openpillars.game.GameState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Handles player-related events like death, damage, join/quit.
 */
public class PlayerListener implements Listener {

    private final OpenPillars plugin;

    public PlayerListener(OpenPillars plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        GameManager gameManager = plugin.getGameManager();
        
        if (!gameManager.isPlaying(player)) return;
        if (gameManager.getState() != GameState.ACTIVE) return;
        
        GamePlayer gamePlayer = gameManager.getGamePlayer(player);
        if (gamePlayer == null || !gamePlayer.isAlive()) return;
        
        // Get killer if applicable
        Player killer = player.getKiller();
        PlayerEliminatedEvent.EliminationCause cause = killer != null 
                ? PlayerEliminatedEvent.EliminationCause.KILLED 
                : PlayerEliminatedEvent.EliminationCause.OTHER;
        
        // Check for void death
        EntityDamageEvent lastDamage = player.getLastDamageCause();
        if (lastDamage != null && lastDamage.getCause() == EntityDamageEvent.DamageCause.VOID) {
            cause = PlayerEliminatedEvent.EliminationCause.VOID;
        }
        
        // Fire elimination event
        PlayerEliminatedEvent eliminatedEvent = new PlayerEliminatedEvent(
                player, gamePlayer, cause, killer);
        Bukkit.getPluginManager().callEvent(eliminatedEvent);
        
        if (eliminatedEvent.isCancelled()) {
            // PlayerDeathEvent is not cancellable, restore health instead
            player.setHealth(player.getMaxHealth());
            return;
        }
        
        // Clear drops in game
        event.getDrops().clear();
        event.setDroppedExp(0);
        
        // Broadcast death message
        String deathMessage;
        if (killer != null) {
            deathMessage = plugin.getFileHandler().getMessage("game.player-killed",
                    "%player%", player.getName(),
                    "%killer%", killer.getName());
            
            // Give kill to killer
            GamePlayer killerPlayer = gameManager.getGamePlayer(killer);
            if (killerPlayer != null) {
                killerPlayer.addKill();
            }
        } else if (cause == PlayerEliminatedEvent.EliminationCause.VOID) {
            deathMessage = plugin.getFileHandler().getMessage("game.player-died-void",
                    "%player%", player.getName());
        } else {
            deathMessage = plugin.getFileHandler().getMessage("game.player-died",
                    "%player%", player.getName());
        }
        
        event.setDeathMessage(null); // Remove vanilla death message
        gameManager.broadcastMessage(deathMessage);
        
        // Show death title
        String title = plugin.getFileHandler().getRawMessage("titles.death.title");
        String subtitle = plugin.getFileHandler().getRawMessage("titles.death.subtitle");
        com.openpillars.util.FileHandler.sendTitle(player, title, subtitle, 10, 60, 20);
        
        // Make spectator
        gameManager.makeSpectator(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        GameManager gameManager = plugin.getGameManager();
        
        if (!gameManager.isPlaying(player)) return;
        
        // Prevent damage during countdown
        if (gameManager.getState() == GameState.STARTING) {
            event.setCancelled(true);
            return;
        }
        
        // Handle void damage with grace period
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            int gracePeriod = plugin.getConfig().getInt("game.void-grace-period", 3);
            
            if (gracePeriod > 0) {
                event.setCancelled(true);
                
                // Warn player
                player.sendMessage(plugin.getFileHandler().getMessage("game.void-warning"));
                
                // Teleport back to spawn after grace period
                GamePlayer gamePlayer = gameManager.getGamePlayer(player);
                if (gamePlayer != null) {
                    Location spawnLoc = gamePlayer.getSpawnLocation();
                    if (spawnLoc != null) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (player.isOnline() && gamePlayer.isAlive()) {
                                    player.teleport(spawnLoc);
                                    player.sendMessage(plugin.getFileHandler().getMessage("game.void-saved"));
                                }
                            }
                        }.runTaskLater(plugin, gracePeriod * 20L);
                    }
                }
            } else if (gracePeriod == -1) {
                // Instant spectator mode (no death)
                event.setCancelled(true);
                gameManager.makeSpectator(player);
            }
            // gracePeriod == 0 means normal death
        }
    }

    @EventHandler
    public void onPlayerDamageByPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getDamager() instanceof Player)) return;
        
        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();
        GameManager gameManager = plugin.getGameManager();
        
        // Check if both players are in the same game
        boolean victimInGame = gameManager.isPlaying(victim);
        boolean attackerInGame = gameManager.isPlaying(attacker);
        
        if (victimInGame != attackerInGame) {
            // One is in game, one isn't - prevent damage
            event.setCancelled(true);
            return;
        }
        
        // Prevent PvP during non-active states
        if (victimInGame && gameManager.getState() != GameState.ACTIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        GameManager gameManager = plugin.getGameManager();
        
        if (gameManager.isPlaying(player) || gameManager.isSpectating(player)) {
            GamePlayer gamePlayer = gameManager.getGamePlayer(player);
            
            if (gamePlayer != null && gamePlayer.getSpawnLocation() != null) {
                // Respawn at their pillar (as spectator)
                event.setRespawnLocation(gamePlayer.getSpawnLocation());
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        GameManager gameManager = plugin.getGameManager();
        
        if (gameManager.isPlaying(player)) {
            GamePlayer gamePlayer = gameManager.getGamePlayer(player);
            
            // Fire elimination event for disconnect
            if (gamePlayer != null && gamePlayer.isAlive() && 
                    gameManager.getState() == GameState.ACTIVE) {
                
                PlayerEliminatedEvent eliminatedEvent = new PlayerEliminatedEvent(
                        player, gamePlayer, 
                        PlayerEliminatedEvent.EliminationCause.DISCONNECT, null);
                Bukkit.getPluginManager().callEvent(eliminatedEvent);
            }
            
            // Remove from game
            gameManager.removePlayer(player);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Could implement reconnection logic here in the future
    }
}
