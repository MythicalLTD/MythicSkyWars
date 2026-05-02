package com.walrusone.skywarsreloaded.managers.worlds;

import org.bukkit.World;

import java.io.File;

public interface WorldManager {

    World createEmptyWorld(String name, World.Environment environment);

    boolean loadWorld(String worldName, World.Environment environment, boolean readOnly);

    void unloadWorld(String world, boolean save);

    void copyWorld(File source, File target);

    /**
     *
     * @param name Name of the world to remove
     * @param removeFile Only applies to {@link ASWMWorldManager#deleteWorld(String, boolean)}, whether to delete the world folder.
     */
    void deleteWorld(String name, boolean removeFile);

    void deleteWorld(File file);

    /**
     * Gets the actual filesystem folder for a world by name.
     * In MC 1.21.11+ (Paper/Purpur 26.1+), worlds are stored under world/dimensions/minecraft/{name}
     * rather than as top-level folders.
     */
    default File getWorldFolder(String worldName) {
        File legacy = new File(org.bukkit.Bukkit.getWorldContainer().getAbsolutePath(), worldName);
        if (legacy.exists()) return legacy;
        // Modern path: world/dimensions/minecraft/<name>
        File modern = new File(org.bukkit.Bukkit.getWorldContainer().getAbsolutePath(), "world/dimensions/minecraft/" + worldName);
        if (modern.exists()) return modern;
        // Default to legacy path
        return legacy;
    }

    WorldManagerType getType();

}
