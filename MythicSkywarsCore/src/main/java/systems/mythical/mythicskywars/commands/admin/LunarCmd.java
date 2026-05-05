package systems.mythical.mythicskywars.commands.admin;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import systems.mythical.mythicskywars.clients.lunar.LunarApolloBridge;

public class LunarCmd extends systems.mythical.mythicskywars.commands.BaseCmd {
    public LunarCmd(String t) {
        type = t;
        forcePlayer = false;
        cmdName = "lunar";
        argLength = 1;
    }

    @Override
    public boolean run(CommandSender sender, Player player, String[] args) {
        try {
            sender.sendMessage(ChatColor.AQUA + "[Lunar] " + ChatColor.GRAY + LunarApolloBridge.getStatusSummary());
        } catch (NoClassDefFoundError e) {
            sender.sendMessage(ChatColor.RED + "[Lunar] Apollo classes not available (Apollo plugin not installed)");
        }
        return true;
    }
}
