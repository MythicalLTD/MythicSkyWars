package systems.mythical.mythicskywars.config;

import com.google.common.collect.Lists;
import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.enums.LeaderType;
import systems.mythical.mythicskywars.utilities.LuckyBlockHook;
import systems.mythical.mythicskywars.utilities.Util;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class Config {

    private final List<String> itemNames = Arrays.asList("kitvote", "votingItem", "teamSelectItem",
            "exitMenuItem", "nextPageItem", "prevPageItem",
            "exitGameItem", "playAgainItem",
            "chestvote", "chestrandom", "chestbasic", "chestnormal", "chestop",
            "healthvote", "healthrandom", "healthfive", "healthten", "healthfifteen", "healthtwenty",
            "nopermission",
            "timevote", "timerandom", "timedawn", "timenoon", "timedusk", "timemidnight",
            "weathervote", "weatherrandom", "weathersunny", "weatherrain", "weatherstorm", "weathersnow",
            "modifiervote", "modifierrandom", "modifierspeed", "modifierjump", "modifierstrength", "modifiernone",
            "joinselect",
            "backlobbyitem",
            "rejoinitem",
            "statsitem",
            "singlemenu",
            "teammenu",
            "spectateselect",
            "optionselect",
            "particleselect",
            "projectileselect",
            "killsoundselect", "killsounditem",
            "winsoundselect",
            "glassselect", "tauntselect");
    private final List<String> defItems13 = Arrays.asList("ENDER_EYE", "COMPASS", "END_CRYSTAL",
            "BARRIER", "FEATHER", "FEATHER",
            "IRON_DOOR", "EMERALD",
            "SHIELD", "NETHER_STAR", "STONE_SWORD", "IRON_SWORD", "DIAMOND_SWORD",
            "EXPERIENCE_BOTTLE", "NETHER_STAR", "REDSTONE", "REDSTONE", "REDSTONE", "REDSTONE",
            "BARRIER",
            "CLOCK", "NETHER_STAR", "CLOCK", "CLOCK", "CLOCK", "CLOCK",
            "BLAZE_POWDER", "NETHER_STAR", "PRISMARINE_SHARD", "PRISMARINE_SHARD", "PRISMARINE_SHARD", "PRISMARINE_SHARD",
            "DRAGON_BREATH", "NETHER_STAR", "BOOK", "BOOK", "BOOK", "BOOK",
            "DIAMOND_SWORD", "RED_BED", "DIAMOND_SWORD", "BOOK",
            "REDSTONE_TORCH",
            "COMPARATOR",
            "WOOD_SWORD",
            "ENDER_EYE",
            "BLAZE_POWDER",
            "ARROW",
            "DIAMOND_SWORD", "NOTE_BLOCK",
            "DRAGON_EGG",
            "GLASS", "SHIELD");
    private final List<String> defItems12 = Arrays.asList("EYE_OF_ENDER", "COMPASS", "END_CRYSTAL",
            "BARRIER", "FEATHER", "FEATHER",
            "IRON_DOOR", "EMERALD",
            "SHIELD", "NETHER_STAR", "STONE_SWORD", "IRON_SWORD", "DIAMOND_SWORD",
            "EXP_BOTTLE", "NETHER_STAR", "REDSTONE", "REDSTONE", "REDSTONE", "REDSTONE",
            "BARRIER",
            "WATCH", "NETHER_STAR", "WATCH", "WATCH", "WATCH", "WATCH",
            "BLAZE_POWDER", "NETHER_STAR", "PRISMARINE_SHARD", "PRISMARINE_SHARD", "PRISMARINE_SHARD", "PRISMARINE_SHARD",
            "DRAGONS_BREATH", "NETHER_STAR", "BOOK", "BOOK", "BOOK", "BOOK",
            "DIAMOND_SWORD", "BED", "DIAMOND_SWORD", "BOOK",
            "REDSTONE_TORCH_OFF",
            "REDSTONE_COMPARATOR",
            "WOOD_SWORD",
            "EYE_OF_ENDER",
            "BLAZE_POWDER",
            "ARROW",
            "DIAMOND_SWORD", "NOTE_BLOCK",
            "DRAGON_EGG",
            "STAINED_GLASS", "SHIELD");
    private final List<String> defItems8 = Arrays.asList("EYE_OF_ENDER", "COMPASS", "WATCH",
            "BARRIER", "FEATHER", "FEATHER",
            "IRON_DOOR", "EMERALD",
            "DIAMOND", "NETHER_STAR", "STONE_SWORD", "IRON_SWORD", "DIAMOND_SWORD",
            "EXP_BOTTLE", "NETHER_STAR", "REDSTONE", "REDSTONE", "REDSTONE", "REDSTONE",
            "BARRIER",
            "WATCH", "NETHER_STAR", "WATCH", "WATCH", "WATCH", "WATCH",
            "BLAZE_POWDER", "NETHER_STAR", "PRISMARINE_SHARD", "PRISMARINE_SHARD", "PRISMARINE_SHARD", "PRISMARINE_SHARD",
            "DRAGON_EGG", "NETHER_STAR", "BOOK", "BOOK", "BOOK", "BOOK",
            "DIAMOND_SWORD", "BED", "DIAMOND_SWORD", "BOOK",
            "REDSTONE_TORCH_OFF",
            "REDSTONE_COMPARATOR",
            "WOOD_SWORD",
            "EYE_OF_ENDER",
            "BLAZE_POWDER",
            "ARROW",
            "DIAMOND_SWORD", "NOTE_BLOCK",
            "DRAGON_EGG",
            "STAINED_GLASS", "DRAGON_EGG");
    private final List<String> signItems = Arrays.asList("blockoffline", "blockwaiting", "blockplaying", "blockending", "almostfull", "threefull", "halffull", "almostempty");
    private final List<String> signDef8 = Arrays.asList("COAL_BLOCK", "EMERALD_BLOCK", "REDSTONE_BLOCK", "LAPIS_BLOCK", "DIAMOND_SWORD", "GOLD_HELMET", "IRON_HELMET", "WOOD_SWORD");
    private final List<String> signDef13 = Arrays.asList("COAL_BLOCK", "EMERALD_BLOCK", "REDSTONE_BLOCK", "LAPIS_BLOCK", "DIAMOND_SWORD", "GOLDEN_HELMET", "IRON_HELMET", "WOODEN_SWORD");
    private boolean debug;
    private boolean bungeeMode;
    /** proxy = network lobby/sign mode, server = local maps with optional lobby bed transfer */
    private String bungeeModeType;
    private boolean bungeeRandomMapPickOnStart;
    private String bungeeLobby;
    private boolean economyEnabled;
    private List<String> gameEndCommands;
    private boolean resetTimerOnJoin;
    private String resourcePack;
    private boolean promptResource;
    private int kitvotepos;
    private boolean kitsEnabled;
    private int votepos;
    private boolean voteEnabled;
    private int exitpos;
    private int playAgainPos;
    private int teamSelectPos;

    private boolean clearInventoryOnLobbyJoin;

    private boolean randomOptionVoteEnabled;
    private boolean joinGameItemEnabled;
    /** When true, the lobby join compass joins a random arena instead of opening solo/team menus. */
    private boolean joinCompassRandomJoin;
    private boolean spectateGameItemEnabled;
    private boolean optionsGameItemEnabled;
    private boolean playAgainItemEnabled;
    private boolean backToLobbyItemEnabled;
    private boolean rejoinItemEnabled;
    private boolean statsItemEnabled;
    private int backToLobbyPos;
    private int rejoinItemPos;
    private int statsItemPos;
    private boolean rejoinEnabled;
    private int rejoinWindowSeconds;

    private int chestvotepos;
    private boolean chestVoteEnabled;
    private int healthvotepos;
    private boolean healthVoteEnabled;
    private int timevotepos;
    private boolean timeVoteEnabled;
    private int weathervotepos;
    private boolean weatherVoteEnabled;
    private int modifiervotepos;
    private boolean modifierVoteEnabled;
    private int particleselectslot;
    private int projectileselectslot;
    private int killsoundselectslot;
    private int winsoundselectslot;
    private int glassselectslot;
    private int tauntselectslot;
    private int leaderSize;
    private boolean leaderSignsEnabled;
    private boolean leaderHeadsEnabled;
    private int leaderboardUpdateInterval;
    private boolean winsEnabled;
    private boolean lossesEnabled;
    private boolean killsEnabled;
    private boolean deathsEnabled;
    private boolean xpEnabled;
    private boolean lobbyBoardEnabled;
    private boolean protectlobby;
    private boolean lobbyForceSunny;
    private boolean lobbyForceDay;
    private boolean lobbyHideJoinQuitMessages;
    private boolean lobbyHideDeathMessages;
    private boolean displayPlayerExeperience;
    private boolean borderEnabled;
    private int borderSize;
    private boolean showHealth;
    private int winnerEco;
    private int killerEco;
    private int snowballDamage;
    private int eggDamage;
    private List<String> winCommands;
    private List<String> killCommands;
    private boolean tauntsEnabled;
    private boolean titlesEnabled;
    private boolean kitVotingEnabled;
    private int waitTimer;
    private int strength;
    private int speed;
    private int jump;

    private boolean usePlayerNames;
    private boolean usePlayerGlassColors;
    private String teamMaterial;
    private boolean useTeamMaterialBytes;
    private int standardTeamMaterialByte;
    private boolean useSeparateCages;
    private boolean changeTablistNames;
    private boolean useTeamNumberInMenu;
    private boolean balanceUnselectedPlayersAcrossTeams;

    private int timeAfterMatch;
    private boolean fireworksEnabled;
    private int fireworksPer5Tick;
    private int maxMapSize;
    private Location spawn;
    private boolean lookDirectionEnabled;
    private boolean pressurePlate;
    private boolean teleportOnJoin;
    private boolean teleportOnWorldEnter;
    private int maxPartySize;
    private boolean partyEnabled;
    private List<String> lobbyWorlds;
    private boolean loadTrappedChestsAsCenter;
    private int maxChest;
    private int maxDoubleChest;
    private boolean chestRefillEnabled;
    private int chestRefillIntervalSeconds;
    private boolean chestRefillKeepChestOpen;
    private boolean chestRefillShowHologram;
    private boolean useHolograms;
    private int cooldown;
    private int kitMenuSize;
    private int randPos;
    private int noKitPos;
    private String randMat;
    private String noKitMat;
    private boolean particlesEnabled;
    private int ticksPerUpdate;
    private boolean joinEnabled;
    private int joinSlot;
    private int singleSlot;
    private int teamSlot;
    private boolean spectateMenuEnabled;
    private int spectateSlot;
    private boolean optionsEnabled;
    private int optionsSlot;
    private boolean glassEnabled;
    private boolean particleEnabled;
    private boolean projectEnabled;
    private boolean killsoundEnabled;
    private boolean winsoundEnabled;
    private boolean tauntsMenuEnabled;
    private boolean soulWellEnabled;
    private int soulWellSoulsPerKill;
    private int soulWellSoulsPerWin;
    private int soulWellSoulsPerSpin;
    private int soulWellMaxSpinsAtOnce;
    private String soulWellXezbethPermission;
    private int soulWellAnimTicksPerFrame;
    private int soulWellAnimFrames;
    private int soulWellOptionsMenuSlot;
    private int soulWellWeightCoins;
    private int soulWellWeightXp;
    private int soulWellWeightCage;
    private int soulWellWeightKit;
    private int soulWellWeightPerk;
    private int soulWellWeightCommand;
    private int soulWellWeightGamePerk;
    private int soulWellCoinsMin;
    private int soulWellCoinsMax;
    private int soulWellXpMin;
    private int soulWellXpMax;
    private List<String> soulWellKitAllowList = Lists.newArrayList();
    private List<String> soulWellKitDenyList = Lists.newArrayList();
    private List<String> soulWellPerkAllowList = Lists.newArrayList();
    private List<String> soulWellPerkDenyList = Lists.newArrayList();
    private List<String> soulWellCageAllowList = Lists.newArrayList();
    private List<String> soulWellCageDenyList = Lists.newArrayList();
    private List<String> soulWellCommandRewards = Lists.newArrayList();
    private int soulWellWeightKillSound;
    private int soulWellWeightWinSound;
    private int soulWellWeightProjectile;
    private int soulWellWeightTaunt;
    private List<String> soulWellKillSoundAllowList = Lists.newArrayList();
    private List<String> soulWellKillSoundDenyList = Lists.newArrayList();
    private List<String> soulWellWinSoundAllowList = Lists.newArrayList();
    private List<String> soulWellWinSoundDenyList = Lists.newArrayList();
    private List<String> soulWellProjectileAllowList = Lists.newArrayList();
    private List<String> soulWellProjectileDenyList = Lists.newArrayList();
    private List<String> soulWellTauntAllowList = Lists.newArrayList();
    private List<String> soulWellTauntDenyList = Lists.newArrayList();
    private boolean playSounds;
    private String countdown;
    private String joinSound;
    private String leaveSound;
    private String openJoinMenu;
    private String openSpectateMenu;
    private String openOptionsMenu;
    private String openGlassMenu;
    private String openWinSoundMenu;
    private String openKillSoundMenu;
    private String openParticleMenu;
    private String openProjectileMenu;
    private String openTauntMenu;
    private String openKitMenu;
    private String openChestMenu;
    private String openTimeMenu;
    private String openWeatherMenu;
    private String openHealthMenu;
    private String openModifierMenu;
    private String confirmSelection;
    private String errorSound;
    private boolean spectateEnabled;
    private boolean disableCommands;
    private List<String> enabledCommands;
    private boolean disableCommandsSpectate;
    private List<String> enabledCommandsSpectate;
    private boolean useExternalChat;
    private boolean addPrefix;
    private boolean enableFormatter;
    private boolean limitGameChat;
    private boolean limitSpecChat;
    private boolean limitLobbyChat;
    private Map<String, String> materials = new HashMap<>();
    private boolean loading = false;

    private boolean enableWinMessage = true;
    private boolean enablePVPTimer = true;
    private int PVPTimerTime = 0;
    private boolean enableQuickDeath = true;
    private int quickDeathY = 0;
    private boolean kickOnWorldTeleport = true;

    private boolean clearInventoryOnWin = true;
    private boolean enableFlightOnWin = false;

    private List<String> gameServers = Lists.newArrayList();
    private boolean isLobbyServer = false;

    private boolean useSlimeWorldManager = false;
    private String slimeWorldManagerSource = "file";
    private boolean usePartyAndFriends = false;
    private boolean useTeamChat = true;
    private String timeFormat = "mm:ss";
    private boolean checkForBetaVersion = true;
    private boolean displayTimerOnLevelbar = true;
    private String teamJoinSound;

    private String defaultKit;

    public Config() {
        load();
    }

    public void load() {
        if (!loading) {
            loading = true;
            debug = MythicSkywars.get().getConfig().getBoolean("debugMode");

            checkForBetaVersion = MythicSkywars.get().getConfig().getBoolean("updater.checkForBetaVersions");

            gameServers = MythicSkywars.get().getConfig().getStringList("gameServers");

            bungeeMode = MythicSkywars.get().getConfig().getBoolean("bungeeMode");
            bungeeModeType = MythicSkywars.get().getConfig().getString("bungeeModeType", "proxy");
            bungeeRandomMapPickOnStart = MythicSkywars.get().getConfig().getBoolean("bungeeRandomMapPickOnStart");
            bungeeLobby = MythicSkywars.get().getConfig().getString("bungeeLobby");
            isLobbyServer = MythicSkywars.get().getConfig().getBoolean("isLobbyServer");
            economyEnabled = MythicSkywars.get().getConfig().getBoolean("economyEnabled");
            gameEndCommands = MythicSkywars.get().getConfig().getStringList("gameEndCommands");
            resourcePack = MythicSkywars.get().getConfig().getString("resourcepack");
            promptResource = MythicSkywars.get().getConfig().getBoolean("promptForResourcePackOnJoin");
            clearInventoryOnLobbyJoin = MythicSkywars.get().getConfig().getBoolean("clearInventoryOnLobbyJoin");
            timeFormat = MythicSkywars.get().getConfig().getString("timeFormat");

            defaultKit = MythicSkywars.get().getConfig().getString("defaultKit", "null");

            lobbyBoardEnabled = MythicSkywars.get().getConfig().getBoolean("lobbyBoardEnabled");
            protectlobby = MythicSkywars.get().getConfig().getBoolean("enabledLobbyGuard");
            lobbyForceSunny = MythicSkywars.get().getConfig().getBoolean("lobby.forceSunnyWhenProtected", true);
            lobbyForceDay = MythicSkywars.get().getConfig().getBoolean("lobby.forceDayWhenProtected", true);
            lobbyHideJoinQuitMessages = MythicSkywars.get().getConfig().getBoolean("lobby.hideJoinQuitMessages", false);
            lobbyHideDeathMessages = MythicSkywars.get().getConfig().getBoolean("lobby.hideDeathMessages", false);
            displayPlayerExeperience = MythicSkywars.get().getConfig().getBoolean("displayPlayerLevelOnXpBar");
            leaderSize = MythicSkywars.get().getConfig().getInt("leaderboards.length");
            leaderSignsEnabled = MythicSkywars.get().getConfig().getBoolean("leaderboards.signsEnabled");
            leaderHeadsEnabled = MythicSkywars.get().getConfig().getBoolean("leaderboards.headsEnabled");
            winsEnabled = MythicSkywars.get().getConfig().getBoolean("leaderboards.winsLeaderboardEnabled");
            lossesEnabled = MythicSkywars.get().getConfig().getBoolean("leaderboards.lossesLeaderboardEnabled");
            killsEnabled = MythicSkywars.get().getConfig().getBoolean("leaderboards.killsLeaderboardEnabled");
            deathsEnabled = MythicSkywars.get().getConfig().getBoolean("leaderboards.deathsLeaderboardEnabled");
            xpEnabled = MythicSkywars.get().getConfig().getBoolean("leaderboards.xpLeaderboardEnabled");
            leaderboardUpdateInterval = MythicSkywars.get().getConfig().getInt("leaderboards.leaderboardUpdateInterval");

            displayTimerOnLevelbar = MythicSkywars.get().getConfig().getBoolean("game.displayTimerOnLevelbar");
            enableFlightOnWin =     MythicSkywars.get().getConfig().getBoolean("game.win.enableFlight");
            clearInventoryOnWin =   MythicSkywars.get().getConfig().getBoolean("game.win.clearInventory");
            kickOnWorldTeleport =   MythicSkywars.get().getConfig().getBoolean("game.kickOnWorldTeleport");
            enableQuickDeath =      MythicSkywars.get().getConfig().getBoolean("game.enableQuickDeath");
            quickDeathY =           MythicSkywars.get().getConfig().getInt("game.quickDeathY");
            enablePVPTimer =        MythicSkywars.get().getConfig().getBoolean("game.enablePVPTimer");
            PVPTimerTime =          MythicSkywars.get().getConfig().getInt("game.PVPTimerTime");
            enableWinMessage =      MythicSkywars.get().getConfig().getBoolean("game.enableWinMessage");
            borderEnabled =         MythicSkywars.get().getConfig().getBoolean("game.worldBorder.enabled");
            lookDirectionEnabled =  MythicSkywars.get().getConfig().getBoolean("game.enableLookDirection");
            borderSize =            MythicSkywars.get().getConfig().getInt("game.worldBorder.borderSize");
            showHealth =            MythicSkywars.get().getConfig().getBoolean("game.showHealth");
            winnerEco =             MythicSkywars.get().getConfig().getInt("game.ecoForWin");
            killerEco =             MythicSkywars.get().getConfig().getInt("game.ecoForKill");
            snowballDamage =        MythicSkywars.get().getConfig().getInt("game.snowballDamage");
            eggDamage =             MythicSkywars.get().getConfig().getInt("game.eggDamage");
            winCommands =           MythicSkywars.get().getConfig().getStringList("game.winCommands");
            killCommands =          MythicSkywars.get().getConfig().getStringList("game.killCommands");
            spawn = Util.get().stringToLocation(MythicSkywars.get().getConfig().getString("spawn"));
            debugTesting();
            timeAfterMatch =        MythicSkywars.get().getConfig().getInt("game.timeAfterMatch");
            fireworksPer5Tick =     MythicSkywars.get().getConfig().getInt("fireworks.per5Ticks");
            fireworksEnabled =      MythicSkywars.get().getConfig().getBoolean("fireworks.enabled");
            waitTimer =             MythicSkywars.get().getConfig().getInt("game.waitTimer");
            resetTimerOnJoin =      MythicSkywars.get().getConfig().getBoolean("game.resetTimerOnJoin");
            tauntsEnabled =         MythicSkywars.get().getConfig().getBoolean("game.tauntsEnabled");
            titlesEnabled =         MythicSkywars.get().getConfig().getBoolean("titles.enabled");
            kitVotingEnabled =      MythicSkywars.get().getConfig().getBoolean("game.kitVotingEnabled");
            spectateEnabled =       MythicSkywars.get().getConfig().getBoolean("game.spectateEnabled");
            maxMapSize =            MythicSkywars.get().getConfig().getInt("game.maxMapSize");
            pressurePlate =         MythicSkywars.get().getConfig().getBoolean("enablePressurePlateJoin");
            teleportOnJoin =        MythicSkywars.get().getConfig().getBoolean("teleportToSpawnOnJoin");
            teleportOnWorldEnter =  MythicSkywars.get().getConfig().getBoolean("teleportToSpawnOnWorldEnter");
            strength =              MythicSkywars.get().getConfig().getInt("game.modifierLevel.strength");
            speed =                 MythicSkywars.get().getConfig().getInt("game.modifierLevel.speed");
            jump =                  MythicSkywars.get().getConfig().getInt("game.modifierLevel.jump");

            useSlimeWorldManager =      MythicSkywars.get().getConfig().getBoolean("slimeworldmanager.enable");
            slimeWorldManagerSource =   MythicSkywars.get().getConfig().getString("slimeworldmanager.source");

            usePlayerNames =            MythicSkywars.get().getConfig().getBoolean("teams.usePlayerNames");
            usePlayerGlassColors =      MythicSkywars.get().getConfig().getBoolean("teams.usePlayerGlassColors");
            useTeamNumberInMenu =       MythicSkywars.get().getConfig().getBoolean("teams.useTeamNumberInMenu");
            teamMaterial =              MythicSkywars.get().getConfig().getString("teams.teamCageMaterial");
            standardTeamMaterialByte =  MythicSkywars.get().getConfig().getInt("teams.standardTeamMaterialByte");
            useTeamMaterialBytes =      MythicSkywars.get().getConfig().getBoolean("teams.useTeamMaterialBytes");
            if (useTeamMaterialBytes) {
                if (teamMaterial == null || (!teamMaterial.equalsIgnoreCase("wool") && !teamMaterial.equalsIgnoreCase("stained_glass") && !teamMaterial.equalsIgnoreCase("banner"))) {
                    teamMaterial = "STAINED_GLASS";
                }
            }
            useSeparateCages =          MythicSkywars.get().getConfig().getBoolean("teams.useSeparateCages");
            changeTablistNames =        MythicSkywars.get().getConfig().getBoolean("teams.changeTablistNames");
            balanceUnselectedPlayersAcrossTeams = MythicSkywars.get().getConfig().getBoolean("teams.balanceUnselectedPlayersAcrossTeams", true);

            maxPartySize =              MythicSkywars.get().getConfig().getInt("parties.maxPartySize");
            partyEnabled =              MythicSkywars.get().getConfig().getBoolean("parties.enabled");
            usePartyAndFriends =        MythicSkywars.get().getConfig().getBoolean("parties.enablePartyAndFriendsSupport");
            lobbyWorlds =               MythicSkywars.get().getConfig().getStringList("parties.lobbyWorlds");

            loadTrappedChestsAsCenter = MythicSkywars.get().getConfig().getBoolean("chests.loadTrappedChestsAsCenter", true);
            maxChest =                  MythicSkywars.get().getConfig().getInt("chests.maxItemsChest");
            maxDoubleChest =            MythicSkywars.get().getConfig().getInt("chests.maxItemsDoubleChest");
            chestRefillEnabled =        MythicSkywars.get().getConfig().getBoolean("chests.refill.enabled", true);
            chestRefillIntervalSeconds = MythicSkywars.get().getConfig().getInt("chests.refill.intervalSeconds", 180);
            chestRefillKeepChestOpen =  MythicSkywars.get().getConfig().getBoolean("chests.refill.keepChestOpen", false);
            chestRefillShowHologram =   MythicSkywars.get().getConfig().getBoolean("chests.refill.showHologram", false);

            useHolograms =              MythicSkywars.get().getConfig().getBoolean("holograms.enabled");

            boolean requireSave = false;

            if (spawn != null) {
                if (lobbyWorlds == null) {
                    lobbyWorlds = Lists.newArrayList();
                } else {
                    String world = MythicSkywars.get().getConfig().getString("spawn").split(":")[0];
                    if (!lobbyWorlds.contains(world)) {
                        lobbyWorlds.add(world);

                        requireSave = true;
                    }
                }
            }

            randomOptionVoteEnabled =   MythicSkywars.get().getConfig().getBoolean("items.randomVoteEnabled");
            joinGameItemEnabled =       MythicSkywars.get().getConfig().getBoolean("items.joinGameItemEnabled");
            joinCompassRandomJoin =     MythicSkywars.get().getConfig().getBoolean("items.joinCompassRandomJoin", false);
            spectateGameItemEnabled =   MythicSkywars.get().getConfig().getBoolean("items.spectateGameItemEnabled");
            optionsGameItemEnabled =    MythicSkywars.get().getConfig().getBoolean("items.optionsItemEnabled");
            playAgainItemEnabled =      MythicSkywars.get().getConfig().getBoolean("items.playAgainItemEnabled", true);
            backToLobbyItemEnabled =    MythicSkywars.get().getConfig().getBoolean("items.backToLobbyItemEnabled", true);
            rejoinItemEnabled =         MythicSkywars.get().getConfig().getBoolean("items.rejoinItemEnabled", true);
            statsItemEnabled =          MythicSkywars.get().getConfig().getBoolean("items.statsItemEnabled", true);

            teamSelectPos =         MythicSkywars.get().getConfig().getInt("items.teamSelectPosition");
            kitvotepos =            MythicSkywars.get().getConfig().getInt("items.kitVotePosition");
            kitsEnabled =           MythicSkywars.get().getConfig().getBoolean("items.kitsEnabled");
            votepos =               MythicSkywars.get().getConfig().getInt("items.votingPosition");
            voteEnabled =           MythicSkywars.get().getConfig().getBoolean("items.voteEnabled");
            exitpos =               MythicSkywars.get().getConfig().getInt("items.exitPosition");
            playAgainPos =          MythicSkywars.get().getConfig().getInt("items.playAgainPosition", 8);
            backToLobbyPos =        MythicSkywars.get().getConfig().getInt("items.backToLobbyPosition", 8);
            rejoinItemPos =         MythicSkywars.get().getConfig().getInt("items.rejoinItemPosition", 1);
            statsItemPos =          MythicSkywars.get().getConfig().getInt("items.statsItemPosition", 6);
            rejoinEnabled =         MythicSkywars.get().getConfig().getBoolean("rejoin.enabled", true);
            rejoinWindowSeconds =   MythicSkywars.get().getConfig().getInt("rejoin.windowSeconds", 180);
            chestvotepos =          MythicSkywars.get().getConfig().getInt("items.chestVotePosition");
            chestVoteEnabled =      MythicSkywars.get().getConfig().getBoolean("items.chestVoteEnabled");
            healthvotepos =         MythicSkywars.get().getConfig().getInt("items.healthVotePosition");
            healthVoteEnabled =     MythicSkywars.get().getConfig().getBoolean("items.healthVoteEnabled");
            timevotepos =           MythicSkywars.get().getConfig().getInt("items.timeVotePosition");
            timeVoteEnabled =       MythicSkywars.get().getConfig().getBoolean("items.timeVoteEnabled");
            weathervotepos =        MythicSkywars.get().getConfig().getInt("items.weatherVotePosition");
            weatherVoteEnabled =    MythicSkywars.get().getConfig().getBoolean("items.weatherVoteEnabled");
            modifiervotepos =       MythicSkywars.get().getConfig().getInt("items.modifierVotePosition");
            modifierVoteEnabled =   MythicSkywars.get().getConfig().getBoolean("items.modifierVoteEnabled");
            particleselectslot =    MythicSkywars.get().getConfig().getInt("items.particleselectslot");
            projectileselectslot =  MythicSkywars.get().getConfig().getInt("items.projectileselectslot");
            killsoundselectslot =   MythicSkywars.get().getConfig().getInt("items.killsoundselectslot");
            winsoundselectslot =    MythicSkywars.get().getConfig().getInt("items.winsoundselectslot");
            glassselectslot =       MythicSkywars.get().getConfig().getInt("items.glassselectslot");
            tauntselectslot =       MythicSkywars.get().getConfig().getInt("items.tauntselectslot");

            cooldown =              MythicSkywars.get().getConfig().getInt("tauntCooldown");

            randPos =               MythicSkywars.get().getConfig().getInt("kit.randPos");
            kitMenuSize =           MythicSkywars.get().getConfig().getInt("kit.menuSize");
            noKitPos =              MythicSkywars.get().getConfig().getInt("kit.noKitPos");
            randMat =               MythicSkywars.get().getConfig().getString("kit.randItem");
            noKitMat =              MythicSkywars.get().getConfig().getString("kit.noKitItem");

            particlesEnabled =      MythicSkywars.get().getConfig().getBoolean("particles.enabled");
            ticksPerUpdate =        MythicSkywars.get().getConfig().getInt("particles.ticksperupdate");

            spectateMenuEnabled =   MythicSkywars.get().getConfig().getBoolean("enabledMenus.spectate");
            spectateSlot =          MythicSkywars.get().getConfig().getInt("enabledMenus.spectateSlot");
            joinEnabled =           MythicSkywars.get().getConfig().getBoolean("enabledMenus.join");
            joinSlot =              MythicSkywars.get().getConfig().getInt("enabledMenus.joinSlot");
            singleSlot =            MythicSkywars.get().getConfig().getInt("items.singleSlot");
            teamSlot =              MythicSkywars.get().getConfig().getInt("items.teamSlot");
            optionsEnabled =        MythicSkywars.get().getConfig().getBoolean("enabledMenus.options");
            optionsSlot =           MythicSkywars.get().getConfig().getInt("enabledMenus.optionsSlot", 7);
            glassEnabled =          MythicSkywars.get().getConfig().getBoolean("enabledMenus.glass");
            particleEnabled =       MythicSkywars.get().getConfig().getBoolean("enabledMenus.particle");
            projectEnabled =        MythicSkywars.get().getConfig().getBoolean("enabledMenus.projectile");
            killsoundEnabled =      MythicSkywars.get().getConfig().getBoolean("enabledMenus.killsound");
            winsoundEnabled =       MythicSkywars.get().getConfig().getBoolean("enabledMenus.winsound");
            tauntsMenuEnabled =     MythicSkywars.get().getConfig().getBoolean("enabledMenus.taunts");

            soulWellEnabled = MythicSkywars.get().getConfig().getBoolean("soulwell.enabled", true);
            soulWellSoulsPerKill = MythicSkywars.get().getConfig().getInt("soulwell.souls-per-kill", 1);
            soulWellSoulsPerWin = MythicSkywars.get().getConfig().getInt("soulwell.souls-per-win", 3);
            soulWellSoulsPerSpin = MythicSkywars.get().getConfig().getInt("soulwell.souls-per-spin", 10);
            soulWellMaxSpinsAtOnce = MythicSkywars.get().getConfig().getInt("soulwell.max-spins-at-once", 5);
            soulWellXezbethPermission = MythicSkywars.get().getConfig().getString("soulwell.xezbeth-permission", "sw.soulwell.xezbethluck");
            soulWellAnimTicksPerFrame = MythicSkywars.get().getConfig().getInt("soulwell.animation-ticks-per-frame", 2);
            soulWellAnimFrames = MythicSkywars.get().getConfig().getInt("soulwell.animation-frames", 24);
            soulWellOptionsMenuSlot = MythicSkywars.get().getConfig().getInt("soulwell.options-menu-slot", 22);
            soulWellWeightCoins = MythicSkywars.get().getConfig().getInt("soulwell.rewards.coins.weight", 40);
            soulWellWeightXp = MythicSkywars.get().getConfig().getInt("soulwell.rewards.xp.weight", 30);
            soulWellWeightCage = MythicSkywars.get().getConfig().getInt("soulwell.rewards.cage.weight", 15);
            soulWellWeightKit = MythicSkywars.get().getConfig().getInt("soulwell.rewards.kit.weight", 10);
            soulWellWeightPerk = MythicSkywars.get().getConfig().getInt("soulwell.rewards.perk.weight", 5);
            soulWellWeightCommand = MythicSkywars.get().getConfig().getInt("soulwell.rewards.command.weight", 0);
            soulWellWeightGamePerk = MythicSkywars.get().getConfig().getInt("soulwell.rewards.gameperk.weight", 4);
            soulWellCoinsMin = MythicSkywars.get().getConfig().getInt("soulwell.rewards.coins.min", 5);
            soulWellCoinsMax = MythicSkywars.get().getConfig().getInt("soulwell.rewards.coins.max", 30);
            soulWellXpMin = MythicSkywars.get().getConfig().getInt("soulwell.rewards.xp.min", 5);
            soulWellXpMax = MythicSkywars.get().getConfig().getInt("soulwell.rewards.xp.max", 20);
            soulWellKitAllowList = MythicSkywars.get().getConfig().getStringList("soulwell.rewards.kit.allow-list");
            soulWellKitDenyList = MythicSkywars.get().getConfig().getStringList("soulwell.rewards.kit.deny-list");
            soulWellPerkAllowList = MythicSkywars.get().getConfig().getStringList("soulwell.rewards.perk.allow-list");
            soulWellPerkDenyList = MythicSkywars.get().getConfig().getStringList("soulwell.rewards.perk.deny-list");
            soulWellCageAllowList = MythicSkywars.get().getConfig().getStringList("soulwell.rewards.cage.allow-list");
            soulWellCageDenyList = MythicSkywars.get().getConfig().getStringList("soulwell.rewards.cage.deny-list");
            soulWellCommandRewards = MythicSkywars.get().getConfig().getStringList("soulwell.rewards.command.commands");
            soulWellWeightKillSound = MythicSkywars.get().getConfig().getInt("soulwell.rewards.killsound.weight", 3);
            soulWellWeightWinSound = MythicSkywars.get().getConfig().getInt("soulwell.rewards.winsound.weight", 3);
            soulWellWeightProjectile = MythicSkywars.get().getConfig().getInt("soulwell.rewards.projectile.weight", 3);
            soulWellWeightTaunt = MythicSkywars.get().getConfig().getInt("soulwell.rewards.taunt.weight", 3);
            soulWellKillSoundAllowList = MythicSkywars.get().getConfig().getStringList("soulwell.rewards.killsound.allow-list");
            soulWellKillSoundDenyList = MythicSkywars.get().getConfig().getStringList("soulwell.rewards.killsound.deny-list");
            soulWellWinSoundAllowList = MythicSkywars.get().getConfig().getStringList("soulwell.rewards.winsound.allow-list");
            soulWellWinSoundDenyList = MythicSkywars.get().getConfig().getStringList("soulwell.rewards.winsound.deny-list");
            soulWellProjectileAllowList = MythicSkywars.get().getConfig().getStringList("soulwell.rewards.projectile.allow-list");
            soulWellProjectileDenyList = MythicSkywars.get().getConfig().getStringList("soulwell.rewards.projectile.deny-list");
            soulWellTauntAllowList = MythicSkywars.get().getConfig().getStringList("soulwell.rewards.taunt.allow-list");
            soulWellTauntDenyList = MythicSkywars.get().getConfig().getStringList("soulwell.rewards.taunt.deny-list");

            playSounds =            MythicSkywars.get().getConfig().getBoolean("sounds.enabled");
            countdown =             MythicSkywars.get().getConfig().getString("sounds.countdown");
            joinSound =             MythicSkywars.get().getConfig().getString("sounds.join");
            leaveSound =            MythicSkywars.get().getConfig().getString("sounds.leave");
            openJoinMenu =          MythicSkywars.get().getConfig().getString("sounds.openJoinMenu");
            openSpectateMenu =      MythicSkywars.get().getConfig().getString("sounds.openSpectateMenu");
            openOptionsMenu =       MythicSkywars.get().getConfig().getString("sounds.openOptionsMenu");
            openGlassMenu =         MythicSkywars.get().getConfig().getString("sounds.openGlassMenu");
            openWinSoundMenu =      MythicSkywars.get().getConfig().getString("sounds.openWinSoundMenu");
            openKillSoundMenu =     MythicSkywars.get().getConfig().getString("sounds.openKillSoundMenu");
            openParticleMenu =      MythicSkywars.get().getConfig().getString("sounds.openParticleMenu");
            openProjectileMenu =    MythicSkywars.get().getConfig().getString("sounds.openProjectileMenu");
            openTauntMenu =         MythicSkywars.get().getConfig().getString("sounds.openTauntMenu");
            openKitMenu =           MythicSkywars.get().getConfig().getString("sounds.openKitMenu");
            openChestMenu =         MythicSkywars.get().getConfig().getString("sounds.openChestMenu");
            openHealthMenu =        MythicSkywars.get().getConfig().getString("sounds.openHealthMenu");
            openTimeMenu =          MythicSkywars.get().getConfig().getString("sounds.openTimeMenu");
            openWeatherMenu =       MythicSkywars.get().getConfig().getString("sounds.openWeatherMenu");
            openModifierMenu =      MythicSkywars.get().getConfig().getString("sounds.openModifierMenu");
            confirmSelection =      MythicSkywars.get().getConfig().getString("sounds.confirmSelectionSound");
            errorSound =            MythicSkywars.get().getConfig().getString("sounds.errorSound");
            teamJoinSound = MythicSkywars.get().getConfig().getString("sounds.teamJoinSound");

            enabledCommands =       MythicSkywars.get().getConfig().getStringList("disable-commands.exceptions");
            disableCommands =       MythicSkywars.get().getConfig().getBoolean("disable-commands.enabled");

            disableCommandsSpectate = MythicSkywars.get().getConfig().getBoolean("disable-commands-spectate.enabled");
            enabledCommandsSpectate = MythicSkywars.get().getConfig().getStringList("disable-commands-spectate.exceptions");

            useExternalChat =   MythicSkywars.get().getConfig().getBoolean("chat.externalChat.useExternalChat");
            addPrefix =         MythicSkywars.get().getConfig().getBoolean("chat.externalChat.addPrefix");
            enableFormatter =   MythicSkywars.get().getConfig().getBoolean("chat.enableFormatter");
            limitGameChat =     MythicSkywars.get().getConfig().getBoolean("chat.limitGameChatToGame");
            limitSpecChat =     MythicSkywars.get().getConfig().getBoolean("chat.limitSpecChatToSpec");
            limitLobbyChat =    MythicSkywars.get().getConfig().getBoolean("chat.limitLobbyChatToLobby");
            useTeamChat =       MythicSkywars.get().getConfig().getBoolean("chat.useTeamChat");

            for (int i = 0; i < itemNames.size(); i++) {
                String name = itemNames.get(i);
                String def;
                if (MythicSkywars.getNMS().getVersion() < 9) {
                    def = defItems8.get(i);
                } else if (MythicSkywars.getNMS().getVersion() > 8 && MythicSkywars.getNMS().getVersion() < 13) {
                    def = defItems12.get(i);
                } else {
                    def = defItems13.get(i);
                }
                addMaterial(name, MythicSkywars.get().getConfig().getString("items." + name), def);
            }

            for (int i = 0; i < signItems.size(); i++) {
                String name = signItems.get(i);
                String def;
                if (MythicSkywars.getNMS().getVersion() < 13) {
                    def = signDef8.get(i);
                } else {
                    def = signDef13.get(i);
                }

                addMaterial(name, MythicSkywars.get().getConfig().getString("signs." + name), def);
            }

            LuckyBlockHook.reloadLuckyBlocksYaml();

            if (requireSave) {
                save();
            }
        }
        loading = false;
    }

    private void debugTesting() {
        if (debugEnabled()) {
            Logger log = MythicSkywars.get().getLogger();
            //log.info("[DEBUG] spawn.x null? " + String.join(", ", MythicSkywars.get().getServer().getWorlds()));
            //if (t3) return;
            boolean t1 = getSpawn() == null;
            log.info("[DEBUG] spawn null? " + t1);
            if (t1) return;
            boolean t2 = getSpawn().getWorld() == null;
            log.info("[DEBUG] spawn.world null? " + t2);
            if (t2) return;
        }
    }

    private void addMaterial(String key, String mat, String def) {
        int data = -1;
        String matWithData = "";
        String[] matParts = mat.split(":");
        if (matParts.length == 2) {
            matWithData = matParts[0];
            data = Integer.parseInt(matParts[1]);
        }
        Material material;
        if (data != -1) {
            material = Material.matchMaterial(matWithData);
        } else {
            material = Material.matchMaterial(mat);
        }
        if (material == null) {
            materials.put(key, def);
        } else {
            materials.put(key, mat);
        }
    }

    public void save() {
        MythicSkywars.get().getConfig().set("debugMode", debug);
        MythicSkywars.get().getConfig().set("gameServers", gameServers);
        MythicSkywars.get().getConfig().set("isLobbyServer", isLobbyServer);

        MythicSkywars.get().getConfig().set("updater.checkForBetaVersions", checkForBetaVersion);

        if (spawn != null) {
            MythicSkywars.get().getConfig().set("spawn", Util.get().locationToString(spawn));
        }

        MythicSkywars.get().getConfig().set("economyEnabled", economyEnabled);
        MythicSkywars.get().getConfig().set("bungeeMode", bungeeMode);
        MythicSkywars.get().getConfig().set("bungeeModeType", bungeeModeType);
        MythicSkywars.get().getConfig().set("bungeeRandomMapPickOnStart", bungeeRandomMapPickOnStart);
        MythicSkywars.get().getConfig().set("bungeeLobby", bungeeLobby);
        MythicSkywars.get().getConfig().set("gameEndCommands", gameEndCommands);

        MythicSkywars.get().getConfig().set("resourcepack", resourcePack);
        MythicSkywars.get().getConfig().set("promptForResourcePackOnJoin", promptResource);
        MythicSkywars.get().getConfig().set("clearInventoryOnLobbyJoin", clearInventoryOnLobbyJoin);
        MythicSkywars.get().getConfig().set("timeFormat", timeFormat);

        MythicSkywars.get().getConfig().set("lobbyBoardEnabled", lobbyBoardEnabled);
        MythicSkywars.get().getConfig().set("lobbyBoardEnabled", lobbyBoardEnabled);
        MythicSkywars.get().getConfig().set("enabledLobbyGuard", protectlobby);
        MythicSkywars.get().getConfig().set("lobby.forceSunnyWhenProtected", lobbyForceSunny);
        MythicSkywars.get().getConfig().set("lobby.forceDayWhenProtected", lobbyForceDay);
        MythicSkywars.get().getConfig().set("lobby.hideJoinQuitMessages", lobbyHideJoinQuitMessages);
        MythicSkywars.get().getConfig().set("lobby.hideDeathMessages", lobbyHideDeathMessages);
        MythicSkywars.get().getConfig().set("leaderboards.length", leaderSize);
        MythicSkywars.get().getConfig().set("leaderboards.signsEnabled", leaderSignsEnabled);
        MythicSkywars.get().getConfig().set("leaderboards.headsEnabled", leaderHeadsEnabled);
        MythicSkywars.get().getConfig().set("leaderboards.winsLeaderboardEnabled", winsEnabled);
        MythicSkywars.get().getConfig().set("leaderboards.lossesLeaderboardEnabled", lossesEnabled);
        MythicSkywars.get().getConfig().set("leaderboards.killsLeaderboardEnabled", killsEnabled);
        MythicSkywars.get().getConfig().set("leaderboards.deathsLeaderboardEnabled", deathsEnabled);
        MythicSkywars.get().getConfig().set("leaderboards.xpLeaderboardEnabled", xpEnabled);
        MythicSkywars.get().getConfig().set("leaderboards.leaderboardUpdateInterval", leaderboardUpdateInterval);

        MythicSkywars.get().getConfig().set("game.displayTimerOnLevelbar", displayTimerOnLevelbar);
        MythicSkywars.get().getConfig().set("game.win.clearInventory", clearInventoryOnWin);
        MythicSkywars.get().getConfig().set("game.win.enableFlight", enableFlightOnWin);
        MythicSkywars.get().getConfig().set("game.kickOnWorldTeleport", kickOnWorldTeleport);
        MythicSkywars.get().getConfig().set("game.enableQuickDeath", enableQuickDeath);
        MythicSkywars.get().getConfig().set("game.quickDeathY", quickDeathY);
        MythicSkywars.get().getConfig().set("game.enablePVPTimer", enablePVPTimer);
        MythicSkywars.get().getConfig().set("game.PVPTimerTime", PVPTimerTime);
        MythicSkywars.get().getConfig().set("game.enableWinMessage", enableWinMessage);
        MythicSkywars.get().getConfig().set("game.enableLookDirection", lookDirectionEnabled);
        MythicSkywars.get().getConfig().set("game.worldBorder.enabled", borderEnabled);
        MythicSkywars.get().getConfig().set("game.worldBorder.borderSize", borderSize);
        MythicSkywars.get().getConfig().set("game.showHealth", showHealth);
        MythicSkywars.get().getConfig().set("game.ecoForWin", winnerEco);
        MythicSkywars.get().getConfig().set("game.ecoForKill", killerEco);
        MythicSkywars.get().getConfig().set("game.snowballDamage", snowballDamage);
        MythicSkywars.get().getConfig().set("game.eggDamage", eggDamage);
        MythicSkywars.get().getConfig().set("game.winCommands", winCommands);
        MythicSkywars.get().getConfig().set("game.killCommands", killCommands);
        MythicSkywars.get().getConfig().set("titles.enabled", titlesEnabled);
        MythicSkywars.get().getConfig().set("game.waitTimer", waitTimer);
        MythicSkywars.get().getConfig().set("game.resetTimerOnJoin", resetTimerOnJoin);
        MythicSkywars.get().getConfig().set("game.timeAfterMatch", timeAfterMatch);
        MythicSkywars.get().getConfig().set("fireworks.per5Ticks", fireworksPer5Tick);
        MythicSkywars.get().getConfig().set("fireworks.enabled", fireworksEnabled);
        MythicSkywars.get().getConfig().set("game.spectateEnabled", spectateEnabled);
        MythicSkywars.get().getConfig().set("game.maxMapSize", maxMapSize);
        MythicSkywars.get().getConfig().set("game.tauntsEnabled", tauntsEnabled);
        MythicSkywars.get().getConfig().set("enablePressurePlateJoin", pressurePlate);
        MythicSkywars.get().getConfig().set("teleportToSpawnOnJoin", teleportOnJoin);
        MythicSkywars.get().getConfig().set("teleportToSpawnOnWorldEnter", teleportOnWorldEnter);
        MythicSkywars.get().getConfig().set("game.kitVotingEnabled", kitVotingEnabled);
        MythicSkywars.get().getConfig().set("game.modifierLevel.strength", strength);
        MythicSkywars.get().getConfig().set("game.modifierLevel.speed", speed);
        MythicSkywars.get().getConfig().set("game.modifierLevel.jump", jump);

        MythicSkywars.get().getConfig().set("slimeworldmanager.enable", useSlimeWorldManager);
        MythicSkywars.get().getConfig().set("slimeworldmanager.source", slimeWorldManagerSource);

        MythicSkywars.get().getConfig().set("teams.usePlayerNames", usePlayerNames);
        MythicSkywars.get().getConfig().set("teams.usePlayerGlassColors", usePlayerGlassColors);
        MythicSkywars.get().getConfig().set("teams.useTeamNumberInMenu", useTeamNumberInMenu);
        MythicSkywars.get().getConfig().set("teams.teamCageMaterial", teamMaterial.toUpperCase());
        MythicSkywars.get().getConfig().set("teams.standardTeamMaterialByte", standardTeamMaterialByte);
        MythicSkywars.get().getConfig().set("teams.useTeamMaterialBytes", useTeamMaterialBytes);
        MythicSkywars.get().getConfig().set("teams.useSeparateCages", useSeparateCages);
        MythicSkywars.get().getConfig().set("teams.changeTablistNames", changeTablistNames);
        MythicSkywars.get().getConfig().set("teams.balanceUnselectedPlayersAcrossTeams", balanceUnselectedPlayersAcrossTeams);

        MythicSkywars.get().getConfig().set("parties.maxPartySize", maxPartySize);
        MythicSkywars.get().getConfig().set("parties.enabled", partyEnabled);
        MythicSkywars.get().getConfig().set("parties.enablePartyAndFriendsSupport", usePartyAndFriends);
        MythicSkywars.get().getConfig().set("parties.lobbyWorlds", lobbyWorlds);

        MythicSkywars.get().getConfig().set("chests.loadTrappedChestsAsCenter", loadTrappedChestsAsCenter);
        MythicSkywars.get().getConfig().set("chests.maxItemsChest", maxChest);
        MythicSkywars.get().getConfig().set("chests.maxItemsDoubleChest", maxDoubleChest);
        MythicSkywars.get().getConfig().set("chests.refill.enabled", chestRefillEnabled);
        MythicSkywars.get().getConfig().set("chests.refill.intervalSeconds", chestRefillIntervalSeconds);
        MythicSkywars.get().getConfig().set("chests.refill.keepChestOpen", chestRefillKeepChestOpen);
        MythicSkywars.get().getConfig().set("chests.refill.showHologram", chestRefillShowHologram);


        MythicSkywars.get().getConfig().set("holograms.enabled", useHolograms);

        MythicSkywars.get().getConfig().set("items.randomVoteEnabled", randomOptionVoteEnabled);
        MythicSkywars.get().getConfig().set("items.joinGameItemEnabled", joinGameItemEnabled);
        MythicSkywars.get().getConfig().set("items.joinCompassRandomJoin", joinCompassRandomJoin);
        MythicSkywars.get().getConfig().set("items.spectateGameItemEnabled", spectateGameItemEnabled);
        MythicSkywars.get().getConfig().set("items.optionsItemEnabled", optionsGameItemEnabled);
        MythicSkywars.get().getConfig().set("items.playAgainItemEnabled", playAgainItemEnabled);
        MythicSkywars.get().getConfig().set("items.backToLobbyItemEnabled", backToLobbyItemEnabled);
        MythicSkywars.get().getConfig().set("items.rejoinItemEnabled", rejoinItemEnabled);
        MythicSkywars.get().getConfig().set("items.statsItemEnabled", statsItemEnabled);

        MythicSkywars.get().getConfig().set("items.teamSelectPosition", teamSelectPos);
        MythicSkywars.get().getConfig().set("items.kitVotePosition", kitvotepos);
        MythicSkywars.get().getConfig().set("items.kitsEnabled", kitsEnabled);
        MythicSkywars.get().getConfig().set("items.votingPosition", votepos);
        MythicSkywars.get().getConfig().set("items.voteEnabled", voteEnabled);
        MythicSkywars.get().getConfig().set("items.exitPosition", exitpos);
        MythicSkywars.get().getConfig().set("items.playAgainPosition", playAgainPos);
        MythicSkywars.get().getConfig().set("items.backToLobbyPosition", backToLobbyPos);
        MythicSkywars.get().getConfig().set("items.rejoinItemPosition", rejoinItemPos);
        MythicSkywars.get().getConfig().set("items.statsItemPosition", statsItemPos);
        MythicSkywars.get().getConfig().set("rejoin.enabled", rejoinEnabled);
        MythicSkywars.get().getConfig().set("rejoin.windowSeconds", rejoinWindowSeconds);
        MythicSkywars.get().getConfig().set("items.chestVotePosition", chestvotepos);
        MythicSkywars.get().getConfig().set("items.chestVoteEnabled", chestVoteEnabled);
        MythicSkywars.get().getConfig().set("items.healthVotePosition", healthvotepos);
        MythicSkywars.get().getConfig().set("items.healthVoteEnabled", healthVoteEnabled);
        MythicSkywars.get().getConfig().set("items.timeVotePosition", timevotepos);
        MythicSkywars.get().getConfig().set("items.timeVoteEnabled", timeVoteEnabled);
        MythicSkywars.get().getConfig().set("items.weatherVotePosition", weathervotepos);
        MythicSkywars.get().getConfig().set("items.weatherVoteEnabled", weatherVoteEnabled);
        MythicSkywars.get().getConfig().set("items.modifierVotePosition", modifiervotepos);
        MythicSkywars.get().getConfig().set("items.modifierVoteEnabled", modifierVoteEnabled);
        MythicSkywars.get().getConfig().set("items.particleselectslot", particleselectslot);
        MythicSkywars.get().getConfig().set("items.projectileselectslot", projectileselectslot);
        MythicSkywars.get().getConfig().set("items.killsoundselectslot", killsoundselectslot);
        MythicSkywars.get().getConfig().set("items.winsoundselectslot", winsoundselectslot);
        MythicSkywars.get().getConfig().set("items.glassselectslot", glassselectslot);
        MythicSkywars.get().getConfig().set("items.tauntselectslot", tauntselectslot);

        MythicSkywars.get().getConfig().set("tauntCooldown", cooldown);

        MythicSkywars.get().getConfig().set("kit.randPos", randPos);
        MythicSkywars.get().getConfig().set("kit.menuSize", kitMenuSize);
        MythicSkywars.get().getConfig().set("kit.noKitPos", noKitPos);
        MythicSkywars.get().getConfig().set("kit.randItem", randMat);
        MythicSkywars.get().getConfig().set("kit.noKitItem", noKitMat);

        MythicSkywars.get().getConfig().set("particles.enabled", particlesEnabled);
        MythicSkywars.get().getConfig().set("particles.ticksperupdate", ticksPerUpdate);

        MythicSkywars.get().getConfig().set("enabledMenus.spectate", spectateMenuEnabled);
        MythicSkywars.get().getConfig().set("enabledMenus.spectateSlot", spectateSlot);
        MythicSkywars.get().getConfig().set("enabledMenus.join", joinEnabled);
        MythicSkywars.get().getConfig().set("enabledMenus.joinSlot", joinSlot);
        MythicSkywars.get().getConfig().set("items.singleSlot", singleSlot);
        MythicSkywars.get().getConfig().set("items.teamSlot", teamSlot);
        MythicSkywars.get().getConfig().set("enabledMenus.options", optionsEnabled);
        MythicSkywars.get().getConfig().set("enabledMenus.optionsSlot", optionsSlot);
        MythicSkywars.get().getConfig().set("enabledMenus.glass", glassEnabled);
        MythicSkywars.get().getConfig().set("enabledMenus.particle", particlesEnabled);
        MythicSkywars.get().getConfig().set("enabledMenus.projectile", projectEnabled);
        MythicSkywars.get().getConfig().set("enabledMenus.killsound", killsoundEnabled);
        MythicSkywars.get().getConfig().set("enabledMenus.winsound", winsoundEnabled);
        MythicSkywars.get().getConfig().set("enabledMenus.taunts", tauntsMenuEnabled);

        MythicSkywars.get().getConfig().set("sounds.enabled", playSounds);
        MythicSkywars.get().getConfig().set("sounds.countdown", countdown);
        MythicSkywars.get().getConfig().set("sounds.join", joinSound);
        MythicSkywars.get().getConfig().set("sounds.leave", leaveSound);
        MythicSkywars.get().getConfig().set("sounds.openJoinMenu", openJoinMenu);
        MythicSkywars.get().getConfig().set("sounds.openSpectateMenu", openSpectateMenu);
        MythicSkywars.get().getConfig().set("sounds.openOptionsMenu", openOptionsMenu);
        MythicSkywars.get().getConfig().set("sounds.openGlassMenu", openGlassMenu);
        MythicSkywars.get().getConfig().set("sounds.openWinSoundMenu", openWinSoundMenu);
        MythicSkywars.get().getConfig().set("sounds.openKillSoundMenu", openKillSoundMenu);
        MythicSkywars.get().getConfig().set("sounds.openParticleMenu", openParticleMenu);
        MythicSkywars.get().getConfig().set("sounds.openProjectileMenu", openProjectileMenu);
        MythicSkywars.get().getConfig().set("sounds.openTauntMenu", openTauntMenu);
        MythicSkywars.get().getConfig().set("sounds.openKitMenu", openKitMenu);
        MythicSkywars.get().getConfig().set("sounds.openChestMenu", openChestMenu);
        MythicSkywars.get().getConfig().set("sounds.openHealthMenu", openHealthMenu);
        MythicSkywars.get().getConfig().set("sounds.openTimeMenu", openTimeMenu);
        MythicSkywars.get().getConfig().set("sounds.openWeatherMenu", openWeatherMenu);
        MythicSkywars.get().getConfig().set("sounds.openModifierMenu", openModifierMenu);
        MythicSkywars.get().getConfig().set("sounds.confirmSelectionSound", confirmSelection);
        MythicSkywars.get().getConfig().set("sounds.errorSound", errorSound);
        MythicSkywars.get().getConfig().set("sounds.teamJoinSound", teamJoinSound);

        MythicSkywars.get().getConfig().set("disable-commands.exceptions", enabledCommands);
        MythicSkywars.get().getConfig().set("disable-commands.enabled", disableCommands);

        MythicSkywars.get().getConfig().set("disable-commands-spectate.enabled", disableCommandsSpectate);
        MythicSkywars.get().getConfig().set("disable-commands-spectate.exceptions", enabledCommandsSpectate);

        MythicSkywars.get().getConfig().getBoolean("chat.externalChat.useExternalChat", useExternalChat);
        MythicSkywars.get().getConfig().getBoolean("chat.externalChat.addPrefix", addPrefix);
        MythicSkywars.get().getConfig().getBoolean("chat.enableFormatter", enableFormatter);
        MythicSkywars.get().getConfig().getBoolean("chat.limitGameChatToGame", limitGameChat);
        MythicSkywars.get().getConfig().getBoolean("chat.limitSpecChatToSpec", limitSpecChat);
        MythicSkywars.get().getConfig().getBoolean("chat.limitLobbyChatToLobby", limitLobbyChat);
        MythicSkywars.get().getConfig().getBoolean("chat.useTeamChat", useTeamChat);

        for (String name : itemNames) {
            MythicSkywars.get().getConfig().set("items." + name, materials.get(name));
        }

        MythicSkywars.get().saveConfig();
    }

    public List<String> getEnabledCommands() {
        return enabledCommands;
    }

    public List<String> getEnabledCommandsSpectate() {
        return enabledCommandsSpectate;
    }

    public int getUpdateTime() {
        return leaderboardUpdateInterval;
    }

    public boolean disableCommands() {
        return disableCommands;
    }

    public boolean disableCommandsSpectate() {
        return disableCommandsSpectate;
    }

    public int getWaitTimer() {
        return waitTimer;
    }

    public int getTimeAfterMatch() {
        return timeAfterMatch;
    }

    public int getFireWorksPer5Tick() {
        return fireworksPer5Tick;
    }

    public boolean fireworksEnabled() {
        return fireworksEnabled;
    }

    public boolean titlesEnabled() {
        return titlesEnabled;
    }

    public boolean particlesEnabled() {
        return particlesEnabled;
    }

    public boolean soundsEnabled() {
        return playSounds;
    }

    public boolean spectateEnable() {
        return spectateEnabled;
    }

    public boolean debugEnabled() {
        return debug;
    }

    public int getMaxMapSize() {
        return maxMapSize;
    }

    public Location getSpawn() {
        return spawn;
    }

    public void setSpawn(Location location) {
        this.spawn = location;
        if (!lobbyWorlds.contains(spawn.getWorld().getName())) {
            lobbyWorlds.add(spawn.getWorld().getName());
            save();
        }
    }

    public boolean bungeeMode() {
        return isBungeeProxyMode();
    }

    public boolean isBungeeEnabled() {
        return bungeeMode && MythicSkywars.get().isEnabled();
    }

    public boolean isBungeeProxyMode() {
        return isBungeeEnabled() && !"server".equalsIgnoreCase(bungeeModeType);
    }

    public boolean isBungeeServerMode() {
        return isBungeeEnabled() && "server".equalsIgnoreCase(bungeeModeType);
    }

    public boolean getBungeeRandomMapPickOnStart() {
        return bungeeRandomMapPickOnStart;
    }

    public String getBungeeLobby() {
        return bungeeLobby;
    }

    public int getCooldown() {
        return cooldown;
    }

    public String getMaterial(String string) {
        return materials.get(string);
    }

    public boolean pressurePlateJoin() {
        return pressurePlate;
    }

    public boolean resetTimerOnJoin() {
        return resetTimerOnJoin;
    }

    public boolean enableWinMessage() {
        return enableWinMessage;
    }

    public String getCountdownSound() {
        return countdown;
    }

    public List<String> getWinCommands() {
        return winCommands;
    }

    public List<String> getKillCommands() {
        return killCommands;
    }

    public boolean glassMenuEnabled() {
        return glassEnabled;
    }

    public boolean particleMenuEnabled() {
        return particleEnabled;
    }

    public boolean projectileMenuEnabled() {
        return projectEnabled;
    }

    public boolean killsoundMenuEnabled() {
        return killsoundEnabled;
    }

    public boolean winsoundMenuEnabled() {
        return winsoundEnabled;
    }

    public boolean tauntsMenuEnabled() {
        return tauntsMenuEnabled;
    }

    public boolean optionsMenuEnabled() {
        return optionsEnabled;
    }

    public int getOptionsSlot() {
        return optionsSlot;
    }

    public boolean joinMenuEnabled() {
        return joinEnabled;
    }

    public int getJoinSlot() {
        return joinSlot;
    }

    public boolean spectateMenuEnabled() {
        return spectateMenuEnabled;
    }

    public int getSpectateSlot() {
        return spectateSlot;
    }

    public boolean teleportOnJoin() {
        return teleportOnJoin;
    }

    public int getRandPos() {
        return randPos;
    }

    public Material getRandMat() {
        Material mat = Material.matchMaterial(randMat);
        return mat != null ? mat : Material.CHEST;
    }

    public int getNoKitPos() {
        return noKitPos;
    }

    public Material getNoKitMat() {
        Material mat = Material.matchMaterial(noKitMat);
        return mat != null ? mat : Material.BARRIER;
    }

    public boolean promptForResource() {
        return promptResource;
    }

    public String getResourceLink() {
        return resourcePack;
    }

    public int getLeaderSize() {
        return leaderSize;
    }

    public boolean leaderSignsEnabled() {
        return leaderSignsEnabled;
    }

    public boolean leaderHeadsEnabled() {
        return leaderHeadsEnabled;
    }

    public long getTicksPerUpdate() {
        return ticksPerUpdate;
    }

    public boolean kitVotingEnabled() {
        return kitVotingEnabled;
    }

    public int getKitVotePos() {
        return kitvotepos;
    }

    public boolean areKitsEnabled() {
        return kitsEnabled;
    }

    public int getChestVotePos() {
        return chestvotepos;
    }

    public boolean isChestVoteEnabled() {
        return chestVoteEnabled;
    }

    public int getHealthVotePos() {
        return healthvotepos;
    }

    public boolean isHealthVoteEnabled() {
        return healthVoteEnabled;
    }

    public int getTimeVotePos() {
        return timevotepos;
    }

    public boolean isTimeVoteEnabled() {
        return timeVoteEnabled;
    }

    public int getWeatherVotePos() {
        return weathervotepos;
    }

    public boolean isWeatherVoteEnabled() {
        return weatherVoteEnabled;
    }

    public int getModifierVotePos() {
        return modifiervotepos;
    }

    public boolean isModifierVoteEnabled() {
        return modifierVoteEnabled;
    }

    public boolean lobbyBoardEnabled() {
        return lobbyBoardEnabled;
    }

    public String getJoinSound() {
        return joinSound;
    }

    public String getLeaveSound() {
        return leaveSound;
    }

    public String getOpenOptionsMenuSound() {
        return openOptionsMenu;
    }

    public String getOpenJoinMenuSound() {
        return openJoinMenu;
    }

    public String getOpenSpectateMenuSound() {
        return openSpectateMenu;
    }

    public String getOpenParticleMenuSound() {
        return openParticleMenu;
    }

    public String getOpenProjectileMenuSound() {
        return openProjectileMenu;
    }

    public String getOpenKillSoundMenuSound() {
        return openKillSoundMenu;
    }

    public String getOpenWinSoundMenuSound() {
        return openWinSoundMenu;
    }

    public String getOpenGlassMenuSound() {
        return openGlassMenu;
    }

    public String getOpenTauntMenuSound() {
        return openTauntMenu;
    }

    public String getOpenKitMenuSound() {
        return openKitMenu;
    }

    public String getOpenChestMenuSound() {
        return openChestMenu;
    }

    public String getOpenTimeMenuSound() {
        return openTimeMenu;
    }

    public String getOpenWeatherMenuSound() {
        return openWeatherMenu;
    }

    public String getOpenModifierMenuSound() {
        return openModifierMenu;
    }

    public String getConfirmeSelctionSound() {
        return confirmSelection;
    }

    public String getErrorSound() {
        return errorSound;
    }

    public boolean isSoulWellEnabled() {
        return soulWellEnabled;
    }

    public int getSoulWellSoulsPerKill() {
        return soulWellSoulsPerKill;
    }

    public int getSoulWellSoulsPerWin() {
        return soulWellSoulsPerWin;
    }

    public int getSoulWellSoulsPerSpin() {
        return soulWellSoulsPerSpin;
    }

    public int getSoulWellMaxSpinsAtOnce() {
        return Math.max(1, soulWellMaxSpinsAtOnce);
    }

    public String getSoulWellXezbethPermission() {
        return soulWellXezbethPermission != null ? soulWellXezbethPermission : "sw.soulwell.xezbethluck";
    }

    public int getSoulWellAnimTicksPerFrame() {
        return Math.max(1, soulWellAnimTicksPerFrame);
    }

    public int getSoulWellAnimFrames() {
        return Math.max(4, soulWellAnimFrames);
    }

    public int getSoulWellOptionsMenuSlot() {
        return soulWellOptionsMenuSlot;
    }

    public int getSoulWellWeightCoins() {
        return Math.max(0, soulWellWeightCoins);
    }

    public int getSoulWellWeightXp() {
        return Math.max(0, soulWellWeightXp);
    }

    public int getSoulWellWeightCage() {
        return Math.max(0, soulWellWeightCage);
    }

    public int getSoulWellWeightKit() {
        return Math.max(0, soulWellWeightKit);
    }

    public int getSoulWellWeightPerk() {
        return Math.max(0, soulWellWeightPerk);
    }

    public int getSoulWellWeightCommand() {
        return Math.max(0, soulWellWeightCommand);
    }

    public int getSoulWellWeightGamePerk() {
        return Math.max(0, soulWellWeightGamePerk);
    }

    public int getSoulWellCoinsMin() {
        return soulWellCoinsMin;
    }

    public int getSoulWellCoinsMax() {
        return Math.max(soulWellCoinsMin, soulWellCoinsMax);
    }

    public int getSoulWellXpMin() {
        return soulWellXpMin;
    }

    public int getSoulWellXpMax() {
        return Math.max(soulWellXpMin, soulWellXpMax);
    }

    public List<String> getSoulWellKitAllowList() {
        return soulWellKitAllowList;
    }

    public List<String> getSoulWellKitDenyList() {
        return soulWellKitDenyList;
    }

    public List<String> getSoulWellPerkAllowList() {
        return soulWellPerkAllowList;
    }

    public List<String> getSoulWellPerkDenyList() {
        return soulWellPerkDenyList;
    }

    public List<String> getSoulWellCageAllowList() {
        return soulWellCageAllowList;
    }

    public List<String> getSoulWellCageDenyList() {
        return soulWellCageDenyList;
    }

    public List<String> getSoulWellCommandRewards() {
        return soulWellCommandRewards;
    }

    public int getSoulWellWeightKillSound() {
        return Math.max(0, soulWellWeightKillSound);
    }

    public int getSoulWellWeightWinSound() {
        return Math.max(0, soulWellWeightWinSound);
    }

    public int getSoulWellWeightProjectile() {
        return Math.max(0, soulWellWeightProjectile);
    }

    public int getSoulWellWeightTaunt() {
        return Math.max(0, soulWellWeightTaunt);
    }

    public List<String> getSoulWellKillSoundAllowList() {
        return soulWellKillSoundAllowList;
    }

    public List<String> getSoulWellKillSoundDenyList() {
        return soulWellKillSoundDenyList;
    }

    public List<String> getSoulWellWinSoundAllowList() {
        return soulWellWinSoundAllowList;
    }

    public List<String> getSoulWellWinSoundDenyList() {
        return soulWellWinSoundDenyList;
    }

    public List<String> getSoulWellProjectileAllowList() {
        return soulWellProjectileAllowList;
    }

    public List<String> getSoulWellProjectileDenyList() {
        return soulWellProjectileDenyList;
    }

    public List<String> getSoulWellTauntAllowList() {
        return soulWellTauntAllowList;
    }

    public List<String> getSoulWellTauntDenyList() {
        return soulWellTauntDenyList;
    }

    public boolean economyEnabled() {
        return economyEnabled;
    }

    public boolean protectLobby() {
        return protectlobby;
    }

    public boolean isLobbyForceSunny() {
        return lobbyForceSunny;
    }

    public boolean isLobbyForceDay() {
        return lobbyForceDay;
    }

    public boolean isLobbyHideJoinQuitMessages() {
        return lobbyHideJoinQuitMessages;
    }

    public boolean isLobbyHideDeathMessages() {
        return lobbyHideDeathMessages;
    }

    public boolean displayPlayerExeperience() {
        return displayPlayerExeperience;
    }

    public int maxPartySize() {
        return maxPartySize;
    }

    public boolean partyEnabled() {
        return partyEnabled;
    }

    public List<String> getLobbyWorlds() {
        return lobbyWorlds;
    }

    public boolean votingEnabled() {
        return voteEnabled;
    }

    public int getVotingPos() {
        return votepos;
    }

    public int getExitPos() {
        return exitpos;
    }

    public int getWinnerEco() {
        return winnerEco;
    }

    public int getKillerEco() {
        return killerEco;
    }

    public void setEconomyEnabled(boolean b) {
        economyEnabled = b;
    }

    public boolean hologramsEnabled() {
        return useHolograms;
    }

    public void setHologramsEnabled(boolean b) {
        useHolograms = b;
    }

    public boolean isTypeEnabled(LeaderType type) {
        switch (type) {
            case WINS:
                return winsEnabled;
            case LOSSES:
                return lossesEnabled;
            case KILLS:
                return killsEnabled;
            case DEATHS:
                return deathsEnabled;
            case XP:
                return xpEnabled;
            default:
                return false;
        }
    }

    public int getSpeed() {
        return speed;
    }

    public int getJump() {
        return jump;
    }

    public int getStrength() {
        return strength;
    }

    public boolean tauntsEnabled() {
        return tauntsEnabled;
    }

    public String getOpenHealthMenuSound() {
        return openHealthMenu;
    }

    public int getKitMenuSize() {
        return kitMenuSize;
    }

    public int getGlassSlot() {
        return glassselectslot;
    }

    public int getParticleSlot() {
        return particleselectslot;
    }

    public int getProjectileSlot() {
        return projectileselectslot;
    }

    public int getKillSoundSlot() {
        return killsoundselectslot;
    }

    public int getWinSoundSlot() {
        return winsoundselectslot;
    }

    public int getTauntSlot() {
        return tauntselectslot;
    }

    public double getSnowDamage() {
        return snowballDamage;
    }

    public double getEggDamage() {
        return eggDamage;
    }

    public int getMaxDoubleChest() {
        return maxDoubleChest;
    }

    public boolean getLoadTrappedChestsAsCenter() {
        return loadTrappedChestsAsCenter;
    }

    public int getMaxChest() {
        return maxChest;
    }

    public boolean isChestRefillEnabled() {
        return chestRefillEnabled;
    }

    public int getChestRefillIntervalSeconds() {
        return Math.max(5, chestRefillIntervalSeconds);
    }

    public boolean isChestRefillKeepChestOpen() {
        return chestRefillKeepChestOpen;
    }

    public boolean isChestRefillShowHologram() {
        return chestRefillShowHologram;
    }

    /**
     * Lucky block tuning lives in {@code plugins/MythicSkywars/chests/lucky/luckyblocks.yml} (not {@code config.yml}).
     */
    public boolean isLuckyBlockReplaceCenterChests() {
        return LuckyBlockHook.isReplaceCenterChests();
    }

    public String getLuckyBlockIslandType(LuckyBlockHook.LuckyProfile profile) {
        return LuckyBlockHook.getIslandType(profile);
    }

    public String getLuckyBlockCenterType(LuckyBlockHook.LuckyProfile profile) {
        return LuckyBlockHook.getCenterType(profile);
    }

    public boolean isLuckyBlockDropsEnabled(LuckyBlockHook.LuckyProfile profile) {
        return LuckyBlockHook.isDropsEnabled(profile);
    }

    public int getLuckyBlockDropChancePercent(LuckyBlockHook.LuckyProfile profile) {
        return LuckyBlockHook.getDropChance(profile);
    }

    public List<String> getLuckyBlockDropMessages(LuckyBlockHook.LuckyProfile profile) {
        return LuckyBlockHook.getDropMessages(profile);
    }

    public List<String> getLuckyBlockDropCommands(LuckyBlockHook.LuckyProfile profile) {
        return LuckyBlockHook.getDropCommands(profile);
    }

    public boolean useExternalChat() {
        return useExternalChat;
    }

    public boolean addPrefix() {
        return addPrefix;
    }

    public boolean formatChat() {
        return enableFormatter;
    }

    public boolean limitGameChat() {
        return limitGameChat;
    }

    public boolean limitSpecChat() {
        return limitSpecChat;
    }

    public boolean limitLobbyChat() {
        return limitLobbyChat;
    }

    public int getSingleSlot() {
        return singleSlot;
    }

    public int getTeamSlot() {
        return teamSlot;
    }

    public boolean usePlayerNames() {
        return usePlayerNames;
    }

    public String getTeamMaterial() {
        return teamMaterial;
    }

    public boolean usePlayerGlassColors() {
        return usePlayerGlassColors;
    }

    public boolean showHealth() {
        return showHealth;
    }

    public double getBorderSize() {
        return borderSize;
    }

    public boolean borderEnabled() {
        return borderEnabled;
    }

    public List<String> getGameEndCommands() {
        return gameEndCommands;
    }

    public boolean getLookDirectionEnabled() {
        return lookDirectionEnabled;
    }

    public boolean getEnablePVPTimer() {
        return enablePVPTimer;
    }

    public int getPVPTimerTime() {
        return PVPTimerTime;
    }

    public boolean getEnableQuickDeath() {
        return enableQuickDeath;
    }

    public int getQuickDeathY() {
        return quickDeathY;
    }

    public boolean getKickOnWorldTeleport() {
        return kickOnWorldTeleport;
    }

    public boolean getClearInventoryOnWin() { return clearInventoryOnWin; }

    public boolean getEnableFlightOnWin() { return enableFlightOnWin; }

    public List<String> getGameServers() { return gameServers; }

    public boolean isLobbyServer() { return isLobbyServer; }

    public boolean isRandomVoteEnabled() { return randomOptionVoteEnabled; }
    public boolean isJoinGameItemEnabled() { return joinGameItemEnabled; }

    public boolean isJoinCompassRandomJoin() { return joinCompassRandomJoin; }
    public boolean isSpectateGameItemEnabled() { return spectateGameItemEnabled; }
    public boolean isOptionsItemEnabled() { return optionsGameItemEnabled; }
    public boolean isPlayAgainItemEnabled() { return playAgainItemEnabled; }
    public boolean isBackToLobbyItemEnabled() { return backToLobbyItemEnabled; }
    public boolean isRejoinItemEnabled() { return rejoinItemEnabled; }
    public boolean isStatsItemEnabled() { return statsItemEnabled; }

    public int getTeamSelectPos() { return teamSelectPos; }
    public int getPlayAgainPos() { return playAgainPos; }
    public int getBackToLobbyPos() { return backToLobbyPos; }
    public int getRejoinItemPos() { return rejoinItemPos; }
    public int getStatsItemPos() { return statsItemPos; }
    public boolean isRejoinEnabled() { return rejoinEnabled; }
    public int getRejoinWindowSeconds() { return Math.max(10, rejoinWindowSeconds); }

    public boolean isUseTeamMaterialBytes() { return useTeamMaterialBytes; }
    public int getStandardTeamMaterialByte() { return standardTeamMaterialByte; }
    public boolean isUseSeparateCages() { return useSeparateCages; }

    public boolean isClearInventoryOnLobbyJoin() { return clearInventoryOnLobbyJoin; }

    public boolean isChangeTablistNames() {
        return changeTablistNames;
    }
    public boolean isUseTeamNumberInMenu() { return useTeamNumberInMenu; }
    public boolean isBalanceUnselectedPlayersAcrossTeams() { return balanceUnselectedPlayersAcrossTeams; }


    public boolean isUseSlimeWorldManager() { return useSlimeWorldManager; }
    public String getSlimeWorldManagerSource() { return slimeWorldManagerSource; }
    public boolean isUsePartyAndFriends() { return usePartyAndFriends; }

    public boolean isUseTeamChat() { return useTeamChat; }
    public String getTimeFormat() { return timeFormat; }

    public boolean isCheckForBetaVersion() { return checkForBetaVersion; }
    public boolean isDisplayPlayerExeperience() {
        return displayPlayerExeperience;
    }

    public String getTeamJoinSound() { return teamJoinSound; }

    public String getDefaultKit() {
        if (defaultKit != null && !defaultKit.equalsIgnoreCase("null")) {
            return defaultKit;
        } else {
            return null;
        }
    }

}

