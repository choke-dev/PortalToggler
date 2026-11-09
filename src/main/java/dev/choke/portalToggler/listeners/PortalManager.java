package dev.choke.portalToggler.listeners;

import dev.choke.portalToggler.PortalToggler;
import dev.choke.portalToggler.api.PortalTogglerAPI;
import dev.choke.portalToggler.config.MessageManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.entity.EntityPortalEvent;

import java.util.Locale;

public class PortalManager implements Listener {
    private final MessageManager messageManager;
    private final PortalTogglerAPI api;

    public PortalManager(PortalToggler plugin, PortalTogglerAPI api) {
        this.messageManager = plugin.getMessages();
        this.api = api;
    }

    @EventHandler
    public void onPortalTeleport(PlayerPortalEvent event) {
        Player player = event.getPlayer();

        // Handle Nether Portal
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            if (!isPortalEnabled("nether") && !player.hasPermission("portaltoggler.bypass.enter.nether")) {
                event.setCancelled(true);
                sendMessage(player, "nether", "attempt");
                return;
            }
            return;
        }

        // Handle End Portal
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            if (!isPortalEnabled("end") && !player.hasPermission("portaltoggler.bypass.enter.end")) {
                event.setCancelled(true);
                sendMessage(player, "end", "attempt");
            }
        }
    }

    @EventHandler
    public void onPortalCreation(PortalCreateEvent event) {

        if (event.getReason() == PortalCreateEvent.CreateReason.FIRE) {
            if (isPortalCreationEnabled("nether")) {
                debugLog("Nether portal creation is enabled, allowing creation");
                return;
            }

            // Check if a player caused the portal creation
            if (event.getEntity() instanceof Player player) {
                debugLog("Player " + player.getName() + " attempted to create a nether portal");

                // Check for bypass permission
                if (player.hasPermission("portaltoggler.bypass.create.nether")) {
                    debugLog("Player " + player.getName() + " has bypass permission, allowing portal creation");
                    return;
                }

                // Cancel event and send message
                event.setCancelled(true);
                debugLog("Cancelled nether portal creation for player " + player.getName() + " (no bypass permission)");
                sendMessage(player, "nether", "creation");
            } else {
                debugLog("Non-player entity attempted to create nether portal, cancelling event");
                event.setCancelled(true);
            }
        }

    }

    @EventHandler
    public void onEyeInsertion(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        
        // Check if player is trying to insert an eye of ender into an end portal frame
        if (event.getAction().isRightClick() && 
            event.getItem() != null && 
            event.getItem().getType() == Material.ENDER_EYE && 
            block != null && 
            block.getType() == Material.END_PORTAL_FRAME) {
            
            if (!isPortalCreationEnabled("end") && !player.hasPermission("portaltoggler.bypass.create.end")) {
                event.setCancelled(true);
                sendMessage(player, "end", "creation");
            }
        }
    }

    @EventHandler
    public void onEntityPortal(EntityPortalEvent event) {
        // getTo() is @Nullable (e.g. no destination could be found)
        Location to = event.getTo();
        if (to == null || to.getWorld() == null) {
            return;
        }
        World.Environment destination = to.getWorld().getEnvironment();

        // Handle Nether Portal
        if (destination == World.Environment.NETHER) {
            if (!isPortalEnabled("nether")) {
                event.setCancelled(true);
                return;
            }
            return;
        }

        // Handle End Portal
        if (destination == World.Environment.THE_END) {
            if (!isPortalEnabled("end")) {
                event.setCancelled(true);
            }
        }
    }

    private boolean isPortalEnabled(String portalType) {
        return api.isEnabled(toPortalType(portalType), PortalTogglerAPI.Action.ENTRY);
    }

    private boolean isPortalCreationEnabled(String portalType) {
        return api.isEnabled(toPortalType(portalType), PortalTogglerAPI.Action.CREATION);
    }

    private static PortalTogglerAPI.PortalType toPortalType(String portalType) {
        return PortalTogglerAPI.PortalType.valueOf(portalType.toUpperCase(Locale.ROOT));
    }

    private void debugLog(String message) {
        PortalToggler.getInstance().debugLog(message);
    }

    private void sendMessage(Player player, String portalType, String action) {
        String messageKey;
        if (action.equals("attempt")) {
            messageKey = "disabled-portal-attempt";
        } else if (action.equals("creation")) {
            messageKey = "disabled-" + portalType + "-portal-creation";
        } else {
            messageKey = "portal-" + action;
        }
        String portalName = portalType.substring(0, 1).toUpperCase() + portalType.substring(1);
        Component message = messageManager.getMessage(messageKey, player, "portal", portalName);
        if (message != null) {
            player.sendMessage(message);
        }
    }
}