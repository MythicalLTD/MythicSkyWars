package systems.mythical.mythicskywars.perks;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.enums.MatchState;
import systems.mythical.mythicskywars.events.MythicSkywarsKillEvent;
import systems.mythical.mythicskywars.events.MythicSkywarsMatchStateChangeEvent;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.managers.MatchManager;
import systems.mythical.mythicskywars.managers.PlayerStat;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Applies perk effects during SkyWars matches.
 */
public class PerkListener implements Listener {

    private static final Set<Material> ORES = new HashSet<>();

    static {
        for (String name : new String[]{
                "COAL_ORE", "IRON_ORE", "GOLD_ORE", "DIAMOND_ORE", "EMERALD_ORE",
                "LAPIS_ORE", "REDSTONE_ORE", "NETHER_QUARTZ_ORE", "NETHER_GOLD_ORE",
                "DEEPSLATE_COAL_ORE", "DEEPSLATE_IRON_ORE", "DEEPSLATE_GOLD_ORE",
                "DEEPSLATE_DIAMOND_ORE", "DEEPSLATE_EMERALD_ORE", "DEEPSLATE_LAPIS_ORE",
                "DEEPSLATE_REDSTONE_ORE", "COPPER_ORE", "DEEPSLATE_COPPER_ORE"
        }) {
            try {
                ORES.add(Material.valueOf(name));
            } catch (IllegalArgumentException ignored) {
                // Not available on this server version
            }
        }
    }

    /**
     * Gets the player's unlocked level for a perk (highest level they have permission for).
     */
    private int getPlayerPerkLevel(Player player, Perk perk) {
        int highest = 0;
        for (int i = 1; i <= perk.getMaxLevel(); i++) {
            if (player.hasPermission(perk.getPermission(i))) {
                highest = i;
            }
        }
        return highest;
    }

    private boolean isInActiveGame(Player player) {
        GameMap map = MatchManager.get().getPlayerMap(player);
        return map != null && map.getMatchState() == MatchState.PLAYING;
    }

    // ==================== DAMAGE REDUCTION ====================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!PerkManager.get().isEnabled()) return;
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (!isInActiveGame(player)) return;

        EntityDamageEvent.DamageCause cause = event.getCause();

        for (Perk perk : PerkManager.get().getAllPerks()) {
            if (perk.isDisabled() || perk.getType() != PerkType.DAMAGE_REDUCTION) continue;
            if (!matchesDamageCause(perk, cause)) continue;

            int level = getPlayerPerkLevel(player, perk);
            if (level <= 0) continue;

            PerkLevel pl = perk.getLevel(level);
            if (pl == null) continue;

            double reduction = pl.getPercent() / 100.0;
            event.setDamage(event.getDamage() * (1.0 - reduction));
        }

        // Ender Mastery - reduce ender pearl damage
        if (cause == EntityDamageEvent.DamageCause.FALL) {
            // Check if this is ender pearl damage (entity teleport causes fall damage)
            // We handle this in the EntityDamageByEntity for projectile hits instead
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (!PerkManager.get().isEnabled()) return;
        if (!(event.getEntity() instanceof Player)) return;
        Player victim = (Player) event.getEntity();
        if (!isInActiveGame(victim)) return;

        // Ender Mastery - reduce ender pearl self-damage
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL
                && event.getDamager() instanceof EnderPearl) {
            for (Perk perk : PerkManager.get().getAllPerks()) {
                if (perk.isDisabled() || perk.getType() != PerkType.ENDER_MASTERY) continue;
                int level = getPlayerPerkLevel(victim, perk);
                if (level <= 0) continue;
                PerkLevel pl = perk.getLevel(level);
                if (pl == null) continue;
                double reduction = pl.getPercent() / 100.0;
                event.setDamage(event.getDamage() * (1.0 - reduction));
            }
        }

        // Arrow Recovery - check if victim was hit by arrow from a player
        if (event.getDamager() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getDamager();
            if (arrow.getShooter() instanceof Player) {
                Player shooter = (Player) arrow.getShooter();
                if (isInActiveGame(shooter)) {
                    for (Perk perk : PerkManager.get().getAllPerks()) {
                        if (perk.isDisabled() || perk.getType() != PerkType.ARROW_RECOVERY) continue;
                        int level = getPlayerPerkLevel(shooter, perk);
                        if (level <= 0) continue;
                        PerkLevel pl = perk.getLevel(level);
                        if (pl == null) continue;
                        if (ThreadLocalRandom.current().nextInt(100) < pl.getPercent()) {
                            shooter.getInventory().addItem(new ItemStack(Material.ARROW, 1));
                        }
                    }

                    // Anno-o-Mite - spawn silverfish on arrow hit
                    for (Perk perk : PerkManager.get().getAllPerks()) {
                        if (perk.isDisabled() || perk.getType() != PerkType.ANNOYOMITE) continue;
                        int level = getPlayerPerkLevel(shooter, perk);
                        if (level <= 0) continue;
                        PerkLevel pl = perk.getLevel(level);
                        if (pl == null) continue;
                        if (ThreadLocalRandom.current().nextInt(100) < pl.getPercent()) {
                            Location loc = victim.getLocation();
                            victim.getWorld().spawnEntity(loc, EntityType.SILVERFISH);
                        }
                    }
                }
            }
        }
    }

    private boolean matchesDamageCause(Perk perk, EntityDamageEvent.DamageCause cause) {
        String dt = perk.getDamageType();
        if (dt == null) return false;
        switch (dt.toUpperCase()) {
            case "FALL":
                return cause == EntityDamageEvent.DamageCause.FALL;
            case "FIRE":
                return cause == EntityDamageEvent.DamageCause.FIRE
                        || cause == EntityDamageEvent.DamageCause.FIRE_TICK
                        || cause == EntityDamageEvent.DamageCause.LAVA;
            case "PROJECTILE":
                return cause == EntityDamageEvent.DamageCause.PROJECTILE;
            default:
                return false;
        }
    }

    // ==================== BLAZING ARROWS ====================

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBowShoot(EntityShootBowEvent event) {
        if (!PerkManager.get().isEnabled()) return;
        if (!(event.getEntity() instanceof Player)) return;
        Player shooter = (Player) event.getEntity();
        if (!isInActiveGame(shooter)) return;

        for (Perk perk : PerkManager.get().getAllPerks()) {
            if (perk.isDisabled() || perk.getType() != PerkType.BLAZING_ARROWS) continue;
            int level = getPlayerPerkLevel(shooter, perk);
            if (level <= 0) continue;
            PerkLevel pl = perk.getLevel(level);
            if (pl == null) continue;
            if (ThreadLocalRandom.current().nextInt(100) < pl.getPercent()) {
                event.getProjectile().setFireTicks(100);
            }
        }
    }

    // ==================== KILL EFFECTS ====================

    @EventHandler(priority = EventPriority.NORMAL)
    public void onKill(MythicSkywarsKillEvent event) {
        if (!PerkManager.get().isEnabled()) return;
        Player killer = event.getKiller();
        if (killer == null) return;

        for (Perk perk : PerkManager.get().getAllPerks()) {
            if (perk.isDisabled()) continue;
            int level = getPlayerPerkLevel(killer, perk);
            if (level <= 0) continue;
            PerkLevel pl = perk.getLevel(level);
            if (pl == null) continue;

            switch (perk.getType()) {
                case KILL_EFFECT:
                    if (ThreadLocalRandom.current().nextInt(100) < pl.getPercent()) {
                        PotionEffectType effectType = PotionEffectType.getByName(perk.getEffect());
                        if (effectType != null) {
                            int dur = pl.getDuration() > 0 ? pl.getDuration() : perk.getDefaultDuration();
                            killer.addPotionEffect(new PotionEffect(effectType, dur * 20, perk.getAmplifier()), true);
                        }
                    }
                    break;
                case NOURISHMENT:
                    if (ThreadLocalRandom.current().nextInt(100) < pl.getPercent()) {
                        killer.setFoodLevel(20);
                        killer.setSaturation(20f);
                    }
                    break;
                case KNOWLEDGE:
                    if (ThreadLocalRandom.current().nextInt(100) < pl.getPercent()) {
                        killer.giveExpLevels(2);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    // ==================== GAME START EFFECTS ====================

    @EventHandler(priority = EventPriority.NORMAL)
    public void onMatchStateChange(MythicSkywarsMatchStateChangeEvent event) {
        if (!PerkManager.get().isEnabled()) return;
        if (event.getState() != MatchState.PLAYING) return;
        if (event.isBungeecord()) return;

        GameMap map;
        try {
            map = event.getGameMap();
        } catch (Exception e) {
            return;
        }
        if (map == null) return;

        for (Player player : map.getAlivePlayers()) {
            for (Perk perk : PerkManager.get().getAllPerks()) {
                if (perk.isDisabled() || perk.getType() != PerkType.GAME_START) continue;
                int level = getPlayerPerkLevel(player, perk);
                if (level <= 0) continue;
                PerkLevel pl = perk.getLevel(level);
                if (pl == null) continue;

                PotionEffectType effectType = PotionEffectType.getByName(perk.getEffect());
                if (effectType != null) {
                    int dur = pl.getDuration() > 0 ? pl.getDuration() : perk.getDefaultDuration();
                    player.addPotionEffect(new PotionEffect(effectType, dur * 20, perk.getAmplifier()), true);
                }
            }
        }
    }

    // ==================== MINING EXPERTISE ====================

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!PerkManager.get().isEnabled()) return;
        Player player = event.getPlayer();
        if (!isInActiveGame(player)) return;

        Block block = event.getBlock();
        if (!ORES.contains(block.getType())) return;

        for (Perk perk : PerkManager.get().getAllPerks()) {
            if (perk.isDisabled() || perk.getType() != PerkType.MINING_EXPERTISE) continue;
            int level = getPlayerPerkLevel(player, perk);
            if (level <= 0) continue;
            PerkLevel pl = perk.getLevel(level);
            if (pl == null) continue;
            if (ThreadLocalRandom.current().nextInt(100) < pl.getPercent()) {
                // Drop one extra of the block's drops
                ItemStack tool;
                try {
                    tool = player.getInventory().getItemInMainHand();
                } catch (NoSuchMethodError e) {
                    tool = player.getItemInHand();
                }
                for (ItemStack drop : block.getDrops(tool)) {
                    block.getWorld().dropItemNaturally(block.getLocation(), drop);
                    break; // Only one extra
                }
            }
        }
    }
}
