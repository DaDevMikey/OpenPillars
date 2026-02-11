package com.openpillars.commands;

import com.openpillars.OpenPillars;
import com.openpillars.game.GameManager;
import com.openpillars.game.GameState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main command handler for /pillars command.
 * Handles all sub-commands: join, leave, start, stop, reload, help
 */
public class PillarsCommand implements CommandExecutor, TabCompleter {

    private final OpenPillars plugin;
    private final List<String> subCommands = Arrays.asList(
            "join", "leave", "start", "stop", "reload", "help", "setup"
    );

    public PillarsCommand(OpenPillars plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "join":
                return handleJoin(sender);
            case "leave":
                return handleLeave(sender);
            case "start":
                return handleStart(sender);
            case "stop":
                return handleStop(sender);
            case "reload":
                return handleReload(sender);
            case "setup":
                return handleSetup(sender, args);
            case "help":
            default:
                sendHelp(sender);
                return true;
        }
    }

    /**
     * Handles the join sub-command
     */
    private boolean handleJoin(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getFileHandler().getMessage("general.player-only"));
            return true;
        }

        Player player = (Player) sender;
        
        if (!player.hasPermission("openpillars.command.join")) {
            player.sendMessage(plugin.getFileHandler().getMessage("general.no-permission"));
            return true;
        }

        GameManager gameManager = plugin.getGameManager();

        if (gameManager.isPlaying(player)) {
            player.sendMessage(plugin.getFileHandler().getMessage("commands.join-fail-ingame"));
            return true;
        }

        GameState state = gameManager.getState();
        if (state == GameState.ACTIVE || state == GameState.ENDING) {
            player.sendMessage(plugin.getFileHandler().getMessage("commands.join-fail-started"));
            return true;
        }

        int maxPlayers = plugin.getConfig().getInt("game.max-players", 16);
        if (gameManager.getPlayers().size() >= maxPlayers) {
            player.sendMessage(plugin.getFileHandler().getMessage("commands.join-fail-full"));
            return true;
        }

        if (gameManager.addPlayer(player)) {
            player.sendMessage(plugin.getFileHandler().getMessage("commands.join-success"));
        }

        return true;
    }

    /**
     * Handles the leave sub-command
     */
    private boolean handleLeave(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getFileHandler().getMessage("general.player-only"));
            return true;
        }

        Player player = (Player) sender;
        
        if (!player.hasPermission("openpillars.command.leave")) {
            player.sendMessage(plugin.getFileHandler().getMessage("general.no-permission"));
            return true;
        }

        GameManager gameManager = plugin.getGameManager();

        if (!gameManager.isPlaying(player) && !gameManager.isSpectating(player)) {
            player.sendMessage(plugin.getFileHandler().getMessage("commands.leave-fail"));
            return true;
        }

        gameManager.removePlayer(player);
        player.sendMessage(plugin.getFileHandler().getMessage("commands.leave-success"));

        return true;
    }

    /**
     * Handles the start sub-command
     */
    private boolean handleStart(CommandSender sender) {
        if (!sender.hasPermission("openpillars.command.start")) {
            sender.sendMessage(plugin.getFileHandler().getMessage("general.no-permission"));
            return true;
        }

        GameManager gameManager = plugin.getGameManager();

        if (gameManager.getState() != GameState.LOBBY) {
            sender.sendMessage(plugin.getFileHandler().getMessage("commands.start-fail-running"));
            return true;
        }

        int minPlayers = plugin.getConfig().getInt("game.min-players", 2);
        if (gameManager.getPlayers().size() < minPlayers) {
            sender.sendMessage(plugin.getFileHandler().getMessage("commands.start-fail-notenough"));
            return true;
        }

        gameManager.startCountdown();
        sender.sendMessage(plugin.getFileHandler().getMessage("commands.start-success"));

        return true;
    }

    /**
     * Handles the stop sub-command
     */
    private boolean handleStop(CommandSender sender) {
        if (!sender.hasPermission("openpillars.command.stop")) {
            sender.sendMessage(plugin.getFileHandler().getMessage("general.no-permission"));
            return true;
        }

        GameManager gameManager = plugin.getGameManager();

        if (gameManager.getState() == GameState.LOBBY || gameManager.getState() == GameState.RESETTING) {
            sender.sendMessage(plugin.getFileHandler().getMessage("commands.stop-fail"));
            return true;
        }

        gameManager.stopAllGames();
        sender.sendMessage(plugin.getFileHandler().getMessage("commands.stop-success"));

        return true;
    }

    /**
     * Handles the reload sub-command
     */
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("openpillars.command.reload")) {
            sender.sendMessage(plugin.getFileHandler().getMessage("general.no-permission"));
            return true;
        }

        plugin.reload();
        plugin.getGameManager().getLootManager().loadLootTables();
        sender.sendMessage(plugin.getFileHandler().getMessage("general.config-reloaded"));

        return true;
    }

    /**
     * Handles the setup sub-command
     */
    private boolean handleSetup(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getFileHandler().getMessage("general.player-only"));
            return true;
        }

        Player player = (Player) sender;
        
        if (!player.hasPermission("openpillars.command.setup")) {
            player.sendMessage(plugin.getFileHandler().getMessage("general.no-permission"));
            return true;
        }

        // TODO: Implement arena setup wizard
        player.sendMessage(plugin.getFileHandler().getMessage("setup.mode-enabled"));

        return true;
    }

    /**
     * Sends the help message to a sender
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getFileHandler().getRawMessage("commands.help-header"));
        sender.sendMessage(plugin.getFileHandler().getRawMessage("commands.help-join"));
        sender.sendMessage(plugin.getFileHandler().getRawMessage("commands.help-leave"));
        
        if (sender.hasPermission("openpillars.command.start")) {
            sender.sendMessage(plugin.getFileHandler().getRawMessage("commands.help-start"));
        }
        if (sender.hasPermission("openpillars.command.stop")) {
            sender.sendMessage(plugin.getFileHandler().getRawMessage("commands.help-stop"));
        }
        if (sender.hasPermission("openpillars.command.reload")) {
            sender.sendMessage(plugin.getFileHandler().getRawMessage("commands.help-reload"));
        }
        if (sender.hasPermission("openpillars.command.setup")) {
            sender.sendMessage(plugin.getFileHandler().getRawMessage("commands.help-setup"));
        }
        
        sender.sendMessage(plugin.getFileHandler().getRawMessage("commands.help-footer"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return subCommands.stream()
                    .filter(sub -> sub.startsWith(args[0].toLowerCase()))
                    .filter(sub -> hasPermissionForSubCommand(sender, sub))
                    .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }

    /**
     * Checks if a sender has permission for a sub-command
     */
    private boolean hasPermissionForSubCommand(CommandSender sender, String subCommand) {
        switch (subCommand) {
            case "join":
                return sender.hasPermission("openpillars.command.join");
            case "leave":
                return sender.hasPermission("openpillars.command.leave");
            case "start":
                return sender.hasPermission("openpillars.command.start");
            case "stop":
                return sender.hasPermission("openpillars.command.stop");
            case "reload":
                return sender.hasPermission("openpillars.command.reload");
            case "setup":
                return sender.hasPermission("openpillars.command.setup");
            case "help":
                return true;
            default:
                return false;
        }
    }
}
