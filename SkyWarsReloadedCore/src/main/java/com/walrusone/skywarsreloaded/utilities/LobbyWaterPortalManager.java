package com.walrusone.skywarsreloaded.utilities;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.enums.GameType;
import com.walrusone.skywarsreloaded.game.GameMap;
import com.walrusone.skywarsreloaded.managers.MatchManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
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
    private final File file;
    private YamlConfiguration cfg;
    private final Map<String, PortalRegion> portals = new HashMap<>();
    private final Map<UUID, Long> playerCooldown = new HashMap<>();
    /** Prevents move-spam and overlapping delayed join retries for the same player. */
    private final Set<UUID> portalJoinRetryPending = Collections.synchronizedSet(new HashSet<UUID>());
    private static final long COOLDOWN_MS = 2000L;
    private static final long JOIN_RETRY_TICKS = 5L;

    public LobbyWaterPortalManager(SkyWarsReloaded plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "lobby-water-portals.yml");
        reload();
    }

    public void reload() {
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    plugin.getLogger().warning("Could not create lobby-water-portals.yml");
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create lobby-water-portals.yml: " + e.getMessage());
            }
        }
        cfg = YamlConfiguration.loadConfiguration(file);
        portals.clear();

        ConfigurationSection sec = cfg.getConfigurationSection("portals");
        if (sec == null) {
            return;
        }
        for (String nameKey : sec.getKeys(false)) {
            String base = "portals." + nameKey;
            String world = cfg.getString(base + ".world");
            if (world == null || world.isEmpty()) {
                continue;
            }
            String typeRaw = cfg.getString(base + ".type", "ALL");
            GameType type;
            try {
                type = GameType.valueOf(typeRaw.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                type = GameType.ALL;
            }
            PortalRegion p = new PortalRegion(nameKey, world, type);
            p.x1 = cfg.getInt(base + ".x1");
            p.y1 = cfg.getInt(base + ".y1");
            p.z1 = cfg.getInt(base + ".z1");
            p.x2 = cfg.getInt(base + ".x2");
            p.y2 = cfg.getInt(base + ".y2");
            p.z2 = cfg.getInt(base + ".z2");
            p.pos1Set = cfg.getBoolean(base + ".pos1Set", false);
            p.pos2Set = cfg.getBoolean(base + ".pos2Set", false);
            p.luckyMode = cfg.getBoolean(base + ".luckyMode", false);
            portals.put(nameKey.toLowerCase(Locale.ROOT), p);
        }
    }

    private void saveNow() {
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save lobby-water-portals.yml: " + e.getMessage());
        }
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
        cfg.set("portals." + removed.name, null);
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
        String base = "portals." + p.name;
        cfg.set(base + ".world", p.world);
        cfg.set(base + ".type", p.type.name());
        cfg.set(base + ".x1", p.x1);
        cfg.set(base + ".y1", p.y1);
        cfg.set(base + ".z1", p.z1);
        cfg.set(base + ".x2", p.x2);
        cfg.set(base + ".y2", p.y2);
        cfg.set(base + ".z2", p.z2);
        cfg.set(base + ".pos1Set", p.pos1Set);
        cfg.set(base + ".pos2Set", p.pos2Set);
        cfg.set(base + ".luckyMode", p.luckyMode);
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
        return material == Material.WATER || material == Material.STATIONARY_WATER;
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
