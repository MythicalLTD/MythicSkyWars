package systems.mythical.mythicskywars.commands.maps;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.managers.GameMapManager;
import systems.mythical.mythicskywars.utilities.Messaging;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnregisterCmd extends systems.mythical.mythicskywars.commands.BaseCmd {


    public UnregisterCmd(String t) {
        type = t;
        forcePlayer = false;
        cmdName = "unregister";
        alias = new String[]{"unreg"};
        argLength = 2;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        String worldName = args[1];
        GameMap map = MythicSkywars.getGameMapMgr().getMap(worldName);
        if (map != null) {
            map.unregister(true);
            sender.sendMessage(new Messaging.MessageFormatter().setVariable("mapname", map.getDisplayName()).format("maps.unregistered"));
            return true;
        }
        sender.sendMessage(new Messaging.MessageFormatter().format("error.map-does-not-exist"));
        return true;
    }
}
