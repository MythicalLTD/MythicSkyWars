package systems.mythical.mythicskywars.commands.maps;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.utilities.Messaging;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Copies a solo map as a teams (duo) map.
 * Converts solo spawns (list) to team spawns (team-0, team-1, etc.).
 * Also copies the world folder.
 *
 * Usage: /swm copyasduo <soloMapName> [teamSize]
 * Example: /swm copyasduo sw10 2
 */
public class CopySoloAsDuoCmd extends systems.mythical.mythicskywars.commands.BaseCmd {

    public CopySoloAsDuoCmd(String t) {
        type = t;
        forcePlayer = false;
        cmdName = "copyasduo";
        alias = new String[]{"copySoloAsDuo", "copyasteam", "soloasduo", "clone"};
        argLength = 2;
    }

    @Override
    public boolean run(CommandSender sender, Player player, String[] args) {
        String soloName = args[1];
        int teamSize = 2;
        if (args.length > 2) {
            try {
                teamSize = Integer.parseInt(args[2]);
                if (teamSize < 2) teamSize = 2;
            } catch (NumberFormatException e) {
                sender.sendMessage("§cTeam size must be a number (default: 2).");
                return true;
            }
        }

        // Determine target name: if solo starts with "sw", replace with "swt"
        String targetName;
        if (soloName.startsWith("sw") && !soloName.startsWith("swt")) {
            targetName = "swt" + soloName.substring(2);
        } else {
            targetName = soloName + "_teams";
        }

        File dataDir = new File(MythicSkywars.get().getDataFolder(), "mapsData");
        File mapsDir = new File(MythicSkywars.get().getDataFolder(), "maps");

        File soloDataFile = new File(dataDir, soloName + ".yml");
        if (!soloDataFile.exists()) {
            sender.sendMessage("§cMap data file not found: §e" + soloName + ".yml");
            sender.sendMessage("§7Make sure the solo map exists in plugins/MythicSkywars/mapsData/");
            return true;
        }

        File targetDataFile = new File(dataDir, targetName + ".yml");
        File soloWorldDir = new File(mapsDir, soloName);
        File targetWorldDir = new File(mapsDir, targetName);

        final int finalTeamSize = teamSize;
        final String fTargetName = targetName;

        sender.sendMessage("§a[MythicSkywars] §7Converting §e" + soloName + " §7-> §e" + fTargetName + " §7(team size: " + finalTeamSize + ")...");

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    // 1. Load solo map data
                    FileConfiguration soloCfg = YamlConfiguration.loadConfiguration(soloDataFile);

                    // 2. Create target config (copy everything)
                    FileConfiguration targetCfg = YamlConfiguration.loadConfiguration(soloDataFile);

                    // 3. Convert spawns from list to team format
                    List<String> soloSpawns = soloCfg.getStringList("spawns");
                    if (soloSpawns != null && !soloSpawns.isEmpty()) {
                        // Remove the solo spawns list
                        targetCfg.set("spawns", null);

                        // Create team spawns section
                        for (int i = 0; i < soloSpawns.size(); i++) {
                            List<String> teamSpawnList = new ArrayList<>();
                            teamSpawnList.add(soloSpawns.get(i));
                            targetCfg.set("spawns.team-" + i, teamSpawnList);
                        }
                    } else {
                        // Check if spawns is already a section (team format)
                        ConfigurationSection spawnsSection = soloCfg.getConfigurationSection("spawns");
                        if (spawnsSection != null) {
                            // Already team format, just copy as-is
                            sender.sendMessage("§7Spawns already in team format, copying directly.");
                        }
                    }

                    // 4. Set team size and min players
                    targetCfg.set("teamSize", finalTeamSize);
                    int numTeamsCalc = soloSpawns != null ? soloSpawns.size() : 0;
                    if (numTeamsCalc == 0) {
                        ConfigurationSection ss = soloCfg.getConfigurationSection("spawns");
                        if (ss != null) numTeamsCalc = ss.getKeys(false).size();
                    }
                    final int numTeams = numTeamsCalc;
                    int minPlayers = Math.max(finalTeamSize * 2, 4);
                    targetCfg.set("minplayers", minPlayers);

                    // 5. Keep registered as false so admin can review before registering
                    targetCfg.set("registered", false);

                    // 6. Update display name
                    String displayName = soloCfg.getString("displayname", soloName);
                    if (!displayName.toLowerCase().contains("team")) {
                        targetCfg.set("displayname", displayName + " (Teams)");
                    }

                    // 7. Save target data file
                    targetCfg.save(targetDataFile);

                    // 8. Copy world folder
                    boolean worldCopied = false;
                    if (soloWorldDir.exists() && soloWorldDir.isDirectory()) {
                        copyDirectory(soloWorldDir.toPath(), targetWorldDir.toPath());
                        worldCopied = true;
                    }

                    // Report back on main thread
                    final boolean fWorldCopied = worldCopied;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            sender.sendMessage("§a[MythicSkywars] §aSuccessfully created §e" + fTargetName + "§a!");
                            sender.sendMessage("§7  Map data: §f" + targetDataFile.getName());
                            if (fWorldCopied) {
                                sender.sendMessage("§7  World folder: §fcopied");
                            } else {
                                sender.sendMessage("§7  World folder: §cskipped (source not found)");
                            }
                            sender.sendMessage("§7  Team size: §f" + finalTeamSize);
                            sender.sendMessage("§7  Teams: §f" + numTeams);
                            sender.sendMessage("§7  Min players: §f" + minPlayers);
                            sender.sendMessage("");
                            sender.sendMessage("§eNext steps:");
                            sender.sendMessage("§7  1. Review the map with §f/swm edit " + fTargetName);
                            sender.sendMessage("§7  2. Register it with §f/swm register " + fTargetName);
                        }
                    }.runTask(MythicSkywars.get());

                } catch (Exception e) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            sender.sendMessage("§c[MythicSkywars] Error copying map: " + e.getMessage());
                        }
                    }.runTask(MythicSkywars.get());
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(MythicSkywars.get());

        return true;
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        if (Files.exists(target)) {
            deleteDirectory(target);
        }
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                // Skip uid.dat and session.lock to avoid world conflicts
                String name = file.getFileName().toString();
                if ("uid.dat".equals(name) || "session.lock".equals(name)) {
                    return FileVisitResult.CONTINUE;
                }
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void deleteDirectory(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                Files.delete(d);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
