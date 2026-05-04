package systems.mythical.mythicskywars.managers;

import com.google.common.collect.ImmutableList;
import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.enums.GameType;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.game.GameMapComparator;
import systems.mythical.mythicskywars.managers.worlds.FileWorldManager;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GameMapManager {

    private final MythicSkywars plugin;
    private ArrayList<GameMap> arenas;

    public GameMapManager(MythicSkywars plugin) {
        this.plugin = plugin;
        arenas = new ArrayList<>();
    }

    public GameMap addMap(String name) {
        GameMap gMap = new GameMap(name);
        this.addMap(gMap);
        return gMap;
    }

    public void addMap(GameMap gMap) {
        arenas.add(gMap);
    }

    public void loadMaps() {
        File mapFile = new File(MythicSkywars.get().getDataFolder(), "maps.yml");
        if (mapFile.exists()) {
            updateMapData();
        }
        arenas.clear();
        
        // Reset stagger counter before bulk-loading maps
        GameMap.resetLoadStagger();

        if (MythicSkywars.getWM() instanceof FileWorldManager) {
            File dataDirectory = MythicSkywars.get().getDataFolder();
            File maps = new File(dataDirectory, "maps");
            if (maps.exists() && maps.isDirectory()) {
                File[] files = maps.listFiles();
                if (files != null) {
                    for (File map : files) {
                        if (map.isDirectory()) {
                            addMap(map.getName());
                        }
                    }
                }
            } else {
                MythicSkywars.get().getLogger().info("Maps directory is missing or no Maps were found!");
            }
        } else {
            File dataDirectory = MythicSkywars.get().getDataFolder();
            File maps = new File(dataDirectory, "mapsData");
            if (maps.exists() && maps.isDirectory()) {
                File[] files = maps.listFiles();
                if (files != null) {
                    List<File> filesList = Arrays.asList(files);
                    // If in random map load + bungeemode -> only load one random map
                    if (MythicSkywars.getCfg().bungeeMode() &&
                            MythicSkywars.getCfg().getBungeeRandomMapPickOnStart()) {
                        Collections.shuffle(filesList);
                        File first = filesList.get(0);
                        if (first != null) {
                            GameMap gameMap = addMap(first.getName().replace(".yml", ""));
                            int code = gameMap.registerMap(null);
                            if (code != 0) {
                                MythicSkywars.get().getLogger().severe("Attempted to load a map in auto pick mode, " +
                                        "but map failed to register! (Error Code: " + code + ")");
                            }
                        }
                    } else {
                        for (File file : files) {
                            addMap(file.getName().replace(".yml", ""));
                        }
                    }
                }
            }
        }
        
        // Reset stagger counter after all maps have been queued for loading.
        // This ensures subsequent individual refreshMap() calls (after games end) use minimal delay.
        org.bukkit.Bukkit.getScheduler().runTaskLater(MythicSkywars.get(), GameMap::resetLoadStagger, 
                20L + ((long) arenas.size() * GameMap.TICKS_BETWEEN_MAP_LOADS) + 20L);
    }

    private void updateMapData() {
        File mapFile = new File(MythicSkywars.get().getDataFolder(), "maps.yml");
        if (mapFile.exists()) {
            FileConfiguration storage = YamlConfiguration.loadConfiguration(mapFile);

            if (storage.getConfigurationSection("maps") != null) {
                for (String key : storage.getConfigurationSection("maps").getKeys(false)) {
                    String displayname = storage.getString("maps." + key + ".displayname");
                    int minplayers = storage.getInt("maps." + key + ".minplayers");
                    String creator = storage.getString("maps." + key + ".creator");
                    List<String> signs = storage.getStringList("maps." + key + ".signs");
                    boolean registered = storage.getBoolean("maps." + key + ".registered");

                    File dataDirectory = MythicSkywars.get().getDataFolder();
                    File mapDataDirectory = new File(dataDirectory, "mapsData");

                    if (!mapDataDirectory.exists() && !mapDataDirectory.mkdirs()) {
                        return;
                    }

                    File newMapFile = new File(mapDataDirectory, key + ".yml");
                    copyDefaults(newMapFile);
                    FileConfiguration fc = YamlConfiguration.loadConfiguration(newMapFile);
                    fc.set("displayname", displayname);
                    fc.set("minplayers", minplayers);
                    fc.set("creator", creator);
                    fc.set("signs", signs);
                    fc.set("registered", registered);
                    fc.set("environment", "NORMAL");
                    fc.set("spectateSpawn", "0:95:0");
                    fc.set("deathMatchSpawns", null);
                    fc.set("legacy", true);
                    try {
                        fc.save(newMapFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            boolean result = mapFile.delete();
            if (!result) {
                MythicSkywars.get().getLogger().info("Failed to Delete Old MapData File");
            }
        }
    }


    public GameMap.GameMapCreationResult createNewMap(String mapName, World.Environment environment) {
        // Sanity check for world name
        if (!mapName.matches("^[a-zA-Z0-9_\\-]+$")) {
            return new GameMap.GameMapCreationResult(false, null);
        }
        // Attempt world creation
        World newWorld = MythicSkywars.getWM().createEmptyWorld(mapName, environment);
        if (newWorld == null) {
            return new GameMap.GameMapCreationResult(true, null);
        }
        addMap(mapName);
        GameMap map = this.getMap(mapName);
        if (map != null) {
            map.setEnvironment(environment.toString());
            map.saveArenaData();
        }
        return new GameMap.GameMapCreationResult(true, newWorld);
    }

    public ArrayList<GameMap> getPlayableArenas(GameType type) {
        ArrayList<GameMap> sorted = new ArrayList<>();
        if (type == GameType.TEAM) {
            for (GameMap gMap : arenas) {
                if (gMap.isRegistered() && gMap.getTeamSize() > 1) {
                    sorted.add(gMap);
                }
            }
        } else if (type == GameType.SINGLE) {
            for (GameMap gMap : arenas) {
                if (gMap.isRegistered() && gMap.getTeamSize() == 1) {
                    sorted.add(gMap);
                }
            }
        } else {
            for (GameMap gMap : arenas) {
                if (gMap.isRegistered()) {
                    sorted.add(gMap);
                }
            }
        }
        sorted.sort(new GameMapComparator());
        return sorted;
    }

    public GameMap getMap(final String mapNameIn) {
        String mapName = ChatColor.stripColor(mapNameIn).toLowerCase();
        for (final GameMap map : this.arenas) {
            if (map.getName().toLowerCase().equals(mapName)) {
                return map;
            }
        }
        return null;
    }

    public ImmutableList<GameMap> getMapsCopy() {
        return ImmutableList.copyOf(arenas);
    }

    public ArrayList<GameMap> getSortedArenas() {
        ArrayList<GameMap> sorted = new ArrayList<>(arenas);
        sorted.sort(new GameMapComparator());
        return sorted;
    }

    public void shuffle() {
        Collections.shuffle(arenas);
    }

    public GameMap getMapByDisplayName(String name) {
        for (GameMap gMap : arenas) {
            if (ChatColor.stripColor((ChatColor.translateAlternateColorCodes('&', gMap.getDisplayName()))).equalsIgnoreCase(name)) {
                return gMap;
            }
        }
        return null;
    }

    public void copyDefaults(File mapFile) {
        FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(mapFile);
        Reader defConfigStream = new InputStreamReader(MythicSkywars.get().getResource("mapFile.yml"));
        YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
        playerConfig.options().copyDefaults(true);
        playerConfig.setDefaults(defConfig);
        try {
            playerConfig.save(mapFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void remove(GameMap gameMap) {
        arenas.remove(gameMap);
    }
}
