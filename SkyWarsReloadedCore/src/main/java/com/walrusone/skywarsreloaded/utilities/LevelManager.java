package com.walrusone.skywarsreloaded.utilities;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.managers.PlayerStat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public final class LevelManager {

    private static final LevelManager INSTANCE = new LevelManager();
    private final NavigableMap<Integer, Integer> xpByLevel = new TreeMap<>();
    private final Map<Integer, String> prefixByLevel = new HashMap<>();
    private final Map<Integer, Integer> rankupCostByLevel = new HashMap<>();
    private final Map<Integer, List<String>> levelRewards = new HashMap<>();
    private String progressSymbol = "■";
    private String progressUnlockedColor = "&b";
    private String progressLockedColor = "&7";
    private String progressFormat = "&8 [{progress}&8]";
    private int progressSegments = 10;
    private final Map<String, Integer> xpRewards = new HashMap<>();
    private boolean levelUpRewardsEnabled = true;

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
        levelRewards.clear();

        loadXpRewards(cfg.getConfigurationSection("xp-rewards"));
        loadProgressBar(cfg.getConfigurationSection("progress-bar"));

        levelUpRewardsEnabled = cfg.getBoolean("level-up-rewards.enabled", true);

        ConfigurationSection levelsSection = cfg.getConfigurationSection("levels");

        boolean loadedUsw = tryLoadExplicitUswStyleLevels(levelsSection);
        if (!loadedUsw) {
            boolean loadedAdvanced = tryLoadAdvancedLevels(levelsSection);
            if (!loadedAdvanced) {
                loadSimpleXpThresholds(levelsSection);
            }
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

        PrestigeManager.get().load(plugin);
    }

    public synchronized boolean isLevelUpRewardsEnabled() {
        return levelUpRewardsEnabled;
    }

    /** Console / admin reward lines for the given SkyWars numeric level (UltraSkyWars-style). */
    public synchronized List<String> getRewardCommandsForLevel(int level) {
        List<String> list = levelRewards.get(level);
        return list == null ? java.util.Collections.emptyList() : java.util.Collections.unmodifiableList(list);
    }

    /** Fires when XP increases in normal gameplay (online player). */
    public void grantLevelUpRewards(Player player, int oldXp, int newXp) {
        if (!levelUpRewardsEnabled || player == null || newXp <= oldXp) {
            return;
        }
        int oldLvl = getLevelForXp(oldXp);
        int newLvl = getLevelForXp(newXp);
        if (newLvl <= oldLvl) {
            return;
        }
        final java.util.UUID uid = player.getUniqueId();
        Bukkit.getScheduler().runTask(SkyWarsReloaded.get(), () -> {
            Player p = SkyWarsReloaded.get().getServer().getPlayer(uid);
            if (p == null || !p.isOnline()) {
                return;
            }
            for (int level = oldLvl + 1; level <= newLvl; level++) {
                List<String> cmds = levelRewards.get(level);
                if (cmds == null || cmds.isEmpty()) {
                    continue;
                }
                for (String raw : cmds) {
                    runRewardLine(p, raw);
                }
                if (SkyWarsReloaded.getMessaging().getMessage("levels.level-reward-broadcast") != null) {
                    p.sendMessage(new Messaging.MessageFormatter()
                            .setVariable("level", Integer.toString(level))
                            .format("levels.level-reward-broadcast"));
                }
            }
        });
    }

    private void runRewardLine(Player player, String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return;
        }
        String trimmed = raw.trim().startsWith("/") ? raw.trim().substring(1) : raw.trim();
        String lowered = trimmed.toLowerCase(Locale.ROOT);
        boolean selfPlayer = SkyWarsReloaded.get().getName().equalsIgnoreCase("skywars")
                || lowered.startsWith("skywars ")
                || lowered.startsWith("sw ");

        if (lowered.startsWith("tell ") || lowered.startsWith("msg ") || lowered.startsWith("minecraft:tell ")) {
            trimmed = applyPlaceholders(player, trimmed);
            Bukkit.dispatchCommand(player, trimmed);
        } else if (selfPlayer) {
            trimmed = applyPlaceholders(player, trimmed);
            player.performCommand(trimmed);
        } else {
            trimmed = applyPlaceholders(player, trimmed);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), trimmed);
        }
    }

    private static String applyPlaceholders(Player player, String command) {
        String name = player.getName();
        return command
                .replace("<player>", name)
                .replace("%player%", name)
                .replace("{player}", name);
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

    /**
     * Prefix + optional prestige icon (from {@link PrestigeManager}) for scoreboard / chat placeholders.
     */
    public synchronized String getDisplayPrefixForPlayer(PlayerStat ps, Player player, int levelHint) {
        if (ps == null) {
            return getPrefixForLevel(levelHint);
        }
        String levelPart = getPrefixForLevel(levelHint);
        if (!PrestigeManager.get().isEnabled()) {
            return levelPart;
        }
        String pfx = PrestigeManager.get().translatePrefixForStat(ps, levelHint, ps.getPrestigeIcon());
        if (pfx == null || pfx.isEmpty()) {
            return levelPart;
        }
        return ChatColor.translateAlternateColorCodes('&', pfx) + levelPart;
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
            }
        }
    }

    /**
     * UltraSkyWars-style definitions: each row has {@code level}, {@code xp}, optional {@code prefix} / {@code name},
     * and {@code rewards}.
     */
    private boolean tryLoadExplicitUswStyleLevels(ConfigurationSection section) {
        if (section == null) {
            return false;
        }
        Map<Integer, Integer> xpThreshold = new TreeMap<>();
        Map<Integer, String> prefixMap = new TreeMap<>();
        Map<Integer, List<String>> rewardMap = new TreeMap<>();
        boolean any = false;
        for (String key : section.getKeys(false)) {
            if (!section.isConfigurationSection(key)) {
                continue;
            }
            ConfigurationSection node = section.getConfigurationSection(key);
            if (node == null || !node.contains("level") || !node.contains("xp")) {
                continue;
            }
            any = true;
            int levelNum = Math.max(1, node.getInt("level"));
            int minXp = Math.max(0, node.getInt("xp"));
            xpThreshold.put(levelNum, minXp);
            String pref = node.contains("prefix") ? node.getString("prefix", "") : node.getString("name", "&7[{number}✩] ");
            prefixMap.put(levelNum, pref != null ? pref : "&7[{number}✩] ");
            List<String> rewards = node.getStringList("rewards");
            if (!rewards.isEmpty()) {
                rewardMap.put(levelNum, new ArrayList<>(rewards));
            }
        }
        if (!any) {
            return false;
        }
        TreeMap<Integer, Integer> sorted = new TreeMap<>(xpThreshold);
        int maxLevel = sorted.lastKey();
        int carried = 0;
        for (int lvl = 1; lvl <= maxLevel; lvl++) {
            if (sorted.containsKey(lvl)) {
                carried = sorted.get(lvl);
            }
            xpByLevel.put(lvl, carried);
            prefixByLevel.put(lvl, prefixMap.getOrDefault(lvl, "&7[{number}✩] "));
            List<String> rw = rewardMap.get(lvl);
            if (rw != null && !rw.isEmpty()) {
                levelRewards.put(lvl, new ArrayList<>(rw));
            }
        }
        return true;
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
            if (node == null || node.contains("xp")) {
                // Explicit USW rows handled elsewhere.
                continue;
            }
            int rankupCost = Math.max(1, node.getInt("rankup-cost", 1000));
            String name = node.getString("name", "&7[{number}✩] ");
            List<String> rewardRows = node.getStringList("rewards");
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
                if (!rewardRows.isEmpty()) {
                    levelRewards.put(lvl, new ArrayList<>(rewardRows));
                }
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
