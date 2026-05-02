package systems.mythical.mythicskywars.perks;

/**
 * Types of perks supported by the system.
 */
public enum PerkType {
    /** Reduces incoming damage of a specific type by a percentage. */
    DAMAGE_REDUCTION,
    /** Grants a potion effect on kill. */
    KILL_EFFECT,
    /** Grants a potion effect when the game starts. */
    GAME_START,
    /** Chance to recover arrows on hit. */
    ARROW_RECOVERY,
    /** Chance to set arrows on fire when shot. */
    BLAZING_ARROWS,
    /** Restores hunger on kill. */
    NOURISHMENT,
    /** Reduces ender pearl damage. */
    ENDER_MASTERY,
    /** Chance to get extra ores when mining. */
    MINING_EXPERTISE,
    /** Grants XP levels on kill. */
    KNOWLEDGE,
    /** Chance to spawn silverfish on arrow hit. */
    ANNOYOMITE
}
