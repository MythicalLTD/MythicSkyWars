package com.walrusone.skywarsreloaded.utilities;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.enums.MatchState;
import com.walrusone.skywarsreloaded.game.GameMap;
import com.walrusone.skywarsreloaded.managers.MatchManager;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * NTD-LuckyBlock can require {@code luckyblock.break.&lt;color&gt;} when {@code break-permissions} is true.
 * LuckPerms often does not grant those on temporary arena worlds, so we attach superperms for the match.
 */
public final class LuckyBlockBreakAttachment {

    private static final List<String> FALLBACK_COLORS = Collections.unmodifiableList(Arrays.asList(
            "black", "blue", "brown", "cyan", "gray", "green", "light_blue", "light_gray",
            "lime", "magenta", "orange", "pink", "purple", "red", "white", "yellow", "tinted", "iced"
    ));

    private static final Map<UUID, PermissionAttachment> ATTACHMENTS = new HashMap<>();

    private LuckyBlockBreakAttachment() {
    }

    public static void refreshAlivePlayers(GameMap map) {
        if (map == null || !map.isLuckyModeEnabled() || !LuckyBlockHook.isAvailable() || !LuckyBlockHook.isGrantBreakPermissionInMatch()) {
            return;
        }
        for (Player p : map.getAlivePlayers()) {
            apply(p);
        }
    }

    /**
     * Mid-game join: {@link MatchManager} prepares a player already in {@code PLAYING}.
     */
    public static void applyIfLuckyPlaying(Player player, GameMap gameMap) {
        if (player == null || !player.isOnline() || gameMap == null) {
            return;
        }
        if (gameMap.getMatchState() != MatchState.PLAYING) {
            return;
        }
        if (!gameMap.isLuckyModeEnabled() || !LuckyBlockHook.isAvailable() || !LuckyBlockHook.isGrantBreakPermissionInMatch()) {
            return;
        }
        apply(player);
    }

    public static void apply(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        remove(player);
        if (!LuckyBlockHook.isAvailable() || !LuckyBlockHook.isGrantBreakPermissionInMatch()) {
            return;
        }
        GameMap map = MatchManager.get().getPlayerMap(player);
        if (map == null || !map.isLuckyModeEnabled()) {
            return;
        }
        PermissionAttachment att = player.addAttachment(SkyWarsReloaded.get());
        for (String suffix : listBreakSuffixes()) {
            att.setPermission("luckyblock.break." + suffix, true);
        }
        ATTACHMENTS.put(player.getUniqueId(), att);
    }

    public static void remove(Player player) {
        if (player == null) {
            return;
        }
        remove(player.getUniqueId());
    }

    public static void remove(UUID uuid) {
        PermissionAttachment old = ATTACHMENTS.remove(uuid);
        if (old != null) {
            old.remove();
        }
    }

    private static List<String> listBreakSuffixes() {
        List<String> out = new ArrayList<>();
        try {
            Class<?> enumClass = Class.forName("me.DenBeKKer.ntdLuckyBlock.LBMain$LuckyBlockType");
            Object[] constants = enumClass.getEnumConstants();
            if (constants != null) {
                for (Object c : constants) {
                    out.add(((Enum<?>) c).name().toLowerCase());
                }
            }
        } catch (Throwable t) {
            SkyWarsReloaded.get().getLogger().log(Level.FINE, "LuckyBlockBreakAttachment: enum not loaded", t);
        }
        if (out.isEmpty()) {
            out.addAll(FALLBACK_COLORS);
        }
        return out;
    }
}
