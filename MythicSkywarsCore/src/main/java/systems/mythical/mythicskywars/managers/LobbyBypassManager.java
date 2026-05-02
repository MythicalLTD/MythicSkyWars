package systems.mythical.mythicskywars.managers;

import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class LobbyBypassManager {
    private static final Set<UUID> BYPASS_PLAYERS = new HashSet<>();

    private LobbyBypassManager() {
    }

    public static boolean toggle(Player player) {
        if (player == null) {
            return false;
        }
        UUID uuid = player.getUniqueId();
        if (BYPASS_PLAYERS.contains(uuid)) {
            BYPASS_PLAYERS.remove(uuid);
            return false;
        }
        BYPASS_PLAYERS.add(uuid);
        return true;
    }

    public static boolean hasBypass(Player player) {
        return player != null && BYPASS_PLAYERS.contains(player.getUniqueId());
    }
}
