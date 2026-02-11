package com.openpillars.game;

import com.openpillars.OpenPillars;
import com.openpillars.events.GameEndEvent;
import com.openpillars.events.GameStartEvent;
import com.openpillars.events.GameStateChangeEvent;
import com.openpillars.util.FileHandler;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the game loop and state transitions.
 * Handles player management, countdown, and game flow.
 */
public class GameManager {

    private final OpenPillars plugin;
    private final Map<UUID, GamePlayer> players;
    private final Set<UUID> spectators;
    private final PillarGenerator pillarGenerator;
    private final LootManager lootManager;
    
    private GameState state;
    private BukkitTask countdownTask;
    private BukkitTask gameTask;
    private BukkitTask generationTask;
    private int countdown;
    private long gameStartTime;

    public GameManager(OpenPillars plugin) {
        this.plugin = plugin;
        this.players = new ConcurrentHashMap<>();
        this.spectators = ConcurrentHashMap.newKeySet();
        this.pillarGenerator = new PillarGenerator(plugin);
        this.lootManager = new LootManager(plugin);
        this.state = GameState.LOBBY;
    }

    /**
     * Adds a player to the game
     * @param player The player to add
     * @return true if player was added successfully
     */
    public boolean addPlayer(Player player) {
        if (state != GameState.LOBBY && state != GameState.STARTING) {
            return false;
        }
        
        int maxPlayers = plugin.getConfig().getInt("game.max-players", 16);
        if (players.size() >= maxPlayers) {
            return false;
        }
        
        GamePlayer gamePlayer = new GamePlayer(player);
        players.put(player.getUniqueId(), gamePlayer);
        
        // Broadcast join message
        String message = plugin.getFileHandler().getMessage("game.player-joined",
                "%player%", player.getName(),
                "%players%", String.valueOf(players.size()),
                "%max_players%", String.valueOf(maxPlayers));
        broadcastMessage(message);
        
        // Check if we can start countdown
        checkStartCountdown();
        
        return true;
    }

    /**
     * Removes a player from the game
     * @param player The player to remove
     */
    public void removePlayer(Player player) {
        GamePlayer gamePlayer = players.remove(player.getUniqueId());
        spectators.remove(player.getUniqueId());
        
        if (gamePlayer != null) {
            // Restore player state
            player.setGameMode(GameMode.SURVIVAL);
            player.getInventory().clear();
            
            int maxPlayers = plugin.getConfig().getInt("game.max-players", 16);
            String message = plugin.getFileHandler().getMessage("game.player-left",
                    "%player%", player.getName(),
                    "%players%", String.valueOf(players.size()),
                    "%max_players%", String.valueOf(maxPlayers));
            broadcastMessage(message);
            
            // Check game state
            if (state == GameState.STARTING && players.size() < getMinPlayers()) {
                cancelCountdown();
            } else if (state == GameState.ACTIVE) {
                checkWinCondition();
            }
        }
    }

    /**
     * Makes a player a spectator
     * @param player The player to make spectator
     */
    public void makeSpectator(Player player) {
        GamePlayer gamePlayer = players.get(player.getUniqueId());
        if (gamePlayer != null) {
            gamePlayer.setAlive(false);
        }
        
        spectators.add(player.getUniqueId());
        player.setGameMode(GameMode.SPECTATOR);
        
        player.sendMessage(plugin.getFileHandler().getMessage("game.now-spectating"));
        
        checkWinCondition();
    }

    /**
     * Checks if countdown should start
     */
    private void checkStartCountdown() {
        if (state != GameState.LOBBY) return;
        
        if (players.size() >= getMinPlayers()) {
            startCountdown();
        } else {
            String message = plugin.getFileHandler().getMessage("game.waiting-for-players",
                    "%players%", String.valueOf(players.size()),
                    "%min_players%", String.valueOf(getMinPlayers()));
            broadcastMessage(message);
        }
    }

    /**
     * Starts the countdown to game start
     */
    public void startCountdown() {
        if (state != GameState.LOBBY) return;
        
        setState(GameState.STARTING);
        countdown = plugin.getConfig().getInt("game.countdown", 10);
        
        // Teleport players to pillars and freeze them
        teleportPlayersToPillars();
        
        String message = plugin.getFileHandler().getMessage("game.countdown-start",
                "%time%", String.valueOf(countdown));
        broadcastMessage(message);
        
        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (countdown <= 0) {
                    startGame();
                    this.cancel();
                    return;
                }
                
                if (countdown <= 5) {
                    // Show title for last 5 seconds
                    String title = plugin.getFileHandler().getRawMessage("titles.countdown.title",
                            "%time%", String.valueOf(countdown));
                    String subtitle = plugin.getFileHandler().getRawMessage("titles.countdown.subtitle");
                    
                    for (UUID uuid : players.keySet()) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            player.sendTitle(title, subtitle, 0, 20, 0);
                            playSound(player, "countdown-tick");
                        }
                    }
                }
                
                String tickMessage = plugin.getFileHandler().getMessage("game.countdown-tick",
                        "%time%", String.valueOf(countdown));
                broadcastMessage(tickMessage);
                
                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Cancels the countdown
     */
    public void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        
        setState(GameState.LOBBY);
        
        String message = plugin.getFileHandler().getMessage("game.countdown-cancelled");
        broadcastMessage(message);
        
        // Teleport players back to lobby
        // TODO: Implement lobby teleport
    }

    /**
     * Teleports all players to their assigned pillars
     */
    private void teleportPlayersToPillars() {
        int pillarIndex = 0;
        int spacing = plugin.getConfig().getInt("game.pillar-spacing", 10);
        int startY = plugin.getConfig().getInt("game.pillar-start-y", 64);
        
        // Generate pillars in a circle pattern
        int playerCount = players.size();
        double angleStep = (2 * Math.PI) / playerCount;
        int radius = (playerCount * spacing) / (2 * (int) Math.PI);
        radius = Math.max(radius, spacing); // Minimum radius
        
        Location center = getGameCenter();
        
        for (UUID uuid : players.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            
            // Calculate pillar position
            double angle = angleStep * pillarIndex;
            int x = (int) (center.getX() + radius * Math.cos(angle));
            int z = (int) (center.getZ() + radius * Math.sin(angle));
            
            Location pillarLocation = new Location(center.getWorld(), x, startY, z);
            
            // Generate initial pillar
            pillarGenerator.generateInitialPillar(pillarLocation, uuid);
            
            // Teleport player to top of pillar
            Location spawnLoc = pillarLocation.clone().add(0.5, 
                    plugin.getConfig().getInt("pillar.pillar-initial-height", 5), 0.5);
            player.teleport(spawnLoc);
            
            // Store spawn location for freeze logic
            GamePlayer gamePlayer = players.get(uuid);
            if (gamePlayer != null) {
                gamePlayer.setSpawnLocation(spawnLoc);
                gamePlayer.setFrozen(true);
            }
            
            pillarIndex++;
        }
    }

    /**
     * Starts the actual game
     */
    public void startGame() {
        setState(GameState.ACTIVE);
        gameStartTime = System.currentTimeMillis();
        
        // Fire game start event
        GameStartEvent event = new GameStartEvent(this);
        Bukkit.getPluginManager().callEvent(event);
        
        // Unfreeze players
        for (UUID uuid : players.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            GamePlayer gamePlayer = players.get(uuid);
            
            if (player != null && gamePlayer != null) {
                gamePlayer.setFrozen(false);
                
                // Show start title
                String title = plugin.getFileHandler().getRawMessage("titles.game-start.title");
                String subtitle = plugin.getFileHandler().getRawMessage("titles.game-start.subtitle");
                player.sendTitle(title, subtitle, 10, 40, 20);
                
                playSound(player, "game-start");
            }
        }
        
        String message = plugin.getFileHandler().getMessage("game.game-started");
        broadcastMessage(message);
        
        // Start pillar generation task
        startGenerationTask();
        
        // Start game timer
        int gameDuration = plugin.getConfig().getInt("game.game-duration", 15);
        if (gameDuration > 0) {
            gameTask = new BukkitRunnable() {
                @Override
                public void run() {
                    endGame(null); // Time's up, no winner
                }
            }.runTaskLater(plugin, gameDuration * 60 * 20L);
        }
    }

    /**
     * Starts the pillar block generation task
     */
    private void startGenerationTask() {
        int interval = plugin.getConfig().getInt("pillar.generation-interval", 40);
        
        generationTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state != GameState.ACTIVE) {
                    this.cancel();
                    return;
                }
                
                // Generate blocks for all alive players asynchronously
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    for (UUID uuid : players.keySet()) {
                        GamePlayer gamePlayer = players.get(uuid);
                        if (gamePlayer != null && gamePlayer.isAlive()) {
                            // Schedule sync task for block placement
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                pillarGenerator.generateBlock(uuid, lootManager);
                            });
                        }
                    }
                });
            }
        }.runTaskTimer(plugin, interval, interval);
    }

    /**
     * Ends the game with an optional winner
     * @param winner The winner, or null if no winner
     */
    public void endGame(Player winner) {
        if (state == GameState.ENDING || state == GameState.RESETTING) return;
        
        setState(GameState.ENDING);
        
        // Cancel tasks
        if (gameTask != null) {
            gameTask.cancel();
            gameTask = null;
        }
        if (generationTask != null) {
            generationTask.cancel();
            generationTask = null;
        }
        
        // Fire game end event
        GameEndEvent event = new GameEndEvent(this, winner);
        Bukkit.getPluginManager().callEvent(event);
        
        if (winner != null) {
            // Announce winner
            String message = plugin.getFileHandler().getMessage("game.game-winner",
                    "%player%", winner.getName());
            broadcastMessage(message);
            
            // Show winner title
            String title = plugin.getFileHandler().getRawMessage("titles.winner.title");
            String subtitle = plugin.getFileHandler().getRawMessage("titles.winner.subtitle");
            winner.sendTitle(title, subtitle, 10, 100, 20);
            playSound(winner, "game-win");
        } else {
            String message = plugin.getFileHandler().getMessage("game.game-draw");
            broadcastMessage(message);
        }
        
        // Reset after delay
        new BukkitRunnable() {
            @Override
            public void run() {
                resetGame();
            }
        }.runTaskLater(plugin, 100L); // 5 seconds
    }

    /**
     * Checks if there's a winner
     */
    private void checkWinCondition() {
        if (state != GameState.ACTIVE) return;
        
        List<UUID> alivePlayers = new ArrayList<>();
        for (Map.Entry<UUID, GamePlayer> entry : players.entrySet()) {
            if (entry.getValue().isAlive()) {
                alivePlayers.add(entry.getKey());
            }
        }
        
        if (alivePlayers.size() <= 1) {
            Player winner = alivePlayers.isEmpty() ? null : Bukkit.getPlayer(alivePlayers.get(0));
            endGame(winner);
        }
    }

    /**
     * Resets the game to lobby state
     */
    public void resetGame() {
        setState(GameState.RESETTING);
        
        // Clear pillars
        pillarGenerator.clearAllPillars();
        
        // Reset all players
        for (UUID uuid : new HashSet<>(players.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.setGameMode(GameMode.SURVIVAL);
                player.getInventory().clear();
                player.setHealth(player.getMaxHealth());
                player.setFoodLevel(20);
                // TODO: Teleport to lobby
            }
        }
        
        players.clear();
        spectators.clear();
        
        setState(GameState.LOBBY);
    }

    /**
     * Force stops all games
     */
    public void stopAllGames() {
        if (countdownTask != null) countdownTask.cancel();
        if (gameTask != null) gameTask.cancel();
        if (generationTask != null) generationTask.cancel();
        
        resetGame();
    }

    /**
     * Sets the game state and fires event
     * @param newState The new state
     */
    private void setState(GameState newState) {
        GameState oldState = this.state;
        this.state = newState;
        
        GameStateChangeEvent event = new GameStateChangeEvent(oldState, newState);
        Bukkit.getPluginManager().callEvent(event);
    }

    /**
     * Broadcasts a message to all players in the game
     * @param message The message to broadcast
     */
    public void broadcastMessage(String message) {
        for (UUID uuid : players.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(message);
            }
        }
        for (UUID uuid : spectators) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && !players.containsKey(uuid)) {
                player.sendMessage(message);
            }
        }
    }

    /**
     * Plays a sound from config
     * @param player The player to play sound to
     * @param soundKey The config key for the sound
     */
    private void playSound(Player player, String soundKey) {
        if (!plugin.getConfig().getBoolean("sounds.enabled", true)) return;
        
        String soundName = plugin.getConfig().getString("sounds." + soundKey);
        if (soundName != null) {
            try {
                Sound sound = Sound.valueOf(soundName);
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid sound: " + soundName);
            }
        }
    }

    /**
     * Gets the center location for the game
     * @return The center location
     */
    private Location getGameCenter() {
        String worldName = plugin.getConfig().getString("world.world-name", "world");
        return new Location(Bukkit.getWorld(worldName), 0, 64, 0);
    }

    /**
     * Gets the minimum players required
     * @return The minimum player count
     */
    private int getMinPlayers() {
        return plugin.getConfig().getInt("game.min-players", 2);
    }

    // Getters
    public GameState getState() {
        return state;
    }

    public Map<UUID, GamePlayer> getPlayers() {
        return players;
    }

    public Set<UUID> getSpectators() {
        return spectators;
    }

    public GamePlayer getGamePlayer(UUID uuid) {
        return players.get(uuid);
    }

    public GamePlayer getGamePlayer(Player player) {
        return players.get(player.getUniqueId());
    }

    public boolean isPlaying(Player player) {
        return players.containsKey(player.getUniqueId());
    }

    public boolean isSpectating(Player player) {
        return spectators.contains(player.getUniqueId());
    }

    public int getAliveCount() {
        return (int) players.values().stream().filter(GamePlayer::isAlive).count();
    }

    public long getGameTime() {
        if (state != GameState.ACTIVE) return 0;
        return System.currentTimeMillis() - gameStartTime;
    }

    public PillarGenerator getPillarGenerator() {
        return pillarGenerator;
    }

    public LootManager getLootManager() {
        return lootManager;
    }
}
