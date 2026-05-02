package systems.mythical.mythicskywars.commands.maps;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.utilities.Messaging.MessageFormatter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TeamSizeCmd extends systems.mythical.mythicskywars.commands.BaseCmd {
    public TeamSizeCmd(String t) {
        type = t;
        forcePlayer = false;
        cmdName = "teamsize";
        alias = new String[]{"team"};
        argLength = 3;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        String worldName = args[1];
        if (!systems.mythical.mythicskywars.utilities.Util.get().isInteger(args[2])) {
            sender.sendMessage(new MessageFormatter().format("error.map-min-be-int"));
            return true;
        }

        int min = Integer.parseInt(args[2]);
        GameMap map = MythicSkywars.getGameMapMgr().getMap(worldName);
        if (map != null) {
            map.setTeamSize(min);
            sender.sendMessage(new MessageFormatter().setVariable("mapname", worldName).setVariable("size", args[2]).format("maps.teamsize"));
            return true;
        }
        sender.sendMessage(new MessageFormatter().format("error.map-does-not-exist"));
        return true;
    }
}
