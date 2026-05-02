package systems.mythical.mythicskywars.commands.admin;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.utilities.Messaging;
import systems.mythical.mythicskywars.utilities.UpdateChecker;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UpdateCmd extends systems.mythical.mythicskywars.commands.BaseCmd {
    public UpdateCmd(String t) {
        type = t;
        forcePlayer = false;
        cmdName = "update";
        alias = new String[]{"upgrade"};
        argLength = 1;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        UpdateChecker updater = MythicSkywars.getUpdater();

        if (updater == null) {
            sender.sendMessage(new Messaging.MessageFormatter().format("command.update.unavailable"));
            return true;
        }

        if (!updater.isUpdateAvailable()) {
            sender.sendMessage(new Messaging.MessageFormatter()
                    .setVariable("version", MythicSkywars.get().getDescription().getVersion())
                    .format("command.update.up-to-date"));
            return true;
        }

        if (updater.isUpdateDownloaded()) {
            sender.sendMessage(new Messaging.MessageFormatter().format("command.update.already-downloaded"));
            sender.sendMessage(new Messaging.MessageFormatter().format("command.update.restart"));
            return true;
        }

        if (updater.getDownloadUrl() == null) {
            sender.sendMessage(new Messaging.MessageFormatter().format("command.update.no-download"));
            return true;
        }

        sender.sendMessage(new Messaging.MessageFormatter()
                .setVariable("version", updater.getLatestVersion())
                .format("command.update.downloading"));

        // Download async
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(MythicSkywars.get(), () -> {
            boolean success = updater.downloadUpdateNow();

            // Report result on main thread
            org.bukkit.Bukkit.getScheduler().runTask(MythicSkywars.get(), () -> {
                if (success) {
                    sender.sendMessage(new Messaging.MessageFormatter()
                            .setVariable("version", updater.getLatestVersion())
                            .format("command.update.downloaded"));
                    sender.sendMessage(new Messaging.MessageFormatter().format("command.update.restart"));
                } else {
                    sender.sendMessage(new Messaging.MessageFormatter().format("command.update.download-failed"));
                }
            });
        });

        return true;
    }
}
