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
        String name = asString(playerDoc.get("name"));
        if (name == null || name.trim().isEmpty()) {
            name = uuidText;
        }

        MigratedStats stats = readStats(playerDoc);

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
            PreparedStatement update = null;
            try {
                update = connection.prepareStatement(
                        "UPDATE `sw_player` SET `player_name` = ?, `wins` = ?, `kills` = ?, `deaths` = ?, `xp` = ?, `souls` = ? WHERE `uuid` = ?;"
                );
                update.setString(1, name);
                update.setInt(2, stats.wins);
                update.setInt(3, stats.kills);
                update.setInt(4, stats.deaths);
                update.setInt(5, stats.xp);
                update.setInt(6, stats.souls);
                update.setString(7, uuid);
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
            insert = connection.prepareStatement(
                    "INSERT INTO `sw_player` (`player_id`, `uuid`, `player_name`, `wins`, `losses`, `kills`, `deaths`, `xp`, `pareffect`, `proeffect`, `glasscolor`, `killsound`, `winsound`, `taunt`, `souls`, `soulwell_usages`, `soulwell_legendaries`, `soulwell_rares`, `soulwell_souls_gathered`, `soulwell_souls_purchased`) " +
                            "VALUES (NULL, ?, ?, ?, 0, ?, ?, ?, 'none', 'none', 'none', 'none', 'none', 'none', ?, 0, 0, 0, 0, 0);"
            );
            insert.setString(1, uuid);
            insert.setString(2, name);
            insert.setInt(3, stats.wins);
            insert.setInt(4, stats.kills);
            insert.setInt(5, stats.deaths);
            insert.setInt(6, stats.xp);
            insert.setInt(7, stats.souls);
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
        fc.set("uuid", uuid);
        fc.set("player_name", name);
        fc.set("wins", stats.wins);
        fc.set("kills", stats.kills);
        fc.set("deaths", stats.deaths);
        if (!fc.contains("losses")) {
            fc.set("losses", 0);
        }
        fc.set("xp", stats.xp);
        if (!fc.contains("pareffect")) {
            fc.set("pareffect", "none");
        }
        if (!fc.contains("proeffect")) {
            fc.set("proeffect", "none");
        }
        if (!fc.contains("glasscolor")) {
            fc.set("glasscolor", "none");
        }
        if (!fc.contains("killsound")) {
            fc.set("killsound", "none");
        }
        if (!fc.contains("winsound")) {
            fc.set("winsound", "none");
        }
        if (!fc.contains("taunt")) {
            fc.set("taunt", "none");
        }
        fc.set("souls", stats.souls);
        if (!fc.contains("soulwell_usages")) {
            fc.set("soulwell_usages", 0);
        }
        if (!fc.contains("soulwell_legendaries")) {
            fc.set("soulwell_legendaries", 0);
        }
        if (!fc.contains("soulwell_rares")) {
            fc.set("soulwell_rares", 0);
        }
        if (!fc.contains("soulwell_souls_gathered")) {
            fc.set("soulwell_souls_gathered", 0);
        }
        if (!fc.contains("soulwell_souls_purchased")) {
            fc.set("soulwell_souls_purchased", 0);
        }
        fc.save(playerFile);
        return true;
    }

    private static MigratedStats readStats(DBObject playerDoc) {
        MigratedStats stats = new MigratedStats();
        stats.wins = Math.max(0, asInt(playerDoc.get("wins")));
        stats.kills = Math.max(0, asInt(playerDoc.get("kills")));
        stats.deaths = Math.max(0, asInt(playerDoc.get("deaths")));
        stats.coins = Math.max(0, asInt(playerDoc.get("coins")));
        stats.elo = Math.max(0, asInt(playerDoc.get("elo")));
        stats.level = Math.max(1, asInt(playerDoc.get("level")));

        String skywarsJson = asString(playerDoc.get("skywars"));
        if (skywarsJson != null) {
            stats.wins = Math.max(stats.wins, extractJsonInt(skywarsJson, "wins", stats.wins));
            stats.kills = Math.max(stats.kills, extractJsonInt(skywarsJson, "kills", stats.kills));
            stats.deaths = Math.max(stats.deaths, extractJsonInt(skywarsJson, "deaths", stats.deaths));
            stats.xp = Math.max(0, extractJsonInt(skywarsJson, "xp", 0));
            stats.souls = Math.max(0, extractJsonInt(skywarsJson, "souls", 0));
            stats.coins = Math.max(stats.coins, extractJsonInt(skywarsJson, "coins", stats.coins));
            stats.elo = Math.max(stats.elo, extractJsonInt(skywarsJson, "elo", stats.elo));
            stats.level = Math.max(stats.level, extractJsonInt(skywarsJson, "level", stats.level));
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

    private static final class MigratedStats {
        private int wins;
        private int kills;
        private int deaths;
        private int xp;
        private int souls;
        private int coins;
        private int elo;
        private int level;
    }
}
