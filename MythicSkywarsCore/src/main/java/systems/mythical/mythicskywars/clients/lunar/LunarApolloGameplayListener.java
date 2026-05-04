package systems.mythical.mythicskywars.clients.lunar;

import systems.mythical.mythicskywars.enums.MatchState;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.managers.MatchManager;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;

/**
 * Apollo hooks driven by gameplay events (pearls, etc.).
 */
public final class LunarApolloGameplayListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!LunarApolloBridge.isUsable()) {
            return;
        }
        Projectile entity = event.getEntity();
        if (!(entity instanceof EnderPearl)) {
            return;
        }
        if (!(entity.getShooter() instanceof Player)) {
            return;
        }
        Player player = (Player) entity.getShooter();
        GameMap map = MatchManager.get().getPlayerMap(player);
        if (map == null || map.getMatchState() != MatchState.PLAYING) {
            return;
        }
        LunarApolloBridge.showPearlCooldown(player);
    }
}
