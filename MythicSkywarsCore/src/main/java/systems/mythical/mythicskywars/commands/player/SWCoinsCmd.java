package systems.mythical.mythicskywars.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import systems.mythical.mythicskywars.commands.BaseCmd;
import systems.mythical.mythicskywars.utilities.Messaging;
import systems.mythical.mythicskywars.utilities.VaultUtils;

import java.util.Locale;
import java.util.UUID;

public class SWCoinsCmd extends BaseCmd {

    public SWCoinsCmd(String t) {
        type = t;
        forcePlayer = false;
        cmdName = "coins";
        alias = new String[]{"balance", "bal"};
        argLength = 1;
        maxArgs = 2;
    }

    @Override
    public boolean run(CommandSender sender, Player player, String[] args) {
        if (!VaultUtils.get().isEconomyAvailable() || !systems.mythical.mythicskywars.MythicSkywars.getCfg().economyEnabled()) {
            sender.sendMessage(new Messaging.MessageFormatter().format("coins.unavailable"));
            return true;
        }

        OfflinePlayer target = player;
        if (args.length >= 2) {
            if (sender instanceof Player && !sender.hasPermission("sw.admin")) {
                sender.sendMessage(new Messaging.MessageFormatter().format("error.cmd-no-perm"));
                return true;
            }
            Player onlineTarget = Bukkit.getPlayerExact(args[1]);
            if (onlineTarget != null) {
                target = onlineTarget;
            } else {
                try {
                    target = Bukkit.getOfflinePlayer(UUID.fromString(args[1]));
                } catch (IllegalArgumentException ignored) {
                    sender.sendMessage(new Messaging.MessageFormatter().setVariable("player", args[1]).format("coins.player-not-found"));
                    return true;
                }
            }
        } else if (target == null) {
            // Console must specify a player name
            return false;
        }

        String targetName = target.getName() != null ? target.getName() : target.getUniqueId().toString();
        double balance = VaultUtils.get().getBalance(target);
        sender.sendMessage(new Messaging.MessageFormatter()
                .setVariable("player", targetName)
                .setVariable("balance", String.format(Locale.ENGLISH, "%.2f", balance))
                .format("coins.balance"));
        return true;
    }
}
