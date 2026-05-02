package systems.mythical.mythicskywars.menus.soulwell;

import com.google.common.collect.Lists;
import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.database.DataStorage;
import systems.mythical.mythicskywars.managers.MatchManager;
import systems.mythical.mythicskywars.managers.PlayerStat;
import systems.mythical.mythicskywars.menus.gameoptions.objects.GameKit;
import systems.mythical.mythicskywars.menus.playeroptions.GlassColorOption;
import systems.mythical.mythicskywars.menus.playeroptions.KillSoundOption;
import systems.mythical.mythicskywars.menus.playeroptions.ParticleEffectOption;
import systems.mythical.mythicskywars.menus.playeroptions.PlayerOption;
import systems.mythical.mythicskywars.menus.playeroptions.ProjectileEffectOption;
import systems.mythical.mythicskywars.menus.playeroptions.TauntOption;
import systems.mythical.mythicskywars.menus.playeroptions.WinSoundOption;
import systems.mythical.mythicskywars.perks.Perk;
import systems.mythical.mythicskywars.perks.PerkManager;
import systems.mythical.mythicskywars.utilities.Messaging;
import systems.mythical.mythicskywars.utilities.SoulWellManager;
import systems.mythical.mythicskywars.utilities.Util;
import systems.mythical.mythicskywars.utilities.VaultUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Soul Well: spend souls for a random reward (Hypixel-style lobby feature).
 */
public final class SoulWellService {

    private static final Random RANDOM = new Random();
    private static final Map<UUID, BukkitTask> SPINNING = new ConcurrentHashMap<>();
    private static final Material[] FRAMES = new Material[]{
            Material.GOLD_BLOCK, Material.IRON_BLOCK, Material.DIAMOND, Material.EMERALD,
            Material.LAPIS_BLOCK, Material.REDSTONE_BLOCK, Material.NETHER_STAR, Material.ENDER_PEARL
    };

    private SoulWellService() {
    }

    /**
     * Validates Soul Well reward configuration on startup.
     * Logs warnings for allow-list entries that don't match any loaded cosmetic.
     */
    public static void validateRewardsOnStartup() {
        if (!MythicSkywars.getCfg().isSoulWellEnabled()) {
            return;
        }
        java.util.logging.Logger log = MythicSkywars.get().getLogger();
        log.info("[Soul Well] Validating reward configuration...");

        int totalWeight = MythicSkywars.getCfg().getSoulWellWeightCoins()
                + MythicSkywars.getCfg().getSoulWellWeightXp()
                + MythicSkywars.getCfg().getSoulWellWeightCage()
                + MythicSkywars.getCfg().getSoulWellWeightKit()
                + MythicSkywars.getCfg().getSoulWellWeightPerk()
                + MythicSkywars.getCfg().getSoulWellWeightKillSound()
                + MythicSkywars.getCfg().getSoulWellWeightWinSound()
                + MythicSkywars.getCfg().getSoulWellWeightProjectile()
                + MythicSkywars.getCfg().getSoulWellWeightTaunt()
                + MythicSkywars.getCfg().getSoulWellWeightCommand();

        if (totalWeight <= 0) {
            log.warning("[Soul Well] All reward weights are 0! Players will only receive coins as fallback.");
        } else {
            log.info("[Soul Well] Total reward weight: " + totalWeight);
        }

        // Validate cage allow-list
        validateAllowList(log, "cage", MythicSkywars.getCfg().getSoulWellCageAllowList(),
                extractKeys(GlassColorOption.getPlayerOptions()));
        // Validate perk (particle) allow-list
        validateAllowList(log, "perk (particle)", MythicSkywars.getCfg().getSoulWellPerkAllowList(),
                extractKeys(ParticleEffectOption.getPlayerOptions()));
        // Validate kill sound allow-list
        validateAllowList(log, "killsound", MythicSkywars.getCfg().getSoulWellKillSoundAllowList(),
                extractKeys(KillSoundOption.getPlayerOptions()));
        // Validate win sound allow-list
        validateAllowList(log, "winsound", MythicSkywars.getCfg().getSoulWellWinSoundAllowList(),
                extractKeys(WinSoundOption.getPlayerOptions()));
        // Validate projectile allow-list
        validateAllowList(log, "projectile", MythicSkywars.getCfg().getSoulWellProjectileAllowList(),
                extractKeys(ProjectileEffectOption.getPlayerOptions()));
        // Validate taunt allow-list
        validateAllowList(log, "taunt", MythicSkywars.getCfg().getSoulWellTauntAllowList(),
                extractKeys(TauntOption.getPlayerOptions()));

        // Check coins range
        if (MythicSkywars.getCfg().getSoulWellCoinsMin() > MythicSkywars.getCfg().getSoulWellCoinsMax()) {
            log.warning("[Soul Well] coins.min (" + MythicSkywars.getCfg().getSoulWellCoinsMin()
                    + ") > coins.max (" + MythicSkywars.getCfg().getSoulWellCoinsMax() + ")! Using min as both.");
        }
        if (MythicSkywars.getCfg().getSoulWellXpMin() > MythicSkywars.getCfg().getSoulWellXpMax()) {
            log.warning("[Soul Well] xp.min (" + MythicSkywars.getCfg().getSoulWellXpMin()
                    + ") > xp.max (" + MythicSkywars.getCfg().getSoulWellXpMax() + ")! Using min as both.");
        }

        log.info("[Soul Well] Reward validation complete.");
    }

    private static Set<String> extractKeys(java.util.ArrayList<PlayerOption> options) {
        Set<String> keys = new HashSet<>();
        for (PlayerOption o : options) {
            String perm = o.getPermission();
            int dot = perm.lastIndexOf('.');
            if (dot >= 0 && dot < perm.length() - 1) {
                keys.add(perm.substring(dot + 1).toLowerCase(Locale.ENGLISH));
            }
            if (o.getKey() != null) {
                keys.add(o.getKey().toLowerCase(Locale.ENGLISH));
            }
        }
        return keys;
    }

    private static void validateAllowList(java.util.logging.Logger log, String category,
                                           List<String> allowList, Set<String> validKeys) {
        if (allowList == null || allowList.isEmpty()) return;
        for (String entry : allowList) {
            if (entry == null || entry.trim().isEmpty()) continue;
            String normalized = entry.trim().toLowerCase(Locale.ENGLISH);
            if ("*".equals(normalized)) continue;
            if (!validKeys.contains(normalized)) {
                log.warning("[Soul Well] " + category + " allow-list entry '" + entry
                        + "' does not match any loaded cosmetic. Check spelling in config.yml.");
            }
        }
    }

    public static boolean isSpinning(Player player) {
        return SPINNING.containsKey(player.getUniqueId());
    }

    public static void cancelSpin(Player player) {
        BukkitTask t = SPINNING.remove(player.getUniqueId());
        if (t != null) {
            t.cancel();
        }
    }

    public static void onInventoryClose(Player player) {
        cancelSpin(player);
    }

    public static String spinningTitleRaw() {
        return new Messaging.MessageFormatter().format("menu.soul-well-spinning-title");
    }

    public static boolean titlesMatch(String openTitle, String expectedColored) {
        if (openTitle == null || expectedColored == null) {
            return false;
        }
        return ChatColor.stripColor(openTitle).equals(ChatColor.stripColor(expectedColored));
    }

    public static void beginSpin(Player player, int rolls) {
        beginSpin(player, rolls, false);
    }

    public static void beginSpinForced(Player player, int rolls) {
        beginSpin(player, rolls, true);
    }

    private static void beginSpin(Player player, int rolls, boolean forced) {
        if (!forced && !MythicSkywars.getCfg().isSoulWellEnabled()) {
            return;
        }
        SoulWellManager manager = MythicSkywars.getSoulWellManager();
        if (!forced && (manager == null || !manager.canUseSoulWell(player))) {
            player.sendMessage(new Messaging.MessageFormatter().format("soulwell.not-in-lobby"));
            return;
        }
        if (!forced && manager.getWellLocation() != null && player.getWorld().getName().equals(manager.getWellLocation().getWorld().getName())) {
            if (player.getLocation().distanceSquared(manager.getWellLocation().clone().add(0.5, 0, 0.5)) > 25) {
                player.sendMessage(new Messaging.MessageFormatter().format("soulwell.must-use-at-well"));
                return;
            }
        }
        if (MatchManager.get().getPlayerMap(player) != null) {
            player.sendMessage(new Messaging.MessageFormatter().format("soulwell.not-in-lobby"));
            Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getErrorSound(), 1, 1);
            return;
        }
        int max = MythicSkywars.getCfg().getSoulWellMaxSpinsAtOnce();
        if (rolls < 1 || rolls > max) {
            return;
        }
        PlayerStat ps = PlayerStat.getPlayerStats(player);
        if (ps == null || !ps.isInitialized()) {
            player.sendMessage(new Messaging.MessageFormatter().format("stats.not-ready"));
            return;
        }

        int per = MythicSkywars.getCfg().getSoulWellSoulsPerSpin();
        int cost = per * rolls;
        boolean freeXezbeth = rolls == 1 && ps.hasSoulWellXezbethFreeRollPending();
        if (freeXezbeth) {
            ps.setSoulWellXezbethFreeRollPending(false);
            cost = 0;
        }
        if (cost > 0 && ps.getSouls() < cost) {
            player.sendMessage(new Messaging.MessageFormatter()
                    .setVariable("cost", "" + cost)
                    .setVariable("souls", "" + ps.getSouls())
                    .format("soulwell.not-enough-souls"));
            Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getErrorSound(), 1, 1);
            return;
        }
        if (cost > 0) {
            ps.addSouls(-cost);
            DataStorage.get().saveStats(ps);
        }
        ps.addSoulWellUsages(1);

        player.closeInventory();
        Reward grant = rollReward(player);
        int centerSlot = 13;
        int invSize = 27;
        String title = spinningTitleRaw();
        Inventory inv = Bukkit.createInventory(null, invSize, title);
        player.openInventory(inv);

        int ticksPer = MythicSkywars.getCfg().getSoulWellAnimTicksPerFrame();
        int frames = MythicSkywars.getCfg().getSoulWellAnimFrames();
        UUID uuid = player.getUniqueId();

        BukkitTask task = new BukkitRunnable() {
            int frame = 0;

            @Override
            public void run() {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null || !p.isOnline()) {
                    SPINNING.remove(uuid);
                    cancel();
                    return;
                }
                if (!titlesMatch(p.getOpenInventory().getTitle(), title)) {
                    SPINNING.remove(uuid);
                    cancel();
                    return;
                }
                Material mat = FRAMES[frame % FRAMES.length];
                ItemStack show = new ItemStack(mat, 1);
                p.getOpenInventory().getTopInventory().setItem(centerSlot, show);
                Util.get().playSound(p, p.getLocation(), MythicSkywars.getCfg().getConfirmeSelctionSound(), 0.35f, 1.2f);
                frame++;
                if (frame >= frames) {
                    SPINNING.remove(uuid);
                    applyReward(p, grant);
                    p.getOpenInventory().getTopInventory().setItem(centerSlot, grant.displayIcon());
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            Player p2 = Bukkit.getPlayer(uuid);
                            if (p2 != null && p2.isOnline()) {
                                p2.closeInventory();
                                if (forced) {
                                    SoulWellMenu.openForced(p2);
                                } else {
                                    SoulWellMenu.open(p2);
                                }
                            }
                        }
                    }.runTaskLater(MythicSkywars.get(), 25L);
                    cancel();
                }
            }
        }.runTaskTimer(MythicSkywars.get(), 1L, ticksPer);
        SPINNING.put(uuid, task);
    }

    private static Reward rollReward(Player player) {
        EnumMap<RewardKind, Integer> w = new EnumMap<>(RewardKind.class);
        w.put(RewardKind.COINS, MythicSkywars.getCfg().getSoulWellWeightCoins());
        w.put(RewardKind.XP, MythicSkywars.getCfg().getSoulWellWeightXp());
        w.put(RewardKind.CAGE, MythicSkywars.getCfg().getSoulWellWeightCage());
        w.put(RewardKind.KIT, MythicSkywars.getCfg().getSoulWellWeightKit());
        w.put(RewardKind.PERK, MythicSkywars.getCfg().getSoulWellWeightPerk());
        w.put(RewardKind.KILL_SOUND, MythicSkywars.getCfg().getSoulWellWeightKillSound());
        w.put(RewardKind.WIN_SOUND, MythicSkywars.getCfg().getSoulWellWeightWinSound());
        w.put(RewardKind.PROJECTILE, MythicSkywars.getCfg().getSoulWellWeightProjectile());
        w.put(RewardKind.TAUNT, MythicSkywars.getCfg().getSoulWellWeightTaunt());
        w.put(RewardKind.GAME_PERK, MythicSkywars.getCfg().getSoulWellWeightGamePerk());
        w.put(RewardKind.COMMAND, MythicSkywars.getCfg().getSoulWellWeightCommand());
        while (true) {
            int total = 0;
            for (int v : w.values()) {
                total += Math.max(0, v);
            }
            if (total <= 0) {
                return randomCoins();
            }
            int pick = RANDOM.nextInt(total);
            int acc = 0;
            for (Map.Entry<RewardKind, Integer> e : w.entrySet()) {
                int wv = Math.max(0, e.getValue());
                if (wv == 0) {
                    continue;
                }
                acc += wv;
                if (pick < acc) {
                    Reward r = buildReward(player, e.getKey());
                    if (r != null) {
                        return r;
                    }
                    w.put(e.getKey(), 0);
                    break;
                }
            }
        }
    }

    private static Reward randomCoins() {
        int lo = MythicSkywars.getCfg().getSoulWellCoinsMin();
        int hi = MythicSkywars.getCfg().getSoulWellCoinsMax();
        int amt = lo + (lo < hi ? RANDOM.nextInt(hi - lo + 1) : 0);
        return new Reward(RewardKind.COINS, amt, null, null, null, null, null);
    }

    private static Reward buildReward(Player player, RewardKind kind) {
        switch (kind) {
            case COINS:
                return randomCoins();
            case XP:
                int xlo = MythicSkywars.getCfg().getSoulWellXpMin();
                int xhi = MythicSkywars.getCfg().getSoulWellXpMax();
                int xp = xlo + (xlo < xhi ? RANDOM.nextInt(xhi - xlo + 1) : 0);
                return new Reward(RewardKind.XP, xp, null, null, null, null, null);
            case CAGE:
                PlayerOption opt = pickRandomGlass(player);
                if (opt == null) {
                    return null;
                }
                return new Reward(RewardKind.CAGE, 0, opt, null, null, null, null);
            case KIT:
                GameKit kit = pickRandomKit(player);
                if (kit == null) {
                    return null;
                }
                return new Reward(RewardKind.KIT, 0, null, kit, null, null, null);
            case PERK:
                PlayerOption pe = pickRandomParticle(player);
                if (pe == null) {
                    return null;
                }
                return new Reward(RewardKind.PERK, 0, null, null, pe, null, null);
            case KILL_SOUND:
                PlayerOption ks = pickRandomKillSound(player);
                if (ks == null) {
                    return null;
                }
                return new Reward(RewardKind.KILL_SOUND, 0, null, null, null, ks, null);
            case WIN_SOUND:
                PlayerOption ws = pickRandomWinSound(player);
                if (ws == null) {
                    return null;
                }
                return new Reward(RewardKind.WIN_SOUND, 0, null, null, null, ws, null);
            case PROJECTILE:
                PlayerOption pr = pickRandomProjectile(player);
                if (pr == null) {
                    return null;
                }
                return new Reward(RewardKind.PROJECTILE, 0, null, null, null, pr, null);
            case TAUNT:
                PlayerOption ta = pickRandomTaunt(player);
                if (ta == null) {
                    return null;
                }
                return new Reward(RewardKind.TAUNT, 0, null, null, null, ta, null);
            case GAME_PERK:
                String perkResult = pickRandomGamePerk(player);
                if (perkResult == null) {
                    return null;
                }
                return new Reward(RewardKind.GAME_PERK, 0, null, null, null, null, perkResult);
            case COMMAND:
                String cmd = pickRandomCommandReward();
                if (cmd == null) {
                    return null;
                }
                return new Reward(RewardKind.COMMAND, 0, null, null, null, null, cmd);
            default:
                return randomCoins();
        }
    }

    private static PlayerOption pickRandomGlass(Player player) {
        Set<String> allow = normalizeList(MythicSkywars.getCfg().getSoulWellCageAllowList());
        Set<String> deny = normalizeList(MythicSkywars.getCfg().getSoulWellCageDenyList());
        List<PlayerOption> list = new ArrayList<>();
        for (PlayerOption o : GlassColorOption.getPlayerOptions()) {
            if (!isPermittedByLists(extractOptionKey(o.getPermission()), o.getPermission(), allow, deny)) {
                continue;
            }
            if (!player.hasPermission(o.getPermission())) {
                list.add(o);
            }
        }
        if (list.isEmpty()) {
            return null;
        }
        Collections.shuffle(list, RANDOM);
        return list.get(0);
    }

    private static GameKit pickRandomKit(Player player) {
        Set<String> allow = normalizeList(MythicSkywars.getCfg().getSoulWellKitAllowList());
        Set<String> deny = normalizeList(MythicSkywars.getCfg().getSoulWellKitDenyList());
        List<GameKit> list = new ArrayList<>();
        for (GameKit k : GameKit.getAvailableKits()) {
            String perm = "sw.kit." + k.getFilename();
            if (!isPermittedByLists(k.getFilename(), perm, allow, deny)) {
                continue;
            }
            if (k.needPermission() && !player.hasPermission(perm)) {
                list.add(k);
            }
        }
        if (list.isEmpty()) {
            return null;
        }
        Collections.shuffle(list, RANDOM);
        return list.get(0);
    }

    private static PlayerOption pickRandomParticle(Player player) {
        Set<String> allow = normalizeList(MythicSkywars.getCfg().getSoulWellPerkAllowList());
        Set<String> deny = normalizeList(MythicSkywars.getCfg().getSoulWellPerkDenyList());
        List<PlayerOption> list = new ArrayList<>();
        for (PlayerOption o : ParticleEffectOption.getPlayerOptions()) {
            if (!isPermittedByLists(extractOptionKey(o.getPermission()), o.getPermission(), allow, deny)) {
                continue;
            }
            if (!player.hasPermission(o.getPermission())) {
                list.add(o);
            }
        }
        if (list.isEmpty()) {
            return null;
        }
        Collections.shuffle(list, RANDOM);
        return list.get(0);
    }

    private static String pickRandomCommandReward() {
        List<String> cmds = MythicSkywars.getCfg().getSoulWellCommandRewards();
        List<String> valid = new ArrayList<>();
        for (String cmd : cmds) {
            if (cmd != null && !cmd.trim().isEmpty()) {
                valid.add(cmd.trim());
            }
        }
        if (valid.isEmpty()) {
            return null;
        }
        Collections.shuffle(valid, RANDOM);
        return valid.get(0);
    }

    private static PlayerOption pickRandomKillSound(Player player) {
        Set<String> allow = normalizeList(MythicSkywars.getCfg().getSoulWellKillSoundAllowList());
        Set<String> deny = normalizeList(MythicSkywars.getCfg().getSoulWellKillSoundDenyList());
        List<PlayerOption> list = new ArrayList<>();
        for (PlayerOption o : KillSoundOption.getPlayerOptions()) {
            if (!isPermittedByLists(extractOptionKey(o.getPermission()), o.getPermission(), allow, deny)) {
                continue;
            }
            if (!player.hasPermission(o.getPermission())) {
                list.add(o);
            }
        }
        if (list.isEmpty()) {
            return null;
        }
        Collections.shuffle(list, RANDOM);
        return list.get(0);
    }

    private static PlayerOption pickRandomWinSound(Player player) {
        Set<String> allow = normalizeList(MythicSkywars.getCfg().getSoulWellWinSoundAllowList());
        Set<String> deny = normalizeList(MythicSkywars.getCfg().getSoulWellWinSoundDenyList());
        List<PlayerOption> list = new ArrayList<>();
        for (PlayerOption o : WinSoundOption.getPlayerOptions()) {
            if (!isPermittedByLists(extractOptionKey(o.getPermission()), o.getPermission(), allow, deny)) {
                continue;
            }
            if (!player.hasPermission(o.getPermission())) {
                list.add(o);
            }
        }
        if (list.isEmpty()) {
            return null;
        }
        Collections.shuffle(list, RANDOM);
        return list.get(0);
    }

    private static PlayerOption pickRandomProjectile(Player player) {
        Set<String> allow = normalizeList(MythicSkywars.getCfg().getSoulWellProjectileAllowList());
        Set<String> deny = normalizeList(MythicSkywars.getCfg().getSoulWellProjectileDenyList());
        List<PlayerOption> list = new ArrayList<>();
        for (PlayerOption o : ProjectileEffectOption.getPlayerOptions()) {
            if (!isPermittedByLists(extractOptionKey(o.getPermission()), o.getPermission(), allow, deny)) {
                continue;
            }
            if (!player.hasPermission(o.getPermission())) {
                list.add(o);
            }
        }
        if (list.isEmpty()) {
            return null;
        }
        Collections.shuffle(list, RANDOM);
        return list.get(0);
    }

    private static PlayerOption pickRandomTaunt(Player player) {
        Set<String> allow = normalizeList(MythicSkywars.getCfg().getSoulWellTauntAllowList());
        Set<String> deny = normalizeList(MythicSkywars.getCfg().getSoulWellTauntDenyList());
        List<PlayerOption> list = new ArrayList<>();
        for (PlayerOption o : TauntOption.getPlayerOptions()) {
            if (!isPermittedByLists(extractOptionKey(o.getPermission()), o.getPermission(), allow, deny)) {
                continue;
            }
            if (!player.hasPermission(o.getPermission())) {
                list.add(o);
            }
        }
        if (list.isEmpty()) {
            return null;
        }
        Collections.shuffle(list, RANDOM);
        return list.get(0);
    }

    /**
     * Picks a random game perk level the player hasn't unlocked yet.
     * Returns a string in format "perkKey:level" or null if nothing available.
     */
    private static String pickRandomGamePerk(Player player) {
        if (!PerkManager.get().isEnabled()) {
            return null;
        }
        List<String> candidates = new ArrayList<>();
        for (Perk perk : PerkManager.get().getAllPerks()) {
            if (perk.isDisabled()) continue;
            // Find the next level the player doesn't have
            for (int lvl = 1; lvl <= perk.getMaxLevel(); lvl++) {
                if (!player.hasPermission(perk.getPermission(lvl))) {
                    candidates.add(perk.getKey() + ":" + lvl);
                    break; // Only offer the next level up
                }
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        Collections.shuffle(candidates, RANDOM);
        return candidates.get(0);
    }

    private static Set<String> normalizeList(List<String> list) {
        Set<String> out = new HashSet<>();
        if (list == null) {
            return out;
        }
        for (String val : list) {
            if (val != null && !val.trim().isEmpty()) {
                out.add(val.trim().toLowerCase(Locale.ENGLISH));
            }
        }
        return out;
    }

    private static boolean isPermittedByLists(String key, String fullPermission, Set<String> allow, Set<String> deny) {
        String k = key == null ? "" : key.toLowerCase(Locale.ENGLISH);
        String p = fullPermission == null ? "" : fullPermission.toLowerCase(Locale.ENGLISH);
        if (deny.contains(k) || deny.contains(p)) {
            return false;
        }
        if (allow.isEmpty() || allow.contains("*")) {
            return true;
        }
        return allow.contains(k) || allow.contains(p);
    }

    private static String extractOptionKey(String permission) {
        if (permission == null || permission.trim().isEmpty()) {
            return "";
        }
        int dot = permission.lastIndexOf('.');
        if (dot == -1 || dot >= permission.length() - 1) {
            return permission;
        }
        return permission.substring(dot + 1);
    }

    private static void applyReward(Player player, Reward r) {
        PlayerStat ps = PlayerStat.getPlayerStats(player);
        if (ps == null) {
            return;
        }
        switch (r.kind) {
            case COINS:
                if (MythicSkywars.getCfg().economyEnabled()) {
                    VaultUtils.get().give(player, r.amount);
                }
                player.sendMessage(new Messaging.MessageFormatter().setVariable("amount", "" + r.amount).format("soulwell.reward-coins"));
                break;
            case XP:
                ps.addXp(player, r.amount);
                DataStorage.get().saveStats(ps);
                player.sendMessage(new Messaging.MessageFormatter().setVariable("amount", "" + r.amount).format("soulwell.reward-xp"));
                break;
            case CAGE:
                ps.addSoulWellRares(1);
                if (r.glassOption != null) {
                    r.glassOption.setEffect(ps);
                    DataStorage.get().saveStats(ps);
                    player.sendMessage(new Messaging.MessageFormatter()
                            .setVariable("name", ChatColor.stripColor(r.glassOption.getName()))
                            .format("soulwell.reward-cage"));
                }
                break;
            case KIT:
                ps.addSoulWellLegendaries(1);
                if (r.kit != null) {
                    ps.addPerm("sw.kit." + r.kit.getFilename(), true);
                    player.sendMessage(new Messaging.MessageFormatter()
                            .setVariable("kit", r.kit.getColorName())
                            .format("soulwell.reward-kit"));
                }
                break;
            case PERK:
                ps.addSoulWellLegendaries(1);
                if (r.perkOption != null) {
                    ps.addPerm(r.perkOption.getPermission(), true);
                    player.sendMessage(new Messaging.MessageFormatter()
                            .setVariable("name", ChatColor.stripColor(r.perkOption.getName()))
                            .format("soulwell.reward-perk"));
                }
                break;
            case KILL_SOUND:
                ps.addSoulWellRares(1);
                if (r.cosmeticOption != null) {
                    ps.addPerm(r.cosmeticOption.getPermission(), true);
                    player.sendMessage(new Messaging.MessageFormatter()
                            .setVariable("name", ChatColor.stripColor(r.cosmeticOption.getName()))
                            .format("soulwell.reward-killsound"));
                }
                break;
            case WIN_SOUND:
                ps.addSoulWellRares(1);
                if (r.cosmeticOption != null) {
                    ps.addPerm(r.cosmeticOption.getPermission(), true);
                    player.sendMessage(new Messaging.MessageFormatter()
                            .setVariable("name", ChatColor.stripColor(r.cosmeticOption.getName()))
                            .format("soulwell.reward-winsound"));
                }
                break;
            case PROJECTILE:
                ps.addSoulWellRares(1);
                if (r.cosmeticOption != null) {
                    ps.addPerm(r.cosmeticOption.getPermission(), true);
                    player.sendMessage(new Messaging.MessageFormatter()
                            .setVariable("name", ChatColor.stripColor(r.cosmeticOption.getName()))
                            .format("soulwell.reward-projectile"));
                }
                break;
            case TAUNT:
                ps.addSoulWellRares(1);
                if (r.cosmeticOption != null) {
                    ps.addPerm(r.cosmeticOption.getPermission(), true);
                    player.sendMessage(new Messaging.MessageFormatter()
                            .setVariable("name", ChatColor.stripColor(r.cosmeticOption.getName()))
                            .format("soulwell.reward-taunt"));
                }
                break;
            case GAME_PERK:
                ps.addSoulWellLegendaries(1);
                if (r.commandReward != null && r.commandReward.contains(":")) {
                    String[] parts = r.commandReward.split(":", 2);
                    String perkKey = parts[0];
                    int perkLevel = 1;
                    try { perkLevel = Integer.parseInt(parts[1]); } catch (NumberFormatException ignored) {}
                    Perk perk = PerkManager.get().getPerk(perkKey);
                    if (perk != null) {
                        ps.addPerm(perk.getPermission(perkLevel), true);
                        player.sendMessage(new Messaging.MessageFormatter()
                                .setVariable("perk", ChatColor.translateAlternateColorCodes('&', perk.getName()))
                                .setVariable("level", "" + perkLevel)
                                .format("soulwell.reward-gameperk"));
                    }
                }
                break;
            case COMMAND:
                if (r.commandReward != null && !r.commandReward.trim().isEmpty()) {
                    String command = r.commandReward.replace("{player}", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    player.sendMessage(new Messaging.MessageFormatter().format("soulwell.reward-command"));
                }
                break;
            default:
                break;
        }
        DataStorage.get().saveStats(ps);
        Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getConfirmeSelctionSound(), 1, 1);
    }

    private enum RewardKind {
        COINS, XP, CAGE, KIT, PERK, KILL_SOUND, WIN_SOUND, PROJECTILE, TAUNT, GAME_PERK, COMMAND
    }

    private static final class Reward {
        final RewardKind kind;
        final int amount;
        final PlayerOption glassOption;
        final GameKit kit;
        final PlayerOption perkOption;
        final PlayerOption cosmeticOption;
        final String commandReward;

        Reward(RewardKind kind, int amount, PlayerOption glassOption, GameKit kit, PlayerOption perkOption, PlayerOption cosmeticOption, String commandReward) {
            this.kind = kind;
            this.amount = amount;
            this.glassOption = glassOption;
            this.kit = kit;
            this.perkOption = perkOption;
            this.cosmeticOption = cosmeticOption;
            this.commandReward = commandReward;
        }

        ItemStack displayIcon() {
            switch (kind) {
                case COINS:
                    return icon(Material.GOLD_INGOT, "&6&lCoins");
                case XP:
                    return icon(xpBottleMaterial(), "&b&lXP");
                case CAGE:
                    return glassOption != null ? glassOption.getItem().clone() : icon(stainedGlassMaterial(), "&d&lCage");
                case KIT:
                    return kit != null && kit.getIcon() != null ? kit.getIcon().clone() : icon(Material.DIAMOND_SWORD, "&a&lKit");
                case PERK:
                    return perkOption != null ? perkOption.getItem().clone() : icon(Material.BLAZE_POWDER, "&e&lPerk");
                case KILL_SOUND:
                    return cosmeticOption != null ? cosmeticOption.getItem().clone() : icon(Material.NOTE_BLOCK, "&c&lKill Sound");
                case WIN_SOUND:
                    return cosmeticOption != null ? cosmeticOption.getItem().clone() : icon(Material.NOTE_BLOCK, "&d&lWin Sound");
                case PROJECTILE:
                    return cosmeticOption != null ? cosmeticOption.getItem().clone() : icon(Material.ARROW, "&b&lProjectile Trail");
                case TAUNT:
                    return cosmeticOption != null ? cosmeticOption.getItem().clone() : icon(Material.SHIELD, "&6&lTaunt");
                case GAME_PERK:
                    return icon(Material.BEACON, "&5&lGame Perk");
                case COMMAND:
                    return icon(commandBlockMaterial(), "&c&lSpecial Reward");
                default:
                    return icon(Material.NETHER_STAR, "&5&l?");
            }
        }

        private static Material xpBottleMaterial() {
            try {
                return Material.valueOf("EXPERIENCE_BOTTLE");
            } catch (IllegalArgumentException e) {
                return Material.EXP_BOTTLE;
            }
        }

        private static Material stainedGlassMaterial() {
            try {
                return Material.valueOf("WHITE_STAINED_GLASS");
            } catch (IllegalArgumentException e) {
                return Material.STAINED_GLASS;
            }
        }

        private static Material commandBlockMaterial() {
            try {
                return Material.valueOf("COMMAND_BLOCK");
            } catch (IllegalArgumentException e) {
                return Material.REDSTONE_BLOCK;
            }
        }

        private static ItemStack icon(Material m, String name) {
            ItemStack it = new ItemStack(m, 1);
            return MythicSkywars.getNMS().getItemStack(it, Lists.newArrayList(), ChatColor.translateAlternateColorCodes('&', name));
        }
    }
}
