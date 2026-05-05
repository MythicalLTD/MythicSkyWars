package systems.mythical.mythicskywars.commands.admin;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import systems.mythical.mythicskywars.commands.BaseCmd;
import systems.mythical.mythicskywars.utilities.Messaging;
import systems.mythical.mythicskywars.utilities.VaultUtils;

import java.util.Locale;
import java.util.UUID;

public class SWEcoCmd extends BaseCmd {

    public SWEcoCmd(String t) {
        type = t;
        forcePlayer = false;
        cmdName = "eco";
        alias = new String[]{"economy"};
        argLength = 2;
        maxArgs = 4;
    }

    @Override
    public boolean run(CommandSender sender, Player player, String[] args) {
        if (args.length < 2) {
            return false;
        }
        if (!VaultUtils.get().isEconomyAvailable()) {
            sender.sendMessage(new Messaging.MessageFormatter().format("coins.unavailable"));
            return true;
        }
        String mode = args[1].toLowerCase(Locale.ROOT);
        if ("help".equals(mode)) {
            return false;
        }
        if ("get".equals(mode) || "balance".equals(mode)) {
            if (args.length < 3) return false;
            OfflinePlayer target = resolveTarget(args[2]);
            if (target == null) {
                sender.sendMessage(new Messaging.MessageFormatter().setVariable("player", args[2]).format("coins.player-not-found"));
                return true;
            }
            double bal = VaultUtils.get().getBalance(target);
            sender.sendMessage(new Messaging.MessageFormatter()
                    .setVariable("player", target.getName() != null ? target.getName() : target.getUniqueId().toString())
                    .setVariable("balance", String.format(Locale.ENGLISH, "%.2f", bal))
                    .format("coins.balance"));
            return true;
        }

        if (args.length < 4) return false;
        OfflinePlayer target = resolveTarget(args[2]);
        if (target == null) {
            sender.sendMessage(new Messaging.MessageFormatter().setVariable("player", args[2]).format("coins.player-not-found"));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[3]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(new Messaging.MessageFormatter().format("error.must-be-int"));
            return true;
        }
        amount = Math.max(0D, amount);
        double current = VaultUtils.get().getBalance(target);
        boolean ok;
        double finalAmount;
        if ("set".equals(mode)) {
            ok = VaultUtils.get().setBalance(target.getUniqueId(), target.getName(), amount);
            finalAmount = amount;
        } else if ("give".equals(mode) || "add".equals(mode)) {
            ok = VaultUtils.get().setBalance(target.getUniqueId(), target.getName(), current + amount);
            finalAmount = current + amount;
        } else if ("take".equals(mode) || "remove".equals(mode) || "withdraw".equals(mode)) {
            finalAmount = Math.max(0D, current - amount);
            ok = VaultUtils.get().setBalance(target.getUniqueId(), target.getName(), finalAmount);
        } else {
            return false;
        }

        if (!ok) {
            sender.sendMessage(new Messaging.MessageFormatter().format("coins.unavailable"));
            return true;
        }

        sender.sendMessage(new Messaging.MessageFormatter()
                .setVariable("player", target.getName() != null ? target.getName() : target.getUniqueId().toString())
                .setVariable("balance", String.format(Locale.ENGLISH, "%.2f", finalAmount))
                .format("coins.balance"));
        return true;
    }

    private OfflinePlayer resolveTarget(String input) {
        if (input == null || input.trim().isEmpty()) return null;
        Player online = Bukkit.getPlayerExact(input);
        if (online != null) return online;
        try {
            UUID id = UUID.fromString(input);
            return Bukkit.getOfflinePlayer(id);
        } catch (IllegalArgumentException ignored) {
        }
        for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
            if (op != null && op.getName() != null && op.getName().equalsIgnoreCase(input)) {
                return op;
            }
        }
        return null;
    }
}
