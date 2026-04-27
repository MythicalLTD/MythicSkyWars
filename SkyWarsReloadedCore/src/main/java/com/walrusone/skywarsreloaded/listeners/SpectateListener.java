package com.walrusone.skywarsreloaded.listeners;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.enums.PlayerRemoveReason;
import com.walrusone.skywarsreloaded.game.GameMap;
import com.walrusone.skywarsreloaded.managers.MatchManager;
import com.walrusone.skywarsreloaded.menus.gameoptions.objects.CoordLoc;
import com.walrusone.skywarsreloaded.utilities.Messaging;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SpectateListener implements org.bukkit.event.Listener {
    private HashMap<String, BukkitTask> teleportRequests = new HashMap();
    /** Avoid scheduling duplicate void-rescue tasks while falling (spectators often get no VOID damage). */
    private final Set<UUID> voidRescuePending = new HashSet<>();

    public SpectateListener() {
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        Player player = e.getPlayer();
        GameMap gameMap = MatchManager.get().getSpectatorMap(player);
        if ((gameMap == null) || (player.hasPermission("sw.opteleport"))) {
            return;
        }
        if (!(e.getCause() == TeleportCause.END_PORTAL || e.getCause() == TeleportCause.SPECTATE)) {
            e.setCancelled(true);
        }
    }

    /**
     * Must use {@link TeleportCause#END_PORTAL} (or SPECTATE) so {@link #onPlayerTeleport} does not cancel plugin teleports.
     */
    private static void teleportSpectatorToSafeSpawn(Player player, GameMap gameMap) {
        if (player == null || !player.isOnline()) {
            return;
        }
        org.bukkit.World world = gameMap.getCurrentWorld();
        if (world == null) {
            return;
        }
        CoordLoc ss = gameMap.getSpectateSpawn();
        Location loc;
        if (ss != null) {
            loc = new Location(world, ss.getX(), ss.getY(), ss.getZ());
        } else {
            loc = world.getSpawnLocation();
        }
        player.teleport(loc, TeleportCause.END_PORTAL);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpectatorDamaged(EntityDamageEvent e) {
        if ((e.getEntity() instanceof Player)) {
            Player player = (Player) e.getEntity();
            GameMap gameMap = MatchManager.get().getSpectatorMap(player);
            if (gameMap == null) {
                return;
            }
            e.setCancelled(true);
            if (e.getCause() == EntityDamageEvent.DamageCause.VOID) {
                teleportSpectatorToSafeSpawn(player, gameMap);
            }
        }
    }

    // TODO: REMOVE IF NO PROBLEMS PRESENTED
//    @EventHandler
//    public void onPlayerQuit(PlayerQuitEvent e) {
//        Player player = e.getPlayer();
//        GameMap gameMap = MatchManager.get().getSpectatorMap(player);
//        if (gameMap == null) {
//            return;
//        }
//        gameMap.getSpectators().remove(player.getUniqueId());
//        gameMap.getAlivePlayers().remove(player);
//        gameMap.getAllPlayers().remove(player);
//        MatchManager.get().removeSpectator(player);
//
//    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        if (player == null)
            return;
        GameMap gameMap = MatchManager.get().getSpectatorMap(player);
        if (gameMap == null) {
            return;
        }
        int slot = e.getSlot();
        if (slot == 8) {
            player.closeInventory();
            SkyWarsReloaded.get().getPlayerManager().removePlayer(
                    player, PlayerRemoveReason.PLAYER_QUIT_GAME, null, false);
        } else if ((slot >= 9) && (slot <= 35)) {
            player.closeInventory();
            ItemStack item = e.getCurrentItem();
            if (item != null && !item.getType().equals(Material.AIR)) {
                // Get name to TP to & sanity check
                String name = org.bukkit.ChatColor.stripColor(item.getItemMeta().getDisplayName());
                if (name == null)
                    return;
                Player toSpec = SkyWarsReloaded.get().getServer().getPlayer(name);
                if (toSpec != null && !gameMap.mapContainsDead(toSpec.getUniqueId())) {
                    player.teleport(toSpec.getLocation(), TeleportCause.END_PORTAL);
                } else {
                    SkyWarsReloaded.get().getLogger().warning("Spectator attempted to TP to " + name + " but that player is dead or not online!");
                }
            }
        }
    }


    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        if (e.getTo() != null) {
            GameMap specMap = MatchManager.get().getSpectatorMap(player);
            if (specMap != null) {
                org.bukkit.World arenaWorld = specMap.getCurrentWorld();
                if (arenaWorld != null && e.getTo().getWorld().equals(arenaWorld) && e.getTo().getY() < 8.0D) {
                    UUID id = player.getUniqueId();
                    if (voidRescuePending.add(id)) {
                        SkyWarsReloaded.get().getServer().getScheduler().runTask(SkyWarsReloaded.get(), () -> {
                            try {
                                if (player.isOnline() && MatchManager.get().getSpectatorMap(player) == specMap) {
                                    teleportSpectatorToSafeSpawn(player, specMap);
                                }
                            } finally {
                                voidRescuePending.remove(id);
                            }
                        });
                    }
                }
            }
        }
        if (e.getTo() != null && (teleportRequests.containsKey(e.getPlayer().getUniqueId().toString())) && (
                (e.getTo().getBlockX() != e.getFrom().getBlockX()) || (e.getTo().getBlockY() != e.getFrom().getBlockY()) || (e.getTo().getBlockZ() != e.getFrom().getBlockZ()))) {
            e.getPlayer().sendMessage(new Messaging.MessageFormatter().format("error.spectate-cancelled"));
            ((BukkitTask) teleportRequests.get(e.getPlayer().getUniqueId().toString())).cancel();
            teleportRequests.remove(e.getPlayer().getUniqueId().toString());
        }
    }
}