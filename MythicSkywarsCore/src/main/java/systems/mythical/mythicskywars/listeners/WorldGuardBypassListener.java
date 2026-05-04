package systems.mythical.mythicskywars.listeners;

import systems.mythical.mythicskywars.enums.MatchState;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.managers.MatchManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

/**
 * Runs at HIGHEST priority to override WorldGuard's cancellation of events
 * for players that are in an active SkyWars match (PLAYING state).
 * WorldGuard registers at HIGH priority, so HIGHEST runs after it.
 */
public class WorldGuardBypassListener implements Listener {

    private boolean isActivePlayer(Player player) {
        GameMap map = MatchManager.get().getPlayerMap(player);
        return map != null && map.getMatchState() == MatchState.PLAYING && !MatchManager.get().isSpectating(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!event.isCancelled()) return;
        if (isActivePlayer(event.getPlayer())) {
            event.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!event.isCancelled()) return;
        if (isActivePlayer(event.getPlayer())) {
            event.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isActivePlayer(player)) return;
        if (event.useInteractedBlock() == org.bukkit.event.Event.Result.DENY) {
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.ALLOW);
        }
        if (event.useItemInHand() == org.bukkit.event.Event.Result.DENY) {
            event.setUseItemInHand(org.bukkit.event.Event.Result.ALLOW);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!event.isCancelled()) return;
        if (isActivePlayer(event.getPlayer())) {
            event.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (!event.isCancelled()) return;
        if (isActivePlayer(event.getPlayer())) {
            event.setCancelled(false);
        }
    }
}
