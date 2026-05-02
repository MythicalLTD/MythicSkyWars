package com.walrusone.skywarsreloaded.database;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.utilities.LevelManager;
import com.walrusone.skywarsreloaded.utilities.VaultUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UltraSkyWarsMongoMigrator {
    private static final Pattern INT_FIELD_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(-?\\d+)");
    private static final Pattern STRING_FIELD_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"");

    private UltraSkyWarsMongoMigrator() {
    }

    public static final class MigrationResult {
        private int scanned;
        private int imported;
        private int skipped;
        private int failed;

        public int getScanned() {
            return scanned;
        }

        public int getImported() {
            return imported;
        }

        public int getSkipped() {
            return skipped;
        }

        public int getFailed() {
            return failed;
        }
    }

    public static MigrationResult migrateFromConfig(boolean overwrite) throws Exception {
        FileConfiguration cfg = SkyWarsReloaded.get().getConfig();
        String root = "migration.ultimateskywars.mongodb.";
        String host = cfg.getString(root + "host", "").trim();
        int port = cfg.getInt(root + "port", 27017);
        String databaseName = cfg.getString(root + "database", "").trim();
        String username = cfg.getString(root + "username", "").trim();
        String password = cfg.getString(root + "password", "");
        String collectionName = cfg.getString(root + "collection", "players").trim();

        if (host.isEmpty() || databaseName.isEmpty()) {
            throw new IllegalArgumentException("Missing migration MongoDB config. Set migration.ultimateskywars.mongodb.host and database.");
        }

        MongoClient mongoClient;
        if (!username.isEmpty()) {
            MongoCredential credential = MongoCredential.createCredential(
                    username,
                    databaseName,
                    password.toCharArray()
            );
            mongoClient = new MongoClient(new ServerAddress(host, port), Collections.singletonList(credential));
        } else {
            mongoClient = new MongoClient(new ServerAddress(host, port));
        }

        MigrationResult result = new MigrationResult();
        try {
            DB database = mongoClient.getDB(databaseName);
            DBCollection players = database.getCollection(collectionName);
            DBCursor cursor = players.find(new BasicDBObject());
            while (cursor.hasNext()) {
                DBObject playerDoc = cursor.next();
                result.scanned++;
                try {
                    if (migrateOne(playerDoc, overwrite)) {
                        result.imported++;
                    } else {
                        result.skipped++;
                    }
                } catch (Exception ex) {
                    result.failed++;
                    SkyWarsReloaded.get().getLogger().warning("Failed to migrate one USW record: " + ex.getMessage());
                }
            }
        } finally {
            mongoClient.close();
        }
        return result;
    }

    private static boolean migrateOne(DBObject playerDoc, boolean overwrite) throws Exception {
        String uuidRaw = asString(playerDoc.get("uuid"));
        if (uuidRaw == null) {
            return false;
        }

        UUID uuid;
        try {
            uuid = UUID.fromString(uuidRaw);
        } catch (IllegalArgumentException ignored) {
            return false;
        }

        String uuidText = uuid.toString();
        String skywarsJson = asString(playerDoc.get("skywars"));
        String name = choosePreferredName(
                asString(playerDoc.get("name")),
                extractJsonString(skywarsJson, "name", null),
                null
        );

        MigratedStats stats = readStats(playerDoc, skywarsJson);

        boolean sqlEnabled = SkyWarsReloaded.get().getConfig().getBoolean("sqldatabase.enabled");
        boolean changed;
        if (sqlEnabled) {
            changed = migrateToSql(uuidText, name, stats, overwrite);
        } else {
            changed = migrateToYaml(uuidText, name, stats, overwrite);
        }
        if (changed && SkyWarsReloaded.getCfg().economyEnabled() && VaultUtils.get().isEconomyAvailable()) {
            VaultUtils.get().setBalance(uuid, name, Math.max(0D, stats.coins));
        }
        return changed;
    }

    private static boolean migrateToSql(String uuid, String name, MigratedStats stats, boolean overwrite) throws SQLException {
        Database database = SkyWarsReloaded.getDb();
        if (database == null || database.checkConnection()) {
            throw new SQLException("SkyWarsReloaded SQL database is not available.");
        }

        Connection connection = database.getConnection();
        if (connection == null) {
            throw new SQLException("SkyWarsReloaded SQL connection is null.");
        }

        if (playerExistsSql(connection, uuid)) {
            if (!overwrite) {
                return false;
            }
            String existingName = getExistingPlayerNameSql(connection, uuid);
            name = choosePreferredName(name, existingName, uuid);
            PreparedStatement update = null;
            try {
                update = connection.prepareStatement(
                        "UPDATE `sw_player` SET `player_name` = ?, `wins` = ?, `losses` = ?, `kills` = ?, `deaths` = ?, `xp` = ?, `pareffect` = ?, `proeffect` = ?, `glasscolor` = ?, `killsound` = ?, `winsound` = ?, `taunt` = ?, `prestige_icon` = ?, `souls` = ?, `soulwell_usages` = ?, `soulwell_legendaries` = ?, `soulwell_rares` = ?, `soulwell_souls_gathered` = ?, `soulwell_souls_purchased` = ? WHERE `uuid` = ?;"
                );
                update.setString(1, name);
                update.setInt(2, stats.wins);
                update.setInt(3, stats.losses);
                update.setInt(4, stats.kills);
                update.setInt(5, stats.deaths);
                update.setInt(6, stats.xp);
                update.setString(7, stats.particleEffect);
                update.setString(8, stats.projectileEffect);
                update.setString(9, stats.glassColor);
                update.setString(10, stats.killSound);
                update.setString(11, stats.winSound);
                update.setString(12, stats.taunt);
                update.setString(13, stats.prestigeIcon != null ? stats.prestigeIcon : "icon1");
                update.setInt(14, stats.souls);
                update.setInt(15, stats.soulWellUsages);
                update.setInt(16, stats.soulWellLegendaries);
                update.setInt(17, stats.soulWellRares);
                update.setInt(18, stats.soulWellSoulsGathered);
                update.setInt(19, stats.soulWellSoulsPurchased);
                update.setString(20, uuid);
                update.executeUpdate();
            } finally {
                if (update != null) {
                    update.close();
                }
            }
            return true;
        }

        PreparedStatement insert = null;
        try {
            name = choosePreferredName(name, null, uuid);
            insert = connection.prepareStatement(
                    "INSERT INTO `sw_player` (`player_id`, `uuid`, `player_name`, `wins`, `losses`, `kills`, `deaths`, `xp`, `pareffect`, `proeffect`, `glasscolor`, `killsound`, `winsound`, `taunt`, `prestige_icon`, `souls`, `soulwell_usages`, `soulwell_legendaries`, `soulwell_rares`, `soulwell_souls_gathered`, `soulwell_souls_purchased`) " +
                            "VALUES (NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
            );
            insert.setString(1, uuid);
            insert.setString(2, name);
            insert.setInt(3, stats.wins);
            insert.setInt(4, stats.losses);
            insert.setInt(5, stats.kills);
            insert.setInt(6, stats.deaths);
            insert.setInt(7, stats.xp);
            insert.setString(8, stats.particleEffect);
            insert.setString(9, stats.projectileEffect);
            insert.setString(10, stats.glassColor);
            insert.setString(11, stats.killSound);
            insert.setString(12, stats.winSound);
            insert.setString(13, stats.taunt);
            insert.setString(14, stats.prestigeIcon != null ? stats.prestigeIcon : "icon1");
            insert.setInt(15, stats.souls);
            insert.setInt(16, stats.soulWellUsages);
            insert.setInt(17, stats.soulWellLegendaries);
            insert.setInt(18, stats.soulWellRares);
            insert.setInt(19, stats.soulWellSoulsGathered);
            insert.setInt(20, stats.soulWellSoulsPurchased);
            insert.executeUpdate();
            return true;
        } finally {
            if (insert != null) {
                insert.close();
            }
        }
    }

    private static boolean playerExistsSql(Connection connection, String uuid) throws SQLException {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.prepareStatement("SELECT COUNT(`player_id`) FROM `sw_player` WHERE `uuid` = ? LIMIT 1;");
            statement.setString(1, uuid);
            resultSet = statement.executeQuery();
            return resultSet.next() && resultSet.getInt(1) > 0;
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (statement != null) {
                statement.close();
            }
        }
    }

    private static boolean migrateToYaml(String uuid, String name, MigratedStats stats, boolean overwrite) throws Exception {
        File playerDataDirectory = new File(SkyWarsReloaded.get().getDataFolder(), "player_data");
        if (!playerDataDirectory.exists() && !playerDataDirectory.mkdirs()) {
            throw new IllegalStateException("Unable to create player_data directory.");
        }

        File playerFile = new File(playerDataDirectory, uuid + ".yml");
        if (playerFile.exists() && !overwrite) {
            return false;
        }
        if (!playerFile.exists() && !playerFile.createNewFile()) {
            throw new IllegalStateException("Unable to create player file for " + uuid);
        }

        FileConfiguration fc = YamlConfiguration.loadConfiguration(playerFile);
        String existingName = asString(fc.getString("player_name"));
        name = choosePreferredName(name, existingName, uuid);
        fc.set("uuid", uuid);
        fc.set("player_name", name);
        fc.set("wins", stats.wins);
        fc.set("losses", stats.losses);
        fc.set("kills", stats.kills);
        fc.set("deaths", stats.deaths);
        fc.set("xp", stats.xp);
        fc.set("pareffect", stats.particleEffect);
        fc.set("proeffect", stats.projectileEffect);
        fc.set("glasscolor", stats.glassColor);
        fc.set("killsound", stats.killSound);
        fc.set("winsound", stats.winSound);
        fc.set("taunt", stats.taunt);
        fc.set("prestige_icon", stats.prestigeIcon != null ? stats.prestigeIcon : "icon1");
        fc.set("souls", stats.souls);
        fc.set("soulwell_usages", stats.soulWellUsages);
        fc.set("soulwell_legendaries", stats.soulWellLegendaries);
        fc.set("soulwell_rares", stats.soulWellRares);
        fc.set("soulwell_souls_gathered", stats.soulWellSoulsGathered);
        fc.set("soulwell_souls_purchased", stats.soulWellSoulsPurchased);
        fc.save(playerFile);
        return true;
    }

    private static MigratedStats readStats(DBObject playerDoc, String skywarsJson) {
        MigratedStats stats = new MigratedStats();
        stats.wins = Math.max(0, asInt(playerDoc.get("wins")));
        stats.losses = Math.max(0, asInt(playerDoc.get("losses")));
        stats.kills = Math.max(0, asInt(playerDoc.get("kills")));
        stats.deaths = Math.max(0, asInt(playerDoc.get("deaths")));
        stats.souls = Math.max(0, asInt(playerDoc.get("souls")));
        stats.soulWellUsages = Math.max(0, asInt(playerDoc.get("soulwell_usages")));
        stats.soulWellLegendaries = Math.max(0, asInt(playerDoc.get("soulwell_legendaries")));
        stats.soulWellRares = Math.max(0, asInt(playerDoc.get("soulwell_rares")));
        stats.soulWellSoulsGathered = Math.max(0, asInt(playerDoc.get("soulwell_souls_gathered")));
        stats.soulWellSoulsPurchased = Math.max(0, asInt(playerDoc.get("soulwell_souls_purchased")));
        stats.particleEffect = asString(playerDoc.get("pareffect"));
        stats.projectileEffect = asString(playerDoc.get("proeffect"));
        stats.glassColor = asString(playerDoc.get("glasscolor"));
        stats.killSound = asString(playerDoc.get("killsound"));
        stats.winSound = asString(playerDoc.get("winsound"));
        stats.taunt = asString(playerDoc.get("taunt"));
        stats.prestigeIcon = firstNonEmpty(asString(playerDoc.get("prestige_icon")), asString(playerDoc.get("prestigeIcon")));
        stats.coins = Math.max(0, asInt(playerDoc.get("coins")));
        stats.elo = Math.max(0, asInt(playerDoc.get("elo")));
        stats.level = Math.max(1, asInt(playerDoc.get("level")));

        if (skywarsJson != null) {
            stats.wins = Math.max(stats.wins, extractJsonInt(skywarsJson, "wins", stats.wins));
            stats.losses = Math.max(stats.losses, extractJsonInt(skywarsJson, "losses", stats.losses));
            stats.kills = Math.max(stats.kills, extractJsonInt(skywarsJson, "kills", stats.kills));
            stats.deaths = Math.max(stats.deaths, extractJsonInt(skywarsJson, "deaths", stats.deaths));
            stats.xp = Math.max(0, extractJsonInt(skywarsJson, "xp", 0));
            stats.souls = Math.max(stats.souls, extractJsonInt(skywarsJson, "souls", 0));
            stats.soulWellUsages = Math.max(stats.soulWellUsages, extractJsonInt(skywarsJson, "soulwell_usages", stats.soulWellUsages));
            stats.soulWellLegendaries = Math.max(stats.soulWellLegendaries, extractJsonInt(skywarsJson, "soulwell_legendaries", stats.soulWellLegendaries));
            stats.soulWellRares = Math.max(stats.soulWellRares, extractJsonInt(skywarsJson, "soulwell_rares", stats.soulWellRares));
            stats.soulWellSoulsGathered = Math.max(stats.soulWellSoulsGathered, extractJsonInt(skywarsJson, "soulwell_souls_gathered", stats.soulWellSoulsGathered));
            stats.soulWellSoulsPurchased = Math.max(stats.soulWellSoulsPurchased, extractJsonInt(skywarsJson, "soulwell_souls_purchased", stats.soulWellSoulsPurchased));
            // USW-style keys
            stats.soulWellLegendaries = Math.max(stats.soulWellLegendaries, extractJsonInt(skywarsJson, "soulWellHead", stats.soulWellLegendaries));
            stats.soulWellRares = Math.max(stats.soulWellRares, extractJsonInt(skywarsJson, "soulWellExtra", stats.soulWellRares));
            stats.soulWellUsages = Math.max(stats.soulWellUsages, extractJsonInt(skywarsJson, "soulWellMax", stats.soulWellUsages));
            stats.coins = Math.max(stats.coins, extractJsonInt(skywarsJson, "coins", stats.coins));
            stats.elo = Math.max(stats.elo, extractJsonInt(skywarsJson, "elo", stats.elo));
            stats.level = Math.max(stats.level, extractJsonInt(skywarsJson, "level", stats.level));
            stats.taunt = coalesceOptionString(stats.taunt, extractJsonInt(skywarsJson, "taunt", 0));
            stats.killSound = coalesceOptionString(stats.killSound, extractJsonInt(skywarsJson, "killSound", 0));
            stats.winSound = coalesceOptionString(stats.winSound, extractJsonInt(skywarsJson, "winEffect", 0));
            stats.glassColor = coalesceOptionString(stats.glassColor, extractJsonInt(skywarsJson, "glass", 0));
            String jsonPrestige = firstNonEmpty(
                    extractJsonString(skywarsJson, "prestigeIcon", null),
                    extractJsonString(skywarsJson, "prestige_icon", null));
            stats.prestigeIcon = firstNonEmpty(stats.prestigeIcon, jsonPrestige);
        }

        if (stats.particleEffect == null) stats.particleEffect = "none";
        if (stats.projectileEffect == null) stats.projectileEffect = "none";
        if (stats.glassColor == null) stats.glassColor = "none";
        if (stats.killSound == null) stats.killSound = "none";
        if (stats.winSound == null) stats.winSound = "none";
        if (stats.taunt == null) stats.taunt = "none";
        if (stats.prestigeIcon == null || stats.prestigeIcon.trim().isEmpty()) {
            stats.prestigeIcon = "icon1";
        }

        // SWR e gay si calculeaza xp-ul in functie de level, asa ca mai bine ii dam xp necesar pentru levelul respectiv :))))))))
        if (stats.level > 1) {
            int xpForLevel = LevelManager.get().getXpForLevel(stats.level);
            stats.xp = Math.max(stats.xp, Math.max(0, xpForLevel));
        }

        return stats;
    }

    private static int extractJsonInt(String json, String field, int fallback) {
        Matcher matcher = INT_FIELD_PATTERN.matcher(json);
        while (matcher.find()) {
            if (field.equals(matcher.group(1))) {
                try {
                    return Integer.parseInt(matcher.group(2));
                } catch (NumberFormatException ignored) {
                    return fallback;
                }
            }
        }
        return fallback;
    }

    private static String extractJsonString(String json, String field, String fallback) {
        if (json == null) {
            return fallback;
        }
        Matcher matcher = STRING_FIELD_PATTERN.matcher(json);
        while (matcher.find()) {
            if (field.equals(matcher.group(1))) {
                String value = matcher.group(2);
                if (value == null) {
                    return fallback;
                }
                String trimmed = value.trim();
                return trimmed.isEmpty() ? fallback : trimmed;
            }
        }
        return fallback;
    }

    private static String asString(Object val) {
        if (val == null) {
            return null;
        }
        String text = String.valueOf(val).trim();
        return text.isEmpty() ? null : text;
    }

    private static int asInt(Object val) {
        if (val == null) {
            return 0;
        }
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(val));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String choosePreferredName(String primary, String secondary, String fallback) {
        String p = sanitizeName(primary, fallback);
        if (p != null) {
            return p;
        }
        String s = sanitizeName(secondary, fallback);
        if (s != null) {
            return s;
        }
        if (fallback == null || fallback.trim().isEmpty()) {
            return null;
        }
        return fallback;
    }

    private static String sanitizeName(String value, String uuidFallback) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (uuidFallback != null && !uuidFallback.trim().isEmpty() && trimmed.equalsIgnoreCase(uuidFallback.trim())) {
            return null;
        }
        return trimmed;
    }

    private static String getExistingPlayerNameSql(Connection connection, String uuid) throws SQLException {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.prepareStatement("SELECT `player_name` FROM `sw_player` WHERE `uuid` = ? LIMIT 1;");
            statement.setString(1, uuid);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("player_name");
            }
            return null;
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (statement != null) {
                statement.close();
            }
        }
    }

    private static String firstNonEmpty(String a, String b) {
        if (a != null && !a.trim().isEmpty()) {
            return a.trim();
        }
        if (b != null && !b.trim().isEmpty()) {
            return b.trim();
        }
        return null;
    }

    private static String coalesceOptionString(String currentValue, int optionId) {
        String current = asString(currentValue);
        if (current != null && !"none".equalsIgnoreCase(current)) {
            return current;
        }
        // USW stores locked/unselected as 0 or 999999.
        if (optionId <= 0 || optionId == 999999) {
            return "none";
        }
        return String.valueOf(optionId);
    }

    private static final class MigratedStats {
        private int wins;
        private int losses;
        private int kills;
        private int deaths;
        private int xp;
        private int souls;
        private int soulWellUsages;
        private int soulWellLegendaries;
        private int soulWellRares;
        private int soulWellSoulsGathered;
        private int soulWellSoulsPurchased;
        private String particleEffect;
        private String projectileEffect;
        private String glassColor;
        private String killSound;
        private String winSound;
        private String taunt;
        private String prestigeIcon;
        private int coins;
        private int elo;
        private int level;
    }
}
