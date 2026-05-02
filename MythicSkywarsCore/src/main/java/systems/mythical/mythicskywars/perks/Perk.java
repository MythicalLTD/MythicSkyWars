package systems.mythical.mythicskywars.perks;

import org.bukkit.Material;

import java.util.Collections;
import java.util.List;

/**
 * Represents a perk definition loaded from perks.yml.
 */
public final class Perk {
    private final String key;
    private final String name;
    private final List<String> lore;
    private final Material icon;
    private final int slot;
    private final PerkType type;
    private final boolean disabled;
    private final List<String> gameTypes;
    private final String damageType;
    private final String effect;
    private final int amplifier;
    private final int defaultDuration;
    private final List<PerkLevel> levels;

    public Perk(String key, String name, List<String> lore, Material icon, int slot,
                PerkType type, boolean disabled, List<String> gameTypes,
                String damageType, String effect, int amplifier, int defaultDuration,
                List<PerkLevel> levels) {
        this.key = key;
        this.name = name;
        this.lore = lore;
        this.icon = icon;
        this.slot = slot;
        this.type = type;
        this.disabled = disabled;
        this.gameTypes = gameTypes;
        this.damageType = damageType;
        this.effect = effect;
        this.amplifier = amplifier;
        this.defaultDuration = defaultDuration;
        this.levels = levels;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return lore;
    }

    public Material getIcon() {
        return icon;
    }

    public int getSlot() {
        return slot;
    }

    public PerkType getType() {
        return type;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public List<String> getGameTypes() {
        return gameTypes;
    }

    /** For DAMAGE_REDUCTION: FALL, FIRE, PROJECTILE. For ENDER_MASTERY: null. */
    public String getDamageType() {
        return damageType;
    }

    /** For KILL_EFFECT / GAME_START: the PotionEffectType name. */
    public String getEffect() {
        return effect;
    }

    public int getAmplifier() {
        return amplifier;
    }

    public int getDefaultDuration() {
        return defaultDuration;
    }

    public List<PerkLevel> getLevels() {
        return Collections.unmodifiableList(levels);
    }

    public int getMaxLevel() {
        return levels.size();
    }

    public PerkLevel getLevel(int level) {
        if (level < 1 || level > levels.size()) return null;
        return levels.get(level - 1);
    }

    /** Permission node for a specific level: sw.perk.{key}.{level} */
    public String getPermission(int level) {
        return "sw.perk." + key + "." + level;
    }

    /** Base permission node: sw.perk.{key} (used for Soul Well allow/deny lists) */
    public String getBasePermission() {
        return "sw.perk." + key;
    }
}
