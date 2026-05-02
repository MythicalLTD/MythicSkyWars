package systems.mythical.mythicskywars.utilities;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.managers.PlayerStat;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Prestige selection configured under {@code prestige:} inside {@code levels.yml}.
 */
public final class PrestigeManager {

    private static final PrestigeManager INSTANCE = new PrestigeManager();
    private final Map<String, PrestigeEntry> entries = new LinkedHashMap<>();
    private boolean enabled;
    private int optionsMenuSlot = -1;

    private PrestigeManager() {
    }

    public static PrestigeManager get() {
        return INSTANCE;
    }

    public synchronized void load(Plugin plugin) {
        entries.clear();
        enabled = false;
        optionsMenuSlot = -1;
        File levelFile = new File(plugin.getDataFolder(), "levels.yml");
        if (!levelFile.exists()) {
            return;
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(levelFile);
        ConfigurationSection root = cfg.getConfigurationSection("prestige");
        if (root == null) {
            return;
        }
        enabled = root.getBoolean("enabled", false);
        optionsMenuSlot = root.getInt("options-menu-slot", -1);
        if (!enabled) {
            return;
        }
        for (String key : root.getKeys(false)) {
            if ("enabled".equalsIgnoreCase(key)
                    || "options-menu-slot".equalsIgnoreCase(key)
                    || "menu-size".equalsIgnoreCase(key)) {
                continue;
            }
            if (!root.isConfigurationSection(key)) {
                continue;
            }
            ConfigurationSection node = root.getConfigurationSection(key);
            if (node == null) {
                continue;
            }
            PrestigeEntry e = PrestigeEntry.parse(key, node);
            if (e.id != null) {
                entries.put(e.id.toLowerCase(Locale.ROOT), e);
            }
        }
    }

    public synchronized boolean isEnabled() {
        return enabled && !entries.isEmpty();
    }

    public synchronized int getOptionsMenuSlot() {
        return optionsMenuSlot;
    }

    public synchronized int getMenuSlots(Plugin plugin) {
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "levels.yml"));
        ConfigurationSection root = cfg.getConfigurationSection("prestige");
        int size = root != null ? root.getInt("menu-size", 36) : 36;
        size = (size / 9) * 9;
        if (size < 27) {
            size = 27;
        }
        if (size > 54) {
            size = 54;
        }
        return size;
    }

    public synchronized PrestigeEntry getEntry(String id) {
        if (id == null) {
            return null;
        }
        return entries.get(id.trim().toLowerCase(Locale.ROOT));
    }

    public synchronized PrestigeEntry defaultEntryOrNull() {
        for (PrestigeEntry def : entries.values()) {
            return def;
        }
        return null;
    }

    public synchronized List<PrestigeEntry> getAllEntries() {
        return Collections.unmodifiableList(new ArrayList<>(entries.values()));
    }

    public synchronized String translatePrefixForStat(PlayerStat ps, int numericLevel, String prestigeId) {
        if (!isEnabled()) {
            return "";
        }
        PrestigeEntry e = getEntry(prestigeId);
        if (e == null) {
            PrestigeEntry d = defaultEntryOrNull();
            e = d;
        }
        if (e == null || e.prefix == null || e.prefix.isEmpty()) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&',
                substitutePrefixTokens(ps, numericLevel, e.prefix));
    }

    private static String substitutePrefixTokens(PlayerStat ps, int numericLevel, String prefix) {
        String nameTok = "";
        if (ps != null) {
            try {
                Player online = MythicSkywars.get().getServer().getPlayer(java.util.UUID.fromString(ps.getId()));
                nameTok = online != null ? online.getName() : (ps.getPlayerName() != null ? ps.getPlayerName() : "");
            } catch (Exception ex) {
                nameTok = ps.getPlayerName() != null ? ps.getPlayerName() : "";
            }
        }
        return prefix
                .replace("<level>", Integer.toString(numericLevel))
                .replace("{level}", Integer.toString(numericLevel))
                .replace("<name>", nameTok != null ? nameTok : "")
                .replace("{name}", nameTok != null ? nameTok : "");
    }

    public synchronized boolean meetsRequirements(Player player, PlayerStat ps, PrestigeEntry def) {
        if (player == null || ps == null || def == null) {
            return false;
        }
        if (LevelManager.get().getLevelForXp(ps.getXp()) < def.minLevel) {
            return false;
        }
        if (ps.getKills() < def.minAngelDeath) {
            return false;
        }
        for (String need : def.requiredPermissions) {
            if (need == null || need.trim().isEmpty()) {
                continue;
            }
            if (!player.hasPermission(need.trim())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Colored lore lines for prestige GUI (placeholders resolved from player / stat context).
     */
    public synchronized List<String> loreLines(Player player, PlayerStat ps, PrestigeEntry target, PrestigeEntry selected, int levelNum) {
        List<String> out = new ArrayList<>();
        if (target == null) {
            return out;
        }
        boolean reqs = meetsRequirements(player, ps, target);
        boolean isSelected = selected != null && selected.id.equalsIgnoreCase(target.id);
        String status = loreStatusLine(isSelected, reqs);
        for (String line : target.loreTemplate.split("\\r?\\n")) {
            out.add(ChatColor.translateAlternateColorCodes('&', substituteLoreTokens(line,
                    player, ps, target, selected, levelNum, status)));
        }
        return out;
    }

    private String loreStatusLine(boolean selected, boolean unlocked) {
        String key;
        if (selected) {
            key = "levels.prestige-status-selected";
        } else if (unlocked) {
            key = "levels.prestige-status-unlocked";
        } else {
            key = "levels.prestige-status-locked";
        }
        String raw = null;
        if (MythicSkywars.get() != null && MythicSkywars.getMessaging() != null) {
            raw = MythicSkywars.getMessaging().getMessage(key);
        }
        if (raw == null || raw.trim().isEmpty()) {
            if (selected) {
                return "&a&lEquipped";
            }
            return unlocked ? "&aUnlockable — click to equip" : "&cLocked — see requirements";
        }
        return raw;
    }

    private static String substituteLoreTokens(String line, Player player, PlayerStat ps, PrestigeEntry target,
                                                PrestigeEntry selected, int levelNum, String statusColored) {
        String nameTok = player != null ? player.getName() : (ps != null && ps.getPlayerName() != null ? ps.getPlayerName() : "");
        String tierPrefixTok = ChatColor.translateAlternateColorCodes('&', target.prefix != null ? target.prefix : "");
        return line
                .replace("<status>", ChatColor.translateAlternateColorCodes('&', statusColored))
                .replace("<level>", Integer.toString(levelNum))
                .replace("<level><icon>", levelNum + tierPrefixTok)
                .replace("<icon>", tierPrefixTok)
                .replace("<name>", nameTok != null ? nameTok : "")
                .replace("{level}", Integer.toString(levelNum))
                .replace("{icon}", tierPrefixTok)
                .replace("{name}", nameTok != null ? nameTok : "")
                .replace("{status}", ChatColor.translateAlternateColorCodes('&', statusColored));
    }

    public static final class PrestigeEntry {
        public final String yamlKey;
        public final String id;
        /** Raw prefix (&-colour codes ok). */
        public final String prefix;
        public final String displayName;
        public final String loreTemplate;
        public final Material material;
        public final int slot;
        public final int page;
        public final int minLevel;
        /** Requirement treated as SkyWars {@link PlayerStat#getKills()} (“angel of death”). */
        public final int minAngelDeath;
        public final List<String> requiredPermissions;

        PrestigeEntry(String yamlKey, String id, String prefix, String displayName, String loreTemplate,
                      Material material, int slot, int page, int minLevel, int minAngelDeath,
                      List<String> requiredPermissions) {
            this.yamlKey = yamlKey;
            this.id = id;
            this.prefix = prefix != null ? prefix : "";
            this.displayName = displayName != null ? displayName : id;
            this.loreTemplate = loreTemplate != null ? loreTemplate : "";
            this.material = material != null ? material : Material.NETHER_STAR;
            this.slot = slot;
            this.page = Math.max(1, page);
            this.minLevel = Math.max(0, minLevel);
            this.minAngelDeath = Math.max(0, minAngelDeath);
            this.requiredPermissions = requiredPermissions == null ? Collections.emptyList() : requiredPermissions;
        }

        static PrestigeEntry parse(String yamlKey, ConfigurationSection node) {
            String id = node.getString("id", yamlKey).trim();
            String prefixStr = node.getString("prefix", "");
            String name = node.getString("name", id);
            String lore = readLoreBlock(node);

            Material material = resolveMaterial(node.getString("material", "NETHER_STAR"));
            int slot = node.getInt("slot", -1);
            int page = node.getInt("page", 1);

            int minLevel = 0;
            int minAngel = 0;
            List<String> permsNeeded = new ArrayList<>();

            ConfigurationSection req = node.getConfigurationSection("requirements");
            if (req != null) {
                minLevel = Math.max(0, req.getInt("level", 0));
                minAngel = Math.max(0, req.getInt("angelDeath", req.getInt("angel-deaths", 0)));
                harvestPerms(req, permsNeeded);
            } else {
                minLevel = Math.max(0, node.getInt("min-level", minLevel));
                minAngel = Math.max(0, node.getInt("angelDeath", node.getInt("angel-deaths", minAngel)));
                harvestPerms(node, permsNeeded);
            }

            return new PrestigeEntry(yamlKey, id, prefixStr, name, lore, material, slot, page, minLevel, minAngel, dedupe(permsNeeded));
        }

        private static void harvestPerms(ConfigurationSection sec, List<String> sink) {
            List<String> list = sec.getStringList("perms");
            if (!list.isEmpty()) {
                for (String s : list) {
                    maybeAddPerm(sink, s);
                }
            } else {
                String plist = sec.getString("perms", null);
                if (plist != null && !plist.equalsIgnoreCase("none")) {
                    for (String chunk : plist.split(",")) {
                        maybeAddPerm(sink, chunk);
                    }
                }
            }
            maybeAddPerm(sink, sec.getString("perm", null));
        }

        private static void maybeAddPerm(List<String> sink, String candidate) {
            if (candidate == null) {
                return;
            }
            String p = candidate.trim();
            if (p.isEmpty() || "none".equalsIgnoreCase(p)) {
                return;
            }
            if (!sink.contains(p)) {
                sink.add(p);
            }
        }

        private static List<String> dedupe(List<String> in) {
            List<String> o = new ArrayList<>();
            for (String p : in) {
                maybeAddPerm(o, p);
            }
            return o;
        }

        private static String readLoreBlock(ConfigurationSection node) {
            Object loreObj = node.get("lore");
            if (loreObj instanceof List) {
                StringBuilder lb = new StringBuilder();
                @SuppressWarnings("unchecked")
                List<String> loreList = (List<String>) loreObj;
                for (String ln : loreList) {
                    if (lb.length() > 0) {
                        lb.append('\n');
                    }
                    lb.append(ln != null ? ln : "");
                }
                return lb.toString();
            }
            return node.getString("lore", "");
        }

        private static Material resolveMaterial(String raw) {
            if (raw == null || raw.trim().isEmpty()) {
                return Material.NETHER_STAR;
            }
            String u = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
            Material m = Material.matchMaterial(raw.trim());
            if (m != null) {
                return m;
            }
            m = Material.getMaterial(u);
            if (m != null) {
                return m;
            }
            try {
                return Material.valueOf(u);
            } catch (Exception ignored) {
                return Material.NETHER_STAR;
            }
        }
    }
}
