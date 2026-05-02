package systems.mythical.mythicskywars.commands.maps;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.commands.BaseCmd;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.utilities.Messaging;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EditCmd extends BaseCmd {
    public EditCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "edit";
        alias = new String[]{"e"};
        argLength = 2;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        if (systems.mythical.mythicskywars.MythicSkywars.getCfg().getSpawn() != null) {
            String worldName = args[1];
            GameMap gMap = MythicSkywars.getGameMapMgr().getMap(worldName);
            if (gMap == null) {
                sender.sendMessage(new Messaging.MessageFormatter().format("error.map-does-not-exist"));
                return true;
            }
            GameMap.editMap(gMap, player);
            return true;
        }
        sender.sendMessage(new Messaging.MessageFormatter().format("error.nospawn"));
        return true;
    }
}
