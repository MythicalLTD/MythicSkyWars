package systems.mythical.mythicskywars.commands.maps;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.utilities.Messaging;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LegacyLoadCmd extends systems.mythical.mythicskywars.commands.BaseCmd {
    public LegacyLoadCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "legacyload";
        alias = new String[]{"ll"};
        argLength = 2;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        String worldName = args[1];
        GameMap gMap = MythicSkywars.getGameMapMgr().getMap(worldName);
        if (gMap != null) {
            gMap.scanWorld(true, player);
        } else {
            sender.sendMessage(new Messaging.MessageFormatter().format("error.map-register-not-exist"));
        }
        return true;
    }
}
