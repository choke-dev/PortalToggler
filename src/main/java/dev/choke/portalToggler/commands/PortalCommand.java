package dev.choke.portalToggler.commands;

import dev.choke.portalToggler.PortalToggler;
import dev.choke.portalToggler.config.ConfigManager;
import dev.choke.portalToggler.config.MessageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PortalCommand implements CommandExecutor, TabCompleter {
    private final ConfigManager configManager;
    private final MessageManager messageManager;

    public PortalCommand(PortalToggler plugin) {
        this.configManager = plugin.getConfigManager();
        this.messageManager = plugin.getMessages();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("portaltoggler.admin")) {
                    sendMessage(sender, "no-permission");
                    return true;
                }
                configManager.reloadConfig();
                messageManager.reloadConfig();
                sendMessage(sender, "reload-success");
                return true;

            case "toggle":
                if (!sender.hasPermission("portaltoggler.admin")) {
                    sendMessage(sender, "no-permission");
                    return true;
                }
                if (args.length < 3) {
                    sendMessage(sender, "command-usage");
                    return true;
                }
                handleToggle(sender, args[1], args[2]);
                return true;

            case "status":
                if (!sender.hasPermission("portaltoggler.admin")) {
                    sendMessage(sender, "no-permission");
                    return true;
                }
                handleStatus(sender, args.length > 1 ? args[1] : null);
                return true;

            case "enable":
            case "disable":
                if (!sender.hasPermission("portaltoggler.admin")) {
                    sendMessage(sender, "no-permission");
                    return true;
                }
                if (args.length < 3) {
                    sendMessage(sender, "command-usage");
                    return true;
                }
                handleEnableDisable(sender, args[0], args[1], args[2]);
                return true;

            default:
                sendHelp(sender);
                return true;
        }
    }

    private void handleStatus(CommandSender sender, String portalType) {
        if (portalType == null) {
            // Show status for all portals
            sendMessage(sender, "portal-status");
            showPortalStatus(sender, "nether");
            showPortalStatus(sender, "end");
            return;
        }

        // Show status for specific portal
        switch (portalType.toLowerCase()) {
            case "nether":
            case "end":
                showPortalStatus(sender, portalType.toLowerCase());
                break;
            default:
                sendMessage(sender, "invalid-portal-type");
                break;
        }
    }

    private void showPortalStatus(CommandSender sender, String portalType) {
        boolean entryEnabled = configManager.getConfig().getBoolean("portals." + portalType + ".entry.enabled", true);
        boolean creationEnabled = configManager.getConfig().getBoolean("portals." + portalType + ".creation.enabled", true);
        String portalName = portalType.substring(0, 1).toUpperCase() + portalType.substring(1);
        String entryStatus = messageManager.getMessagesConfig().getString(entryEnabled ? "status-enabled" : "status-disabled");
        String creationStatus = messageManager.getMessagesConfig().getString(creationEnabled ? "status-enabled" : "status-disabled");
        sendMessage(sender, "portal-status-single", 
            "portal", portalName,
            "entry-status", entryStatus,
            "creation-status", creationStatus);
    }

    private void handleEnableDisable(CommandSender sender, String action, String portalType, String mode) {
        if (mode == null) {
            sendMessage(sender, "command-usage");
            return;
        }

        // Validate mode
        if (!mode.equalsIgnoreCase("entry") && !mode.equalsIgnoreCase("creation")) {
            sendMessage(sender, "invalid-mode");
            return;
        }

        boolean enable = action.equalsIgnoreCase("enable");
        String portalName = portalType.substring(0, 1).toUpperCase() + portalType.substring(1);
        String modeKey = "mode-" + mode.toLowerCase();
        Component modeComponent = messageManager.getMessage(modeKey, sender instanceof Player ? (Player) sender : null, false, true);
        String modeName = modeComponent != null ? PlainTextComponentSerializer.plainText().serialize(modeComponent) : mode;
        
        switch (portalType.toLowerCase()) {
            case "nether":
                configManager.getConfig().set("portals.nether." + mode + ".enabled", enable);
                configManager.saveConfig();
                sendMessage(sender, enable ? "portal-enabled" : "portal-disabled",
                    "portal", portalName,
                    "mode", modeName);
                break;
            case "end":
                configManager.getConfig().set("portals.end." + mode + ".enabled", enable);
                configManager.saveConfig();
                sendMessage(sender, enable ? "portal-enabled" : "portal-disabled",
                    "portal", portalName,
                    "mode", modeName);
                break;
            default:
                sendMessage(sender, "invalid-portal-type");
                break;
        }
    }

    private void handleToggle(CommandSender sender, String portalType, String mode) {
        if (mode == null) {
            sendMessage(sender, "command-usage");
            return;
        }

        // Validate mode
        if (!mode.equalsIgnoreCase("entry") && !mode.equalsIgnoreCase("creation")) {
            sendMessage(sender, "invalid-mode");
            return;
        }

        switch (portalType.toLowerCase()) {
            case "nether":
                togglePortal(sender, "nether", mode);
                break;
            case "end":
                togglePortal(sender, "end", mode);
                break;
            default:
                sendMessage(sender, "invalid-portal-type");
                break;
        }
    }

    private void togglePortal(CommandSender sender, String portalType, String mode) {
        boolean currentState = configManager.getConfig().getBoolean("portals." + portalType + "." + mode + ".enabled", true);
        boolean newState = !currentState;
        configManager.getConfig().set("portals." + portalType + "." + mode + ".enabled", newState);
        configManager.saveConfig();
        String portalName = portalType.substring(0, 1).toUpperCase() + portalType.substring(1);
        String modeKey = "mode-" + mode.toLowerCase();
        Component modeComponent = messageManager.getMessage(modeKey, sender instanceof Player ? (Player) sender : null, false, true);
        String modeName = modeComponent != null ? PlainTextComponentSerializer.plainText().serialize(modeComponent) : mode;
        sendMessage(sender, newState ? "portal-enabled" : "portal-disabled",
            "portal", portalName,
            "mode", modeName);
    }

    private void sendHelp(CommandSender sender) {
        sendMessage(sender, "help-header");
        sendMessage(sender, "help-reload");
        sendMessage(sender, "help-toggle");
        sendMessage(sender, "help-status");
        sendMessage(sender, "help-enable");
        sendMessage(sender, "help-disable");
    }

    private void sendMessage(CommandSender sender, String key, String... replacements) {
        Player player = sender instanceof Player ? (Player) sender : null;
        Component message = messageManager.getMessage(key, player, true, true, replacements);
        if (message != null) {
            sender.sendMessage(message);
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("reload", "toggle", "status", "enable", "disable"));
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("toggle") || 
                               args[0].equalsIgnoreCase("enable") || 
                               args[0].equalsIgnoreCase("disable") ||
                               args[0].equalsIgnoreCase("status"))) {
            completions.addAll(Arrays.asList("nether", "end"));
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }

        if (args.length == 3 && (args[0].equalsIgnoreCase("toggle") || 
                               args[0].equalsIgnoreCase("enable") || 
                               args[0].equalsIgnoreCase("disable"))) {
            completions.addAll(Arrays.asList("entry", "creation"));
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                    .toList();
        }

        return completions;
    }
} 