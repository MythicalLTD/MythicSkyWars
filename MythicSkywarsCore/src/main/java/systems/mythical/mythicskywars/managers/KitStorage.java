package systems.mythical.mythicskywars.managers;

import systems.mythical.mythicskywars.MythicSkywars;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class KitStorage {
    private static File file = new File(MythicSkywars.get().getDataFolder(), "savedkits.yml");
    private static FileConfiguration config = YamlConfiguration.loadConfiguration(file);

    public static void saveKit(UUID uuid, String kitName) {
        config.set(uuid.toString(), kitName);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getSavedKit(UUID uuid) {
        return config.getString(uuid.toString());
    }
}
