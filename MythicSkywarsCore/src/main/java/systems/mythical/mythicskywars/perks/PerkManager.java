package systems.mythical.mythicskywars.perks;

import systems.mythical.mythicskywars.MythicSkywars;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

/**
 * Loads and manages the perks.yml configuration.
 * Perks are passive abilities that players unlock via Soul Well or coins.
 */
public final class PerkManager {

    private static PerkManager instance;
    private final Map<String, Perk> perks = new LinkedHashMap<>();
    private boolean enabled;
    private int menuSize;
    private String menuTitle;

    private PerkManager() {
    }

    public static PerkManager get() {
        if (instance == null) {
            instance = new PerkManager();
        }
        return instance;
    }

    public void reload() {
        perks.clear();
        File file = new File(MythicSkywars.get().getDataFolder(), "perks.yml");
        if (!file.exists()) {
            MythicSkywars.get().saveResource("perks.yml", false);
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        enabled = cfg.getBoolean("enabled", true);
        menuSize = cfg.getInt("menuSize", 45);
        menuTitle = cfg.getString("menuTitle", "&5&lSkyWars Perks");

        Logger log = MythicSkywars.get().getLogger();
        ConfigurationSection perksSection = cfg.getConfigurationSection("perks");
        if (perksSection == null) {
            log.warning("[Perks] No 'perks' section found in perks.yml");
            return;
        }

        for (String key : perksSection.getKeys(false)) {
            ConfigurationSection ps = perksSection.getConfigurationSection(key);
            if (ps == null) continue;

            String name = ps.getString("name", key);
            List<String> lore = ps.getStringList("lore");
            String iconStr = ps.getString("icon", "NETHER_STAR");
            int slot = ps.getInt("slot", 0);
            String typeStr = ps.getString("type", "DAMAGE_REDUCTION");
            boolean disabled = ps.getBoolean("disabled", false);
            List<String> gameTypes = ps.getStringList("gameTypes");

            // Optional fields for specific perk types
            String damageType = ps.getString("damageType", null);
            String effect = ps.getString("effect", null);
            int amplifier = ps.getInt("amplifier", 0);
            int defaultDuration = ps.getInt("duration", 0);

            Material icon = Material.matchMaterial(iconStr);
            if (icon == null) {
                icon = Material.NETHER_STAR;
                log.warning("[Perks] Invalid icon material '" + iconStr + "' for perk '" + key + "', using NETHER_STAR");
            }

            PerkType type;
            try {
                type = PerkType.valueOf(typeStr.toUpperCase(Locale.ENGLISH));
            } catch (IllegalArgumentException e) {
                log.warning("[Perks] Invalid perk type '" + typeStr + "' for perk '" + key + "', skipping");
                continue;
            }

            // Load levels
            ConfigurationSection levelsSection = ps.getConfigurationSection("levels");
            List<PerkLevel> levels = new ArrayList<>();
            if (levelsSection != null) {
                List<String> levelKeys = new ArrayList<>(levelsSection.getKeys(false));
                // Sort numerically
                levelKeys.sort(Comparator.comparingInt(k -> {
                    try { return Integer.parseInt(k); } catch (NumberFormatException e) { return 0; }
                }));
                for (String lk : levelKeys) {
                    ConfigurationSection ls = levelsSection.getConfigurationSection(lk);
                    if (ls == null) continue;
                    int levelNum;
                    try { levelNum = Integer.parseInt(lk); } catch (NumberFormatException e) { levelNum = levels.size() + 1; }
                    int percent = ls.getInt("percent", 0);
                    int cost = ls.getInt("cost", 0);
                    int dur = ls.getInt("duration", defaultDuration);
                    levels.add(new PerkLevel(levelNum, percent, cost, dur));
                }
            }

            if (levels.isEmpty()) {
                log.warning("[Perks] Perk '" + key + "' has no levels defined, skipping");
                continue;
            }

            Perk perk = new Perk(key, name, lore, icon, slot, type, disabled, gameTypes,
                    damageType, effect, amplifier, defaultDuration, levels);
            perks.put(key, perk);
        }

        log.info("[Perks] Loaded " + perks.size() + " perks from perks.yml");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMenuSize() {
        return menuSize;
    }

    public String getMenuTitle() {
        return menuTitle;
    }

    public Perk getPerk(String key) {
        return perks.get(key);
    }

    public Collection<Perk> getAllPerks() {
        return Collections.unmodifiableCollection(perks.values());
    }

    public List<String> getPerkKeys() {
        return new ArrayList<>(perks.keySet());
    }
}
