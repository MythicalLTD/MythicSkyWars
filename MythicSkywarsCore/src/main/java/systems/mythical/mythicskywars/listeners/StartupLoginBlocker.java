package systems.mythical.mythicskywars.listeners;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.utilities.Messaging;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

/**
 * Blocks player logins while maps are still loading during startup.
 * Only active when bungee mode is enabled (dedicated skywars server).
 * Players with the "sw.bypass.startup" permission can still join.
 */
public class StartupLoginBlocker implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (MythicSkywars.get().areMapsReady()) {
            return;
        }
        if (!MythicSkywars.getCfg().isBungeeEnabled()) {
            return;
        }
        // Allow admins to bypass
        if (event.getPlayer().hasPermission("sw.bypass.startup")) {
            return;
        }
        String message = new Messaging.MessageFormatter().format("error.server-starting");
        if (message == null || message.isEmpty()) {
            message = ChatColor.RED + "Server is still starting up. Please try again in a moment.";
        }
        event.disallow(PlayerLoginEvent.Result.KICK_OTHER, message);
    }
}
