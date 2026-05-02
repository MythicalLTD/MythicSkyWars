package systems.mythical.mythicskywars.commands.maps;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.commands.BaseCmd;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.utilities.Messaging;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaveCmd extends BaseCmd {
    public LeaveCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "leave";
        alias = new String[]{"exit"};
        argLength = 1;
    }

    @Override
    public boolean run(CommandSender sender, Player player, String[] args) {
        GameMap editorMap = MythicSkywars.getGameMapMgr().getMap(player.getWorld().getName());
        if (editorMap == null || !editorMap.isEditing()) {
            player.sendMessage(new Messaging.MessageFormatter().format("error.map-not-editing"));
            return true;
        }
        editorMap.exitEditMode(player, true);
        return true;
    }
}
