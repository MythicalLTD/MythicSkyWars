package systems.mythical.mythicskywars.clients.labymod;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.enums.MatchState;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.game.TeamCard;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * LabyMod 4 Server API integration.
 * <p>
 * Requires the LabyMod Server API plugin (server-bukkit jar) in the plugins folder.
 * Download: <a href="https://github.com/LabyMod/labymod4-server-api/releases">GitHub Releases</a>
 * <p>
 * If the plugin is not installed, all methods gracefully no-op.
 * <p>
 * Settings: {@code plugins/MythicSkywars/clients/labymod/config.yml}
 */
public final class LabyModBridge {

    private static final String LABYMOD_CONFIG = "clients/labymod/config.yml";
    private static final String REQUIRED_PLUGIN_NAME = "LabyModServerAPI";

    private static boolean labyModClassesAvailable;
    private static boolean yamlEnabled;

    // Discord RPC
    private static boolean discordRpcEnabled;
    private static String discordLobbyGameMode;
    private static String discordInGameGameMode;
    private static boolean discordIncludeStartTime;

    // Economy display
    private static boolean economyEnabled;
    private static String economyKey;
    private static String economyIconUrl;

    // Permissions (feature toggles)
    private static boolean permissionsEnabled;
    private static List<String> permissionsDeny = new ArrayList<String>();
    private static List<String> permissionsAllow = new ArrayList<String>();

    // Addon disable (blacklist)
    private static boolean addonDisableEnabled;
    private static List<String> addonsToDisable = new ArrayList<String>();

    // Subtitles
    private static boolean subtitlesEnabled;
    private static String subtitleInGame;

    // Playing Game Mode (shows to LabyMod friends what you're playing)
    private static boolean gameModeEnabled;
    private static String gameModeLobby;
    private static String gameModeInGame;

    // Tab List Banner
    private static boolean tabBannerEnabled;
    private static String tabBannerUrl;

    // Interaction Menu (right-click player menu entries)
    private static boolean interactionMenuEnabled;
    private static List<String[]> interactionMenuEntries = new ArrayList<String[]>();

    // Addon Recommendations (suggest addons on join)
    private static boolean addonRecommendEnabled;
    private static List<String> recommendedAddons = new ArrayList<String>();
    private static List<String> requiredAddons = new ArrayList<String>();

    // Markers (team ping forwarding)
    private static boolean markersEnabled;

    // Server Switch Prompt (BungeeCord: suggest switching after match)
    private static boolean serverSwitchEnabled;
    private static String serverSwitchAddress;
    private static String serverSwitchTitle;

    // Input Prompt (custom text input from player, e.g. report reason)
    private static boolean inputPromptEnabled;

    // Tab List Flags (country flags next to player names)
    private static boolean tabFlagsEnabled;

    // Match tick (glow-like refresh)
    private static boolean matchTickEnabled;
    private static long matchTickInterval = 5L;

    private static final ConcurrentHashMap<String, Long> matchStartMillisByMap = new ConcurrentHashMap<String, Long>();
    private static final Map<String, BukkitTask> matchTasks = new ConcurrentHashMap<String, BukkitTask>();

    static {
        try {
            Class.forName("net.labymod.serverapi.server.bukkit.LabyModProtocolService");
            labyModClassesAvailable = true;
        } catch (ClassNotFoundException e) {
            labyModClassesAvailable = false;
        }
    }

    private LabyModBridge() {
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    public static void reload(MythicSkywars plugin) {
        shutdown();
        File file = new File(plugin.getDataFolder(), LABYMOD_CONFIG);
        if (!file.exists()) {
            yamlEnabled = false;
            plugin.getLogger().warning("LabyMod bridge disabled: missing " + LABYMOD_CONFIG);
            return;
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        yamlEnabled = cfg.getBoolean("enabled", true);
        if (!yamlEnabled) {
            plugin.getLogger().info("LabyMod bridge disabled in config (" + LABYMOD_CONFIG + ").");
            return;
        }

        // Discord RPC
        discordRpcEnabled = cfg.getBoolean("modules.discord-rpc.enabled", true);
        discordLobbyGameMode = cfg.getString("modules.discord-rpc.lobby.game-mode", "In Lobby");
        discordInGameGameMode = cfg.getString("modules.discord-rpc.in-game.game-mode", "Playing {map}");
        discordIncludeStartTime = cfg.getBoolean("modules.discord-rpc.in-game.include-start-time", true);

        // Economy display
        economyEnabled = cfg.getBoolean("modules.economy-display.enabled", false);
        economyKey = cfg.getString("modules.economy-display.key", "cash");
        economyIconUrl = cfg.getString("modules.economy-display.icon-url", "");

        // Permissions
        permissionsEnabled = cfg.getBoolean("modules.permissions.enabled", false);
        permissionsDeny = cfg.getStringList("modules.permissions.deny");
        permissionsAllow = cfg.getStringList("modules.permissions.allow");

        // Addon disable (blacklist)
        addonDisableEnabled = cfg.getBoolean("modules.addon-blacklist.enabled", false);
        addonsToDisable = cfg.getStringList("modules.addon-blacklist.disabled-addons");

        // Subtitles
        subtitlesEnabled = cfg.getBoolean("modules.subtitles.enabled", false);
        subtitleInGame = cfg.getString("modules.subtitles.in-game", "");

        // Playing Game Mode
        gameModeEnabled = cfg.getBoolean("modules.playing-game-mode.enabled", true);
        gameModeLobby = cfg.getString("modules.playing-game-mode.lobby", "SkyWars Lobby");
        gameModeInGame = cfg.getString("modules.playing-game-mode.in-game", "SkyWars - {map}");

        // Tab List Banner
        tabBannerEnabled = cfg.getBoolean("modules.tab-banner.enabled", false);
        tabBannerUrl = cfg.getString("modules.tab-banner.url", "");

        // Interaction Menu
        interactionMenuEnabled = cfg.getBoolean("modules.interaction-menu.enabled", false);
        interactionMenuEntries = new ArrayList<String[]>();
        List<Map<?, ?>> rawEntries = cfg.getMapList("modules.interaction-menu.entries");
        if (rawEntries != null) {
            for (Map<?, ?> entry : rawEntries) {
                String label = entry.get("label") != null ? entry.get("label").toString() : null;
                String action = entry.get("action") != null ? entry.get("action").toString() : "RUN_COMMAND";
                String value = entry.get("value") != null ? entry.get("value").toString() : "";
                if (label != null && !label.trim().isEmpty()) {
                    interactionMenuEntries.add(new String[]{label.trim(), action.trim().toUpperCase(), value});
                }
            }
        }

        // Addon Recommendations
        addonRecommendEnabled = cfg.getBoolean("modules.addon-recommendations.enabled", false);
        recommendedAddons = cfg.getStringList("modules.addon-recommendations.recommended");
        requiredAddons = cfg.getStringList("modules.addon-recommendations.required");

        // Markers
        markersEnabled = cfg.getBoolean("modules.markers.enabled", true);

        // Server Switch Prompt
        serverSwitchEnabled = cfg.getBoolean("modules.server-switch.enabled", false);
        serverSwitchAddress = cfg.getString("modules.server-switch.address", "");
        serverSwitchTitle = cfg.getString("modules.server-switch.title", "Play again?");

        // Input Prompt
        inputPromptEnabled = cfg.getBoolean("modules.input-prompt.enabled", false);

        // Tab List Flags
        tabFlagsEnabled = cfg.getBoolean("modules.tab-flags.enabled", false);

        // Match tick
        matchTickEnabled = cfg.getBoolean("modules.match-tick.enabled", true);
        matchTickInterval = Math.max(1L, cfg.getLong("modules.match-tick.interval-ticks", 5L));

        if (!isUsable()) {
            plugin.getLogger().warning("LabyMod bridge inactive: plugin '" + REQUIRED_PLUGIN_NAME + "' not enabled or classes unavailable.");
            return;
        }
        plugin.getLogger().info("LabyMod bridge active: " + getStatusSummary());
    }

    public static void shutdown() {
        matchStartMillisByMap.clear();
        cancelAllMatchTasks();
    }

    public static boolean isUsable() {
        return yamlEnabled && labyModClassesAvailable && isLabyModPluginPresent();
    }

    public static String getStatusSummary() {
        return "enabled=" + yamlEnabled
                + ", classes=" + labyModClassesAvailable
                + ", plugin=" + isLabyModPluginPresent()
                + ", discordRpc=" + discordRpcEnabled
                + ", gameMode=" + gameModeEnabled
                + ", economy=" + economyEnabled
                + ", tabBanner=" + tabBannerEnabled
                + ", tabFlags=" + tabFlagsEnabled
                + ", interactionMenu=" + interactionMenuEnabled
                + ", markers=" + markersEnabled
                + ", permissions=" + permissionsEnabled
                + ", addonBlacklist=" + addonDisableEnabled
                + ", addonRecommend=" + addonRecommendEnabled
                + ", serverSwitch=" + serverSwitchEnabled
                + ", inputPrompt=" + inputPromptEnabled
                + ", subtitles=" + subtitlesEnabled;
    }

    private static boolean isLabyModPluginPresent() {
        return Bukkit.getPluginManager().isPluginEnabled(REQUIRED_PLUGIN_NAME);
    }

    // =========================================================================
    // Player Join / Quit — apply permissions & addon blacklist on join
    // =========================================================================

    public static void onJoin(Player player) {
        if (!isUsable() || player == null) {
            return;
        }
        // Delay slightly so LabyMod handshake completes
        Bukkit.getScheduler().runTaskLater(MythicSkywars.get(), () -> {
            applyPermissions(player);
            applyAddonBlacklist(player);
            sendAddonRecommendations(player);
            sendInteractionMenu(player);
            sendTabBanner(player);
            enableMarkerForwarding(player);
            sendPlayingGameMode(player, null);
            updateLobbyRpc(player);
        }, 20L);
    }

    public static void onQuit(Player player) {
        // Nothing to clean up — LabyMod handles disconnect
    }

    // =========================================================================
    // Match lifecycle
    // =========================================================================

    public static void onMatchPlaying(GameMap gameMap) {
        if (!isUsable() || gameMap == null) {
            return;
        }
        matchStartMillisByMap.put(gameMap.getName(), System.currentTimeMillis());

        // Update Discord RPC for all alive players
        if (discordRpcEnabled) {
            for (Player alive : gameMap.getAlivePlayers()) {
                updateInGameRpc(alive, gameMap);
            }
        }

        // Update Playing Game Mode (visible to LabyMod friends)
        if (gameModeEnabled) {
            for (Player alive : gameMap.getAlivePlayers()) {
                sendPlayingGameMode(alive, gameMap);
            }
        }

        // Update subtitles
        if (subtitlesEnabled && subtitleInGame != null && !subtitleInGame.isEmpty()) {
            for (Player alive : gameMap.getAlivePlayers()) {
                updateSubtitle(alive, gameMap);
            }
        }

        // Start match tick task for periodic updates
        if (matchTickEnabled) {
            startMatchTask(gameMap);
        }
    }

    public static void onMatchEnd(GameMap gameMap) {
        if (gameMap != null) {
            matchStartMillisByMap.remove(gameMap.getName());
            cancelMatchTask(gameMap.getName());
        }
        if (!isUsable() || gameMap == null) {
            return;
        }

        // Reset Discord RPC to lobby
        if (discordRpcEnabled) {
            for (Player alive : gameMap.getAlivePlayers()) {
                updateLobbyRpc(alive);
            }
            for (UUID spectatorId : gameMap.getSpectators()) {
                Player spectator = Bukkit.getPlayer(spectatorId);
                if (spectator != null) {
                    updateLobbyRpc(spectator);
                }
            }
        }

        // Reset Playing Game Mode to lobby
        if (gameModeEnabled) {
            for (Player p : gameMap.getAllPlayers()) {
                sendPlayingGameMode(p, null);
            }
        }

        // Reset subtitles
        if (subtitlesEnabled) {
            for (Player p : gameMap.getAllPlayers()) {
                resetSubtitle(p);
            }
        }
    }

    // =========================================================================
    // Discord Rich Presence
    // =========================================================================

    public static void updateLobbyRpc(Player player) {
        if (!isUsable() || !discordRpcEnabled || player == null) {
            return;
        }
        try {
            doSendDiscordRpc(player, discordLobbyGameMode, false, null);
        } catch (Throwable t) {
            MythicSkywars.get().getLogger().log(Level.FINE, "Failed to update LabyMod lobby Discord RPC", t);
        }
    }

    public static void updateInGameRpc(Player player, GameMap gameMap) {
        if (!isUsable() || !discordRpcEnabled || player == null || gameMap == null) {
            return;
        }
        try {
            String gameMode = formatTemplate(discordInGameGameMode, player, gameMap);
            Long started = discordIncludeStartTime ? matchStartMillisByMap.get(gameMap.getName()) : null;
            doSendDiscordRpc(player, gameMode, true, started);
        } catch (Throwable t) {
            MythicSkywars.get().getLogger().log(Level.FINE, "Failed to update LabyMod in-game Discord RPC", t);
        }
    }

    public static void resetDiscordRpc(Player player) {
        if (!isUsable() || !discordRpcEnabled || player == null) {
            return;
        }
        try {
            doResetDiscordRpc(player);
        } catch (Throwable t) {
            MythicSkywars.get().getLogger().log(Level.FINE, "Failed to reset LabyMod Discord RPC", t);
        }
    }

    // =========================================================================
    // Economy Display
    // =========================================================================

    /**
     * Update the economy display for a LabyMod player.
     * Call this when the player's balance changes or on join.
     */
    public static void updateEconomyDisplay(Player player, double balance) {
        if (!isUsable() || !economyEnabled || player == null) {
            return;
        }
        try {
            doUpdateEconomy(player, balance);
        } catch (Throwable t) {
            MythicSkywars.get().getLogger().log(Level.FINE, "Failed to update LabyMod economy display", t);
        }
    }

    // =========================================================================
    // Permissions (feature toggles — deny/allow LabyMod features)
    // =========================================================================

    public static void applyPermissions(Player player) {
        if (!isUsable() || !permissionsEnabled || player == null) {
            return;
        }
        if (permissionsDeny.isEmpty() && permissionsAllow.isEmpty()) {
            return;
        }
        try {
            doApplyPermissions(player);
        } catch (Throwable t) {
            MythicSkywars.get().getLogger().log(Level.FINE, "Failed to apply LabyMod permissions", t);
        }
    }

    // =========================================================================
    // Addon Blacklist (disable addons on the client)
    // =========================================================================

    public static void applyAddonBlacklist(Player player) {
        if (!isUsable() || !addonDisableEnabled || player == null) {
            return;
        }
        if (addonsToDisable.isEmpty()) {
            return;
        }
        try {
            doDisableAddons(player);
        } catch (Throwable t) {
            MythicSkywars.get().getLogger().log(Level.FINE, "Failed to disable LabyMod addons", t);
        }
    }

    public static void revertAddonBlacklist(Player player) {
        if (!isUsable() || !addonDisableEnabled || player == null) {
            return;
        }
        try {
            doRevertDisabledAddons(player);
        } catch (Throwable t) {
            MythicSkywars.get().getLogger().log(Level.FINE, "Failed to revert LabyMod addon blacklist", t);
        }
    }

    // =========================================================================
    // Subtitles (text below player nametag)
    // =========================================================================

    public static void updateSubtitle(Player player, GameMap gameMap) {
        if (!isUsable() || !subtitlesEnabled || player == null) {
            return;
        }
        if (subtitleInGame == null || subtitleInGame.isEmpty()) {
            return;
        }
        try {
            String text = formatTemplate(subtitleInGame, player, gameMap);
            doUpdateSubtitle(player, text);
        } catch (Throwable t) {
            MythicSkywars.get().getLogger().log(Level.FINE, "Failed to update LabyMod subtitle", t);
        }
    }

    public static void resetSubtitle(Player player) {
        if (!isUsable() || !subtitlesEnabled || player == null) {
            return;
        }
        try {
            doResetSubtitle(player);
        } catch (Throwable t) {
            MythicSkywars.get().getLogger().log(Level.FINE, "Failed to reset LabyMod subtitle", t);
        }
    }

    // =========================================================================
    // Playing Game Mode (visible to LabyMod friends in chat)
    // =========================================================================

    public static void sendPlayingGameMode(Player player, GameMap gameMap) {
        if (!isUsable() || !gameModeEnabled || player == null) {
            return;
        }
        try {
            String mode;
            if (gameMap != null) {
                mode = formatTemplate(gameModeInGame, player, gameMap);
            } else {
                mode = gameModeLobby;
            }
            doSendPlayingGameMode(player, mode);
        } catch (Throwable t) {
            MythicSkywars.get().getLogger().log(Level.FINE, "Failed to send LabyMod playing game mode", t);
        }
    }

    // =========================================================================
    // Tab List Banner (custom image above player list)
    // =========================================================================

    public static void sendTabBanner(Player player) {
        if (!isUsable() || !tabBannerEnabled || player == null) {
            return;
        }
        if (tabBannerUrl == null || tabBannerUrl.trim().isEmpty()) {
            return;
        }
        try {
            doSendTabBanner(player, tabBannerUrl);
        } catch (Throwable t) {
            MythicSkywars.get().getLogger().log(Level.FINE, "Failed to send LabyMod tab banner", t);
        }
    }

    public static void clearTabBanner(Player player) {
        if (!isUsable() || player == null) {
            return;
        }
        try {
            doSendTabBanner(player, null);
        } catch (Throwable t) {
            MythicSkywars.get().getLogger().log(Level.FINE, "Failed to clear LabyMod tab banner", t);
        }
    }

    // =========================================================================
    // Interaction Menu (custom right-click entries on players)
    // =========================================================================

    public static void sendInteractionMenu(Player player) {
        if (!isUsable() || !interactionMenuEnabled || player == null) {
            return;
        }
        if (interactionMenuEntries.isEmpty()) {
            return;
        }
        try {
            doSendInteractionMenu(player);
        } catch (Throwable t) {
            MythicSkywars.get().getLogger().log(Level.FINE, "Failed to send LabyMod interaction menu", t);
        }
    }

    // =========================================================================
    // Addon Recommendations (suggest/require addons on join)
    // =========================================================================

    public static void sendAddonRecommendations(Player player) {
        if (!isUsable() || !addonRecommendEnabled || player == null) {
            return;
        }
        if (recommendedAddons.isEmpty() && requiredAddons.isEmpty()) {
            return;
        }
        try {
            doSendAddonRecommendations(player);
        } catch (Throwable t) {
            MythicSkywars.get().getLogger().log(Level.FINE, "Failed to send LabyMod addon recommendations", t);
        }
    }

    // =========================================================================
    // Markers (team ping forwarding — sets marker send type to SERVER)
    // =========================================================================

    /**
     * Enable marker forwarding for this player. When enabled, markers placed by
     * the player are sent to the server instead of only to LabyMod friends.
     * The server can then forward them to teammates.
     */
    public static void enableMarkerForwarding(Player player) {
        if (!isUsable() || !markersEnabled || player == null) {
            return;
        }
        try {
            doEnableMarkerForwarding(player);
        } catch (Throwable t) {
            MythicSkywars.get().getLogger().log(Level.FINE, "Failed to enable LabyMod marker forwarding", t);
        }
    }

    // =========================================================================
    // Server Switch Prompt (suggest switching to another server after match)
    // =========================================================================

    /**
     * Open a server switch prompt for the player (e.g. after a match ends in BungeeCord mode).
     */
    public static void promptServerSwitch(Player player) {
        if (!isUsable() || !serverSwitchEnabled || player == null) {
            return;
        }
        if (serverSwitchAddress == null || serverSwitchAddress.trim().isEmpty()) {
            return;
        }
        try {
            doPromptServerSwitch(player, serverSwitchTitle, serverSwitchAddress);
        } catch (Throwable t) {
            MythicSkywars.get().getLogger().log(Level.FINE, "Failed to send LabyMod server switch prompt", t);
        }
    }

    /**
     * Open a server switch prompt with a custom address and title.
     */
    public static void promptServerSwitch(Player player, String title, String address) {
        if (!isUsable() || player == null || address == null || address.trim().isEmpty()) {
            return;
        }
        try {
            doPromptServerSwitch(player, title, address);
        } catch (Throwable t) {
            MythicSkywars.get().getLogger().log(Level.FINE, "Failed to send LabyMod server switch prompt", t);
        }
    }

    // =========================================================================
    // Input Prompt (request text input from the player)
    // =========================================================================

    /**
     * Open an input prompt for the player (e.g. for report reasons).
     * The response is not handled here — register a PacketHandler if needed.
     */
    public static void openInputPrompt(Player player, String title, String placeholder, String defaultValue, int maxLength) {
        if (!isUsable() || !inputPromptEnabled || player == null) {
            return;
        }
        try {
            doOpenInputPrompt(player, title, placeholder, defaultValue, maxLength);
        } catch (Throwable t) {
            MythicSkywars.get().getLogger().log(Level.FINE, "Failed to open LabyMod input prompt", t);
        }
    }

    // =========================================================================
    // Tab List Flags (country flags next to player names)
    // =========================================================================

    /**
     * Set a country flag for a player in the tab list.
     * @param countryCode ISO 3166-1 alpha-2 country code (e.g. "DE", "US", "GB")
     */
    public static void setTabListFlag(Player player, String countryCode) {
        if (!isUsable() || !tabFlagsEnabled || player == null || countryCode == null) {
            return;
        }
        try {
            doSetTabListFlag(player, countryCode);
        } catch (Throwable t) {
            MythicSkywars.get().getLogger().log(Level.FINE, "Failed to set LabyMod tab list flag", t);
        }
    }

    // =========================================================================
    // Match tick task — periodic refresh (Discord RPC elapsed time, subtitles)
    // =========================================================================

    private static void startMatchTask(GameMap gameMap) {
        String arenaId = gameMap.getName();
        cancelMatchTask(arenaId);
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (gameMap.getMatchState() != MatchState.PLAYING) {
                    cancel();
                    matchTasks.remove(arenaId, this);
                    return;
                }
                try {
                    refreshMatchTick(gameMap);
                } catch (Throwable t) {
                    MythicSkywars.get().getLogger().log(Level.WARNING, "LabyMod match tick failed", t);
                }
            }
        }.runTaskTimer(MythicSkywars.get(), matchTickInterval, matchTickInterval);
        matchTasks.put(arenaId, task);
    }

    private static void refreshMatchTick(GameMap gameMap) {
        // Refresh Discord RPC (updates alive count, elapsed time)
        if (discordRpcEnabled) {
            for (Player alive : gameMap.getAlivePlayers()) {
                updateInGameRpc(alive, gameMap);
            }
        }
    }

    private static void cancelMatchTask(String arenaId) {
        BukkitTask t = matchTasks.remove(arenaId);
        if (t != null) {
            t.cancel();
        }
    }

    private static void cancelAllMatchTasks() {
        for (BukkitTask t : matchTasks.values()) {
            if (t != null) {
                t.cancel();
            }
        }
        matchTasks.clear();
    }

    // =========================================================================
    // Internal — LabyMod API calls (uses classes provided by the plugin at runtime)
    // =========================================================================

    private static void doSendDiscordRpc(Player player, String gameMode, boolean withStartTime, Long startMillis) {
        net.labymod.serverapi.server.bukkit.LabyModProtocolService service =
                net.labymod.serverapi.server.bukkit.LabyModProtocolService.get();
        if (service == null) {
            return;
        }
        net.labymod.serverapi.server.bukkit.LabyModPlayer labyPlayer = service.getPlayer(player.getUniqueId());
        if (labyPlayer == null) {
            return;
        }
        net.labymod.serverapi.core.model.feature.DiscordRPC discordRPC;
        if (withStartTime && startMillis != null) {
            discordRPC = net.labymod.serverapi.core.model.feature.DiscordRPC.createWithStart(gameMode, startMillis);
        } else {
            discordRPC = net.labymod.serverapi.core.model.feature.DiscordRPC.create(gameMode);
        }
        labyPlayer.sendDiscordRPC(discordRPC);
    }

    private static void doResetDiscordRpc(Player player) {
        net.labymod.serverapi.server.bukkit.LabyModProtocolService service =
                net.labymod.serverapi.server.bukkit.LabyModProtocolService.get();
        if (service == null) {
            return;
        }
        net.labymod.serverapi.server.bukkit.LabyModPlayer labyPlayer = service.getPlayer(player.getUniqueId());
        if (labyPlayer == null) {
            return;
        }
        labyPlayer.sendDiscordRPC(net.labymod.serverapi.core.model.feature.DiscordRPC.createReset());
    }

    private static void doUpdateEconomy(Player player, double balance) {
        net.labymod.serverapi.server.bukkit.LabyModProtocolService service =
                net.labymod.serverapi.server.bukkit.LabyModProtocolService.get();
        if (service == null) {
            return;
        }
        net.labymod.serverapi.server.bukkit.LabyModPlayer labyPlayer = service.getPlayer(player.getUniqueId());
        if (labyPlayer == null) {
            return;
        }
        if ("bank".equalsIgnoreCase(economyKey)) {
            labyPlayer.updateBankEconomy(economy -> {
                economy.balance(balance);
                economy.visible(true);
                if (economyIconUrl != null && !economyIconUrl.trim().isEmpty()) {
                    economy.iconUrl(economyIconUrl);
                }
            });
        } else if ("cash".equalsIgnoreCase(economyKey)) {
            labyPlayer.updateCashEconomy(economy -> {
                economy.balance(balance);
                economy.visible(true);
                if (economyIconUrl != null && !economyIconUrl.trim().isEmpty()) {
                    economy.iconUrl(economyIconUrl);
                }
            });
        } else {
            labyPlayer.updateEconomy(economyKey, economy -> {
                economy.balance(balance);
                economy.visible(true);
                if (economyIconUrl != null && !economyIconUrl.trim().isEmpty()) {
                    economy.iconUrl(economyIconUrl);
                }
            });
        }
    }

    private static void doApplyPermissions(Player player) {
        net.labymod.serverapi.server.bukkit.LabyModProtocolService service =
                net.labymod.serverapi.server.bukkit.LabyModProtocolService.get();
        if (service == null) {
            return;
        }
        net.labymod.serverapi.server.bukkit.LabyModPlayer labyPlayer = service.getPlayer(player.getUniqueId());
        if (labyPlayer == null) {
            return;
        }
        List<net.labymod.serverapi.core.model.moderation.Permission.StatedPermission> perms =
                new ArrayList<net.labymod.serverapi.core.model.moderation.Permission.StatedPermission>();
        for (String deny : permissionsDeny) {
            if (deny == null || deny.trim().isEmpty()) {
                continue;
            }
            perms.add(net.labymod.serverapi.core.model.moderation.Permission.of(deny.trim()).deny());
        }
        for (String allow : permissionsAllow) {
            if (allow == null || allow.trim().isEmpty()) {
                continue;
            }
            perms.add(net.labymod.serverapi.core.model.moderation.Permission.of(allow.trim()).allow());
        }
        if (!perms.isEmpty()) {
            labyPlayer.sendPermissions(perms);
        }
    }

    private static void doDisableAddons(Player player) {
        net.labymod.serverapi.server.bukkit.LabyModProtocolService service =
                net.labymod.serverapi.server.bukkit.LabyModProtocolService.get();
        if (service == null) {
            return;
        }
        net.labymod.serverapi.server.bukkit.LabyModPlayer labyPlayer = service.getPlayer(player.getUniqueId());
        if (labyPlayer == null) {
            return;
        }
        List<String> cleaned = new ArrayList<String>();
        for (String addon : addonsToDisable) {
            if (addon != null && !addon.trim().isEmpty()) {
                cleaned.add(addon.trim());
            }
        }
        if (!cleaned.isEmpty()) {
            labyPlayer.disableAddons(cleaned);
        }
    }

    private static void doRevertDisabledAddons(Player player) {
        net.labymod.serverapi.server.bukkit.LabyModProtocolService service =
                net.labymod.serverapi.server.bukkit.LabyModProtocolService.get();
        if (service == null) {
            return;
        }
        net.labymod.serverapi.server.bukkit.LabyModPlayer labyPlayer = service.getPlayer(player.getUniqueId());
        if (labyPlayer == null) {
            return;
        }
        labyPlayer.revertDisabledAddons();
    }

    private static void doUpdateSubtitle(Player player, String text) {
        net.labymod.serverapi.server.bukkit.LabyModProtocolService service =
                net.labymod.serverapi.server.bukkit.LabyModProtocolService.get();
        if (service == null) {
            return;
        }
        net.labymod.serverapi.server.bukkit.LabyModPlayer labyPlayer = service.getPlayer(player.getUniqueId());
        if (labyPlayer == null) {
            return;
        }
        labyPlayer.updateSubtitle(
                net.labymod.serverapi.api.model.component.ServerAPIComponent.text(text));
    }

    private static void doResetSubtitle(Player player) {
        net.labymod.serverapi.server.bukkit.LabyModProtocolService service =
                net.labymod.serverapi.server.bukkit.LabyModProtocolService.get();
        if (service == null) {
            return;
        }
        net.labymod.serverapi.server.bukkit.LabyModPlayer labyPlayer = service.getPlayer(player.getUniqueId());
        if (labyPlayer == null) {
            return;
        }
        labyPlayer.resetSubtitle();
    }

    private static void doSendPlayingGameMode(Player player, String gameMode) {
        net.labymod.serverapi.server.bukkit.LabyModProtocolService service =
                net.labymod.serverapi.server.bukkit.LabyModProtocolService.get();
        if (service == null) {
            return;
        }
        net.labymod.serverapi.server.bukkit.LabyModPlayer labyPlayer = service.getPlayer(player.getUniqueId());
        if (labyPlayer == null) {
            return;
        }
        labyPlayer.sendPlayingGameMode(gameMode);
    }

    private static void doSendTabBanner(Player player, String url) {
        net.labymod.serverapi.server.bukkit.LabyModProtocolService service =
                net.labymod.serverapi.server.bukkit.LabyModProtocolService.get();
        if (service == null) {
            return;
        }
        net.labymod.serverapi.server.bukkit.LabyModPlayer labyPlayer = service.getPlayer(player.getUniqueId());
        if (labyPlayer == null) {
            return;
        }
        labyPlayer.sendTabListBanner(url);
    }

    private static void doSendInteractionMenu(Player player) {
        net.labymod.serverapi.server.bukkit.LabyModProtocolService service =
                net.labymod.serverapi.server.bukkit.LabyModProtocolService.get();
        if (service == null) {
            return;
        }
        net.labymod.serverapi.server.bukkit.LabyModPlayer labyPlayer = service.getPlayer(player.getUniqueId());
        if (labyPlayer == null) {
            return;
        }
        List<net.labymod.serverapi.core.model.feature.InteractionMenuEntry> entries =
                new ArrayList<net.labymod.serverapi.core.model.feature.InteractionMenuEntry>();
        for (String[] raw : interactionMenuEntries) {
            String label = raw[0];
            String actionType = raw[1];
            String value = raw[2];
            net.labymod.serverapi.core.model.feature.InteractionMenuEntry.InteractionMenuType type;
            if ("CLIPBOARD".equals(actionType)) {
                type = net.labymod.serverapi.core.model.feature.InteractionMenuEntry.InteractionMenuType.CLIPBOARD;
            } else if ("SUGGEST_COMMAND".equals(actionType)) {
                type = net.labymod.serverapi.core.model.feature.InteractionMenuEntry.InteractionMenuType.SUGGEST_COMMAND;
            } else if ("OPEN_BROWSER".equals(actionType)) {
                type = net.labymod.serverapi.core.model.feature.InteractionMenuEntry.InteractionMenuType.OPEN_BROWSER;
            } else {
                type = net.labymod.serverapi.core.model.feature.InteractionMenuEntry.InteractionMenuType.RUN_COMMAND;
            }
            entries.add(net.labymod.serverapi.core.model.feature.InteractionMenuEntry.create(
                    net.labymod.serverapi.api.model.component.ServerAPIComponent.text(label),
                    type,
                    value
            ));
        }
        if (!entries.isEmpty()) {
            labyPlayer.sendInteractionMenuEntries(entries);
        }
    }

    private static void doSendAddonRecommendations(Player player) {
        net.labymod.serverapi.server.bukkit.LabyModProtocolService service =
                net.labymod.serverapi.server.bukkit.LabyModProtocolService.get();
        if (service == null) {
            return;
        }
        net.labymod.serverapi.server.bukkit.LabyModPlayer labyPlayer = service.getPlayer(player.getUniqueId());
        if (labyPlayer == null) {
            return;
        }
        List<net.labymod.serverapi.core.model.moderation.RecommendedAddon> addons =
                new ArrayList<net.labymod.serverapi.core.model.moderation.RecommendedAddon>();
        for (String namespace : recommendedAddons) {
            if (namespace != null && !namespace.trim().isEmpty()) {
                addons.add(net.labymod.serverapi.core.model.moderation.RecommendedAddon.of(namespace.trim()));
            }
        }
        for (String namespace : requiredAddons) {
            if (namespace != null && !namespace.trim().isEmpty()) {
                net.labymod.serverapi.core.model.moderation.RecommendedAddon addon =
                        net.labymod.serverapi.core.model.moderation.RecommendedAddon.of(namespace.trim());
                addon.require();
                addons.add(addon);
            }
        }
        if (!addons.isEmpty()) {
            labyPlayer.sendAddonRecommendations(addons);
        }
    }

    private static void doEnableMarkerForwarding(Player player) {
        net.labymod.serverapi.server.bukkit.LabyModProtocolService service =
                net.labymod.serverapi.server.bukkit.LabyModProtocolService.get();
        if (service == null) {
            return;
        }
        net.labymod.serverapi.server.bukkit.LabyModPlayer labyPlayer = service.getPlayer(player.getUniqueId());
        if (labyPlayer == null) {
            return;
        }
        // Set marker send type to SERVER so markers are forwarded to the server
        // instead of only to LabyMod friends.
        // Uses the packet directly: MarkerPacket(MarkerSendType.SERVER)
        net.labymod.serverapi.core.LabyModProtocol protocol = service.labyModProtocol();
        if (protocol != null) {
            protocol.sendPacket(player.getUniqueId(),
                    new net.labymod.serverapi.core.packet.clientbound.game.feature.marker.MarkerPacket(
                            net.labymod.serverapi.core.packet.clientbound.game.feature.marker.MarkerPacket.MarkerSendType.SERVER));
        }
    }

    private static void doPromptServerSwitch(Player player, String title, String address) {
        net.labymod.serverapi.server.bukkit.LabyModProtocolService service =
                net.labymod.serverapi.server.bukkit.LabyModProtocolService.get();
        if (service == null) {
            return;
        }
        net.labymod.serverapi.server.bukkit.LabyModPlayer labyPlayer = service.getPlayer(player.getUniqueId());
        if (labyPlayer == null) {
            return;
        }
        net.labymod.serverapi.core.model.supplement.ServerSwitchPrompt prompt =
                net.labymod.serverapi.core.model.supplement.ServerSwitchPrompt.create(
                        net.labymod.serverapi.api.model.component.ServerAPIComponent.text(
                                title != null ? title : "Switch server?"),
                        address
                );
        labyPlayer.openServerSwitchPrompt(prompt);
    }

    private static void doOpenInputPrompt(Player player, String title, String placeholder, String defaultValue, int maxLength) {
        net.labymod.serverapi.server.bukkit.LabyModProtocolService service =
                net.labymod.serverapi.server.bukkit.LabyModProtocolService.get();
        if (service == null) {
            return;
        }
        net.labymod.serverapi.server.bukkit.LabyModPlayer labyPlayer = service.getPlayer(player.getUniqueId());
        if (labyPlayer == null) {
            return;
        }
        net.labymod.serverapi.core.model.supplement.InputPrompt prompt =
                net.labymod.serverapi.core.model.supplement.InputPrompt.create(
                        net.labymod.serverapi.api.model.component.ServerAPIComponent.text(
                                title != null ? title : "Input"),
                        net.labymod.serverapi.api.model.component.ServerAPIComponent.text(
                                placeholder != null ? placeholder : ""),
                        defaultValue != null ? defaultValue : "",
                        maxLength > 0 ? maxLength : 256
                );
        labyPlayer.openInputPrompt(prompt);
    }

    private static void doSetTabListFlag(Player player, String countryCode) {
        net.labymod.serverapi.server.bukkit.LabyModProtocolService service =
                net.labymod.serverapi.server.bukkit.LabyModProtocolService.get();
        if (service == null) {
            return;
        }
        net.labymod.serverapi.server.bukkit.LabyModPlayer labyPlayer = service.getPlayer(player.getUniqueId());
        if (labyPlayer == null) {
            return;
        }
        // Use the country code enum — try to resolve it
        try {
            net.labymod.serverapi.core.model.display.TabListFlag.TabListFlagCountryCode code =
                    net.labymod.serverapi.core.model.display.TabListFlag.TabListFlagCountryCode.valueOf(
                            countryCode.trim().toUpperCase());
            labyPlayer.setTabListFlag(code);
        } catch (IllegalArgumentException ignored) {
            // Invalid country code — silently ignore
        }
    }

    // =========================================================================
    // Utility
    // =========================================================================

    private static String formatTemplate(String template, Player player, GameMap gameMap) {
        if (gameMap == null) {
            return template == null ? "" : template.replace("{player}", player.getName());
        }
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
