package com.walrusone.skywarsreloaded.commands.player;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.enums.GameType;
import com.walrusone.skywarsreloaded.game.GameMap;
import com.walrusone.skywarsreloaded.managers.MatchManager;
import com.walrusone.skywarsreloaded.utilities.Messaging;
import com.walrusone.skywarsreloaded.utilities.Party;
import com.walrusone.skywarsreloaded.utilities.SWRServer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public class SWJoinCmd extends com.walrusone.skywarsreloaded.commands.BaseCmd {
    public SWJoinCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "join";
        alias = new String[]{"j"};
        argLength = 1;
        maxArgs = 3;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        if (SkyWarsReloaded.getCfg().bungeeMode()) {
            SWRServer server = SWRServer.getAvailableServer();
            if (server != null) {
                server.setPlayerCount(server.getPlayerCount() + 1);
                server.updateSigns();
                SkyWarsReloaded.get().sendBungeeMsg(player, "Connect", server.getServerName());
            }
            return true;
        }

        GameType type = GameType.ALL;
        String mapName = null;
        if (args.length >= 2) {
            String a1 = args[1].toLowerCase(Locale.ROOT);
            if ("single".equals(a1) || "solo".equals(a1)) {
                type = GameType.SINGLE;
                if (args.length >= 3) {
                    mapName = args[2];
                }
            } else if ("team".equals(a1)) {
                type = GameType.TEAM;
                if (args.length >= 3) {
                    mapName = args[2];
                }
            } else {
                mapName = args[1];
            }
        }

        Party party = Party.getParty(player);
        if (party != null && !party.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(new Messaging.MessageFormatter().format("party.onlyleader"));
            return true;
        }

        if (mapName != null) {
            GameMap joined;
            if (party != null) {
                joined = MatchManager.get().joinGame(party, type, mapName);
            } else {
                joined = MatchManager.get().joinGame(player, type, mapName);
            }
            if (joined == null) {
                player.sendMessage(new Messaging.MessageFormatter()
                        .setVariable("map", mapName)
                        .format("error.join-map-failed"));
            }
            return true;
        }

        boolean joined;
        if (party != null) {
            joined = MatchManager.get().joinGame(party, type) != null;
            int count = 0;
            while (count < 4 && !joined) {
                joined = MatchManager.get().joinGame(party, type) != null;
                count++;
            }
        } else {
            joined = MatchManager.get().joinGame(player, type) != null;
            int count = 0;
            while (count < 4 && !joined) {
                joined = MatchManager.get().joinGame(player, type) != null;
                count++;
            }
        }
        if (!joined) {
            player.sendMessage(new Messaging.MessageFormatter().format("error.could-not-join"));
        }
        return true;
    }
}
