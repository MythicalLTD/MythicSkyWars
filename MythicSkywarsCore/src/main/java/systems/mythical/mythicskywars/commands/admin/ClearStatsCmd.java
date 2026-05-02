package systems.mythical.mythicskywars.commands.admin;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.database.DataStorage;
import systems.mythical.mythicskywars.managers.PlayerStat;
import systems.mythical.mythicskywars.utilities.Messaging;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ClearStatsCmd extends systems.mythical.mythicskywars.commands.BaseCmd {
    public ClearStatsCmd(String t) {
        type = t;
        forcePlayer = false;
        cmdName = "clearstats";
        alias = new String[]{"cs"};
        argLength = 2;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        Player swPlayer = null;
        for (Player playerMatch : Bukkit.getOnlinePlayers()) {
            if (ChatColor.stripColor(playerMatch.getName()).equalsIgnoreCase(ChatColor.stripColor(args[1]))) {
                swPlayer = playerMatch;
            }
        }

        if (swPlayer != null) {
            PlayerStat pStat = PlayerStat.getPlayerStats(swPlayer);
            if (pStat != null) {
                pStat.clear();
                DataStorage.get().saveStats(pStat);
                sender.sendMessage(new Messaging.MessageFormatter().setVariable("player", args[1]).format("command.stats-cleared"));
                return true;
            }
        } else {
            new BukkitRunnable() {
                public void run() {
                    OfflinePlayer offlinePlayer = null;
                    for (OfflinePlayer playerMatch : Bukkit.getOfflinePlayers()) {
                        if (ChatColor.stripColor(playerMatch.getName()).equalsIgnoreCase(ChatColor.stripColor(args[1]))) {
                            offlinePlayer = playerMatch;
                        }
                    }
                    if (offlinePlayer != null) {
                        final String uuid = offlinePlayer.getUniqueId().toString();
                        new BukkitRunnable() {
                            public void run() {
                                DataStorage.get().removePlayerData(uuid);
                                sender.sendMessage(new Messaging.MessageFormatter().setVariable("player", args[1]).format("command.stats-cleared"));
                            }
                        }.runTask(MythicSkywars.get());


                    } else {

                        new BukkitRunnable() {
                            public void run() {
                                sender.sendMessage(new Messaging.MessageFormatter().format("command.player-not-found"));
                            }
                        }.runTask(MythicSkywars.get());
                    }
                }
            }.runTaskAsynchronously(MythicSkywars.get());
        }
        return true;
    }
}
