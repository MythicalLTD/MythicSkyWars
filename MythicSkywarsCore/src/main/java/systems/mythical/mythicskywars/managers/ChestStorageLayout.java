package systems.mythical.mythicskywars.managers;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Organised loot paths under {@code chests/normal|basic|op|crates|lucky/} plus one-time migration
 * from the legacy plugin root ({@code chest.yml}, {@code luckyblocks.yml}, …).
 */
public final class ChestStorageLayout {

    private static final Map<String, String> LOGICAL_TO_LAYOUT_RELATIVE;

    static {
        Map<String, String> m = new HashMap<>();
        m.put(normalizeLogicalKey("chest.yml"), "chests/normal/chest.yml");
        m.put(normalizeLogicalKey("centerchest.yml"), "chests/normal/centerchest.yml");
        m.put(normalizeLogicalKey("basicchest.yml"), "chests/basic/basicchest.yml");
        m.put(normalizeLogicalKey("basiccenterchest.yml"), "chests/basic/basiccenterchest.yml");
        m.put(normalizeLogicalKey("opchest.yml"), "chests/op/opchest.yml");
        m.put(normalizeLogicalKey("opcenterchest.yml"), "chests/op/opcenterchest.yml");
        m.put(normalizeLogicalKey("crates.yml"), "chests/crates/crates.yml");
        m.put(normalizeLogicalKey("luckyblocks.yml"), "chests/lucky/luckyblocks.yml");
        LOGICAL_TO_LAYOUT_RELATIVE = Collections.unmodifiableMap(m);
    }

    private ChestStorageLayout() {
    }

    private static String normalizeLogicalKey(String logicalYamlName) {
        if (logicalYamlName == null) {
            return "";
        }
        return logicalYamlName.trim().toLowerCase();
    }

    /** Same relative path inside the shaded jar resources and {@link JavaPlugin#getDataFolder()}. */
    public static String layoutRelativePath(String logicalYamlName) {
        String rel = LOGICAL_TO_LAYOUT_RELATIVE.get(normalizeLogicalKey(logicalYamlName));
        if (rel == null) {
            throw new IllegalArgumentException("Unknown chest data file: " + logicalYamlName);
        }
        return rel;
    }

    /**
     * Resolves where to load/save loot data.
     * <p><b>Upgrades:</b> an existing flat file in the plugin root ({@code chest.yml}, …) is copied into
     * {@code chests/...} once; the legacy file is then removed only after a successful copy. If copying fails
     * (permissions, disk full), callers keep reading/writing that legacy path so loot is never replaced by jar
     * defaults alone.</p>
     */
    public static File resolveDataFile(JavaPlugin plugin, String logicalYamlName, boolean seedFromJarIfAbsent) {
        String key = normalizeLogicalKey(logicalYamlName);
        String laidOutRelative = LOGICAL_TO_LAYOUT_RELATIVE.get(key);
        if (laidOutRelative == null) {
            throw new IllegalArgumentException("Unknown chest data file: " + logicalYamlName);
        }
        File organised = new File(plugin.getDataFolder(), laidOutRelative);
        File legacyFlat = new File(plugin.getDataFolder(), key);
        migrateLegacyFlatIfNeeded(plugin, legacyFlat, organised);
        if (organised.exists()) {
            return organised;
        }
        if (legacyFlat.exists()) {
            plugin.getLogger().warning("["
                    + plugin.getName() + "] Still using legacy " + key + " at plugin folder root (cannot write to "
                    + organised.getPath() + "). Fix folder permissions — your loot was not wiped.");
            return legacyFlat;
        }
        if (seedFromJarIfAbsent) {
            try {
                plugin.saveResource(laidOutRelative, false);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Bundled loot template missing: " + laidOutRelative);
            }
        }
        return organised;
    }

    public static File luckyBlocksYaml(JavaPlugin plugin, boolean seedIfAbsent) {
        return resolveDataFile(plugin, "luckyblocks.yml", seedIfAbsent);
    }

    /**
     * For admin messages — prefers migrated path, falls back to a legacy flat file if still present.
     */
    public static File resolvedFileForMessaging(JavaPlugin plugin, String logicalYamlName) {
        String key = normalizeLogicalKey(logicalYamlName);
        String laidOutRelative = LOGICAL_TO_LAYOUT_RELATIVE.get(key);
        if (laidOutRelative == null) {
            return new File(plugin.getDataFolder(), logicalYamlName);
        }
        File organised = new File(plugin.getDataFolder(), laidOutRelative);
        if (organised.exists()) {
            return organised;
        }
        File legacy = new File(plugin.getDataFolder(), key);
        return legacy.exists() ? legacy : organised;
    }

    private static void migrateLegacyFlatIfNeeded(JavaPlugin plugin, File legacyFlat, File organisedDestination) {
        if (!legacyFlat.exists() || organisedDestination.exists()) {
            return;
        }
        File parent = organisedDestination.getParentFile();
        mkdirs(plugin, parent);
        try {
            Files.copy(legacyFlat.toPath(), organisedDestination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(legacyFlat.toPath());
            plugin.getLogger().info("["
                    + plugin.getName() + "] Upgraded loot file layout: moved " + legacyFlat.getName() + " to "
                    + organisedDestination.getPath());
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING,
                    "[" + plugin.getName() + "] Could not move " + legacyFlat.getName()
                            + " into chests/ (will keep loading it from plugin root until this succeeds)", ex);
        }
    }

    private static void mkdirs(JavaPlugin plugin, File dir) {
        if (dir != null && !dir.exists() && !dir.mkdirs()) {
            plugin.getLogger().warning("Could not create directory: " + dir.getAbsolutePath());
        }
    }
}
