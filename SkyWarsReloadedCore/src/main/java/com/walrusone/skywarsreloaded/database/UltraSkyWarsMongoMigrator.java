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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.UUID;

public final class UltraSkyWarsMongoMigrator {

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

        int wins = asInt(playerDoc.get("wins"));
        int kills = asInt(playerDoc.get("kills"));
        int deaths = asInt(playerDoc.get("deaths"));

        boolean sqlEnabled = SkyWarsReloaded.get().getConfig().getBoolean("sqldatabase.enabled");
        if (sqlEnabled) {
            return migrateToSql(uuidText, name, wins, kills, deaths, overwrite);
        }
        return migrateToYaml(uuidText, name, wins, kills, deaths, overwrite);
    }

    private static boolean migrateToSql(String uuid, String name, int wins, int kills, int deaths, boolean overwrite) throws SQLException {
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
                        "UPDATE `sw_player` SET `player_name` = ?, `wins` = ?, `kills` = ?, `deaths` = ? WHERE `uuid` = ?;"
                );
                update.setString(1, name);
                update.setInt(2, Math.max(0, wins));
                update.setInt(3, Math.max(0, kills));
                update.setInt(4, Math.max(0, deaths));
                update.setString(5, uuid);
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
                            "VALUES (NULL, ?, ?, ?, 0, ?, ?, 0, 'none', 'none', 'none', 'none', 'none', 'none', 0, 0, 0, 0, 0, 0);"
            );
            insert.setString(1, uuid);
            insert.setString(2, name);
            insert.setInt(3, Math.max(0, wins));
            insert.setInt(4, Math.max(0, kills));
            insert.setInt(5, Math.max(0, deaths));
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

    private static boolean migrateToYaml(String uuid, String name, int wins, int kills, int deaths, boolean overwrite) throws Exception {
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
        fc.set("wins", Math.max(0, wins));
        fc.set("kills", Math.max(0, kills));
        fc.set("deaths", Math.max(0, deaths));
        if (!fc.contains("losses")) {
            fc.set("losses", 0);
        }
        if (!fc.contains("xp")) {
            fc.set("xp", 0);
        }
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
        if (!fc.contains("souls")) {
            fc.set("souls", 0);
        }
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
}
