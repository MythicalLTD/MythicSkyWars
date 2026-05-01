package com.walrusone.skywarsreloaded.utilities;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.enums.Vote;
import com.walrusone.skywarsreloaded.game.GameMap;
import com.walrusone.skywarsreloaded.managers.ChestStorageLayout;
import com.walrusone.skywarsreloaded.managers.MatchManager;
import com.walrusone.skywarsreloaded.menus.gameoptions.objects.CoordLoc;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.Sound;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;

/**
 * Optional integration with NTD-LuckyBlock.
 * Replaces recorded chest locations with lucky blocks in lucky mode and can register extra drops
 * (messages/commands and/or SkyWars chest-table loot) on top of NTD's own RNG.
 */
public final class LuckyBlockHook {

    private static final String[] PLUGIN_NAMES = new String[] {"ntdLuckyBlock", "NTD-LuckyBlock"};
    private static final int MIN_BUILD = 68;
    private static volatile boolean dropsHooked = false;
    private static final Random RNG = new Random();
    private static YamlConfiguration settings;

    public enum LuckyProfile {
        BASIC, OP;

        public static LuckyProfile fromToken(String token) {
            if (token == null) {
                return null;
            }
            String normalized = token.trim().toLowerCase();
            if (normalized.equals("luckyop") || normalized.equals("op")) {
                return OP;
            }
            if (normalized.equals("luckybasic") || normalized.equals("basic") || normalized.equals("lucky")) {
                return BASIC;
            }
            return null;
        }
    }

    private LuckyBlockHook() {
    }

    public static void setup() {
        reloadLuckyBlocksYaml();
        if (!isAvailable() || dropsHooked) {
            return;
        }
        dropsHooked = true;
        boolean registerMessages = settings.getBoolean("luckyBlocks.registerCustomDrops", false);
        boolean registerChestLoot = settings.getBoolean("luckyBlocks.chestLootDrops.enabled", true);
        if (!registerMessages && !registerChestLoot) {
            if (SkyWarsReloaded.getCfg().debugEnabled()) {
                SkyWarsReloaded.get().getLogger().info(
                        "LuckyBlock: NTD default drops only (registerCustomDrops=false, chestLootDrops=false).");
            }
            return;
        }
        try {
            if (registerMessages) {
                registerConfiguredDropForConfiguredTypes(LuckyProfile.BASIC);
                registerConfiguredDropForConfiguredTypes(LuckyProfile.OP);
            }
            if (registerChestLoot) {
                registerChestLootDropHandlers(LuckyProfile.BASIC);
                registerChestLootDropHandlers(LuckyProfile.OP);
            }
            if (SkyWarsReloaded.getCfg().debugEnabled()) {
                SkyWarsReloaded.get().getLogger().info("LuckyBlock hooks: customDrops=" + registerMessages + ", chestLootDrops=" + registerChestLoot);
            }
            if (registerChestLoot && settings.getBoolean("luckyBlocks.chestLootDrops.skywarsOnlyDrops", false)) {
                SkyWarsReloaded.get().getLogger().info(
                        "LuckyBlock chestLootDrops.skywarsOnlyDrops=true: SkyWars hook runs every lucky break (100%). "
                                + "Clear NTD factory drops for your block colors so outcomes are only chest/sus/ultra twists.");
            }
        } catch (Exception ex) {
            SkyWarsReloaded.get().getLogger().log(Level.WARNING, "Failed to register LuckyBlock drop hooks", ex);
        }
    }

    /**
     * Replaces map chest coordinates with NTD lucky blocks. NTD's current design uses colored stained glass as the
     * visible block (not sponge/heads); {@code LuckyBlockAPI.placeLuckyBlock} is used so behavior matches NTD.
     */
    public static boolean replaceChestsWithLuckyBlocks(GameMap map, LuckyProfile profile) {
        if (!isAvailable()) {
            return false;
        }
        reloadLuckyBlocksYaml();
        setup();
        World world = map.getCurrentWorld();
        int chestCount = map.getChests() == null ? 0 : map.getChests().size();
        int centerCount = map.getCenterChests() == null ? 0 : map.getCenterChests().size();
        int total = 0;
        total += replace(world, map.getChests(), getIslandType(profile));
        if (isReplaceCenterChests()) {
            total += replace(world, map.getCenterChests(), getCenterType(profile));
        }
        if (total > 0 && SkyWarsReloaded.getCfg().debugEnabled()) {
            SkyWarsReloaded.get().getLogger().info("LuckyBlock hook replaced " + total + " chest spots on map " + map.getName());
        } else if (total == 0 && (chestCount > 0 || (isReplaceCenterChests() && centerCount > 0))) {
            SkyWarsReloaded.get().getLogger().warning("LuckyBlock: lucky mode on map '" + map.getName()
                    + "' but 0 placements (world null? " + (world == null)
                    + ", island chest coords: " + chestCount
                    + ", center coords: " + centerCount
                    + "). Check ntdLuckyBlock is enabled and islandType/centerType in luckyblocks.yml match loaded types.");
        }
        return total > 0;
    }

    /**
     * After kits load: optional NTD lucky-block items in inventory (lucky + optional &quot;unlucky&quot; type),
     * plus optional console commands per player. By default uses {@code ntdluckyblock give} so items match NTD
     * (API stacks often look like plain glass). See {@code luckyBlocks.handItems} in luckyblocks.yml.
     */
    public static void giveLuckyModeHandItems(GameMap map) {
        if (map == null || !map.isLuckyModeEnabled() || !isAvailable()) {
            return;
        }
        reloadLuckyBlocksYaml();
        LuckyProfile profile = map.getActiveLuckyProfile();
        if (profile == null) {
            Vote v = map.getChestOption().getChestVoteResult();
            Vote resolved = v == Vote.CHESTRANDOM ? Vote.getRandom("chest") : v;
            profile = resolved == Vote.CHESTOP ? LuckyProfile.OP : LuckyProfile.BASIC;
        }
        if (!settings.getBoolean("luckyBlocks.handItems.enabled", true)) {
            return;
        }
        ensureSettings();
        int luckyAmt = Math.max(0, Math.min(64, settings.getInt("luckyBlocks.handItems.luckyAmount", 3)));
        String luckyOverride = settings.getString("luckyBlocks.handItems.luckyType", "");
        String luckyType = (luckyOverride == null || luckyOverride.trim().isEmpty())
                ? getIslandType(profile)
                : luckyOverride.trim();
        int unluckyAmt = Math.max(0, Math.min(64, settings.getInt("luckyBlocks.handItems.unluckyAmount", 1)));
        String unluckyType = settings.getString("luckyBlocks.handItems.unluckyType", "GRAY");
        java.util.List<String> cmds = settings.getStringList("luckyBlocks.handItems.matchStartCommands");
        int funPer = Math.max(0, Math.min(64, settings.getInt("luckyBlocks.handItems.funExtraAmountPerType", 0)));
        java.util.List<String> funTypes = settings.getStringList("luckyBlocks.handItems.funExtraTypes");
        boolean useConsoleGive = settings.getBoolean("luckyBlocks.handItems.useConsoleGiveCommands", true);

        int kindCount = 0;
        if (luckyAmt > 0) {
            kindCount++;
        }
        if (unluckyAmt > 0 && unluckyType != null && !unluckyType.trim().isEmpty()) {
            kindCount++;
        }
        if (funPer > 0 && funTypes != null) {
            for (String ft : funTypes) {
                if (ft != null && !ft.trim().isEmpty()) {
                    kindCount++;
                }
            }
        }

        if (useConsoleGive) {
            for (Player p : map.getAlivePlayers()) {
                if (p == null || !p.isOnline()) {
                    continue;
                }
                if (luckyAmt > 0) {
                    dispatchNtdLuckyGiveCommand(p, luckyType, luckyAmt);
                }
                if (unluckyAmt > 0 && unluckyType != null && !unluckyType.trim().isEmpty()) {
                    dispatchNtdLuckyGiveCommand(p, unluckyType.trim(), unluckyAmt);
                }
                if (funPer > 0 && funTypes != null) {
                    for (String ft : funTypes) {
                        if (ft == null || ft.trim().isEmpty()) {
                            continue;
                        }
                        dispatchNtdLuckyGiveCommand(p, ft.trim(), funPer);
                    }
                }
                runMatchStartCommandsForPlayer(cmds, p);
            }
            if (kindCount == 0) {
                SkyWarsReloaded.get().getLogger().warning(
                        "Lucky handItems: useConsoleGiveCommands is true but all amounts are 0 (luckyAmount, unluckyAmount, funExtraAmountPerType). "
                                + ChestStorageLayout.resolvedFileForMessaging(SkyWarsReloaded.get(), "luckyblocks.yml").getAbsolutePath());
            } else if (!map.getAlivePlayers().isEmpty()) {
                SkyWarsReloaded.get().getLogger().info(
                        "Lucky handItems: NTD console give (" + kindCount + " color line(s)) for "
                                + map.getAlivePlayers().size() + " player(s) on map " + map.getName());
            }
            return;
        }

        ItemStack luckyTemplate = luckyAmt > 0 ? getLuckyItemStackFromTypeName(luckyType) : null;
        ItemStack unluckyTemplate = (unluckyAmt > 0 && unluckyType != null && !unluckyType.trim().isEmpty())
                ? getLuckyItemStackFromTypeName(unluckyType.trim())
                : null;
        java.util.List<ItemStack> funTemplates = new ArrayList<>();
        if (funPer > 0 && funTypes != null) {
            for (String ft : funTypes) {
                if (ft == null || ft.trim().isEmpty()) {
                    continue;
                }
                ItemStack t = getLuckyItemStackFromTypeName(ft.trim());
                if (t != null && t.getType() != Material.AIR) {
                    funTemplates.add(t);
                }
            }
        }

        for (Player p : map.getAlivePlayers()) {
            if (p == null || !p.isOnline()) {
                continue;
            }
            if (luckyTemplate != null && luckyTemplate.getType() != Material.AIR) {
                ItemStack give = luckyTemplate.clone();
                give.setAmount(luckyAmt);
                p.getInventory().addItem(give);
            }
            if (unluckyTemplate != null && unluckyTemplate.getType() != Material.AIR) {
                ItemStack give = unluckyTemplate.clone();
                give.setAmount(unluckyAmt);
                p.getInventory().addItem(give);
            }
            for (ItemStack tmpl : funTemplates) {
                ItemStack give = tmpl.clone();
                give.setAmount(funPer);
                p.getInventory().addItem(give);
            }
            runMatchStartCommandsForPlayer(cmds, p);
        }
        int templateCount = (luckyTemplate != null && luckyTemplate.getType() != Material.AIR ? 1 : 0)
                + (unluckyTemplate != null && unluckyTemplate.getType() != Material.AIR ? 1 : 0)
                + funTemplates.size();
        if (templateCount == 0) {
            SkyWarsReloaded.get().getLogger().warning(
                    "Lucky handItems: API stacks disabled or failed (set useConsoleGiveCommands: true or fix types). "
                            + ChestStorageLayout.resolvedFileForMessaging(SkyWarsReloaded.get(), "luckyblocks.yml").getAbsolutePath());
        } else if (!map.getAlivePlayers().isEmpty()) {
            SkyWarsReloaded.get().getLogger().info(
                    "Lucky handItems: " + templateCount + " stack type(s) for " + map.getAlivePlayers().size()
                            + " player(s) on map " + map.getName());
        }
    }

    private static void runMatchStartCommandsForPlayer(java.util.List<String> cmds, Player p) {
        if (cmds == null || p == null || !p.isOnline()) {
            return;
        }
        for (String raw : cmds) {
            if (raw == null || raw.trim().isEmpty()) {
                continue;
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), raw.trim().replace("%player%", p.getName()));
        }
    }

    /**
     * Runs {@code <base> give <player> <color> <amount>} as console (default base {@code ntdluckyblock}).
     * Color is the enum token lowercased (e.g. YELLOW → yellow, LIGHT_BLUE → light_blue).
     */
    private static void dispatchNtdLuckyGiveCommand(Player player, String luckyBlockTypeToken, int amount) {
        if (player == null || !player.isOnline() || amount <= 0) {
            return;
        }
        if (luckyBlockTypeToken == null || luckyBlockTypeToken.trim().isEmpty()) {
            return;
        }
        ensureSettings();
        String base = settings.getString("luckyBlocks.handItems.consoleGiveCommandBase", "ntdluckyblock");
        if (base == null) {
            base = "ntdluckyblock";
        }
        base = base.trim();
        if (base.isEmpty()) {
            base = "ntdluckyblock";
        }
        String color = luckyBlockTypeToken.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        String cmd = base + " give " + player.getName() + " " + color + " " + amount;
        try {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        } catch (Exception ex) {
            SkyWarsReloaded.get().getLogger().log(Level.WARNING, "Lucky handItems console give failed: " + cmd, ex);
        }
        if (SkyWarsReloaded.getCfg().debugEnabled()) {
            SkyWarsReloaded.get().getLogger().info("Lucky handItems dispatch: " + cmd);
        }
    }

    /**
     * When true (default), SkyWars grants {@code luckyblock.break.&lt;color&gt;} via {@link com.walrusone.skywarsreloaded.utilities.LuckyBlockBreakAttachment}
     * for alive players during lucky matches so NTD {@code break-permissions} does not block breaks.
     */
    public static boolean isGrantBreakPermissionInMatch() {
        ensureSettings();
        return settings.getBoolean("luckyBlocks.grantBreakPermissionInMatch", true);
    }

    /**
     * Resolves a placeable NTD lucky-block item. Tries {@code LuckyBlockType.get().…} first (some builds differ from
     * {@code getItem()} on the enum), then static API helpers, then {@code getItem()}. Optional display polish so
     * stained-glass stacks show a clear title (NTD still uses glass as the base material).
     */
    private static ItemStack getLuckyItemStackFromTypeName(String typeName) {
        Object en = resolveLuckyTypeEnum(typeName);
        if (en == null) {
            return null;
        }
        ItemStack fromLb = tryHandItemFromLuckyBlockObject(en);
        ItemStack fromApi = tryHandItemFromLuckyBlockApi(en);
        ItemStack fromEnum = tryHandItemFromEnumGetItem(en);
        ItemStack picked = pickPreferredNtHandStack(fromLb, fromApi, fromEnum);
        if (picked == null) {
            return null;
        }
        try {
            return polishHandItemStack(picked.clone(), en, typeName);
        } catch (Exception ex) {
            SkyWarsReloaded.get().getLogger().log(Level.WARNING, "LuckyBlock hand item polish failed for type " + typeName, ex);
            return picked.clone();
        }
    }

    private static ItemStack tryHandItemFromEnumGetItem(Object enumConstant) {
        try {
            Method getItem = enumConstant.getClass().getMethod("getItem");
            Object result = getItem.invoke(enumConstant);
            if (result instanceof ItemStack) {
                ItemStack s = (ItemStack) result;
                return s.getType() == Material.AIR ? null : s;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static ItemStack tryHandItemFromLuckyBlockObject(Object enumConstant) {
        try {
            Method get = enumConstant.getClass().getMethod("get");
            Object luckyBlock = get.invoke(enumConstant);
            if (luckyBlock == null) {
                return null;
            }
            Class<?> lbClass = luckyBlock.getClass();
            String[] noArg = {"getItemStack", "getItem", "asItemStack", "toItemStack", "createItemStack", "getHandItem"};
            for (String name : noArg) {
                try {
                    Method m = lbClass.getMethod(name);
                    if (m.getParameterTypes().length != 0) {
                        continue;
                    }
                    Object r = m.invoke(luckyBlock);
                    if (r instanceof ItemStack) {
                        ItemStack s = (ItemStack) r;
                        if (s.getType() != Material.AIR) {
                            return s;
                        }
                    }
                } catch (NoSuchMethodException ignored) {
                }
            }
            try {
                Method m = lbClass.getMethod("getItemStack", int.class);
                Object r = m.invoke(luckyBlock, 1);
                if (r instanceof ItemStack) {
                    ItemStack s = (ItemStack) r;
                    if (s.getType() != Material.AIR) {
                        return s;
                    }
                }
            } catch (NoSuchMethodException ignored) {
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static ItemStack tryHandItemFromLuckyBlockApi(Object enumConstant) {
        try {
            Class<?> api = Class.forName("me.DenBeKKer.ntdLuckyBlock.api.LuckyBlockAPI");
            Class<?> typeClass = enumConstant.getClass();
            String[] names = {"getItemStack", "getItem", "createItem", "createItemStack"};
            for (String name : names) {
                try {
                    Method m = api.getMethod(name, typeClass);
                    Object r = m.invoke(null, enumConstant);
                    if (r instanceof ItemStack) {
                        ItemStack s = (ItemStack) r;
                        if (s.getType() != Material.AIR) {
                            return s;
                        }
                    }
                } catch (NoSuchMethodException ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static ItemStack pickPreferredNtHandStack(ItemStack a, ItemStack b, ItemStack c) {
        ItemStack fallback = null;
        for (ItemStack s : new ItemStack[] {a, b, c}) {
            if (s == null || s.getType() == Material.AIR) {
                continue;
            }
            if (fallback == null) {
                fallback = s;
            }
            if (isNtRecognizedLuckyItem(s)) {
                return s;
            }
        }
        return fallback;
    }

    private static boolean isNtRecognizedLuckyItem(ItemStack stack) {
        try {
            Class<?> api = Class.forName("me.DenBeKKer.ntdLuckyBlock.api.LuckyBlockAPI");
            Method m = api.getMethod("checkLuckyBlock", ItemStack.class);
            Object r = m.invoke(null, stack);
            return r instanceof Boolean && (Boolean) r;
        } catch (Exception ignored) {
            return true;
        }
    }

    private static ItemStack polishHandItemStack(ItemStack stack, Object enumConstant, String typeLabel) {
        ensureSettings();
        if (!settings.getBoolean("luckyBlocks.handItems.decorateDisplay", true)) {
            return stack;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        String custom = tryReflectEnumCustomName(enumConstant);
        if (!meta.hasDisplayName() && custom != null && !custom.trim().isEmpty()) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', custom.trim().replace('\u00a7', '&')));
        } else if (!meta.hasDisplayName() && isColoredGlassBlockMaterial(stack.getType())) {
            meta.setDisplayName(ChatColor.GOLD + typeLabel.toUpperCase() + ChatColor.GRAY + " lucky block");
        }
        if (settings.getBoolean("luckyBlocks.handItems.addHelpLore", true)) {
            List<String> existing = meta.getLore();
            if (existing == null || existing.isEmpty()) {
                meta.setLore(buildHandHelpLoreLines(typeLabel));
            }
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private static String tryReflectEnumCustomName(Object enumConstant) {
        try {
            Method m = enumConstant.getClass().getMethod("getCustomName");
            Object r = m.invoke(enumConstant);
            if (r instanceof String) {
                return (String) r;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static boolean isColoredGlassBlockMaterial(Material m) {
        if (m == null) {
            return false;
        }
        if (m == Material.STAINED_GLASS) {
            return true;
        }
        String n = m.name();
        return n.endsWith("_STAINED_GLASS") && !n.contains("PANE");
    }

    private static List<String> buildHandHelpLoreLines(String typeLabel) {
        List<String> fromFile = settings.getStringList("luckyBlocks.handItems.helpLore");
        if (fromFile != null && !fromFile.isEmpty()) {
            List<String> out = new ArrayList<>();
            for (String line : fromFile) {
                if (line == null) {
                    continue;
                }
                out.add(ChatColor.translateAlternateColorCodes('&',
                        line.replace("%type%", typeLabel).replace('\u00a7', '&')));
            }
            return out;
        }
        return Arrays.asList(
                ChatColor.GRAY + "NTD lucky block (same glass style as on islands).",
                ChatColor.DARK_GRAY + "Place / break for drops — not build glass.",
                ChatColor.DARK_GRAY + "Type: " + typeLabel.toUpperCase());
    }

    /**
     * Removes vanilla chest / double-chest blocks so NTD can place a clean lucky skull (avoids half-chest glitches).
     * Clears chest loot first so map chest contents are not dumped as world drops.
     */
    private static void clearChestOrTrappedBeforeLucky(Block block) {
        if (block == null) {
            return;
        }
        BlockState state = block.getState();
        if (!(state instanceof Chest)) {
            block.setType(Material.AIR);
            return;
        }
        Chest chest = (Chest) state;
        try {
            chest.getInventory().clear();
        } catch (Exception ignored) {
        }
        InventoryHolder holder = chest.getInventory().getHolder();
        if (holder instanceof DoubleChest) {
            DoubleChest dc = (DoubleChest) holder;
            Block left = ((Chest) dc.getLeftSide()).getBlock();
            Block right = ((Chest) dc.getRightSide()).getBlock();
            left.setType(Material.AIR);
            right.setType(Material.AIR);
            return;
        }
        block.setType(Material.AIR);
    }

    public static boolean isAvailable() {
        Plugin plugin = resolvePlugin();
        if (plugin == null || !plugin.isEnabled()) {
            return false;
        }
        Integer build = readBuildNumber();
        return build != null && build >= MIN_BUILD;
    }

    private static int replace(World world, ArrayList<CoordLoc> coords, String luckyType) {
        if (world == null || coords == null || coords.isEmpty()) {
            return 0;
        }
        Object luckyEnum = resolveLuckyTypeEnum(luckyType);
        if (luckyEnum == null) {
            return 0;
        }
        int changed = 0;
        for (CoordLoc c : coords) {
            if (c == null) {
                continue;
            }
            Location loc = new Location(world, c.getX(), c.getY(), c.getZ());
            Block target = loc.getBlock();
            clearChestOrTrappedBeforeLucky(target);
            if (placeLuckyBlockWithApi(target, luckyEnum)) {
                changed++;
            }
        }
        return changed;
    }

    /**
     * Resolves config string (e.g. YELLOW) to {@code LBMain.LuckyBlockType} via enum name or {@code parse(String)}.
     */
    private static Object resolveLuckyTypeEnum(String configuredTypeIn) {
        String raw = configuredTypeIn == null || configuredTypeIn.trim().isEmpty() ? "YELLOW" : configuredTypeIn.trim();
        try {
            Class<?> enumClass = Class.forName("me.DenBeKKer.ntdLuckyBlock.LBMain$LuckyBlockType");
            Object byName = parseLuckyType(enumClass, raw.toUpperCase());
            if (byName != null) {
                return byName;
            }
            try {
                Method parse = enumClass.getMethod("parse", String.class);
                Object parsed = parse.invoke(null, raw);
                if (parsed != null && enumClass.isAssignableFrom(parsed.getClass())) {
                    return parsed;
                }
            } catch (NoSuchMethodException ignored) {
            }
            return parseLuckyType(enumClass, "YELLOW");
        } catch (Exception ex) {
            SkyWarsReloaded.get().getLogger().log(Level.WARNING,
                    "LuckyBlock hook could not resolve type '" + raw + "'", ex);
            return null;
        }
    }

    /**
     * Uses NTD-LuckyBlock's placement API so block NMS/tags match the plugin (required on 1.8+).
     */
    private static boolean placeLuckyBlockWithApi(Block block, Object luckyBlockTypeEnum) {
        if (block == null || luckyBlockTypeEnum == null) {
            return false;
        }
        try {
            Class<?> apiClass = Class.forName("me.DenBeKKer.ntdLuckyBlock.api.LuckyBlockAPI");
            Class<?> typeClass = luckyBlockTypeEnum.getClass();
            Method place = apiClass.getMethod("placeLuckyBlock", Block.class, typeClass);
            place.invoke(null, block, luckyBlockTypeEnum);
            return true;
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            SkyWarsReloaded.get().getLogger().log(Level.WARNING,
                    "LuckyBlock API place failed at " + block.getLocation() + ": " + cause.getMessage());
            if (SkyWarsReloaded.getCfg().debugEnabled()) {
                SkyWarsReloaded.get().getLogger().log(Level.FINE, "LuckyBlock place stack trace", cause);
            }
            return false;
        } catch (Exception ex) {
            SkyWarsReloaded.get().getLogger().log(Level.WARNING,
                    "LuckyBlock API place error at " + block.getLocation(), ex);
            return false;
        }
    }

    private static Integer readBuildNumber() {
        try {
            Class<?> lbMain = Class.forName("me.DenBeKKer.ntdLuckyBlock.LBMain");
            Method getBuild = lbMain.getMethod("getBuild");
            Object build = getBuild.invoke(null);
            if (build instanceof Number) {
                return ((Number) build).intValue();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Plugin resolvePlugin() {
        for (String pluginName : PLUGIN_NAMES) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
            if (plugin != null) {
                return plugin;
            }
        }
        return null;
    }

    private static void registerConfiguredDropForConfiguredTypes(LuckyProfile profile) throws Exception {
        if (!isDropsEnabled(profile)) {
            return;
        }
        Class<?> apiClass = Class.forName("me.DenBeKKer.ntdLuckyBlock.api.LuckyBlockAPI");
        Class<?> typeClass = Class.forName("me.DenBeKKer.ntdLuckyBlock.LBMain$LuckyBlockType");
        Class<?> dropClass = Class.forName("me.DenBeKKer.ntdLuckyBlock.variables.LuckyDrop");
        Method addLuckyDrop = apiClass.getMethod("addLuckyDrop", typeClass, dropClass);

        Set<String> targetTypes = new HashSet<>(Arrays.asList(
                getIslandType(profile).trim().toUpperCase(),
                getCenterType(profile).trim().toUpperCase()
        ));

        for (String typeName : targetTypes) {
            Object luckyType = parseLuckyType(typeClass, typeName);
            if (luckyType == null) {
                continue;
            }
            Object dropProxy = Proxy.newProxyInstance(
                    dropClass.getClassLoader(),
                    new Class<?>[] { dropClass },
                    new ConfiguredDropHandler(profile));
            try {
                addLuckyDrop.invoke(null, luckyType, dropProxy);
            } catch (Exception ex) {
                if (SkyWarsReloaded.getCfg().debugEnabled()) {
                    SkyWarsReloaded.get().getLogger().log(Level.WARNING,
                            "Could not register LuckyBlock drop for type " + typeName, ex);
                }
            }
        }
    }

    /**
     * Registers extra NTD drops that grant random items from SkyWars {@code chest.yml} / {@code opchest.yml} style tables
     * so lucky mode is not limited to NTD factory RNG alone.
     */
    private static void registerChestLootDropHandlers(LuckyProfile profile) throws Exception {
        Class<?> apiClass = Class.forName("me.DenBeKKer.ntdLuckyBlock.api.LuckyBlockAPI");
        Class<?> typeClass = Class.forName("me.DenBeKKer.ntdLuckyBlock.LBMain$LuckyBlockType");
        Class<?> dropClass = Class.forName("me.DenBeKKer.ntdLuckyBlock.variables.LuckyDrop");
        Method addLuckyDrop = apiClass.getMethod("addLuckyDrop", typeClass, dropClass);

        Set<String> targetTypes = new HashSet<>(Arrays.asList(
                getIslandType(profile).trim().toUpperCase(),
                getCenterType(profile).trim().toUpperCase()
        ));

        for (String typeName : targetTypes) {
            Object luckyType = parseLuckyType(typeClass, typeName);
            if (luckyType == null) {
                continue;
            }
            Object dropProxy = Proxy.newProxyInstance(
                    dropClass.getClassLoader(),
                    new Class<?>[] { dropClass },
                    new ChestLootDropHandler(profile));
            try {
                addLuckyDrop.invoke(null, luckyType, dropProxy);
            } catch (Exception ex) {
                if (SkyWarsReloaded.getCfg().debugEnabled()) {
                    SkyWarsReloaded.get().getLogger().log(Level.WARNING,
                            "Could not register chest-loot LuckyBlock drop for type " + typeName, ex);
                }
            }
        }
    }

    private static boolean isBlockOnCenterChestCoords(GameMap map, Block block) {
        if (map == null || block == null || map.getCenterChests() == null) {
            return false;
        }
        World w = map.getCurrentWorld();
        if (w == null || !w.equals(block.getWorld())) {
            return false;
        }
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        for (CoordLoc c : map.getCenterChests()) {
            if (c != null && c.getX() == x && c.getY() == y && c.getZ() == z) {
                return true;
            }
        }
        return false;
    }

    private static int getChestLootTriggerChancePercent() {
        ensureSettings();
        if (settings.getBoolean("luckyBlocks.chestLootDrops.skywarsOnlyDrops", false)) {
            return 100;
        }
        return Math.max(0, Math.min(100, settings.getInt("luckyBlocks.chestLootDrops.triggerChancePercent", 55)));
    }

    private static int getChestLootUnluckyChancePercent() {
        ensureSettings();
        return Math.max(0, Math.min(100, settings.getInt("luckyBlocks.chestLootDrops.unluckyChancePercent", 32)));
    }

    private static int getBadLootItemsMin() {
        ensureSettings();
        int legacy = settings.getInt("luckyBlocks.chestLootDrops.itemsMin", 1);
        return Math.max(0, Math.min(27, settings.getInt("luckyBlocks.chestLootDrops.badLootItemsMin", legacy)));
    }

    private static int getBadLootItemsMax() {
        ensureSettings();
        int legacy = settings.getInt("luckyBlocks.chestLootDrops.itemsMax", 2);
        return Math.max(0, Math.min(27, settings.getInt("luckyBlocks.chestLootDrops.badLootItemsMax", legacy)));
    }

    private static int getGoodLootItemsMin() {
        ensureSettings();
        return Math.max(0, Math.min(27, settings.getInt("luckyBlocks.chestLootDrops.goodLootItemsMin", 2)));
    }

    private static int getGoodLootItemsMax() {
        ensureSettings();
        return Math.max(0, Math.min(27, settings.getInt("luckyBlocks.chestLootDrops.goodLootItemsMax", 6)));
    }

    private static int getGoodLootExtraPullPercent() {
        ensureSettings();
        return Math.max(0, Math.min(100, settings.getInt("luckyBlocks.chestLootDrops.goodLootExtraPullPercent", 30)));
    }

    private static int getTwistSusWeight() {
        ensureSettings();
        return Math.max(0, settings.getInt("luckyBlocks.chestLootDrops.twistSusWeight", 22));
    }

    private static int getTwistBadLootWeight() {
        ensureSettings();
        return Math.max(0, settings.getInt("luckyBlocks.chestLootDrops.twistBadLootWeight", 33));
    }

    private static int getTwistGoodLootWeight() {
        ensureSettings();
        return Math.max(0, settings.getInt("luckyBlocks.chestLootDrops.twistGoodLootWeight", 45));
    }

    private static int getTwistUltraWeight() {
        ensureSettings();
        return Math.max(0, settings.getInt("luckyBlocks.chestLootDrops.twistUltraWeight", 12));
    }

    private static int getUltraLootItemsMin() {
        ensureSettings();
        return Math.max(0, Math.min(27, settings.getInt("luckyBlocks.chestLootDrops.ultraLootItemsMin", 3)));
    }

    private static int getUltraLootItemsMax() {
        ensureSettings();
        return Math.max(0, Math.min(27, settings.getInt("luckyBlocks.chestLootDrops.ultraLootItemsMax", 7)));
    }

    private static int getUltraLootExtraPullPercent() {
        ensureSettings();
        return Math.max(0, Math.min(100, settings.getInt("luckyBlocks.chestLootDrops.ultraLootExtraPullPercent", 55)));
    }

    private static String getUltraTwistMessage() {
        ensureSettings();
        String raw = settings.getString("luckyBlocks.chestLootDrops.ultraMessage",
                "&6&lJackpot! &7Ultra loot from the OP chest tables.");
        return raw == null ? "" : raw;
    }

    private static boolean isUltraBuffEffectsEnabled() {
        ensureSettings();
        return settings.getBoolean("luckyBlocks.chestLootDrops.ultraBuffEffects.enabled", true);
    }

    private static List<String> getUltraBuffPool() {
        ensureSettings();
        List<String> fromFile = settings.getStringList("luckyBlocks.chestLootDrops.ultraBuffEffects.pool");
        if (fromFile != null && !fromFile.isEmpty()) {
            return fromFile;
        }
        return Arrays.asList(
                "speed",
                "strength",
                "absorption",
                "regeneration",
                "resistance",
                "fire_resistance",
                "instant_health"
        );
    }

    private static String getChestLootPlayerMessage() {
        ensureSettings();
        String raw = settings.getString("luckyBlocks.chestLootDrops.playerMessage", "");
        return raw == null ? "" : raw;
    }

    private static String getSusTwistMessage() {
        ensureSettings();
        String raw = settings.getString("luckyBlocks.chestLootDrops.susMessage", "&c&lSus! &7That block had other plans...");
        return raw == null ? "" : raw;
    }

    private static boolean isSusEffectsEnabled() {
        ensureSettings();
        return settings.getBoolean("luckyBlocks.chestLootDrops.susEffects.enabled", true);
    }

    private static boolean isSusSilverfishAllowed() {
        ensureSettings();
        return settings.getBoolean("luckyBlocks.chestLootDrops.susEffects.allowSilverfish", false);
    }

    private static double getSusTinyDamageHearts() {
        ensureSettings();
        double v = settings.getDouble("luckyBlocks.chestLootDrops.susEffects.tinyDamageHearts", 1.0D);
        return Math.max(0.0D, Math.min(8.0D, v));
    }

    private static List<String> getSusEffectPool() {
        ensureSettings();
        List<String> fromFile = settings.getStringList("luckyBlocks.chestLootDrops.susEffects.pool");
        if (fromFile != null && !fromFile.isEmpty()) {
            return fromFile;
        }
        return Arrays.asList(
                "lightning_effect",
                "confusion",
                "slowness",
                "hunger",
                "weakness",
                "sound_boom",
                "tiny_damage"
        );
    }

    /**
     * Weighted sus / bad / good / ultra(OP) twist when a lucky block triggers SkyWars chest integration.
     * Ultra always rolls OP chest vote tier loot tables plus optional buff effects.
     */
    private static void runChestLootTwist(Player player, Block block, GameMap map, boolean centerTable, LuckyProfile profile) {
        int susW = getTwistSusWeight();
        int badW = getTwistBadLootWeight();
        int goodW = getTwistGoodLootWeight();
        int ultraW = getTwistUltraWeight();
        int sum = susW + badW + goodW + ultraW;
        Vote luckyVote = profile == LuckyProfile.OP ? Vote.CHESTOP : Vote.CHESTNORMAL;

        if (sum <= 0) {
            Vote tier = RNG.nextInt(100) < getChestLootUnluckyChancePercent() ? Vote.CHESTBASIC : luckyVote;
            if (SkyWarsReloaded.getCM() != null) {
                SkyWarsReloaded.getCM().giveSkyWarsChestLoot(
                        player, tier, centerTable, getBadLootItemsMin(), getBadLootItemsMax());
            }
            return;
        }

        int pick = RNG.nextInt(sum);
        if (pick < susW) {
            if (isSusEffectsEnabled()) {
                SusTwist.applyRandom(player, block);
            }
            String sm = getSusTwistMessage();
            if (!sm.trim().isEmpty()) {
                player.sendMessage(sm.replace("&", "\u00a7").replace("%player%", player.getName()));
            }
            return;
        }
        pick -= susW;
        if (pick < badW) {
            if (SkyWarsReloaded.getCM() != null) {
                SkyWarsReloaded.getCM().giveSkyWarsChestLoot(
                        player, Vote.CHESTBASIC, centerTable, getBadLootItemsMin(), getBadLootItemsMax());
            }
            return;
        }
        pick -= badW;
        if (pick < goodW) {
            if (SkyWarsReloaded.getCM() != null) {
                SkyWarsReloaded.getCM().giveSkyWarsChestLoot(
                        player, luckyVote, centerTable, getGoodLootItemsMin(), getGoodLootItemsMax());
                if (RNG.nextInt(100) < getGoodLootExtraPullPercent()) {
                    SkyWarsReloaded.getCM().giveSkyWarsChestLoot(player, luckyVote, centerTable, 1, 2);
                }
            }
            String gm = getChestLootPlayerMessage();
            if (!gm.trim().isEmpty()) {
                player.sendMessage(gm.replace("&", "\u00a7").replace("%player%", player.getName()));
            }
            return;
        }
        if (SkyWarsReloaded.getCM() != null) {
            SkyWarsReloaded.getCM().giveSkyWarsChestLoot(
                    player, Vote.CHESTOP, centerTable, getUltraLootItemsMin(), getUltraLootItemsMax());
            if (RNG.nextInt(100) < getUltraLootExtraPullPercent()) {
                SkyWarsReloaded.getCM().giveSkyWarsChestLoot(player, Vote.CHESTOP, centerTable, 1, 3);
            }
        }
        if (isUltraBuffEffectsEnabled()) {
            UltraBuffTwist.applyRandom(player, block);
        }
        String um = getUltraTwistMessage();
        if (!um.trim().isEmpty()) {
            player.sendMessage(um.replace("&", "\u00a7").replace("%player%", player.getName()));
        }
    }

    /** Short positive potion rolls paired with the ultra OP chest twist. */
    private static final class UltraBuffTwist {
        private UltraBuffTwist() {
        }

        static void applyRandom(Player player, Block block) {
            List<String> pool = new ArrayList<>(getUltraBuffPool());
            pool.removeIf(s -> s == null || s.trim().isEmpty());
            if (pool.isEmpty()) {
                return;
            }
            String id = pool.get(RNG.nextInt(pool.size())).trim().toLowerCase(Locale.ROOT);
            switch (id) {
                case "speed":
                case "swiftness":
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 12, 1, false, true));
                    break;
                case "strength":
                case "strength_buff":
                    player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 12, 0, false, true));
                    break;
                case "absorption":
                    player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 30, 0, false, true));
                    break;
                case "regeneration":
                case "regen":
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 5, 0, false, true));
                    break;
                case "resistance":
                case "res":
                    player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 10, 0, false, true));
                    break;
                case "fire_resistance":
                case "fire_res":
                    player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 30, 0, false, true));
                    break;
                case "jump":
                case "jump_boost":
                    player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 20 * 15, 1, false, true));
                    break;
                case "haste":
                case "fast_digging":
                    player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 20 * 20, 1, false, true));
                    break;
                case "instant_health":
                case "heal":
                case "health_boost":
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HEAL, 1, 0, false, true));
                    break;
                case "saturation":
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 20 * 3, 0, false, true));
                    break;
                default:
                    break;
            }
            World w = block.getWorld();
            if (w != null) {
                try {
                    w.playSound(player.getLocation(), Sound.valueOf("ENTITY_PLAYER_LEVELUP"), 0.85f, 1.25f);
                } catch (Exception ignored) {
                    w.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.85f, 1.4f);
                }
            }
        }
    }

    private static final class SusTwist {
        private SusTwist() {
        }

        static void applyRandom(Player player, Block block) {
            List<String> pool = new ArrayList<>(getSusEffectPool());
            pool.removeIf(s -> s == null || s.trim().isEmpty());
            if (pool.isEmpty()) {
                return;
            }
            String id = pool.get(RNG.nextInt(pool.size())).trim().toLowerCase();
            World world = block.getWorld();
            Location at = player.getLocation().clone();
            switch (id) {
                case "lightning":
                case "lightning_effect":
                    world.strikeLightningEffect(at);
                    break;
                case "confusion":
                case "nausea":
                    player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 20 * 6, 0, false, true));
                    break;
                case "slowness":
                case "slow":
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 5, 1, false, true));
                    break;
                case "hunger":
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 20 * 8, 0, false, true));
                    break;
                case "weakness":
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 5, 0, false, true));
                    break;
                case "blindness":
                case "blind":
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 3, 0, false, true));
                    break;
                case "levitation":
                    try {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 20 * 2, 0, false, true));
                    } catch (Throwable ignored) {
                        // Older API without levitation
                    }
                    break;
                case "sound_boom":
                case "boom_sound":
                    try {
                        world.playSound(at, Sound.valueOf("ENTITY_GENERIC_EXPLODE"), 0.9f, 1.2f);
                    } catch (Exception ignored) {
                        world.playSound(at, Sound.ENTITY_CREEPER_PRIMED, 1f, 0.6f);
                    }
                    break;
                case "silverfish":
                case "silverfish_swarm":
                    if (isSusSilverfishAllowed() && world != null) {
                        for (int i = 0; i < 1 + RNG.nextInt(2); i++) {
                            Location spawn = at.clone().add(RNG.nextDouble() - 0.5, 0.1, RNG.nextDouble() - 0.5);
                            if (world.getBlockAt(spawn).getType().isSolid()) {
                                spawn.add(0, 1, 0);
                            }
                            world.spawnEntity(spawn, EntityType.SILVERFISH);
                        }
                    }
                    break;
                case "tiny_damage":
                case "pinch_damage": {
                    double hearts = getSusTinyDamageHearts();
                    if (hearts > 0) {
                        double dmg = hearts * 2.0D;
                        double nh = Math.max(0.5D, player.getHealth() - dmg);
                        player.setHealth(nh);
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }

    private static Object parseLuckyType(Class<?> typeClass, String typeName) {
        try {
            @SuppressWarnings("unchecked")
            Object value = Enum.valueOf((Class<Enum>) typeClass.asSubclass(Enum.class), typeName);
            return value;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static final class ConfiguredDropHandler implements InvocationHandler {
        private final LuckyProfile profile;

        private ConfiguredDropHandler(LuckyProfile profile) {
            this.profile = profile;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (method == null || !"execute".equals(method.getName()) || args == null || args.length < 2 || args[1] == null) {
                return null;
            }
            if (RNG.nextInt(100) >= getDropChance(profile)) {
                return null;
            }
            org.bukkit.entity.Player player = (org.bukkit.entity.Player) args[1];
            for (String line : getDropMessages(profile)) {
                if (line != null && !line.trim().isEmpty()) {
                    player.sendMessage(line.replace("&", "\u00a7").replace("%player%", player.getName()));
                }
            }
            int repeats = getDropCommandRepeats(profile);
            for (int i = 0; i < repeats; i++) {
                for (String command : getDropCommands(profile)) {
                    if (command == null || command.trim().isEmpty()) {
                        continue;
                    }
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
                }
            }
            return null;
        }
    }

    private static final class ChestLootDropHandler implements InvocationHandler {
        private final LuckyProfile profile;

        private ChestLootDropHandler(LuckyProfile profile) {
            this.profile = profile;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (method == null || !"execute".equals(method.getName()) || args == null || args.length < 1) {
                return null;
            }
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length < 2 || !Player.class.isAssignableFrom(paramTypes[1])) {
                return null;
            }
            if (!(args[0] instanceof Block) || !(args[1] instanceof Player)) {
                return null;
            }
            if (RNG.nextInt(100) >= getChestLootTriggerChancePercent()) {
                return null;
            }
            Player player = (Player) args[1];
            GameMap map = MatchManager.get().getPlayerMap(player);
            if (map == null || !map.isLuckyModeEnabled()) {
                return null;
            }
            Block block = (Block) args[0];
            boolean centerTable = isBlockOnCenterChestCoords(map, block);
            runChestLootTwist(player, block, map, centerTable, profile);
            return null;
        }
    }

    /**
     * Reloads {@code luckyblocks.yml} from disk and merges defaults from the jar so new keys work without deleting the server file.
     */
    public static void reloadLuckyBlocksYaml() {
        File file = ChestStorageLayout.resolveDataFile(SkyWarsReloaded.get(), "luckyblocks.yml", true);
        YamlConfiguration user = YamlConfiguration.loadConfiguration(file);
        InputStream defStream = SkyWarsReloaded.get()
                .getResource(ChestStorageLayout.layoutRelativePath("luckyblocks.yml"));
        if (defStream != null) {
            try {
                YamlConfiguration def = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defStream, StandardCharsets.UTF_8));
                user.setDefaults(def);
                user.options().copyDefaults(true);
            } finally {
                try {
                    defStream.close();
                } catch (Exception ignored) {
                }
            }
        }
        settings = user;
    }

    private static void ensureSettings() {
        if (settings == null) {
            reloadLuckyBlocksYaml();
        }
    }

    private static String section(LuckyProfile profile) {
        return profile == LuckyProfile.OP ? "luckyBlockOp" : "luckyBlockBasic";
    }

    /** Read from {@code luckyblocks.yml}; exposed for {@link com.walrusone.skywarsreloaded.config.Config}. */
    public static boolean isReplaceCenterChests() {
        ensureSettings();
        return settings.getBoolean("luckyBlocks.replaceCenterChests", true);
    }

    /** Read from {@code luckyblocks.yml}; exposed for {@link com.walrusone.skywarsreloaded.config.Config}. */
    public static String getIslandType(LuckyProfile profile) {
        ensureSettings();
        return settings.getString("luckyBlocks." + section(profile) + ".islandType", profile == LuckyProfile.OP ? "RED" : "YELLOW");
    }

    /** Read from {@code luckyblocks.yml}; exposed for {@link com.walrusone.skywarsreloaded.config.Config}. */
    public static String getCenterType(LuckyProfile profile) {
        ensureSettings();
        return settings.getString("luckyBlocks." + section(profile) + ".centerType", getIslandType(profile));
    }

    /** Read from {@code luckyblocks.yml}; exposed for {@link com.walrusone.skywarsreloaded.config.Config}. */
    public static boolean isDropsEnabled(LuckyProfile profile) {
        ensureSettings();
        return settings.getBoolean("luckyBlocks." + section(profile) + ".drops.enabled", true);
    }

    /** Read from {@code luckyblocks.yml}; exposed for {@link com.walrusone.skywarsreloaded.config.Config}. */
    public static int getDropChance(LuckyProfile profile) {
        ensureSettings();
        int value = settings.getInt("luckyBlocks." + section(profile) + ".drops.chancePercent", 100);
        return Math.max(0, Math.min(100, value));
    }

    /** Read from {@code luckyblocks.yml}; exposed for {@link com.walrusone.skywarsreloaded.config.Config}. */
    public static java.util.List<String> getDropMessages(LuckyProfile profile) {
        ensureSettings();
        return settings.getStringList("luckyBlocks." + section(profile) + ".drops.messages");
    }

    /** Read from {@code luckyblocks.yml}; exposed for {@link com.walrusone.skywarsreloaded.config.Config}. */
    public static java.util.List<String> getDropCommands(LuckyProfile profile) {
        ensureSettings();
        return settings.getStringList("luckyBlocks." + section(profile) + ".drops.commands");
    }

    /**
     * How many times to run {@code drops.commands} per successful drop (1 = default). Only used when {@code registerCustomDrops} is true.
     */
    private static int getDropCommandRepeats(LuckyProfile profile) {
        ensureSettings();
        int value = settings.getInt("luckyBlocks." + section(profile) + ".drops.commandRepeats", 1);
        return Math.max(1, Math.min(10, value));
    }
}
