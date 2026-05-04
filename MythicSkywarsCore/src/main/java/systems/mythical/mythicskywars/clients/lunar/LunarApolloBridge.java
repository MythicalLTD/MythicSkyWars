package systems.mythical.mythicskywars.clients.lunar;

import com.lunarclient.apollo.Apollo;
import com.lunarclient.apollo.common.icon.ItemStackIcon;
import com.lunarclient.apollo.common.location.ApolloBlockLocation;
import com.lunarclient.apollo.common.location.ApolloLocation;
import com.lunarclient.apollo.module.cooldown.Cooldown;
import com.lunarclient.apollo.module.cooldown.CooldownModule;
import com.lunarclient.apollo.module.glow.GlowModule;
import com.lunarclient.apollo.module.notification.Notification;
import com.lunarclient.apollo.module.notification.NotificationModule;
import com.lunarclient.apollo.module.richpresence.RichPresenceModule;
import com.lunarclient.apollo.module.richpresence.ServerRichPresence;
import com.lunarclient.apollo.module.team.TeamMember;
import com.lunarclient.apollo.module.team.TeamModule;
import com.lunarclient.apollo.module.title.TitleModule;
import com.lunarclient.apollo.module.waypoint.Waypoint;
import com.lunarclient.apollo.module.waypoint.WaypointModule;
import com.lunarclient.apollo.player.ApolloPlayer;
import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.enums.MatchState;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.game.PlayerCard;
import systems.mythical.mythicskywars.game.TeamCard;
import systems.mythical.mythicskywars.menus.gameoptions.objects.CoordLoc;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.awt.Color;
import java.io.File;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Lunar Client Apollo integration (requires Apollo on the server).
 * Settings: {@code plugins/MythicSkywars/clients/lunar/config.yml}
 */
public final class LunarApolloBridge {

    private static final String LUNAR_CONFIG = "clients/lunar/config.yml";
    private static final int NOTIFICATION_TITLE_MAX = 15;
    private static final double TRACK_RANGE = 48.0;

    private static boolean apolloClassesAvailable;
    private static boolean yamlEnabled;
    private static boolean teamModuleEnabled;
    private static long teamRefreshTicks = 1L;
    private static boolean cooldownModuleEnabled;
    private static String pvpCooldownId = "msw-pvp";
    private static String pearlCooldownId = "msw-pearl";
    private static int pearlCooldownSeconds = 15;
    private static String pvpIconMaterial = "DIAMOND_SWORD";

    private static boolean notificationEnabled;
    private static int notificationDisplaySeconds = 4;
    private static String notifMatchTitle;
    private static String notifMatchDesc;
    private static String notifPvpTitle;
    private static String notifPvpDesc;
    private static String notifRefillTitle;
    private static String notifRefillDesc;
    private static String notifVictoryTitle;
    private static String notifVictoryDesc;

    private static boolean waypointEnabled;
    /** auto | spectator | centroid | border */
    private static String waypointMode = "auto";
    private static String waypointName = "Spectate";
    private static Color waypointColor = new Color(0xFFAA00);
    private static boolean waypointResetOnEnd;

    private static boolean glowEnabled;
    private static boolean glowTeammates;
    private static boolean glowEnemiesSolo;
    /** When true, glow is only sent if the viewer has unobstructed line of sight (no x-ray through walls). */
    private static boolean glowRequireLineOfSight = true;
    /** Max horizontal distance in blocks (0 = unlimited). Uses viewer eye to target bounding box for LOS. */
    private static int glowMaxHorizontalBlocks = 48;

    private static boolean richPresenceEnabled;

    private static final Map<String, BukkitTask> teamTasks = new ConcurrentHashMap<String, BukkitTask>();

    static {
        try {
            Class.forName("com.lunarclient.apollo.Apollo");
            apolloClassesAvailable = true;
        } catch (ClassNotFoundException e) {
            apolloClassesAvailable = false;
        }
    }

    private LunarApolloBridge() {
    }

    public static void reload(MythicSkywars plugin) {
        cancelTeamTasks();
        File file = new File(plugin.getDataFolder(), LUNAR_CONFIG);
        if (!file.exists()) {
            yamlEnabled = false;
            LunarLobbyWaterPortalApollo.reload(null);
            return;
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        yamlEnabled = cfg.getBoolean("enabled", true);
        teamModuleEnabled = cfg.getBoolean("modules.team.enabled", true);
        teamRefreshTicks = Math.max(1L, cfg.getLong("modules.team.refresh-ticks", 1L));
        cooldownModuleEnabled = cfg.getBoolean("modules.cooldown.enabled", true);
        pvpCooldownId = cfg.getString("modules.cooldown.entries.pvp-timer.apollo-id", "msw-pvp");
        pearlCooldownId = cfg.getString("modules.cooldown.entries.ender-pearl.apollo-id", "msw-pearl");
        pearlCooldownSeconds = Math.max(1, cfg.getInt("modules.cooldown.pearl-seconds", 15));
        pvpIconMaterial = cfg.getString("modules.cooldown.entries.pvp-timer.icon-material", "DIAMOND_SWORD");

        notificationEnabled = cfg.getBoolean("modules.notification.enabled", true);
        notificationDisplaySeconds = Math.max(1, cfg.getInt("modules.notification.default-display-seconds", 4));
        notifMatchTitle = cfg.getString("modules.notification.match-start.title", "Match live!");
        notifMatchDesc = cfg.getString("modules.notification.match-start.description", "Map: {map}");
        notifPvpTitle = cfg.getString("modules.notification.pvp-enabled.title", "PVP on!");
        notifPvpDesc = cfg.getString("modules.notification.pvp-enabled.description", "You can take damage now.");
        notifRefillTitle = cfg.getString("modules.notification.chest-refill.title", "Refill!");
        notifRefillDesc = cfg.getString("modules.notification.chest-refill.description", "Chests restocked.");
        notifVictoryTitle = cfg.getString("modules.notification.victory.title", "Victory!");
        notifVictoryDesc = cfg.getString("modules.notification.victory.description", "You won on {map}");

        waypointEnabled = cfg.getBoolean("modules.waypoint.enabled", true);
        waypointMode = cfg.getString("modules.waypoint.mode", "auto").trim().toLowerCase(java.util.Locale.ROOT);
        waypointName = cfg.getString("modules.waypoint.name", "Spectate");
        waypointColor = parseColor(cfg.getString("modules.waypoint.color", "#FFAA00"), new Color(0xFFAA00));
        waypointResetOnEnd = cfg.getBoolean("modules.waypoint.reset-on-match-end", true);

        glowEnabled = cfg.getBoolean("modules.glow.enabled", true);
        glowTeammates = cfg.getBoolean("modules.glow.teammates", true);
        glowEnemiesSolo = cfg.getBoolean("modules.glow.enemies-in-solo", false);
        glowRequireLineOfSight = cfg.getBoolean("modules.glow.require-line-of-sight", true);
        glowMaxHorizontalBlocks = Math.max(0, cfg.getInt("modules.glow.max-horizontal-blocks", 48));

        richPresenceEnabled = cfg.getBoolean("modules.rich-presence.enabled", true);

        LunarLobbyWaterPortalApollo.reload(cfg);

        if (yamlEnabled && apolloClassesAvailable && isApolloPluginPresent()) {
            plugin.getLogger().info("Lunar Apollo bridge enabled (clients/lunar/config.yml).");
        } else if (yamlEnabled) {
            plugin.getLogger().warning("clients/lunar/config.yml has enabled: true but Apollo is not available; Lunar features are inactive.");
        }
    }

    public static void shutdown() {
        cancelTeamTasks();
        LunarLobbyWaterPortalApollo.shutdown();
    }

    private static void cancelTeamTasks() {
        for (BukkitTask t : teamTasks.values()) {
            if (t != null) {
                t.cancel();
            }
        }
        teamTasks.clear();
    }

    public static boolean isUsable() {
        return yamlEnabled && apolloClassesAvailable && isApolloPluginPresent();
    }

    /**
     * Official Bukkit distribution uses {@code Apollo-Bukkit}; some builds may register as {@code Apollo}.
     */
    private static boolean isApolloPluginPresent() {
        if (Bukkit.getPluginManager().isPluginEnabled("Apollo-Bukkit")) {
            return true;
        }
        if (Bukkit.getPluginManager().isPluginEnabled("Apollo")) {
            return true;
        }
        for (org.bukkit.plugin.Plugin p : Bukkit.getPluginManager().getPlugins()) {
            if (p == null || !p.isEnabled()) {
                continue;
            }
            String n = p.getName();
            if (n != null && n.startsWith("Apollo")) {
                return true;
            }
        }
        return false;
    }

    public static void onMatchPlaying(GameMap gameMap) {
        if (!isUsable()) {
            return;
        }
        String arenaId = gameMap.getName();
        cancelTeamTask(arenaId);
        showCenterWaypoint(gameMap);
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (gameMap.getMatchState() != MatchState.PLAYING) {
                    cancel();
                    teamTasks.remove(arenaId, this);
                    return;
                }
                try {
                    refreshTeams(gameMap);
                    refreshGlow(gameMap);
                    refreshRichPresence(gameMap);
                } catch (Throwable t) {
                    MythicSkywars.get().getLogger().log(Level.WARNING, "Lunar Apollo match tick failed", t);
                }
            }
        }.runTaskTimer(MythicSkywars.get(), 1L, teamRefreshTicks);
        teamTasks.put(arenaId, task);
    }

    public static void notifyMatchStarted(GameMap gameMap) {
        if (!isUsable() || !notificationEnabled) {
            return;
        }
        NotificationModule mod = Apollo.getModuleManager().getModule(NotificationModule.class);
        if (mod == null || !Apollo.getModuleManager().isEnabled(NotificationModule.class)) {
            return;
        }
        Duration show = Duration.ofSeconds(notificationDisplaySeconds);
        for (Player p : gameMap.getAlivePlayers()) {
            Optional<ApolloPlayer> ap = Apollo.getPlayerManager().getPlayer(p.getUniqueId());
            if (ap.isPresent()) {
                Notification n = buildNotification(notifMatchTitle, notifMatchDesc, show, gameMap, p);
                if (n != null) {
                    mod.displayNotification(ap.get(), n);
                }
            }
        }
    }

    public static void notifyPvpEnabled(Player player, GameMap gameMap) {
        if (!isUsable() || !notificationEnabled) {
            return;
        }
        NotificationModule mod = Apollo.getModuleManager().getModule(NotificationModule.class);
        if (mod == null || !Apollo.getModuleManager().isEnabled(NotificationModule.class)) {
            return;
        }
        Optional<ApolloPlayer> ap = Apollo.getPlayerManager().getPlayer(player.getUniqueId());
        if (ap.isPresent()) {
            Notification n = buildNotification(notifPvpTitle, notifPvpDesc,
                    Duration.ofSeconds(notificationDisplaySeconds), gameMap, player);
            if (n != null) {
                mod.displayNotification(ap.get(), n);
            }
        }
    }

    public static void notifyChestRefill(GameMap gameMap) {
        if (!isUsable() || !notificationEnabled) {
            return;
        }
        NotificationModule mod = Apollo.getModuleManager().getModule(NotificationModule.class);
        if (mod == null || !Apollo.getModuleManager().isEnabled(NotificationModule.class)) {
            return;
        }
        Duration show = Duration.ofSeconds(notificationDisplaySeconds);
        for (Player p : gameMap.getAlivePlayers()) {
            Optional<ApolloPlayer> ap = Apollo.getPlayerManager().getPlayer(p.getUniqueId());
            if (ap.isPresent()) {
                Notification n = buildNotification(notifRefillTitle, notifRefillDesc, show, gameMap, p);
                if (n != null) {
                    mod.displayNotification(ap.get(), n);
                }
            }
        }
    }

    public static void notifyVictory(Player winner, GameMap gameMap) {
        if (!isUsable() || !notificationEnabled || winner == null) {
            return;
        }
        NotificationModule mod = Apollo.getModuleManager().getModule(NotificationModule.class);
        if (mod == null || !Apollo.getModuleManager().isEnabled(NotificationModule.class)) {
            return;
        }
        Optional<ApolloPlayer> ap = Apollo.getPlayerManager().getPlayer(winner.getUniqueId());
        if (ap.isPresent()) {
            Notification n = buildNotification(notifVictoryTitle, notifVictoryDesc,
                    Duration.ofSeconds(Math.max(notificationDisplaySeconds, 5)), gameMap, winner);
            if (n != null) {
                mod.displayNotification(ap.get(), n);
            }
        }
    }

    public static void showPvpProtectionCooldowns(GameMap gameMap, int seconds) {
        if (!isUsable() || !cooldownModuleEnabled || seconds < 1) {
            return;
        }
        CooldownModule mod = Apollo.getModuleManager().getModule(CooldownModule.class);
        if (mod == null || !Apollo.getModuleManager().isEnabled(CooldownModule.class)) {
            return;
        }
        Duration dur = Duration.ofSeconds(seconds);
        ItemStackIcon icon = ItemStackIcon.builder().itemName(pvpIconMaterial.toUpperCase(java.util.Locale.ROOT)).build();
        for (Player viewer : gameMap.getAlivePlayers()) {
            Optional<ApolloPlayer> ap = Apollo.getPlayerManager().getPlayer(viewer.getUniqueId());
            if (ap.isPresent()) {
                mod.displayCooldown(ap.get(), Cooldown.builder()
                        .name(pvpCooldownId)
                        .duration(dur)
                        .icon(icon)
                        .build());
            }
        }
    }

    public static void showPearlCooldown(Player player) {
        if (!isUsable() || !cooldownModuleEnabled || pearlCooldownSeconds < 1) {
            return;
        }
        CooldownModule mod = Apollo.getModuleManager().getModule(CooldownModule.class);
        if (mod == null || !Apollo.getModuleManager().isEnabled(CooldownModule.class)) {
            return;
        }
        Optional<ApolloPlayer> ap = Apollo.getPlayerManager().getPlayer(player.getUniqueId());
        if (ap.isPresent()) {
            mod.displayCooldown(ap.get(), Cooldown.builder()
                    .name(pearlCooldownId)
                    .duration(Duration.ofSeconds(pearlCooldownSeconds))
                    .icon(ItemStackIcon.builder().itemName("ENDER_PEARL").build())
                    .build());
        }
    }

    public static void clearPvpCooldown(Player player) {
        if (!isUsable() || !cooldownModuleEnabled) {
            return;
        }
        CooldownModule mod = Apollo.getModuleManager().getModule(CooldownModule.class);
        if (mod == null || !Apollo.getModuleManager().isEnabled(CooldownModule.class)) {
            return;
        }
        Optional<ApolloPlayer> ap = Apollo.getPlayerManager().getPlayer(player.getUniqueId());
        if (ap.isPresent()) {
            mod.removeCooldown(ap.get(), pvpCooldownId);
        }
    }

    public static void resetForPlayer(Player player) {
        if (!isUsable()) {
            return;
        }
        Optional<ApolloPlayer> ap = Apollo.getPlayerManager().getPlayer(player.getUniqueId());
        if (!ap.isPresent()) {
            return;
        }
        ApolloPlayer apolloPlayer = ap.get();
        try {
            TeamModule teamMod = Apollo.getModuleManager().getModule(TeamModule.class);
            if (teamMod != null && Apollo.getModuleManager().isEnabled(TeamModule.class)) {
                teamMod.resetTeamMembers(apolloPlayer);
            }
            CooldownModule cdMod = Apollo.getModuleManager().getModule(CooldownModule.class);
            if (cdMod != null && Apollo.getModuleManager().isEnabled(CooldownModule.class)) {
                cdMod.resetCooldowns(apolloPlayer);
            }
            NotificationModule nMod = Apollo.getModuleManager().getModule(NotificationModule.class);
            if (nMod != null && Apollo.getModuleManager().isEnabled(NotificationModule.class)) {
                nMod.resetNotifications(apolloPlayer);
            }
            WaypointModule wMod = Apollo.getModuleManager().getModule(WaypointModule.class);
            if (wMod != null && Apollo.getModuleManager().isEnabled(WaypointModule.class)) {
                wMod.resetWaypoints(apolloPlayer);
            }
            GlowModule gMod = Apollo.getModuleManager().getModule(GlowModule.class);
            if (gMod != null && Apollo.getModuleManager().isEnabled(GlowModule.class)) {
                gMod.resetGlow(apolloPlayer);
            }
            TitleModule tMod = Apollo.getModuleManager().getModule(TitleModule.class);
            if (tMod != null && Apollo.getModuleManager().isEnabled(TitleModule.class)) {
                tMod.resetTitles(apolloPlayer);
            }
            RichPresenceModule rpMod = Apollo.getModuleManager().getModule(RichPresenceModule.class);
            if (rpMod != null && Apollo.getModuleManager().isEnabled(RichPresenceModule.class)) {
                rpMod.resetServerRichPresence(apolloPlayer);
            }
        } catch (Throwable t) {
            MythicSkywars.get().getLogger().log(Level.FINE, "Lunar Apollo reset for player failed", t);
        }
    }

    public static void onMatchEnd(GameMap gameMap) {
        cancelTeamTask(gameMap.getName());
        if (!isUsable()) {
            return;
        }
        if (waypointEnabled && waypointResetOnEnd) {
            WaypointModule wMod = Apollo.getModuleManager().getModule(WaypointModule.class);
            if (wMod != null && Apollo.getModuleManager().isEnabled(WaypointModule.class)) {
                for (Player p : gameMap.getAllPlayers()) {
                    Optional<ApolloPlayer> ap = Apollo.getPlayerManager().getPlayer(p.getUniqueId());
                    if (ap.isPresent()) {
                        wMod.removeWaypoint(ap.get(), waypointName);
                    }
                }
            }
        }
        for (Player p : gameMap.getAllPlayers()) {
            resetForPlayer(p);
        }
    }

    private static void cancelTeamTask(String arenaId) {
        BukkitTask t = teamTasks.remove(arenaId);
        if (t != null) {
            t.cancel();
        }
    }

    /**
     * Lunar waypoint target: SkyWars spectator spawn (spec), else average of cage spawns, else world border center.
     */
    private static ApolloBlockLocation resolveWaypointBlock(GameMap gameMap) {
        if (gameMap.getCurrentWorld() == null) {
            return null;
        }
        String worldName = gameMap.getCurrentWorld().getName();
        String mode = waypointMode == null ? "auto" : waypointMode;

        if ("border".equals(mode)) {
            return blockFromWorldBorder(gameMap, worldName);
        }
        if ("centroid".equals(mode)) {
            ApolloBlockLocation c = blockFromSpawnCentroid(gameMap, worldName);
            return c != null ? c : blockFromWorldBorder(gameMap, worldName);
        }
        if ("spectator".equals(mode)) {
            ApolloBlockLocation s = blockFromSpectateSpawn(gameMap, worldName);
            return s != null ? s : blockFromSpawnCentroid(gameMap, worldName);
        }
        // auto
        ApolloBlockLocation s = blockFromSpectateSpawn(gameMap, worldName);
        if (s != null) {
            return s;
        }
        ApolloBlockLocation c = blockFromSpawnCentroid(gameMap, worldName);
        if (c != null) {
            return c;
        }
        return blockFromWorldBorder(gameMap, worldName);
    }

    private static ApolloBlockLocation blockFromSpectateSpawn(GameMap gameMap, String worldName) {
        CoordLoc spec = gameMap.getSpectateSpawn();
        if (spec == null) {
            return null;
        }
        return ApolloBlockLocation.builder()
                .world(worldName)
                .x(spec.getX())
                .y(spec.getY())
                .z(spec.getZ())
                .build();
    }

    private static ApolloBlockLocation blockFromSpawnCentroid(GameMap gameMap, String worldName) {
        long sx = 0, sy = 0, sz = 0, n = 0;
        for (java.util.List<CoordLoc> list : gameMap.getSpawnLocations().values()) {
            if (list == null) {
                continue;
            }
            for (CoordLoc c : list) {
                if (c == null) {
                    continue;
                }
                sx += c.getX();
                sy += c.getY();
                sz += c.getZ();
                n++;
            }
        }
        if (n == 0) {
            return null;
        }
        return ApolloBlockLocation.builder()
                .world(worldName)
                .x((int) (sx / n))
                .y((int) (sy / n))
                .z((int) (sz / n))
                .build();
    }

    private static ApolloBlockLocation blockFromWorldBorder(GameMap gameMap, String worldName) {
        org.bukkit.WorldBorder border = gameMap.getCurrentWorld().getWorldBorder();
        org.bukkit.Location c = border.getCenter();
        return ApolloBlockLocation.builder()
                .world(worldName)
                .x(c.getBlockX())
                .y(c.getBlockY())
                .z(c.getBlockZ())
                .build();
    }

    private static void showCenterWaypoint(GameMap gameMap) {
        if (!waypointEnabled || !isUsable()) {
            return;
        }
        WaypointModule wMod = Apollo.getModuleManager().getModule(WaypointModule.class);
        if (wMod == null || !Apollo.getModuleManager().isEnabled(WaypointModule.class)) {
            return;
        }
        if (gameMap.getCurrentWorld() == null) {
            return;
        }
        ApolloBlockLocation loc = resolveWaypointBlock(gameMap);
        if (loc == null) {
            return;
        }
        Waypoint wp = Waypoint.builder()
                .name(waypointName)
                .location(loc)
                .color(waypointColor)
                .preventRemoval(false)
                .hidden(false)
                .build();
        for (Player p : gameMap.getAlivePlayers()) {
            Optional<ApolloPlayer> ap = Apollo.getPlayerManager().getPlayer(p.getUniqueId());
            if (ap.isPresent()) {
                wMod.removeWaypoint(ap.get(), waypointName);
                wMod.displayWaypoint(ap.get(), wp);
            }
        }
    }

    private static void refreshTeams(GameMap gameMap) {
        TeamModule teamModule = Apollo.getModuleManager().getModule(TeamModule.class);
        if (teamModule == null || !Apollo.getModuleManager().isEnabled(TeamModule.class)) {
            return;
        }
        if (!teamModuleEnabled) {
            for (Player viewer : gameMap.getAlivePlayers()) {
                Optional<ApolloPlayer> viewerOpt = Apollo.getPlayerManager().getPlayer(viewer.getUniqueId());
                if (viewerOpt.isPresent()) {
                    teamModule.resetTeamMembers(viewerOpt.get());
                }
            }
            return;
        }
        if (gameMap.getTeamSize() <= 1) {
            for (Player viewer : gameMap.getAlivePlayers()) {
                Optional<ApolloPlayer> viewerOpt = Apollo.getPlayerManager().getPlayer(viewer.getUniqueId());
                if (viewerOpt.isPresent()) {
                    teamModule.resetTeamMembers(viewerOpt.get());
                }
            }
            return;
        }
        for (Player viewer : gameMap.getAlivePlayers()) {
            Optional<ApolloPlayer> viewerOpt = Apollo.getPlayerManager().getPlayer(viewer.getUniqueId());
            if (!viewerOpt.isPresent()) {
                continue;
            }
            TeamCard viewerTeam = gameMap.getTeamCard(viewer);
            if (viewerTeam == null) {
                teamModule.resetTeamMembers(viewerOpt.get());
                continue;
            }
            List<TeamMember> teammates = new ArrayList<TeamMember>();
            for (PlayerCard pc : viewerTeam.getPlayerCards()) {
                Player member = pc.getPlayer();
                if (member == null || member.equals(viewer)) {
                    continue;
                }
                if (!viewer.canSee(member)) {
                    continue;
                }
                if (!viewer.getWorld().equals(member.getWorld())) {
                    continue;
                }
                Color marker = colorFromTeamWool(viewerTeam.getByte());
                boolean close = isWithinHorizontalTrackingRange(viewer, member);
                if (close) {
                    teammates.add(TeamMember.builder()
                            .playerUuid(member.getUniqueId())
                            .markerColor(marker)
                            .build());
                } else {
                    org.bukkit.Location loc = member.getLocation();
                    teammates.add(TeamMember.builder()
                            .playerUuid(member.getUniqueId())
                            .markerColor(marker)
                            .location(ApolloLocation.builder()
                                    .world(loc.getWorld().getName())
                                    .x(loc.getX())
                                    .y(loc.getY())
                                    .z(loc.getZ())
                                    .build())
                            .build());
                }
            }
            teamModule.updateTeamMembers(viewerOpt.get(), teammates);
        }
    }

    private static void refreshGlow(GameMap gameMap) {
        if (!glowEnabled) {
            return;
        }
        GlowModule glow = Apollo.getModuleManager().getModule(GlowModule.class);
        if (glow == null || !Apollo.getModuleManager().isEnabled(GlowModule.class)) {
            return;
        }
        List<Player> alive = gameMap.getAlivePlayers();
        for (Player viewer : alive) {
            Optional<ApolloPlayer> apOpt = Apollo.getPlayerManager().getPlayer(viewer.getUniqueId());
            if (!apOpt.isPresent()) {
                continue;
            }
            ApolloPlayer ap = apOpt.get();
            glow.resetGlow(ap);
            if (gameMap.getTeamSize() > 1 && glowTeammates) {
                TeamCard tc = gameMap.getTeamCard(viewer);
                if (tc == null) {
                    continue;
                }
                Color col = colorFromTeamWool(tc.getByte());
                for (PlayerCard pc : tc.getPlayerCards()) {
                    Player mate = pc.getPlayer();
                    if (mate == null || mate.equals(viewer)) {
                        continue;
                    }
                    if (!viewer.getWorld().equals(mate.getWorld())) {
                        continue;
                    }
                    if (!viewer.canSee(mate) || !shouldApplyApolloGlow(viewer, mate)) {
                        continue;
                    }
                    glow.overrideGlow(ap, mate.getUniqueId(), col);
                }
            } else if (gameMap.getTeamSize() == 1 && glowEnemiesSolo) {
                for (Player other : alive) {
                    if (other.equals(viewer)) {
                        continue;
                    }
                    if (!viewer.getWorld().equals(other.getWorld())) {
                        continue;
                    }
                    if (!viewer.canSee(other) || !shouldApplyApolloGlow(viewer, other)) {
                        continue;
                    }
                    glow.overrideGlow(ap, other.getUniqueId(), new Color(0xE74848));
                }
            }
        }
    }

    private static void refreshRichPresence(GameMap gameMap) {
        if (!richPresenceEnabled) {
            return;
        }
        RichPresenceModule rp = Apollo.getModuleManager().getModule(RichPresenceModule.class);
        if (rp == null || !Apollo.getModuleManager().isEnabled(RichPresenceModule.class)) {
            return;
        }
        for (Player viewer : gameMap.getAlivePlayers()) {
            Optional<ApolloPlayer> apOpt = Apollo.getPlayerManager().getPlayer(viewer.getUniqueId());
            if (!apOpt.isPresent()) {
                continue;
            }
            TeamCard tc = gameMap.getTeamCard(viewer);
            int teamAlive = 0;
            int teamMax = gameMap.getTeamSize();
            if (tc != null) {
                teamAlive = tc.getPlayersSize();
            }
            String mapLabel = gameMap.getDisplayName();
            if (mapLabel == null || mapLabel.trim().isEmpty()) {
                mapLabel = gameMap.getName() != null ? gameMap.getName() : "SkyWars";
            }
            String subName = Bukkit.getServer().getName();
            if (subName == null || subName.trim().isEmpty()) {
                subName = "Server";
            }
            ServerRichPresence presence = ServerRichPresence.builder()
                    .gameName("SkyWars")
                    .gameVariantName("MythicSkywars")
                    .gameState("In match")
                    .playerState("Playing")
                    .mapName(mapLabel)
                    .subServerName(subName)
                    .teamCurrentSize(teamAlive)
                    .teamMaxSize(teamMax)
                    .build();
            rp.overrideServerRichPresence(apOpt.get(), presence);
        }
    }

    private static Notification buildNotification(String titleRaw, String descRaw, Duration display, GameMap map, Player player) {
        String mapName = map.getDisplayName() == null ? map.getName() : map.getDisplayName();
        String title = apolloNotificationTitlePlain(formatPlaceholders(titleRaw, mapName, player));
        String desc = apolloNotificationDescriptionPlain(formatPlaceholders(descRaw, mapName, player));
        return buildApolloNotification(title, desc, display, "GOLD", "GRAY", "textures/items/diamond_sword.png");
    }

    /**
     * Current Lunar clients deserialize notification title/description as Adventure JSON from
     * {@code titleComponent}/{@code descriptionComponent}. Deprecated {@code title}/{@code description}
     * strings can produce empty component payloads ("Don't know how to turn  into a Component").
     * <p>
     * Our shaded jar relocates {@code net.kyori}, so we build real Kyori instances via reflection using
     * {@link Apollo}'s classloader (same types Apollo-Bukkit was compiled against).
     */
    private static final Object apolloKyoriReflectionLock = new Object();
    private static volatile Method apolloKyoriTextMethod;

    static Notification buildApolloNotification(String titlePlain, String descPlain, Duration display,
            String titleNamedColor, String descNamedColor, String resourceLocation) {
        try {
            String t = titlePlain == null || titlePlain.trim().isEmpty() ? APOLLO_TEXT_FALLBACK : titlePlain;
            String d = descPlain == null || descPlain.trim().isEmpty() ? APOLLO_TEXT_FALLBACK : descPlain;
            Object titleC = apolloKyoriText(t, titleNamedColor);
            Object descC = apolloKyoriText(d, descNamedColor);
            ClassLoader cl = Apollo.class.getClassLoader();
            Class<?> compIface = Class.forName(apolloKyoriClassName("text.Component"), true, cl);
            Object builder = Notification.builder();
            builder.getClass().getMethod("titleComponent", compIface).invoke(builder, titleC);
            builder.getClass().getMethod("descriptionComponent", compIface).invoke(builder, descC);
            builder.getClass().getMethod("displayTime", Duration.class).invoke(builder, display);
            if (resourceLocation != null && !resourceLocation.isEmpty()) {
                builder.getClass().getMethod("resourceLocation", String.class).invoke(builder, resourceLocation);
            }
            return (Notification) builder.getClass().getMethod("build").invoke(builder);
        } catch (Throwable e) {
            MythicSkywars.get().getLogger().log(Level.WARNING, "Could not build Lunar Apollo notification (Kyori reflection)", e);
            return null;
        }
    }

    private static Object apolloKyoriText(String plain, String namedColorName) throws ReflectiveOperationException {
        ClassLoader cl = Apollo.class.getClassLoader();
        if (apolloKyoriTextMethod == null) {
            synchronized (apolloKyoriReflectionLock) {
                if (apolloKyoriTextMethod == null) {
                    Class<?> comp = Class.forName(apolloKyoriClassName("text.Component"), true, cl);
                    Class<?> textColor = Class.forName(apolloKyoriClassName("text.format.TextColor"), true, cl);
                    apolloKyoriTextMethod = comp.getMethod("text", String.class, textColor);
                }
            }
        }
        Class<?> named = Class.forName(apolloKyoriClassName("text.format.NamedTextColor"), true, cl);
        Object color = named.getField(namedColorName).get(null);
        return apolloKyoriTextMethod.invoke(null, plain, color);
    }

    /**
     * Keep class names runtime-built so maven-shade relocation does not rewrite Kyori names in string constants.
     */
    private static String apolloKyoriClassName(String suffix) {
        return new StringBuilder(40).append("net").append(".kyori.adventure.").append(suffix).toString();
    }

    /**
     * Strip legacy color codes for visible length / Apollo text; keep non-empty fallbacks.
     */
    private static final String APOLLO_TEXT_FALLBACK = "\u00A0";

    static String apolloNotificationTitlePlain(String formattedWithColorCodes) {
        if (formattedWithColorCodes == null) {
            return APOLLO_TEXT_FALLBACK;
        }
        String translated = ChatColor.translateAlternateColorCodes('&', formattedWithColorCodes);
        String plain = ChatColor.stripColor(translated).trim();
        if (plain.isEmpty()) {
            return APOLLO_TEXT_FALLBACK;
        }
        if (plain.length() > NOTIFICATION_TITLE_MAX) {
            plain = plain.substring(0, NOTIFICATION_TITLE_MAX);
        }
        return plain;
    }

    static String apolloNotificationDescriptionPlain(String formattedWithColorCodes) {
        if (formattedWithColorCodes == null) {
            return APOLLO_TEXT_FALLBACK;
        }
        String translated = ChatColor.translateAlternateColorCodes('&', formattedWithColorCodes);
        String plain = ChatColor.stripColor(translated);
        if (plain.trim().isEmpty()) {
            return APOLLO_TEXT_FALLBACK;
        }
        return plain;
    }

    private static String formatPlaceholders(String s, String mapName, Player player) {
        if (s == null) {
            return "";
        }
        return s.replace("{map}", mapName).replace("{player}", player != null ? player.getName() : "");
    }

    private static boolean isWithinHorizontalTrackingRange(Player viewer, Player member) {
        double dx = viewer.getLocation().getX() - member.getLocation().getX();
        double dz = viewer.getLocation().getZ() - member.getLocation().getZ();
        return (dx * dx + dz * dz) <= (TRACK_RANGE * TRACK_RANGE);
    }

    /**
     * Apollo glow is rendered client-side and can act like a wallhack if sent for every player.
     * Optional horizontal cap + {@link Player#hasLineOfSight(org.bukkit.entity.Entity)} keep it fair.
     */
    private static boolean shouldApplyApolloGlow(Player viewer, Player target) {
        if (glowMaxHorizontalBlocks > 0) {
            double max = (double) glowMaxHorizontalBlocks;
            double dx = viewer.getLocation().getX() - target.getLocation().getX();
            double dz = viewer.getLocation().getZ() - target.getLocation().getZ();
            if ((dx * dx + dz * dz) > (max * max)) {
                return false;
            }
        }
        if (glowRequireLineOfSight) {
            return viewer.hasLineOfSight(target);
        }
        return true;
    }

    static Color parseColor(String raw, Color fallback) {
        if (raw == null || !raw.startsWith("#") || raw.length() < 4) {
            return fallback;
        }
        try {
            return new Color(Integer.parseInt(raw.substring(1), 16));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static Color colorFromTeamWool(byte data) {
        switch (data) {
            case 0:
                return new Color(0xFFFFFF);
            case 1:
                return new Color(0xF9801D);
            case 2:
                return new Color(0xC74EBD);
            case 3:
                return new Color(0x3AB3DA);
            case 4:
                return new Color(0xFED83D);
            case 5:
                return new Color(0x80C71F);
            case 6:
                return new Color(0xF38BAA);
            case 7:
                return new Color(0x474F52);
            case 8:
                return new Color(0x9D9D97);
            case 9:
                return new Color(0x169C9C);
            case 10:
                return new Color(0x8932B8);
            case 11:
                return new Color(0x3C44AA);
            case 12:
                return new Color(0x835432);
            case 13:
                return new Color(0x5E7C16);
            case 14:
                return new Color(0xB02E26);
            case 15:
                return new Color(0x1D1D21);
            default:
                return Color.WHITE;
        }
    }
}
