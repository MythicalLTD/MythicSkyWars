package com.walrusone.skywarsreloaded.utilities;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.enums.GameType;
import com.walrusone.skywarsreloaded.game.GameMap;
import com.walrusone.skywarsreloaded.managers.MatchManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class LobbyWaterPortalManager {

    private final SkyWarsReloaded plugin;
    private final Map<String, PortalRegion> portals = new HashMap<>();
    private final Map<UUID, Long> playerCooldown = new HashMap<>();
    /** Prevents move-spam and overlapping delayed join retries for the same player. */
    private final Set<UUID> portalJoinRetryPending = Collections.synchronizedSet(new HashSet<UUID>());
    private static final long COOLDOWN_MS = 2000L;
    private static final long JOIN_RETRY_TICKS = 5L;

    public LobbyWaterPortalManager(SkyWarsReloaded plugin) {
        this.plugin = plugin;
        migrateOldFile();
        reload();
    }

    /**
     * Migrates data from the old lobby-water-portals.yml file into config.yml and deletes the old file.
     */
    private void migrateOldFile() {
        File oldFile = new File(plugin.getDataFolder(), "lobby-water-portals.yml");
        if (oldFile.exists()) {
            plugin.getLogger().info("Migrating lobby-water-portals.yml into config.yml...");
            FileConfiguration oldCfg = YamlConfiguration.loadConfiguration(oldFile);
            ConfigurationSection oldPortals = oldCfg.getConfigurationSection("portals");

            if (oldPortals != null) {
                FileConfiguration config = plugin.getConfig();
                for (String nameKey : oldPortals.getKeys(false)) {
                    String oldBase = "portals." + nameKey;
                    String newBase = "lobby-water-portals." + nameKey;
                    config.set(newBase + ".world", oldCfg.getString(oldBase + ".world"));
                    config.set(newBase + ".type", oldCfg.getString(oldBase + ".type", "ALL"));
                    config.set(newBase + ".x1", oldCfg.getInt(oldBase + ".x1"));
                    config.set(newBase + ".y1", oldCfg.getInt(oldBase + ".y1"));
                    config.set(newBase + ".z1", oldCfg.getInt(oldBase + ".z1"));
                    config.set(newBase + ".x2", oldCfg.getInt(oldBase + ".x2"));
                    config.set(newBase + ".y2", oldCfg.getInt(oldBase + ".y2"));
                    config.set(newBase + ".z2", oldCfg.getInt(oldBase + ".z2"));
                    config.set(newBase + ".pos1Set", oldCfg.getBoolean(oldBase + ".pos1Set", false));
                    config.set(newBase + ".pos2Set", oldCfg.getBoolean(oldBase + ".pos2Set", false));
                    config.set(newBase + ".luckyMode", oldCfg.getBoolean(oldBase + ".luckyMode", false));
                }
                plugin.saveConfig();
            }

            if (oldFile.delete()) {
                plugin.getLogger().info("Successfully migrated lobby-water-portals.yml into config.yml and deleted old file.");
            } else {
                plugin.getLogger().warning("Migrated lobby-water-portals.yml data but failed to delete old file.");
            }
        }
    }

    public void reload() {
        portals.clear();
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection sec = config.getConfigurationSection("lobby-water-portals");
        if (sec == null) {
            return;
        }
        for (String nameKey : sec.getKeys(false)) {
            String base = "lobby-water-portals." + nameKey;
            String world = config.getString(base + ".world");
            if (world == null || world.isEmpty()) {
                continue;
            }
            String typeRaw = config.getString(base + ".type", "ALL");
            GameType type;
            try {
                type = GameType.valueOf(typeRaw.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                type = GameType.ALL;
            }
            PortalRegion p = new PortalRegion(nameKey, world, type);
            p.x1 = config.getInt(base + ".x1");
            p.y1 = config.getInt(base + ".y1");
            p.z1 = config.getInt(base + ".z1");
            p.x2 = config.getInt(base + ".x2");
            p.y2 = config.getInt(base + ".y2");
            p.z2 = config.getInt(base + ".z2");
            p.pos1Set = config.getBoolean(base + ".pos1Set", false);
            p.pos2Set = config.getBoolean(base + ".pos2Set", false);
            p.luckyMode = config.getBoolean(base + ".luckyMode", false);
            portals.put(nameKey.toLowerCase(Locale.ROOT), p);
        }
    }

    private void saveNow() {
        FileConfiguration config = plugin.getConfig();
        // Clear existing section and rewrite
        config.set("lobby-water-portals", null);
        for (PortalRegion p : portals.values()) {
            writePortal(p);
        }
        plugin.saveConfig();
    }

    public PortalRegion create(String name, String worldName) {
        String key = key(name);
        PortalRegion existing = portals.get(key);
        if (existing != null) {
            return existing;
        }
        PortalRegion p = new PortalRegion(name, worldName, GameType.ALL);
        portals.put(key, p);
        writePortal(p);
        saveNow();
        return p;
    }

    public boolean delete(String name) {
        PortalRegion removed = portals.remove(key(name));
        if (removed == null) {
            return false;
        }
        saveNow();
        return true;
    }

    public PortalRegion get(String name) {
        return portals.get(key(name));
    }

    public ArrayList<PortalRegion> all() {
        return new ArrayList<>(portals.values());
    }

    public void setPos1(String name, Location loc) {
        PortalRegion p = get(name);
        if (p == null) {
            return;
        }
        p.world = loc.getWorld().getName();
        p.x1 = loc.getBlockX();
        p.y1 = loc.getBlockY();
        p.z1 = loc.getBlockZ();
        p.pos1Set = true;
        writePortal(p);
        saveNow();
    }

    public void setPos2(String name, Location loc) {
        PortalRegion p = get(name);
        if (p == null) {
            return;
        }
        p.world = loc.getWorld().getName();
        p.x2 = loc.getBlockX();
        p.y2 = loc.getBlockY();
        p.z2 = loc.getBlockZ();
        p.pos2Set = true;
        writePortal(p);
        saveNow();
    }

    public void setType(String name, GameType type) {
        PortalRegion p = get(name);
        if (p == null) {
            return;
        }
        p.type = type;
        writePortal(p);
        saveNow();
    }

    public void setLuckyMode(String name, boolean luckyMode) {
        PortalRegion p = get(name);
        if (p == null) {
            return;
        }
        p.luckyMode = luckyMode;
        writePortal(p);
        saveNow();
    }

    private void writePortal(PortalRegion p) {
        FileConfiguration config = plugin.getConfig();
        String base = "lobby-water-portals." + p.name;
        config.set(base + ".world", p.world);
        config.set(base + ".type", p.type.name());
        config.set(base + ".x1", p.x1);
        config.set(base + ".y1", p.y1);
        config.set(base + ".z1", p.z1);
        config.set(base + ".x2", p.x2);
        config.set(base + ".y2", p.y2);
        config.set(base + ".z2", p.z2);
        config.set(base + ".pos1Set", p.pos1Set);
        config.set(base + ".pos2Set", p.pos2Set);
        config.set(base + ".luckyMode", p.luckyMode);
    }

    public void tryEnterPortal(Player player) {
        if (!SkyWarsReloaded.getCfg().protectLobby()) {
            return;
        }
        if (!Util.get().isSpawnWorld(player.getWorld())) {
            return;
        }
        if (portalJoinRetryPending.contains(player.getUniqueId())) {
            return;
        }
        if (MatchManager.get().getPlayerMap(player) != null) {
            return;
        }
        if (!isInWater(player.getLocation().getBlock().getType())) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = playerCooldown.get(player.getUniqueId());
        if (last != null && now - last < COOLDOWN_MS) {
            return;
        }

        for (PortalRegion portal : portals.values()) {
            if (!portal.isReady() || !portal.contains(player.getLocation())) {
                continue;
            }
            // Never use sw.admin.joinBypass spectator fallback from water portals — admins expect to play.
            if (portal.luckyMode && portal.type != GameType.SINGLE) {
                continue;
            }
            GameMap joinedMap = MatchManager.get().joinGame(player, portal.type, portal.luckyMode, false);
            if (joinedMap != null) {
                playerCooldown.put(player.getUniqueId(), now);
                return;
            }
            portalJoinRetryPending.add(player.getUniqueId());
            plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    portalJoinRetryPending.remove(player.getUniqueId());
                    if (!player.isOnline()) {
                        return;
                    }
                    if (MatchManager.get().getPlayerMap(player) != null) {
                        return;
                    }
                    if (!Util.get().isSpawnWorld(player.getWorld())) {
                        return;
                    }
                    if (!isInWater(player.getLocation().getBlock().getType())) {
                        return;
                    }
                    if (!portal.contains(player.getLocation())) {
                        return;
                    }
                    GameMap retryMap = MatchManager.get().joinGame(player, portal.type, portal.luckyMode, false);
                    playerCooldown.put(player.getUniqueId(), System.currentTimeMillis());
                    if (retryMap == null) {
                        player.sendMessage(new Messaging.MessageFormatter().format("error.could-not-join"));
                    }
                }
            }, JOIN_RETRY_TICKS);
            return;
        }
    }

    private static boolean isInWater(Material material) {
        if (material == Material.WATER) return true;
        // STATIONARY_WATER was removed in 1.13+, check by name for legacy support
        try {
            return material.name().equals("STATIONARY_WATER");
        } catch (Exception e) {
            return false;
        }
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public static String formatPortal(PortalRegion p) {
        String status = p.isReady() ? ChatColor.GREEN + "ready" : ChatColor.YELLOW + "incomplete";
        return ChatColor.AQUA + p.name + ChatColor.GRAY + " (" + p.type.name().toLowerCase(Locale.ROOT)
                + ", lucky=" + (p.luckyMode ? "on" : "off")
                + ", " + p.world + ") " + status;
    }

    public static final class PortalRegion {
        public final String name;
        public String world;
        public int x1;
        public int y1;
        public int z1;
        public int x2;
        public int y2;
        public int z2;
        public boolean pos1Set;
        public boolean pos2Set;
        public GameType type;
        public boolean luckyMode = false;

        PortalRegion(String name, String world, GameType type) {
            this.name = name;
            this.world = world;
            this.type = type;
        }

        public boolean isReady() {
            return pos1Set && pos2Set && world != null && !world.isEmpty();
        }

        public boolean contains(Location loc) {
            if (loc.getWorld() == null || world == null || !world.equals(loc.getWorld().getName()) || !isReady()) {
                return false;
            }
            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();
            return between(x, x1, x2) && between(y, y1, y2) && between(z, z1, z2);
        }

        private static boolean between(int v, int a, int b) {
            int min = Math.min(a, b);
            int max = Math.max(a, b);
            return v >= min && v <= max;
        }
    }
}
