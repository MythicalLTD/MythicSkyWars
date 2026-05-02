package systems.mythical.mythicskywars.commands.maps;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.commands.BaseCmd;
import systems.mythical.mythicskywars.enums.GameType;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.game.TeamCard;
import systems.mythical.mythicskywars.managers.MatchManager;
import systems.mythical.mythicskywars.menus.gameoptions.objects.CoordLoc;
import systems.mythical.mythicskywars.utilities.Messaging;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DebugCmd extends BaseCmd {
    public DebugCmd(String t) {
        type = t;
        forcePlayer = false;
        cmdName = "debug";
        alias = new String[0];
        argLength = 2;
    }


    public boolean run(CommandSender sender, Player player, String[] args) {
        String worldName = args[1];
        GameMap gMap = MythicSkywars.getGameMapMgr().getMap(worldName);
        if (worldName.equalsIgnoreCase("null")) {
            sender.sendMessage(new Messaging.MessageFormatter()
                    .setVariable("enabled", Boolean.toString(MythicSkywars.getCfg().isRandomVoteEnabled()))
                    .format("debug.random-vote-enabled"));
            return true;
        }

        if (gMap == null) {
            sender.sendMessage(new Messaging.MessageFormatter().format("error.map-does-not-exist"));
            return true;
        }

        sender.sendMessage(new Messaging.MessageFormatter().setVariable("map", worldName).format("debug.arena-header"));
        sender.sendMessage(new Messaging.MessageFormatter().setVariable("registered", Boolean.toString(gMap.isRegistered())).format("debug.arena-registered"));
        sender.sendMessage(new Messaging.MessageFormatter().setVariable("status", gMap.getMatchState().name()).format("debug.arena-status"));
        sender.sendMessage(new Messaging.MessageFormatter().setVariable("players", Integer.toString(gMap.getPlayerCount())).setVariable("maxplayers", Integer.toString(gMap.getMaxPlayers())).format("debug.arena-players"));
        sender.sendMessage(new Messaging.MessageFormatter().setVariable("count", Integer.toString(MythicSkywars.getGameMapMgr().getPlayableArenas(GameType.ALL).size())).format("debug.playable-arenas"));
        sender.sendMessage(" ");
        sender.sendMessage(" ");

        sender.sendMessage(new Messaging.MessageFormatter().format("debug.teams-header"));
        sender.sendMessage(new Messaging.MessageFormatter().setVariable("count", Integer.toString(gMap.getTeamCards().size())).format("debug.teamcards"));
        for (TeamCard card : gMap.getTeamCards()) {
            sender.sendMessage(new Messaging.MessageFormatter()
                    .setVariable("position", Integer.toString(card.getPosition()))
                    .setVariable("players", Integer.toString(card.getPlayerCards().size()))
                    .setVariable("spawns", Integer.toString(card.getSpawns().size()))
                    .format("debug.teamcard-line"));
        }
        sender.sendMessage(" ");

        sender.sendMessage(new Messaging.MessageFormatter().setVariable("count", Integer.toString(gMap.getSpawnLocations().size())).format("debug.spawn-teams"));
        for (TeamCard key : gMap.getSpawnLocations().keySet()) {
            for (CoordLoc loc : gMap.getSpawnLocations().get(key)) {
                sender.sendMessage(new Messaging.MessageFormatter()
                        .setVariable("team", key.getTeamName())
                        .setVariable("loc", loc.getLocationString())
                        .format("debug.spawn-team-line"));
            }
        }

        sender.sendMessage(" ");

        // List all chests
        sender.sendMessage(new Messaging.MessageFormatter().setVariable("count", Integer.toString(gMap.getChests().size())).format("debug.island-chests"));
        for (CoordLoc loc : gMap.getChests()) {
            sender.sendMessage(loc.getLocationString());
        }

        sender.sendMessage(" ");

        // List all center chests
        sender.sendMessage(new Messaging.MessageFormatter().setVariable("count", Integer.toString(gMap.getCenterChests().size())).format("debug.center-chests"));
        for (CoordLoc loc : gMap.getCenterChests()) {
            sender.sendMessage(loc.getLocationString());
        }

        sender.sendMessage(" ");

        if (sender instanceof Player) {
            sender.sendMessage(new Messaging.MessageFormatter().setVariable("player", sender.getName()).format("debug.player-header"));
            GameMap map = MatchManager.get().getPlayerMap(player);
            GameMap dead = MatchManager.get().getPlayerMap(player);
            GameMap spec = MatchManager.get().getPlayerMap(player);

            sender.sendMessage(new Messaging.MessageFormatter().setVariable("in", map == null ? "no" : "yes").setVariable("map", map != null ? map.getName() : "").format("debug.player-as-player"));
            sender.sendMessage(new Messaging.MessageFormatter().setVariable("in", dead == null ? "no" : "yes").setVariable("map", dead != null ? dead.getName() : "").format("debug.player-as-dead"));
            sender.sendMessage(new Messaging.MessageFormatter().setVariable("in", spec == null ? "no" : "yes").setVariable("map", spec != null ? spec.getName() : "").format("debug.player-as-spec"));
        }
        return true;
    }
}
