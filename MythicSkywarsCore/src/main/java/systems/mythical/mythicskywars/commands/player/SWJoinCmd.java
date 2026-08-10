package systems.mythical.mythicskywars.commands.player;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.enums.GameType;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.managers.MatchManager;
import systems.mythical.mythicskywars.utilities.LuckyBlockHook;
import systems.mythical.mythicskywars.utilities.Messaging;
import systems.mythical.mythicskywars.utilities.Party;
import systems.mythical.mythicskywars.utilities.SWRServer;
import systems.mythical.mythicskywars.utilities.Util;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;

public class SWJoinCmd extends systems.mythical.mythicskywars.commands.BaseCmd {
    public SWJoinCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "join";
        alias = new String[]{"j"};
        argLength = 1;
        maxArgs = 3;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        if (MythicSkywars.getCfg().bungeeMode()) {
            SWRServer server = SWRServer.getAvailableServer();
            if (server != null) {
                server.setPlayerCount(server.getPlayerCount() + 1);
                server.updateSigns();
                MythicSkywars.get().sendBungeeMsg(player, "Connect", server.getServerName());
            }
            return true;
        }

        GameType type = GameType.ALL;
        String mapName = null;
        boolean luckyMode = false;
        if (args.length >= 2) {
            for (int i = 1; i < args.length; i++) {
                String token = args[i].toLowerCase(Locale.ROOT);
                if ("lucky".equals(token) || "luckymode".equals(token)
                        || "luckyblock".equals(token) || "luckysolo".equals(token)) {
                    luckyMode = true;
                    type = GameType.SINGLE;
                    continue;
                }
                if ("single".equals(token) || "solo".equals(token)) {
                    type = GameType.SINGLE;
                    continue;
                }
                if ("team".equals(token)) {
                    type = GameType.TEAM;
                    continue;
                }
                if (mapName == null) {
                    mapName = args[i];
                }
            }
        }

        Party party = Party.getParty(player);
        if (luckyMode && !LuckyBlockHook.isAvailable()) {
            player.sendMessage(new Messaging.MessageFormatter().format("error.could-not-join2"));
            return true;
        }
        if (luckyMode && party != null) {
            player.sendMessage(new Messaging.MessageFormatter().format("error.lucky-solo-only"));
            return true;
        }
        if (party != null && !party.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(new Messaging.MessageFormatter().format("party.onlyleader"));
            return true;
        }

        // Prevent re-running join flow while already in or recently dead from a match.
        if (MatchManager.get().getPlayerMap(player) != null || MatchManager.get().getDeadPlayerMap(player) != null) {
            player.sendMessage(new Messaging.MessageFormatter().format("error.could-not-join2"));
            return true;
        }

        // Wait for stats — previously this was misreported as "busy" and could soft-lock joins.
        if (party != null) {
            for (UUID memberId : party.getMembers()) {
                if (!Util.get().isStatsReady(memberId)) {
                    player.sendMessage(new Messaging.MessageFormatter().format("error.send-stats-not-loaded"));
                    return true;
                }
                if (Util.get().isBusy(memberId)) {
                    Player member = Bukkit.getPlayer(memberId);
                    if (member != null) {
                        party.sendPartyMessage(new Messaging.MessageFormatter().setVariable("player", member.getName()).format("party.memberbusy"));
                    }
                    return true;
                }
            }
        } else if (!Util.get().isStatsReady(player.getUniqueId())) {
            player.sendMessage(new Messaging.MessageFormatter().format("error.send-stats-not-loaded"));
            return true;
        }

        if (mapName != null) {
            GameMap joined;
            if (party != null) {
                joined = MatchManager.get().joinGame(party, type, mapName, luckyMode);
            } else {
                joined = MatchManager.get().joinGame(player, type, mapName, luckyMode);
            }
            if (joined == null) {
                player.sendMessage(new Messaging.MessageFormatter()
                        .setVariable("map", mapName)
                        .format("error.join-map-failed"));
            }
            return true;
        }

        GameMap joinedMap = null;
        if (party != null) {
            joinedMap = MatchManager.get().joinGame(party, type, luckyMode);
            int count = 0;
            while (count < 4 && joinedMap == null) {
                joinedMap = MatchManager.get().joinGame(party, type, luckyMode);
                count++;
            }
        } else {
            joinedMap = MatchManager.get().joinGame(player, type, luckyMode, true);
            int count = 0;
            while (count < 4 && joinedMap == null) {
                joinedMap = MatchManager.get().joinGame(player, type, luckyMode, true);
                count++;
            }
        }
        if (joinedMap == null) {
            player.sendMessage(new Messaging.MessageFormatter().format("error.could-not-join"));
        }
        return true;
    }
}
