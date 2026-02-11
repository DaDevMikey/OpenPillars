package com.openpillars.placeholders;

import com.openpillars.OpenPillars;
import com.openpillars.game.GameManager;
import com.openpillars.game.GamePlayer;
import com.openpillars.game.GameState;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion for OpenPillars.
 * Provides placeholders for scoreboards, holograms, etc.
 */
public class PillarsExpansion extends PlaceholderExpansion {

    private final OpenPillars plugin;

    public PillarsExpansion(OpenPillars plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "openpillars";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        GameManager gameManager = plugin.getGameManager();
        GamePlayer gamePlayer = player != null ? gameManager.getGamePlayer(player) : null;
        
        switch (params.toLowerCase()) {
            // Game State
            case "state":
                return gameManager.getState().name();
            
            case "state_formatted":
                return formatState(gameManager.getState());
            
            // Player Counts
            case "players":
                return String.valueOf(gameManager.getPlayers().size());
            
            case "players_alive":
                return String.valueOf(gameManager.getAliveCount());
            
            case "max_players":
                return String.valueOf(plugin.getConfig().getInt("game.max-players", 16));
            
            case "min_players":
                return String.valueOf(plugin.getConfig().getInt("game.min-players", 2));
            
            case "spectators":
                return String.valueOf(gameManager.getSpectators().size());
            
            // Game Time
            case "time":
                return formatTime(gameManager.getGameTime());
            
            case "time_seconds":
                return String.valueOf(gameManager.getGameTime() / 1000);
            
            // Player Stats (requires player)
            case "kills":
                return gamePlayer != null ? String.valueOf(gamePlayer.getKills()) : "0";
            
            case "blocks_broken":
                return gamePlayer != null ? String.valueOf(gamePlayer.getBlocksBroken()) : "0";
            
            case "blocks_placed":
                return gamePlayer != null ? String.valueOf(gamePlayer.getBlocksPlaced()) : "0";
            
            case "items_collected":
                return gamePlayer != null ? String.valueOf(gamePlayer.getItemsCollected()) : "0";
            
            // Player State
            case "in_game":
                return gameManager.isPlaying(player) ? "true" : "false";
            
            case "spectating":
                return gameManager.isSpectating(player) ? "true" : "false";
            
            case "alive":
                return gamePlayer != null && gamePlayer.isAlive() ? "true" : "false";
            
            case "frozen":
                return gamePlayer != null && gamePlayer.isFrozen() ? "true" : "false";
            
            default:
                return null;
        }
    }

    /**
     * Formats a game state to a readable string
     * @param state The game state
     * @return Formatted state string
     */
    private String formatState(GameState state) {
        switch (state) {
            case LOBBY:
                return "Waiting";
            case STARTING:
                return "Starting";
            case ACTIVE:
                return "In Game";
            case ENDING:
                return "Ending";
            case RESETTING:
                return "Resetting";
            default:
                return state.name();
        }
    }

    /**
     * Formats milliseconds to a time string (MM:SS)
     * @param millis The milliseconds
     * @return Formatted time string
     */
    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
