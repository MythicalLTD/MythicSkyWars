package systems.mythical.mythicskywars.commands.maps;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.enums.ChestPlacementType;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.utilities.Messaging;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChestTypeCmd extends systems.mythical.mythicskywars.commands.BaseCmd {
    public ChestTypeCmd(String t) {
        type = t;
        forcePlayer = false;
        cmdName = "chesttype";
        alias = new String[]{"ct"};
        argLength = 2;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        String worldName = args[1];
        GameMap map = MythicSkywars.getGameMapMgr().getMap(worldName);
        if (map != null) {
            if (map.getChestPlacementType() == ChestPlacementType.NORMAL) {
                map.setChestPlacementType(ChestPlacementType.CENTER);
            } else {
                map.setChestPlacementType(ChestPlacementType.NORMAL);
            }
            sender.sendMessage(new Messaging.MessageFormatter().setVariable("mapname", worldName)
                    .setVariable("type", map.getChestPlacementType().toString())
                    .format("maps.chestPlacementType"));
            return true;
        }
        sender.sendMessage(new Messaging.MessageFormatter().format("error.map-does-not-exist"));
        return true;
    }
}
