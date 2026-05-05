package systems.mythical.mythicskywars;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import systems.mythical.mythicskywars.api.MythicSkywarsAPI;
import systems.mythical.mythicskywars.api.impl.MythicSkywarsImpl;
import systems.mythical.mythicskywars.commands.*;
import systems.mythical.mythicskywars.commands.player.LeaveMatchExecutor;
import systems.mythical.mythicskywars.commands.player.RejoinMatchExecutor;
import systems.mythical.mythicskywars.clients.feather.FeatherClientBridge;
import systems.mythical.mythicskywars.clients.lunar.LunarApolloBridge;
import systems.mythical.mythicskywars.clients.lunar.LunarApolloGameplayListener;
import systems.mythical.mythicskywars.clients.labymod.LabyModBridge;
import systems.mythical.mythicskywars.config.Config;
import systems.mythical.mythicskywars.config.ConfigMerge;
import systems.mythical.mythicskywars.database.DataStorage;
import systems.mythical.mythicskywars.database.Database;
import systems.mythical.mythicskywars.enums.LeaderType;
import systems.mythical.mythicskywars.enums.MatchState;
import systems.mythical.mythicskywars.enums.PlayerRemoveReason;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.game.PlayerData;
import systems.mythical.mythicskywars.listeners.*;
import systems.mythical.mythicskywars.managers.*;
import systems.mythical.mythicskywars.managers.worlds.*;
import systems.mythical.mythicskywars.matchevents.GameEventsConfig;
import systems.mythical.mythicskywars.menus.*;
import systems.mythical.mythicskywars.menus.gameoptions.objects.GameKit;
import systems.mythical.mythicskywars.nms.NMS;
import systems.mythical.mythicskywars.utilities.Messaging;
import systems.mythical.mythicskywars.nms.NMSUtils;
import systems.mythical.mythicskywars.utilities.LobbyWaterPortalManager;
import systems.mythical.mythicskywars.utilities.LevelManager;
import systems.mythical.mythicskywars.utilities.LuckyBlockHook;
import systems.mythical.mythicskywars.utilities.SoulWellManager;
import systems.mythical.mythicskywars.utilities.SWRServer;
import systems.mythical.mythicskywars.utilities.BuiltInVaultEconomy;
import systems.mythical.mythicskywars.utilities.Util;
import systems.mythical.mythicskywars.utilities.UpdateChecker;
import systems.mythical.mythicskywars.utilities.holograms.DecentHoloUtil;
import systems.mythical.mythicskywars.utilities.holograms.HologramsUtil;
import systems.mythical.mythicskywars.utilities.minecraftping.MinecraftPing;
import systems.mythical.mythicskywars.utilities.minecraftping.MinecraftPingOptions;
import systems.mythical.mythicskywars.utilities.minecraftping.MinecraftPingReply;
import systems.mythical.mythicskywars.utilities.placeholders.SWRPlaceholderAPI;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Logger;

public class MythicSkywars extends JavaPlugin implements PluginMessageListener {

    private static MythicSkywars instance;
    private final ArrayList<String> leaderTypes = new ArrayList<>();
    private final Object leaderboardLock = new Object();
    private String servername;
    private Database db;
    private NMS nmsHandler;
    private MythicSkywarsAPI swrAPI = null;
    // Managers
    private MatchManager matchManager = null;
    private PlayerManager playerManager = null;
    private MainCmdManager mainCmdManager = null;
    private KitCmdManager kitCmdManager = null;
    private MapCmdManager mapCmdManager = null;
    private PartyCmdManager partyCmdManager = null;
    private SWTabCompleter swTabCompleter = null;
    private KitStorage kitStorage = null;
    private ChestManager cm = null;
    private WorldManager wm = null;
    private Messaging messaging;
    private Leaderboard leaderboard = null;
    private IconMenuController ic;
    private ItemsManager im;
    private GameMapManager gameMapManager;

    private PlayerOptionsManager pom;
    private LobbyWaterPortalManager lobbyWaterPortalManager;
    private SoulWellManager soulWellManager;

    private Config config;

    private HologramsUtil hu;
    private boolean loaded;
    private boolean mapsReady;
    private BukkitTask specObserver;
    private UpdateChecker updateChecker;
    private BuiltInVaultEconomy builtInVaultEconomy;

    // Utils

    public static MythicSkywars get() {
        return instance;
    }

    public static MythicSkywarsAPI getAPI() {
        return get().swrAPI;
    }

    public static Messaging getMessaging() {
        return instance.messaging;
    }

    public static Leaderboard getLB() {
        // TODO: Make non static
        synchronized (instance.leaderboardLock) {
            return instance.leaderboard;
        }
    }

    public static Config getCfg() {
        return instance.config;
    }

    public static IconMenuController getIC() {
        return instance.ic;
    }

    public static WorldManager getWM() {
        return instance.wm;
    }

    public static Database getDb() {
        return instance.db;
    }

    public static ChestManager getCM() {
        return instance.cm;
    }

    public static ItemsManager getIM() {
        return instance.im;
    }

    public static NMS getNMS() {
        return instance.nmsHandler;
    }

    public static HologramsUtil getHoloManager() {
        return instance.hu;
    }

    public static PlayerOptionsManager getOM() {
        return instance.pom;
    }

    public static LobbyWaterPortalManager getLobbyWaterPortals() {
        return instance.lobbyWaterPortalManager;
    }

    public static SoulWellManager getSoulWellManager() {
        return instance.soulWellManager;
    }

    public static UpdateChecker getUpdater() {
        return instance.updateChecker;
    }

    public boolean isNewVersion() {
        return true;
    }

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        loaded = false;
        mapsReady = false;

        // bStats metrics
        int pluginId = 24498; // MythicSkywars bStats plugin ID
        new org.bstats.bukkit.Metrics(this, pluginId);

        // NMS Init
        this.nmsHandler = NMSUtils.loadNMS(this);
        if (this.nmsHandler == null) {
            this.setEnabled(false);
            return;
        }

        servername = "none";

        // Load config for 1.8
        if (nmsHandler.getVersion() < 9) {
            File config = new File(MythicSkywars.get().getDataFolder(), "config.yml");
            if (!config.exists()) {
                MythicSkywars.get().saveResource("config18.yml", false);
                config = new File(MythicSkywars.get().getDataFolder(), "config18.yml");
                if (config.exists()) {
                    boolean result = config.renameTo(new File(MythicSkywars.get().getDataFolder(), "config.yml"));
                    if (result) {
                        getLogger().info("Loading 1.8 Configuration Files");
                    }
                }
            }
            // Load config for 1.12
        } else if (nmsHandler.getVersion() < 13 && nmsHandler.getVersion() > 8) {
            File config = new File(MythicSkywars.get().getDataFolder(), "config.yml");
            if (!config.exists()) {
                MythicSkywars.get().saveResource("config112.yml", false);
                config = new File(MythicSkywars.get().getDataFolder(), "config112.yml");
                if (config.exists()) {
                    boolean result = config.renameTo(new File(MythicSkywars.get().getDataFolder(), "config.yml"));
                    if (result) {
                        getLogger().info("Loading 1.9 - 1.12 Configuration Files");
                    }
                }
            }
        }
        // Ship default file if missing, then merge missing keys into config.yml on disk (not getConfig() — Bukkit
        // defaults from jar would make contains() think every key already exists).
        saveDefaultConfig();
        mergeConfigFromBundledTemplate();
        reloadConfig();

        // Load config data
        config = new Config();

        // State using debug mode or not
        if (getCfg().debugEnabled()) this.getLogger().info("Debug mode enabled");

        // Managers
        if (this.gameMapManager == null) this.gameMapManager = new GameMapManager(this);
        matchManager = MatchManager.get();
        playerManager = new PlayerManager(this);

        // Cages
        File cagesFolder = new File(getDataFolder(), "cages");
        if (!cagesFolder.exists()) {
            if (!cagesFolder.mkdir()) {
                getLogger().severe("Failed to create the cages folder.");
            }
        }

        // Client integration defaults (Lunar Apollo reference + Feather placeholder)
        saveResourceIfMissing("clients/lunar/config.yml");
        saveResourceIfMissing("clients/lunar/mods.yml.example");
        saveResourceIfMissing("clients/feather/config.yml");
        saveResourceIfMissing("clients/feather/mods.yml");
        saveResourceIfMissing("clients/labymod/config.yml");

        // ------ All external integrations --------
        // PAPI
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new SWRPlaceholderAPI().register();
        }
        // PAF
        if (getCfg().bungeeMode() && getCfg().isUsePartyAndFriends()) {
            // Currently disabled due to inability to access bungeecord PAF from spigot
            // this.getServer().getPluginManager().registerEvents(new PartyAndFriendsHook(), this);
        }
        // SLIME WORLD MANAGER
        if (Bukkit.getPluginManager().isPluginEnabled("SlimeWorldManager") && getCfg().isUseSlimeWorldManager()) {
            getLogger().info("SlimeWorldManager option enabled. Checking for AdvancedSlimePaper...");
            try {
                Class.forName("com.infernalsuite.aswm.SlimeNMSBridgeImpl");
                getLogger().info("Found AdvancedSlimePaper!");
                wm = (ASPWorldManager) Class.forName("systems.mythical.mythicskywars.managers.worlds.ASPWorldManagerImpl")
                        .getConstructor()
                        .newInstance();
            } catch (Exception e) {
                e.printStackTrace();
                getLogger().info("AdvancedSlimePaper not found");
                int serverFeatureVersion;
                try {
                    String bukkitVer = getServer().getBukkitVersion().split("-")[0]; // e.g. "26.1.2" or "1.21.1"
                    String[] verParts = bukkitVer.split("\\.");
                    if (verParts[0].equals("1")) {
                        serverFeatureVersion = Integer.parseInt(verParts[1]); // 1.XX.Y -> XX
                    } else {
                        serverFeatureVersion = Integer.parseInt(verParts[0]); // YY.D.H -> YY
                    }
                } catch (Exception ex2) {
                    serverFeatureVersion = Integer.parseInt(getServer().getVersion().split("\\.")[1]);
                }
                if (serverFeatureVersion > 19) {
                    getLogger().info("SlimeWorldManager cannot be used on 1.20 or higher. We expected the server to be running AdvancedSlimePaper.");
                    wm = null;
                } else if (serverFeatureVersion > 14) {
                    try {
                        getLogger().info("Using ASWM World Manager");
                        wm = (WorldManager) Class.forName("systems.mythical.mythicskywars.managers.worlds.ASWMWorldManager")
                                .getConstructor()
                                .newInstance();
                    } catch (Exception ex) {
                        getLogger().info("Using Bukkit World Manager");
                        wm = null;
                    }
                } else {
                    try {
                        getLogger().info("Using Legacy SWM World Manager");
                        wm = (WorldManager) Class.forName("systems.mythical.mythicskywars.managers.worlds.LegacySWMWorldManager")
                                .getConstructor()
                                .newInstance();
                    } catch (Exception ex) {
                        getLogger().info("Using Bukkit World Manager");
                        wm = null;
                    }
                }
            }
        }

        if (wm == null) {
            getLogger().info("Using Bukkit World Manager");
            wm = new FileWorldManager();
        }

        // MENUS
        ic = new IconMenuController();

        // LISTENERS
        if (nmsHandler.getVersion() > 8) {
            this.getServer().getPluginManager().registerEvents(new SwapHandListener(), this);
        }
        this.getServer().getPluginManager().registerEvents(ic, this);
        this.getServer().getPluginManager().registerEvents(new ArenaDamageListener(), this);
        ArenaPickupListener.registerAll(this);
        this.getServer().getPluginManager().registerEvents(new PlayerDeathListener(), this);
        this.getServer().getPluginManager().registerEvents(new PlayerQuitListener(), this);
        this.getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
        this.getServer().getPluginManager().registerEvents(new PlayerInteractListener(), this);
        this.getServer().getPluginManager().registerEvents(new SummonedMobListener(), this);
        this.getServer().getPluginManager().registerEvents(new PlayerTeleportListener(), this);
        this.getServer().getPluginManager().registerEvents(new LobbyListener(), this);
        this.getServer().getPluginManager().registerEvents(new StartupLoginBlocker(), this);
        this.getServer().getPluginManager().registerEvents(new LobbyWaterPortalListener(), this);
        this.getServer().getPluginManager().registerEvents(new SoulWellListener(), this);
        this.getServer().getPluginManager().registerEvents(new systems.mythical.mythicskywars.perks.PerkListener(), this);
        this.getServer().getPluginManager().registerEvents(new MapSearchListener(), this);
        this.getServer().getPluginManager().registerEvents(new SpectateListener(), this);
        this.getServer().getPluginManager().registerEvents(new ChatListener(), this);
        this.getServer().getPluginManager().registerEvents(new ProjectileSpleefListener(), this);
        try {
            this.getServer().getPluginManager().registerEvents(new LunarApolloGameplayListener(), this);
        } catch (NoClassDefFoundError e) {
            // Apollo not available, skip listener registration
        }
        this.getServer().getPluginManager().registerEvents(new WorldGuardBypassListener(), this);

        // LOAD BEFORE HOLO - Holo needs server to be loaded to update correctly
        load();
        // Apollo-Bukkit / Feather / LabyMod API may enable after this plugin; re-scan client bridges next tick.
        Bukkit.getScheduler().runTaskLater(this, () -> {
            try {
                LunarApolloBridge.reload(this);
            } catch (NoClassDefFoundError e) {
                // Apollo not available
            }
            try {
                FeatherClientBridge.reload(this);
            } catch (NoClassDefFoundError e) {
                // Feather not available
            }
            try {
                LabyModBridge.reload(this);
            } catch (NoClassDefFoundError e) {
                // LabyMod not available
            }
        }, 1L);

        // Requires IM - aka load()
        new ArenasMenu();

        // Holograms
        if (MythicSkywars.getCfg().hologramsEnabled()) {
            hu = null;
            if (Bukkit.getPluginManager().isPluginEnabled("DecentHolograms")) {
                hu = new DecentHoloUtil();
                hu.load();
            }
            if (hu == null) {
                config.setHologramsEnabled(false);
                config.save();
            }
        }

        // Taunts
        if (MythicSkywars.getCfg().tauntsEnabled()) {
            this.getServer().getPluginManager().registerEvents(new TauntListener(), this);
        }
        // Particles
        if (MythicSkywars.getCfg().particlesEnabled()) {
            this.getServer().getPluginManager().registerEvents(new ParticleEffectListener(), this);
        }
        // Disabled commands
        if (config.disableCommands()) {
            this.getServer().getPluginManager().registerEvents(new PlayerCommandPrepocessListener(), this);
        }

        // Plugin messaging channels
        if (getCfg().isBungeeEnabled()) {
            this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);
            Bukkit.getPluginManager().registerEvents(new PingListener(), this);
        }
        if (getCfg().bungeeMode() && getCfg().isLobbyServer()) {
            new BukkitRunnable() {
                public void run() {
                    if (servername.equalsIgnoreCase("none")) {
                        Player player = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
                        if (player != null) {
                            sendBungeeMsg(player, "GetServer", "none");
                        }
                    } else {
                        this.cancel();
                    }
                }
            }.runTaskTimer(this, 0, 20);

            prepareServers();

            SWRServer.updateServerSigns();

        }
        // TODO: SWR API - Not finished
        swrAPI = new MythicSkywarsImpl();
    }

    public void prepareServers() {
        SWRServer.clearServers();
        for (String server : getCfg().getGameServers()) {
            if (MythicSkywars.getCfg().debugEnabled()) {
                MythicSkywars.get().getLogger().warning(
                        "Setting up pinging service for " + server);
            }
            final String[] serverParts = server.split(":");
            if (serverParts.length >= 5) {
                SWRServer.addServer(new SWRServer(serverParts[0], Integer.parseInt(serverParts[1])));
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        SWRServer swrServer = SWRServer.getServer(serverParts[0]);
                        if (swrServer != null) {
                            swrServer.setDisplayName(serverParts[2]);
                            swrServer.setMaxPlayers(Integer.parseInt(serverParts[3]));
                            swrServer.setTeamsize(Integer.parseInt(serverParts[4]));
                            if (serverParts.length == 6) {
                                swrServer.setHostname(serverParts[5]);
                            }

                            /*Player player = Iterables.getFirst(MythicSkywars.get().getServer().getOnlinePlayers(), null);
                            if (player != null) {
                                Bukkit.getLogger().warning("Data we're trying to send: " + serverParts[0]);
                                sendBungeeMsg(player, "PlayerCount", serverParts[0]);
                            } else {*/
                            try {
                                String hostname = swrServer.getHostname() == null ? "127.0.0.1" : swrServer.getHostname();

                                MinecraftPingReply data = new MinecraftPing().getPing(new MinecraftPingOptions().setHostname(hostname).setPort(swrServer.getPort()));
                                if (data == null) return;
                                final String[] serverInfo = data.getDescription().getText().split(":");
                                if (serverInfo.length < 3) {
                                    MythicSkywars.get().getLogger().warning("Skywars Server Ping failed! " +
                                            hostname + ":" + swrServer.getPort() + " failed to return valid ping!\n" +
                                            "(" + data.getDescription().getText() + ")");
                                    return;
                                }
                                swrServer.setMatchState(serverInfo[0]);
                                swrServer.setPlayerCount(Integer.parseInt(serverInfo[1]));
                                swrServer.setMaxPlayers(Integer.parseInt(serverInfo[2]));
                            } catch (IOException e) {
                                swrServer.setMatchState(MatchState.OFFLINE);
                                e.printStackTrace();
                            } finally {
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        swrServer.updateSigns();
                                    }
                                }.runTask(MythicSkywars.get());
                            }
                            /*}*/
                        }
                    }
                }.runTaskTimerAsynchronously(this, 20, 200);
            }
        }
    }

    public void updateServers() {
        new BukkitRunnable() {
            public void run() {
                for (SWRServer server : SWRServer.getServersCopy()) {
                    Player player = Iterables.getFirst(MythicSkywars.get().getServer().getOnlinePlayers(), null);
                    if (player != null) {
                        sendBungeeMsg(player, "PlayerCount", server.getServerName());
                    }
                }
            }
        }.runTask(this);
    }

    public void onDisable() {
        loaded = false;
        unregisterBuiltInVaultEconomy();
        try {
            LunarApolloBridge.shutdown();
        } catch (NoClassDefFoundError e) {
            // Apollo not available, skip
        }
        try {
            FeatherClientBridge.shutdown();
        } catch (NoClassDefFoundError e) {
            // Feather not available, skip
        }
        try {
            LabyModBridge.shutdown();
        } catch (NoClassDefFoundError e) {
            // LabyMod not available, skip
        }
        if (updateChecker != null) {
            updateChecker.stop();
        }
        if (soulWellManager != null) {
            soulWellManager.deleteHologram();
        }
        this.getServer().getScheduler().cancelTasks(this);
        if (gameMapManager != null) {
            for (final GameMap gameMap : MythicSkywars.getGameMapMgr().getMapsCopy()) {
                if (gameMap.isEditing()) {
                    gameMap.saveMap(null);
                }
                ImmutableList<UUID> specUUIDs = ImmutableList.copyOf(gameMap.getSpectators());
                for (final UUID uuid : specUUIDs) {
                    final Player player = getServer().getPlayer(uuid);
                    if (player != null) {
                        MythicSkywars.get().getPlayerManager().removePlayer(
                                player, PlayerRemoveReason.OTHER, null, false);
                    }
                }
                ImmutableList<Player> players = ImmutableList.copyOf(gameMap.getAlivePlayers());
                for (final Player player : players) {
                    if (player != null) {
                        this.getPlayerManager().removePlayer(
                                player, PlayerRemoveReason.OTHER, null, false);
                    }
                }
                getWM().deleteWorld(gameMap.getName(), false);
            }
        }
        ImmutableList<PlayerData> pDataSnapshot = ImmutableList.copyOf(PlayerData.getAllPlayerData());
        for (final PlayerData playerData : pDataSnapshot) {
            playerData.restoreToBeforeGameState(true);
        }
        PlayerData.getAllPlayerData().clear();
        for (final PlayerStat fData : PlayerStat.getPlayers()) {
            DataStorage.get().saveStats(fData);
        }
        this.gameMapManager = null;
    }

    public void load() {
        messaging = null;
        messaging = new Messaging(this);
        LevelManager.get().load(this);
        GameEventsConfig.load(this);
        purgeLegacyMapEventSections();
        mergeConfigFromBundledTemplate();
        reloadConfig();
        config.load();
        try {
            LunarApolloBridge.reload(this);
        } catch (NoClassDefFoundError e) {
            getLogger().fine("Lunar Apollo not available (Apollo plugin not installed)");
        }
        try {
            FeatherClientBridge.reload(this);
        } catch (NoClassDefFoundError e) {
            getLogger().fine("Feather Client not available");
        }
        try {
            LabyModBridge.reload(this);
        } catch (NoClassDefFoundError e) {
            getLogger().fine("LabyMod not available");
        }
        // Register BUILTIN Vault provider as early as possible so other plugins can discover it on startup.
        if (MythicSkywars.getCfg().economyEnabled() && "BUILTIN".equalsIgnoreCase(MythicSkywars.getCfg().economyProvider())) {
            registerBuiltInVaultEconomy();
        }
        cm = new ChestManager();
        LuckyBlockHook.setup();
        if (LuckyBlockHook.isAvailable()) {
            getLogger().info("Lucky blocks: edit hand items, tiers, and NTD options in "
                    + ChestStorageLayout.resolvedFileForMessaging(this, "luckyblocks.yml").getAbsolutePath());
        }
        im = new ItemsManager();
        pom = new PlayerOptionsManager();
        lobbyWaterPortalManager = new LobbyWaterPortalManager(this);
        soulWellManager = new SoulWellManager(this);
        systems.mythical.mythicskywars.perks.PerkManager.get().reload();
        systems.mythical.mythicskywars.menus.soulwell.SoulWellService.validateRewardsOnStartup();

        if (gameMapManager == null) gameMapManager = new GameMapManager(this);

        GameKit.loadkits();
        MythicSkywars.getGameMapMgr().loadMaps();

        boolean sqlEnabled = getConfig().getBoolean("sqldatabase.enabled");
        if (sqlEnabled) {
            getFWDatabase();
        }
        leaderTypes.clear();

        for (LeaderType type : LeaderType.values()) {
            if (MythicSkywars.getCfg().isTypeEnabled(type)) {
                leaderTypes.add(type.toString());
            }
        }

        new BukkitRunnable() {
            public void run() {
                for (final Player player : getServer().getOnlinePlayers()) {
                    if (PlayerStat.getPlayerStats(player.getUniqueId().toString()) == null) {
                        PlayerStat pStats = new PlayerStat(player);
                        PlayerStat.getPlayers().add(pStats);
                        pStats.updatePlayerIfInLobby(player);
                        pStats.loadStats(null);
                    }
                }
                synchronized (leaderboardLock) {
                    leaderboard = new Leaderboard();
                }
            }
        }.runTaskAsynchronously(this);

        if (MythicSkywars.getCfg().economyEnabled()) {
            if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
                MythicSkywars.getCfg().setEconomyEnabled(false);
            }
            if (MythicSkywars.getCfg().economyEnabled()) {
                if ("BUILTIN".equalsIgnoreCase(MythicSkywars.getCfg().economyProvider())) {
                    registerBuiltInVaultEconomy();
                }
                RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
                if (rsp == null) {
                    MythicSkywars.getCfg().setEconomyEnabled(false);
                }
            }
        } else {
            unregisterBuiltInVaultEconomy();
        }

        if (MythicSkywars.getCfg().joinMenuEnabled() || MythicSkywars.getCfg().spectateMenuEnabled()) {
            new JoinMenu();
            new JoinSoloModeMenu();
            new JoinSingleMenu();
            new JoinTeamModeMenu();
            new JoinTeamMenu();
        }
        if (MythicSkywars.getCfg().spectateMenuEnabled()) {
            new SpectateMenu();
            new SpectateSingleMenu();
            new SpectateTeamMenu();
        }

        swTabCompleter = new SWTabCompleter();

        mainCmdManager = new MainCmdManager();
        getCommand("skywars").setExecutor(mainCmdManager);
        getCommand("skywars").setTabCompleter(swTabCompleter);
        if (getCommand("leave") != null) {
            getCommand("leave").setExecutor(new LeaveMatchExecutor());
        }
        if (getCommand("rejoin") != null) {
            getCommand("rejoin").setExecutor(new RejoinMatchExecutor());
        }

        kitCmdManager = new KitCmdManager();
        getCommand("swkit").setExecutor(kitCmdManager);
        getCommand("swkit").setTabCompleter(swTabCompleter);

        mapCmdManager = new MapCmdManager();
        getCommand("swmap").setExecutor(mapCmdManager);
        getCommand("swmap").setTabCompleter(swTabCompleter);

        if (config.partyEnabled()) {
            partyCmdManager = new PartyCmdManager();
            getCommand("swparty").setExecutor(partyCmdManager);
        }
        if (getCfg().borderEnabled()) {
            if (specObserver != null) {
                specObserver.cancel();
            }
            specObserver = new BukkitRunnable() {
                @Override
                public void run() {
                    for (GameMap gMap : MythicSkywars.getGameMapMgr().getMapsCopy()) {
                        gMap.checkSpectators();
                    }
                }
            }.runTaskTimer(MythicSkywars.get(), 0, 40);
        }
        loaded = true;

        // Start update checker
        if (getConfig().getBoolean("updater.enabled", true)) {
            String owner = getConfig().getString("updater.github-owner", "MythicalLTD");
            String repo = getConfig().getString("updater.github-repo", "MythicSkywars");
            boolean checkBeta = getCfg().isCheckForBetaVersion();
            boolean autoUpdate = getConfig().getBoolean("updater.autoUpdate", true);
            updateChecker = new UpdateChecker(this, owner, repo, checkBeta, autoUpdate);
            updateChecker.start();
        }
    }

    private void purgeLegacyMapEventSections() {
        File mapDataDirectory = new File(getDataFolder(), "mapsData");
        if (!mapDataDirectory.exists() || !mapDataDirectory.isDirectory()) {
            return;
        }
        File[] files = mapDataDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null || files.length == 0) {
            return;
        }
        int touched = 0;
        for (File file : files) {
            try {
                org.bukkit.configuration.file.FileConfiguration fc = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
                if (fc.contains("events")) {
                    fc.set("events", null);
                    fc.save(file);
                    touched++;
                }
            } catch (Exception ex) {
                getLogger().warning("Could not purge legacy events from " + file.getName() + ": " + ex.getMessage());
            }
        }
        if (touched > 0) {
            getLogger().info("Removed legacy per-map event sections from " + touched + " map file(s). Using gameevents.yml now.");
        }
    }

    private void mergeConfigFromBundledTemplate() {
        try {
            int merged = ConfigMerge.mergeBundledDefaultsIntoDiskConfigFile(this, getBundledMainConfigResourceName());
            if (merged > 0) {
                getLogger().info("Merged " + merged + " new config key(s) from " + getBundledMainConfigResourceName()
                        + " into config.yml (your existing settings were kept).");
            }
        } catch (IOException e) {
            getLogger().severe("Failed to merge bundled config into config.yml: " + e.getMessage());
        }
    }

    private void registerBuiltInVaultEconomy() {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        if (builtInVaultEconomy == null) {
            builtInVaultEconomy = new BuiltInVaultEconomy();
        }
        unregisterBuiltInVaultEconomy();
        Bukkit.getServicesManager().register(Economy.class, builtInVaultEconomy, this, ServicePriority.Highest);
        getLogger().info("Registered BUILTIN Vault economy provider for compatibility.");
    }

    private void unregisterBuiltInVaultEconomy() {
        if (builtInVaultEconomy != null) {
            Bukkit.getServicesManager().unregister(Economy.class, builtInVaultEconomy);
        }
    }

    /**
     * Bundled YAML that matches the Minecraft version (same file used for first-time installs).
     */
    private String getBundledMainConfigResourceName() {
        int v = nmsHandler.getVersion();
        if (v < 9) {
            return "config18.yml";
        }
        if (v < 13) {
            return "config112.yml";
        }
        return "config.yml";
    }

    private void getFWDatabase() {
        try {
            db = new Database();
        } catch (ClassNotFoundException | SQLException e) {
            getLogger().severe("Failed to initialize SQL database: " + e.getMessage());
            db = null;
            return;
        }
        try {
            db.createTables();
        } catch (IOException | SQLException e) {
            getLogger().severe("Failed creating SQL tables: " + e.getMessage());
        }
        if (db != null) {
            db.ensureSoulColumn();
        }
    }

    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord")) {
            return;
        }
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subchannel = in.readUTF();

        try {
            if (subchannel.equals("GetServer")) {
                servername = in.readUTF();
            }

            if (subchannel.equals("PlayerCount")) {
                String server = in.readUTF();
                int playercount = in.readInt();

                final SWRServer swrServer = SWRServer.getServer(server);

                if (swrServer != null) {
                    if (playercount == 0) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                try {
                                    String hostname = swrServer.getHostname() == null ? "127.0.0.1" : swrServer.getHostname();

                                    MinecraftPingReply data = new MinecraftPing().getPing(new MinecraftPingOptions().setHostname(hostname).setPort(swrServer.getPort()));
                                    if (data == null) return;
                                    final String[] serverInfo = data.getDescription().getText().split(":");
                                    swrServer.setMatchState(serverInfo[0]);
                                    swrServer.updateSigns();

                                } catch (IOException e) {
                                    swrServer.setMatchState(MatchState.OFFLINE);
                                    swrServer.updateSigns();
                                }
                            }
                        }.runTask(this);
                    } else {
                        if (player != null) {
                            ArrayList<String> messages = new ArrayList<String>();
                            messages.add("RequestUpdate");
                            messages.add(servername);
                            sendSWRMessage(player, server, messages);
                        }
                    }
                }
            }

            if (subchannel.equals("SWRMessaging")) {
                short len = in.readShort();
                byte[] msgbytes = new byte[len];
                in.readFully(msgbytes);

                DataInputStream msgin = new DataInputStream(new ByteArrayInputStream(msgbytes));
                try {
                    String header = msgin.readUTF();
                    if (header.equalsIgnoreCase("ServerUpdate")) {
                        String server = msgin.readUTF();
                        String playerCount = msgin.readUTF();
                        String maxPlayers = msgin.readUTF();
                        String gameStarted = msgin.readUTF();
                        SWRServer swrServer = SWRServer.getServer(server);
                        if (swrServer != null) {
                            if (Util.get().isInteger(playerCount)) {
                                swrServer.setPlayerCount(Integer.parseInt(playerCount));
                            }
                            if (Util.get().isInteger(maxPlayers)) {
                                swrServer.setMaxPlayers(Integer.parseInt(maxPlayers));
                            }
                            swrServer.setMatchState(gameStarted);
                            swrServer.updateSigns();
                        }
                    }
                    if (header.equalsIgnoreCase("RequestUpdate")) {
                        String sendToServer = msgin.readUTF();
                        GameMap gMap = MythicSkywars.getGameMapMgr().getMapsCopy().get(0);
                        String playerCount = "" + gMap.getAlivePlayers().size();
                        String maxPlayers = "" + gMap.getMaxPlayers();
                        String gameStarted = "" + gMap.getMatchState().toString();
                        ArrayList<String> messages = new ArrayList<>();
                        messages.add("ServerUpdate");
                        messages.add(servername);
                        messages.add(playerCount);
                        messages.add(maxPlayers);
                        messages.add(gameStarted);
                        sendSWRMessage(player, sendToServer, messages);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception ex) {
            Logger logger = instance.getLogger();
            logger.warning("Invalid plugin message was received! Enable debug mode before reporting this! (in config.yml)");
            if (config.debugEnabled()) {
                String pName = player == null ? "null" : player.getName();
                logger.info("MythicSkywars::onPluginMessageReceived subchannel=" + subchannel + ", playerName=" + pName);
                ex.printStackTrace();
            }
        }
    }

    public void sendBungeeMsg(Player player, String subchannel, String message) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(subchannel);
        if (!message.equalsIgnoreCase("none")) {
            out.writeUTF(message);
        }
        player.sendPluginMessage(this, "BungeeCord", out.toByteArray());
    }

    public void sendSWRMessage(Player player, String server, ArrayList<String> messages) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF(server);
        out.writeUTF("SWRMessaging");

        ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
        DataOutputStream msgout = new DataOutputStream(msgbytes);
        try {
            for (String msg : messages) {
                msgout.writeUTF(msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        out.writeShort(msgbytes.toByteArray().length);
        out.write(msgbytes.toByteArray());
        player.sendPluginMessage(this, "BungeeCord", out.toByteArray());
    }


    // GETTERS AND SETTERS

    public MatchManager getMatchManager() {
        return this.matchManager;
    }

    public PlayerManager getPlayerManager() {
        return this.playerManager;
    }

    public MainCmdManager getMainCmdManager() {
        return this.mainCmdManager;
    }

    public KitCmdManager getKitCmdManager() {
        return this.kitCmdManager;
    }

    public MapCmdManager getMapCmdManager() {
        return this.mapCmdManager;
    }

    public PartyCmdManager getPartyCmdManager() {
        return this.partyCmdManager;
    }

    public SWTabCompleter getSwTabCompleter() {
        return this.swTabCompleter;
    }

    public KitStorage getKitStorage() {return this.kitStorage;}

    public String getServerName() {
        return servername;
    }

    public ArrayList<String> getLeaderTypes() {
        return leaderTypes;
    }

    public PlayerStat getPlayerStat(Player player) {
        return PlayerStat.getPlayerStats(player);
    }

    public void setChestManager(ChestManager chestManager) {
        this.cm = chestManager;
    }

    public boolean serverLoaded() {
        return loaded;
    }

    public boolean areMapsReady() {
        return mapsReady;
    }

    public void setMapsReady(boolean ready) {
        this.mapsReady = ready;
    }

    public GameMapManager getGameMapManager() {
        return this.gameMapManager;
    }

    public void setGameMapManager(GameMapManager gameMapManager) {
        this.gameMapManager = gameMapManager;
    }

    public static GameMapManager getGameMapMgr() {
        return instance.gameMapManager;
    }

    private void saveResourceIfMissing(String path) {
        File out = new File(getDataFolder(), path);
        if (!out.exists()) {
            saveResource(path, false);
        }
    }
}
