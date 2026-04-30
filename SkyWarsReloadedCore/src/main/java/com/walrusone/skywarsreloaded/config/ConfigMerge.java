package com.walrusone.skywarsreloaded.config;

import com.google.common.base.Charsets;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Synchronizes a disk YAML with a bundled YAML resource.
 * Missing bundled keys are added, and obsolete disk keys are removed.
 * Existing values for valid keys are never overwritten.
 */
public final class ConfigMerge {
    /**
     * Keys written by runtime code (e.g. /sw setspawn) that may be absent from bundled templates.
     * Never prune these.
     */
    private static final Set<String> RUNTIME_MANAGED_ROOT_KEYS = new HashSet<String>() {{
        add("spawn");
    }};

    private ConfigMerge() {
    }

    /**
     * Loads {@code plugins/&lt;plugin&gt;/config.yml} from disk (no Bukkit {@code setDefaults} merge), adds any paths
     * missing on disk from the bundled template, removes any paths that no longer exist in the bundled template,
     * then saves. Call {@link JavaPlugin#reloadConfig()} after this so
     * {@link JavaPlugin#getConfig()} matches the file.
     *
     * @return number of paths added
     */
    public static int mergeBundledDefaultsIntoDiskConfigFile(JavaPlugin plugin, String resourceName) throws IOException {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder for config merge.");
        }
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration disk = YamlConfiguration.loadConfiguration(configFile);
        YamlConfiguration defaults = loadYamlFromResource(plugin, resourceName);
        if (defaults == null) {
            return 0;
        }
        int added = mergeMissingKeys(defaults, disk);
        int removed = pruneKeysNotInSource(defaults, disk);
        if (added > 0 || removed > 0) {
            disk.save(configFile);
        }
        if (removed > 0) {
            plugin.getLogger().info("Removed " + removed + " obsolete config key(s) not present in " + resourceName + ".");
        }
        return added;
    }

    /**
     * @return number of paths that were added to {@code target}
     */
    public static int mergeMissingKeysFromResource(JavaPlugin plugin, String resourceName, FileConfiguration target) {
        YamlConfiguration defaults = loadYamlFromResource(plugin, resourceName);
        if (defaults == null) {
            return 0;
        }
        return mergeMissingKeys(defaults, target);
    }

    private static YamlConfiguration loadYamlFromResource(JavaPlugin plugin, String resourceName) {
        InputStream in = plugin.getResource(resourceName);
        if (in == null) {
            plugin.getLogger().warning("Bundled config resource not found: " + resourceName);
            return null;
        }
        try (InputStreamReader reader = new InputStreamReader(in, Charsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to read bundled config: " + resourceName, e);
            return null;
        }
    }

    private static int mergeMissingKeys(FileConfiguration defaults, FileConfiguration target) {
        int added = 0;
        List<String> keys = new ArrayList<>(defaults.getKeys(true));
        // Shorter paths first so a missing parent section is set before its leaves.
        keys.sort(Comparator.comparingInt(k -> k.split("\\.").length));
        for (String key : keys) {
            // 1.8.x has no ConfigurationSection#contains(String, boolean); use it via reflection when available.
            if (pathMissingOnTarget(target, key)) {
                target.set(key, defaults.get(key));
                added++;
            }
        }
        return added;
    }

    /**
     * Remove keys from target that do not exist in bundled defaults.
     *
     * @return number of removed paths
     */
    public static int pruneKeysNotInSource(FileConfiguration defaults, FileConfiguration target) {
        int removed = 0;
        List<String> keys = new ArrayList<>(target.getKeys(true));
        // Remove deepest paths first so child values are removed before parent sections.
        keys.sort((a, b) -> Integer.compare(b.split("\\.").length, a.split("\\.").length));
        for (String key : keys) {
            if (isRuntimeManagedKey(key)) {
                continue;
            }
            if (!pathExistsOnSource(defaults, key)) {
                target.set(key, null);
                removed++;
            }
        }
        return removed;
    }

    /**
     * True if {@code key} is not defined on this configuration (ignores Bukkit defaults when the API supports it).
     */
    private static boolean pathMissingOnTarget(FileConfiguration target, String key) {
        try {
            Method m = target.getClass().getMethod("contains", String.class, boolean.class);
            return !(Boolean) m.invoke(target, key, Boolean.TRUE);
        } catch (NoSuchMethodException e) {
            return !target.contains(key);
        } catch (ReflectiveOperationException e) {
            return !target.contains(key);
        }
    }

    private static boolean pathExistsOnSource(FileConfiguration source, String key) {
        try {
            Method m = source.getClass().getMethod("contains", String.class, boolean.class);
            return (Boolean) m.invoke(source, key, Boolean.TRUE);
        } catch (NoSuchMethodException e) {
            return source.contains(key);
        } catch (ReflectiveOperationException e) {
            return source.contains(key);
        }
    }

    private static boolean isRuntimeManagedKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        String root = key;
        int dot = key.indexOf('.');
        if (dot >= 0) {
            root = key.substring(0, dot);
        }
        return RUNTIME_MANAGED_ROOT_KEYS.contains(root);
    }
}
