package systems.mythical.mythicskywars.commands.admin;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.utilities.Messaging;
import systems.mythical.mythicskywars.utilities.UpdateChecker;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CheckUpdatesCmd extends systems.mythical.mythicskywars.commands.BaseCmd {
    public CheckUpdatesCmd(String t) {
        type = t;
        forcePlayer = false;
        cmdName = "checkupdates";
        alias = new String[]{"checkupdate", "cu"};
        argLength = 1;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        UpdateChecker updater = MythicSkywars.getUpdater();

        if (updater == null) {
            sender.sendMessage(new Messaging.MessageFormatter().format("command.update.unavailable"));
            return true;
        }

        boolean force = args.length > 1 && (args[1].equalsIgnoreCase("--force") || args[1].equalsIgnoreCase("-f"));

        if (force) {
            sender.sendMessage(new Messaging.MessageFormatter().format("command.update.force-checking"));
        } else {
            sender.sendMessage(new Messaging.MessageFormatter().format("command.update.checking"));
        }

        // Run the check async, then report back on the main thread
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(MythicSkywars.get(), () -> {
            if (force) {
                updater.clearDownloadState();
            }
            updater.checkNow();

            // Report result on main thread
            org.bukkit.Bukkit.getScheduler().runTask(MythicSkywars.get(), () -> {
                if (updater.isUpdateAvailable()) {
                    sender.sendMessage(new Messaging.MessageFormatter()
                            .setVariable("version", updater.getLatestVersion())
                            .setVariable("current", MythicSkywars.get().getDescription().getVersion())
                            .format("command.update.available"));

                    if (updater.isUpdateDownloaded()) {
                        sender.sendMessage(new Messaging.MessageFormatter().format("command.update.already-downloaded"));
                    } else {
                        sender.sendMessage(new Messaging.MessageFormatter().format("command.update.use-update-cmd"));
                    }
                } else {
                    sender.sendMessage(new Messaging.MessageFormatter()
                            .setVariable("version", MythicSkywars.get().getDescription().getVersion())
                            .format("command.update.up-to-date"));
                }
            });
        });

        return true;
    }
}
