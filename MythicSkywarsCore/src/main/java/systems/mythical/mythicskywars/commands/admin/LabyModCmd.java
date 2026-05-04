package systems.mythical.mythicskywars.commands.admin;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import systems.mythical.mythicskywars.clients.labymod.LabyModBridge;

public class LabyModCmd extends systems.mythical.mythicskywars.commands.BaseCmd {
    public LabyModCmd(String t) {
        type = t;
        forcePlayer = false;
        cmdName = "labymod";
        argLength = 1;
    }

    @Override
    public boolean run(CommandSender sender, Player player, String[] args) {
        sender.sendMessage(ChatColor.AQUA + "[LabyMod] " + ChatColor.GRAY + LabyModBridge.getStatusSummary());
        return true;
    }
}
