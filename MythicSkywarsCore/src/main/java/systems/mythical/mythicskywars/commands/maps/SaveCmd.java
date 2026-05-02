package systems.mythical.mythicskywars.commands.maps;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.utilities.Messaging;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SaveCmd extends systems.mythical.mythicskywars.commands.BaseCmd {
    public SaveCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "save";
        alias = new String[]{"s"};
        argLength = 2;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        String worldName = args[1];
        GameMap gMap = MythicSkywars.getGameMapMgr().getMap(worldName);
        if ((gMap == null) || (!gMap.isEditing())) {
            player.sendMessage(new Messaging.MessageFormatter().format("error.map-does-not-exist"));
            return true;
        }
        gMap.saveMap(player);
        return true;
    }
}
