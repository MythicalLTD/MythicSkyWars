package systems.mythical.mythicskywars.commands.maps;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.utilities.Messaging;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DeleteCmd extends systems.mythical.mythicskywars.commands.BaseCmd {
    public DeleteCmd(String t) {
        type = t;
        forcePlayer = false;
        cmdName = "delete";
        alias = new String[]{"d", "remove"};
        argLength = 2;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        String worldName = args[1];
        GameMap map = MythicSkywars.getGameMapMgr().getMap(worldName);
        if (map != null) {
            boolean result = map.removeMap();
            if (result) {
                sender.sendMessage(new Messaging.MessageFormatter().setVariable("mapname", worldName).format("maps.deleted"));
                return true;
            }
            sender.sendMessage(new Messaging.MessageFormatter().format("error.map-remove"));
            return true;
        }
        sender.sendMessage(new Messaging.MessageFormatter().format("error.map-does-not-exist"));
        return true;
    }
}
