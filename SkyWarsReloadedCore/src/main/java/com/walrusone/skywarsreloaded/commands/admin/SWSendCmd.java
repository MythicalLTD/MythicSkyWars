package com.walrusone.skywarsreloaded.commands.admin;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.commands.BaseCmd;
import com.walrusone.skywarsreloaded.game.GameMap;
import com.walrusone.skywarsreloaded.managers.MatchManager;
import com.walrusone.skywarsreloaded.managers.PlayerStat;
import com.walrusone.skywarsreloaded.enums.PlayerRemoveReason;
import com.walrusone.skywarsreloaded.utilities.Messaging;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class SWSendCmd extends BaseCmd {

    public SWSendCmd(String t) {
        type = t;
        forcePlayer = false;
        cmdName = "send";
        argLength = 2;
        maxArgs = 3;
    }

    @Override
    public boolean run(CommandSender sender, Player ignored, String[] args) {
        if (SkyWarsReloaded.getCfg().bungeeMode()) {
            sender.sendMessage(new Messaging.MessageFormatter().format("error.send-bungee-not-supported"));
            return true;
        }

        Player target;
        String mapToken;
        if (args.length == 2) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(new Messaging.MessageFormatter().format("error.send-console-needs-player"));
                return false;
            }
            Player admin = (Player) sender;
            UUID sel = AdminSendSelection.getSelected(admin.getUniqueId());
            if (sel == null) {
                sender.sendMessage(new Messaging.MessageFormatter().format("error.send-no-selection"));
                return false;
            }
            target = Bukkit.getPlayer(sel);
            if (target == null) {
                sender.sendMessage(new Messaging.MessageFormatter().format("error.send-target-offline"));
                AdminSendSelection.clearSelection(admin.getUniqueId());
                return true;
            }
            mapToken = args[1];
        } else {
            target = Bukkit.getPlayer(ChatColor.stripColor(args[1]));
            if (target == null) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (ChatColor.stripColor(p.getName()).equalsIgnoreCase(ChatColor.stripColor(args[1]))) {
                        target = p;
                        break;
                    }
                }
            }
            if (target == null) {
                sender.sendMessage(new Messaging.MessageFormatter().format("error.send-player-not-found"));
                return true;
            }
            mapToken = args[2];
        }

        PlayerStat ps = PlayerStat.getPlayerStats(target.getUniqueId());
        if (ps == null || !ps.isInitialized()) {
            sender.sendMessage(new Messaging.MessageFormatter().format("error.send-stats-not-loaded"));
            return true;
        }

        GameMap map = SkyWarsReloaded.getGameMapMgr().getMap(ChatColor.stripColor(mapToken));
        if (map == null) {
            map = SkyWarsReloaded.getGameMapMgr().getMapByDisplayName(ChatColor.stripColor(mapToken));
        }
        if (map == null) {
            sender.sendMessage(new Messaging.MessageFormatter().format("error.send-map-not-found"));
            return true;
        }

        GameMap current = MatchManager.get().getPlayerMap(target);
        if (current != null && current.getName().equals(map.getName())) {
            sender.sendMessage(new Messaging.MessageFormatter().format("error.send-already-in-arena"));
            return true;
        }

        if (!map.canAddPlayer(target)) {
            sender.sendMessage(new Messaging.MessageFormatter().format("error.send-cannot-join"));
            return true;
        }

        if (current != null) {
            SkyWarsReloaded.get().getPlayerManager().removePlayer(
                    target, PlayerRemoveReason.OTHER, null, false, false);
        }

        if (!map.addPlayers(null, target)) {
            sender.sendMessage(new Messaging.MessageFormatter().format("error.send-cannot-join"));
            return true;
        }

        if (sender instanceof Player) {
            AdminSendSelection.clearSelection(((Player) sender).getUniqueId());
        }

        sender.sendMessage(new Messaging.MessageFormatter()
                .setVariable("player", target.getName())
                .setVariable("map", map.getDisplayName())
                .format("command.send-success"));
        return true;
    }
}
