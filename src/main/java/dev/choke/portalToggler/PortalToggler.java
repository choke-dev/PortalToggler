package dev.choke.portalToggler;

import dev.choke.portalToggler.commands.PortalCommand;
import dev.choke.portalToggler.config.ConfigManager;
import dev.choke.portalToggler.config.MessageManager;
import dev.choke.portalToggler.listeners.PortalManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class PortalToggler extends JavaPlugin {
    private ConfigManager configManager;
    private MessageManager messageManager;
    private static PortalToggler instance;

    public static PortalToggler getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveResource("messages.yml", false);

        configManager = new ConfigManager(this, "config.yml");

        configManager.loadConfig();

        messageManager = new MessageManager(this);

        PortalManager portalManager = new PortalManager(this);
        getServer().getPluginManager().registerEvents(portalManager, this);

        Objects.requireNonNull(getCommand("portal")).setExecutor(new PortalCommand(this));

        getLogger().info("Enabled " + getName() + " v" + getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        // Save configurations before shutdown
        if (configManager != null) {
            configManager.saveConfig();
        }
        
        // Plugin shutdown logic
        getLogger().info("PortalToggler has been disabled!");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessages() {
        return messageManager;
    }

    public boolean isDebugEnabled() {
        return configManager.getConfig().getBoolean("debug", false);
    }

    public void debugLog(String message) {
        if (isDebugEnabled()) {
            getLogger().info(message);
        }
    }
}
