package com.walrusone.skywarsreloaded.managers;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.game.GameMap;
import com.walrusone.skywarsreloaded.menus.gameoptions.objects.CoordLoc;
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
        if (!SkyWarsReloaded.getCfg().isChestRefillEnabled()) {
            return;
        }
        int refillInterval = SkyWarsReloaded.getCfg().getChestRefillIntervalSeconds();
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

        if (SkyWarsReloaded.getCfg().isChestRefillKeepChestOpen()) {
            trackAndOpenChest(gameMap, clickedBlock);
        }
        if (SkyWarsReloaded.getCfg().isChestRefillShowHologram()) {
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

        for (String key : keys) {
            Block block = getBlockFromKey(world, key);
            if (block == null) {
                continue;
            }
            if (SkyWarsReloaded.getCfg().isChestRefillKeepChestOpen()) {
                SkyWarsReloaded.getNMS().playChestAction(block, false);
            }
            if (SkyWarsReloaded.getCfg().isChestRefillShowHologram()) {
                showRefilledHolo(gameMap, block);
            }
        }
        keys.clear();
        cancelRefreshTask(mapName);
    }

    private void trackAndOpenChest(GameMap gameMap, Block block) {
        String mapName = gameMap.getName();
        Set<String> opened = openedChestKeysByMap.computeIfAbsent(mapName, k -> new HashSet<>());
        opened.add(toKey(block));
        SkyWarsReloaded.getNMS().playChestAction(block, true);

        if (block.getState() instanceof Chest) {
            InventoryHolder holder = ((Chest) block.getState()).getInventory().getHolder();
            if (holder instanceof DoubleChest) {
                Chest left = (Chest) ((DoubleChest) holder).getLeftSide();
                Chest right = (Chest) ((DoubleChest) holder).getRightSide();
                opened.add(toKey(left.getBlock()));
                opened.add(toKey(right.getBlock()));
                SkyWarsReloaded.getNMS().playChestAction(left.getBlock(), true);
                SkyWarsReloaded.getNMS().playChestAction(right.getBlock(), true);
            }
        }
    }

    private void spawnOrReplaceCountdownHolo(GameMap gameMap, Block block, int remainingSeconds) {
        if (!Bukkit.getPluginManager().isPluginEnabled("DecentHolograms")) {
            return;
        }
        String key = toKey(block);
        String id = "swr_refill_" + gameMap.getName() + "_" + key.replace(':', '_');
        String line = ChatColor.YELLOW + "Refill in: " + ChatColor.GOLD + formatTime(remainingSeconds);
        Location holoLoc = block.getLocation().add(0.5D, 1.15D, 0.5D);
        try {
            eu.decentsoftware.holograms.api.holograms.Hologram existing = eu.decentsoftware.holograms.api.DHAPI.getHologram(id);
            if (existing != null) {
                existing.delete();
            }
            eu.decentsoftware.holograms.api.DHAPI.createHologram(id, holoLoc, false, Collections.singletonList(line));
        } catch (Throwable ignored) {
        }
    }

    private void showRefilledHolo(GameMap gameMap, Block block) {
        if (!Bukkit.getPluginManager().isPluginEnabled("DecentHolograms")) {
            return;
        }
        String key = toKey(block);
        String id = "swr_refill_" + gameMap.getName() + "_" + key.replace(':', '_');
        Location holoLoc = block.getLocation().add(0.5D, 1.15D, 0.5D);
        try {
            eu.decentsoftware.holograms.api.holograms.Hologram existing = eu.decentsoftware.holograms.api.DHAPI.getHologram(id);
            if (existing != null) {
                existing.delete();
            }
            eu.decentsoftware.holograms.api.DHAPI.createHologram(
                    id,
                    holoLoc,
                    false,
                    Arrays.asList(ChatColor.GREEN + "Refilled!")
            );
            Bukkit.getScheduler().runTaskLater(SkyWarsReloaded.get(), () -> {
                try {
                    eu.decentsoftware.holograms.api.holograms.Hologram holo = eu.decentsoftware.holograms.api.DHAPI.getHologram(id);
                    if (holo != null) {
                        holo.delete();
                    }
                } catch (Throwable ignored) {
                }
            }, 40L);
        } catch (Throwable ignored) {
        }
    }

    private void ensureRefreshTask(GameMap gameMap) {
        String mapName = gameMap.getName();
        activeMaps.put(mapName, gameMap);
        BukkitTask existing = refreshTasksByMap.get(mapName);
        if (existing != null && !existing.isCancelled()) {
            return;
        }
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(SkyWarsReloaded.get(), () -> tickMap(mapName), 20L, 20L);
        refreshTasksByMap.put(mapName, task);
    }

    private void tickMap(String mapName) {
        GameMap gameMap = activeMaps.get(mapName);
        if (gameMap == null || gameMap.getMatchState() != com.walrusone.skywarsreloaded.enums.MatchState.PLAYING) {
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
        int refillInterval = SkyWarsReloaded.getCfg().getChestRefillIntervalSeconds();
        int remaining = refillInterval > 0 ? refillInterval - (gameMap.getTimer() % refillInterval) : 0;
        if (remaining <= 0 && refillInterval > 0) {
            remaining = refillInterval;
        }
        for (String key : new HashSet<>(keys)) {
            Block block = getBlockFromKey(world, key);
            if (block == null) {
                continue;
            }
            Material type = block.getType();
            if (type != Material.CHEST && type != Material.TRAPPED_CHEST) {
                continue;
            }
            if (SkyWarsReloaded.getCfg().isChestRefillKeepChestOpen()) {
                SkyWarsReloaded.getNMS().playChestAction(block, true);
            }
            if (SkyWarsReloaded.getCfg().isChestRefillShowHologram() && refillInterval > 0) {
                spawnOrReplaceCountdownHolo(gameMap, block, remaining);
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
