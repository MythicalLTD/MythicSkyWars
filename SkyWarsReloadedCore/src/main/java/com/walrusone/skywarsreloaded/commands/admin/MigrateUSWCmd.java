package com.walrusone.skywarsreloaded.commands.admin;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.commands.BaseCmd;
import com.walrusone.skywarsreloaded.database.UltraSkyWarsMongoMigrator;
import com.walrusone.skywarsreloaded.utilities.Messaging;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class MigrateUSWCmd extends BaseCmd {

    public MigrateUSWCmd(String t) {
        type = t;
        forcePlayer = false;
        cmdName = "migrateusw";
        alias = new String[]{"uswmigrate", "importusw"};
        argLength = 1;
        maxArgs = 2;
    }

    @Override
    public boolean run(CommandSender sender, Player player, String[] args) {
        boolean overwrite = args.length >= 2 && "overwrite".equalsIgnoreCase(args[1]);
        if (args.length >= 2 && !overwrite) {
            return false;
        }

        sender.sendMessage(new Messaging.MessageFormatter().format("command.migrateusw-start"));
        sender.sendMessage(new Messaging.MessageFormatter()
                .setVariable("mode", overwrite ? "overwrite" : "skip")
                .format("command.migrateusw-mode"));

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    UltraSkyWarsMongoMigrator.MigrationResult result = UltraSkyWarsMongoMigrator.migrateFromConfig(overwrite);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            sender.sendMessage(new Messaging.MessageFormatter().format("command.migrateusw-finished"));
                            sender.sendMessage(new Messaging.MessageFormatter()
                                    .setVariable("scanned", Integer.toString(result.getScanned()))
                                    .setVariable("imported", Integer.toString(result.getImported()))
                                    .setVariable("skipped", Integer.toString(result.getSkipped()))
                                    .setVariable("failed", Integer.toString(result.getFailed()))
                                    .format("command.migrateusw-summary"));
                        }
                    }.runTask(SkyWarsReloaded.get());
                } catch (Exception ex) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            sender.sendMessage(new Messaging.MessageFormatter()
                                    .setVariable("error", ex.getMessage() == null ? "unknown error" : ex.getMessage())
                                    .format("error.migrateusw-failed"));
                        }
                    }.runTask(SkyWarsReloaded.get());
                }
            }
        }.runTaskAsynchronously(SkyWarsReloaded.get());
        return true;
    }
}
