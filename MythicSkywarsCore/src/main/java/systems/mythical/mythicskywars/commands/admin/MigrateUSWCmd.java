package systems.mythical.mythicskywars.commands.admin;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.commands.BaseCmd;
import systems.mythical.mythicskywars.database.UltraSkyWarsMongoMigrator;
import systems.mythical.mythicskywars.utilities.Messaging;
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
        final long[] lastProgressSentAt = {0L};
        final long progressIntervalMillis = 5000L;

        sender.sendMessage(new Messaging.MessageFormatter().format("command.migrateusw-start"));
        sender.sendMessage(new Messaging.MessageFormatter()
                .setVariable("mode", overwrite ? "overwrite" : "skip")
                .format("command.migrateusw-mode"));

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    UltraSkyWarsMongoMigrator.MigrationResult result = UltraSkyWarsMongoMigrator.migrateFromConfig(overwrite, snapshot -> {
                        long now = System.currentTimeMillis();
                        if (now - lastProgressSentAt[0] < progressIntervalMillis) {
                            return;
                        }
                        lastProgressSentAt[0] = now;
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                String total = snapshot.getTotal() > 0 ? Long.toString(snapshot.getTotal()) : "?";
                                String eta = snapshot.getEtaMillis() >= 0L ? formatDuration(snapshot.getEtaMillis()) : "unknown";
                                sender.sendMessage("§b§lSkyWars §7▸ §r§7USW migration progress: §b"
                                        + snapshot.getScanned() + "§7/§b" + total
                                        + " §7| imported: §a" + snapshot.getImported()
                                        + " §7| skipped: §e" + snapshot.getSkipped()
                                        + " §7| failed: §c" + snapshot.getFailed()
                                        + " §7| elapsed: §f" + formatDuration(snapshot.getElapsedMillis())
                                        + " §7| rate: §f" + String.format("%.2f", snapshot.getDocsPerSecond()) + "/s"
                                        + " §7| eta: §f" + eta);
                            }
                        }.runTask(MythicSkywars.get());
                    });
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
                            String total = result.getTotal() > 0 ? Long.toString(result.getTotal()) : "?";
                            sender.sendMessage("§b§lSkyWars §7▸ §r§7USW migration timing: total=§b" + total
                                    + " §7| elapsed=§f" + formatDuration(result.getElapsedMillis())
                                    + " §7| avgRate=§f" + String.format("%.2f", (result.getElapsedMillis() <= 0L ? 0D : (result.getScanned() / Math.max(0.001D, result.getElapsedMillis() / 1000D)))) + "/s");
                        }
                    }.runTask(MythicSkywars.get());
                } catch (Exception ex) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            sender.sendMessage(new Messaging.MessageFormatter()
                                    .setVariable("error", ex.getMessage() == null ? "unknown error" : ex.getMessage())
                                    .format("error.migrateusw-failed"));
                        }
                    }.runTask(MythicSkywars.get());
                }
            }
        }.runTaskAsynchronously(MythicSkywars.get());
        return true;
    }

    private static String formatDuration(long millis) {
        if (millis < 0L) {
            return "unknown";
        }
        long totalSeconds = millis / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) {
            return hours + "h " + minutes + "m " + seconds + "s";
        }
        if (minutes > 0L) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }
}
