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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class Messaging {
    private static final Pattern COLOR_PATTERN = Pattern.compile("(?i)([&\\u00A7])[0-9A-FK-OR]");
    private static final Pattern HEX_PATTERN = Pattern.compile("(?i)[&\\u00A7]#([0-9A-F]{6})");
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

        FileConfiguration loaded = YamlConfiguration.loadConfiguration(storageFile);
        if (storageFile.exists()) {
            applyBundledDefaultsInMemoryOnly(loaded);
        }
        storage = loaded;
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

    public String getMessage(String format) {
        if (storage.contains(format)) {
            return storage.getString(format);
        }
        return null;
    }

    /**
     * Primary unified chat prefix ({@code &} codes). Set {@code message-prefix} in {@code messages.yml};
     * if unset, falls back to {@code game.broadcast-prefix} for compatibility.
     */
    public String getUniversalMessagePrefixRaw() {
        String p = storage.getString("message-prefix");
        if (p != null && !p.isEmpty()) {
            return p;
        }
        p = storage.getString("game.broadcast-prefix");
        return p != null ? p : "";
    }

    /** @deprecated use {@link #getUniversalMessagePrefixRaw()} */
    @Deprecated
    public String getGameBroadcastPrefixRaw() {
        return getUniversalMessagePrefixRaw();
    }

    /**
     * Keys passed as the first argument to {@link MessageFormatter#format(String)} that must not get the
     * universal prefix (inventory titles, item names, hologram snippets, titles, scoreboards, sign lines, etc.).
     */
    public boolean isUniversalPrefixExcludedForKey(String lookupKey) {
        if (lookupKey == null || lookupKey.isEmpty()) {
            return true;
        }
        if ("chat.externalPrefix".equals(lookupKey)) {
            return true;
        }
        if (lookupKey.endsWith("-actionbar")) {
            return true;
        }
        if (lookupKey.startsWith("game.select-team-before")) {
            return true;
        }
        List<String> extra = storage.getStringList("message-prefix-exclude-prefixes");
        for (String p : getBuiltInExcludedPrefixes()) {
            if (matchesPrefixRule(lookupKey, p)) {
                return true;
            }
        }
        if (extra != null) {
            for (String p : extra) {
                if (p != null && !p.isEmpty() && matchesPrefixRule(lookupKey, p.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<String> getBuiltInExcludedPrefixes() {
        return Collections.unmodifiableList(Arrays.asList(
                "items.",
                "menu.",
                "maps.editor.item.",
                "maps.editor.hologram.",
                "titles.",
                "scoreboards.",
                "signs.line",
                "lunar.",
                "kit.",
                "soulwell.holo-",
                "game.chest-refill-hologram."
        ));
    }

    private static boolean matchesPrefixRule(String key, String rule) {
        if (rule.endsWith("*")) {
            return key.startsWith(rule.substring(0, rule.length() - 1));
        }
        return key.startsWith(rule) || key.equals(rule);
    }

    /**
     * Picks a random template line from a string list in config, or falls back to a single message key.
     * List key should reference a {@link List} of strings (e.g. {@code timer.wait-timer-variants}).
     */
    public String pickGameLineTemplate(String variantsListKey, String fallbackMessageKey) {
        List<String> variants = storage.getStringList(variantsListKey);
        if (variants != null && !variants.isEmpty()) {
            return variants.get(ThreadLocalRandom.current().nextInt(variants.size()));
        }
        return storage.getString(fallbackMessageKey, "");
    }

    /**
     * Picks a random line from a YAML string list and runs it through {@link MessageFormatter#format(String)}.
     * Each list entry may be a message key or a raw template with {@code {placeholders}}.
     */
    public String formatRandomListLine(String listKey, MessageFormatter formatter) {
        List<String> lines = storage.getStringList(listKey);
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        String tpl = lines.get(ThreadLocalRandom.current().nextInt(lines.size()));
        if (tpl == null || tpl.isEmpty()) {
            return "";
        }
        return formatter.format(tpl.trim());
    }

    /**
     * Fills in any keys present in the jar's {@code messages.yml} but missing from disk, only in memory.
     * The on-disk file is never rewritten here, so reload/restart does not reformat or clobber user edits.
     */
    private static void applyBundledDefaultsInMemoryOnly(FileConfiguration playerConfig) {
        int added = ConfigMerge.mergeMissingKeysFromResource(MythicSkywars.get(), "messages.yml", playerConfig);
        if (added > 0) {
            MythicSkywars.get().getLogger().info("Loaded " + added + " default message key(s) from the jar "
                    + "(missing from messages.yml on disk; disk file was not modified).");
        }
    }

    public static class MessageFormatter {
        private static final Pattern PATTERN = Pattern.compile("(?i)(\\{[a-z0-9_]+})");
        private final Map<String, String> variableMap = Maps.newHashMap();
        /** When true, universal prefix is applied for keys not on the exclusion list (default: true). */
        private boolean universalPrefixEnabled = true;
        /** When true, universal prefix is applied even for excluded keys (e.g. a {@code menu.*} line sent as chat). */
        private boolean forceUniversalPrefix = false;

        public MessageFormatter() {
        }

        /** Disables the global {@code message-prefix} for this format chain (raw / UI strings). */
        public MessageFormatter withoutUniversalPrefix() {
            this.universalPrefixEnabled = false;
            return this;
        }

        /** Forces {@code message-prefix} even for keys that are normally excluded (inventory/menu keys used in chat). */
        public MessageFormatter withUniversalPrefixForced() {
            this.forceUniversalPrefix = true;
            return this;
        }

        /** @deprecated Legacy hook; universal prefix is used instead. Kept for API compatibility (no-op). */
        @Deprecated
        public MessageFormatter withPrefix() {
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

            final String lookupKey = message;

            if (MythicSkywars.getMessaging().getMessage(message) != null) {
                message = MythicSkywars.getMessaging().getMessage(message);
            } else if (message.contains(".") && !message.contains(" ")) {
                // Looks like an unresolved message key; log it once.
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

            Messaging messaging = MythicSkywars.getMessaging();
            if (messaging != null && universalPrefixEnabled) {
                boolean excluded = messaging.isUniversalPrefixExcludedForKey(lookupKey);
                if (forceUniversalPrefix || !excluded) {
                    String p = messaging.getUniversalMessagePrefixRaw();
                    if (p != null && !p.isEmpty()) {
                        message = p + message;
                    }
                }
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
     * Translates hex color codes in the format {@code &#RRGGBB} or {@code \u00A7#RRGGBB}
     * to the Minecraft-compatible {@code \u00A7x\u00A7R\u00A7R\u00A7G\u00A7G\u00A7B\u00A7B} format.
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
            StringBuilder replacement = new StringBuilder("\u00A7x");
            for (char c : hex.toCharArray()) {
                replacement.append('\u00A7').append(c);
            }
            matcher.appendReplacement(sb, replacement.toString());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
