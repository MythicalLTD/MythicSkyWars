package com.walrusone.skywarsreloaded.utilities.holograms;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.enums.LeaderType;
import com.walrusone.skywarsreloaded.managers.Leaderboard;
import com.walrusone.skywarsreloaded.utilities.Util;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Leaderboard holograms via DecentHolograms ({@link DHAPI}).
 */
public class DecentHoloUtil extends HologramsUtil {

    private static final HashMap<LeaderType, HashMap<String, ArrayList<String>>> hologramIds = new HashMap<>();

    public DecentHoloUtil() {
        getFC();
    }

    @Override
    public void createLeaderHologram(Location loc, LeaderType type, String formatKey) {
        if (SkyWarsReloaded.getCfg().debugEnabled()) {
            SkyWarsReloaded.get().getLogger().info(this.getClass().getName() + "#debug::createLeaderHologram: HEADER");
        }
        String base = "swr_lb_" + type.name().toLowerCase(Locale.ROOT) + "_" + sanitizeKey(formatKey);
        String id = base + "_" + UUID.randomUUID().toString().replace("-", "");
        while (DHAPI.getHologram(id) != null) {
            id = base + "_" + UUID.randomUUID().toString().replace("-", "");
        }
        DHAPI.createHologram(id, loc, false, Collections.singletonList(ChatColor.GRAY + "..."));
        hologramIds.computeIfAbsent(type, k -> new HashMap<>());
        hologramIds.get(type).computeIfAbsent(formatKey, k -> new ArrayList<>());
        hologramIds.get(type).get(formatKey).add(id);

        if (fc == null) {
            getFC();
        }
        if (fc != null) {
            List<String> locs = new ArrayList<>();
            for (String hid : hologramIds.get(type).get(formatKey)) {
                Hologram hol = DHAPI.getHologram(hid);
                if (hol != null) {
                    locs.add(Util.get().locationToString(hol.getLocation()));
                }
            }
            fc.set("leaderboard." + type.toString().toLowerCase(Locale.ROOT) + "." + formatKey + ".locations", locs);
            try {
                fc.save(holoFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (SkyWarsReloaded.getCfg().debugEnabled()) {
            SkyWarsReloaded.get().getLogger().info(this.getClass().getName() + "#debug::createLeaderHologram: id = " + id);
        }
        updateLeaderHologramsWhenReady(type);
    }

    private static String sanitizeKey(String formatKey) {
        return formatKey.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    /**
     * Only update the holograms when the leaderboard obj is no longer null.
     */
    public void updateLeaderHologramsWhenReady(LeaderType type) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (SkyWarsReloaded.getLB() != null) {
                    this.cancel();
                    updateLeaderHolograms(type);
                }
            }
        }.runTaskTimer(SkyWarsReloaded.get(), 10, 10);
    }

    @Override
    public void updateLeaderHolograms(LeaderType type) {
        Leaderboard lbManager = SkyWarsReloaded.getLB();

        if (SkyWarsReloaded.get().serverLoaded()) {
            hologramIds.computeIfAbsent(type, k -> new HashMap<>());
            if (SkyWarsReloaded.getCfg().isTypeEnabled(type)) {
                if (fc == null) {
                    getFC();
                }
                if (fc != null) {
                    for (String key : hologramIds.get(type).keySet()) {
                        for (String hid : hologramIds.get(type).get(key)) {
                            Hologram hologram = DHAPI.getHologram(hid);
                            if (hologram == null) {
                                continue;
                            }
                            if (SkyWarsReloaded.getCfg().debugEnabled()) {
                                SkyWarsReloaded.get().getLogger().info(this.getClass().getName() + "#debug::updateLeaderHolograms: hologram PRE = " + hid);
                            }
                            clearAllLines(hologram);
                            List<String> format = fc.getStringList("leaderboard." + type.toString().toLowerCase(Locale.ROOT) + "." + key + ".format");
                            for (int i = 0; i < format.size(); i++) {
                                String line = getFormattedString(format.get(i), type);
                                if (line.startsWith("item:")) {
                                    ItemStack item = null;
                                    String mat = line.substring(5);
                                    if (mat.contains("playerhead")) {
                                        String num = mat.substring(12, mat.length() - 1);
                                        if (Util.get().isInteger(num) && lbManager.getTopList(type) != null
                                                && lbManager.getTopList(type).size() > Integer.parseInt(num) - 1) {
                                            Player player = Bukkit.getPlayer(lbManager.getTopList(type).get(Integer.parseInt(num) - 1).getUUID());
                                            if (player != null) {
                                                if (SkyWarsReloaded.getNMS().getVersion() < 13) {
                                                    item = new ItemStack(Material.valueOf("SKULL_ITEM"), 1, (short) 3);
                                                } else {
                                                    item = new ItemStack(Material.valueOf("PLAYER_HEAD"), 1);
                                                }
                                                SkullMeta meta1 = (SkullMeta) item.getItemMeta();
                                                SkyWarsReloaded.getNMS().updateSkull(meta1, player);
                                                meta1.setDisplayName(ChatColor.YELLOW + player.getName());
                                                item.setItemMeta(meta1);
                                            }
                                        }
                                    } else {
                                        Material material = Material.matchMaterial(mat);
                                        if (material != null) {
                                            item = new ItemStack(material, 1);
                                        }
                                    }
                                    if (item != null) {
                                        DHAPI.addHologramLine(hologram, item);
                                    } else {
                                        DHAPI.addHologramLine(hologram, "  ");
                                    }
                                } else {
                                    DHAPI.addHologramLine(hologram, ChatColor.translateAlternateColorCodes('&', line));
                                }
                            }
                            if (SkyWarsReloaded.getCfg().debugEnabled()) {
                                SkyWarsReloaded.get().getLogger().info(this.getClass().getName() + "#debug::updateLeaderHolograms: hologram POST = " + hid);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void clearAllLines(Hologram h) {
        while (true) {
            try {
                if (DHAPI.removeHologramLine(h, 0) == null) {
                    break;
                }
            } catch (Exception e) {
                break;
            }
        }
    }

    @Override
    public boolean removeHologram(Location loc) {
        if (SkyWarsReloaded.getCfg().debugEnabled()) {
            SkyWarsReloaded.get().getLogger().info(this.getClass().getName() + "#debug::removeHologram loc = " + loc);
        }
        String idToRemove = null;
        LeaderType typeToRemove = null;
        String keyToRemove = null;
        double distance = 1000000;
        for (LeaderType type : LeaderType.values()) {
            if (SkyWarsReloaded.getCfg().isTypeEnabled(type)) {
                if (fc != null && fc.getConfigurationSection("leaderboard." + type.toString().toLowerCase(Locale.ROOT)) != null) {
                    for (String key : fc.getConfigurationSection("leaderboard." + type.toString().toLowerCase(Locale.ROOT)).getKeys(false)) {
                        if (hologramIds.get(type) != null && hologramIds.get(type).get(key) != null) {
                            for (String hid : hologramIds.get(type).get(key)) {
                                Hologram holo = DHAPI.getHologram(hid);
                                if (holo == null) {
                                    continue;
                                }
                                if (loc.distance(holo.getLocation()) < distance) {
                                    idToRemove = hid;
                                    typeToRemove = type;
                                    keyToRemove = key;
                                    distance = loc.distance(holo.getLocation());
                                }
                            }
                        }
                    }
                }
            }
        }
        if (idToRemove != null) {
            hologramIds.get(typeToRemove).get(keyToRemove).remove(idToRemove);
            Hologram h = DHAPI.getHologram(idToRemove);
            if (h != null) {
                h.delete();
            }
            if (fc != null) {
                List<String> locs = new ArrayList<>();
                for (String hid : hologramIds.get(typeToRemove).get(keyToRemove)) {
                    Hologram holo = DHAPI.getHologram(hid);
                    if (holo != null) {
                        locs.add(Util.get().locationToString(holo.getLocation()));
                    }
                }
                fc.set("leaderboard." + typeToRemove.toString().toLowerCase(Locale.ROOT) + "." + keyToRemove + ".locations", locs);
                try {
                    fc.save(holoFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return true;
        }
        return false;
    }
}
