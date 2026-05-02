package com.walrusone.skywarsreloaded.utilities;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import eu.decentsoftware.holograms.api.DHAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class SoulWellManager {

    private static final String HOLO_ID = "swr_soulwell_holo";
    private final SkyWarsReloaded plugin;
    private Location wellLocation;
    private boolean clickDebugEnabled;

    public SoulWellManager(SkyWarsReloaded plugin) {
        this.plugin = plugin;
        migrateOldFile();
        reload();
    }

    /**
     * Migrates data from the old soulwell.yml file into config.yml and deletes the old file.
     */
    private void migrateOldFile() {
        File oldFile = new File(plugin.getDataFolder(), "soulwell.yml");
        if (oldFile.exists()) {
            plugin.getLogger().info("Migrating soulwell.yml into config.yml...");
            FileConfiguration oldCfg = YamlConfiguration.loadConfiguration(oldFile);
            String serialized = oldCfg.getString("well-location", "");
            boolean debug = oldCfg.getBoolean("click-debug-enabled", false);

            FileConfiguration config = plugin.getConfig();
            if (serialized != null && !serialized.isEmpty()) {
                config.set("soulwell.well-location", serialized);
            }
            config.set("soulwell.click-debug-enabled", debug);
            plugin.saveConfig();

            if (oldFile.delete()) {
                plugin.getLogger().info("Successfully migrated soulwell.yml into config.yml and deleted old file.");
            } else {
                plugin.getLogger().warning("Migrated soulwell.yml data but failed to delete old file.");
            }
        }
    }

    public void reload() {
        FileConfiguration config = plugin.getConfig();
        String serialized = config.getString("soulwell.well-location", "");
        wellLocation = serialized == null || serialized.isEmpty() ? null : Util.get().stringToLocation(serialized);
        clickDebugEnabled = config.getBoolean("soulwell.click-debug-enabled", false);
        refreshHologram();
    }

    public void save() {
        FileConfiguration config = plugin.getConfig();
        config.set("soulwell.well-location", wellLocation == null ? null : Util.get().locationToString(wellLocation));
        config.set("soulwell.click-debug-enabled", clickDebugEnabled);
        plugin.saveConfig();
    }

    public Location getWellLocation() {
        return wellLocation == null ? null : wellLocation.clone();
    }

    public void setWellLocation(Location location) {
        if (location == null) {
            this.wellLocation = null;
        } else {
            this.wellLocation = new Location(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        }
        save();
        refreshHologram();
    }

    public boolean isWellBlock(Location location) {
        if (wellLocation == null || location == null || location.getWorld() == null || wellLocation.getWorld() == null) {
            return false;
        }
        return location.getWorld().getName().equals(wellLocation.getWorld().getName())
                && location.getBlockX() == wellLocation.getBlockX()
                && location.getBlockY() == wellLocation.getBlockY()
                && location.getBlockZ() == wellLocation.getBlockZ();
    }

    /**
     * Accept exact block or directly adjacent frame block to make setup/use less brittle.
     */
    public boolean isWellInteractBlock(Location location) {
        if (wellLocation == null || location == null || location.getWorld() == null || wellLocation.getWorld() == null) {
            return false;
        }
        if (!location.getWorld().getName().equals(wellLocation.getWorld().getName())) {
            return false;
        }
        int dx = Math.abs(location.getBlockX() - wellLocation.getBlockX());
        int dy = Math.abs(location.getBlockY() - wellLocation.getBlockY());
        int dz = Math.abs(location.getBlockZ() - wellLocation.getBlockZ());
        return dy == 0 && dx <= 1 && dz <= 1;
    }

    public boolean isInSpawnWorld(Player player) {
        return player != null && Util.get().isSpawnWorld(player.getWorld());
    }

    public boolean canUseSoulWell(Player player) {
        if (player == null || wellLocation == null || player.getWorld() == null || wellLocation.getWorld() == null) {
            return false;
        }
        return player.getWorld().getName().equals(wellLocation.getWorld().getName());
    }

    public boolean hasWellConfigured() {
        return wellLocation != null;
    }

    public boolean isClickDebugEnabled() {
        return clickDebugEnabled;
    }

    public void setClickDebugEnabled(boolean clickDebugEnabled) {
        this.clickDebugEnabled = clickDebugEnabled;
        save();
    }

    public boolean isNearWell(Location location, double radius) {
        if (wellLocation == null || location == null || location.getWorld() == null || wellLocation.getWorld() == null) {
            return false;
        }
        if (!location.getWorld().getName().equals(wellLocation.getWorld().getName())) {
            return false;
        }
        double r2 = radius * radius;
        return location.distanceSquared(wellLocation.clone().add(0.5, 0.5, 0.5)) <= r2;
    }

    public void refreshHologram() {
        deleteHologram();
        if (wellLocation == null) {
            return;
        }
        if (!Bukkit.getPluginManager().isPluginEnabled("DecentHolograms")) {
            return;
        }
        Location holoLoc = wellLocation.clone().add(0.5, 2.2, 0.5);
        List<String> lines = Arrays.asList(
                new Messaging.MessageFormatter().format("soulwell.holo-title"),
                new Messaging.MessageFormatter().format("soulwell.holo-line-use"),
                new Messaging.MessageFormatter().format("soulwell.holo-line-souls")
        );
        try {
            DHAPI.createHologram(HOLO_ID, holoLoc, false, lines);
        } catch (Throwable ignored) {
        }
    }

    public void deleteHologram() {
        try {
            eu.decentsoftware.holograms.api.holograms.Hologram h = DHAPI.getHologram(HOLO_ID);
            if (h != null) {
                h.delete();
            }
        } catch (Throwable ignored) {
        }
    }

    public String describeWell() {
        if (wellLocation == null || wellLocation.getWorld() == null) {
            return new Messaging.MessageFormatter().format("command.soulwell.not-set");
        }
        World world = wellLocation.getWorld();
        return new Messaging.MessageFormatter()
                .setVariable("world", world.getName())
                .setVariable("x", String.valueOf(wellLocation.getBlockX()))
                .setVariable("y", String.valueOf(wellLocation.getBlockY()))
                .setVariable("z", String.valueOf(wellLocation.getBlockZ()))
                .format("command.soulwell.set-at");
    }
}

