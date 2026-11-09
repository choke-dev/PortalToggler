package dev.choke.portalToggler.internal;

import dev.choke.portalToggler.api.PortalLockReleasedEvent;
import dev.choke.portalToggler.api.PortalTogglerAPI;
import dev.choke.portalToggler.config.ConfigManager;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Internal implementation of {@link PortalTogglerAPI}. Not part of the public API.
 */
public final class PortalTogglerAPIImpl implements PortalTogglerAPI, Listener {
    private static final int API_VERSION = 1;
    private static final String NO_REASON = "no reason given";

    private record LockKey(PortalType portal, Action action) {
    }

    private final Plugin plugin;
    private final ConfigManager configManager;
    private final Logger logger;
    /** (portal, action) -> (owner plugin name -> reason). All four keys are pre-populated. */
    private final Map<LockKey, ConcurrentHashMap<String, String>> locks = new ConcurrentHashMap<>();

    public PortalTogglerAPIImpl(Plugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.logger = plugin.getLogger();
        for (PortalType portal : PortalType.values()) {
            for (Action action : Action.values()) {
                locks.put(new LockKey(portal, action), new ConcurrentHashMap<>());
            }
        }
    }

    @Override
    public int apiVersion() {
        return API_VERSION;
    }

    @Override
    public boolean isEnabled(@NotNull PortalType portal, @NotNull Action action) {
        Objects.requireNonNull(portal, "portal");
        Objects.requireNonNull(action, "action");
        return configManager.isPortalEnabled(portal, action) && locksFor(portal, action).isEmpty();
    }

    @Override
    public void disable(@NotNull Plugin owner, @NotNull PortalType portal, @NotNull Action action, @Nullable String reason) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(portal, "portal");
        Objects.requireNonNull(action, "action");
        if (!owner.isEnabled()) {
            throw new IllegalStateException("Plugin " + owner.getName() + " is not enabled");
        }

        String ownerName = owner.getName();
        String storedReason = reason == null || reason.isBlank() ? NO_REASON : reason;
        ConcurrentHashMap<String, String> owners = locksFor(portal, action);
        owners.put(ownerName, storedReason);

        // The owner may have been disabled between the check above and the put; if so,
        // its PluginDisableEvent may already have run, so drop the lock rather than leak it.
        if (!owner.isEnabled()) {
            owners.remove(ownerName);
            throw new IllegalStateException("Plugin " + ownerName + " is not enabled");
        }

        logger.info(ownerName + " disabled " + describe(portal, action) + ": " + storedReason);
    }

    @Override
    public boolean release(@NotNull Plugin owner, @NotNull PortalType portal, @NotNull Action action) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(portal, "portal");
        Objects.requireNonNull(action, "action");
        boolean released = locksFor(portal, action).remove(owner.getName()) != null;
        if (released) {
            logger.info(owner.getName() + " released its lock on " + describe(portal, action));
        }
        return released;
    }

    /** @return a sorted snapshot of owner name -> reason for one portal action */
    public Map<String, String> getLocks(PortalType portal, Action action) {
        Map<String, String> snapshot = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        snapshot.putAll(locksFor(portal, action));
        return Collections.unmodifiableMap(snapshot);
    }

    /** @return the names of all plugins currently holding at least one lock */
    public Set<String> getAllLockHolders() {
        Set<String> holders = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        locks.values().forEach(owners -> holders.addAll(owners.keySet()));
        return holders;
    }

    /**
     * Removes locks on behalf of an administrator. Main thread only (fires Bukkit events).
     *
     * @param pluginName only remove this plugin's lock (case-insensitive), or {@code null} for all
     * @return owner name -> reason of every lock that was removed
     */
    public Map<String, String> adminUnlock(CommandSender sender, PortalType portal, Action action, @Nullable String pluginName) {
        ConcurrentHashMap<String, String> owners = locksFor(portal, action);
        Map<String, String> removed = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, String> entry : owners.entrySet()) {
            String owner = entry.getKey();
            String reason = entry.getValue();
            if (pluginName != null && !owner.equalsIgnoreCase(pluginName)) {
                continue;
            }
            // Conditional remove: if the owner re-locked concurrently, leave the new lock alone.
            if (owners.remove(owner, reason)) {
                removed.put(owner, reason);
            }
        }

        for (Map.Entry<String, String> entry : removed.entrySet()) {
            logger.info(sender.getName() + " unlocked " + describe(portal, action)
                    + ": removed lock held by " + entry.getKey() + " (reason: " + entry.getValue() + ")");
            plugin.getServer().getPluginManager().callEvent(new PortalLockReleasedEvent(
                    entry.getKey(), portal, action, entry.getValue(),
                    PortalLockReleasedEvent.Reason.ADMIN, sender));
        }
        return removed;
    }

    /** Releases every lock held by the given plugin. */
    public void releaseAll(String ownerName) {
        for (Map.Entry<LockKey, ConcurrentHashMap<String, String>> entry : locks.entrySet()) {
            if (entry.getValue().remove(ownerName) != null) {
                LockKey key = entry.getKey();
                logger.info("Released " + ownerName + "'s lock on " + describe(key.portal(), key.action())
                        + " (plugin disabled)");
            }
        }
    }

    /** Clears all locks; used when PortalToggler itself shuts down. */
    public void clear() {
        locks.values().forEach(Map::clear);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent event) {
        releaseAll(event.getPlugin().getName());
    }

    private ConcurrentHashMap<String, String> locksFor(PortalType portal, Action action) {
        return locks.get(new LockKey(portal, action));
    }

    private static String describe(PortalType portal, Action action) {
        return portal.name().toLowerCase(Locale.ROOT) + " portal " + action.name().toLowerCase(Locale.ROOT);
    }
}
