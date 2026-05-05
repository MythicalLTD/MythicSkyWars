package systems.mythical.mythicskywars.matchevents;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.game.GameMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.lang.reflect.Field;

public final class GameEventsConfig {

    private static FileConfiguration cfg;

    private GameEventsConfig() {
    }

    public static synchronized void load(Plugin plugin) {
        File file = new File(plugin.getDataFolder(), "gameevents.yml");
        if (!file.exists()) {
            plugin.saveResource("gameevents.yml", false);
        }
        cfg = YamlConfiguration.loadConfiguration(file);
    }

    public static synchronized boolean resolveEnabled(GameMap map, String eventName, boolean fallback) {
        ConfigurationSection node = resolveNode(map, eventName);
        if (node == null) {
            return fallback;
        }
        return node.getBoolean("enabled", fallback);
    }

    public static synchronized void apply(GameMap map, MatchEvent event) {
        ConfigurationSection node = resolveNode(map, event.getEventName());
        if (node == null) {
            return;
        }
        event.setEnabled(node.getBoolean("enabled", event.isEnabled()));
        event.setMin(node.getInt("minStart", event.getMin()));
        event.setMax(node.getInt("maxStart", event.getMax()));
        event.setChance(node.getInt("chance", event.getChance()));
        event.setAnnounceEvent(node.getBoolean("announceTimer", event.isAnnounceEvent()));
        event.setRepeatable(node.getBoolean("repeatable", event.isRepeatable()));
        if (node.contains("title")) {
            event.setTitle(node.getString("title", event.getTitle()));
        }
        if (node.contains("subtitle")) {
            event.setSubtitle(node.getString("subtitle", event.getSubtitle()));
        }
        if (node.contains("startMessage")) {
            event.setStartMessage(node.getString("startMessage", event.getStartMessage()));
        }
        if (node.contains("endMessage")) {
            event.setEndMessage(node.getString("endMessage", event.getEndMessage()));
        }
        applyIntField(event, node, "spawnPer2Tick", "per2Tick");
        applyIntField(event, node, "spawnPer5Tick", "per5Tick");
        // Caller must invoke MatchEvent.reset() after apply so chance (useThisMatch) is rolled — not only start time.
    }

    private static ConfigurationSection resolveNode(GameMap map, String eventName) {
        if (cfg == null) {
            load(MythicSkywars.get());
        }
        String type = map.getTeamSize() > 1 ? "team" : "solo";
        ConfigurationSection typed = cfg.getConfigurationSection("events." + type + "." + eventName);
        if (typed != null) {
            return typed;
        }
        return cfg.getConfigurationSection("events.all." + eventName);
    }

    private static void applyIntField(MatchEvent event, ConfigurationSection node, String cfgKey, String fieldName) {
        if (node == null || !node.contains(cfgKey)) {
            return;
        }
        try {
            Field f = event.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.setInt(event, node.getInt(cfgKey));
        } catch (Exception ignored) {
            // Field not present on this event type.
        }
    }
}
