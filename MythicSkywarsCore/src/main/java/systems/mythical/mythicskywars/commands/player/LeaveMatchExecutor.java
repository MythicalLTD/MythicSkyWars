package systems.mythical.mythicskywars.commands.player;

import systems.mythical.mythicskywars.managers.MatchManager;
import systems.mythical.mythicskywars.utilities.Messaging;
import systems.mythical.mythicskywars.utilities.Util;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Standalone {@code /leave} command (same behaviour as {@code /sw quit}).
 */
public class LeaveMatchExecutor implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(new Messaging.MessageFormatter().format("error.must-be-player"));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("sw.leave") && !Util.get().hasPerm("sw", player, "quit")) {
            sender.sendMessage(new Messaging.MessageFormatter().format("error.cmd-no-perm"));
            return true;
        }
        if (!MatchManager.get().quitCurrentGame(player)) {
            player.sendMessage(new Messaging.MessageFormatter().format("error.leave-not-in-game"));
        }
        return true;
    }
}
