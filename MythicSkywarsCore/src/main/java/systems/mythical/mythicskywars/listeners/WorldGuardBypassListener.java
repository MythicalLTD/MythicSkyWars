package systems.mythical.mythicskywars.listeners;

import systems.mythical.mythicskywars.MythicSkywars;
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
 * Runs at MONITOR priority to override ANY plugin's cancellation of events
 * for players that are in an active SkyWars match (PLAYING state).
 * This ensures no other plugin (WorldGuard, FAWE, etc.) can block arena gameplay.
 *
 * NOTE: Using MONITOR to modify event state is unconventional but necessary here
 * because we need to guarantee arena players can always interact during matches.
 */
public class WorldGuardBypassListener implements Listener {

    private boolean isActivePlayer(Player player) {
        GameMap map = MatchManager.get().getPlayerMap(player);
        return map != null && map.getMatchState() == MatchState.PLAYING && !MatchManager.get().isSpectating(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!event.isCancelled()) return;
        if (isActivePlayer(event.getPlayer())) {
            event.setCancelled(false);
            if (MythicSkywars.getCfg().debugEnabled()) {
                MythicSkywars.get().getLogger().info("[SWBypass] Uncancelled block break for " + event.getPlayer().getName());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!event.isCancelled()) return;
        if (isActivePlayer(event.getPlayer())) {
            event.setCancelled(false);
            if (MythicSkywars.getCfg().debugEnabled()) {
                MythicSkywars.get().getLogger().info("[SWBypass] Uncancelled block place for " + event.getPlayer().getName());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isActivePlayer(player)) return;
        if (event.useInteractedBlock() == org.bukkit.event.Event.Result.DENY) {
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.ALLOW);
            if (MythicSkywars.getCfg().debugEnabled()) {
                MythicSkywars.get().getLogger().info("[SWBypass] Uncancelled interact for " + player.getName());
            }
        }
        if (event.useItemInHand() == org.bukkit.event.Event.Result.DENY) {
            event.setUseItemInHand(org.bukkit.event.Event.Result.ALLOW);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!event.isCancelled()) return;
        if (isActivePlayer(event.getPlayer())) {
            event.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (!event.isCancelled()) return;
        if (isActivePlayer(event.getPlayer())) {
            event.setCancelled(false);
        }
    }
}
