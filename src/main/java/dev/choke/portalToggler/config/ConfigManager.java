package dev.choke.portalToggler.config;

import dev.choke.portalToggler.api.PortalTogglerAPI.Action;
import dev.choke.portalToggler.api.PortalTogglerAPI.PortalType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;

public class ConfigManager {
    private final Plugin plugin;
    private FileConfiguration config;
    private File configFile;
    private final String fileName;

    // Snapshot of the portal toggles. YamlConfiguration is not thread-safe, so anything that
    // may run off the main thread (e.g. the public API) reads these instead of the config.
    private volatile boolean netherEntry = true;
    private volatile boolean netherCreation = true;
    private volatile boolean endEntry = true;
    private volatile boolean endCreation = true;

    public ConfigManager(Plugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
        loadConfig();
    }

    public void loadConfig() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        configFile = new File(plugin.getDataFolder(), fileName);
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create " + fileName + " file!", e);
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        refreshPortalCache();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + fileName + " file!", e);
        }
    }

    public void reloadConfig() {
        loadConfig();
    }

    /** Thread-safe: reads the cached value, never the YAML config. */
    public boolean isPortalEnabled(PortalType portal, Action action) {
        return switch (portal) {
            case NETHER -> action == Action.ENTRY ? netherEntry : netherCreation;
            case END -> action == Action.ENTRY ? endEntry : endCreation;
        };
    }

    /** Main thread only: updates the config, the cache and the file on disk. */
    public void setPortalEnabled(PortalType portal, Action action, boolean enabled) {
        config.set(path(portal, action), enabled);
        refreshPortalCache();
        saveConfig();
    }

    private void refreshPortalCache() {
        netherEntry = read(PortalType.NETHER, Action.ENTRY);
        netherCreation = read(PortalType.NETHER, Action.CREATION);
        endEntry = read(PortalType.END, Action.ENTRY);
        endCreation = read(PortalType.END, Action.CREATION);
    }

    private boolean read(PortalType portal, Action action) {
        return config.getBoolean(path(portal, action), true);
    }

    private static String path(PortalType portal, Action action) {
        return "portals." + portal.name().toLowerCase(Locale.ROOT) + "."
                + action.name().toLowerCase(Locale.ROOT) + ".enabled";
    }
}
