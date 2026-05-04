package systems.mythical.mythicskywars.listeners;

import systems.mythical.mythicskywars.clients.lunar.LunarLobbyWaterPortalApollo;
import systems.mythical.mythicskywars.MythicSkywars;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;

public class LobbyWaterPortalListener implements org.bukkit.event.Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        Player player = event.getPlayer();
        if (MythicSkywars.getLobbyWaterPortals() != null) {
            MythicSkywars.getLobbyWaterPortals().tryEnterPortal(player);
        }
        LunarLobbyWaterPortalApollo.onPlayerMove(player, event.getTo());
    }
}
