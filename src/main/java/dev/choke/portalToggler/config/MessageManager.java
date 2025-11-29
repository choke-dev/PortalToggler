package dev.choke.portalToggler.config;

import dev.choke.portalToggler.PortalToggler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MessageManager {
    private final PortalToggler plugin;
    private final File messagesFile;
    private FileConfiguration messagesConfig;
    private final MiniMessage miniMessage;
    private String prefix;
    private final Map<String, Map<UUID, Long>> messageCooldowns;
    private static final long DEFAULT_COOLDOWN = 10000; // 10 seconds in milliseconds

    public MessageManager(PortalToggler plugin) {
        this.plugin = plugin;
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        this.miniMessage = MiniMessage.miniMessage();
        this.messageCooldowns = new HashMap<>();
        loadMessages();
    }

    public void loadMessages() {
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // Load default messages from resource
        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            messagesConfig.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8)));
        }

        // Load prefix
        prefix = messagesConfig.getString("prefix", "<dark_gray>[<aqua>PortalToggler<dark_gray>]");
    }

    public Component getMessage(String key, Player player, boolean includePrefix, boolean bypassCooldown, String... replacements) {
        if (player != null && !bypassCooldown && isOnCooldown(player, key)) {
            return null;
        }

        String message = messagesConfig.getString(key);
        if (message == null) {
            plugin.getLogger().warning("Message key '" + key + "' not found in messages.yml");
            return miniMessage.deserialize("<red>Message not found: " + key);
        }

        // Apply replacements
        if (replacements != null && replacements.length > 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                if (i + 1 < replacements.length) {
                    message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
                }
            }
        }

        // Add prefix if requested
        Component component = miniMessage.deserialize(includePrefix && prefix != null && !prefix.isEmpty() ? prefix + " " + message : message);
        
        // Set cooldown if player is provided and cooldown is not bypassed
        if (player != null && !bypassCooldown) {
            setCooldown(player, key);
        }
        
        return component;
    }

    public Component getMessage(String key, Player player, boolean includePrefix, String... replacements) {
        return getMessage(key, player, includePrefix, false, replacements);
    }

    public Component getMessage(String key, Player player, String... replacements) {
        return getMessage(key, player, true, false, replacements);
    }

    private boolean isOnCooldown(Player player, String messageKey) {
        Map<UUID, Long> playerCooldowns = messageCooldowns.get(messageKey);
        if (playerCooldowns == null) return false;

        Long cooldownEnd = playerCooldowns.get(player.getUniqueId());
        if (cooldownEnd == null) return false;

        if (cooldownEnd <= System.currentTimeMillis()) {
            playerCooldowns.remove(player.getUniqueId());
            if (playerCooldowns.isEmpty()) {
                messageCooldowns.remove(messageKey);
            }
            return false;
        }
        return true;
    }

    private void setCooldown(Player player, String messageKey) {
        messageCooldowns.computeIfAbsent(messageKey, k -> new HashMap<>())
                .put(player.getUniqueId(), System.currentTimeMillis() + DEFAULT_COOLDOWN);
    }

    public void reloadConfig() {
        loadMessages();
        messageCooldowns.clear();
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }
} 