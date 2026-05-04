package systems.mythical.mythicskywars.managers;

import systems.mythical.mythicskywars.clients.lunar.LunarApolloBridge;
import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.menus.gameoptions.objects.CoordLoc;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ChestRefillVisualManager {
    private static final ChestRefillVisualManager INSTANCE = new ChestRefillVisualManager();

    private final Map<String, Set<String>> openedChestKeysByMap = new HashMap<>();
    private final Map<String, BukkitTask> refreshTasksByMap = new HashMap<>();
    private final Map<String, GameMap> activeMaps = new HashMap<>();

    private ChestRefillVisualManager() {
    }

    public static ChestRefillVisualManager get() {
        return INSTANCE;
    }

    public void onChestInteract(GameMap gameMap, Block clickedBlock) {
        if (gameMap == null || clickedBlock == null) {
            return;
        }
        if (!MythicSkywars.getCfg().isChestRefillEnabled()) {
            return;
        }
        int refillInterval = MythicSkywars.getCfg().getChestRefillIntervalSeconds();
        if (refillInterval <= 0) {
            return;
        }
        Material type = clickedBlock.getType();
        if (type != Material.CHEST && type != Material.TRAPPED_CHEST) {
            return;
        }
        if (!isArenaChest(gameMap, clickedBlock)) {
            return;
        }

        if (MythicSkywars.getCfg().isChestRefillKeepChestOpen()) {
            trackAndOpenChest(gameMap, clickedBlock);
        }
        if (MythicSkywars.getCfg().isChestRefillShowHologram()) {
            int remaining = refillInterval - (gameMap.getTimer() % refillInterval);
            if (remaining <= 0) {
                remaining = refillInterval;
            }
            spawnOrReplaceCountdownHolo(gameMap, clickedBlock, remaining);
        }
        ensureRefreshTask(gameMap);
    }

    public void onRefill(GameMap gameMap) {
        if (gameMap == null) {
            return;
        }
        String mapName = gameMap.getName();
        World world = gameMap.getCurrentWorld();
        if (world == null) {
            return;
        }
        Set<String> keys = openedChestKeysByMap.getOrDefault(mapName, Collections.emptySet());
        if (keys.isEmpty()) {
            cancelRefreshTask(mapName);
            return;
        }

        Set<String> processedHoloIds = new HashSet<>();
        for (String key : keys) {
            Block block = getBlockFromKey(world, key);
            if (block == null) {
                continue;
            }
            if (MythicSkywars.getCfg().isChestRefillKeepChestOpen()) {
                MythicSkywars.getNMS().playChestAction(block, false);
            }
            if (MythicSkywars.getCfg().isChestRefillShowHologram()) {
                Block holoAnchor = getHoloAnchorBlock(block);
                String holoId = "swr_refill_" + gameMap.getName() + "_" + toKey(holoAnchor).replace(':', '_');
                if (!processedHoloIds.add(holoId)) {
                    continue;
                }
                removeHolo(gameMap, holoAnchor);
            }
        }
        keys.clear();
        cancelRefreshTask(mapName);
        LunarApolloBridge.notifyChestRefill(gameMap);
    }

    private void trackAndOpenChest(GameMap gameMap, Block block) {
        String mapName = gameMap.getName();
        Set<String> opened = openedChestKeysByMap.computeIfAbsent(mapName, k -> new HashSet<>());
        opened.add(toKey(block));
        MythicSkywars.getNMS().playChestAction(block, true);

        if (block.getState() instanceof Chest) {
            InventoryHolder holder = ((Chest) block.getState()).getInventory().getHolder();
            if (holder instanceof DoubleChest) {
                Chest left = (Chest) ((DoubleChest) holder).getLeftSide();
                Chest right = (Chest) ((DoubleChest) holder).getRightSide();
                opened.add(toKey(left.getBlock()));
                opened.add(toKey(right.getBlock()));
                MythicSkywars.getNMS().playChestAction(left.getBlock(), true);
                MythicSkywars.getNMS().playChestAction(right.getBlock(), true);
            }
        }
    }

    private void spawnOrReplaceCountdownHolo(GameMap gameMap, Block block, int remainingSeconds) {
        if (!Bukkit.getPluginManager().isPluginEnabled("DecentHolograms")) {
            return;
        }
        Block holoAnchor = getHoloAnchorBlock(block);
        String id = getCanonicalHoloId(gameMap, holoAnchor);
        cleanupDuplicateChestHolograms(gameMap, block, id);
        String line = ChatColor.YELLOW + "Refill in: " + ChatColor.GOLD + formatTime(remainingSeconds);
        Location holoLoc = holoAnchor.getLocation().add(0.5D, 1.15D, 0.5D);
        try {
            eu.decentsoftware.holograms.api.holograms.Hologram existing = eu.decentsoftware.holograms.api.DHAPI.getHologram(id);
            if (existing != null) {
                clearHologramLines(existing);
                eu.decentsoftware.holograms.api.DHAPI.addHologramLine(existing, line);
                return;
            }
            eu.decentsoftware.holograms.api.DHAPI.createHologram(id, holoLoc, false, Collections.singletonList(line));
        } catch (Throwable ignored) {
        }
    }

    private void removeHolo(GameMap gameMap, Block block) {
        if (!Bukkit.getPluginManager().isPluginEnabled("DecentHolograms")) {
            return;
        }
        Block holoAnchor = getHoloAnchorBlock(block);
        String id = getCanonicalHoloId(gameMap, holoAnchor);
        cleanupDuplicateChestHolograms(gameMap, block, id);
        try {
            eu.decentsoftware.holograms.api.holograms.Hologram existing = eu.decentsoftware.holograms.api.DHAPI.getHologram(id);
            if (existing != null) {
                existing.delete();
            }
        } catch (Throwable ignored) {
        }
    }

    private void ensureRefreshTask(GameMap gameMap) {
        String mapName = gameMap.getName();
        activeMaps.put(mapName, gameMap);
        BukkitTask existing = refreshTasksByMap.get(mapName);
        if (existing != null) {
            return;
        }
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(MythicSkywars.get(), () -> tickMap(mapName), 20L, 20L);
        refreshTasksByMap.put(mapName, task);
    }

    private void tickMap(String mapName) {
        GameMap gameMap = activeMaps.get(mapName);
        if (gameMap == null || gameMap.getMatchState() != systems.mythical.mythicskywars.enums.MatchState.PLAYING) {
            cancelRefreshTask(mapName);
            return;
        }
        Set<String> keys = openedChestKeysByMap.get(mapName);
        if (keys == null || keys.isEmpty()) {
            cancelRefreshTask(mapName);
            return;
        }
        World world = gameMap.getCurrentWorld();
        if (world == null) {
            return;
        }
        int refillInterval = MythicSkywars.getCfg().getChestRefillIntervalSeconds();
        int remaining = refillInterval > 0 ? refillInterval - (gameMap.getTimer() % refillInterval) : 0;
        if (remaining <= 0 && refillInterval > 0) {
            remaining = refillInterval;
        }
        Set<String> processedHoloIds = new HashSet<>();
        for (String key : new HashSet<>(keys)) {
            Block block = getBlockFromKey(world, key);
            if (block == null) {
                continue;
            }
            Material type = block.getType();
            if (type != Material.CHEST && type != Material.TRAPPED_CHEST) {
                continue;
            }
            if (MythicSkywars.getCfg().isChestRefillKeepChestOpen()) {
                MythicSkywars.getNMS().playChestAction(block, true);
            }
            if (MythicSkywars.getCfg().isChestRefillShowHologram() && refillInterval > 0) {
                Block holoAnchor = getHoloAnchorBlock(block);
                String holoId = "swr_refill_" + gameMap.getName() + "_" + toKey(holoAnchor).replace(':', '_');
                if (!processedHoloIds.add(holoId)) {
                    continue;
                }
                spawnOrReplaceCountdownHolo(gameMap, block, remaining);
            }
        }
    }

    private Block getHoloAnchorBlock(Block block) {
        if (!(block.getState() instanceof Chest)) {
            return block;
        }
        InventoryHolder holder = ((Chest) block.getState()).getInventory().getHolder();
        if (!(holder instanceof DoubleChest)) {
            return block;
        }
        Chest left = (Chest) ((DoubleChest) holder).getLeftSide();
        Chest right = (Chest) ((DoubleChest) holder).getRightSide();
        Block leftBlock = left.getBlock();
        Block rightBlock = right.getBlock();
        if (leftBlock.getX() < rightBlock.getX()) {
            return leftBlock;
        }
        if (leftBlock.getX() > rightBlock.getX()) {
            return rightBlock;
        }
        if (leftBlock.getZ() <= rightBlock.getZ()) {
            return leftBlock;
        }
        return rightBlock;
    }

    private void clearHologramLines(eu.decentsoftware.holograms.api.holograms.Hologram hologram) {
        if (hologram == null) {
            return;
        }
        while (true) {
            try {
                if (eu.decentsoftware.holograms.api.DHAPI.removeHologramLine(hologram, 0) == null) {
                    break;
                }
            } catch (Throwable ignored) {
                break;
            }
        }
    }

    private String getCanonicalHoloId(GameMap gameMap, Block anchorBlock) {
        return "swr_refill_" + gameMap.getName() + "_" + toKey(anchorBlock).replace(':', '_');
    }

    private void cleanupDuplicateChestHolograms(GameMap gameMap, Block block, String canonicalId) {
        String mapPrefix = "swr_refill_" + gameMap.getName() + "_";
        Set<String> candidateIds = new HashSet<>();
        candidateIds.add(mapPrefix + toKey(block).replace(':', '_'));
        Block anchor = getHoloAnchorBlock(block);
        candidateIds.add(mapPrefix + toKey(anchor).replace(':', '_'));

        if (block.getState() instanceof Chest) {
            InventoryHolder holder = ((Chest) block.getState()).getInventory().getHolder();
            if (holder instanceof DoubleChest) {
                Chest left = (Chest) ((DoubleChest) holder).getLeftSide();
                Chest right = (Chest) ((DoubleChest) holder).getRightSide();
                candidateIds.add(mapPrefix + toKey(left.getBlock()).replace(':', '_'));
                candidateIds.add(mapPrefix + toKey(right.getBlock()).replace(':', '_'));
            }
        }

        for (String id : candidateIds) {
            if (id.equals(canonicalId)) {
                continue;
            }
            try {
                eu.decentsoftware.holograms.api.holograms.Hologram h = eu.decentsoftware.holograms.api.DHAPI.getHologram(id);
                if (h != null) {
                    h.delete();
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private void cancelRefreshTask(String mapName) {
        BukkitTask task = refreshTasksByMap.remove(mapName);
        if (task != null) {
            task.cancel();
        }
        if (!openedChestKeysByMap.containsKey(mapName) || openedChestKeysByMap.get(mapName).isEmpty()) {
            activeMaps.remove(mapName);
        }
    }

    private boolean isArenaChest(GameMap gameMap, Block block) {
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        for (CoordLoc c : gameMap.getChests()) {
            if (c.getX() == x && c.getY() == y && c.getZ() == z) {
                return true;
            }
        }
        for (CoordLoc c : gameMap.getCenterChests()) {
            if (c.getX() == x && c.getY() == y && c.getZ() == z) {
                return true;
            }
        }
        return false;
    }

    private String toKey(Block block) {
        return block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private Block getBlockFromKey(World world, String key) {
        try {
            String[] parts = key.split(":");
            if (parts.length != 3) {
                return null;
            }
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            return world.getBlockAt(x, y, z);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String formatTime(int seconds) {
        int s = Math.max(0, seconds);
        int min = s / 60;
        int sec = s % 60;
        return String.format("%d:%02d", min, sec);
    }
}
