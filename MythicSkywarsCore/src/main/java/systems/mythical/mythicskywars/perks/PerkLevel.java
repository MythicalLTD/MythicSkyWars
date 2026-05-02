package systems.mythical.mythicskywars.perks;

/**
 * Represents a single upgrade level of a perk.
 */
public final class PerkLevel {
    private final int level;
    private final int percent;
    private final int cost;
    private final int duration;

    public PerkLevel(int level, int percent, int cost, int duration) {
        this.level = level;
        this.percent = percent;
        this.cost = cost;
        this.duration = duration;
    }

    public int getLevel() {
        return level;
    }

    /** The percentage value for this level (damage reduction %, probability %, etc.) */
    public int getPercent() {
        return percent;
    }

    /** Cost in coins to unlock/upgrade to this level. */
    public int getCost() {
        return cost;
    }

    /** Duration in seconds (for potion effects). */
    public int getDuration() {
        return duration;
    }
}
