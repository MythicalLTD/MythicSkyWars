package com.walrusone.skywarsreloaded.utilities;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public final class LevelManager {

    private static final LevelManager INSTANCE = new LevelManager();
    private final NavigableMap<Integer, Integer> xpByLevel = new TreeMap<>();
    private final Map<Integer, String> prefixByLevel = new HashMap<>();
    private final Map<Integer, Integer> rankupCostByLevel = new HashMap<>();
    private String progressSymbol = "■";
    private String progressUnlockedColor = "&b";
    private String progressLockedColor = "&7";
    private String progressFormat = "&8 [{progress}&8]";
    private int progressSegments = 10;
    private final Map<String, Integer> xpRewards = new HashMap<>();

    private LevelManager() {
    }

    public static LevelManager get() {
        return INSTANCE;
    }

    public synchronized void load(Plugin plugin) {
        File levelFile = new File(plugin.getDataFolder(), "levels.yml");
        if (!levelFile.exists()) {
            plugin.saveResource("levels.yml", false);
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(levelFile);
        xpByLevel.clear();
        prefixByLevel.clear();
        rankupCostByLevel.clear();
        xpRewards.clear();

        loadXpRewards(cfg.getConfigurationSection("xp-rewards"));
        loadProgressBar(cfg.getConfigurationSection("progress-bar"));

        ConfigurationSection levelsSection = cfg.getConfigurationSection("levels");
        boolean loadedAdvanced = tryLoadAdvancedLevels(levelsSection);
        if (!loadedAdvanced) {
            loadSimpleXpThresholds(levelsSection);
        }
        if (xpByLevel.isEmpty()) {
            xpByLevel.put(1, 0);
            xpByLevel.put(2, 100);
            xpByLevel.put(3, 250);
            xpByLevel.put(4, 450);
            xpByLevel.put(5, 700);
        }
        if (!xpByLevel.containsKey(1)) {
            xpByLevel.put(1, 0);
        }
        for (int level : xpByLevel.keySet()) {
            if (!prefixByLevel.containsKey(level)) {
                prefixByLevel.put(level, "&7[" + level + "✩] ");
            }
        }
    }

    public synchronized int getLevelForXp(int xp) {
        int safeXp = Math.max(0, xp);
        int level = 1;
        for (Map.Entry<Integer, Integer> e : xpByLevel.entrySet()) {
            if (safeXp >= e.getValue()) {
                level = e.getKey();
            } else {
                break;
            }
        }
        return level;
    }

    public synchronized int getXpForLevel(int level) {
        Integer xp = xpByLevel.get(level);
        if (xp != null) {
            return xp;
        }
        return xpByLevel.firstEntry().getValue();
    }

    public synchronized int getNextLevel(int level) {
        Integer higher = xpByLevel.higherKey(level);
        return higher == null ? level : higher;
    }

    public synchronized int getXpToNextLevel(int xp) {
        int level = getLevelForXp(xp);
        int nextLevel = getNextLevel(level);
        if (nextLevel == level) {
            return 0;
        }
        int nextXp = getXpForLevel(nextLevel);
        return Math.max(0, nextXp - Math.max(0, xp));
    }

    public synchronized int getXpIntoCurrentLevel(int xp) {
        int level = getLevelForXp(xp);
        int base = getXpForLevel(level);
        return Math.max(0, Math.max(0, xp) - base);
    }

    public synchronized int getXpRequiredForCurrentLevel(int xp) {
        int level = getLevelForXp(xp);
        int next = getNextLevel(level);
        if (next == level) {
            return 0;
        }
        return Math.max(0, getXpForLevel(next) - getXpForLevel(level));
    }

    public synchronized int getProgressPercent(int xp) {
        int req = getXpRequiredForCurrentLevel(xp);
        if (req <= 0) {
            return 100;
        }
        int into = getXpIntoCurrentLevel(xp);
        return Math.min(100, Math.max(0, (int) Math.floor((into * 100.0D) / req)));
    }

    public synchronized String getPrefixForLevel(int level) {
        String raw = prefixByLevel.get(level);
        if (raw == null) {
            raw = "&7[" + level + "✩] ";
        }
        return ChatColor.translateAlternateColorCodes('&', raw.replace("{number}", Integer.toString(level)));
    }

    public synchronized String getPrefixForXp(int xp) {
        return getPrefixForLevel(getLevelForXp(xp));
    }

    public synchronized String getProgressBar(int xp) {
        int pct = getProgressPercent(xp);
        int unlocked = Math.min(progressSegments, Math.max(0, (int) Math.round((pct / 100.0D) * progressSegments)));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < progressSegments; i++) {
            if (i < unlocked) {
                sb.append(progressUnlockedColor).append(progressSymbol);
            } else {
                sb.append(progressLockedColor).append(progressSymbol);
            }
        }
        String built = progressFormat.replace("{progress}", sb.toString());
        return ChatColor.translateAlternateColorCodes('&', built);
    }

    private void loadProgressBar(ConfigurationSection section) {
        if (section == null) {
            return;
        }
        progressSymbol = section.getString("symbol", progressSymbol);
        progressUnlockedColor = section.getString("unlocked-color", progressUnlockedColor);
        progressLockedColor = section.getString("locked-color", progressLockedColor);
        progressFormat = section.getString("format", progressFormat);
        progressSegments = Math.max(5, Math.min(30, section.getInt("segments", progressSegments)));
    }

    private void loadXpRewards(ConfigurationSection section) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            xpRewards.put(key.toLowerCase(), section.getInt(key, 0));
        }
    }

    private void loadSimpleXpThresholds(ConfigurationSection section) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (!(value instanceof Number)) {
                continue;
            }
            try {
                int level = Integer.parseInt(key);
                int xp = ((Number) value).intValue();
                if (level > 0 && xp >= 0) {
                    xpByLevel.put(level, xp);
                }
            } catch (NumberFormatException ignored) {
                // Ignore non-numeric levels in simple mode.
            }
        }
    }

    private boolean tryLoadAdvancedLevels(ConfigurationSection section) {
        if (section == null) {
            return false;
        }
        boolean foundConfigSection = false;
        for (String key : section.getKeys(false)) {
            if (section.isConfigurationSection(key)) {
                foundConfigSection = true;
                break;
            }
        }
        if (!foundConfigSection) {
            return false;
        }

        int maxLevel = 0;
        int othersCost = 1000;
        String othersName = "&7[{number}✩] ";
        Map<Integer, Integer> tempCostByLevel = new HashMap<>();
        Map<Integer, String> tempPrefixByLevel = new HashMap<>();

        for (String key : section.getKeys(false)) {
            if (!section.isConfigurationSection(key)) {
                continue;
            }
            ConfigurationSection node = section.getConfigurationSection(key);
            if (node == null) {
                continue;
            }
            int rankupCost = Math.max(1, node.getInt("rankup-cost", 1000));
            String name = node.getString("name", "&7[{number}✩] ");
            if ("others".equalsIgnoreCase(key)) {
                othersCost = rankupCost;
                othersName = name;
                continue;
            }
            int start;
            int end;
            if (key.contains("-")) {
                String[] parts = key.split("-");
                if (parts.length != 2) {
                    continue;
                }
                try {
                    start = Integer.parseInt(parts[0].trim());
                    end = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException ignored) {
                    continue;
                }
            } else {
                try {
                    start = Integer.parseInt(key.trim());
                    end = start;
                } catch (NumberFormatException ignored) {
                    continue;
                }
            }
            if (start <= 0 || end < start) {
                continue;
            }
            for (int lvl = start; lvl <= end; lvl++) {
                tempCostByLevel.put(lvl, rankupCost);
                tempPrefixByLevel.put(lvl, name);
            }
            maxLevel = Math.max(maxLevel, end);
        }

        maxLevel = Math.max(maxLevel, 100);
        int xp = 0;
        for (int level = 1; level <= maxLevel; level++) {
            xpByLevel.put(level, xp);
            int cost = tempCostByLevel.getOrDefault(level, othersCost);
            String prefix = tempPrefixByLevel.getOrDefault(level, othersName);
            rankupCostByLevel.put(level, cost);
            prefixByLevel.put(level, prefix);
            xp += cost;
        }
        return true;
    }

    public synchronized int getXpReward(String key, int fallback) {
        if (key == null) {
            return fallback;
        }
        return xpRewards.getOrDefault(key.toLowerCase(), fallback);
    }
}
