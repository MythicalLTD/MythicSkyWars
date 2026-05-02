package systems.mythical.mythicskywars.utilities;

import com.google.common.collect.Maps;
import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.config.ConfigMerge;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class Messaging {
    private static final Pattern COLOR_PATTERN = Pattern.compile("(?i)([&§])[0-9A-FK-OR]");
    private final FileConfiguration storage;

    public Messaging(Plugin plugin) {
        File storageFile = new File(plugin.getDataFolder(), "messages.yml");

        if (!storageFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        if (storageFile.exists()) {
            copyDefaults(storageFile);
        }
        storage = YamlConfiguration.loadConfiguration(storageFile);
    }

    public static String stripColor(String input) {
        if (input == null) {
            return "";
        }

        return COLOR_PATTERN.matcher(input).replaceAll("");
    }

    public FileConfiguration getFile() {
        return storage;
    }

    private String getPrefix() {
        return storage.getString("prefix", "");
    }

    public String getMessage(String format) {
        if (storage.contains(format)) {
            return storage.getString(format);
        }
        return null;
    }

    private void copyDefaults(File playerFile) {
        try {
            FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
            int added = ConfigMerge.mergeMissingKeysFromResource(MythicSkywars.get(), "messages.yml", playerConfig);
            if (added > 0) {
                playerConfig.save(playerFile);
                MythicSkywars.get().getLogger().info("Merged " + added + " new message key(s) from messages.yml.");
            }
        } catch (IOException e) {
            MythicSkywars.get().getLogger().warning("Failed to merge messages.yml defaults: " + e.getMessage());
        }
    }

    public static class MessageFormatter {
        private static final Pattern PATTERN = Pattern.compile("(?i)(\\{[a-z0-9_]+})");
        private final Map<String, String> variableMap = Maps.newHashMap();
        private boolean prefix;

        public MessageFormatter() {
        }

        public MessageFormatter withPrefix() {
            prefix = true;
            return this;
        }

        public MessageFormatter setVariable(String format, String value) {
            if ((format != null) && (!format.isEmpty())) {
                if (value == null) {
                    variableMap.remove(format);
                } else {
                    variableMap.put(format, value);
                }
            }
            return this;
        }

        public void send(Player player, String message) {
            String formatted = format(message, true, player);
            player.sendMessage(formatted);
        }

        public String format(String message, boolean placeholders, Player player) {
            String format = format(message);
            if (placeholders && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                return PlaceholderAPI.setPlaceholders(player, format);
            }
            return format;
        }

        public String format(String message) {
            if ((message == null) || (message.isEmpty())) {
                return "";
            }

            if (MythicSkywars.getMessaging().getMessage(message) != null) {
                message = MythicSkywars.getMessaging().getMessage(message);
            }

            if (message == null) {
                return "";
            }

            Matcher matcher = PATTERN.matcher(message);

            while (matcher.find()) {
                String variable = matcher.group();
                variable = variable.substring(1, variable.length() - 1);

                String value = (String) variableMap.get(variable);
                if (value == null) {
                    value = "";
                }

                message = message.replaceFirst(Pattern.quote(matcher.group()), Matcher.quoteReplacement(value));
            }

            if (prefix) {
                message = MythicSkywars.getMessaging().getPrefix() + message;
            }

            return ChatColor.translateAlternateColorCodes('&', message);
        }
    }
}
