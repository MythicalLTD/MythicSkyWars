package systems.mythical.mythicskywars.clients.feather;

import net.digitalingot.feather.serverapi.api.FeatherAPI;
import net.digitalingot.feather.serverapi.api.event.EventSubscription;
import net.digitalingot.feather.serverapi.api.event.player.PlayerHelloEvent;
import net.digitalingot.feather.serverapi.api.model.FeatherMod;
import net.digitalingot.feather.serverapi.api.model.Platform;
import net.digitalingot.feather.serverapi.api.meta.DiscordActivity;
import net.digitalingot.feather.serverapi.api.meta.MetaService;
import net.digitalingot.feather.serverapi.api.player.FeatherPlayer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.enums.MatchState;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.game.TeamCard;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Feather Server API integration.
 *
 * Uses direct Feather API classes (no reflection).
 */
public final class FeatherClientBridge {

    private static final String FEATHER_CONFIG = "clients/feather/config.yml";
    private static final String FEATHER_MODS_CONFIG = "clients/feather/mods.yml";
    private static final String REQUIRED_PLUGIN_NAME = "FeatherServerAPI";

    private static boolean yamlEnabled;
    private static boolean trackHelloPlayers;
    private static boolean debugLogHello;
    private static boolean verboseDebug;

    private static boolean discordEnabled;
    private static String discordImageUrl;
    private static String discordImageText;
    private static String discordLobbyState;
    private static String discordLobbyDetails;
    private static String discordInGameState;
    private static String discordInGameDetails;
    private static boolean discordIncludePartySize;
    private static boolean discordIncludeMatchStartTimestamp;
    private static boolean combatBypassMissPenalty;
    private static boolean combatPermissionBasedBypass;
    private static boolean modPolicyEnabled;
    private static boolean modResetBlockedBeforeApply;
    private static boolean modWhitelistEnabled;
    private static List<FeatherMod> modsBlock = new ArrayList<FeatherMod>();
    private static List<FeatherMod> modsUnblock = new ArrayList<FeatherMod>();
    private static List<FeatherMod> modsEnable = new ArrayList<FeatherMod>();
    private static List<FeatherMod> modsDisable = new ArrayList<FeatherMod>();
    private static Set<String> modWhitelistSlugs = new HashSet<String>();

    private static EventSubscription<PlayerHelloEvent> helloSubscription;
    private static final ConcurrentHashMap<UUID, Boolean> featherPlayers = new ConcurrentHashMap<UUID, Boolean>();
    private static final ConcurrentHashMap<String, Boolean> featherPlayersByName = new ConcurrentHashMap<String, Boolean>();
    private static final ConcurrentHashMap<String, Long> matchStartMillisByMap = new ConcurrentHashMap<String, Long>();
    private static final List<String> COMMON_FEATHER_MOD_SLUGS = Arrays.asList(
            "teamtracker", "animations", "armorBar", "armorStatus", "attackIndicator", "autohidehud",
            "autoperspective", "autoText", "backups", "blockIndicator", "blockOverlay", "bossBar",
            "brightness", "camera", "colorSaturation", "comboDisplay", "coordinates", "cps", "crosshair",
            "culllogs", "customadvancementsscreen", "customChat", "customf3", "customfog", "damageIndicator",
            "darkmode", "deathInfo", "direction", "discordRP", "dropprevention", "elytras", "fovChanger",
            "fps", "glint", "hearts", "hitbox", "hitindicator", "horses", "hypixel", "inventory", "itemCounter",
            "itemdespawn", "itemInfo1", "itemPhysic", "jumpreset", "keystrokes", "lightleveloverlay", "lootBeams",
            "mobOverlay", "motionBlur", "mousestrokes", "nametags", "nickHider", "packdisplay", "packOrganizer",
            "perspective", "ping", "playerModel", "playtime", "potionEffects", "reachDisplay", "reconnect",
            "saturation", "scoreboard", "screenshot", "searchkeybind", "serverAddress", "shulkertooltips",
            "snaplook", "soundfilters", "speedMeter", "stopwatch1", "subtitles", "systemresources", "tablist",
            "tiertagger", "time", "timeChanger", "titletweaker", "tnttimer", "toastcontrol", "toggleSprint",
            "tooltips", "totem", "tps", "uhcoverlay", "uiScaling", "viewModel", "voice", "waypoints",
            "weatherchanger", "zoom"
    );

    private FeatherClientBridge() {
    }

    public static void reload(MythicSkywars plugin) {
        shutdown();
        File file = new File(plugin.getDataFolder(), FEATHER_CONFIG);
        if (!file.exists()) {
            yamlEnabled = false;
            plugin.getLogger().warning("Feather bridge disabled: missing " + FEATHER_CONFIG);
            return;
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        yamlEnabled = cfg.getBoolean("enabled", false);
        if (!yamlEnabled) {
            plugin.getLogger().info("Feather bridge disabled in config (" + FEATHER_CONFIG + ").");
            return;
        }
        trackHelloPlayers = cfg.getBoolean("player-tracking.enabled", true);
        debugLogHello = cfg.getBoolean("player-tracking.log-hello-events", false);
        verboseDebug = debugLogHello;

        discordEnabled = cfg.getBoolean("discord-rich-presence.enabled", true);
        discordImageUrl = cfg.getString("discord-rich-presence.image-url", "");
        discordImageText = cfg.getString("discord-rich-presence.image-text", "MythicSkywars");
        discordLobbyState = cfg.getString("discord-rich-presence.lobby.state", "In Lobby");
        discordLobbyDetails = cfg.getString("discord-rich-presence.lobby.details", "Waiting for a match");
        discordInGameState = cfg.getString("discord-rich-presence.in-game.state", "Playing {map}");
        discordInGameDetails = cfg.getString("discord-rich-presence.in-game.details", "{alive}/{max_players} players alive");
        discordIncludePartySize = cfg.getBoolean("discord-rich-presence.in-game.include-party-size", true);
        discordIncludeMatchStartTimestamp = cfg.getBoolean("discord-rich-presence.in-game.include-match-start-timestamp", true);
        combatBypassMissPenalty = cfg.getBoolean("combat.bypass-miss-penalty", false);
        combatPermissionBasedBypass = cfg.getBoolean("combat.permission-based-bypass", false);
        loadModsPolicy(plugin);

        if (!isUsable()) {
            plugin.getLogger().warning("Feather bridge inactive: plugin '" + REQUIRED_PLUGIN_NAME + "' not enabled.");
            return;
        }
        plugin.getLogger().info("Feather bridge active: " + getStatusSummary());
        if (trackHelloPlayers) {
            registerHelloSubscription(plugin);
        }
        for (FeatherPlayer fp : FeatherAPI.getPlayerService().getPlayers()) {
            if (fp != null) {
                featherPlayers.put(fp.getUniqueId(), Boolean.TRUE);
                applyModPolicy(fp);
                Player bukkitPlayer = resolveBukkitPlayer(fp);
                if (bukkitPlayer != null) {
                    applyCombatPolicy(bukkitPlayer, fp);
                }
            }
        }
        syncTrackedPlayers(verboseDebug);
    }

    public static void shutdown() {
        matchStartMillisByMap.clear();
        featherPlayers.clear();
        featherPlayersByName.clear();
        if (helloSubscription != null) {
            helloSubscription.unsubscribe();
            helloSubscription = null;
        }
    }

    public static boolean isUsable() {
        if (!yamlEnabled) {
            return false;
        }
        return Bukkit.getPluginManager().isPluginEnabled(REQUIRED_PLUGIN_NAME);
    }

    public static String getStatusSummary() {
        syncTrackedPlayers(false);
        int apiPlayers = 0;
        if (isUsable()) {
            try {
                apiPlayers = FeatherAPI.getPlayerService().getPlayers().size();
            } catch (Throwable ignored) {
            }
        }
        return "enabled=" + yamlEnabled
                + ", plugin=" + Bukkit.getPluginManager().isPluginEnabled(REQUIRED_PLUGIN_NAME)
                + ", apiPlayers=" + apiPlayers
                + ", trackedPlayers=" + featherPlayers.size()
                + ", trackedByName=" + featherPlayersByName.size()
                + ", helloSubscribed=" + (helloSubscription != null);
    }

    public static String getPolicySummary() {
        return "modPolicyEnabled=" + modPolicyEnabled
                + ", whitelistMode=" + modWhitelistEnabled
                + ", resetBlockedBeforeApply=" + modResetBlockedBeforeApply
                + ", block=" + modsBlock.size()
                + ", unblock=" + modsUnblock.size()
                + ", enable=" + modsEnable.size()
                + ", disable=" + modsDisable.size();
    }

    public static boolean isFeatherPlayer(Player player) {
        syncTrackedPlayers(false);
        return player != null && featherPlayers.containsKey(player.getUniqueId());
    }

    public static String debugPlayerState(Player player) {
        if (player == null) {
            return "player=null";
        }
        FeatherPlayer fp = null;
        boolean nameExistsInApi = false;
        try {
            fp = resolveFeatherPlayer(player);
            if (isUsable()) {
                for (FeatherPlayer candidate : FeatherAPI.getPlayerService().getPlayers()) {
                    if (candidate != null && player.getName().equalsIgnoreCase(candidate.getName())) {
                        nameExistsInApi = true;
                        break;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return "name=" + player.getName()
                + ", online=" + player.isOnline()
                + ", tracked=" + featherPlayers.containsKey(player.getUniqueId())
                + ", trackedByName=" + featherPlayersByName.containsKey(normalizeName(player.getName()))
                + ", nameInApiList=" + nameExistsInApi
                + ", apiDetected=" + (fp != null);
    }

    public static void onQuit(Player player) {
        if (player == null) {
            return;
        }
        featherPlayers.remove(player.getUniqueId());
        featherPlayersByName.remove(normalizeName(player.getName()));
        if (isUsable() && discordEnabled) {
            FeatherPlayer fp = resolveFeatherPlayer(player);
            if (fp != null) {
                FeatherAPI.getMetaService().clearDiscordActivity(fp);
            }
        }
    }

    public static void onJoin(Player player) {
        if (player == null || !isUsable()) {
            return;
        }
        // Feather hello may arrive shortly after Bukkit join; resync after a short delay.
        Bukkit.getScheduler().runTaskLater(MythicSkywars.get(), () -> syncTrackedPlayers(verboseDebug), 40L);
    }

    public static void onMatchPlaying(GameMap gameMap) {
        if (!isUsable() || !discordEnabled || gameMap == null) {
            return;
        }
        matchStartMillisByMap.put(gameMap.getName(), System.currentTimeMillis());
        for (Player alive : gameMap.getAlivePlayers()) {
            updateInGameActivity(alive, gameMap);
        }
    }

    public static void onMatchEnd(GameMap gameMap) {
        if (gameMap != null) {
            matchStartMillisByMap.remove(gameMap.getName());
        }
        if (!isUsable() || !discordEnabled || gameMap == null) {
            return;
        }
        for (Player alive : gameMap.getAlivePlayers()) {
            updateLobbyActivity(alive);
        }
        for (UUID spectatorId : gameMap.getSpectators()) {
            Player spectator = Bukkit.getPlayer(spectatorId);
            if (spectator != null) {
                updateLobbyActivity(spectator);
            }
        }
    }

    public static void updateLobbyActivity(Player player) {
        if (!isUsable() || !discordEnabled || player == null) {
            return;
        }
        FeatherPlayer fp = resolveFeatherPlayer(player);
        if (fp == null) {
            return;
        }
        featherPlayers.put(player.getUniqueId(), Boolean.TRUE);
        try {
            DiscordActivity.Builder builder = DiscordActivity.builder()
                    .withState(discordLobbyState)
                    .withDetails(discordLobbyDetails);
            if (discordImageUrl != null && !discordImageUrl.trim().isEmpty()) {
                builder.withImage(discordImageUrl);
            }
            if (discordImageText != null && !discordImageText.trim().isEmpty()) {
                builder.withImageText(discordImageText);
            }
            FeatherAPI.getMetaService().updateDiscordActivity(fp, builder.build());
        } catch (Throwable t) {
            MythicSkywars.get().getLogger().log(Level.FINE, "Failed to update Feather lobby Discord activity", t);
        }
    }

    public static void updateInGameActivity(Player player, GameMap gameMap) {
        if (!isUsable() || !discordEnabled || player == null || gameMap == null) {
            return;
        }
        FeatherPlayer fp = resolveFeatherPlayer(player);
        if (fp == null) {
            return;
        }
        featherPlayers.put(player.getUniqueId(), Boolean.TRUE);
        try {
            DiscordActivity.Builder builder = DiscordActivity.builder()
                    .withState(formatDiscord(discordInGameState, player, gameMap))
                    .withDetails(formatDiscord(discordInGameDetails, player, gameMap));
            if (discordImageUrl != null && !discordImageUrl.trim().isEmpty()) {
                builder.withImage(discordImageUrl);
            }
            if (discordImageText != null && !discordImageText.trim().isEmpty()) {
                builder.withImageText(discordImageText);
            }
            if (discordIncludePartySize) {
                builder.withPartySize(gameMap.getAlivePlayers().size(), Math.max(1, gameMap.getMaxPlayers()));
            }
            if (discordIncludeMatchStartTimestamp) {
                Long started = matchStartMillisByMap.get(gameMap.getName());
                if (started != null) {
                    builder.withStartTimestamp(started);
                }
            }
            MetaService meta = FeatherAPI.getMetaService();
            meta.updateDiscordActivity(fp, builder.build());
        } catch (Throwable t) {
            MythicSkywars.get().getLogger().log(Level.FINE, "Failed to update Feather in-game Discord activity", t);
        }
    }

    private static void registerHelloSubscription(MythicSkywars plugin) {
        helloSubscription = FeatherAPI.getEventService().subscribe(PlayerHelloEvent.class, event -> {
            FeatherPlayer featherPlayer = event.getPlayer();
            if (featherPlayer == null) {
                return;
            }
            featherPlayers.put(featherPlayer.getUniqueId(), Boolean.TRUE);
            featherPlayersByName.put(normalizeName(featherPlayer.getName()), Boolean.TRUE);
            applyModPolicy(featherPlayer);
            Player bukkitPlayer = resolveBukkitPlayer(featherPlayer);
            if (bukkitPlayer != null) {
                applyCombatPolicy(bukkitPlayer, featherPlayer);
            }
            if (debugLogHello) {
                Platform platform = event.getPlatform();
                int modCount = event.getFeatherMods() == null ? 0 : event.getFeatherMods().size();
                plugin.getLogger().info("Feather hello: " + featherPlayer.getName()
                        + " (" + platform + ", mods=" + modCount + ")");
            }
            if (verboseDebug && !debugLogHello) {
                plugin.getLogger().info("Feather hello: " + featherPlayer.getName());
            }
            if (bukkitPlayer == null || !discordEnabled) {
                return;
            }
            GameMap playerMap = MythicSkywars.get().getMatchManager().getPlayerMapSafe(bukkitPlayer);
            if (playerMap != null && playerMap.getMatchState() == MatchState.PLAYING) {
                updateInGameActivity(bukkitPlayer, playerMap);
            } else {
                updateLobbyActivity(bukkitPlayer);
            }
        });
        plugin.getLogger().info("Feather client bridge enabled (clients/feather/config.yml), helloSubscribed=" + (helloSubscription != null));
    }

    private static void applyCombatPolicy(Player bukkitPlayer, FeatherPlayer featherPlayer) {
        if (bukkitPlayer == null || featherPlayer == null) {
            return;
        }
        boolean bypass;
        if (combatPermissionBasedBypass) {
            bypass = bukkitPlayer.hasPermission("sw.feather.combat.bypassmisspenalty");
        } else {
            bypass = combatBypassMissPenalty;
        }
        featherPlayer.bypassMissPenalty(bypass);
    }

    private static void loadModsPolicy(MythicSkywars plugin) {
        modPolicyEnabled = false;
        modResetBlockedBeforeApply = true;
        modWhitelistEnabled = false;
        modWhitelistSlugs = new HashSet<String>();
        modsBlock = new ArrayList<FeatherMod>();
        modsUnblock = new ArrayList<FeatherMod>();
        modsEnable = new ArrayList<FeatherMod>();
        modsDisable = new ArrayList<FeatherMod>();

        File modsFile = new File(plugin.getDataFolder(), FEATHER_MODS_CONFIG);
        if (!modsFile.exists()) {
            return;
        }
        FileConfiguration modsCfg = YamlConfiguration.loadConfiguration(modsFile);
        modPolicyEnabled = modsCfg.getBoolean("enabled", false);
        modResetBlockedBeforeApply = modsCfg.getBoolean("reset-blocked-mods-before-apply", true);
        modWhitelistEnabled = modsCfg.getBoolean("whitelist-mode.enabled", false);
        modWhitelistSlugs = parseModSlugSet(modsCfg.getStringList("whitelist-mode.allowed"));
        modsBlock = parseMods(computeEffectiveBlockList(modsCfg.getStringList("policy.block")));
        modsUnblock = parseMods(modsCfg.getStringList("policy.unblock"));
        modsEnable = parseMods(modsCfg.getStringList("policy.enable"));
        modsDisable = parseMods(modsCfg.getStringList("policy.disable"));
    }

    private static List<String> computeEffectiveBlockList(List<String> explicitBlockList) {
        if (!modWhitelistEnabled) {
            return explicitBlockList;
        }
        Set<String> blocked = new HashSet<String>();
        for (String slug : COMMON_FEATHER_MOD_SLUGS) {
            if (!modWhitelistSlugs.contains(slug)) {
                blocked.add(slug);
            }
        }
        if (explicitBlockList != null) {
            for (String slug : explicitBlockList) {
                if (slug == null) {
                    continue;
                }
                String clean = slug.trim();
                if (!clean.isEmpty() && !clean.startsWith("#")) {
                    blocked.add(clean);
                }
            }
        }
        return new ArrayList<String>(blocked);
    }

    private static List<FeatherMod> parseMods(List<String> slugs) {
        List<FeatherMod> out = new ArrayList<FeatherMod>();
        if (slugs == null) {
            return out;
        }
        for (String slug : slugs) {
            if (slug == null) {
                continue;
            }
            String clean = slug.trim();
            if (clean.isEmpty() || clean.startsWith("#")) {
                continue;
            }
            out.add(new FeatherMod(clean));
        }
        return out;
    }

    private static Set<String> parseModSlugSet(List<String> slugs) {
        Set<String> out = new HashSet<String>();
        if (slugs == null) {
            return out;
        }
        for (String slug : slugs) {
            if (slug == null) {
                continue;
            }
            String clean = slug.trim();
            if (!clean.isEmpty() && !clean.startsWith("#")) {
                out.add(clean);
            }
        }
        return out;
    }

    private static void applyModPolicy(FeatherPlayer player) {
        if (player == null || !modPolicyEnabled) {
            return;
        }
        if (modResetBlockedBeforeApply) {
            player.getBlockedMods().thenAccept(currentlyBlocked -> {
                if (currentlyBlocked != null && !currentlyBlocked.isEmpty()) {
                    player.unblockMods(currentlyBlocked);
                }
                applyModPolicyLists(player);
            });
            return;
        }
        applyModPolicyLists(player);
    }

    private static void applyModPolicyLists(FeatherPlayer player) {
        try {
            if (!modsUnblock.isEmpty()) {
                player.unblockMods(modsUnblock);
            }
            if (!modsBlock.isEmpty()) {
                player.blockMods(modsBlock);
            }
            if (!modsDisable.isEmpty()) {
                player.disableMods(modsDisable);
            }
            if (!modsEnable.isEmpty()) {
                player.enableMods(modsEnable);
            }
        } catch (Throwable t) {
            MythicSkywars.get().getLogger().log(Level.FINE, "Failed to apply Feather mod policy", t);
        }
    }

    public static String syncTrackedPlayers(boolean logDetails) {
        if (!isUsable()) {
            return "usable=false";
        }
        int online = 0;
        int detected = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p == null) {
                continue;
            }
            online++;
            FeatherPlayer fp = resolveFeatherPlayer(p);
            if (fp != null) {
                featherPlayers.put(p.getUniqueId(), Boolean.TRUE);
                featherPlayersByName.put(normalizeName(p.getName()), Boolean.TRUE);
                detected++;
            } else if (logDetails) {
                MythicSkywars.get().getLogger().info("[Feather debug] API did not detect online player " + p.getName());
            }
        }
        String summary = "online=" + online + ", detected=" + detected + ", tracked=" + featherPlayers.size();
        if (logDetails) {
            MythicSkywars.get().getLogger().info("[Feather debug] syncTrackedPlayers => " + summary);
        }
        return summary;
    }

    private static FeatherPlayer resolveFeatherPlayer(Player bukkitPlayer) {
        if (bukkitPlayer == null || !isUsable()) {
            return null;
        }
        FeatherPlayer byUuid = FeatherAPI.getPlayerService().getPlayer(bukkitPlayer.getUniqueId());
        if (byUuid != null) {
            return byUuid;
        }
        String name = bukkitPlayer.getName();
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        for (FeatherPlayer fp : FeatherAPI.getPlayerService().getPlayers()) {
            if (fp != null && name.equalsIgnoreCase(fp.getName())) {
                if (verboseDebug) {
                    MythicSkywars.get().getLogger().info("[Feather debug] Resolved by name fallback: "
                            + name + " (Bukkit UUID=" + bukkitPlayer.getUniqueId()
                            + ", Feather UUID=" + fp.getUniqueId() + ")");
                }
                return fp;
            }
        }
        return null;
    }

    private static Player resolveBukkitPlayer(FeatherPlayer featherPlayer) {
        if (featherPlayer == null) {
            return null;
        }
        Player byUuid = Bukkit.getPlayer(featherPlayer.getUniqueId());
        if (byUuid != null) {
            return byUuid;
        }
        String name = featherPlayer.getName();
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        return Bukkit.getPlayerExact(name);
    }

    private static String normalizeName(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }

    private static String formatDiscord(String template, Player player, GameMap gameMap) {
        String mapName = gameMap.getDisplayName();
        if (mapName == null || mapName.trim().isEmpty()) {
            mapName = gameMap.getName();
        }
        String out = template == null ? "" : template;
        out = out.replace("{map}", mapName == null ? "SkyWars" : mapName);
        out = out.replace("{player}", player.getName());
        out = out.replace("{alive}", String.valueOf(gameMap.getAlivePlayers().size()));
        out = out.replace("{max_players}", String.valueOf(gameMap.getMaxPlayers()));
        out = out.replace("{team_size}", String.valueOf(gameMap.getTeamSize()));
        TeamCard tc = gameMap.getTeamCard(player);
        out = out.replace("{team_alive}", String.valueOf(tc != null ? tc.getPlayersSize() : 0));
        return out;
    }
}