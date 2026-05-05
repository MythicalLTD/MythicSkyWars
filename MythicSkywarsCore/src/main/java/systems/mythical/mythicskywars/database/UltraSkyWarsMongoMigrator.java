package systems.mythical.mythicskywars.database;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.utilities.LevelManager;
import systems.mythical.mythicskywars.utilities.VaultUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UltraSkyWarsMongoMigrator {
    private static final Pattern INT_FIELD_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(-?\\d+)");
    private static final Pattern STRING_FIELD_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern OBJECT_FIELD_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\\{([^}]*)\\}");
    private static final Pattern KITS_OBJECT_PATTERN = Pattern.compile("\"kits\"\\s*:\\s*\\{(.*?)\\}", Pattern.DOTALL);
    private static final Pattern NUMERIC_ARRAY_ENTRY_PATTERN = Pattern.compile("\"?(\\d+)\"?\\s*:\\s*\\[([^\\]]*)\\]");
    private static final Pattern INT_LIST_PATTERN = Pattern.compile("-?\\d+");
    private static final Pattern PERK_ENTRY_PATTERN = Pattern.compile("\"?(\\d+)\"?\\s*:\\s*(-?\\d+)");
    private static final int MONGO_BATCH_SIZE = 1250;
    private static final int PROGRESS_LOG_EVERY = 1000;
    private static final int SQL_COMMIT_EVERY = 2000;
    private static volatile boolean migrationRunning = false;
    private static volatile long migrationTotal = 0L;
    private static volatile long migrationScanned = 0L;

    private UltraSkyWarsMongoMigrator() {
    }

    public static final class MigrationResult {
        private int scanned;
        private int imported;
        private int skipped;
        private int failed;
        private long total;
        private long elapsedMillis;

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

        public long getTotal() {
            return total;
        }

        public long getElapsedMillis() {
            return elapsedMillis;
        }
    }

    public interface ProgressListener {
        void onProgress(ProgressSnapshot snapshot);
    }

    public static final class ProgressSnapshot {
        private final long total;
        private final int scanned;
        private final int imported;
        private final int skipped;
        private final int failed;
        private final long elapsedMillis;

        private ProgressSnapshot(long total, int scanned, int imported, int skipped, int failed, long elapsedMillis) {
            this.total = total;
            this.scanned = scanned;
            this.imported = imported;
            this.skipped = skipped;
            this.failed = failed;
            this.elapsedMillis = elapsedMillis;
        }

        public long getTotal() {
            return total;
        }

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

        public long getElapsedMillis() {
            return elapsedMillis;
        }

        public double getDocsPerSecond() {
            if (elapsedMillis <= 0L) {
                return 0D;
            }
            return scanned / Math.max(0.001D, elapsedMillis / 1000D);
        }

        public long getRemainingDocs() {
            if (total <= 0L) {
                return -1L;
            }
            return Math.max(0L, total - scanned);
        }

        public long getEtaMillis() {
            double rate = getDocsPerSecond();
            if (rate <= 0D || total <= 0L) {
                return -1L;
            }
            return (long) ((getRemainingDocs() / rate) * 1000D);
        }
    }

    public static MigrationResult migrateFromConfig(boolean overwrite) throws Exception {
        return migrateFromConfig(overwrite, null);
    }

    public static MigrationResult migrateFromConfig(boolean overwrite, ProgressListener progressListener) throws Exception {
        FileConfiguration cfg = MythicSkywars.get().getConfig();
        String root = "migration.ultimateskywars.mongodb.";
        migrationRunning = true;
        migrationTotal = 0L;
        migrationScanned = 0L;
        prepareUswDirectoryAndSyncConfigs();
        String host = cfg.getString(root + "host", "").trim();
        int port = cfg.getInt(root + "port", 27017);
        String databaseName = cfg.getString(root + "database", "").trim();
        String authDatabaseName = cfg.getString(root + "authDatabase", databaseName).trim();
        String username = cfg.getString(root + "username", "").trim();
        String password = cfg.getString(root + "password", "");
        String collectionName = cfg.getString(root + "collection", "players").trim();
        String authMechanism = cfg.getString(root + "authMechanism", "auto").trim();
        String economyImportMode = cfg.getString(root + "economyImportMode", "ESSENTIALSX").trim();
        boolean migrationDebug = cfg.getBoolean(root + "debug", false);
        Map<Integer, String> kitIdMappings = readNumericKeyMappings(cfg, root + "mappings.kits");
        Map<Integer, String> perkIdMappings = readNumericKeyMappings(cfg, root + "mappings.perks");
        mergeIfMissing(kitIdMappings, autoDetectKitMappings(MythicSkywars.get().getDataFolder()));
        mergeIfMissing(perkIdMappings, autoDetectPerkMappings(MythicSkywars.get().getDataFolder()));

        if (host.isEmpty() || databaseName.isEmpty()) {
            throw new IllegalArgumentException("Missing migration MongoDB config. Set migration.ultimateskywars.mongodb.host and database.");
        }

        if (migrationDebug) {
            MythicSkywars.get().getLogger().info("[MigrationDebug] Mongo config resolved -> host=" + host
                    + ", port=" + port
                    + ", database=" + databaseName
                    + ", authDatabase=" + (authDatabaseName.isEmpty() ? "<empty>" : authDatabaseName)
                    + ", username=" + (username.isEmpty() ? "<empty>" : username)
                    + ", collection=" + collectionName
                    + ", authMechanism=" + (authMechanism.isEmpty() ? "<empty>" : authMechanism)
                    + ", economyImportMode=" + (economyImportMode.isEmpty() ? "<empty>" : economyImportMode)
                    + ", kitMappings=" + kitIdMappings.size()
                    + ", perkMappings=" + perkIdMappings.size());
        }

        MongoClient mongoClient;
        if (!username.isEmpty()) {
            if (authDatabaseName.isEmpty()) {
                authDatabaseName = databaseName;
            }
            MongoCredential credential;
            String mechanism = authMechanism == null ? "" : authMechanism.trim().toLowerCase();
            if (mechanism.equals("scram-sha-256") || mechanism.equals("sha256")) {
                credential = MongoCredential.createScramSha256Credential(
                        username,
                        authDatabaseName,
                        password.toCharArray()
                );
            } else if (mechanism.equals("scram-sha-1") || mechanism.equals("sha1")) {
                credential = MongoCredential.createScramSha1Credential(
                        username,
                        authDatabaseName,
                        password.toCharArray()
                );
            } else {
                credential = MongoCredential.createCredential(
                        username,
                        authDatabaseName,
                        password.toCharArray()
                );
            }
            if (migrationDebug) {
                MythicSkywars.get().getLogger().info("[MigrationDebug] Using Mongo credential mechanism="
                        + credential.getMechanism()
                        + ", source=" + credential.getSource());
            }
            mongoClient = new MongoClient(new ServerAddress(host, port), Collections.singletonList(credential));
        } else {
            mongoClient = new MongoClient(new ServerAddress(host, port));
            if (migrationDebug) {
                MythicSkywars.get().getLogger().info("[MigrationDebug] Using Mongo connection without credentials.");
            }
        }

        MigrationResult result = new MigrationResult();
        long startMillis = System.currentTimeMillis();
        boolean sqlEnabled = MythicSkywars.get().getConfig().getBoolean("sqldatabase.enabled");
        Connection sqlConnection = null;
        try {
            if (sqlEnabled) {
                Database sqlDatabase = MythicSkywars.getDb();
                if (sqlDatabase == null || sqlDatabase.checkConnection()) {
                    throw new SQLException("MythicSkywars SQL database is not available.");
                }
                sqlConnection = sqlDatabase.getConnection();
                if (sqlConnection == null) {
                    throw new SQLException("MythicSkywars SQL connection is null.");
                }
                sqlConnection.setAutoCommit(false);
            }
            clearExistingMythicData(sqlEnabled, sqlConnection);
            DB database = mongoClient.getDB(databaseName);
            DBCollection players = database.getCollection(collectionName);
            result.total = players.count();
            migrationTotal = result.total;
            BasicDBObject projection = new BasicDBObject("_id", 0)
                    .append("uuid", 1)
                    .append("name", 1)
                    .append("skywars", 1)
                    .append("wins", 1)
                    .append("losses", 1)
                    .append("kills", 1)
                    .append("deaths", 1)
                    .append("coins", 1)
                    .append("elo", 1)
                    .append("level", 1)
                    .append("xp", 1)
                    .append("souls", 1)
                    .append("soulwell_usages", 1)
                    .append("soulwell_legendaries", 1)
                    .append("soulwell_rares", 1)
                    .append("soulwell_souls_gathered", 1)
                    .append("soulwell_souls_purchased", 1)
                    .append("pareffect", 1)
                    .append("proeffect", 1)
                    .append("glasscolor", 1)
                    .append("killsound", 1)
                    .append("winsound", 1)
                    .append("taunt", 1)
                    .append("prestige_icon", 1)
                    .append("prestigeIcon", 1);
            DBCursor cursor = players.find(new BasicDBObject(), projection).batchSize(MONGO_BATCH_SIZE);
            try {
                while (cursor.hasNext()) {
                    DBObject playerDoc = cursor.next();
                    result.scanned++;
                    migrationScanned = result.scanned;
                    try {
                        if (migrateOne(playerDoc, overwrite, sqlEnabled, sqlConnection, economyImportMode, kitIdMappings, perkIdMappings)) {
                            result.imported++;
                        } else {
                            result.skipped++;
                        }
                    } catch (Exception ex) {
                        result.failed++;
                        MythicSkywars.get().getLogger().warning("Failed to migrate one USW record: " + ex.getMessage());
                    }
                    if (migrationDebug && result.scanned % PROGRESS_LOG_EVERY == 0) {
                        ProgressSnapshot snapshot = buildSnapshot(result, startMillis);
                        MythicSkywars.get().getLogger().info("[MigrationDebug] Progress scanned=" + snapshot.getScanned()
                                + "/" + snapshot.getTotal()
                                + ", imported=" + snapshot.getImported()
                                + ", skipped=" + snapshot.getSkipped()
                                + ", failed=" + snapshot.getFailed()
                                + ", elapsed=" + formatDuration(snapshot.getElapsedMillis())
                                + ", rate=" + String.format("%.2f", snapshot.getDocsPerSecond()) + "/s"
                                + ", eta=" + formatDuration(snapshot.getEtaMillis()));
                    }
                    if (progressListener != null && result.scanned % PROGRESS_LOG_EVERY == 0) {
                        progressListener.onProgress(buildSnapshot(result, startMillis));
                    }
                    if (sqlEnabled && sqlConnection != null && result.scanned % SQL_COMMIT_EVERY == 0) {
                        sqlConnection.commit();
                    }
                }
            } finally {
                cursor.close();
            }
            if (sqlEnabled && sqlConnection != null) {
                sqlConnection.commit();
            }
        } catch (Exception ex) {
            if (sqlEnabled && sqlConnection != null) {
                try {
                    sqlConnection.rollback();
                } catch (SQLException ignored) {
                }
            }
            if (migrationDebug) {
                logExceptionChain(ex);
            }
            throw ex;
        } finally {
            if (sqlEnabled && sqlConnection != null) {
                try {
                    sqlConnection.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
            result.elapsedMillis = Math.max(0L, System.currentTimeMillis() - startMillis);
            mongoClient.close();
            migrationRunning = false;
            migrationTotal = 0L;
            migrationScanned = 0L;
        }
        return result;
    }

    public static boolean isMigrationRunning() {
        return migrationRunning;
    }

    public static int getMigrationProgressPercent() {
        long total = migrationTotal;
        if (total <= 0L) {
            return 0;
        }
        long scanned = Math.max(0L, Math.min(migrationScanned, total));
        return (int) Math.max(0, Math.min(100, (scanned * 100L) / total));
    }

    private static ProgressSnapshot buildSnapshot(MigrationResult result, long startMillis) {
        long elapsed = Math.max(0L, System.currentTimeMillis() - startMillis);
        return new ProgressSnapshot(result.total, result.scanned, result.imported, result.skipped, result.failed, elapsed);
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

    private static void logExceptionChain(Throwable throwable) {
        int depth = 0;
        Throwable current = throwable;
        while (current != null && depth < 8) {
            MythicSkywars.get().getLogger().warning("[MigrationDebug] Mongo exception[" + depth + "]: "
                    + current.getClass().getName() + " -> " + String.valueOf(current.getMessage()));
            current = current.getCause();
            depth++;
        }
    }

    private static void clearExistingMythicData(boolean sqlEnabled, Connection sqlConnection) throws SQLException {
        if (sqlEnabled) {
            if (sqlConnection == null) {
                throw new SQLException("SQL connection unavailable for truncate.");
            }
            PreparedStatement truncatePermissions = null;
            PreparedStatement truncatePlayers = null;
            try {
                truncatePermissions = sqlConnection.prepareStatement("TRUNCATE TABLE `sw_permissions`;");
                truncatePermissions.executeUpdate();
                truncatePlayers = sqlConnection.prepareStatement("TRUNCATE TABLE `sw_player`;");
                truncatePlayers.executeUpdate();
            } finally {
                if (truncatePermissions != null) {
                    truncatePermissions.close();
                }
                if (truncatePlayers != null) {
                    truncatePlayers.close();
                }
            }
            return;
        }

        File playerDataDir = new File(MythicSkywars.get().getDataFolder(), "player_data");
        deleteDirectoryContents(playerDataDir);
    }

    private static boolean migrateOne(DBObject playerDoc, boolean overwrite, boolean sqlEnabled, Connection sqlConnection, String economyImportMode,
                                      Map<Integer, String> kitIdMappings, Map<Integer, String> perkIdMappings) throws Exception {
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
        String fullDocumentJson = playerDoc.toString();
        String name = choosePreferredName(
                asString(playerDoc.get("name")),
                extractJsonString(skywarsJson, "name", null),
                null
        );

        MigratedStats stats = readStats(playerDoc, skywarsJson);
        Set<String> unlockedPermissions = extractOwnedUnlockPermissions(stats, skywarsJson, kitIdMappings, perkIdMappings);

        boolean changed;
        if (sqlEnabled) {
            changed = migrateToSql(sqlConnection, uuidText, name, stats, overwrite);
            // Preserve full legacy payload on sw_player.usw_data for future migrations/features.
            storeRawUswSnapshotSql(sqlConnection, uuidText, skywarsJson, fullDocumentJson);
            if (changed && !unlockedPermissions.isEmpty()) {
                storePermissionsSql(sqlConnection, uuidText, name, unlockedPermissions);
            }
        } else {
            changed = migrateToYaml(uuidText, name, stats, overwrite);
            if (changed && !unlockedPermissions.isEmpty()) {
                storePermissionsYaml(uuidText, unlockedPermissions);
            }
        }
        if (changed) {
            applyEconomyImport(uuidText, name, stats.coins, sqlConnection, economyImportMode);
        }
        return changed;
    }

    private static void applyEconomyImport(String uuid, String name, int coins, Connection sqlConnection, String economyImportMode) throws SQLException {
        String mode = economyImportMode == null ? "ESSENTIALSX" : economyImportMode.trim().toUpperCase();
        double amount = Math.max(0D, coins);
        if ("SKIP".equals(mode) || "NONE".equals(mode)) {
            return;
        }
        if ("BUILTIN".equals(mode)) {
            storeEconomyBuiltinSql(sqlConnection, uuid, amount);
            return;
        }
        // Default: import via active Vault provider (typically EssentialsX).
        if (MythicSkywars.getCfg().economyEnabled() && VaultUtils.get().isEconomyAvailable()) {
            VaultUtils.get().setBalance(UUID.fromString(uuid), name, amount);
        }
    }

    private static void prepareUswDirectoryAndSyncConfigs() throws IOException {
        File pluginDataDir = MythicSkywars.get().getDataFolder();
        File uswDir = new File(pluginDataDir, "UltraSkyWars");
        File uswCosmeticsDir = new File(uswDir, "cosmetics");
        if (!uswDir.exists() && !uswDir.mkdirs()) {
            throw new IOException("Could not create directory: " + uswDir.getAbsolutePath());
        }
        if (!uswCosmeticsDir.exists() && !uswCosmeticsDir.mkdirs()) {
            throw new IOException("Could not create directory: " + uswCosmeticsDir.getAbsolutePath());
        }

        File[] rootYmlFiles = uswDir.listFiles((dir, name) -> name != null && name.toLowerCase(Locale.ENGLISH).endsWith(".yml"));
        File[] cosmeticYmlFiles = uswCosmeticsDir.listFiles((dir, name) -> name != null && name.toLowerCase(Locale.ENGLISH).endsWith(".yml"));

        if ((rootYmlFiles == null || rootYmlFiles.length == 0) && (cosmeticYmlFiles == null || cosmeticYmlFiles.length == 0)) {
            throw new IllegalStateException("Place your UltraSkyWars .yml files in " + uswDir.getAbsolutePath() +
                    " and cosmetics .yml files in " + uswCosmeticsDir.getAbsolutePath() + ", then run /sw migrateusw again.");
        }

        clearTranslatedContent(pluginDataDir);

        translateUswKits(uswDir, pluginDataDir);
        translateUswPerks(uswDir, pluginDataDir);
        translateUswLevels(uswDir, pluginDataDir);
        translateUswCosmetics(uswCosmeticsDir, pluginDataDir);
        translateUswChests(uswDir, pluginDataDir);
        MythicSkywars.get().getLogger().info("[MigrationDebug] USW translation complete: primary kits/perks/levels/cosmetics/chests files refreshed.");
    }

    private static void clearTranslatedContent(File pluginDataDir) {
        deleteFileIfExists(new File(pluginDataDir, "UltraSkyWars-perks-reference.yml"));
        deleteFileIfExists(new File(pluginDataDir, "levels.yml"));
        deleteFileIfExists(new File(pluginDataDir, "perks.yml"));
        deleteDirectoryContents(new File(pluginDataDir, "kits"));
        File cosmetics = new File(pluginDataDir, "cosmetics");
        deleteFileIfExists(new File(cosmetics, "glasscolors.yml"));
        deleteFileIfExists(new File(cosmetics, "killsounds.yml"));
        deleteFileIfExists(new File(cosmetics, "taunts.yml"));
        deleteFileIfExists(new File(cosmetics, "winsounds.yml"));
        deleteFileIfExists(new File(cosmetics, "projectileeffects.yml"));
        deleteFileIfExists(new File(cosmetics, "particleeffects.yml"));
        deleteFileIfExists(new File(pluginDataDir, "chests/basic/basicchest.yml"));
        deleteFileIfExists(new File(pluginDataDir, "chests/basic/basiccenterchest.yml"));
        deleteFileIfExists(new File(pluginDataDir, "chests/normal/chest.yml"));
        deleteFileIfExists(new File(pluginDataDir, "chests/normal/centerchest.yml"));
        deleteFileIfExists(new File(pluginDataDir, "chests/op/opchest.yml"));
        deleteFileIfExists(new File(pluginDataDir, "chests/op/opcenterchest.yml"));
    }

    private static void deleteDirectoryContents(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                deleteDirectoryContents(file);
                if (!file.delete()) {
                    MythicSkywars.get().getLogger().warning("Failed to delete directory during USW cleanup: " + file.getAbsolutePath());
                }
                continue;
            }
            if (!file.delete()) {
                MythicSkywars.get().getLogger().warning("Failed to delete file during USW cleanup: " + file.getAbsolutePath());
            }
        }
    }

    private static void deleteFileIfExists(File file) {
        if (file != null && file.exists() && !file.delete()) {
            MythicSkywars.get().getLogger().warning("Failed to delete file during USW cleanup: " + file.getAbsolutePath());
        }
    }

    private static void translateUswKits(File uswDir, File pluginDataDir) {
        File source = new File(uswDir, "kits.yml");
        if (!source.exists()) {
            return;
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(source);
        if (cfg.getConfigurationSection("kits") == null) {
            return;
        }
        File kitsDir = new File(pluginDataDir, "kits");
        if (!kitsDir.exists()) kitsDir.mkdirs();
        for (String key : cfg.getConfigurationSection("kits").getKeys(false)) {
            String base = "kits." + key;
            String name = cfg.getString(base + ".name", key);
            String filename = safeFileName(name);
            String levelBase = base + ".levels.1";
            Object icon = cfg.get(levelBase + ".icon");
            Object inventory = cfg.get(levelBase + ".inv");
            Object armor = cfg.get(levelBase + ".armor");
            boolean requirePermission = cfg.getBoolean(levelBase + ".needPermToBuy", false);
            int position = cfg.getInt(base + ".slot", 0);
            int page = cfg.getInt(base + ".page", 1);

            YamlConfiguration out = new YamlConfiguration();
            out.set("inventory", inventory);
            out.set("armor", armor);
            out.set("requirePermission", requirePermission);
            out.set("icon", icon);
            out.set("lockedIcon.type", "BARRIER");
            out.set("position", position);
            out.set("page", page);
            out.set("name", name);
            out.set("enabled", true);
            out.set("lores.unlocked", cfg.getStringList(levelBase + ".icon.meta.lore"));
            out.set("lores.locked", "&cA permission is required to unlock this kit!");
            out.set("gameSettings.noRegen", false);
            out.set("gameSettings.noPvp", false);
            out.set("gameSettings.soupPvp", false);
            out.set("gameSettings.noFallDamage", false);
            out.set("filename", filename);

            try {
                out.save(new File(kitsDir, filename + ".yml"));
            } catch (IOException ignored) {
            }
        }
        MythicSkywars.get().getLogger().info("[MigrationDebug] Translated kits.yml -> kits/ (" + cfg.getConfigurationSection("kits").getKeys(false).size() + " kits)");
    }

    private static void translateUswPerks(File uswDir, File pluginDataDir) {
        File source = new File(uswDir, "perks.yml");
        if (!source.exists()) {
            return;
        }
        FileConfiguration in = YamlConfiguration.loadConfiguration(source);
        if (in.getConfigurationSection("perks") == null) {
            return;
        }
        YamlConfiguration out = new YamlConfiguration();
        out.set("enabled", true);
        out.set("menuSize", 45);
        out.set("menuTitle", "&5&lSkyWars Perks");
        out.set("options-menu-slot", 20);

        for (String key : in.getConfigurationSection("perks").getKeys(false)) {
            String base = "perks." + key;
            String outBase = "perks." + key;
            String type = mapUswPerkType(key);
            String effect = mapUswPerkEffect(key);
            Integer amplifier = mapUswPerkAmplifier(key);
            Integer duration = mapUswPerkDuration(in, base, key);

            out.set(outBase + ".name", in.getString(base + ".name", "&a" + titleCaseKey(key)));
            out.set(outBase + ".lore", splitLore(in.getString(base + ".lore", "&8SkyWars Perk\n<state>")));
            out.set(outBase + ".icon", in.getString(base + ".icon.material", "NETHER_STAR"));
            out.set(outBase + ".slot", in.getInt(base + ".slot", 0));
            out.set(outBase + ".type", type);
            out.set(outBase + ".disabled", in.getBoolean(base + ".disabled", false));
            out.set(outBase + ".gameTypes", in.getStringList(base + ".gameTypes"));

            if ("DAMAGE_REDUCTION".equals(type)) {
                out.set(outBase + ".damageType", mapUswDamageType(key));
            }
            if (effect != null) {
                out.set(outBase + ".effect", effect);
            }
            if (amplifier != null) {
                out.set(outBase + ".amplifier", amplifier);
            }
            if (duration != null) {
                out.set(outBase + ".duration", duration);
            }

            String levelsBase = base + ".levels";
            if (in.getConfigurationSection(levelsBase) != null) {
                for (String levelKey : in.getConfigurationSection(levelsBase).getKeys(false)) {
                    String inLevelBase = levelsBase + "." + levelKey;
                    int lvl = in.getInt(inLevelBase + ".level", asInt(levelKey));
                    if (lvl <= 0) {
                        lvl = 1;
                    }
                    String outLevelBase = outBase + ".levels." + lvl;
                    out.set(outLevelBase + ".percent", in.getInt(inLevelBase + ".percent", in.getInt(base + ".percent", 0)));
                    out.set(outLevelBase + ".cost", in.getInt(inLevelBase + ".price", 0));
                    int perLevelDuration = in.getInt(inLevelBase + ".duration", duration == null ? 0 : duration);
                    if (perLevelDuration > 0) {
                        out.set(outLevelBase + ".duration", perLevelDuration);
                    }
                }
            } else {
                out.set(outBase + ".levels.1.percent", in.getInt(base + ".percent", 0));
                out.set(outBase + ".levels.1.cost", 0);
            }
        }
        try {
            out.save(new File(pluginDataDir, "perks.yml"));
        } catch (IOException ignored) {
        }
        MythicSkywars.get().getLogger().info("[MigrationDebug] Translated perks.yml -> perks.yml (" + in.getConfigurationSection("perks").getKeys(false).size() + " perks)");
    }

    private static void translateUswLevels(File uswDir, File pluginDataDir) {
        File source = new File(uswDir, "levels.yml");
        if (!source.exists()) {
            return;
        }
        FileConfiguration in = YamlConfiguration.loadConfiguration(source);
        YamlConfiguration out = new YamlConfiguration();
        out.set("version", in.getInt("version", 1));
        out.set("levels", in.getConfigurationSection("levels"));
        out.set("prestige", in.getConfigurationSection("prestige"));
        try {
            out.save(new File(pluginDataDir, "levels.yml"));
        } catch (IOException ignored) {
        }
        int levelCount = in.getConfigurationSection("levels") == null ? 0 : in.getConfigurationSection("levels").getKeys(false).size();
        int prestigeCount = in.getConfigurationSection("prestige") == null ? 0 : in.getConfigurationSection("prestige").getKeys(false).size();
        MythicSkywars.get().getLogger().info("[MigrationDebug] Translated levels.yml -> levels.yml (levels=" + levelCount + ", prestige=" + prestigeCount + ")");
    }

    private static void translateUswCosmetics(File uswCosmeticsDir, File pluginDataDir) {
        File targetCosmeticsDir = new File(pluginDataDir, "cosmetics");
        if (!targetCosmeticsDir.exists()) targetCosmeticsDir.mkdirs();
        int serverVersion = getServerVersionForMigration();

        translateUswGlass(new File(uswCosmeticsDir, "glass.yml"),
                new File(targetCosmeticsDir, "glasscolors.yml"),
                serverVersion);
        File uswKillSounds = new File(uswCosmeticsDir, "killsound.yml");
        translateUswKillSounds(uswKillSounds, new File(targetCosmeticsDir, "killsounds.yml"), serverVersion);
        translateUswTaunts(new File(uswCosmeticsDir, "taunt.yml"),
                new File(targetCosmeticsDir, "taunts.yml"),
                serverVersion);
        translateUswTrail(new File(uswCosmeticsDir, "trail.yml"), new File(targetCosmeticsDir, "projectileeffects.yml"));
        translateUswKillEffect(new File(uswCosmeticsDir, "killeffect.yml"), new File(targetCosmeticsDir, "particleeffects.yml"));
        File winsoundsSource = new File(uswCosmeticsDir, "winsound.yml");
        if (!winsoundsSource.exists()) {
            winsoundsSource = new File(uswCosmeticsDir, "winsounds.yml");
        }
        translateUswWinSounds(winsoundsSource, new File(targetCosmeticsDir, "winsounds.yml"), serverVersion);
        MythicSkywars.get().getLogger().info("[MigrationDebug] Translated cosmetics -> glasscolors.yml, killsounds.yml, taunts.yml, projectileeffects.yml, particleeffects.yml, winsounds.yml");
    }

    private static void translateUswGlass(File source, File target, int serverVersion) {
        if (!source.exists()) return;
        FileConfiguration in = YamlConfiguration.loadConfiguration(source);
        if (in.getConfigurationSection("glasses") == null) return;
        YamlConfiguration out = new YamlConfiguration();
        out.set("menuSize", 45);
        boolean legacy18 = serverVersion < 9;
        for (String key : in.getConfigurationSection("glasses").getKeys(false)) {
            String base = "glasses." + key;
            String name = in.getString(base + ".name", key);
            String outKey = normalizeYamlKey(name);
            int level = 1 + (in.getInt(base + ".id", 0) * 5);
            int cost = in.getInt(base + ".price", 100);
            int dataValue = in.getInt(base + ".item.damage", -1);
            int position = in.getInt(base + ".slot", 0);
            int page = in.getInt(base + ".page", 1);
            String material = in.getString(base + ".item.type", "WHITE_STAINED_GLASS");
            String outBase = "colors." + outKey;

            out.set(outBase + ".displayname", "&b" + name);
            out.set(outBase + ".level", level);
            out.set(outBase + ".cost", cost);
            out.set(outBase + ".material", legacy18 ? "STAINED_GLASS" : material);
            out.set(outBase + ".datavalue", legacy18 ? (dataValue < 0 ? 0 : dataValue) : dataValue);
            out.set(outBase + ".position", position);
            out.set(outBase + ".page", page);
        }
        try { out.save(target); } catch (IOException ignored) {}
    }

    private static void translateUswKillSounds(File source, File target, int serverVersion) {
        if (!source.exists()) return;
        FileConfiguration in = YamlConfiguration.loadConfiguration(source);
        if (in.getConfigurationSection("killsounds") == null) return;
        YamlConfiguration out = new YamlConfiguration();
        out.set("menuSize", 45);
        boolean legacy18 = serverVersion < 9;
        boolean legacy112 = serverVersion < 13;
        for (String key : in.getConfigurationSection("killsounds").getKeys(false)) {
            String base = "killsounds." + key;
            String name = in.getString(base + ".name", key);
            String outKey = normalizeYamlKey(name);
            String legacySound = in.getString(base + ".sound", "LEVEL_UP");
            double volume = in.getDouble(base + ".vol1", 1.0);
            double pitch = in.getDouble(base + ".vol2", 1.0);
            String legacyIcon = in.getString(base + ".icon.type", "NOTE_BLOCK");
            int level = 1 + (in.getInt(base + ".id", 0) * 5);
            int cost = in.getInt(base + ".price", 100);
            int position = in.getInt(base + ".slot", 0);
            int page = in.getInt(base + ".page", 1);

            String resolvedSound = legacy18 ? legacySound : mapLegacySound(legacySound, legacy112);
            String resolvedIcon = legacy18 ? legacyIcon : "NOTE_BLOCK";
            applyKillSoundEntry(out, outKey, resolvedSound, resolvedIcon, name, volume, pitch, level, cost, position, page);
        }
        try { out.save(target); } catch (IOException ignored) {}
    }

    private static void applyKillSoundEntry(YamlConfiguration cfg, String outKey, String sound, String icon, String name,
                                            double volume, double pitch, int level, int cost, int position, int page) {
        String base = "sounds." + outKey;
        cfg.set(base + ".sound", sound);
        cfg.set(base + ".isCustomSound", false);
        cfg.set(base + ".volume", volume);
        cfg.set(base + ".pitch", pitch);
        cfg.set(base + ".icon", icon);
        cfg.set(base + ".displayName", "&b" + name);
        cfg.set(base + ".level", level);
        cfg.set(base + ".cost", cost);
        cfg.set(base + ".position", position);
        cfg.set(base + ".page", page);
    }

    private static String mapLegacySound(String legacySound, boolean for112) {
        if (legacySound == null || legacySound.trim().isEmpty()) {
            return for112 ? "ENTITY_PLAYER_LEVELUP" : "ENTITY_PLAYER_LEVELUP";
        }
        String s = legacySound.trim().toUpperCase(Locale.ENGLISH);
        if ("LEVEL_UP".equals(s)) return "ENTITY_PLAYER_LEVELUP";
        if ("BAT_DEATH".equals(s)) return "ENTITY_BAT_DEATH";
        if ("AMBIENCE_CAVE".equals(s)) return "AMBIENT_CAVE";
        if ("AMBIENCE_RAIN".equals(s)) return "WEATHER_RAIN";
        if ("AMBIENCE_THUNDER".equals(s)) return for112 ? "ENTITY_LIGHTNING_IMPACT" : "ENTITY_LIGHTNING_BOLT_IMPACT";
        if ("ANVIL_BREAK".equals(s)) return "BLOCK_ANVIL_BREAK";
        if ("ANVIL_LAND".equals(s)) return "BLOCK_ANVIL_LAND";
        if ("ARROW_HIT".equals(s)) return "ENTITY_ARROW_HIT_PLAYER";
        if ("BAT_LOOP".equals(s)) return "ENTITY_BAT_AMBIENT";
        if ("BLAZE_BREATH".equals(s)) return "ENTITY_BLAZE_AMBIENT";
        if ("PIG_DEATH".equals(s)) return "ENTITY_PIG_DEATH";
        if ("HORSE_DEATH".equals(s)) return "ENTITY_HORSE_DEATH";
        if ("GHAST_DEATH".equals(s)) return "ENTITY_GHAST_DEATH";
        if ("FIREWORK_BLAST".equals(s)) return "ENTITY_FIREWORK_BLAST";
        if ("DONKEY_DEATH".equals(s)) return "ENTITY_DONKEY_DEATH";
        if ("CHICKEN_HURT".equals(s)) return "ENTITY_CHICKEN_HURT";
        if ("COW_HURT".equals(s)) return "ENTITY_COW_HURT";
        if ("DONKEY_ANGRY".equals(s)) return "ENTITY_DONKEY_ANGRY";
        if ("DRINK".equals(s)) return "ENTITY_GENERIC_DRINK";
        if ("FIZZ".equals(s)) return "BLOCK_FIRE_EXTINGUISH";
        if ("FUSE".equals(s)) return "ENTITY_TNT_PRIMED";
        if ("ZOMBIE_DEATH".equals(s)) return "ENTITY_ZOMBIE_DEATH";
        if ("ZOMBIE_PIG_DEATH".equals(s)) return "ENTITY_ZOMBIE_PIG_DEATH";
        if ("WOLF_DEATH".equals(s)) return "ENTITY_WOLF_DEATH";
        if ("VILLAGER_DEATH".equals(s)) return "ENTITY_VILLAGER_DEATH";
        if ("VILLAGER_HAGGLE".equals(s)) return "ENTITY_VILLAGER_TRADING";
        if ("BLAZE_DEATH".equals(s)) return "ENTITY_BLAZE_DEATH";
        if ("CAT_HISS".equals(s)) return "ENTITY_CAT_HISS";
        if ("EXPLODE".equals(s)) return "ENTITY_GENERIC_EXPLODE";
        if ("GHAST_SCREAM".equals(s)) return "ENTITY_GHAST_WARN";
        if ("WOLF_GROWL".equals(s)) return "ENTITY_WOLF_GROWL";
        if ("SKELETON_DEATH".equals(s)) return "ENTITY_SKELETON_DEATH";
        if ("ZOMBIE_REMEDY".equals(s)) return "ENTITY_ZOMBIE_VILLAGER_CONVERTED";
        return s;
    }

    private static void translateUswTaunts(File source, File target, int serverVersion) {
        if (!source.exists()) return;
        FileConfiguration in = YamlConfiguration.loadConfiguration(source);
        if (in.getConfigurationSection("taunts") == null) return;
        YamlConfiguration out = new YamlConfiguration();
        out.set("menuSize", 45);
        boolean legacy18 = serverVersion < 9;
        String tauntSound = legacy18 ? "ORB_PICKUP" : "ENTITY_EXPERIENCE_ORB_PICKUP";
        for (String key : in.getConfigurationSection("taunts").getKeys(false)) {
            String base = "taunts." + key;
            String name = in.getString(base + ".name", key);
            String outKey = normalizeYamlKey(name);
            String icon = in.getString(base + ".icon.type", "BLAZE_POWDER");
            int level = 1 + (in.getInt(base + ".id", 0) * 5);
            int cost = in.getInt(base + ".price", 100);
            List<String> lore = in.getStringList(base + ".icon.meta.lore");
            String message = in.getString(base + ".title", "&eGotcha!");
            int position = in.getInt(base + ".slot", 0);
            int page = in.getInt(base + ".page", 1);
            applyTauntEntry(out, outKey, name, icon, level, cost, lore, message, tauntSound, position, page);
        }
        try { out.save(target); } catch (IOException ignored) {}
    }

    private static void applyTauntEntry(YamlConfiguration out, String outKey, String name, String icon, int level, int cost,
                                        List<String> lore, String message, String sound, int position, int page) {
        String base = "taunts." + outKey;
        out.set(base + ".name", "&b" + name);
        out.set(base + ".icon", icon);
        out.set(base + ".level", level);
        out.set(base + ".cost", cost);
        out.set(base + ".addGlow", true);
        out.set(base + ".lore", lore);
        out.set(base + ".message", message);
        out.set(base + ".sound", sound);
        out.set(base + ".useCustomSound", false);
        out.set(base + ".volume", 10);
        out.set(base + ".pitch", 1);
        out.set(base + ".particleSpeed", 0.2);
        out.set(base + ".particleDensity", 20);
        out.set(base + ".particles", Collections.singletonList("CRIT"));
        out.set(base + ".position", position);
        out.set(base + ".page", page);
    }

    private static void translateUswTrail(File source, File target) {
        if (!source.exists()) return;
        FileConfiguration in = YamlConfiguration.loadConfiguration(source);
        if (in.getConfigurationSection("trails") == null) return;
        YamlConfiguration out = new YamlConfiguration();
        out.set("menuSize", 45);
        for (String key : in.getConfigurationSection("trails").getKeys(false)) {
            String base = "trails." + key;
            String name = in.getString(base + ".name", key);
            String outKey = normalizeYamlKey(name);
            out.set("effects." + outKey + ".displayname", "&b" + name);
            out.set("effects." + outKey + ".icon", "NETHER_STAR");
            out.set("effects." + outKey + ".level", 1 + (in.getInt(base + ".id", 0) * 5));
            out.set("effects." + outKey + ".cost", in.getInt(base + ".price", 100));
            String particle = in.getString(base + ".particle", "CRIT");
            double ox = in.getDouble(base + ".offsetX", 0);
            double oy = in.getDouble(base + ".offsetY", 0);
            double oz = in.getDouble(base + ".offsetZ", 0);
            double speed = in.getDouble(base + ".speed", 1);
            int amount = in.getInt(base + ".amount", 2);
            out.set("effects." + outKey + ".particles", Collections.singletonList(
                    particle + ":" + ox + ":" + oy + ":" + oz + ":" + speed + ":" + amount));
            out.set("effects." + outKey + ".position", in.getInt(base + ".slot", 0));
            out.set("effects." + outKey + ".page", in.getInt(base + ".page", 1));
        }
        try { out.save(target); } catch (IOException ignored) {}
    }

    private static void translateUswKillEffect(File source, File target) {
        if (!source.exists()) return;
        FileConfiguration in = YamlConfiguration.loadConfiguration(source);
        if (in.getConfigurationSection("killeffects") == null) return;
        YamlConfiguration out = new YamlConfiguration();
        out.set("menuSize", 45);
        for (String key : in.getConfigurationSection("killeffects").getKeys(false)) {
            String base = "killeffects." + key;
            String name = in.getString(base + ".name", key);
            String outKey = normalizeYamlKey(name);
            out.set("effects." + outKey + ".displayname", "&b" + name);
            out.set("effects." + outKey + ".icon", "NETHER_STAR");
            out.set("effects." + outKey + ".level", 1 + (in.getInt(base + ".id", 0) * 5));
            out.set("effects." + outKey + ".cost", in.getInt(base + ".price", 100));
            String type = in.getString(base + ".type", "none").toLowerCase(Locale.ENGLISH);
            String particle = "CRIT";
            if (type.contains("fire")) particle = "FLAME";
            else if (type.contains("thunder")) particle = "REDSTONE";
            else if (type.contains("blood")) particle = "SPELL_MOB";
            else if (type.contains("cloud")) particle = "CLOUD";
            out.set("effects." + outKey + ".particles", Collections.singletonList(particle + ":0:1:0:14:8"));
            out.set("effects." + outKey + ".position", in.getInt(base + ".slot", 0));
            out.set("effects." + outKey + ".page", in.getInt(base + ".page", 1));
        }
        try { out.save(target); } catch (IOException ignored) {}
    }

    private static void translateUswWinSounds(File source, File target, int serverVersion) {
        if (!source.exists()) return;
        FileConfiguration in = YamlConfiguration.loadConfiguration(source);
        if (in.getConfigurationSection("sounds") == null) return;
        YamlConfiguration out = new YamlConfiguration();
        out.set("menuSize", 45);
        boolean legacy18 = serverVersion < 9;
        boolean legacy112 = serverVersion < 13;
        int position = 2;
        for (String key : in.getConfigurationSection("sounds").getKeys(false)) {
            String base = "sounds." + key;
            String outKey = normalizeYamlKey(key);
            String sound = in.getString(base + ".sound", "LEVEL_UP");
            double volume = in.getDouble(base + ".volume", 1.0);
            double pitch = in.getDouble(base + ".pitch", 1.0);
            String display = titleCaseKey(key);
            int level = Math.max(1, position + 2);
            String resolvedSound = legacy18 ? sound : mapLegacySound(sound, legacy112);
            applyWinSoundEntry(out, outKey, resolvedSound, "JUKEBOX", display, volume, pitch, level, 100, position, 1);
            position++;
        }
        try { out.save(target); } catch (IOException ignored) {}
    }

    private static void applyWinSoundEntry(YamlConfiguration cfg, String outKey, String sound, String icon, String displayName,
                                           double volume, double pitch, int level, int cost, int position, int page) {
        String base = "sounds." + outKey;
        cfg.set(base + ".sound", sound);
        cfg.set(base + ".isCustomSound", false);
        cfg.set(base + ".volume", volume);
        cfg.set(base + ".pitch", pitch);
        cfg.set(base + ".icon", icon);
        cfg.set(base + ".displayName", "&b" + displayName);
        cfg.set(base + ".level", level);
        cfg.set(base + ".cost", cost);
        cfg.set(base + ".position", position);
        cfg.set(base + ".page", page);
    }

    private static String titleCaseKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return "Sound";
        }
        String normalized = key.trim().replace('_', ' ').replace('-', ' ').toLowerCase(Locale.ENGLISH);
        String[] parts = normalized.split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                out.append(part.substring(1));
            }
        }
        return out.length() == 0 ? "Sound" : out.toString();
    }

    private static List<String> splitLore(String rawLore) {
        if (rawLore == null || rawLore.trim().isEmpty()) {
            return Collections.singletonList("&8SkyWars Perk");
        }
        String[] lines = rawLore.split("\\r?\\n");
        List<String> out = new ArrayList<>();
        for (String line : lines) {
            out.add(line);
        }
        return out;
    }

    private static String mapUswPerkType(String key) {
        String k = normalizeYamlKey(key);
        if ("fallreduction".equals(k) || "firedamagereduction".equals(k) || "shotdamagereduction".equals(k)) {
            return "DAMAGE_REDUCTION";
        }
        if ("arrowrecovery".equals(k)) {
            return "ARROW_RECOVERY";
        }
        if ("blazingarrows".equals(k)) {
            return "BLAZING_ARROWS";
        }
        if ("annoomite".equals(k) || "anno_o_mite".equals(k) || "annoyomite".equals(k)) {
            return "ANNOYOMITE";
        }
        if ("bulldozer".equals(k) || "juggernaut".equals(k)) {
            return "KILL_EFFECT";
        }
        if ("frosty".equals(k) || "swiftness".equals(k) || "nourishment".equals(k)) {
            return "GAME_START";
        }
        if ("endermastery".equals(k)) {
            return "ENDER_MASTERY";
        }
        if ("miningexpertise".equals(k)) {
            return "MINING_EXPERTISE";
        }
        if ("knowledge".equals(k)) {
            return "KNOWLEDGE";
        }
        return "DAMAGE_REDUCTION";
    }

    private static String mapUswDamageType(String key) {
        String k = normalizeYamlKey(key);
        if ("fallreduction".equals(k)) {
            return "FALL";
        }
        if ("firedamagereduction".equals(k)) {
            return "FIRE";
        }
        return "PROJECTILE";
    }

    private static String mapUswPerkEffect(String key) {
        String k = normalizeYamlKey(key);
        if ("bulldozer".equals(k)) {
            return "INCREASE_DAMAGE";
        }
        if ("juggernaut".equals(k)) {
            return "REGENERATION";
        }
        if ("frosty".equals(k)) {
            return "DAMAGE_RESISTANCE";
        }
        if ("swiftness".equals(k)) {
            return "SPEED";
        }
        if ("nourishment".equals(k)) {
            return "SATURATION";
        }
        return null;
    }

    private static Integer mapUswPerkAmplifier(String key) {
        String k = normalizeYamlKey(key);
        if ("bulldozer".equals(k) || "juggernaut".equals(k) || "frosty".equals(k) || "swiftness".equals(k) || "nourishment".equals(k)) {
            return 0;
        }
        return null;
    }

    private static Integer mapUswPerkDuration(FileConfiguration in, String base, String key) {
        int configured = in.getInt(base + ".duration", -1);
        if (configured > 0) {
            return configured;
        }
        String k = normalizeYamlKey(key);
        if ("bulldozer".equals(k)) return 5;
        if ("juggernaut".equals(k)) return 4;
        if ("frosty".equals(k)) return 5;
        if ("swiftness".equals(k)) return 5;
        return null;
    }

    private static int getServerVersionForMigration() {
        try {
            if (MythicSkywars.getNMS() != null) {
                return MythicSkywars.getNMS().getVersion();
            }
        } catch (Throwable ignored) {
        }
        return 13;
    }

    private static void translateUswChests(File uswDir, File pluginDataDir) {
        File source = new File(uswDir, "chests.yml");
        if (!source.exists()) return;
        FileConfiguration in = YamlConfiguration.loadConfiguration(source);
        if (in.getConfigurationSection("chests") == null) return;

        writeChestTypeFiles(in, "basic", new File(pluginDataDir, "chests/basic/basicchest.yml"), new File(pluginDataDir, "chests/basic/basiccenterchest.yml"));
        writeChestTypeFiles(in, "normal", new File(pluginDataDir, "chests/normal/chest.yml"), new File(pluginDataDir, "chests/normal/centerchest.yml"));
        writeChestTypeFiles(in, "op", new File(pluginDataDir, "chests/op/opchest.yml"), new File(pluginDataDir, "chests/op/opcenterchest.yml"));
        MythicSkywars.get().getLogger().info("[MigrationDebug] Translated chests.yml -> six chest files (basic/normal/op + center)");
    }

    private static void writeChestTypeFiles(FileConfiguration in, String chestType, File normalOutFile, File centerOutFile) {
        String base = "chests." + chestType;
        if (in.getConfigurationSection(base) == null) return;

        YamlConfiguration normalOut = new YamlConfiguration();
        YamlConfiguration centerOut = new YamlConfiguration();
        Map<Integer, List<Object>> normalByChance = new TreeMap<>(Collections.reverseOrder());
        Map<Integer, List<Object>> centerByChance = new TreeMap<>(Collections.reverseOrder());

        for (String entryKey : in.getConfigurationSection(base).getKeys(false)) {
            String itemBase = base + "." + entryKey;
            Object itemStack = in.get(itemBase + ".item");
            if (itemStack == null) {
                continue;
            }
            int chance = Math.max(1, in.getInt(itemBase + ".chance", 1));
            boolean center = in.getBoolean(itemBase + ".center", false);
            if (center) {
                centerByChance.computeIfAbsent(chance, k -> new ArrayList<>()).add(itemStack);
            } else {
                normalByChance.computeIfAbsent(chance, k -> new ArrayList<>()).add(itemStack);
            }
        }

        for (Map.Entry<Integer, List<Object>> entry : normalByChance.entrySet()) {
            normalOut.set("chestItems." + entry.getKey() + ".items", entry.getValue());
        }
        for (Map.Entry<Integer, List<Object>> entry : centerByChance.entrySet()) {
            centerOut.set("chestItems." + entry.getKey() + ".items", entry.getValue());
        }

        if (normalOutFile.getParentFile() != null && !normalOutFile.getParentFile().exists()) {
            normalOutFile.getParentFile().mkdirs();
        }
        if (centerOutFile.getParentFile() != null && !centerOutFile.getParentFile().exists()) {
            centerOutFile.getParentFile().mkdirs();
        }
        try { normalOut.save(normalOutFile); } catch (IOException ignored) {}
        try { centerOut.save(centerOutFile); } catch (IOException ignored) {}
    }

    private static String safeFileName(String value) {
        if (value == null || value.trim().isEmpty()) return "Kit";
        return value.trim().replaceAll("[^A-Za-z0-9_\\- ]", "").replace(' ', '_');
    }

    private static String normalizeYamlKey(String value) {
        if (value == null || value.trim().isEmpty()) return "entry";
        return value.toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    private static void mergeIfMissing(Map<Integer, String> base, Map<Integer, String> discovered) {
        if (base == null || discovered == null || discovered.isEmpty()) {
            return;
        }
        for (Map.Entry<Integer, String> entry : discovered.entrySet()) {
            base.putIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    private static Map<Integer, String> autoDetectKitMappings(File pluginDataDir) {
        Map<Integer, String> mappings = new HashMap<>();
        try {
            File uswKitsFile = new File(new File(pluginDataDir, "UltraSkyWars"), "kits.yml");
            if (!uswKitsFile.exists()) {
                return mappings;
            }
            FileConfiguration uswKits = YamlConfiguration.loadConfiguration(uswKitsFile);
            if (uswKits.getConfigurationSection("kits") == null) {
                return mappings;
            }
            List<String> availableSwrKitKeys = discoverSwrKitKeys(pluginDataDir);
            for (String entry : uswKits.getConfigurationSection("kits").getKeys(false)) {
                int id = uswKits.getInt("kits." + entry + ".id", asInt(entry));
                String uswName = uswKits.getString("kits." + entry + ".name", entry);
                String matched = matchByNormalizedName(uswName, availableSwrKitKeys);
                if (matched != null && !matched.isEmpty()) {
                    mappings.put(id, matched);
                }
            }
        } catch (Exception ignored) {
        }
        return mappings;
    }

    private static Map<Integer, String> autoDetectPerkMappings(File pluginDataDir) {
        Map<Integer, String> mappings = new HashMap<>();
        try {
            File uswPerksFile = new File(new File(pluginDataDir, "UltraSkyWars"), "perks.yml");
            if (!uswPerksFile.exists()) {
                return mappings;
            }
            FileConfiguration uswPerks = YamlConfiguration.loadConfiguration(uswPerksFile);
            if (uswPerks.getConfigurationSection("perks") == null) {
                return mappings;
            }
            for (String perkKey : uswPerks.getConfigurationSection("perks").getKeys(false)) {
                int id = uswPerks.getInt("perks." + perkKey + ".id", -1);
                if (id >= 0) {
                    mappings.put(id, perkKey);
                }
            }
        } catch (Exception ignored) {
        }
        return mappings;
    }

    private static List<String> discoverSwrKitKeys(File pluginDataDir) {
        List<String> keys = new ArrayList<>();
        File kitsDir = new File(pluginDataDir, "kits");
        File[] kitFiles = kitsDir.listFiles((dir, name) -> name != null && name.toLowerCase(Locale.ENGLISH).endsWith(".yml"));
        if (kitFiles == null) {
            return keys;
        }
        for (File file : kitFiles) {
            String name = file.getName();
            int dot = name.lastIndexOf('.');
            if (dot > 0) {
                keys.add(name.substring(0, dot));
            }
        }
        return keys;
    }

    private static String matchByNormalizedName(String sourceName, List<String> candidates) {
        if (sourceName == null || candidates == null || candidates.isEmpty()) {
            return null;
        }
        String normalizedSource = normalizeKey(sourceName);
        String fallback = sourceName.trim().replace(' ', '_');
        for (String candidate : candidates) {
            if (normalizeKey(candidate).equals(normalizedSource)) {
                return candidate;
            }
        }
        for (String candidate : candidates) {
            if (candidate.equalsIgnoreCase(fallback)) {
                return candidate;
            }
        }
        return null;
    }

    private static String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9]", "");
    }

    private static Map<Integer, String> readNumericKeyMappings(FileConfiguration cfg, String path) {
        Map<Integer, String> mappings = new HashMap<>();
        if (cfg.getConfigurationSection(path) == null) {
            return mappings;
        }
        for (String key : cfg.getConfigurationSection(path).getKeys(false)) {
            try {
                int id = Integer.parseInt(key.trim());
                String value = cfg.getString(path + "." + key, "").trim();
                if (!value.isEmpty()) {
                    mappings.put(id, value);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return mappings;
    }

    private static Set<String> extractOwnedUnlockPermissions(MigratedStats stats, String skywarsJson, Map<Integer, String> kitIdMappings, Map<Integer, String> perkIdMappings) {
        Set<String> permissions = new LinkedHashSet<>();
        if (skywarsJson == null || skywarsJson.trim().isEmpty()) {
            return permissions;
        }
        for (int tauntId : extractIntArrayField(skywarsJson, "taunts")) {
            if (tauntId > 0) permissions.add("sw.taunt." + tauntId);
        }
        for (int killSoundId : extractIntArrayField(skywarsJson, "killsounds")) {
            if (killSoundId > 0) permissions.add("sw.killsound." + killSoundId);
        }
        for (int glassId : extractIntArrayField(skywarsJson, "glasses")) {
            if (glassId > 0) permissions.add("sw.glasscolor." + glassId);
        }

        Map<Integer, List<Integer>> ownedKitLevels = extractKitsOwnedLevels(skywarsJson);
        for (Map.Entry<Integer, List<Integer>> entry : ownedKitLevels.entrySet()) {
            String mappedKitKey = kitIdMappings.get(entry.getKey());
            if (mappedKitKey != null && !mappedKitKey.trim().isEmpty()) {
                permissions.add("sw.kit." + mappedKitKey.trim().toLowerCase(Locale.ENGLISH));
            }
        }

        Map<Integer, Integer> perkLevels = extractPerkLevels(skywarsJson);
        for (Map.Entry<Integer, Integer> entry : perkLevels.entrySet()) {
            String mappedPerkKey = perkIdMappings.get(entry.getKey());
            if (mappedPerkKey != null && !mappedPerkKey.trim().isEmpty()) {
                int level = Math.max(1, entry.getValue());
                permissions.add("sw.perk." + mappedPerkKey.trim().toLowerCase(Locale.ENGLISH) + "." + level);
            }
        }

        // Approximate equivalent progression from USW soulwell profile fields.
        if (stats.soulWellUsages >= 1) permissions.add("sw.soulwell.upgrade.discount.1");
        if (stats.soulWellUsages >= 2) permissions.add("sw.soulwell.upgrade.discount.2");
        if (stats.soulWellUsages >= 3) permissions.add("sw.soulwell.upgrade.discount.3");
        if (stats.soulWellExtra > 0 || stats.soulanimation > 0) permissions.add("sw.soulwell.upgrade.frames.1");
        return permissions;
    }

    private static List<Integer> extractIntArrayField(String json, String fieldName) {
        List<Integer> values = new ArrayList<>();
        Pattern fieldPattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\\[([^\\]]*)\\]");
        Matcher fieldMatcher = fieldPattern.matcher(json);
        if (!fieldMatcher.find()) {
            return values;
        }
        Matcher intMatcher = INT_LIST_PATTERN.matcher(fieldMatcher.group(1));
        while (intMatcher.find()) {
            try {
                values.add(Integer.parseInt(intMatcher.group()));
            } catch (NumberFormatException ignored) {
            }
        }
        return values;
    }

    private static Map<Integer, List<Integer>> extractKitsOwnedLevels(String json) {
        Map<Integer, List<Integer>> out = new HashMap<>();
        Matcher kitsMatcher = KITS_OBJECT_PATTERN.matcher(json);
        if (!kitsMatcher.find()) {
            return out;
        }
        String kitsObject = kitsMatcher.group(1);
        Matcher entryMatcher = NUMERIC_ARRAY_ENTRY_PATTERN.matcher(kitsObject);
        while (entryMatcher.find()) {
            int kitId;
            try {
                kitId = Integer.parseInt(entryMatcher.group(1));
            } catch (NumberFormatException ignored) {
                continue;
            }
            List<Integer> levels = new ArrayList<>();
            Matcher intMatcher = INT_LIST_PATTERN.matcher(entryMatcher.group(2));
            while (intMatcher.find()) {
                try {
                    levels.add(Integer.parseInt(intMatcher.group()));
                } catch (NumberFormatException ignored) {
                }
            }
            out.put(kitId, levels);
        }
        return out;
    }

    private static Map<Integer, Integer> extractPerkLevels(String json) {
        Map<Integer, Integer> out = new HashMap<>();
        String perksDataObject = extractJsonObject(json, "perksData");
        if (perksDataObject == null) {
            return out;
        }
        Matcher matcher = PERK_ENTRY_PATTERN.matcher(perksDataObject);
        while (matcher.find()) {
            try {
                int perkId = Integer.parseInt(matcher.group(1));
                int level = Integer.parseInt(matcher.group(2));
                out.put(perkId, Math.max(1, level));
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    private static String extractJsonObject(String json, String objectField) {
        Matcher matcher = OBJECT_FIELD_PATTERN.matcher(json);
        while (matcher.find()) {
            if (objectField.equals(matcher.group(1))) {
                return matcher.group(2);
            }
        }
        return null;
    }

    private static boolean migrateToSql(Connection connection, String uuid, String name, MigratedStats stats, boolean overwrite) throws SQLException {
        if (connection == null) {
            throw new SQLException("MythicSkywars SQL connection is null.");
        }
        String resolvedName = choosePreferredName(name, null, uuid);

        if (!overwrite) {
            PreparedStatement insertIgnore = null;
            try {
                insertIgnore = connection.prepareStatement(
                        "INSERT IGNORE INTO `sw_player` (`player_id`, `uuid`, `player_name`, `wins`, `losses`, `kills`, `deaths`, `xp`, `pareffect`, `proeffect`, `glasscolor`, `killsound`, `winsound`, `taunt`, `prestige_icon`, `souls`, `soulwell_usages`, `soulwell_legendaries`, `soulwell_rares`, `soulwell_souls_gathered`, `soulwell_souls_purchased`) " +
                                "VALUES (NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
                );
                fillPlayerStatement(insertIgnore, uuid, resolvedName, stats);
                return insertIgnore.executeUpdate() > 0;
            } finally {
                if (insertIgnore != null) {
                    insertIgnore.close();
                }
            }
        }

        PreparedStatement upsert = null;
        try {
            upsert = connection.prepareStatement(
                    "INSERT INTO `sw_player` (`player_id`, `uuid`, `player_name`, `wins`, `losses`, `kills`, `deaths`, `xp`, `pareffect`, `proeffect`, `glasscolor`, `killsound`, `winsound`, `taunt`, `prestige_icon`, `souls`, `soulwell_usages`, `soulwell_legendaries`, `soulwell_rares`, `soulwell_souls_gathered`, `soulwell_souls_purchased`) " +
                            "VALUES (NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE `player_name` = VALUES(`player_name`), `wins` = VALUES(`wins`), `losses` = VALUES(`losses`), `kills` = VALUES(`kills`), `deaths` = VALUES(`deaths`), `xp` = VALUES(`xp`), `pareffect` = VALUES(`pareffect`), `proeffect` = VALUES(`proeffect`), `glasscolor` = VALUES(`glasscolor`), `killsound` = VALUES(`killsound`), `winsound` = VALUES(`winsound`), `taunt` = VALUES(`taunt`), `prestige_icon` = VALUES(`prestige_icon`), `souls` = VALUES(`souls`), `soulwell_usages` = VALUES(`soulwell_usages`), `soulwell_legendaries` = VALUES(`soulwell_legendaries`), `soulwell_rares` = VALUES(`soulwell_rares`), `soulwell_souls_gathered` = VALUES(`soulwell_souls_gathered`), `soulwell_souls_purchased` = VALUES(`soulwell_souls_purchased`);"
            );
            fillPlayerStatement(upsert, uuid, resolvedName, stats);
            upsert.executeUpdate();
            return true;
        } finally {
            if (upsert != null) {
                upsert.close();
            }
        }
    }

    private static void fillPlayerStatement(PreparedStatement statement, String uuid, String name, MigratedStats stats) throws SQLException {
        statement.setString(1, uuid);
        statement.setString(2, name);
        statement.setInt(3, stats.wins);
        statement.setInt(4, stats.losses);
        statement.setInt(5, stats.kills);
        statement.setInt(6, stats.deaths);
        statement.setInt(7, stats.xp);
        statement.setString(8, stats.particleEffect);
        statement.setString(9, stats.projectileEffect);
        statement.setString(10, stats.glassColor);
        statement.setString(11, stats.killSound);
        statement.setString(12, stats.winSound);
        statement.setString(13, stats.taunt);
        statement.setString(14, stats.prestigeIcon != null ? stats.prestigeIcon : "icon1");
        statement.setInt(15, stats.souls);
        statement.setInt(16, stats.soulWellUsages);
        statement.setInt(17, stats.soulWellLegendaries);
        statement.setInt(18, stats.soulWellRares);
        statement.setInt(19, stats.soulWellSoulsGathered);
        statement.setInt(20, stats.soulWellSoulsPurchased);
    }

    private static void storeRawUswSnapshotSql(Connection connection, String uuid, String skywarsJson, String fullDocumentJson) throws SQLException {
        if (connection == null) {
            return;
        }

        PreparedStatement statement = null;
        try {
            String payload = firstNonEmpty(skywarsJson, fullDocumentJson);
            statement = connection.prepareStatement(
                    "UPDATE `sw_player` SET `usw_data` = ? WHERE `uuid` = ?;"
            );
            statement.setString(1, payload);
            statement.setString(2, uuid);
            statement.executeUpdate();
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    private static void storeEconomyBuiltinSql(Connection connection, String uuid, double amount) throws SQLException {
        if (connection == null) {
            return;
        }
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(
                    "UPDATE `sw_player` SET `economy` = ? WHERE `uuid` = ?;"
            );
            statement.setDouble(1, Math.max(0D, amount));
            statement.setString(2, uuid);
            statement.executeUpdate();
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    private static void storePermissionsSql(Connection connection, String uuid, String playerName, Set<String> permissions) throws SQLException {
        if (connection == null || permissions == null || permissions.isEmpty()) {
            return;
        }
        PreparedStatement statement = null;
        try {
            String query = "INSERT INTO `sw_permissions` (`uuid`, `playername`, `permissions`) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE " +
                    "`uuid`=`uuid`, `playername`=`playername`, `permissions`=`permissions`;";
            statement = connection.prepareStatement(query);
            for (String permission : permissions) {
                statement.setString(1, uuid);
                statement.setString(2, playerName);
                statement.setString(3, permission);
                statement.executeUpdate();
            }
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    private static void storePermissionsYaml(String uuid, Set<String> permissions) throws Exception {
        if (permissions == null || permissions.isEmpty()) {
            return;
        }
        File playerDataDirectory = new File(MythicSkywars.get().getDataFolder(), "player_data");
        if (!playerDataDirectory.exists() && !playerDataDirectory.mkdirs()) {
            return;
        }
        File playerFile = new File(playerDataDirectory, uuid + ".yml");
        if (!playerFile.exists() && !playerFile.createNewFile()) {
            return;
        }
        FileConfiguration fc = YamlConfiguration.loadConfiguration(playerFile);
        List<String> existing = new ArrayList<>(fc.getStringList("permissions"));
        Set<String> merged = new LinkedHashSet<>(existing);
        merged.addAll(permissions);
        fc.set("permissions", new ArrayList<>(merged));
        fc.save(playerFile);
    }

    private static boolean migrateToYaml(String uuid, String name, MigratedStats stats, boolean overwrite) throws Exception {
        File playerDataDirectory = new File(MythicSkywars.get().getDataFolder(), "player_data");
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
            stats.soulWellExtra = Math.max(0, extractJsonInt(skywarsJson, "soulWellExtra", 0));
            stats.soulanimation = Math.max(0, extractJsonInt(skywarsJson, "soulanimation", 0));
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

            // USW frequently stores aggregated values under "totalStats" and omits direct "losses".
            int totalPlayed = extractNestedJsonInt(skywarsJson, "totalStats", "PLAYED", -1);
            int totalWins = extractNestedJsonInt(skywarsJson, "totalStats", "WINS", -1);
            int totalDeaths = extractNestedJsonInt(skywarsJson, "totalStats", "DEATHS", -1);
            if (totalWins >= 0) {
                stats.wins = Math.max(stats.wins, totalWins);
            }
            if (totalDeaths >= 0) {
                stats.deaths = Math.max(stats.deaths, totalDeaths);
            }
            if (totalPlayed >= 0 && totalWins >= 0) {
                stats.losses = Math.max(stats.losses, Math.max(0, totalPlayed - totalWins));
            }
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

    private static int extractNestedJsonInt(String json, String objectField, String intField, int fallback) {
        if (json == null) {
            return fallback;
        }
        Matcher matcher = OBJECT_FIELD_PATTERN.matcher(json);
        while (matcher.find()) {
            if (!objectField.equals(matcher.group(1))) {
                continue;
            }
            String objectJson = matcher.group(2);
            Matcher intMatcher = INT_FIELD_PATTERN.matcher(objectJson);
            while (intMatcher.find()) {
                if (intField.equals(intMatcher.group(1))) {
                    try {
                        return Integer.parseInt(intMatcher.group(2));
                    } catch (NumberFormatException ignored) {
                        return fallback;
                    }
                }
            }
            return fallback;
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
        return buildImportPlaceholderName(fallback);
    }

    private static String buildImportPlaceholderName(String fallback) {
        String trimmed = fallback == null ? "" : fallback.trim();
        if (trimmed.isEmpty()) {
            return "USW_IMPORT_PENDING_UNKNOWN";
        }
        String compact = trimmed.replaceAll("[^A-Za-z0-9]", "");
        if (compact.isEmpty()) {
            compact = "UNKNOWN";
        }
        if (compact.length() > 16) {
            compact = compact.substring(0, 16);
        }
        return "USW_IMPORT_PENDING_" + compact;
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
        private int soulWellExtra;
        private int soulanimation;
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
