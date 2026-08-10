package systems.mythical.mythicskywars.database;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.enums.LeaderType;
import systems.mythical.mythicskywars.managers.PlayerStat;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DataStorage {

    private static DataStorage instance;

    public static DataStorage get() {
        if (DataStorage.instance == null) {
            DataStorage.instance = new DataStorage();
        }
        return DataStorage.instance;
    }

    public void saveStats(final PlayerStat pData) {
        boolean sqlEnabled = isSqlEnabledAndAvailable();
        if (!sqlEnabled) {
            try {
                File dataDirectory = MythicSkywars.get().getDataFolder();
                File playerDataDirectory = new File(dataDirectory, "player_data");

                if (!playerDataDirectory.exists() && !playerDataDirectory.mkdirs()) {
                    return;
                }

                File playerFile = new File(playerDataDirectory, pData.getId() + ".yml");
                if (!playerFile.exists()) {
                    if (!playerFile.createNewFile()) {
                        MythicSkywars.get().getLogger().warning("Could not create player file for " + pData.getId());
                        return;
                    }
                }

                copyDefaults(playerFile);
                FileConfiguration fc = YamlConfiguration.loadConfiguration(playerFile);
                fc.set("uuid", pData.getId());
                fc.set("player_name", pData.getPlayerName());
                fc.set("wins", pData.getWins());
                fc.set("losses", pData.getLosses());
                fc.set("kills", pData.getKills());
                fc.set("deaths", pData.getDeaths());
                fc.set("xp", pData.getXp());
                fc.set("pareffect", pData.getParticleEffect());
                fc.set("proeffect", pData.getProjectileEffect());
                fc.set("glasscolor", pData.getGlassColor());
                fc.set("killsound", pData.getKillSound());
                fc.set("winsound", pData.getWinSound());
                fc.set("taunt", pData.getTaunt());
                fc.set("souls", pData.getSouls());
                fc.set("soulwell_usages", pData.getSoulWellUsages());
                fc.set("soulwell_legendaries", pData.getSoulWellLegendaries());
                fc.set("soulwell_rares", pData.getSoulWellRares());
                fc.set("soulwell_souls_gathered", pData.getSoulWellSoulsGathered());
                fc.set("soulwell_souls_purchased", pData.getSoulWellSoulsPurchased());
                fc.set("prestige_icon", pData.getPrestigeIcon());
                fc.save(playerFile);

            } catch (IOException ioException) {
                MythicSkywars.get().getLogger().severe("Failed to load stats " + pData.getId() + ": " + ioException.getMessage());
            }
        } else {
            Database database = MythicSkywars.getDb();

            if (database.checkConnection()) {
                return;
            }

            Connection connection = database.getConnection();
            PreparedStatement preparedStatement = null;

            try {
                String query = "UPDATE `sw_player` SET `player_name` = ?, `wins` = ?, `losses` = ?, `kills` = ?, `deaths` = ?, `xp` = ?, `pareffect` = ?, " +
                        "`proeffect` = ?, `glasscolor` = ?,`killsound` = ?, `winsound` = ?, `taunt` = ?, `prestige_icon` = ?, `souls` = ?, `soulwell_usages` = ?, " +
                        "`soulwell_legendaries` = ?, `soulwell_rares` = ?, `soulwell_souls_gathered` = ?, `soulwell_souls_purchased` = ? WHERE `uuid` = ?;";

                preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, pData.getPlayerName());
                preparedStatement.setInt(2, pData.getWins());
                preparedStatement.setInt(3, pData.getLosses());
                preparedStatement.setInt(4, pData.getKills());
                preparedStatement.setInt(5, pData.getDeaths());
                preparedStatement.setInt(6, pData.getXp());
                preparedStatement.setString(7, pData.getParticleEffect());
                preparedStatement.setString(8, pData.getProjectileEffect());
                preparedStatement.setString(9, pData.getGlassColor());
                preparedStatement.setString(10, pData.getKillSound());
                preparedStatement.setString(11, pData.getWinSound());
                preparedStatement.setString(12, pData.getTaunt());
                preparedStatement.setString(13, pData.getPrestigeIcon());
                preparedStatement.setInt(14, pData.getSouls());
                preparedStatement.setInt(15, pData.getSoulWellUsages());
                preparedStatement.setInt(16, pData.getSoulWellLegendaries());
                preparedStatement.setInt(17, pData.getSoulWellRares());
                preparedStatement.setInt(18, pData.getSoulWellSoulsGathered());
                preparedStatement.setInt(19, pData.getSoulWellSoulsPurchased());
                preparedStatement.setString(20, pData.getId());
                preparedStatement.executeUpdate();

            } catch (final SQLException sqlException) {
                sqlException.printStackTrace();

            } finally {
                if (preparedStatement != null) {
                    try {
                        preparedStatement.close();
                    } catch (final SQLException ignored) {
                    }
                }
            }
        }
    }


    public void loadStats(final PlayerStat pData, Runnable postLoadStatsTask) {
        new BukkitRunnable() {
            @Override
            public void run() {
                boolean sqlEnabled = MythicSkywars.get().getConfig().getBoolean("sqldatabase.enabled");
                sqlEnabled = sqlEnabled && isSqlEnabledAndAvailable();
                if (sqlEnabled) {
                    Database database = MythicSkywars.getDb();

                    if (database.checkConnection()) {
                        // Never leave players stuck uninitialized — fall back to blank defaults so joins still work.
                        MythicSkywars.get().getLogger().severe("Failed to connect to the database; loading blank defaults for "
                                + pData.getPlayerName() + " so they can still play.");
                        applyBlankStatDefaults(pData);
                    } else if (!database.doesPlayerExist(pData.getId())) {
                        database.createNewPlayer(pData.getId(), pData.getPlayerName());
                        applyBlankStatDefaults(pData);
                    } else {
                        Connection connection = database.getConnection();
                        PreparedStatement preparedStatement = null;
                        ResultSet resultSet = null;

                        try {
                            String query = "SELECT `player_name`, `wins`, `losses`, `kills`, `deaths`, `xp`, `pareffect`, `proeffect`, `glasscolor`, `killsound`, `winsound`, `taunt`, `prestige_icon`, `souls`, `soulwell_usages`, `soulwell_legendaries`, `soulwell_rares`, `soulwell_souls_gathered`, `soulwell_souls_purchased` " +
                                    "FROM `sw_player` WHERE `uuid` = ? LIMIT 1;";

                            preparedStatement = connection.prepareStatement(query);
                            preparedStatement.setString(1, pData.getId());
                            resultSet = preparedStatement.executeQuery();

                            if (resultSet != null && resultSet.next()) {
                                String name = resolvePlayerName(pData.getId(), pData.getPlayerName(), resultSet.getString("player_name"));
                                if (name == null) name = pData.getPlayerName();
                                if (name == null) name = resultSet.getString("player_name");
                                pData.setPlayerName(name);
                                pData.setWins(resultSet.getInt("wins"));
                                pData.setLosts(resultSet.getInt("losses"));
                                pData.setKills(resultSet.getInt("kills"));
                                pData.setDeaths(resultSet.getInt("deaths"));
                                pData.setXp(resultSet.getInt("xp"));
                                pData.setParticleEffect(resultSet.getString("pareffect"));
                                pData.setProjectileEffect(resultSet.getString("proeffect"));
                                pData.setGlassColor(resultSet.getString("glasscolor"));
                                pData.setKillSound(resultSet.getString("killsound"));
                                pData.setWinSound(resultSet.getString("winsound"));
                                pData.setTaunt(resultSet.getString("taunt"));
                                String prestige = resultSet.getString("prestige_icon");
                                pData.setPrestigeIcon(prestige != null && !prestige.isEmpty() ? prestige : "icon1");
                                pData.setSouls(resultSet.getInt("souls"));
                                pData.setSoulWellUsages(resultSet.getInt("soulwell_usages"));
                                pData.setSoulWellLegendaries(resultSet.getInt("soulwell_legendaries"));
                                pData.setSoulWellRares(resultSet.getInt("soulwell_rares"));
                                pData.setSoulWellSoulsGathered(resultSet.getInt("soulwell_souls_gathered"));
                                pData.setSoulWellSoulsPurchased(resultSet.getInt("soulwell_souls_purchased"));
                            }

                        } catch (final SQLException sqlException) {
                            sqlException.printStackTrace();

                        } finally {
                            if (resultSet != null) {
                                try {
                                    resultSet.close();
                                } catch (final SQLException ignored) {
                                }
                            }

                            if (preparedStatement != null) {
                                try {
                                    preparedStatement.close();
                                } catch (final SQLException ignored) {
                                }
                            }
                        }
                    }
                } else {
                    try {
                        File playerDataDirectory = getPrimaryPlayerDataDirectory();

                        if (!playerDataDirectory.exists() && !playerDataDirectory.mkdirs()) {
                            MythicSkywars.get().getLogger().info("Encountered an error while creating data directory!");
                        }

                        File playerFile = new File(playerDataDirectory, pData.getId() + ".yml");
                        migrateFromSiblingDataDirsIfNeeded(pData, playerFile);
                        migrateLegacyNameFileIfNeeded(playerDataDirectory, pData, playerFile);
                        mergeLegacyNameFileIfUuidLooksBlank(playerDataDirectory, pData, playerFile);

                        if (!playerFile.exists() && !playerFile.createNewFile()) {
                            MythicSkywars.get().getLogger().info("Something strange is happening while saving!");
                        }

                        copyDefaults(playerFile);
                        FileConfiguration fc = YamlConfiguration.loadConfiguration(playerFile);
                        String name = resolvePlayerName(pData.getId(), pData.getPlayerName(), fc.getString("player_name", "null"));
                        if (name == null) name = pData.getPlayerName();
                        if (name == null) name = fc.getString("player_name", "null");
                        pData.setPlayerName(name);
                        pData.setWins(fc.getInt("wins", 0));
                        pData.setLosts(fc.getInt("losses", 0));
                        pData.setKills(fc.getInt("kills", 0));
                        pData.setDeaths(fc.getInt("deaths", 0));
                        pData.setXp(fc.getInt("xp", 0));
                        pData.setParticleEffect(fc.getString("pareffect", "none"));
                        pData.setProjectileEffect(fc.getString("proeffect", "none"));
                        pData.setGlassColor(fc.getString("glasscolor", "none"));
                        pData.setKillSound(fc.getString("killsound", "none"));
                        pData.setWinSound(fc.getString("winsound", "none"));
                        pData.setTaunt(fc.getString("taunt", "none"));
                        pData.setPrestigeIcon(fc.getString("prestige_icon", "icon1"));
                        pData.setSouls(fc.getInt("souls", 0));
                        pData.setSoulWellUsages(fc.getInt("soulwell_usages", 0));
                        pData.setSoulWellLegendaries(fc.getInt("soulwell_legendaries", 0));
                        pData.setSoulWellRares(fc.getInt("soulwell_rares", 0));
                        pData.setSoulWellSoulsGathered(fc.getInt("soulwell_souls_gathered", 0));
                        pData.setSoulWellSoulsPurchased(fc.getInt("soulwell_souls_purchased", 0));
                    } catch (IOException ioException) {
                        MythicSkywars.get().getLogger().severe("Failed to load player " + pData.getId() + ": " + ioException.getMessage());
                        applyBlankStatDefaults(pData);
                    }
                }
                // Always finish on the main thread so initialized flags and join logic see a consistent state.
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (postLoadStatsTask != null) {
                            postLoadStatsTask.run();
                        }
                    }
                }.runTask(MythicSkywars.get());
            }
        }.runTaskAsynchronously(MythicSkywars.get());
    }

    private void applyBlankStatDefaults(PlayerStat pData) {
        pData.setWins(0);
        pData.setLosts(0);
        pData.setKills(0);
        pData.setDeaths(0);
        pData.setXp(0);
        pData.setParticleEffect("none");
        pData.setProjectileEffect("none");
        pData.setGlassColor("none");
        pData.setKillSound("none");
        pData.setWinSound("none");
        pData.setTaunt("none");
        pData.setPrestigeIcon("icon1");
        pData.setSouls(0);
        pData.setSoulWellUsages(0);
        pData.setSoulWellLegendaries(0);
        pData.setSoulWellRares(0);
        pData.setSoulWellSoulsGathered(0);
        pData.setSoulWellSoulsPurchased(0);
    }

    private void copyDefaults(File playerFile) {
        FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
        Reader defConfigStream = new InputStreamReader(MythicSkywars.get().getResource("playerFile.yml"));
        YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
        playerConfig.setDefaults(defConfig);
        playerConfig.options().copyDefaults(true);
        try {
            playerConfig.save(playerFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String resolvePlayerName(String uuid, String fallbackOne, String fallbackTwo) {
        try {
            UUID id = UUID.fromString(uuid);
            Player online = Bukkit.getPlayer(id);
            if (online != null && online.getName() != null) {
                return online.getName();
            }
            OfflinePlayer offline = Bukkit.getOfflinePlayer(id);
            if (offline != null && offline.getName() != null) {
                return offline.getName();
            }
        } catch (IllegalArgumentException ignored) {
        }
        if (fallbackOne != null && !fallbackOne.trim().isEmpty()) {
            return fallbackOne;
        }
        if (fallbackTwo != null && !fallbackTwo.trim().isEmpty()) {
            return fallbackTwo;
        }
        return null;
    }

    private void migrateLegacyNameFileIfNeeded(File playerDataDirectory, PlayerStat pData, File uuidFile) {
        if (uuidFile.exists()) {
            return;
        }
        String playerName = pData.getPlayerName();
        if (playerName == null || playerName.trim().isEmpty()) {
            return;
        }
        File legacyNameFile = new File(playerDataDirectory, playerName + ".yml");
        if (legacyNameFile.exists()) {
            if (!legacyNameFile.renameTo(uuidFile)) {
                MythicSkywars.get().getLogger().warning("Failed to migrate legacy player file " + legacyNameFile.getName() + " -> " + uuidFile.getName());
            }
        }
    }

    private void mergeLegacyNameFileIfUuidLooksBlank(File playerDataDirectory, PlayerStat pData, File uuidFile) {
        if (!uuidFile.exists()) {
            return;
        }
        String playerName = pData.getPlayerName();
        if (playerName == null || playerName.trim().isEmpty()) {
            return;
        }
        File legacyNameFile = new File(playerDataDirectory, playerName + ".yml");
        if (!legacyNameFile.exists()) {
            return;
        }
        try {
            FileConfiguration uuidCfg = YamlConfiguration.loadConfiguration(uuidFile);
            FileConfiguration legacyCfg = YamlConfiguration.loadConfiguration(legacyNameFile);
            if (!looksLikeBlankStats(uuidCfg) || looksLikeBlankStats(legacyCfg)) {
                return;
            }
            uuidCfg.set("player_name", legacyCfg.getString("player_name", playerName));
            uuidCfg.set("wins", legacyCfg.getInt("wins", 0));
            uuidCfg.set("losses", legacyCfg.getInt("losses", 0));
            uuidCfg.set("kills", legacyCfg.getInt("kills", 0));
            uuidCfg.set("deaths", legacyCfg.getInt("deaths", 0));
            uuidCfg.set("xp", legacyCfg.getInt("xp", 0));
            uuidCfg.set("pareffect", legacyCfg.getString("pareffect", "none"));
            uuidCfg.set("proeffect", legacyCfg.getString("proeffect", "none"));
            uuidCfg.set("glasscolor", legacyCfg.getString("glasscolor", "none"));
            uuidCfg.set("killsound", legacyCfg.getString("killsound", "none"));
            uuidCfg.set("winsound", legacyCfg.getString("winsound", "none"));
            uuidCfg.set("taunt", legacyCfg.getString("taunt", "none"));
            uuidCfg.set("prestige_icon", legacyCfg.getString("prestige_icon",
                    uuidCfg.getString("prestige_icon", "icon1")));
            uuidCfg.set("souls", legacyCfg.getInt("souls", 0));
            uuidCfg.set("soulwell_usages", legacyCfg.getInt("soulwell_usages", 0));
            uuidCfg.set("soulwell_legendaries", legacyCfg.getInt("soulwell_legendaries", 0));
            uuidCfg.set("soulwell_rares", legacyCfg.getInt("soulwell_rares", 0));
            uuidCfg.set("soulwell_souls_gathered", legacyCfg.getInt("soulwell_souls_gathered", 0));
            uuidCfg.set("soulwell_souls_purchased", legacyCfg.getInt("soulwell_souls_purchased", 0));
            uuidCfg.save(uuidFile);
            MythicSkywars.get().getLogger().info("Merged legacy stats file " + legacyNameFile.getName() + " into " + uuidFile.getName());
        } catch (Exception ex) {
            MythicSkywars.get().getLogger().warning("Failed to merge legacy stats file for " + pData.getId() + ": " + ex.getMessage());
        }
    }

    private File getPrimaryPlayerDataDirectory() {
        File dataDirectory = MythicSkywars.get().getDataFolder();
        return new File(dataDirectory, "player_data");
    }

    private void migrateFromSiblingDataDirsIfNeeded(PlayerStat pData, File targetUuidFile) {
        boolean targetBlank = false;
        if (targetUuidFile.exists()) {
            FileConfiguration targetCfg = YamlConfiguration.loadConfiguration(targetUuidFile);
            if (!looksLikeBlankStats(targetCfg)) {
                return;
            }
            targetBlank = true;
        }
        String uuidFileName = pData.getId() + ".yml";
        String playerName = pData.getPlayerName();
        File dataFolder = MythicSkywars.get().getDataFolder();
        File pluginsDir = dataFolder.getParentFile();
        if (pluginsDir == null || !pluginsDir.exists()) {
            return;
        }

        String[] legacyPluginDirNames = new String[] {"SkyWars", "Skywars", "MythicSkywars"};
        for (String dirName : legacyPluginDirNames) {
            File legacyDir = new File(new File(pluginsDir, dirName), "player_data");
            if (!legacyDir.exists() || legacyDir.equals(targetUuidFile.getParentFile())) {
                continue;
            }

            File legacyUuidFile = new File(legacyDir, uuidFileName);
            if (legacyUuidFile.exists() && copyFileReplacingBlank(legacyUuidFile, targetUuidFile, targetBlank)) {
                MythicSkywars.get().getLogger().info("Recovered legacy stats from " + legacyUuidFile.getAbsolutePath());
                return;
            }

            if (playerName != null && !playerName.trim().isEmpty()) {
                File legacyNameFile = new File(legacyDir, playerName + ".yml");
                if (legacyNameFile.exists() && copyFileReplacingBlank(legacyNameFile, targetUuidFile, targetBlank)) {
                    MythicSkywars.get().getLogger().info("Recovered legacy name-based stats from " + legacyNameFile.getAbsolutePath());
                    return;
                }
            }
        }
    }

    private boolean copyFileReplacingBlank(File source, File target, boolean allowReplaceExisting) {
        if (!source.exists()) {
            return false;
        }
        if (target.exists() && !allowReplaceExisting) {
            return false;
        }
        try {
            java.nio.file.Files.createDirectories(target.getParentFile().toPath());
            java.nio.file.Files.copy(source.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception ex) {
            MythicSkywars.get().getLogger().warning("Failed to recover legacy stats file " + source.getAbsolutePath() + ": " + ex.getMessage());
            return false;
        }
    }

    private boolean looksLikeBlankStats(FileConfiguration cfg) {
        return cfg.getInt("wins", 0) == 0
                && cfg.getInt("losses", 0) == 0
                && cfg.getInt("kills", 0) == 0
                && cfg.getInt("deaths", 0) == 0
                && cfg.getInt("xp", 0) == 0
                && cfg.getInt("souls", 0) == 0
                && cfg.getInt("soulwell_usages", 0) == 0
                && cfg.getInt("soulwell_legendaries", 0) == 0
                && cfg.getInt("soulwell_rares", 0) == 0
                && cfg.getInt("soulwell_souls_gathered", 0) == 0
                && cfg.getInt("soulwell_souls_purchased", 0) == 0;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void removePlayerData(String uuid) {
        boolean sqlEnabled = isSqlEnabledAndAvailable();
        if (!sqlEnabled) {
            File dataDirectory = MythicSkywars.get().getDataFolder();
            File playerDataDirectory = new File(dataDirectory, "player_data");

            if (!playerDataDirectory.exists() && !playerDataDirectory.mkdirs()) {
                MythicSkywars.get().getLogger().info("Failed to create data directory");
            }

            File playerFile = new File(playerDataDirectory, uuid + ".yml");
            if (playerFile.exists()) {
                if (!playerFile.delete())
                    MythicSkywars.get().getLogger().info("Failed to delete playerfile for: " + uuid);
            }
        } else {
            Database database = MythicSkywars.getDb();

            if (database.checkConnection()) {
                return;
            }

            Connection connection = database.getConnection();
            PreparedStatement preparedStatement = null;

            try {
                String query = "DELETE FROM `sw_player` WHERE `uuid` = ?;";

                preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, uuid);
                preparedStatement.executeUpdate();

            } catch (final SQLException sqlException) {
                sqlException.printStackTrace();

            } finally {
                if (preparedStatement != null) {
                    try {
                        preparedStatement.close();
                    } catch (final SQLException ignored) {
                    }
                }
            }
        }
    }

    public void updateTop(LeaderType type, int size) {
        new BukkitRunnable() {

            @Override
            public void run() {
                boolean sqlEnabled = MythicSkywars.get().getConfig().getBoolean("sqldatabase.enabled");
                sqlEnabled = sqlEnabled && isSqlEnabledAndAvailable();
                if (sqlEnabled) {
                    Database database = MythicSkywars.getDb();

                    if (database.checkConnection()) {
                        return;
                    }

                    Connection connection = database.getConnection();
                    PreparedStatement preparedStatement;
                    ResultSet resultSet;

                    try {
                        String query = "SELECT `uuid`, `player_name`, `wins`, `losses`, `kills`, `deaths`, `xp` FROM `sw_player` GROUP BY `uuid` " +
                                "ORDER BY `" + type.toString().toLowerCase() + "` DESC LIMIT " + size + ";";

                        preparedStatement = connection.prepareStatement(query);
                        resultSet = preparedStatement.executeQuery();
                        MythicSkywars.getLB().resetLeader(type);
                        while (resultSet.next()) {
                            String uuid = resultSet.getString("uuid");
                            String name = Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName();
                            if (name == null) name = resultSet.getString("player_name");
                            int wins = resultSet.getInt("wins");
                            int losses = resultSet.getInt("losses");
                            int kills = resultSet.getInt("kills");
                            int deaths = resultSet.getInt( "deaths");
                            int xp = resultSet.getInt("xp");
                            MythicSkywars.getLB().addLeader(type, uuid, name, wins, losses, kills, deaths, xp);
                        }

                    } catch (final SQLException sqlException) {
                        sqlException.printStackTrace();

                    }
                } else {
                    File dataDirectory = MythicSkywars.get().getDataFolder();
                    File playerDirectory = new File(dataDirectory, "player_data");

                    if (!playerDirectory.exists()) {
                        if (!playerDirectory.mkdirs()) {
                            return;
                        }
                    }

                    File[] playerFiles = playerDirectory.listFiles();
                    if (playerFiles == null) {
                        return;
                    }

                    MythicSkywars.getLB().resetLeader(type);
                    for (File playerFile : playerFiles) {
                        if (!playerFile.getName().endsWith(".yml")) {
                            continue;
                        }

                        FileConfiguration fc = YamlConfiguration.loadConfiguration(playerFile);

                        String uuid = playerFile.getName().replace(".yml", "");
                        String name = Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName();
                        if (name == null) name = fc.getString("player_name", "null");
                        int wins = fc.getInt("wins", 0);
                        int losses = fc.getInt("losses", 0);
                        int kills = fc.getInt("kills", 0);
                        int deaths = fc.getInt("deaths", 0);
                        int xp = fc.getInt("xp", 0);
                        MythicSkywars.getLB().addLeader(type, uuid, name, wins, losses, kills, deaths, xp);
                    }
                }
                MythicSkywars.getLB().finishedLoading(type);
            }
        }.runTaskLaterAsynchronously(MythicSkywars.get(), 10L);
    }

    public void loadperms(PlayerStat playerStat) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isSqlEnabledAndAvailable()) {
                    try {
                        File dataDirectory = MythicSkywars.get().getDataFolder();
                        File playerDataDirectory = new File(dataDirectory, "player_data");

                        if (!playerDataDirectory.exists() && !playerDataDirectory.mkdirs()) {
                            MythicSkywars.get().getLogger().severe("Failed to load player " + playerStat.getPlayerName() + ": Could not create player_data directory.");
                            return;
                        }

                        File playerFile = new File(playerDataDirectory, playerStat.getId() + ".yml");
                        if (!playerFile.exists() && !playerFile.createNewFile()) {
                            MythicSkywars.get().getLogger().severe("Failed to load player " + playerStat.getPlayerName() + ": Could not create player file.");
                            return;
                        }
                        copyDefaults(playerFile);
                        FileConfiguration fc = YamlConfiguration.loadConfiguration(playerFile);

                        List<String> perms = fc.getStringList("permissions");
                        for (String perm : perms) {
                            playerStat.addPerm(perm, false);
                        }
                    } catch (IOException ioException) {
                        MythicSkywars.get().getLogger().severe("Failed to load player " + playerStat.getPlayerName() + ": " + ioException.getMessage());
                    }
                } else {
                    Database database = MythicSkywars.getDb();
                    if (database.checkConnection()) {
                        return;
                    }
                    Connection connection = database.getConnection();
                    PreparedStatement preparedStatement = null;
                    ResultSet resultSet = null;

                    try {
                        String query = "SELECT `permissions` FROM `sw_permissions` WHERE `uuid` = ?;";

                        preparedStatement = connection.prepareStatement(query);
                        preparedStatement.setString(1, playerStat.getId());
                        resultSet = preparedStatement.executeQuery();

                        while (resultSet != null && resultSet.next()) {
                            playerStat.addPerm(resultSet.getString("permissions"), false);
                        }
                    } catch (final SQLException sqlException) {
                        sqlException.printStackTrace();

                    } finally {
                        if (resultSet != null) {
                            try {
                                resultSet.close();
                            } catch (final SQLException ignored) {
                            }
                        }
                        if (preparedStatement != null) {
                            try {
                                preparedStatement.close();
                            } catch (final SQLException ignored) {
                            }
                        }
                    }
                }
            }

        }.runTaskAsynchronously(MythicSkywars.get());
    }

    public void savePerms(PlayerStat playerStat) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isSqlEnabledAndAvailable()) {
                    try {
                        File dataDirectory = MythicSkywars.get().getDataFolder();
                        File playerDataDirectory = new File(dataDirectory, "player_data");

                        if (!playerDataDirectory.exists() && !playerDataDirectory.mkdirs()) {
                            MythicSkywars.get().getLogger().severe("Failed to load player " + playerStat.getPlayerName() + ": Could not create player_data directory.");
                            return;
                        }

                        File playerFile = new File(playerDataDirectory, playerStat.getId() + ".yml");
                        if (!playerFile.exists() && !playerFile.createNewFile()) {
                            MythicSkywars.get().getLogger().severe("Failed to load player " + playerStat.getPlayerName() + ": Could not create player file.");
                            return;
                        }
                        copyDefaults(playerFile);
                        FileConfiguration fc = YamlConfiguration.loadConfiguration(playerFile);

                        List<String> perms = new ArrayList<>(playerStat.getPerms().getPermissions().keySet());
                        fc.set("permissions", perms);
                        fc.save(playerFile);

                    } catch (IOException ioException) {
                        MythicSkywars.get().getLogger().severe("Failed to load player " + playerStat.getPlayerName() + ": " + ioException.getMessage());
                    }
                } else {
                    if (playerStat.getPerms().getPermissions().size() > 0) {
                        Database database = MythicSkywars.getDb();
                        if (database.checkConnection()) {
                            return;
                        }
                        Connection connection = database.getConnection();
                        PreparedStatement preparedStatement = null;
                        try {
                            if (playerStat.getPerms().getPermissions().size() >= 1) {
                                for (String perm : playerStat.getPerms().getPermissions().keySet()) {
                                    String query = "INSERT INTO `sw_permissions` (`uuid`, `playername`, `permissions`) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE " +
                                            "`uuid`=`uuid`, `playername`=`playername`, `permissions`=`permissions` ";

                                    preparedStatement = connection.prepareStatement(query);
                                    preparedStatement.setString(1, playerStat.getId());
                                    preparedStatement.setString(2, playerStat.getPlayerName());
                                    preparedStatement.setString(3, perm);
                                    preparedStatement.executeUpdate();
                                }
                            }
                        } catch (final SQLException sqlException) {
                            sqlException.printStackTrace();
                        } finally {
                            if (preparedStatement != null) {
                                try {
                                    preparedStatement.close();
                                } catch (final SQLException ignored) {
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskAsynchronously(MythicSkywars.get());
    }


    private boolean isSqlEnabledAndAvailable() {
        if (!MythicSkywars.get().getConfig().getBoolean("sqldatabase.enabled")) {
            return false;
        }
        Database database = MythicSkywars.getDb();
        if (database == null) {
            return false;
        }
        return !database.checkConnection();
    }
}