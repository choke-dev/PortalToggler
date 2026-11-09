package dev.choke.portalToggler.api;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired on the main thread after a plugin's portal lock has been removed by someone other
 * than the owning plugin. One event is fired per removed lock.
 *
 * <p>Locks are not persistent: the owner may call
 * {@link PortalTogglerAPI#disable} again, for example from a listener for this event.
 */
public final class PortalLockReleasedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Why the lock was released. More constants may be added in later versions,
     * so handle unknown values gracefully.
     */
    public enum Reason {
        /** Removed by an administrator with {@code /portal unlock}. */
        ADMIN
    }

    private final String ownerName;
    private final PortalTogglerAPI.PortalType portal;
    private final PortalTogglerAPI.Action action;
    private final String lockReason;
    private final Reason releaseReason;
    private final CommandSender releasedBy;

    @ApiStatus.Internal
    public PortalLockReleasedEvent(@NotNull String ownerName,
                                   @NotNull PortalTogglerAPI.PortalType portal,
                                   @NotNull PortalTogglerAPI.Action action,
                                   @NotNull String lockReason,
                                   @NotNull Reason releaseReason,
                                   @Nullable CommandSender releasedBy) {
        this.ownerName = ownerName;
        this.portal = portal;
        this.action = action;
        this.lockReason = lockReason;
        this.releaseReason = releaseReason;
        this.releasedBy = releasedBy;
    }

    /** @return the name of the plugin that held the lock */
    public @NotNull String getOwnerName() {
        return ownerName;
    }

    /** @return the plugin that held the lock, or {@code null} if it is no longer loaded */
    public @Nullable Plugin getOwner() {
        return Bukkit.getPluginManager().getPlugin(ownerName);
    }

    public @NotNull PortalTogglerAPI.PortalType getPortal() {
        return portal;
    }

    public @NotNull PortalTogglerAPI.Action getAction() {
        return action;
    }

    /** @return the reason the owner gave when placing the lock */
    public @NotNull String getLockReason() {
        return lockReason;
    }

    /** @return why the lock was released */
    public @NotNull Reason getReleaseReason() {
        return releaseReason;
    }

    /** @return who released the lock, if it was released by a command sender */
    public @Nullable CommandSender getReleasedBy() {
        return releasedBy;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
