package com.walrusone.skywarsreloaded.utilities.holograms;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.enums.MatchState;
import com.walrusone.skywarsreloaded.enums.Vote;
import com.walrusone.skywarsreloaded.events.SkyWarsMatchStateChangeEvent;
import com.walrusone.skywarsreloaded.game.GameMap;
import com.walrusone.skywarsreloaded.matchevents.MatchEvent;
import com.walrusone.skywarsreloaded.game.TeamCard;
import com.walrusone.skywarsreloaded.menus.gameoptions.objects.CoordLoc;
import com.walrusone.skywarsreloaded.utilities.Messaging;
import com.walrusone.skywarsreloaded.utilities.Util;
import eu.decentsoftware.holograms.api.DHAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Optional island/center chest refill countdown above chests (DecentHolograms).
 */
public final class ChestRefillHologramManager implements Listener {

    private static ChestRefillHologramManager instance;
    private static BukkitTask tickTask;

    private final JavaPlugin plugin;
    private final Map<String, TrackedChest> tracked = new ConcurrentHashMap<>();
    private HoloBackend backend = new NoopBackend();

    private ChestRefillHologramManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static void init(JavaPlugin plugin) {
        if (!SkyWarsReloaded.getCfg().isChestRefillStatusHologramsEnabled()) {
            plugin.getLogger().info("Chest refill holograms: disabled in config.yml (chests.refillStatusHolograms.enabled).");
            return;
        }
        if (instance != null) {
            shutdown();
        }
        instance = new ChestRefillHologramManager(plugin);
        instance.backend = instance.resolveBackend();
        Bukkit.getPluginManager().registerEvents(instance, plugin);
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, instance::tick, 20L, 20L);
        // DecentHolograms may enable after SkyWars; re-check once next tick.
        Bukkit.getScheduler().runTaskLater(plugin, instance::upgradeBackendIfDecentHologramsAvailable, 1L);
        boolean dh = Bukkit.getPluginManager().isPluginEnabled("DecentHolograms");
        plugin.getLogger().info("Chest refill holograms: feature on, cooldown "
                + SkyWarsReloaded.getCfg().getChestRefillHologramCooldownSeconds() + "s, DecentHolograms "
                + (dh ? "detected (floating text active)." : "NOT running — install/enable DecentHolograms for text."));
    }

    public static void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        if (instance != null) {
            HandlerList.unregisterAll(instance);
            instance.clearAll();
            instance = null;
        }
    }

    public static void clearForMap(GameMap map) {
        if (instance == null) {
            return;
        }
        Iterator<Map.Entry<String, TrackedChest>> it = instance.tracked.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, TrackedChest> e = it.next();
            if (e.getValue().mapName.equals(map.getName())) {
                instance.setChestVisualOpen(e.getValue(), false);
                instance.backend.delete(e.getKey());
                it.remove();
            }
        }
    }

    private HoloBackend resolveBackend() {
        if (Bukkit.getPluginManager().isPluginEnabled("DecentHolograms")) {
            return new DecentBackend();
        }
        SkyWarsReloaded.get().getLogger().info("Chest refill holograms enabled but DecentHolograms is not loaded. Countdown/refill still runs; install DecentHolograms for floating text.");
        return new NoopBackend();
    }

    /**
     * Softdepend does not guarantee load order; DecentHolograms can enable after we first resolved the backend.
     */
    private void upgradeBackendIfDecentHologramsAvailable() {
        if (!(backend instanceof NoopBackend)) {
            return;
        }
        if (!Bukkit.getPluginManager().isPluginEnabled("DecentHolograms")) {
            return;
        }
        backend = new DecentBackend();
        plugin.getLogger().info("DecentHolograms is available; chest refill holograms are now active.");
    }

    @EventHandler
    public void onDecentHologramsEnabled(PluginEnableEvent event) {
        if (!"DecentHolograms".equalsIgnoreCase(event.getPlugin().getName())) {
            return;
        }
        if (instance == null) {
            return;
        }
        instance.upgradeBackendIfDecentHologramsAvailable();
    }

    private void tick() {
        if (!SkyWarsReloaded.getCfg().isChestRefillStatusHologramsEnabled()) {
            return;
        }
        upgradeBackendIfDecentHologramsAvailable();
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, TrackedChest>> it = tracked.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, TrackedChest> en = it.next();
            TrackedChest t = en.getValue();
            GameMap map = findMapByName(t.mapName);
            if (map == null || map.getMatchState() != MatchState.PLAYING) {
                setChestVisualOpen(t, false);
                backend.delete(t.holoId);
                it.remove();
                continue;
            }
            if (t.refillAtMillis <= 0) {
                continue;
            }
            if (now >= t.refillAtMillis) {
                refillChest(map, t);
                setChestVisualOpen(t, false);
                t.refillAtMillis = 0;
                t.lastShownSeconds = null;
                if (SkyWarsReloaded.getCfg().isChestRefillHologramAnnounceRefilled()) {
                    World w = map.getCurrentWorld();
                    if (w != null) {
                        Location at = new Location(w, t.bx + 0.5, t.by + 0.5, t.bz + 0.5);
                        announceNear(at, map,
                                new Messaging.MessageFormatter().format("game.chest-refill-hologram.announce-refilled"));
                    }
                }
                if (SkyWarsReloaded.getCfg().isChestRefillHologramShowWhenFull()) {
                    List<String> ready = Collections.singletonList(
                            new Messaging.MessageFormatter().format("game.chest-refill-hologram.line-ready"));
                    backend.setLines(t.holoLoc, t.holoId, ready);
                } else {
                    backend.delete(t.holoId);
                }
            } else {
                int seconds = (int) Math.ceil((t.refillAtMillis - now) / 1000.0);
                if (t.lastShownSeconds == null || t.lastShownSeconds != seconds) {
                    List<String> lines = Collections.singletonList(
                            new Messaging.MessageFormatter()
                                    .setVariable("seconds", Integer.toString(seconds))
                                    .setVariable("time", Util.get().secondsToTimeString(seconds))
                                    .format("game.chest-refill-hologram.line-countdown"));
                    backend.setLines(t.holoLoc, t.holoId, lines);
                    t.lastShownSeconds = seconds;
                }
                if (SkyWarsReloaded.getCfg().isChestRefillHologramKeepChestOpen()) {
                    setChestVisualOpen(t, true);
                }
            }
        }
    }

    private GameMap findMapByName(String name) {
        for (GameMap m : SkyWarsReloaded.getGameMapMgr().getMapsCopy()) {
            if (m.getName().equals(name)) {
                return m;
            }
        }
        return null;
    }

    private void refillChest(GameMap map, TrackedChest t) {
        World w = map.getCurrentWorld();
        if (w == null) {
            return;
        }
        Block b = w.getBlockAt(t.bx, t.by, t.bz);
        if (!(b.getState() instanceof Chest)) {
            return;
        }
        Chest chest = (Chest) b.getState();
        InventoryHolder ih = chest.getInventory().getHolder();
        Vote vote = resolveChestVote(map);
        if (ih instanceof DoubleChest) {
            SkyWarsReloaded.getCM().populateChest((DoubleChest) ih, vote, t.center);
        } else {
            SkyWarsReloaded.getCM().populateChest(chest, vote, t.center);
        }
    }

    private static Vote resolveChestVote(GameMap map) {
        Vote v = map.getChestOption().getChestVoteResult();
        if (v == Vote.CHESTRANDOM) {
            return Vote.getRandom("chest");
        }
        return v;
    }

    private void clearAll() {
        for (TrackedChest t : tracked.values()) {
            setChestVisualOpen(t, false);
            backend.delete(t.holoId);
        }
        tracked.clear();
    }

    @EventHandler
    public void onMatchState(SkyWarsMatchStateChangeEvent event) {
        if (event.isBungeecord()) {
            return;
        }
        try {
            GameMap map = event.getGameMap();
            if (event.getState() != MatchState.PLAYING) {
                clearForMap(map);
            }
        } catch (Exception ignored) {
            // bungee constructor path
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!SkyWarsReloaded.getCfg().isChestRefillStatusHologramsEnabled()) {
            return;
        }
        Inventory inv = event.getInventory();
        Block canonical = canonicalChestBlock(inv.getHolder());
        if (canonical == null) {
            return;
        }
        final Location blockLoc = canonical.getLocation();
        Bukkit.getScheduler().runTask(plugin, () -> {
            // Start countdown on first open (Hypixel-like), not only when chest becomes fully empty.
            beginRefillTrackingAfterEmptyChest(blockLoc, false);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!SkyWarsReloaded.getCfg().isChestRefillStatusHologramsEnabled()) {
            return;
        }
        Inventory inv = event.getInventory();
        InventoryHolder holder = inv.getHolder();
        Block canonical = canonicalChestBlock(holder);
        if (canonical == null) {
            return;
        }
        final Location blockLoc = canonical.getLocation();
        final Inventory invFinal = inv;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!invFinal.getViewers().isEmpty()) {
                return;
            }
            Inventory fresh = readChestInventoryFromWorld(blockLoc);
            if (fresh == null || !isInventoryEmpty(fresh)) {
                return;
            }
            beginRefillTrackingAfterEmptyChest(blockLoc, true);
        });
    }

    /**
     * @param announceIfConfigured chat message when {@code announceLootEmpty} is on (intended for close-after-loot)
     */
    private void beginRefillTrackingAfterEmptyChest(Location blockLoc, boolean announceIfConfigured) {
        upgradeBackendIfDecentHologramsAvailable();
        GameMap map = findPlayingMapContainingChest(blockLoc);
        if (map == null) {
            if (SkyWarsReloaded.getCfg().debugEnabled()) {
                plugin.getLogger().info("ChestRefill: no PLAYING map matched chest at "
                        + blockLoc.getWorld().getName() + " " + blockLoc.getBlockX() + "," + blockLoc.getBlockY() + "," + blockLoc.getBlockZ()
                        + " (empty chest + close/open during PVP only).");
            }
            return;
        }
        if (!isChestRefillEventEnabled(map)) {
            if (announceIfConfigured && SkyWarsReloaded.getCfg().isChestRefillHologramAnnounceEmpty()) {
                Location at = blockLoc.clone().add(0.5, 0.5, 0.5);
                announceNear(at, map, new Messaging.MessageFormatter().format("game.chest-refill-hologram.announce-empty-no-refill"));
            }
            return;
        }
        boolean center = isCenterChest(map, blockLoc);
        String holoId = holoId(map.getName(), blockLoc);
        TrackedChest existing = tracked.get(holoId);
        if (existing != null && existing.refillAtMillis > System.currentTimeMillis()) {
            return;
        }
        Location holoLoc = blockLoc.clone().add(0.5, SkyWarsReloaded.getCfg().getChestRefillHologramOffsetY(), 0.5);
        int refillSeconds = resolveRefillCountdownSeconds(map);
        long refillAt = System.currentTimeMillis() + refillSeconds * 1000L;
        int bx = blockLoc.getBlockX();
        int by = blockLoc.getBlockY();
        int bz = blockLoc.getBlockZ();
        TrackedChest t = new TrackedChest(map.getName(), holoId, holoLoc, bx, by, bz, center, refillAt);
        tracked.put(holoId, t);
        setChestVisualOpen(t, SkyWarsReloaded.getCfg().isChestRefillHologramKeepChestOpen());

        int sec = refillSeconds;
        List<String> lines = Collections.singletonList(
                new Messaging.MessageFormatter()
                        .setVariable("seconds", Integer.toString(sec))
                        .setVariable("time", Util.get().secondsToTimeString(sec))
                        .format("game.chest-refill-hologram.line-countdown"));
        backend.setLines(holoLoc, holoId, lines);

        if (announceIfConfigured && SkyWarsReloaded.getCfg().isChestRefillHologramAnnounceEmpty()) {
            Location at = blockLoc.clone().add(0.5, 0.5, 0.5);
            String msg = new Messaging.MessageFormatter()
                    .setVariable("seconds", Integer.toString(sec))
                    .setVariable("time", Util.get().secondsToTimeString(sec))
                    .format("game.chest-refill-hologram.announce-empty");
            announceNear(at, map, msg);
        }
    }

    private void announceNear(Location chestBlockCenter, GameMap map, String message) {
        double r = SkyWarsReloaded.getCfg().getChestRefillHologramMessageRadius();
        double r2 = r * r;
        String wn = chestBlockCenter.getWorld().getName();
        for (Player p : map.getAllPlayers()) {
            if (p.getWorld().getName().equals(wn) && p.getLocation().distanceSquared(chestBlockCenter) <= r2) {
                p.sendMessage(message);
            }
        }
    }

    private static String holoId(String mapName, Location blockLoc) {
        String safe = mapName.replaceAll("[^a-zA-Z0-9_]", "_");
        return "swr_cf_" + safe + "_" + blockLoc.getBlockX() + "_" + blockLoc.getBlockY() + "_" + blockLoc.getBlockZ();
    }

    private static Inventory readChestInventoryFromWorld(Location blockLoc) {
        World w = blockLoc.getWorld();
        if (w == null) {
            return null;
        }
        Block b = w.getBlockAt(blockLoc.getBlockX(), blockLoc.getBlockY(), blockLoc.getBlockZ());
        if (!(b.getState() instanceof Chest)) {
            return null;
        }
        return ((Chest) b.getState()).getInventory();
    }

    private static Location normalizeHologramWorld(Location loc) {
        Location o = loc.clone();
        if (o.getWorld() != null) {
            World w = Bukkit.getWorld(o.getWorld().getName());
            if (w != null) {
                o.setWorld(w);
            }
        }
        return o;
    }

    private static void invokeDecentShowAll(Object hologram) {
        if (hologram == null) {
            return;
        }
        try {
            Method m = hologram.getClass().getMethod("showAll");
            m.invoke(hologram);
        } catch (NoSuchMethodException ignored) {
        } catch (ReflectiveOperationException ignored) {
        }
    }

    /**
     * On 1.8, DecentHolograms defers line {@code show} to the next tick to avoid entity id flicker; a follow-up
     * {@code showAll}/{@code show(player,0)} next tick fixes holograms that never appeared after API create/update.
     */
    private static void scheduleDecentVisibilityRefresh(Location holoLoc, String id) {
        JavaPlugin pl = SkyWarsReloaded.get();
        Location norm = normalizeHologramWorld(holoLoc.clone());
        long delay = SkyWarsReloaded.getNMS().getVersion() < 9 ? 2L : 1L;
        Bukkit.getScheduler().runTaskLater(pl, () -> {
            try {
                eu.decentsoftware.holograms.api.holograms.Hologram h = DHAPI.getHologram(id);
                if (h == null) {
                    return;
                }
                invokeDecentShowAll(h);
                Method show = h.getClass().getMethod("show", Player.class, int.class);
                World w = norm.getWorld();
                if (w == null) {
                    return;
                }
                for (Player p : w.getPlayers()) {
                    try {
                        show.invoke(h, p, 0);
                    } catch (ReflectiveOperationException ignored) {
                    }
                }
            } catch (Throwable ignored) {
            }
        }, delay);
    }

    private static Block canonicalChestBlock(InventoryHolder holder) {
        if (holder instanceof DoubleChest) {
            Chest left = (Chest) ((DoubleChest) holder).getLeftSide();
            return left.getBlock();
        }
        if (holder instanceof Chest) {
            return ((Chest) holder).getBlock();
        }
        return null;
    }

    private static boolean isInventoryEmpty(Inventory inv) {
        for (ItemStack stack : inv.getContents()) {
            if (stack != null && stack.getType() != Material.AIR) {
                return false;
            }
        }
        return true;
    }

    private GameMap findPlayingMapContainingChest(Location blockLoc) {
        World w = blockLoc.getWorld();
        if (w == null) {
            return null;
        }
        String worldName = w.getName();
        for (GameMap map : SkyWarsReloaded.getGameMapMgr().getMapsCopy()) {
            World cw = map.getCurrentWorld();
            if (map.getMatchState() != MatchState.PLAYING || cw == null || !cw.getName().equals(worldName)) {
                continue;
            }
            if (chestMatchesMapChest(map, blockLoc)) {
                return map;
            }
        }
        return null;
    }

    private boolean chestMatchesMapChest(GameMap map, Location blockLoc) {
        CoordLoc c = new CoordLoc(blockLoc);
        if (isTrackedCoord(map, c)) {
            return true;
        }
        Block b = blockLoc.getWorld().getBlockAt(blockLoc.getBlockX(), blockLoc.getBlockY(), blockLoc.getBlockZ());
        if (!(b.getState() instanceof Chest)) {
            return false;
        }
        Chest chest = (Chest) b.getState();
        InventoryHolder ih = chest.getInventory().getHolder();
        if (ih instanceof DoubleChest) {
            Chest left = (Chest) ((DoubleChest) ih).getLeftSide();
            Chest right = (Chest) ((DoubleChest) ih).getRightSide();
            if (isTrackedCoord(map, new CoordLoc(left.getLocation())) || isTrackedCoord(map, new CoordLoc(right.getLocation()))) {
                return true;
            }
            return isChestNearAnyTeamSpawn(map, left.getBlock().getLocation())
                    || isChestNearAnyTeamSpawn(map, right.getBlock().getLocation())
                    || looseArenaChestBlock(map, blockLoc);
        }
        // Many maps never had every island chest saved under chests/centerChests; accept chests near recorded spawns.
        return isChestNearAnyTeamSpawn(map, blockLoc) || looseArenaChestBlock(map, blockLoc);
    }

    /**
     * Dedicated arena worlds (sw10, swt4, …): any vanilla chest in that world during PLAYING is refill-trackable.
     */
    private static boolean looseArenaChestBlock(GameMap map, Location blockLoc) {
        World cw = map.getCurrentWorld();
        if (cw == null || !cw.getName().equals(blockLoc.getWorld().getName())) {
            return false;
        }
        Block b = blockLoc.getWorld().getBlockAt(blockLoc.getBlockX(), blockLoc.getBlockY(), blockLoc.getBlockZ());
        Material t = b.getType();
        return t == Material.CHEST || t == Material.TRAPPED_CHEST;
    }

    /**
     * Horizontal distance from any team spawn — same heuristic scale as {@code game.maxMapSize} / world border.
     */
    private static boolean isChestNearAnyTeamSpawn(GameMap map, Location blockLoc) {
        // Cover islands and mid-map chests: within ~1 world-border radius of at least one spawn (maxMapSize matches border scale).
        int half = Math.max(96, SkyWarsReloaded.getCfg().getMaxMapSize());
        long maxDistSq = (long) half * half;
        for (TeamCard team : map.getTeamCards()) {
            List<CoordLoc> spawns = team.getSpawns();
            if (spawns == null) {
                continue;
            }
            for (CoordLoc s : spawns) {
                long dx = (long) blockLoc.getBlockX() - s.getX();
                long dz = (long) blockLoc.getBlockZ() - s.getZ();
                if (dx * dx + dz * dz <= maxDistSq) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isCenterChest(GameMap map, Location blockLoc) {
        CoordLoc c = new CoordLoc(blockLoc);
        if (map.getCenterChests().contains(c)) {
            return true;
        }
        Block b = blockLoc.getWorld().getBlockAt(blockLoc.getBlockX(), blockLoc.getBlockY(), blockLoc.getBlockZ());
        if (!(b.getState() instanceof Chest)) {
            return false;
        }
        InventoryHolder ih = ((Chest) b.getState()).getInventory().getHolder();
        if (ih instanceof DoubleChest) {
            Chest left = (Chest) ((DoubleChest) ih).getLeftSide();
            Chest right = (Chest) ((DoubleChest) ih).getRightSide();
            return map.getCenterChests().contains(new CoordLoc(left.getLocation()))
                    || map.getCenterChests().contains(new CoordLoc(right.getLocation()));
        }
        return false;
    }

    private static boolean isTrackedCoord(GameMap map, CoordLoc c) {
        return map.getChests().contains(c) || map.getCenterChests().contains(c);
    }

    private static boolean isChestRefillEventEnabled(GameMap map) {
        for (MatchEvent event : map.getEvents()) {
            if (event != null && "ChestRefillEvent".equalsIgnoreCase(event.getEventName())) {
                return event.isEnabled();
            }
        }
        return false;
    }

    private static int resolveRefillCountdownSeconds(GameMap map) {
        int fallback = Math.max(1, SkyWarsReloaded.getCfg().getChestRefillHologramCooldownSeconds());
        if (map == null) {
            return fallback;
        }
        for (MatchEvent event : map.getEvents()) {
            if (event == null || !"ChestRefillEvent".equalsIgnoreCase(event.getEventName()) || !event.isEnabled()) {
                continue;
            }
            int remaining = event.getStartTime() - map.getTimer();
            if (remaining > 0) {
                return remaining;
            }
            break;
        }
        return fallback;
    }

    private void setChestVisualOpen(TrackedChest trackedChest, boolean open) {
        if (!SkyWarsReloaded.getCfg().isChestRefillHologramKeepChestOpen() && open) {
            return;
        }
        World world = trackedChest.holoLoc.getWorld();
        if (world == null) {
            return;
        }
        Block block = world.getBlockAt(trackedChest.bx, trackedChest.by, trackedChest.bz);
        Material type = block.getType();
        if (type != Material.CHEST && type != Material.TRAPPED_CHEST) {
            return;
        }
        SkyWarsReloaded.getNMS().playChestAction(block, open);
        if (block.getState() instanceof Chest) {
            Chest chest = (Chest) block.getState();
            InventoryHolder ih = chest.getInventory().getHolder();
            if (ih instanceof DoubleChest) {
                DoubleChest dc = (DoubleChest) ih;
                Chest left = (Chest) dc.getLeftSide();
                Chest right = (Chest) dc.getRightSide();
                SkyWarsReloaded.getNMS().playChestAction(left.getBlock(), open);
                SkyWarsReloaded.getNMS().playChestAction(right.getBlock(), open);
            }
        }
    }

    private interface HoloBackend {
        void setLines(Location loc, String id, List<String> lines);

        void delete(String id);
    }

    private static final class NoopBackend implements HoloBackend {
        @Override
        public void setLines(Location loc, String id, List<String> lines) {
        }

        @Override
        public void delete(String id) {
        }
    }

    private static final class DecentBackend implements HoloBackend {
        @Override
        public void setLines(Location loc, String id, List<String> lines) {
            ArrayList<String> colored = new ArrayList<>(lines);
            Location useLoc = normalizeHologramWorld(loc);
            try {
                eu.decentsoftware.holograms.api.holograms.Hologram existing = DHAPI.getHologram(id);
                if (existing != null) {
                    DHAPI.setHologramLines(existing, colored);
                } else {
                    DHAPI.createHologram(id, useLoc, false, colored);
                    scheduleDecentVisibilityRefresh(useLoc, id);
                }
            } catch (Throwable ex) {
                SkyWarsReloaded.get().getLogger().log(Level.WARNING,
                        "Could not create or update chest refill hologram '" + id + "' at " + loc + ": " + ex.getMessage(), ex);
            }
        }

        @Override
        public void delete(String id) {
            try {
                eu.decentsoftware.holograms.api.holograms.Hologram h = DHAPI.getHologram(id);
                if (h != null) {
                    h.delete();
                }
            } catch (Throwable ex) {
                SkyWarsReloaded.get().getLogger().log(Level.FINE, "Could not delete chest refill hologram '" + id + "': " + ex.getMessage(), ex);
            }
        }
    }

    private static final class TrackedChest {
        final String mapName;
        final String holoId;
        final Location holoLoc;
        final int bx;
        final int by;
        final int bz;
        final boolean center;
        volatile long refillAtMillis;
        Integer lastShownSeconds;

        TrackedChest(String mapName, String holoId, Location holoLoc, int bx, int by, int bz, boolean center, long refillAtMillis) {
            this.mapName = mapName;
            this.holoId = holoId;
            this.holoLoc = holoLoc;
            this.bx = bx;
            this.by = by;
            this.bz = bz;
            this.center = center;
            this.refillAtMillis = refillAtMillis;
            this.lastShownSeconds = null;
        }
    }
}
