package com.walrusone.skywarsreloaded.commands.player;

import com.walrusone.skywarsreloaded.commands.BaseCmd;
import com.walrusone.skywarsreloaded.managers.LobbyBypassManager;
import com.walrusone.skywarsreloaded.utilities.Messaging;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SWBypassLobbySlotsCmd extends BaseCmd {
    public SWBypassLobbySlotsCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "bypasslobbyslots";
        alias = new String[]{"bypassLobbySlots"};
        argLength = 1;
    }

    @Override
    public boolean run(CommandSender sender, Player player, String[] args) {
        boolean enabled = LobbyBypassManager.toggle(player);
        if (enabled) {
            player.sendMessage(new Messaging.MessageFormatter().format("command.bypass-lobby-slots-enabled"));
        } else {
            player.sendMessage(new Messaging.MessageFormatter().format("command.bypass-lobby-slots-disabled"));
        }
        return true;
    }
}
