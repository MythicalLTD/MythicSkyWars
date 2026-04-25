package com.walrusone.skywarsreloaded.commands.player;

import com.walrusone.skywarsreloaded.commands.BaseCmd;
import com.walrusone.skywarsreloaded.managers.MatchManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SWQuitCmd extends BaseCmd {
    public SWQuitCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "quit";
        alias = new String[]{"q", "leave", "l"};
        argLength = 1;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        MatchManager.get().quitCurrentGame(player);
        return true;
    }
}