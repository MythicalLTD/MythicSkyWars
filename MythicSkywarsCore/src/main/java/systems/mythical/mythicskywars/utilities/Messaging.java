package systems.mythical.mythicskywars.utilities;

import com.google.common.collect.Maps;
import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.config.ConfigMerge;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
    private static final Pattern HEX_PATTERN = Pattern.compile("(?i)[&§]#([0-9A-F]{6})");
    // Detects MiniMessage tags like <red>, <bold>, <#FF5555>, <gradient:red:blue>, <reset>, etc.
    private static final Pattern MINIMESSAGE_PATTERN = Pattern.compile("<(/?[a-zA-Z_#][a-zA-Z0-9_:#.\\-]*)>");

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

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
            } else if (message.contains(".") && !message.contains(" ")) {
                // Looks like an unresolved message key — log it once
                MythicSkywars.get().getLogger().warning("[Messages] Missing translation key: " + message);
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

            return colorize(message);
        }
    }

    /**
     * Auto-detects the color format and applies the appropriate colorization.
     * <ul>
     *   <li>If the message contains MiniMessage tags (e.g. {@code <red>}, {@code <bold>}, {@code <#FF5555>}),
     *       it is parsed as MiniMessage and serialized to legacy format.</li>
     *   <li>Otherwise, legacy {@code &} color codes and {@code &#RRGGBB} hex codes are translated.</li>
     * </ul>
     */
    private static String colorize(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        // Auto-detect: if it contains MiniMessage-style tags, parse as MiniMessage
        if (isMiniMessage(message)) {
            try {
                Component component = MINI_MESSAGE.deserialize(message);
                return LEGACY_SERIALIZER.serialize(component);
            } catch (Exception e) {
                // If MiniMessage parsing fails, fall through to legacy
            }
        }

        // Legacy format: translate & codes and &#hex codes
        return translateHexColors(ChatColor.translateAlternateColorCodes('&', message));
    }

    /**
     * Detects whether a message uses MiniMessage format.
     * Returns true if the message contains recognized MiniMessage tags.
     */
    private static boolean isMiniMessage(String message) {
        return MINIMESSAGE_PATTERN.matcher(message).find();
    }

    /**
     * Translates hex color codes in the format {@code &#RRGGBB} or {@code §#RRGGBB}
     * to the Minecraft-compatible {@code §x§R§R§G§G§B§B} format.
     * Only applies on servers that support RGB colors (1.16+).
     */
    private static String translateHexColors(String message) {
        if (message == null || MythicSkywars.getNMS() == null || MythicSkywars.getNMS().getVersion() < 16) {
            return message;
        }
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(sb, replacement.toString());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
