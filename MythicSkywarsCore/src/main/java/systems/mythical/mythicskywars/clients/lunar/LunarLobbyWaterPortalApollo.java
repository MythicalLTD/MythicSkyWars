package systems.mythical.mythicskywars.clients.lunar;

import com.lunarclient.apollo.Apollo;
import com.lunarclient.apollo.common.location.ApolloBlockLocation;
import com.lunarclient.apollo.module.notification.Notification;
import com.lunarclient.apollo.module.notification.NotificationModule;
import com.lunarclient.apollo.module.waypoint.Waypoint;
import com.lunarclient.apollo.module.waypoint.WaypointModule;
import com.lunarclient.apollo.player.ApolloPlayer;
import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.enums.GameType;
import systems.mythical.mythicskywars.managers.MatchManager;
import systems.mythical.mythicskywars.utilities.LobbyWaterPortalManager;
import systems.mythical.mythicskywars.utilities.Messaging;
import systems.mythical.mythicskywars.utilities.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.awt.Color;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Lunar waypoints + hints for lobby {@code lobby-water-portals} regions while players are in the lobby.
 * Text comes from {@code messages.yml} keys configured in {@code clients/lunar/config.yml}.
 */
public final class LunarLobbyWaterPortalApollo {

    private static boolean enabled;
    private static String defWaypointNameMsg;
    private static String defHintTitleMsg;
    private static String defHintDescMsg;
    private static boolean hintEnabled;
    private static boolean hintOnlyOnEnter;
    private static int hintSeconds;
    private static Color waypointColor;

    private static final Map<String, PortalOverride> portalOverrides = new HashMap<String, PortalOverride>();

    /** Last portal config name (lowercase) the player was considered "in" for enter detection. */
    private static final Map<UUID, String> lastPortalKey = new HashMap<UUID, String>();
    /** Last Lunar waypoint label shown (must match removeWaypoint). */
    private static final Map<UUID, String> lastWaypointLabel = new HashMap<UUID, String>();

    private static final class PortalOverride {
        final String waypointNameMsg;
        final String hintTitleMsg;
        final String hintDescMsg;

        PortalOverride(String waypointNameMsg, String hintTitleMsg, String hintDescMsg) {
            this.waypointNameMsg = waypointNameMsg;
            this.hintTitleMsg = hintTitleMsg;
            this.hintDescMsg = hintDescMsg;
        }
    }

    private LunarLobbyWaterPortalApollo() {
    }

    public static void reload(FileConfiguration cfg) {
        portalOverrides.clear();
        if (cfg == null) {
            enabled = false;
            return;
        }
        ConfigurationSection root = cfg.getConfigurationSection("lobby-water-portals");
        if (root == null) {
            enabled = false;
            return;
        }
        enabled = root.getBoolean("enabled", true);
        ConfigurationSection def = root.getConfigurationSection("defaults");
        if (def == null) {
            defWaypointNameMsg = "lunar.water-portal.waypoint-name";
            defHintTitleMsg = "lunar.water-portal.hint-title";
            defHintDescMsg = "lunar.water-portal.hint-desc";
            hintEnabled = true;
            hintOnlyOnEnter = true;
            hintSeconds = 4;
            waypointColor = new Color(0x00CCFF);
        } else {
            defWaypointNameMsg = def.getString("waypoint-name-message", "lunar.water-portal.waypoint-name");
            defHintTitleMsg = def.getString("hint-title-message", "lunar.water-portal.hint-title");
            defHintDescMsg = def.getString("hint-description-message", "lunar.water-portal.hint-desc");
            hintEnabled = def.getBoolean("hint-notification-enabled", true);
            hintOnlyOnEnter = def.getBoolean("hint-only-on-enter", true);
            hintSeconds = Math.max(1, def.getInt("hint-display-seconds", 4));
            waypointColor = LunarApolloBridge.parseColor(def.getString("waypoint-color", "#00CCFF"), new Color(0x00CCFF));
        }
        ConfigurationSection portals = root.getConfigurationSection("portals");
        if (portals != null) {
            for (String key : portals.getKeys(false)) {
                ConfigurationSection one = portals.getConfigurationSection(key);
                if (one == null) {
                    continue;
                }
                String lk = key.toLowerCase(Locale.ROOT);
                portalOverrides.put(lk, new PortalOverride(
                        one.contains("waypoint-name-message") ? one.getString("waypoint-name-message") : null,
                        one.contains("hint-title-message") ? one.getString("hint-title-message") : null,
                        one.contains("hint-description-message") ? one.getString("hint-description-message") : null
                ));
            }
        }
    }

    public static void shutdown() {
        lastPortalKey.clear();
        lastWaypointLabel.clear();
    }

    public static void onQuit(Player player) {
        if (player != null) {
            clearLunarFor(player);
        }
    }

    /**
     * Call from lobby move listener when the player changes block (or similar).
     */
    public static void onPlayerMove(Player player, Location to) {
        if (!enabled || !LunarApolloBridge.isUsable() || player == null || to == null) {
            return;
        }
        if (MatchManager.get().getPlayerMap(player) != null) {
            clearLunarFor(player);
            return;
        }
        if (to.getWorld() == null || !Util.get().isSpawnWorld(to.getWorld())) {
            clearLunarFor(player);
            return;
        }
        LobbyWaterPortalManager mgr = MythicSkywars.getLobbyWaterPortals();
        if (mgr == null) {
            clearLunarFor(player);
            return;
        }
        LobbyWaterPortalManager.PortalRegion inside = null;
        for (LobbyWaterPortalManager.PortalRegion p : mgr.all()) {
            if (p.isReady() && p.contains(to)) {
                inside = p;
                break;
            }
        }
        if (inside == null) {
            clearLunarFor(player);
            return;
        }
        UUID id = player.getUniqueId();
        String portalKey = inside.name.toLowerCase(Locale.ROOT);
        boolean entered = !portalKey.equals(lastPortalKey.get(id));
        lastPortalKey.put(id, portalKey);

        Optional<ApolloPlayer> apOpt = Apollo.getPlayerManager().getPlayer(id);
        if (!apOpt.isPresent()) {
            return;
        }
        ApolloPlayer ap = apOpt.get();

        String waypointLabel = formatMessage(resolveWaypointKey(inside), player, inside);
        String prevLabel = lastWaypointLabel.get(id);
        WaypointModule wMod = Apollo.getModuleManager().getModule(WaypointModule.class);
        if (wMod != null && Apollo.getModuleManager().isEnabled(WaypointModule.class)) {
            if (prevLabel != null && !prevLabel.equals(waypointLabel)) {
                wMod.removeWaypoint(ap, prevLabel);
            }
            Location center = inside.getCenterLocation(Bukkit.getServer());
            if (center != null && center.getWorld() != null) {
                ApolloBlockLocation block = ApolloBlockLocation.builder()
                        .world(center.getWorld().getName())
                        .x(center.getBlockX())
                        .y(center.getBlockY())
                        .z(center.getBlockZ())
                        .build();
                Waypoint wp = Waypoint.builder()
                        .name(waypointLabel)
                        .location(block)
                        .color(waypointColor)
                        .preventRemoval(false)
                        .hidden(false)
                        .build();
                wMod.removeWaypoint(ap, waypointLabel);
                wMod.displayWaypoint(ap, wp);
            }
        }
        lastWaypointLabel.put(id, waypointLabel);

        if (hintEnabled && (!hintOnlyOnEnter || entered)) {
            showHint(ap, player, inside);
        }
    }

    private static String resolveWaypointKey(LobbyWaterPortalManager.PortalRegion inside) {
        PortalOverride o = portalOverrides.get(inside.name.toLowerCase(Locale.ROOT));
        if (o != null && o.waypointNameMsg != null && !o.waypointNameMsg.isEmpty()) {
            return o.waypointNameMsg;
        }
        return defWaypointNameMsg;
    }

    private static void showHint(ApolloPlayer ap, Player player, LobbyWaterPortalManager.PortalRegion inside) {
        NotificationModule nMod = Apollo.getModuleManager().getModule(NotificationModule.class);
        if (nMod == null || !Apollo.getModuleManager().isEnabled(NotificationModule.class)) {
            return;
        }
        PortalOverride o = portalOverrides.get(inside.name.toLowerCase(Locale.ROOT));
        String titleKey = o != null && o.hintTitleMsg != null && !o.hintTitleMsg.isEmpty() ? o.hintTitleMsg : defHintTitleMsg;
        String descKey = o != null && o.hintDescMsg != null && !o.hintDescMsg.isEmpty() ? o.hintDescMsg : defHintDescMsg;
        try {
            String titleRaw = formatMessage(titleKey, player, inside);
            String descRaw = formatMessage(descKey, player, inside);
            String title = LunarApolloBridge.apolloNotificationTitlePlain(titleRaw);
            String desc = LunarApolloBridge.apolloNotificationDescriptionPlain(descRaw);
            Notification n = LunarApolloBridge.buildApolloNotification(
                    title, desc, Duration.ofSeconds(hintSeconds), "AQUA", "GRAY", "textures/blocks/water_still.png");
            if (n != null) {
                nMod.displayNotification(ap, n);
            }
        } catch (Throwable t) {
            MythicSkywars.get().getLogger().log(Level.FINE, "Lunar lobby portal hint failed", t);
        }
    }

    private static String formatMessage(String messageOrKey, Player player, LobbyWaterPortalManager.PortalRegion inside) {
        String luckyLine = new Messaging.MessageFormatter().format(
                inside.luckyMode ? "lunar.water-portal.lucky-on" : "lunar.water-portal.lucky-off");
        Messaging.MessageFormatter mf = new Messaging.MessageFormatter()
                .setVariable("portal", inside.name)
                .setVariable("type", inside.type.name().toLowerCase(Locale.ROOT))
                .setVariable("friendly_type", friendlyType(inside.type))
                .setVariable("lucky", inside.luckyMode ? "on" : "off")
                .setVariable("lucky_line", luckyLine);
        return mf.format(messageOrKey, true, player);
    }

    private static String friendlyType(GameType t) {
        if (t == GameType.SINGLE) {
            return "Solo";
        }
        if (t == GameType.TEAM) {
            return "Teams";
        }
        if (t == GameType.ALL) {
            return "All modes";
        }
        return t.name();
    }

    private static void clearLunarFor(Player player) {
        UUID id = player.getUniqueId();
        lastPortalKey.remove(id);
        String label = lastWaypointLabel.remove(id);
        if (!LunarApolloBridge.isUsable() || label == null) {
            return;
        }
        Optional<ApolloPlayer> apOpt = Apollo.getPlayerManager().getPlayer(id);
        if (!apOpt.isPresent()) {
            return;
        }
        WaypointModule wMod = Apollo.getModuleManager().getModule(WaypointModule.class);
        if (wMod != null && Apollo.getModuleManager().isEnabled(WaypointModule.class)) {
            wMod.removeWaypoint(apOpt.get(), label);
        }
    }
}
