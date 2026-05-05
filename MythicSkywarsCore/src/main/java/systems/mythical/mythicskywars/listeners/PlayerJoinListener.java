package systems.mythical.mythicskywars.listeners;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.clients.feather.FeatherClientBridge;
import systems.mythical.mythicskywars.clients.labymod.LabyModBridge;
import systems.mythical.mythicskywars.database.UltraSkyWarsMongoMigrator;
import systems.mythical.mythicskywars.enums.GameType;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.managers.MatchManager;
import systems.mythical.mythicskywars.managers.PlayerStat;
import systems.mythical.mythicskywars.utilities.Messaging;
import systems.mythical.mythicskywars.utilities.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Level;

public class PlayerJoinListener implements Listener {


    public PlayerJoinListener() {
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(final PlayerJoinEvent event) {

        final Player player = event.getPlayer();
        if (UltraSkyWarsMongoMigrator.isMigrationRunning()) {
            player.kickPlayer(ChatColor.RED + "Data import is running. Please reconnect after it finishes.");
            return;
        }
        FeatherClientBridge.onJoin(player);
        LabyModBridge.onJoin(player);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (MythicSkywars.getCfg().getSpawn() != null && MythicSkywars.getCfg().teleportOnJoin()) {
                    player.teleport(MythicSkywars.getCfg().getSpawn());
                }
                // Self-heal stale arena tracking immediately on join so lobby items/portal/protection work.
                MatchManager.get().getPlayerMapSafe(player);
            }
        }.runTaskLater(MythicSkywars.get(), 1);

        if (MythicSkywars.getCfg().promptForResource()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.setResourcePack(MythicSkywars.getCfg().getResourceLink());
                }
            }.runTaskLater(MythicSkywars.get(), 20);
        }

        if (PlayerStat.getPlayerStats(player) != null) {
            PlayerStat.removePlayer(player.getUniqueId().toString());
        }

        if (!MythicSkywars.getCfg().bungeeMode()) {
            for (GameMap gMap : MythicSkywars.getGameMapMgr().getMapsCopy()) {
                if (gMap.getCurrentWorld() != null && gMap.getCurrentWorld().equals(player.getWorld())) {
                    if (MythicSkywars.getCfg().getSpawn() != null) {
                        player.teleport(MythicSkywars.getCfg().getSpawn());
                    }
                }
            }
        }

        PlayerStat pStats = new PlayerStat(player);
        PlayerStat.getPlayers().add(pStats);
        pStats.updatePlayerIfInLobby(player);

        // Load player data
        pStats.loadStats(() -> {
            // Not allowed? Stop.
            if (!postLoadStats(player)) return;
            // Send updater message if the player was allowed to join
            if (MythicSkywars.getUpdater() != null) {
                MythicSkywars.getUpdater().notifyPlayer(player);
            }
        });
    }

    /**
     * Handle bungeecord join
     * @param player The joining player
     * @return Whether the player was allowed to join
     */
    public boolean postLoadStats(Player player) {
        // After stats are done loading, move to a game if in bungeecord mode
        if (!MythicSkywars.getCfg().bungeeMode()) return true;

        if (player == null) return false;
        if (MythicSkywars.getCfg().isLobbyServer()) return true;

        Bukkit.getLogger().log(Level.WARNING, "Trying to let " + player.getName() + " join a game");

        boolean joined = MatchManager.get().joinGame(player, GameType.ALL) != null;
        if (joined) return true;

        Bukkit.getLogger().log(Level.WARNING, "Failed to put " + player.getName() + " in a game");
        if (MythicSkywars.getCfg().debugEnabled()) {
            Util.get().logToFile(ChatColor.YELLOW + "Couldn't find an arena for player " + player.getName() + ". Sending the player back to the skywars lobby.");
        }

        if (player.hasPermission("sw.admin")) {
            player.sendMessage(new Messaging.MessageFormatter().format("error.bungee-join-failed-admin"));
        } else {
            MythicSkywars.get().sendBungeeMsg(player, "Connect", MythicSkywars.getCfg().getBungeeLobby());
            kickPlayerIfStillOnline(player, 20);
        }

        return false;
    }

    // UTILS

    public void kickPlayerIfStillOnline(Player player, long ticks) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) player.kickPlayer("");
            }
        }.runTaskLater(MythicSkywars.get(), ticks);
    }
}