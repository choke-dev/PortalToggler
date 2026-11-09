package dev.choke.portalToggler.api;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Public API for PortalToggler.
 *
 * <p>Obtain it through the Bukkit {@link org.bukkit.plugin.ServicesManager}:
 * <pre>{@code
 * RegisteredServiceProvider<PortalTogglerAPI> rsp =
 *         Bukkit.getServicesManager().getRegistration(PortalTogglerAPI.class);
 * if (rsp != null) {
 *     PortalTogglerAPI api = rsp.getProvider();
 *     api.disable(this, PortalTogglerAPI.PortalType.END, PortalTogglerAPI.Action.ENTRY, "Event running");
 * }
 * }</pre>
 *
 * <p>Locks placed through {@link #disable} are held per owning plugin, kept in memory only
 * (never written to the config), and released automatically when the owning plugin is disabled.
 * A portal action is effectively enabled only when the config allows it <em>and</em> no plugin
 * holds a lock on it. All methods are thread-safe.
 */
public interface PortalTogglerAPI {

    /** Which portal a lock or query refers to. */
    enum PortalType {
        NETHER,
        END
    }

    /** Which portal behaviour a lock or query refers to. */
    enum Action {
        /** Entities and players travelling through the portal. */
        ENTRY,
        /** Lighting a nether portal / inserting eyes of ender into an end portal frame. */
        CREATION
    }

    /**
     * @return the API version implemented by the running PortalToggler. Incremented when
     *         methods are added, so callers can feature-check at runtime.
     */
    int apiVersion();

    /**
     * @return {@code true} if the config enables this portal action and no plugin currently
     *         holds a lock on it.
     */
    boolean isEnabled(@NotNull PortalType portal, @NotNull Action action);

    /**
     * Disables a portal action on behalf of {@code owner}. Calling again with the same owner,
     * portal and action replaces the reason. The lock lasts until {@link #release} is called
     * or {@code owner} is disabled. An administrator may also remove it with
     * {@code /portal unlock}, in which case a {@link PortalLockReleasedEvent} is fired;
     * calling this method again re-applies the lock.
     *
     * @param owner  the plugin holding the lock; must be enabled
     * @param reason human-readable reason, shown in the server log; may be {@code null}
     * @throws IllegalStateException if {@code owner} is not enabled
     */
    void disable(@NotNull Plugin owner, @NotNull PortalType portal, @NotNull Action action, @Nullable String reason);

    /**
     * Releases the lock {@code owner} holds on a portal action. Locks held by other plugins
     * are unaffected.
     *
     * @return {@code true} if a lock was held and has been released
     */
    boolean release(@NotNull Plugin owner, @NotNull PortalType portal, @NotNull Action action);
}
