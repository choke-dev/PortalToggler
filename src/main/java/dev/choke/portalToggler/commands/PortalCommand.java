package dev.choke.portalToggler.commands;

import dev.choke.portalToggler.PortalToggler;
import dev.choke.portalToggler.api.PortalTogglerAPI.Action;
import dev.choke.portalToggler.api.PortalTogglerAPI.PortalType;
import dev.choke.portalToggler.config.ConfigManager;
import dev.choke.portalToggler.config.MessageManager;
import dev.choke.portalToggler.internal.PortalTogglerAPIImpl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class PortalCommand implements CommandExecutor, TabCompleter {
    private static final String ADMIN_PERMISSION = "portaltoggler.admin";
    private static final String UNLOCK_PERMISSION = "portaltoggler.admin.unlock";

    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final PortalTogglerAPIImpl api;

    public PortalCommand(PortalToggler plugin, PortalTogglerAPIImpl api) {
        this.configManager = plugin.getConfigManager();
        this.messageManager = plugin.getMessages();
        this.api = api;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("unlock")) {
            if (!sender.hasPermission(UNLOCK_PERMISSION)) {
                sendMessage(sender, "no-permission");
                return true;
            }
            if (args.length < 3) {
                sendMessage(sender, "unlock-usage");
                return true;
            }
            handleUnlock(sender, args[1], args[2], args.length > 3 ? args[3] : null);
            return true;
        }

        switch (sub) {
            case "reload", "toggle", "status", "enable", "disable" -> {
                if (!sender.hasPermission(ADMIN_PERMISSION)) {
                    sendMessage(sender, "no-permission");
                    return true;
                }
            }
            default -> {
                sendHelp(sender);
                return true;
            }
        }

        switch (sub) {
            case "reload" -> {
                configManager.reloadConfig();
                messageManager.reloadConfig();
                sendMessage(sender, "reload-success");
            }
            case "status" -> handleStatus(sender, args.length > 1 ? args[1] : null);
            default -> { // toggle, enable, disable
                if (args.length < 3) {
                    sendMessage(sender, "command-usage");
                    return true;
                }
                Boolean target = sub.equals("toggle") ? null : sub.equals("enable");
                handleSet(sender, args[1], args[2], target);
            }
        }
        return true;
    }

    private void handleStatus(CommandSender sender, String portalArg) {
        if (portalArg == null) {
            sendMessage(sender, "portal-status");
            for (PortalType portal : PortalType.values()) {
                showPortalStatus(sender, portal);
            }
            return;
        }

        PortalType portal = parsePortal(portalArg);
        if (portal == null) {
            sendMessage(sender, "invalid-portal-type");
            return;
        }
        showPortalStatus(sender, portal);
    }

    private void showPortalStatus(CommandSender sender, PortalType portal) {
        sendMessage(sender, "portal-status-header", "portal", portalName(portal));
        for (Action action : Action.values()) {
            boolean configEnabled = configManager.isPortalEnabled(portal, action);
            Map<String, String> locks = api.getLocks(portal, action);
            boolean effective = configEnabled && locks.isEmpty();
            sendLine(sender, "portal-status-line",
                    "mode", modeName(sender, action),
                    "effective-status", statusText(effective),
                    "config-status", statusText(configEnabled));
            locks.forEach((owner, reason) -> sendLine(sender, "portal-status-lock",
                    "plugin", escape(owner),
                    "reason", escape(reason)));
        }
    }

    /** @param target {@code true}/{@code false} to enable/disable, {@code null} to toggle */
    private void handleSet(CommandSender sender, String portalArg, String modeArg, Boolean target) {
        Action action = parseAction(modeArg);
        if (action == null) {
            sendMessage(sender, "invalid-mode");
            return;
        }
        PortalType portal = parsePortal(portalArg);
        if (portal == null) {
            sendMessage(sender, "invalid-portal-type");
            return;
        }

        boolean newState = target != null ? target : !configManager.isPortalEnabled(portal, action);
        configManager.setPortalEnabled(portal, action, newState);

        String portalName = portalName(portal);
        String modeName = modeName(sender, action);
        if (!newState) {
            sendMessage(sender, "portal-disabled", "portal", portalName, "mode", modeName);
            return;
        }

        Map<String, String> locks = api.getLocks(portal, action);
        if (locks.isEmpty()) {
            sendMessage(sender, "portal-enabled", "portal", portalName, "mode", modeName);
        } else {
            sendMessage(sender, "portal-enabled-but-locked",
                    "portal", portalName,
                    "mode", modeName,
                    "plugins", joinEscaped(locks.keySet()));
        }
    }

    private void handleUnlock(CommandSender sender, String portalArg, String modeArg, String pluginArg) {
        Action action = parseAction(modeArg);
        if (action == null) {
            sendMessage(sender, "invalid-mode");
            return;
        }
        PortalType portal = parsePortal(portalArg);
        if (portal == null) {
            sendMessage(sender, "invalid-portal-type");
            return;
        }

        Map<String, String> removed = api.adminUnlock(sender, portal, action, pluginArg);
        String portalName = portalName(portal);
        String modeName = modeName(sender, action);
        if (removed.isEmpty()) {
            sendMessage(sender, "unlock-none", "portal", portalName, "mode", modeName);
            return;
        }
        sendMessage(sender, "unlock-success",
                "portal", portalName,
                "mode", modeName,
                "count", String.valueOf(removed.size()),
                "plugins", joinEscaped(removed.keySet()));
    }

    private void sendHelp(CommandSender sender) {
        sendMessage(sender, "help-header");
        sendMessage(sender, "help-reload");
        sendMessage(sender, "help-toggle");
        sendMessage(sender, "help-status");
        sendMessage(sender, "help-enable");
        sendMessage(sender, "help-disable");
        if (sender.hasPermission(UNLOCK_PERMISSION)) {
            sendMessage(sender, "help-unlock");
        }
    }

    private static PortalType parsePortal(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "nether" -> PortalType.NETHER;
            case "end" -> PortalType.END;
            default -> null;
        };
    }

    private static Action parseAction(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "entry" -> Action.ENTRY;
            case "creation" -> Action.CREATION;
            default -> null;
        };
    }

    private static String portalName(PortalType portal) {
        String name = portal.name().toLowerCase(Locale.ROOT);
        return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
    }

    private String modeName(CommandSender sender, Action action) {
        String mode = action.name().toLowerCase(Locale.ROOT);
        Component modeComponent = messageManager.getMessage("mode-" + mode, asPlayer(sender), false, true);
        return modeComponent != null ? PlainTextComponentSerializer.plainText().serialize(modeComponent) : mode;
    }

    private String statusText(boolean enabled) {
        return messageManager.getMessagesConfig().getString(enabled ? "status-enabled" : "status-disabled");
    }

    /** Plugin names and lock reasons come from other plugins; never let them inject MiniMessage tags. */
    private static String escape(String text) {
        return MiniMessage.miniMessage().escapeTags(text);
    }

    private static String joinEscaped(Collection<String> names) {
        return names.stream().map(PortalCommand::escape).collect(Collectors.joining(", "));
    }

    private static Player asPlayer(CommandSender sender) {
        return sender instanceof Player player ? player : null;
    }

    private void sendMessage(CommandSender sender, String key, String... replacements) {
        send(sender, key, true, replacements);
    }

    /** Continuation line of a multi-line output: no prefix. */
    private void sendLine(CommandSender sender, String key, String... replacements) {
        send(sender, key, false, replacements);
    }

    private void send(CommandSender sender, String key, boolean prefix, String... replacements) {
        Component message = messageManager.getMessage(key, asPlayer(sender), prefix, true, replacements);
        if (message != null) {
            sender.sendMessage(message);
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        String sub = args[0].toLowerCase(Locale.ROOT);

        if (args.length == 1) {
            completions.addAll(Arrays.asList("reload", "toggle", "status", "enable", "disable"));
            if (sender.hasPermission(UNLOCK_PERMISSION)) {
                completions.add("unlock");
            }
        } else if (args.length == 2 && List.of("toggle", "enable", "disable", "status", "unlock").contains(sub)) {
            completions.addAll(Arrays.asList("nether", "end"));
        } else if (args.length == 3 && List.of("toggle", "enable", "disable", "unlock").contains(sub)) {
            completions.addAll(Arrays.asList("entry", "creation"));
        } else if (args.length == 4 && sub.equals("unlock") && sender.hasPermission(UNLOCK_PERMISSION)) {
            PortalType portal = parsePortal(args[1]);
            Action action = parseAction(args[2]);
            completions.addAll(portal != null && action != null
                    ? api.getLocks(portal, action).keySet()
                    : api.getAllLockHolders());
        } else {
            return completions;
        }

        String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
        return completions.stream()
                .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(typed))
                .toList();
    }
}
