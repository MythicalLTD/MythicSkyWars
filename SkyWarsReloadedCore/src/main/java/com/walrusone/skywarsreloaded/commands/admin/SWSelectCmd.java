package com.walrusone.skywarsreloaded.commands.admin;

import com.walrusone.skywarsreloaded.commands.BaseCmd;
import com.walrusone.skywarsreloaded.utilities.Messaging;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SWSelectCmd extends BaseCmd {

    public SWSelectCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "select";
        argLength = 2;
    }

    @Override
    public boolean run(CommandSender sender, Player admin, String[] args) {
        Player target = Bukkit.getPlayer(ChatColor.stripColor(args[1]));
        if (target == null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (ChatColor.stripColor(p.getName()).equalsIgnoreCase(ChatColor.stripColor(args[1]))) {
                    target = p;
                    break;
                }
            }
        }
        if (target == null) {
            sender.sendMessage(new Messaging.MessageFormatter().format("error.send-player-not-found"));
            return true;
        }
        AdminSendSelection.setSelection(admin.getUniqueId(), target.getUniqueId());
        sender.sendMessage(new Messaging.MessageFormatter()
                .setVariable("player", target.getName())
                .format("command.select-set"));
        return true;
    }
}
