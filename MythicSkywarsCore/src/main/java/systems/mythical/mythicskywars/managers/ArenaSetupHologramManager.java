package systems.mythical.mythicskywars.managers;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.game.TeamCard;
import systems.mythical.mythicskywars.menus.gameoptions.objects.CoordLoc;
import systems.mythical.mythicskywars.utilities.Messaging;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ArenaSetupHologramManager {
    private static final ArenaSetupHologramManager INSTANCE = new ArenaSetupHologramManager();
    private final Map<String, Set<String>> holoIdsByMap = new HashMap<>();
    private final Map<String, BukkitTask> pendingRefreshByMap = new HashMap<>();
    private final Map<String, Boolean> pendingFullRefreshByMap = new HashMap<>();

    private ArenaSetupHologramManager() {
    }

    public static ArenaSetupHologramManager get() {
        return INSTANCE;
    }

    public void requestRefresh(GameMap map) {
        requestRefresh(map, false);
    }

    public void requestFullRefresh(GameMap map) {
        requestRefresh(map, true);
    }

    private void requestRefresh(GameMap map, boolean fullScan) {
        if (map == null) {
            return;
        }
        String mapName = map.getName();
        boolean alreadyFull = pendingFullRefreshByMap.getOrDefault(mapName, false);
        pendingFullRefreshByMap.put(mapName, alreadyFull || fullScan);
        if (pendingRefreshByMap.containsKey(mapName)) {
            return;
        }
        BukkitTask task = Bukkit.getScheduler().runTaskLater(MythicSkywars.get(), () -> {
            pendingRefreshByMap.remove(mapName);
            boolean doFull = pendingFullRefreshByMap.remove(mapName) == Boolean.TRUE;
            refreshNow(map, doFull);
        }, 5L);
        pendingRefreshByMap.put(mapName, task);
    }

    private void refreshNow(GameMap map, boolean scanAllRegionChunks) {
        if (map == null) {
            return;
        }
        if (!isEnabled()) {
            clear(map);
            return;
        }
        clear(map);
        World world = map.getCurrentWorld();
        if (world == null) {
            return;
        }

        Set<String> ids = holoIdsByMap.computeIfAbsent(map.getName(), k -> new HashSet<>());

        int normalChestIndex = 0;
        for (CoordLoc chest : map.getChests()) {
            if (!isChestAt(world, chest)) {
                continue;
            }
            create(world, ids, map.getName(), "chest_n_" + normalChestIndex++, chest, msg("maps.editor.hologram.type.chest-normal"));
        }
        int centerChestIndex = 0;
        for (CoordLoc chest : map.getCenterChests()) {
            if (!isChestAt(world, chest)) {
                continue;
            }
            create(world, ids, map.getName(), "chest_c_" + centerChestIndex++, chest, msg("maps.editor.hologram.type.chest-center"));
        }
        addUnregisteredChestHolograms(world, ids, map, scanAllRegionChunks);

        int teamIndex = 0;
        for (TeamCard teamCard : map.getTeamCards()) {
            List<CoordLoc> spawns = teamCard.getSpawns();
            if (spawns == null) {
                teamIndex++;
                continue;
            }
            for (int spawnIndex = 0; spawnIndex < spawns.size(); spawnIndex++) {
                CoordLoc spawn = spawns.get(spawnIndex);
                String teamLabel = new Messaging.MessageFormatter()
                        .setVariable("team", String.valueOf(teamIndex + 1))
                        .setVariable("spawn", String.valueOf(spawnIndex + 1))
                        .format("maps.editor.hologram.type.spawn");
                create(world, ids, map.getName(), "spawn_t" + teamIndex + "_s" + spawnIndex, spawn, teamLabel);
            }
            teamIndex++;
        }

        CoordLoc waiting = map.getWaitingLobbySpawn();
        if (waiting != null) {
            create(world, ids, map.getName(), "waiting", waiting, msg("maps.editor.hologram.type.world-spawn"));
        }
        CoordLoc spectate = map.getSpectateSpawn();
        if (spectate != null) {
            create(world, ids, map.getName(), "spectate", spectate, msg("maps.editor.hologram.type.spectator-spawn"));
        }

        // Deathmatch spawn holograms
        int dmIndex = 0;
        for (CoordLoc dmSpawn : map.getDeathMatchSpawns()) {
            String dmLabel = new Messaging.MessageFormatter()
                    .setVariable("num", String.valueOf(dmIndex + 1))
                    .format("maps.editor.hologram.type.deathmatch-spawn");
            create(world, ids, map.getName(), "dm_" + dmIndex, dmSpawn, dmLabel);
            dmIndex++;
        }
    }

    public void clear(GameMap map) {
        if (map == null) {
            return;
        }
        Set<String> ids = holoIdsByMap.remove(map.getName());
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (String id : ids) {
            try {
                eu.decentsoftware.holograms.api.holograms.Hologram h = eu.decentsoftware.holograms.api.DHAPI.getHologram(id);
                if (h != null) {
                    h.delete();
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private void create(World world, Set<String> ids, String mapName, String suffix, CoordLoc loc, String typeLabel) {
        if (loc == null) {
            return;
        }
        String id = "swr_setup_" + mapName + "_" + suffix;
        Location holoLoc = new Location(world, loc.getX() + 0.5D, loc.getY() + 1.35D, loc.getZ() + 0.5D);
        String line = new Messaging.MessageFormatter().setVariable("type", typeLabel).format("maps.editor.hologram.line");
        try {
            eu.decentsoftware.holograms.api.DHAPI.createHologram(id, holoLoc, false, java.util.Collections.singletonList(line));
            ids.add(id);
        } catch (Throwable ignored) {
        }
    }

    private void addUnregisteredChestHolograms(World world, Set<String> ids, GameMap map, boolean scanAllRegionChunks) {
        Set<String> registered = new HashSet<>();
        for (CoordLoc chest : map.getChests()) {
            registered.add(toKey(chest));
        }
        for (CoordLoc chest : map.getCenterChests()) {
            registered.add(toKey(chest));
        }

        Set<ChunkRef> chunksToScan = new HashSet<>();
        if (scanAllRegionChunks) {
            chunksToScan.addAll(getAllRegionChunks(world));
        }
        if (chunksToScan.isEmpty()) {
            for (Chunk loadedChunk : world.getLoadedChunks()) {
                chunksToScan.add(new ChunkRef(loadedChunk.getX(), loadedChunk.getZ()));
            }
        }

        int unregisteredIndex = 0;
        for (ChunkRef chunkRef : chunksToScan) {
            boolean wasLoaded = world.isChunkLoaded(chunkRef.x, chunkRef.z);
            if (!wasLoaded) {
                world.loadChunk(chunkRef.x, chunkRef.z, false);
            }
            if (!world.isChunkLoaded(chunkRef.x, chunkRef.z)) {
                continue;
            }
            Chunk chunk = world.getChunkAt(chunkRef.x, chunkRef.z);
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < world.getMaxHeight(); y++) {
                        Block block = chunk.getBlock(x, y, z);
                        Material type = block.getType();
                        if (type != Material.CHEST && type != Material.TRAPPED_CHEST) {
                            continue;
                        }
                        CoordLoc loc = new CoordLoc(block.getX(), block.getY(), block.getZ());
                        if (registered.contains(toKey(loc))) {
                            continue;
                        }
                        create(world, ids, map.getName(), "chest_u_" + (unregisteredIndex++), loc, msg("maps.editor.hologram.type.chest-unregistered"));
                    }
                }
            }
            if (!wasLoaded) {
                world.unloadChunkRequest(chunkRef.x, chunkRef.z);
            }
        }
    }

    private Set<ChunkRef> getAllRegionChunks(World world) {
        Set<ChunkRef> refs = new HashSet<>();
        File regionFolder = new File(world.getWorldFolder(), "region");
        if (!regionFolder.exists() || !regionFolder.isDirectory()) {
            return refs;
        }
        File[] files = regionFolder.listFiles();
        if (files == null) {
            return refs;
        }

        Pattern regionPattern = Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.(mca|mcr)$");
        for (File file : files) {
            if (file == null || !file.isFile()) {
                continue;
            }
            Matcher matcher = regionPattern.matcher(file.getName());
            if (!matcher.matches()) {
                continue;
            }
            int regionX = Integer.parseInt(matcher.group(1));
            int regionZ = Integer.parseInt(matcher.group(2));
            int chunkBaseX = regionX << 5;
            int chunkBaseZ = regionZ << 5;
            for (int dx = 0; dx < 32; dx++) {
                for (int dz = 0; dz < 32; dz++) {
                    refs.add(new ChunkRef(chunkBaseX + dx, chunkBaseZ + dz));
                }
            }
        }
        return refs;
    }

    private String toKey(CoordLoc loc) {
        return loc.getX() + ":" + loc.getY() + ":" + loc.getZ();
    }

    private boolean isChestAt(World world, CoordLoc loc) {
        Material type = world.getBlockAt(loc.getX(), loc.getY(), loc.getZ()).getType();
        return type == Material.CHEST || type == Material.TRAPPED_CHEST;
    }

    private static final class ChunkRef {
        private final int x;
        private final int z;

        private ChunkRef(int x, int z) {
            this.x = x;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ChunkRef)) {
                return false;
            }
            ChunkRef chunkRef = (ChunkRef) o;
            return x == chunkRef.x && z == chunkRef.z;
        }

        @Override
        public int hashCode() {
            int result = x;
            result = 31 * result + z;
            return result;
        }
    }

    private boolean isEnabled() {
        return MythicSkywars.getCfg().hologramsEnabled() && Bukkit.getPluginManager().isPluginEnabled("DecentHolograms");
    }

    private String msg(String key) {
        return new Messaging.MessageFormatter().format(key);
    }
}
