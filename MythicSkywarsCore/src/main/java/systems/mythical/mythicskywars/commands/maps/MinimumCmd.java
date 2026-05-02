package systems.mythical.mythicskywars.commands.maps;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.utilities.Messaging;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MinimumCmd extends systems.mythical.mythicskywars.commands.BaseCmd {
    public MinimumCmd(String t) {
        type = t;
        forcePlayer = false;
        cmdName = "minimum";
        alias = new String[]{"min"};
        argLength = 3;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        String worldName = args[1];
        if (!systems.mythical.mythicskywars.utilities.Util.get().isInteger(args[2])) {
            sender.sendMessage(new Messaging.MessageFormatter().format("error.map-min-be-int"));
            return true;
        }

        int min = Integer.parseInt(args[2]);
        GameMap map = MythicSkywars.getGameMapMgr().getMap(worldName);
        if (map != null) {
            map.setMinTeams(min);
            sender.sendMessage(new Messaging.MessageFormatter().setVariable("mapname", worldName).setVariable("min", args[2]).format("maps.minplayer"));
            return true;
        }
        sender.sendMessage(new Messaging.MessageFormatter().format("error.map-does-not-exist"));
        return true;
    }
}
