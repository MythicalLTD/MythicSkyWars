package com.walrusone.skywarsreloaded.commands.player;

import com.walrusone.skywarsreloaded.menus.playeroptions.PrestigeMenu;
import com.walrusone.skywarsreloaded.utilities.Messaging;
import com.walrusone.skywarsreloaded.utilities.PrestigeManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SWPrestigeCmd extends com.walrusone.skywarsreloaded.commands.BaseCmd {

    public SWPrestigeCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "prestige";
        argLength = 1;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        if (!PrestigeManager.get().isEnabled()) {
            player.sendMessage(new Messaging.MessageFormatter().format("levels.prestige-disabled"));
            return true;
        }
        PrestigeMenu.open(player);
        return true;
    }
}
