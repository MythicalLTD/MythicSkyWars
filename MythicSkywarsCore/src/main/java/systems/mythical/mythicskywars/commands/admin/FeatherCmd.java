package systems.mythical.mythicskywars.commands.admin;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import systems.mythical.mythicskywars.clients.feather.FeatherClientBridge;

public class FeatherCmd extends systems.mythical.mythicskywars.commands.BaseCmd {
    public FeatherCmd(String t) {
        type = t;
        forcePlayer = false;
        cmdName = "feather";
        argLength = 1;
    }

    @Override
    public boolean run(CommandSender sender, Player player, String[] args) {
        Player senderPlayer = sender instanceof Player ? (Player) sender : null;
        sender.sendMessage(ChatColor.AQUA + "[Feather] " + ChatColor.GRAY + FeatherClientBridge.getStatusSummary());
        sender.sendMessage(ChatColor.AQUA + "[Feather] " + ChatColor.GRAY + "sync{" + FeatherClientBridge.syncTrackedPlayers(false) + "}");
        sender.sendMessage(ChatColor.AQUA + "[Feather] " + ChatColor.GRAY + FeatherClientBridge.getPolicySummary());
        if (senderPlayer != null) {
            sender.sendMessage(ChatColor.AQUA + "[Feather] " + ChatColor.GRAY + "you{" + FeatherClientBridge.debugPlayerState(senderPlayer) + "}");
        }
        return true;
    }
}
