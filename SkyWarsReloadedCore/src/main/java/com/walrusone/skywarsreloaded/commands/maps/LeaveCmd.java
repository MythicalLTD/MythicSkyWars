package com.walrusone.skywarsreloaded.commands.maps;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.commands.BaseCmd;
import com.walrusone.skywarsreloaded.game.GameMap;
import com.walrusone.skywarsreloaded.utilities.Messaging;
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
        GameMap editorMap = SkyWarsReloaded.getGameMapMgr().getMap(player.getWorld().getName());
        if (editorMap == null || !editorMap.isEditing()) {
            player.sendMessage(new Messaging.MessageFormatter().format("error.map-not-editing"));
            return true;
        }
        editorMap.exitEditMode(player, true);
        return true;
    }
}
