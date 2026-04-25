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
import java.util.List;
import java.util.logging.Level;

/**
 * Merges missing keys from a bundled YAML resource into a configuration.
 * Existing values are never overwritten.
 */
public final class ConfigMerge {

    private ConfigMerge() {
    }

    /**
     * Loads {@code plugins/&lt;plugin&gt;/config.yml} from disk (no Bukkit {@code setDefaults} merge), adds any paths
     * missing on disk from the bundled template, then saves. Call {@link JavaPlugin#reloadConfig()} after this so
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
        int added = mergeMissingKeysFromResource(plugin, resourceName, disk);
        if (added > 0) {
            disk.save(configFile);
        }
        return added;
    }

    /**
     * @return number of paths that were added to {@code target}
     */
    public static int mergeMissingKeysFromResource(JavaPlugin plugin, String resourceName, FileConfiguration target) {
        InputStream in = plugin.getResource(resourceName);
        if (in == null) {
            plugin.getLogger().warning("Bundled config resource not found: " + resourceName);
            return 0;
        }
        YamlConfiguration defaults;
        try (InputStreamReader reader = new InputStreamReader(in, Charsets.UTF_8)) {
            defaults = YamlConfiguration.loadConfiguration(reader);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to read bundled config: " + resourceName, e);
            return 0;
        }
        int added = 0;
        List<String> keys = new ArrayList<>(defaults.getKeys(true));
        // Shorter paths first so a missing parent section (e.g. chests.refillStatusHolograms) is set before its leaves.
        keys.sort(Comparator.comparingInt(k -> k.split("\\.").length));
        for (String key : keys) {
            // 1.8.x has no ConfigurationSection#contains(String, boolean); use it via reflection when available.
            if (pathMissingOnTarget(target, key)) {
                target.set(key, defaults.get(key));
                added++;
            }
        }
        if (added > 0) {
            plugin.getLogger().info("Added " + added + " missing config entries from " + resourceName + " (existing values were not changed).");
        }
        return added;
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
}
