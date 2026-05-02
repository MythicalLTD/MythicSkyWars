package systems.mythical.mythicskywars.listeners;

import com.google.common.collect.Lists;
import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.config.Config;
import systems.mythical.mythicskywars.enums.MatchState;
import systems.mythical.mythicskywars.enums.PlayerRemoveReason;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.managers.MatchManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class PlayerTeleportListener implements org.bukkit.event.Listener {
    public PlayerTeleportListener() {
    }

    private static final List<Player> cooldowns = Lists.newArrayList();
    private static final Object cooldownsLock = new Object();

    @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        GameMap gameMap = MatchManager.get().getPlayerMap(player);

        if (gameMap == null) {

            if (event.getTo() == null) {
                MythicSkywars.get().getLogger().warning(String.format("Player %s teleported to nowhere! (changing dimension?)", player.getName()));
                return;
            }

            if (event.getTo().getWorld() == null) {
                MythicSkywars.get().getLogger().warning(String.format("Player %s teleported to location with no world! (changing dimension?)", player.getName()));
                return;
            }

            if (MythicSkywars.getCfg().getSpawn() != null) {
                // Pre vars
                World spawnWorld = MythicSkywars.getCfg().getSpawn().getWorld();
                boolean wasInSpawnWorld = event.getFrom().getWorld().equals(spawnWorld);
                boolean isGoingToSpawnWorld = event.getTo().getWorld().equals(spawnWorld);

                // Going to spawn world
                if (!wasInSpawnWorld && isGoingToSpawnWorld) {
                    setPlayerOnCooldown(player, true);
                    Bukkit.getScheduler().runTaskLaterAsynchronously(MythicSkywars.get(), () -> setPlayerOnCooldown(player, false), 5);
                    systems.mythical.mythicskywars.managers.PlayerStat.updatePlayer(player.getUniqueId().toString());
                    //if (MythicSkywars.get().getUpdater().getUpdateStatus() == 1 && (player.isOp() || player.hasPermission("sw.admin"))) {
                    //    //player.spigot().sendMessage(base);
                    //    MythicSkywars.getNMS().sendJSON(player, "[\"\",{\"text\":\"§d§l[MythicSkywars] §aA new update has been found: §b" + MythicSkywars.get().getUpdater().getLatestVersion() + "§a. Click here to update!\",\"clickEvent\":{\"action\":\"open_url\",\"value\":\"" + MythicSkywars.get().getUpdater().getUpdateURL() + "\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"\",\"extra\":[{\"text\":\"§7Click here to update to the latest version!\"}]}}}]");
                    //}
                    return;
                }

                // Leaving spawn world
                if (wasInSpawnWorld && !isGoingToSpawnWorld) {
                    if (MythicSkywars.getCfg().lobbyBoardEnabled()) {
                        MythicSkywars.getNMS().removeFromScoreboardCollection(player.getScoreboard());//for 1.13+
                        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
                    }
                    if ((MythicSkywars.getCfg().optionsMenuEnabled()) &&
                            (player.getInventory().getItem(MythicSkywars.getCfg().getOptionsSlot()) != null) &&
                            (player.getInventory().getItem(MythicSkywars.getCfg().getOptionsSlot()).equals(MythicSkywars.getIM().getItem("optionselect")))) {
                        player.getInventory().setItem(MythicSkywars.getCfg().getOptionsSlot(), new ItemStack(Material.AIR, 1));
                    }


                    if ((MythicSkywars.getCfg().joinMenuEnabled()) && (player.hasPermission("sw.join")) &&
                            (player.getInventory().getItem(MythicSkywars.getCfg().getJoinSlot()) != null) &&
                            (player.getInventory().getItem(MythicSkywars.getCfg().getJoinSlot()).equals(MythicSkywars.getIM().getItem("joinselect")))) {
                        player.getInventory().setItem(MythicSkywars.getCfg().getJoinSlot(), new ItemStack(Material.AIR, 1));
                    }


                    if ((MythicSkywars.getCfg().spectateMenuEnabled()) && (player.hasPermission("sw.spectate")) &&
                            (player.getInventory().getItem(MythicSkywars.getCfg().getSpectateSlot()) != null) &&
                            (player.getInventory().getItem(MythicSkywars.getCfg().getSpectateSlot()).equals(MythicSkywars.getIM().getItem("spectateselect")))) {
                        player.getInventory().setItem(MythicSkywars.getCfg().getSpectateSlot(), new ItemStack(Material.AIR, 1));
                    }
                    if (MythicSkywars.getCfg().isStatsItemEnabled() &&
                            player.getInventory().getItem(MythicSkywars.getCfg().getStatsItemPos()) != null &&
                            player.getInventory().getItem(MythicSkywars.getCfg().getStatsItemPos()).equals(MythicSkywars.getIM().getItem("statsitem"))) {
                        player.getInventory().setItem(MythicSkywars.getCfg().getStatsItemPos(), new ItemStack(Material.AIR, 1));
                    }
                    if (MythicSkywars.getCfg().isBackToLobbyItemEnabled() &&
                            player.getInventory().getItem(MythicSkywars.getCfg().getBackToLobbyPos()) != null &&
                            player.getInventory().getItem(MythicSkywars.getCfg().getBackToLobbyPos()).equals(MythicSkywars.getIM().getItem("backlobbyitem"))) {
                        player.getInventory().setItem(MythicSkywars.getCfg().getBackToLobbyPos(), new ItemStack(Material.AIR, 1));
                    }

                }

            }
        }
        else if (event.getCause().equals(TeleportCause.SPECTATE)) {
            event.setCancelled(true);
        }
        else if (event.getCause().equals(TeleportCause.END_PORTAL) ||
                player.hasPermission("sw.opteleport") ||
                event.getTo().getWorld().equals(event.getFrom().getWorld()))
        {
            event.setCancelled(false);
        }
        else if (event.getCause().equals(TeleportCause.ENDER_PEARL) &&
                gameMap.getMatchState() != MatchState.ENDING &&
                gameMap.getMatchState() != MatchState.WAITINGSTART &&
                gameMap.getMatchState() != MatchState.WAITINGLOBBY)
        {
            event.setCancelled(false);
        }
        else {
            event.setCancelled(true);
        }
    }

    @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onTP(PlayerTeleportEvent e) {
        GameMap gameMap = MatchManager.get().getPlayerMap(e.getPlayer());
        if (gameMap != null) {
            String toWorldName = e.getTo().getWorld().getName();
            if (gameMap.getCurrentWorld() == null) {
                MythicSkywars.get().getLogger().severe("Skywars could not find a world by the name of " + gameMap.getName());
                return;
            }
            String mapWorldName = gameMap.getCurrentWorld().getName();
            if (!toWorldName.equals(mapWorldName)) {
                Config config = MythicSkywars.getCfg();
                if (config.getKickOnWorldTeleport()) {
                    Player player = e.getPlayer();
//                    EntityDamageEvent.DamageCause damageCause = EntityDamageEvent.DamageCause.CUSTOM;
//                    if (player.getLastDamageCause() != null) {
//                        damageCause = player.getLastDamageCause().getCause();
//                    }
                    Location spawnLoc = config.getSpawn();
                    if (spawnLoc == null) {
                        MythicSkywars.get().getLogger().severe("Spawn location is not set! Cannot perform auto return to lobby on world change!");
                        return;
                    }
                    String lobbyWorldName = spawnLoc.getWorld().getName();
                    boolean shouldSendToLobby = config.bungeeMode() || !lobbyWorldName.equals(toWorldName);
                    MythicSkywars.get().getPlayerManager().removePlayer(
                           player,
                           PlayerRemoveReason.PLAYER_QUIT_GAME,
                           null,
                            shouldSendToLobby,
                           true
                   );
                    // MatchManager.get().removeAlivePlayer(player, damageCause, true, true);
                } else {
                    e.setCancelled(true);
                }
            }
        }
    }

    // UTILS

    public static void setPlayerOnCooldown(Player p, boolean cooldown) {
        synchronized (cooldownsLock) {
            boolean contains = cooldowns.contains(p);
            if (cooldown && !contains) cooldowns.add(p);
            else if (!cooldown && contains) cooldowns.remove(p);
        }
    }

    public static boolean isPlayerOnCooldown(Player p) {
        synchronized (cooldownsLock) {
            return cooldowns.contains(p);
        }
    }
}
