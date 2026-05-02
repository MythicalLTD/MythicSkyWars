package systems.mythical.mythicskywars.game;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.config.Config;
import systems.mythical.mythicskywars.enums.ChestPlacementType;
import systems.mythical.mythicskywars.enums.MatchState;
import systems.mythical.mythicskywars.enums.PlayerRemoveReason;
import systems.mythical.mythicskywars.enums.Vote;
import systems.mythical.mythicskywars.events.MythicSkywarsJoinEvent;
import systems.mythical.mythicskywars.events.MythicSkywarsMatchStateChangeEvent;
import systems.mythical.mythicskywars.game.cages.*;
import systems.mythical.mythicskywars.game.signs.SWRSign;
import systems.mythical.mythicskywars.managers.ArenaSetupHologramManager;
import systems.mythical.mythicskywars.managers.MatchManager;
import systems.mythical.mythicskywars.managers.PlayerStat;
import systems.mythical.mythicskywars.managers.worlds.ASWMWorldManager;
import systems.mythical.mythicskywars.managers.worlds.FileWorldManager;
import systems.mythical.mythicskywars.managers.worlds.WorldManager;
import systems.mythical.mythicskywars.managers.worlds.WorldManagerType;
import systems.mythical.mythicskywars.matchevents.*;
import systems.mythical.mythicskywars.menus.ArenaMenu;
import systems.mythical.mythicskywars.menus.ArenaSetupMenu;
import systems.mythical.mythicskywars.menus.TeamSelectionMenu;
import systems.mythical.mythicskywars.menus.TeamSpectateMenu;
import systems.mythical.mythicskywars.menus.gameoptions.*;
import systems.mythical.mythicskywars.menus.gameoptions.objects.CoordLoc;
import systems.mythical.mythicskywars.menus.gameoptions.objects.GameKit;
import systems.mythical.mythicskywars.utilities.LuckyBlockHook;
import systems.mythical.mythicskywars.utilities.Messaging;
import systems.mythical.mythicskywars.utilities.Party;
import systems.mythical.mythicskywars.utilities.Util;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.*;
import java.util.logging.Level;

public class GameMap {

    // Managers
    // In single mode:
    //      All team spawn locations are in team index 0 (aka spawnLocations.get(0))
    // In teams mode:
    //      Key Integer is team index, List is the spawn locations of such team
    private final HashMap<TeamCard, List<CoordLoc>> spawnLocations = new HashMap<>();
    private final ArrayList<Crate> crates = new ArrayList<>();
    private boolean forceStart;
    // TODO: Move all of these into WorldOptions
    private boolean disableDamage = false;
    private boolean allowFallDamage;
    private boolean allowRegen;
    private boolean projectilesOnly;
    private boolean projectileSpleefEnabled;
    private boolean doubleDamageEnabled;
    private boolean thunder;
    private boolean allowFriendlyFire;
    // Player data
    private final ArrayList<TeamCard> teamCards;
    private ArrayList<UUID> waitingPlayers = Lists.newArrayList();
    private final ArrayList<UUID> spectators = new ArrayList<>();
    private final List<String> winners = new ArrayList<>();
    private final HashMap<Player, Integer> playerKills = new HashMap<>();
    private final ArrayList<String> deathMatchWaiters = new ArrayList<>();
    // ..
    private int strikeCounter;
    private int nextStrike;
    private MatchState matchState;
    private int teamSize;
    // Edit mode settings
    private ChestPlacementType chestPlacementType;
    private final String name;
    private int timer;
    private int minPlayers;
    private GameKit kit;
    private Cage cage;
    // Map settings
    private String currentTime;
    private String currentHealth;
    private String currentChest;
    private String currentWeather;
    private String currentModifier;
    private KitVoteOption kitVoteOption;
    private ChestOption chestOption;
    private HealthOption healthOption;
    private TimeOption timeOption;
    private WeatherOption weatherOption;
    private ModifierOption modifierOption;
    private ArrayList<CoordLoc> chests;
    private ArrayList<CoordLoc> centerChests;
    private String environment;
    private Vote defaultChestType;
    private Vote defaultHealth;
    private Vote defaultTime;
    private Vote defaultWeather;
    private Vote defaultModifier;
    // Map descriptions
    private String displayName;
    private String designedBy;
    private final ArrayList<SWRSign> signs;
    private GameBoard gameboard;
    private boolean registered;
    private final String arenakey;
    private final GameQueue joinQueue;
    private boolean inEditing = false;
    private CoordLoc spectateSpawn;
    private CoordLoc lookDirection;
    private final ArrayList<CoordLoc> deathMatchSpawns;
    private boolean legacy = false;
    private final ArrayList<MatchEvent> events = new ArrayList<>();
    private final ArrayList<String> anvils = new ArrayList<>();
    private boolean customJoinMenuIcon = false;
    private ItemStack customJoinMenuItem = null;
    private CoordLoc waitingLobbySpawn;
    private boolean luckyModeEnabled;
    /** Set when lucky chest replacement succeeds; used for join items and refills. */
    private LuckyBlockHook.LuckyProfile activeLuckyProfile;

    public GameMap(final String name) {
        this.name = name;

        this.matchState = MatchState.OFFLINE;
        this.teamCards = new ArrayList<>();
        this.deathMatchSpawns = new ArrayList<>();
        this.signs = new ArrayList<>();
        this.chests = new ArrayList<>();
        this.centerChests = new ArrayList<>();
        this.chestPlacementType = ChestPlacementType.CENTER;
        loadArenaData();
        this.thunder = false;
        this.allowRegen = true;
        this.projectilesOnly = false;
        this.projectileSpleefEnabled = false;
        this.doubleDamageEnabled = false;
        this.timer = MythicSkywars.getCfg().getWaitTimer();
        this.joinQueue = new GameQueue(this);
        this.arenakey = name + "menu";
        if (MythicSkywars.getCfg().kitVotingEnabled()) {
            kitVoteOption = new KitVoteOption(this, name + "kitVote");
        }
        this.chestOption = new ChestOption(this, name + "chest");
        this.healthOption = new HealthOption(this, name + "health");
        this.timeOption = new TimeOption(this, name + "time");
        this.weatherOption = new WeatherOption(this, name + "weather");
        this.modifierOption = new ModifierOption(this, name + "modifier");

        this.gameboard = new GameBoard(this);
        if (legacy) {
            boolean loaded = loadWorldForScanning(name);
            if (loaded) {
                scanChunksForSkywarsFeatures(null, false);
                saveArenaData();
                MythicSkywars.getWM().deleteWorld(name, false);
            }
        }
        if (registered) {
            registerMap(null);
        }

        new ArenaMenu(arenakey, this);
        if (MythicSkywars.getCfg().joinMenuEnabled()) {
            new TeamSelectionMenu(this);
        }
        if (MythicSkywars.getCfg().spectateMenuEnabled()) {
            new TeamSpectateMenu(this);
        }
    }



    /*Player Handling Methods*/


    private static boolean loadWorldForScanning(String name) {
        WorldManager wm = MythicSkywars.getWM();

        if (wm instanceof FileWorldManager) {
            File dataDirectory = MythicSkywars.get().getDataFolder();
            File maps = new File(dataDirectory, "maps");

            File source = new File(maps, name);
            File target = wm.getWorldFolder(name);
            wm.copyWorld(source, target);
            boolean mapExists = false;
            if (target.isDirectory()) {
                String[] list = target.list();
                if (list != null && list.length > 0) {
                    mapExists = true;
                }
            }
            if (mapExists) {
                MythicSkywars.getWM().deleteWorld(name, false);
                MythicSkywars.get().getLogger().info("Deleted existing map: " + name);
            }

            if (source.exists()) {
                wm.copyWorld(source, target);
            } else {
                MythicSkywars.get().getLogger().info("Map source does not exist: " + source.getAbsolutePath());
            }
        }

        final boolean[] loaded = {false};
        Bukkit.getScheduler().runTaskAsynchronously(MythicSkywars.get(), () -> {
            loaded[0] = MythicSkywars.getWM().loadWorld(name, World.Environment.NORMAL, true);
            if (!loaded[0]) {
                MythicSkywars.get().getLogger().info("Could Not Load Map: " + name);
            }
        });

        return loaded[0];
    }

    public static void editMap(GameMap gMap, Player player) {
        if (gMap.isRegistered()) {
            gMap.unregister(true);
            new BukkitRunnable() {
                @Override
                public void run() {
                    startEdit(gMap, player);
                }
            }.runTaskLater(MythicSkywars.get(), 20);
        } else {
            startEdit(gMap, player);
        }
    }

    private static void startEdit(GameMap gMap, Player player) {
        String worldName = gMap.getName();
        if (gMap.isEditing()) {
            boolean loaded = false;
            for (World world : MythicSkywars.get().getServer().getWorlds()) {
                if (world.getName().equals(worldName)) {
                    loaded = true;
                }
            }
            if (!loaded) {
                loaded = loadWorld(worldName, gMap, false);
            }
            if (loaded) {
                prepareForEditor(player, gMap, worldName);
            }
        } else {
            gMap.setEditing(true);
            boolean loaded = loadWorld(worldName, gMap, false);
            if (loaded) {
                prepareForEditor(player, gMap, worldName);
            } else {
                player.sendMessage(new Messaging.MessageFormatter().format("error.map-fail-load"));
            }
        }
    }

    private static boolean loadWorld(String worldName, GameMap gMap, boolean readOnly) {
        File dataDirectory = new File(MythicSkywars.get().getDataFolder(), "maps");
        File source = new File(dataDirectory, worldName);
        File target = MythicSkywars.getWM().getWorldFolder(worldName);
        boolean mapExists = false;
        if (target.isDirectory()) {
            String[] list = target.list();
            if (list != null && list.length > 0) {
                mapExists = true;
            }
        }
        if (mapExists) {
            MythicSkywars.getWM().deleteWorld(worldName, false);
        }
        MythicSkywars.getWM().copyWorld(source, target);
        return MythicSkywars.getWM().loadWorld(worldName, World.Environment.valueOf(gMap.environment), readOnly);
    }

    private static void prepareForEditor(Player player, GameMap gMap, String worldName) {
        World editWorld = MythicSkywars.get().getServer().getWorld(worldName);
        editWorld.setAutoSave(true);
        
        // Disable mob spawning and clear all existing mobs/animals
        MythicSkywars.getNMS().setGameRule(editWorld, "doMobSpawning", "false");
        MythicSkywars.getNMS().setGameRule(editWorld, "doDaylightCycle", "false");
        for (org.bukkit.entity.Entity entity : editWorld.getEntities()) {
            if (!(entity instanceof Player)) {
                entity.remove();
            }
        }
        
        for (TeamCard tCard : gMap.getTeamCards()) {
            if (tCard.getSpawns() != null) {
                for (CoordLoc loc : tCard.getSpawns()) {
                    editWorld.getBlockAt(loc.getX(), loc.getY(), loc.getZ()).setType(Material.DIAMOND_BLOCK);
                }
            }
        }
        for (CoordLoc cl : gMap.getDeathMatchSpawns()) {
            editWorld.getBlockAt(cl.getX(), cl.getY(), cl.getZ()).setType(Material.EMERALD_BLOCK);
        }
        MythicSkywars.get().getServer().getScheduler().scheduleSyncDelayedTask(MythicSkywars.get(), () -> {
            // Remove lobby items/menu tools when entering map editor mode.
            Util.get().clear(player);
            player.teleport(new Location(editWorld, 0, 95, 0), TeleportCause.PLUGIN);
            player.setGameMode(GameMode.CREATIVE);
            player.setAllowFlight(true);
            player.setFlying(true);
            ArenaSetupMenu.giveTool(player);
            player.sendMessage(new Messaging.MessageFormatter().format("maps.editor.tool-given"));
            ArenaSetupHologramManager.get().requestFullRefresh(gMap);
        }, 20);
    }

    /*Map Handling Methods*/

    public static void openArenasManager(Player player) {
        if (player.hasPermission("sw.arenas")) {
            MythicSkywars.getIC().show(player, "arenasmenu");
            updateArenasManager();
        }
    }

    public static void updateArenasManager() {
        if (MythicSkywars.getIC().has("arenasmenu")) {
            MythicSkywars.getIC().getMenu("arenasmenu").update();
        }
    }

    public void scanWorld(boolean b, Player player) {
        if (!inEditing) {
            GameMap.editMap(this, player);
        }
        scanChunksForSkywarsFeatures(player, b);
        saveArenaData();
    }

    public void checkSpectators() {
        if (!spectators.isEmpty()) {
            for (UUID uuid : spectators) {
                Player spec = MythicSkywars.get().getServer().getPlayer(uuid);
                if (spec == null) continue;
                if (isOutsideBorder(spec)) {
                    CoordLoc ss = getSpectateSpawn();
                    Location spectateSpawn = new Location(getCurrentWorld(), ss.getX(), ss.getY(), ss.getZ());
                    spec.teleport(spectateSpawn, TeleportCause.END_PORTAL);
                }
            }
        }
    }

    private boolean isOutsideBorder(Player p) {
        Location loc = p.getLocation();
        WorldBorder border = p.getWorld().getWorldBorder();
        double size = MythicSkywars.getCfg().getBorderSize() / 2;
        Location center = border.getCenter();
        double x = loc.getX() - center.getX(), z = loc.getZ() - center.getZ();
        return ((x > size || (-x) > size) || (z > size || (-z) > size));
    }

    private void loadEvents() {
        events.clear();
        File dataDirectory = MythicSkywars.get().getDataFolder();
        File mapDataDirectory = new File(dataDirectory, "mapsData");

        if (!mapDataDirectory.exists() && !mapDataDirectory.mkdirs()) {
            return;
        }

        File mapFile = new File(mapDataDirectory, name + ".yml");
        FileConfiguration fc = YamlConfiguration.loadConfiguration(mapFile);
        events.add(new DisableRegenEvent(this, GameEventsConfig.resolveEnabled(this, "DisableRegenEvent", fc.getBoolean("events.DisableRegenEvent.enabled"))));
        events.add(new HealthDecayEvent(this, GameEventsConfig.resolveEnabled(this, "HealthDecayEvent", fc.getBoolean("events.HealthDecayEvent.enabled"))));
        events.add(new EnderDragonEvent(this, GameEventsConfig.resolveEnabled(this, "EnderDragonEvent", fc.getBoolean("events.EnderDragonEvent.enabled"))));
        events.add(new WitherEvent(this, GameEventsConfig.resolveEnabled(this, "WitherEvent", fc.getBoolean("events.WitherEvent.enabled"))));
        events.add(new MobSpawnEvent(this, GameEventsConfig.resolveEnabled(this, "MobSpawnEvent", fc.getBoolean("events.MobSpawnEvent.enabled"))));
        events.add(new DeathMatchEvent(this, GameEventsConfig.resolveEnabled(this, "DeathMatchEvent", fc.getBoolean("events.DeathMatchEvent.enabled"))));
        events.add(new ArrowRainEvent(this, GameEventsConfig.resolveEnabled(this, "ArrowRainEvent", fc.getBoolean("events.ArrowRainEvent.enabled"))));
        events.add(new AnvilRainEvent(this, GameEventsConfig.resolveEnabled(this, "AnvilRainEvent", fc.getBoolean("events.AnvilRainEvent.enabled"))));
        events.add(new CrateDropEvent(this, GameEventsConfig.resolveEnabled(this, "CrateDropEvent", fc.getBoolean("events.CrateDropEvent.enabled"))));
        events.add(new ShrinkingBorderEvent(this, GameEventsConfig.resolveEnabled(this, "ShrinkingBorderEvent", fc.getBoolean("events.ShrinkingBorderEvent.enabled"))));
        events.add(new ProjectilesOnlyEvent(this, GameEventsConfig.resolveEnabled(this, "ProjectilesOnlyEvent", fc.getBoolean("events.ProjectilesOnlyEvent.enabled"))));
        events.add(new ProjectileSpleefEvent(this, GameEventsConfig.resolveEnabled(this, "ProjectileSpleefEvent", fc.getBoolean("events.ProjectileSpleefEvent.enabled"))));
        events.add(new DoubleDamageEvent(this, GameEventsConfig.resolveEnabled(this, "DoubleDamageEvent", fc.getBoolean("events.DoubleDamageEvent.enabled"))));
        events.add(new GhastEvent(this, GameEventsConfig.resolveEnabled(this, "GhastEvent", fc.getBoolean("events.GhastEvent.enabled"))));
        for (MatchEvent event : events) {
            GameEventsConfig.apply(this, event);
        }
    }

    public void update() {
        updateArenasManager();
        this.updateArenaManager();
        this.updateSigns();
        this.sendBungeeUpdate();
        if (MythicSkywars.getIC().has("joinsinglemenu") && teamSize == 1) {
            MythicSkywars.getIC().getMenu("joinsinglemenu").update();
        }
        if (MythicSkywars.getIC().has("jointeammenu") && teamSize > 1) {
            MythicSkywars.getIC().getMenu("jointeammenu").update();
            if ((matchState == MatchState.WAITINGSTART || matchState == MatchState.WAITINGLOBBY) && MythicSkywars.getIC().has(name + "teamselect")) {
                MythicSkywars.getIC().getMenu(name + "teamselect").update();
            }
        }
    }

    public boolean addPlayers(@Nullable TeamCard teamToTry, final Player player) {
        // Idempotency guard: repeated /sw join spam should not enqueue duplicate joins.
        if (player == null) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        if (waitingPlayers.contains(playerId) || spectators.contains(playerId)) {
            return true;
        }
        if (getAllPlayers().contains(player)) {
            return true;
        }

        // If busy return false
        if (Util.get().isBusy(playerId)) {
            if (MythicSkywars.getCfg().debugEnabled()) {
                MythicSkywars.get().getLogger().info("#addPlayers: " + player.getName() + " is busy, cannot join " + this.getName());
            }
            return false;
        }
        boolean result = false;

        // Check that player stats have been loaded
        PlayerStat ps = PlayerStat.getPlayerStats(player.getUniqueId());
        if (ps == null || !ps.isInitialized()) return false;

        if (teamSize > 1) {
            teamCards.sort(new TeamCardComparator());
        } else {
            Collections.shuffle(teamCards);
        }

        // Debug logging if enabled in config
        if (MythicSkywars.getCfg().debugEnabled()) {
            MythicSkywars.get().getLogger().info("#addPlayers: " + player.getName() + "'s PlayerStats are initialized");
            MythicSkywars.get().getLogger().info("#addPlayers: MatchState: " + getMatchState().name());
        }

        // Verify player does not attempt to join while mounted
        Util.get().ejectPassengers(player);

        // If in any mode & WAITING while countdown & lobby mode is not enabled
        if (getMatchState() == MatchState.WAITINGSTART) {
            TeamCard reservedTeamCard = null;
            if (teamToTry == null) { // If not in party mode
                for (TeamCard tCard : teamCards) {
                    if (MythicSkywars.getCfg().debugEnabled()) {
                        MythicSkywars.get().getLogger().info("#addPlayers: --teamCard: " + (tCard.getPlace() + 1));
                        MythicSkywars.get().getLogger().info("#addPlayers: (" + (tCard.getPlace() + 1) + ") emptySlots: " + tCard.getEmptySlots());
                    }
                    if (tCard.getEmptySlots() > 0) { // If space available
                        reservedTeamCard = tCard.sendReservation(player, ps);
                        if (reservedTeamCard != null) break;                    }
                }
            } else { // In party mode
                if (teamToTry.getEmptySlots() > 0) {
                    reservedTeamCard = teamToTry.sendReservation(player, ps);
                } else {
                    MythicSkywars.get().getLogger().warning("Player attempted to join party team but the team referenced is empty (" + player.getName() + ", " + teamToTry.getTeamName() + ")");
                }
            }
            if (reservedTeamCard != null) { // If setup for player success
                result = reservedTeamCard.joinGame(player); // tp to arena
                if (result) {
                    PlayerStat.resetScoreboard(player);
                }
            } else { // Warn console that setup failed
                MythicSkywars.get().getLogger().warning("Failed to send reservation for " + player.getName());
            }
        // else if in lobby waiting mode
        } else if (getMatchState() == MatchState.WAITINGLOBBY) {
            PlayerStat.resetScoreboard(player);
            addWaitingPlayer(player);
            getJoinQueue().add(new PlayerCard(null, player.getUniqueId(), null));
            Bukkit.getPluginManager().callEvent(new MythicSkywarsJoinEvent(player, this));
            if (MythicSkywars.getCfg().kitVotingEnabled()) {
                getKitVoteOption().updateKitVotes();
            }
            if (MythicSkywars.getCfg().resetTimerOnJoin()) {
                setTimer(MythicSkywars.getCfg().getWaitTimer());
            }
            result = true;
        }
        if (MythicSkywars.getCfg().debugEnabled()) {
            Bukkit.getLogger().info("#addPlayers: result = " + result);
        }
        this.update();
        return result;
    }

    public boolean addPlayers(@Nullable TeamCard teamToTry, final Party party) {
        TeamCard team = null;
        Map<TeamCard, ArrayList<Player>> players = new HashMap<>();
        if (teamSize == 1) {
            Collections.shuffle(teamCards);
            for (UUID uuid : party.getMembers()) {
                Player player = Bukkit.getPlayer(uuid);
                if (Util.get().isBusy(uuid)) {
                    party.sendPartyMessage(new Messaging.MessageFormatter().setVariable("player", player.getName()).format("party.memberbusy"));
                } else {
                    PlayerStat ps = PlayerStat.getPlayerStats(uuid);
                    if (ps != null && player != null && ps.isInitialized()) {
                        for (TeamCard tCard : teamCards) {
                            if (tCard.getEmptySlots() > 0) {
                                Util.get().ejectPassengers(player);
                                TeamCard reserve = tCard.sendReservation(player, ps);
                                if (reserve != null) {
                                    players.computeIfAbsent(reserve, k -> new ArrayList<>());
                                    players.get(reserve).add(player);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        } else {
            if (teamToTry == null) {
                teamCards.sort(new TeamCardComparator());
                for (TeamCard tCard : teamCards) {
                    if (tCard.getEmptySlots() >= party.getSize()) {
                        for (int i = 0; i < party.getSize(); i++) {
                            Player player = Bukkit.getPlayer(party.getMembers().get(i));
                            PlayerStat ps = PlayerStat.getPlayerStats(player.getUniqueId());
                            if (ps != null && ps.isInitialized()) {
                                Util.get().ejectPassengers(player);
                                TeamCard reserve = tCard.sendReservation(player, ps);
                                if (reserve != null) {
                                    players.computeIfAbsent(reserve, k -> new ArrayList<>()).add(player);
                                }
                                team = reserve;
                            }
                        }
                    }
                    if (team != null && players.get(team).size() == party.getSize()) {
                        break;
                    }
                }
            } else {
                    if (teamToTry.getEmptySlots() >= party.getSize()) {
                    for (int i = 0; i < party.getSize(); i++) {
                        Player player = Bukkit.getPlayer(party.getMembers().get(i));
                        PlayerStat ps = PlayerStat.getPlayerStats(player.getUniqueId());
                        if (ps != null && ps.isInitialized()) {
                            Util.get().ejectPassengers(player);
                            TeamCard reserve = teamToTry.sendReservation(player, ps);
                            if (reserve != null) {
                                players.computeIfAbsent(reserve, k -> new ArrayList<>()).add(player);
                            }
                            team = reserve;
                        }
                    }
                }
            }
        }

        boolean result = false;
        if (teamSize == 1 && players.size() == party.getSize()) {
            for (TeamCard tCard : players.keySet()) {
                result = tCard.joinGame(players.get(tCard).get(0));
            }
        } else if (teamSize > 1 && team != null && players.get(team).size() == party.getSize()) {
            for (int i = 0; i < players.get(team).size(); i++) {
                result = team.joinGame(players.get(team).get(i));
            }
        } else {
            for (ArrayList<Player> play : players.values()) {
                for (Player player : play) {
                    PlayerCard pCard = this.getPlayerCard(player);
                    pCard.reset();
                }
            }
        }
        this.update();
        gameboard.updateScoreboard();
        return result;
    }

    public void removePlayer(final UUID uuid) {
        for (TeamCard tCard : teamCards) {
            if (tCard.removePlayer(uuid)) break;
        }
        spectators.remove(uuid);
        this.removeWaitingPlayer(uuid);
        this.update();
        gameboard.updateScoreboard();
    }

    public ArrayList<Player> getAlivePlayers() {
        ArrayList<Player> alivePlayers = new ArrayList<>();
        for (TeamCard tCard : teamCards) {
            for (PlayerCard pCard : tCard.getPlayerCards()) {
                Player cardPlayer = pCard.getPlayer();
                if (cardPlayer != null) {
                    if (!mapContainsDead(cardPlayer.getUniqueId()) && cardPlayer.getGameMode() != GameMode.SPECTATOR) {
                        alivePlayers.add(cardPlayer);
                    }
                }
            }
        }
        return alivePlayers;
    }

    public boolean mapContainsDead(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        for (TeamCard tCard : teamCards) {
            for (PlayerCard p : tCard.getPlayerCards()) {
                if (uuid.equals(p.getUUID()) && p.isDead()) {
                    return true;
                }
            }
        }
        return false;
    }

    public ArrayList<Player> getAllPlayers() {
        ArrayList<Player> allPlayers = new ArrayList<>();
        for (TeamCard tCard : teamCards) {
            for (PlayerCard pCard : tCard.getPlayerCards()) {
                if (pCard.getPlayer() != null) {
                    allPlayers.add(pCard.getPlayer());
                }
            }
        }
        for (UUID player : waitingPlayers) {
            Player pl = Bukkit.getPlayer(player);
            if (pl == null) continue;
            if (!allPlayers.contains(pl)) {
                allPlayers.add(pl);
            }
        }
        for (UUID player : spectators) {
            Player pl = Bukkit.getPlayer(player);
            if (pl == null) continue;
            if (!allPlayers.contains(pl)) {
                allPlayers.add(pl);
            }
        }
        return allPlayers;
    }

    public ArrayList<Player> getMessageAblePlayers(boolean isSpectator) {
        ArrayList<Player> receivers = new ArrayList<>();
        if (!isSpectator) {
            for (TeamCard tCard : teamCards) {
                for (PlayerCard pCard : tCard.getPlayerCards()) {
                    if (pCard.getPlayer() != null) {
                        if (!mapContainsDead(pCard.getPlayer().getUniqueId())) {
                            receivers.add(pCard.getPlayer());
                        }
                    }
                }
            }
            for (UUID pUid : waitingPlayers) {
                if (!mapContainsDead(pUid)) {
                    Player p = Bukkit.getPlayer(pUid);
                    if (p != null && !receivers.contains(p)) {
                        receivers.add(p);
                    }
                }
            }
        }
        for (UUID uuid : spectators) {
            Player player = MythicSkywars.get().getServer().getPlayer(uuid);
            if (player != null) {
                receivers.add(player);
            }
        }
        return receivers;
    }

    /**
     *
     * @param player Player context for the request. Can be null. Not used in SWR by default. API only.
     * @return true if the player can be added to the match
     */
    public boolean canAddPlayer(Player player) {
        if (!((this.matchState == MatchState.WAITINGSTART || this.matchState == MatchState.WAITINGLOBBY) && this.registered)) {
            return false;
        }
        for (TeamCard tCard : teamCards) {
            if (tCard.getEmptySlots() > 0) {
                return true;
            }
        }
        return false;
    }

    public boolean canAddParty(Party party) {
        if (!((this.matchState == MatchState.WAITINGSTART || this.matchState == MatchState.WAITINGLOBBY) && this.registered)) {
            return false;
        }
        if (teamSize == 1) {
            int playerCount = getPlayerCount();
            return playerCount + party.getSize() - 1 < teamCards.size();
        } else {
            for (TeamCard tCard : teamCards) {
                if (tCard.getEmptySlots() >= party.getSize()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean removeMap() {
        unregister(false);
        File dataDirectory = new File(MythicSkywars.get().getDataFolder(), "maps");
        File target = new File(dataDirectory, name);
        MythicSkywars.getWM().deleteWorld(target);
        if (MythicSkywars.getWM() instanceof ASWMWorldManager) MythicSkywars.getWM().deleteWorld(name, true);

        File mapDataDirectory = new File(MythicSkywars.get().getDataFolder(), "mapsData");
        if (!mapDataDirectory.exists() && !mapDataDirectory.mkdirs()) {
            return false;
        }
        File mapFile = new File(mapDataDirectory, name + ".yml");
        boolean result = mapFile.delete();
        if (result) {
            MythicSkywars.getGameMapMgr().remove(this);
        }
        return result;
    }

    public void saveArenaData() {
        File dataDirectory = MythicSkywars.get().getDataFolder();
        File mapDataDirectory = new File(dataDirectory, "mapsData");

        if (!mapDataDirectory.exists() && !mapDataDirectory.mkdirs()) {
            return;
        }

        File mapFile = new File(mapDataDirectory, name + ".yml");
        if (!mapFile.exists()) {
            MythicSkywars.get().getLogger().info("File doesn't exist!");
            return;
        }
        MythicSkywars.getGameMapMgr().copyDefaults(mapFile);
        FileConfiguration fc = YamlConfiguration.loadConfiguration(mapFile);
        fc.set("displayname", displayName);
        fc.set("minplayers", minPlayers);
        fc.set("creator", designedBy);
        fc.set("registered", registered);
        fc.set("spectateSpawn", spectateSpawn == null ? null : spectateSpawn.getLocationString());
        fc.set("cage", cage.getType().toString().toLowerCase());
        fc.set("teamSize", teamSize);
        fc.set("environment", environment);
        fc.set("defaultChestType", defaultChestType.name());
        fc.set("defaultHealth", defaultHealth.name());
        fc.set("defaultTime", defaultTime.name());
        fc.set("defaultWeather", defaultWeather.name());
        fc.set("defaultModifier", defaultModifier.name());
        fc.set("allowFriendlyFire", allowFriendlyFire);

        fc.set("enableCustomJoinMenuItem", customJoinMenuIcon);

        fc.set("waitingLobbySpawn", waitingLobbySpawn == null ? null : waitingLobbySpawn.getLocationString());

        if (teamSize == 1) {
            List<String> spawns = new ArrayList<>();
            for (List<CoordLoc> coords : spawnLocations.values()) {
                // Purposefully only taking index 0 in case a teams map was converted to a solo map
                spawns.add(coords.get(0).getLocationString());
            }
            fc.set("spawns", spawns);
        } else {
            for (Map.Entry<TeamCard, List<CoordLoc>> teamEntry : this.spawnLocations.entrySet()) {

                List<String> spawns = new ArrayList<>();
                for (CoordLoc loc : teamEntry.getValue()) {
                    spawns.add(loc.getLocationString());
                }

                fc.set("spawns.team-" + this.getTeamCardPosition(teamEntry.getKey()), spawns);
            }
        }

        List<String> dSpawns = new ArrayList<>();
        for (CoordLoc loc : deathMatchSpawns) {
            dSpawns.add(loc.getLocationString());
        }
        fc.set("deathMatchSpawns", dSpawns);

        List<String> stringSigns = new ArrayList<>();
        for (SWRSign s : signs) {
            stringSigns.add(Util.get().locationToString(s.getLocation()));
        }
        fc.set("signs", stringSigns);

        List<String> stringChests = new ArrayList<>();
        for (CoordLoc chest : chests) {
            stringChests.add(chest.getLocationString());
        }
        fc.set("chests", stringChests);

        List<String> stringCenterChests = new ArrayList<>();
        for (CoordLoc chest : centerChests) {
            stringCenterChests.add(chest.getLocationString());
        }
        fc.set("centerChests", stringCenterChests);

        fc.set("legacy", null);
        try {
            fc.save(mapFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean getCustomJoinMenuItemEnabled() {
        return customJoinMenuIcon;
    }

    public ItemStack getCustomJoinMenuItem() {
        return customJoinMenuItem;
    }

    public void loadArenaData() {
        File dataDirectory = MythicSkywars.get().getDataFolder();
        File mapDataDirectory = new File(dataDirectory, "mapsData");

        if (!mapDataDirectory.exists() && !mapDataDirectory.mkdirs()) {
            return;
        }

        File mapFile = new File(mapDataDirectory, name + ".yml");
        MythicSkywars.getGameMapMgr().copyDefaults(mapFile);

        // Start parse options
        FileConfiguration fc = YamlConfiguration.loadConfiguration(mapFile);
        displayName = fc.getString("displayname", name);
        designedBy = fc.getString("creator", "");
        registered = fc.getBoolean("registered", false);
        spectateSpawn = Util.get().getCoordLocFromString(fc.getString("spectateSpawn", "0:95:0"));
        lookDirection = Util.get().getCoordLocFromString(fc.getString("lookDirection", "00:95:0"));
        legacy = fc.getBoolean("legacy");
        teamSize = fc.getInt("teamSize", 1);
        environment = fc.getString("environment", "NORMAL");
        allowFriendlyFire = fc.getBoolean("allowFriendlyFire", false);

        // Default Vote Types
        String cfgDefaultChestType = fc.getString("defaultChestType", "CHESTNORMAL");
        Bukkit.getConsoleSender().sendMessage("Default chest type of map " + name + " is " + cfgDefaultChestType);
        if (cfgDefaultChestType.startsWith("CHEST")) {
            defaultChestType = Vote.getByValue(cfgDefaultChestType, Vote.CHESTNORMAL);
            Bukkit.getConsoleSender().sendMessage("Default chest type of map" + name + " started with CHEST and is set to " + defaultChestType);
        } else defaultChestType = Vote.CHESTNORMAL;

        String cfgDefaultHealth = fc.getString("defaultHealth", "HEALTHTEN");
        if (cfgDefaultHealth.startsWith("HEALTH"))
            defaultHealth = Vote.getByValue(cfgDefaultHealth, Vote.HEALTHTEN);
        else defaultHealth = Vote.HEALTHTEN;
        String cfgDefaultTime = fc.getString("defaultTime", "TIMENOON");
        if (cfgDefaultTime.startsWith("TIME"))
            defaultTime = Vote.getByValue(cfgDefaultTime, Vote.TIMENOON);
        else defaultTime = Vote.TIMENOON;
        String cfgDefaultWeather = fc.getString("defaultWeather", "WEATHERSUN");
        if (cfgDefaultWeather.startsWith("WEATHER"))
            defaultWeather = Vote.getByValue(cfgDefaultWeather, Vote.WEATHERSUN);
        else defaultWeather = Vote.WEATHERSUN;
        String cfgDefaultModifier = fc.getString("defaultModifier", "MODIFIERNONE");
        if (cfgDefaultModifier.startsWith("MODIFIER"))
            defaultModifier = Vote.getByValue(cfgDefaultModifier, Vote.MODIFIERNONE);
        else defaultModifier = Vote.MODIFIERNONE;

        // Icon
        customJoinMenuIcon = fc.getBoolean("enableCustomJoinMenuItem", false);
        MythicSkywars.getIM().addExtraItem("joinitem-" + name, new ArrayList<>(), fc.getString("customJoinMenuItem", "LEATHER_HELMET"));
        customJoinMenuItem = MythicSkywars.getIM().getItem("joinitem-" + name);

        String cage = fc.getString("cage");
        CageType ct = CageType.matchType(cage.toUpperCase());
        setCage(ct);

        List<String> dSpawns = fc.getStringList("deathMatchSpawns");
        List<String> stringSigns = fc.getStringList("signs");
        List<String> stringChests = fc.getStringList("chests");
        List<String> stringCenterChests = fc.getStringList("centerChests");

        // Setup team cards
        this.spawnLocations.clear();
        this.teamCards.clear();

        boolean spawnsIsList = fc.get("spawns") instanceof List;
        if (spawnsIsList || this.teamSize == 1) {
            List<String> spawns = fc.getStringList("spawns");
            for (String spawn : spawns) {
                CoordLoc spawnLocation = Util.get().getCoordLocFromString(spawn);
                addTeamCard(Lists.newArrayList(spawnLocation));
            }
        } else {
            ConfigurationSection configSpawns = fc.getConfigurationSection("spawns");
            if (configSpawns == null) {
                MythicSkywars.get().getLogger().warning("Invalid configuration section 'spawns'! This may cause an error below!");
                configSpawns = fc.createSection("spawns");
            }

            for (String team : configSpawns.getKeys(false)) {
                if (team.startsWith("team-")) {
                    // Get spawn location strings
                    List<String> spawns = fc.getStringList("spawns." + team);

                    // Parse all spawn location strings to actual coordinates
                    ArrayList<CoordLoc> locs = Lists.newArrayList();
                    for (String spawn : spawns) {
                        locs.add(Util.get().getCoordLocFromString(spawn));
                    }

                    // Remove 1 to change from user readable to program index
                    addTeamCard(locs);
                } else {
                    MythicSkywars.get().getLogger().warning("Invalid configuration section! Ignoring for now, however PLEASE REMOVE OR FIX IT (spawns." + team + ")");
                }
            }
        }

        if (fc.contains("waitingLobbySpawn")) {
            this.waitingLobbySpawn = Util.get().getCoordLocFromString(fc.getString("waitingLobbySpawn"));
        }

        int def = 2;
        if (teamCards.size() > 4) {
            def = teamCards.size() / 2;
        }
        minPlayers = fc.getInt("minplayers", def);

        deathMatchSpawns.clear();
        for (String dSpawn : dSpawns) {
            deathMatchSpawns.add(Util.get().getCoordLocFromString(dSpawn));
        }

        signs.clear();
        for (String s : stringSigns) {
            signs.add(MythicSkywars.getNMS().createSWRSign(name, Util.get().stringToLocation(s)));
        }

        chests.clear();
        for (String chest : stringChests) {
            addChest(Util.get().getCoordLocFromString(chest), ChestPlacementType.NORMAL, false);
        }

        centerChests.clear();
        for (String chest : stringCenterChests) {
            addChest(Util.get().getCoordLocFromString(chest), ChestPlacementType.CENTER, false);
        }

        loadEvents();
        try {
            fc.save(mapFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Code 0 = successfully registered
     * Code 1 = Unbalanced teams
     * Code 2 = Not enough spawns (at least 2)
     * Code 3 = No spectating spawn set
     * Code 4 = No lobby spawn set in team mode
     * Code -1 = Unknown issue
     *
     * @return int status of registering
     */
    public int registerMap(@Nullable CommandSender sender) {
        if (inEditing) {
            saveMap(sender);
        }
        if (spawnLocations.size() > 1) {
            int maxPlayers = getMaxPlayers();
            int actualMaxPlayers = teamCards.size() * teamSize;

            if ((teamSize > 1 && maxPlayers == actualMaxPlayers || !MythicSkywars.getCfg().isUseSeparateCages()) || (teamSize == 1 && maxPlayers > 1)) {
                if (spectateSpawn == null && MythicSkywars.getCfg().spectateEnable()) {
                    MythicSkywars.get().getLogger().info("Could Not Register Map: " + name + " - No spectator spawn has been set. Set it using '/swm spawn spec'");
                    registered = false;
                    return 3;
                }
                if (waitingLobbySpawn == null && teamSize > 1) {
                    // Auto-generate a waiting lobby platform at Y=100 with barrier walls
                    MythicSkywars.get().getLogger().info("Map " + name + " has no waiting lobby spawn. Generating barrier platform at Y=100...");
                    waitingLobbySpawn = new CoordLoc(0, 100, 0);
                    World w = getCurrentWorld();
                    if (w != null) {
                        // Create a 5x5 diamond block floor at Y=100
                        for (int x = -2; x <= 2; x++) {
                            for (int z = -2; z <= 2; z++) {
                                w.getBlockAt(x, 100, z).setType(org.bukkit.Material.DIAMOND_BLOCK);
                            }
                        }
                        // Create barrier walls around the platform (3 blocks high)
                        for (int y = 101; y <= 103; y++) {
                            for (int x = -3; x <= 3; x++) {
                                w.getBlockAt(x, y, -3).setType(org.bukkit.Material.BARRIER);
                                w.getBlockAt(x, y, 3).setType(org.bukkit.Material.BARRIER);
                            }
                            for (int z = -3; z <= 3; z++) {
                                w.getBlockAt(-3, y, z).setType(org.bukkit.Material.BARRIER);
                                w.getBlockAt(3, y, z).setType(org.bukkit.Material.BARRIER);
                            }
                        }
                        // Barrier ceiling
                        for (int x = -3; x <= 3; x++) {
                            for (int z = -3; z <= 3; z++) {
                                w.getBlockAt(x, 104, z).setType(org.bukkit.Material.BARRIER);
                            }
                        }
                    }
                    MythicSkywars.get().getLogger().info("Generated waiting lobby for map " + name + " at 0, 101, 0");
                }

                registered = true;
                gameboard = new GameBoard(this);
                refreshMap();
                getJoinQueue().start();
                MythicSkywars.get().getLogger().info("Registered Map " + name + "!");
                return 0;
            } else {
                registered = false;
                MythicSkywars.get().getLogger().info("Could Not Register Map: " + name + " - Not all teams have enough spawns. There are only " + maxPlayers + "/" + actualMaxPlayers + " spawns set.");
                return 1;
            }
        } else {
            registered = false;
            MythicSkywars.get().getLogger().info("Could Not Register Map: " + name + " - Map must have at least 2 Spawn Points");
            return 2;
        }
    }

    /*Inventories*/

    public void unregister(boolean save) {
        if (getJoinQueue() != null) getJoinQueue().kill();
        // Unregister and stop
        this.registered = false;
        stopGameInProgress();
        // If save -> save all arena data
        if (save) {
            saveArenaData();
        }
    }

    public void stopGameInProgress() {
        this.setMatchState(MatchState.OFFLINE);
        ImmutableList<UUID> specUUIDs = ImmutableList.copyOf(this.getSpectators());
        for (final UUID uuid : specUUIDs) {
            final Player player = MythicSkywars.get().getServer().getPlayer(uuid);
            if (player != null) {
                MythicSkywars.get().getPlayerManager().removePlayer(
                        player, PlayerRemoveReason.PLAYER_QUIT_GAME, null, false);
            }
        }
        for (final Player player : this.getAlivePlayers()) {
            if (player != null) {
                MythicSkywars.get().getPlayerManager().removePlayer(
                        player, PlayerRemoveReason.OTHER, null, false);
            }
        }
        MythicSkywars.getWM().deleteWorld(this.getName(), false);
    }

    private void loadMap() {
        WorldManager worldManager = MythicSkywars.getWM();
        String mapName = name;
        Server server = MythicSkywars.get().getServer();

        // ONLY in file mode
        if (worldManager.getType() == WorldManagerType.FILE) {
            boolean mapFolderExists = false;

            // Find map folder
            File target = worldManager.getWorldFolder(mapName);
            if (target.isDirectory()) {
                String[] list = target.list();
                if (list != null && list.length > 0) {
                    mapFolderExists = true;
                }
            }

            // Delete the folder if it exists and we're not using SWM
            if (mapFolderExists) {
                worldManager.deleteWorld(mapName, true);
            }

            // Copy the map folder back over
            File dataDirectory = MythicSkywars.get().getDataFolder();
            File maps = new File(dataDirectory, "maps");
            File source = new File(maps, name);

            worldManager.copyWorld(source, target);
        }
        else {
            worldManager.unloadWorld(mapName, false);
        }

        // Make sure no players are in this world before loading it
        MythicSkywars swr = MythicSkywars.get();
        Config swrConfig = MythicSkywars.getCfg();
        Location spawnLoc = swrConfig.getSpawn();
        World world = server.getWorld(mapName);
        // Remove all
        if (world != null) {
            for (Player player : server.getOnlinePlayers()) {
                if (player.getLocation().getWorld() == world) {
                    PlayerData pData = PlayerData.getPlayerData(player.getUniqueId());
                    if (pData == null) pData = new PlayerData(player);
                    pData.restoreToBeforeGameState(true);
                    // BungeeCord does not move players instantly, so we force them out
                    // of the world or kick them if spawn location isn't set
                    if (swrConfig.bungeeMode()) {
                        if (spawnLoc != null) player.teleport(spawnLoc);
                        else player.kickPlayer("");
                    }
                }
            }
        }

        boolean loaded = worldManager.loadWorld(mapName, World.Environment.valueOf(environment), true);

        if (loaded) {
            World worldLoaded = server.getWorld(mapName);
            // Debug
            if (swrConfig.debugEnabled()) {
                MythicSkywars.get().getLogger().info(this.getClass().getName() + "#loadMap: world null ? " + (worldLoaded == null));
            }

            // Remove all entities (mobs, animals, dropped items, etc.) from the arena
            for (org.bukkit.entity.Entity entity : worldLoaded.getEntities()) {
                if (!(entity instanceof Player)) {
                    entity.remove();
                }
            }

            // Disable mob spawning in the arena
            MythicSkywars.getNMS().setGameRule(worldLoaded, "doMobSpawning", "false");

            // Avoid vanilla spawn protection
            worldLoaded.setSpawnLocation(5000, 0, 5000);
            // Setup border if enabled
            if (swrConfig.borderEnabled()) {
                WorldBorder worldBorder = worldLoaded.getWorldBorder();
                CoordLoc firstSpawnLoc = teamCards.get(0).getSpawns().get(0);
                worldBorder.setCenter(firstSpawnLoc.getX(), firstSpawnLoc.getZ());
                worldBorder.setSize(swrConfig.getBorderSize());
            }

            // Remove diamond blocks (representing spawn locations) from the map
            for (List<CoordLoc> coords : spawnLocations.values()) {
                for (CoordLoc loc : coords) {
                    getCurrentWorld().getBlockAt(loc.getX(), loc.getY(), loc.getZ()).setType(Material.AIR);
                }
            }
            if (teamSize > 1) {
                cage.createSpawnPlatforms(this);
            }
        }
    }

    private void scanChunksForSkywarsFeatures(CommandSender sender, boolean message) {
        World chunkWorld;
        chunkWorld = MythicSkywars.get().getServer().getWorld(name);
        int mapSize = MythicSkywars.getCfg().getMaxMapSize();
        int max1 = mapSize / 2;
        int min1 = -mapSize / 2;
        Block min = chunkWorld.getBlockAt(min1, 0, min1);
        Block max = chunkWorld.getBlockAt(max1, 0, max1);
        Chunk cMin = min.getChunk();
        Chunk cMax = max.getChunk();
        teamCards.clear();
        spawnLocations.clear();
        chests.clear();

        List<Material> nonSpawnMaterials = Arrays.asList(Material.IRON_BLOCK, Material.GOLD_BLOCK, Material.EMERALD_BLOCK, Material.DIAMOND_BLOCK);

        for (int cx = cMin.getX(); cx < cMax.getX(); cx++) {
            for (int cz = cMin.getZ(); cz < cMax.getZ(); cz++) {
                Chunk currentChunk = chunkWorld.getChunkAt(cx, cz);
                currentChunk.load(true);

                for (BlockState blockState : currentChunk.getTileEntities()) {
                    if (blockState instanceof Beacon) {
                        Beacon beacon = (Beacon) blockState;
                        Block blockUnder = beacon.getBlock().getRelative(0, -1, 0);
                        if (blockUnder == null || !nonSpawnMaterials.contains(blockUnder.getType())) {
                            Location loc = beacon.getLocation();
                            if (teamSize == 1) {
                                addTeamCard(Lists.newArrayList(new CoordLoc(loc)));
                                if (message) {
                                    sender.sendMessage(new Messaging.MessageFormatter()
                                            .setVariable("num", "" + getMaxPlayers())
                                            .setVariable("mapname", getDisplayName())
                                            .format("maps.addSpawn"));
                                }
                            }
                        }
                        // Add chests found in the world to map chests
                    } else if (blockState instanceof Chest) {
                        Chest chest = (Chest) blockState;
                        // If loadTrappedChestsAsCenter option is enabled, we check if the chest is trapped. If so it's a center chest
                        if (MythicSkywars.getCfg().getLoadTrappedChestsAsCenter() &&
                                chest.getType().equals(Material.TRAPPED_CHEST)) {
                            Block trappedChestBlock = chest.getBlock();
                            if (trappedChestBlock != null) {
                                // Replace the trapped chest with a chest to avoid anomalies in-game & fix orientation
                                org.bukkit.material.Chest chestData = (org.bukkit.material.Chest) chest.getData();
                                BlockFace facing = chestData.getFacing();
                                trappedChestBlock.setType(Material.CHEST);
                                ((org.bukkit.material.Chest)trappedChestBlock.getState().getData()).setFacingDirection(facing);
                                // Add the chest as center
                                addChest(chest, ChestPlacementType.CENTER);
                            }
                        } else {
                            // Add the chest as normal
                            addChest(chest, ChestPlacementType.NORMAL);
                        }
                        if (message) {
                            // Inform the player scanning the map about found chests
                            sender.sendMessage(new Messaging.MessageFormatter().setVariable("mapname", getDisplayName()).format("maps.addChest"));
                        }
                    }
                }
            }
        }
    }

    public void refreshMap() {
        for (TeamCard tCard : teamCards) {
            tCard.reset();
        }
        thunder = false;
        forceStart = false;
        allowRegen = true;
        projectilesOnly = false;
        projectileSpleefEnabled = false;
        doubleDamageEnabled = false;
        kit = null;
        winners.clear();
        deathMatchWaiters.clear();
        waitingPlayers.clear();
        luckyModeEnabled = false;
        activeLuckyProfile = null;
        spectators.clear();
        playerKills.clear();
        if (MythicSkywars.getCfg().kitVotingEnabled()) {
            kitVoteOption.restore();
        }
        for (MatchEvent event : events) {
            event.reset();
        }
        healthOption.restore();
        chestOption.restore();
        timeOption.restore();
        weatherOption.restore();
        modifierOption.restore();
        gameboard.setRestartTimer(-1);
        MythicSkywars.getWM().deleteWorld(name, false);
        final GameMap gMap = this;
        if (MythicSkywars.get().isEnabled()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    gMap.loadMap();
                }
            }.runTaskLater(MythicSkywars.get(), 20);
        }
        if (MythicSkywars.get().isEnabled()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    // Ensure world is loaded before starting match
                    if (gMap.getCurrentWorld() == null) {
                        // Retry after another 40 ticks
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (teamSize > 1) {
                                    setMatchState(MatchState.WAITINGLOBBY);
                                } else {
                                    setMatchState(MatchState.WAITINGSTART);
                                }
                                gameboard.updateScoreboard();
                                MatchManager.get().start(gMap);
                                update();
                            }
                        }.runTaskLater(MythicSkywars.get(), 40);
                        return;
                    }
                    if (teamSize > 1) {
                        setMatchState(MatchState.WAITINGLOBBY);
                    } else {
                        setMatchState(MatchState.WAITINGSTART);
                    }
                    gameboard.updateScoreboard();
                    MatchManager.get().start(gMap);
                    update();
                }
            }.runTaskLater(MythicSkywars.get(), 60);
        }
    }

    /*Bungeemode Methods*/

    public void updateArenaManager() {
        if (MythicSkywars.getIC().has(arenakey)) {
            MythicSkywars.getIC().getMenu(arenakey).update();
        }
    }

    /*Sign Methods*/

    public void setKitVote(Player player, GameKit kit2) {
        for (TeamCard tCard : teamCards) {
            for (PlayerCard pCard : tCard.getPlayerCards()) {
                if (pCard.getPlayer() != null && pCard.getPlayer().equals(player)) {
                    pCard.setKitVote(kit2);
                    return;
                }
            }
        }
    }

    public GameKit getSelectedKit(Player player) {
        for (TeamCard tCard : teamCards) {
            for (PlayerCard pCard : tCard.getPlayerCards()) {
                if (pCard != null) {
                    if (pCard.getPlayer() != null && pCard.getPlayer().equals(player)) {
                        return pCard.getKitVote();
                    }
                }
            }
        }
        return null;
    }

    private void sendBungeeUpdate() {
        if (MythicSkywars.getCfg().bungeeMode()) {
            String playerCount = "" + this.getAlivePlayers().size();
            String maxPlayers = "" + this.getMaxPlayers();
            String gameStarted = "" + this.matchState.toString();
            ArrayList<String> messages = new ArrayList<>();
            messages.add("ServerUpdate");
            messages.add(MythicSkywars.get().getServerName());
            messages.add(playerCount);
            messages.add(maxPlayers);
            messages.add(gameStarted);
            Player player = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
            if (player != null) {
                MythicSkywars.get().sendSWRMessage(player, MythicSkywars.getCfg().getBungeeLobby(), messages);
            }
        }
    }

    public void updateSigns() {
        for (SWRSign s : signs) {
            s.update();
        }
    }

    public List<SWRSign> getSigns() {
        return this.signs;
    }



    /*Getter and Setter Methods*/

    public boolean hasSign(Location loc) {
        for (SWRSign s : signs) {
            if (s.getLocation().equals(loc)) {
                return true;
            }
        }
        return false;
    }

    public boolean removeSign(Location loc) {
        SWRSign sign = null;
        for (SWRSign s : signs) {
            if (s.getLocation().equals(loc)) {
                sign = s;
            }
        }
        if (sign != null) {
            signs.remove(sign);
            saveArenaData();
            updateSigns();
            return true;
        }
        return false;
    }

    public void addSign(Location loc) {
        signs.add(MythicSkywars.getNMS().createSWRSign(name, loc));
        saveArenaData();

        MythicSkywars.get().getServer().getScheduler().runTask(MythicSkywars.get(), this::updateSigns);
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(String displayName2) {
        this.displayName = displayName2;
        saveArenaData();
    }

    public String getDesigner() {
        return this.designedBy;
    }

    public ArrayList<CoordLoc> getChests() {
        return chests;
    }

    public ArrayList<CoordLoc> getCenterChests() {
        return centerChests;
    }

    public Vote getDefaultChestType() {
        return defaultChestType;
    }

    public Vote getDefaultHealth() {
        return defaultHealth;
    }

    public Vote getDefaultTime() {
        return defaultTime;
    }

    public Vote getDefaultWeather() {
        return defaultWeather;
    }

    public Vote getDefaultModifier() {
        return defaultModifier;
    }

    public MatchState getMatchState() {
        return this.matchState;
    }

    public void setMatchState(final MatchState state) {
        Bukkit.getPluginManager().callEvent(new MythicSkywarsMatchStateChangeEvent(this, state));
        this.matchState = state;
    }

    public int getPlayerCount() {
        int count = 0;
        for (TeamCard tCard : teamCards) {
            for (PlayerCard pCard : tCard.getPlayerCards()) {
                if (pCard.getPlayer() != null) count++;
            }
        }
        return count;
    }

    public int getMinTeams() {
        if (minPlayers == 0) {
            return teamCards.size();
        }
        return minPlayers;
    }

    public void setMinTeams(int x) {
        minPlayers = x;
        saveArenaData();
    }

    public int getTimer() {
        return this.timer;
    }

    public void setTimer(final int length) {
        if (MythicSkywars.getCfg().isDisplayPlayerExeperience()) {
            if (matchState != MatchState.PLAYING) {
                for (Player player : getAllPlayers()) {
                    player.setLevel(this.timer);
                }
            }
        }
        getGameBoard().updateScoreboard();
        this.timer = length;
    }

    public GameKit getKit() {
        return kit;
    }

    public void setKit(GameKit voted) {
        this.kit = voted;
    }

    public String getName() {
        return this.name;
    }

    public void setAllowFallDamage(boolean b) {
        allowFallDamage = b;
    }

    public boolean getAllowFallDamage() {
        return allowFallDamage;
    }

    public ArrayList<UUID> getSpectators() {
        return spectators;
    }

    public boolean isThunder() {
        return thunder;
    }

    public int getNextStrike() {
        return nextStrike;
    }

    public void setNextStrike(int randomNum) {
        nextStrike = randomNum;
    }

    public int getStrikeCounter() {
        return strikeCounter;
    }

    public void setStrikeCounter(int num) {
        strikeCounter = num;
    }

    /**
     * Returns the maximum number of players that can join a match.
     * For team games this matches scoreboard capacity ({@code teamCards × teamSize});
     * summing spawn coordinate lists only counts one point per island/team, which understated duo capacity.
     */
    public int getMaxPlayers() {
        if (teamSize > 1) {
            return teamCards.size() * teamSize;
        }
        int i = 0;
        for (List<CoordLoc> coords : spawnLocations.values()) {
            i += coords.size();
        }
        return i;
    }

    public void setThunderStorm(boolean b) {
        this.thunder = b;
    }

    public ArrayList<PlayerCard> getPlayerCards() {
        ArrayList<PlayerCard> cards = new ArrayList<>();
        for (TeamCard tCard : teamCards) {
            cards.addAll(tCard.getPlayerCards());
        }
        return cards;
    }

    public PlayerCard getPlayerCard(Player player) {
        for (TeamCard tCard : teamCards) {
            for (PlayerCard pCard : tCard.getPlayerCards()) {
                if (pCard.getPlayer() != null && pCard.getPlayer().equals(player)) {
                    return pCard;
                }
            }
        }
        return null;
    }

    public boolean getForceStart() {
        return forceStart;
    }

    public void setForceStart(boolean state) {
        forceStart = state;
    }

    public void setAllowRegen(boolean b) {
        allowRegen = b;
    }

    public boolean allowRegen() {
        return allowRegen;
    }

    public void addWinner(String name) {
        winners.add(name);
    }

    public boolean isRegistered() {
        return registered;
    }

    public void setRegistered(boolean b) {
        registered = b;
        saveArenaData();
        update();
    }

    public void setCreator(String creator) {
        this.designedBy = creator;
        saveArenaData();
    }

    public String getArenaKey() {
        return arenakey;
    }

    public ChestOption getChestOption() {
        return chestOption;
    }

    public GameOption getTimeOption() {
        return timeOption;
    }

    public GameOption getWeatherOption() {
        return weatherOption;
    }

    public GameOption getModifierOption() {
        return modifierOption;
    }

    public KitVoteOption getKitVoteOption() {
        return kitVoteOption;
    }

    public GameOption getHealthOption() {
        return healthOption;
    }

    public boolean isEditing() {
        return inEditing;
    }

    public void setEditing(boolean editingIn) {
        inEditing = editingIn;
        if (!editingIn) {
            ArenaSetupHologramManager.get().clear(this);
        }
    }

    public World getCurrentWorld() {
        return MythicSkywars.get().getServer().getWorld(name);
    }

    /**
     * Midpoint of all island spawn markers, used for localized world effects (e.g. snow biome)
     * that must not touch unrelated areas like the main lobby at world origin.
     */
    @Nullable
    public CoordLoc getApproximateIslandsCenter() {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        boolean any = false;
        for (TeamCard tc : teamCards) {
            List<CoordLoc> spawns = tc.getSpawns();
            if (spawns == null) {
                continue;
            }
            for (CoordLoc c : spawns) {
                any = true;
                minX = Math.min(minX, c.getX());
                maxX = Math.max(maxX, c.getX());
                minZ = Math.min(minZ, c.getZ());
                maxZ = Math.max(maxZ, c.getZ());
            }
        }
        if (!any) {
            return null;
        }
        return new CoordLoc((minX + maxX) / 2, 0, (minZ + maxZ) / 2);
    }

    /**
     * Removes dropped {@link Item} entities from this arena's world. Death drops would otherwise
     * remain visible during {@link MatchState#ENDING} while pickup is blocked.
     */
    public void clearDroppedItemsInArenaWorld() {
        World world = getCurrentWorld();
        if (world == null) {
            return;
        }
        ArrayList<Item> drops = new ArrayList<>();
        for (Item item : world.getEntitiesByClass(Item.class)) {
            drops.add(item);
        }
        for (Item item : drops) {
            item.remove();
        }
    }

    public CoordLoc getLookDirection() {
        return lookDirection;
    }

    public void setLookDirection(Location location) {
        lookDirection = new CoordLoc(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        saveArenaData();
    }

    public void setFriendlyFire(boolean state) {
        allowFriendlyFire = state;
        saveArenaData();
    }

    // ----- Team Card management -----

    /**
     * Add a team slot to the current GameMap
     * @param defaultSpawns List of initial CoordLocs to assign to the team card being created
     */
    public TeamCard addTeamCard(ArrayList<CoordLoc> defaultSpawns) {
        return addTeamCard(defaultSpawns, false);
    }

    public TeamCard addTeamCard(ArrayList<CoordLoc> defaultSpawns, boolean saveFile) {
        String prefix;
        int teamCardsSize = teamCards.size();

        ArrayList<CoordLoc> spawnLocs = defaultSpawns == null ?
                Lists.newArrayList() :
                Lists.newArrayList(defaultSpawns);

        prefix = getChatColor(teamCardsSize);

        // Add new team at index maxIndex + 1 which is also size()
        TeamCard createdTeamCard = new TeamCard(teamSize, this, prefix, getStringColor(teamCardsSize), teamCardsSize, Lists.newArrayList(spawnLocs));
        teamCards.add(createdTeamCard);
        spawnLocations.put(createdTeamCard, spawnLocs);
        if (saveFile) {
            saveArenaData();
        }
        refreshEditorSetupHolograms();
        return createdTeamCard;
    }

    public void addSpawnLocationForTeam(TeamCard teamCard, Location loc) {

        assert this.teamCards.contains(teamCard);

        CoordLoc coordLoc = new CoordLoc(loc);
        List<CoordLoc> spawnLocs;

        spawnLocs = spawnLocations.getOrDefault(teamCard, null);
        if (spawnLocs == null) spawnLocs = new ArrayList<>();

        spawnLocs.add(coordLoc);
        spawnLocations.put(teamCard, spawnLocs);
        teamCard.getSpawns().add(coordLoc);
        refreshEditorSetupHolograms();

    }

    public TeamCard getTeamCardByIndex(int teamIndex) {
        if (this.teamCards.size() > teamIndex) return this.teamCards.get(teamIndex);
        else return null;
    }

    public int getTeamCardPosition(TeamCard teamCard) {
        return this.teamCards.indexOf(teamCard);
    }

    private String getChatColor(int size) {
        double div14 = ((double) size + 1) / 14;
        long longDiv14 = (long) div14;
        double truncatedDiv14 = div14 - longDiv14;
        int remainderDiv14 = (int) (truncatedDiv14 * 14);
        switch (remainderDiv14) {
            // case 1 is the same as default
            case 2:
                return ChatColor.RED.toString();
            case 3:
                return ChatColor.DARK_BLUE.toString();
            case 4:
                return ChatColor.YELLOW.toString();
            case 5:
                return ChatColor.WHITE.toString();
            case 6:
                return ChatColor.AQUA.toString();
            case 7:
                return ChatColor.GRAY.toString();
            case 8:
                return ChatColor.DARK_PURPLE.toString();
            case 9:
                return ChatColor.DARK_GREEN.toString();
            case 10:
                return ChatColor.BLUE.toString();
            case 11:
                return ChatColor.DARK_GRAY.toString();
            case 12:
                return ChatColor.BLACK.toString();
            case 13:
                return ChatColor.LIGHT_PURPLE.toString();
            case 14:
                return ChatColor.GOLD.toString();
            default:
                return ChatColor.GREEN.toString();
        }
    }

    private String getStringColor(int index) {
        /*double d = ((double) size + 1) / 14;
        long i = (long) d;
        double f = d - i;
        int s = (int) (f * 14);*/
        switch (index) {
            // Case 0 is the same as default
            case 1:
                return "Red";
            case 2:
                return "Blue";
            case 3:
                return "Yellow";
            case 4:
                return "White";
            case 5:
                return "Cyan";
            case 6:
                return "Light Gray";
            case 7:
                return "Purple";
            case 8:
                return "Green";
            case 9:
                return "Light Blue";
            case 10:
                return "Gray";
            case 11:
                return "Black";
            case 12:
                return "Magenta";
            case 13:
                return "Orange";
            default:
                return "Lime";
        }
    }

    /**
     * Remove team slot by spawn location
     * @param loc Bukkit location of the spawn
     * @return
     *      Map of which, Keys are the teams removed and
     *      Values are the indexes of the spawn location removed from within that team
     *          (if no more spawns are left, team is completely removed from the map.)
     */
    public Map<TeamCard, List<Integer>> removeSpawnsAtLocation(Location loc) {
        CoordLoc locToRemove = new CoordLoc(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        // List of Cards to remove from and whether to remove that team completely
        HashMap<TeamCard, List<Integer>> toRemove = new HashMap<>();

        // Find teams that have matching spawn locs
        for (TeamCard tCard : this.teamCards) {
            if (tCard.getSpawns() == null || tCard.getSpawns().size() < 1) {
                toRemove.put(tCard, toRemove.getOrDefault(tCard, new ArrayList<>()));
            } else {
                for (int i = 0; i < tCard.getSpawns().size(); i++) {
                    if (tCard.getSpawns().get(i).equals(locToRemove)) {
                        // Add index to list
                        List<Integer> spawnIndexes = toRemove.getOrDefault(tCard, new ArrayList<>());
                        spawnIndexes.add(i);
                        // Mark as to remove from game map data
                        toRemove.put(tCard, spawnIndexes);
                    }
                }
            }
        }

        // Remove spawns if any found
        if (!toRemove.isEmpty()) {
            for (Map.Entry<TeamCard, List<Integer>> toRemoveEntry : toRemove.entrySet()) {
                // Remove all spawns with this location from team obj
                toRemoveEntry.getKey().getSpawns().remove(locToRemove);
                // Remove spawn locations from GameMap
                List<CoordLoc> coordsForTeam = this.spawnLocations.get(toRemoveEntry.getKey());
                if (coordsForTeam == null) continue;
                coordsForTeam.remove(locToRemove);
                // Should we remove the team completely?
                if (coordsForTeam.size() == 0) {
                    this.spawnLocations.remove(toRemoveEntry.getKey());
                    teamCards.remove(toRemoveEntry.getKey());
                }
            }
            // Save all after changes
            saveArenaData();
            refreshEditorSetupHolograms();
        }
        return toRemove;
        /*} else {
            boolean foundMatch = false;
            List<TeamCard> cardsToRemove = new ArrayList<>();
            List<CoordLoc> spawnsOfTeam = new ArrayList<>();
            TeamCard tCardAffected = null;

            for (TeamCard tCard : teamCards) {
                if (tCard.getSpawns().contains(locToRemove)) {
                    spawnsOfTeam = spawnLocations.get(tCard.getPosition());
                    spawnsOfTeam.remove(locToRemove);
                    // If card empty -> remove completely
                    if (spawnsOfTeam.size() == 0) {
                        cardsToRemove.add(tCard);
                    }
                    //addTeamCard(tCard.getPosition() + 1, true); -- remove ??
                    foundMatch = true;
                    tCardAffected = tCard;
                    break;
                }
            }
            // Remove empty ones
            for (TeamCard tCard : cardsToRemove) {
                teamCards.remove(tCard);
            }
            // Save if modified
            if (foundMatch) {
                saveArenaData();
                // Use size since the removed spawn location for that team no longer exists, hence it's index was at maxIndex + 1 = size()
                return new int[] { spawnsOfTeam.size(), tCardAffected.getPosition() };
            }
        }*/
    }

    public void addDeathMatchSpawn(Location loc) {
        addDeathMatchSpawn(new CoordLoc(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
        saveArenaData();
        refreshEditorSetupHolograms();
    }

    private void addDeathMatchSpawn(CoordLoc loc) {
        deathMatchSpawns.add(loc);
        saveArenaData();
    }

    public boolean removeDeathMatchSpawn(Location loc) {
        CoordLoc remove = new CoordLoc(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        boolean result = deathMatchSpawns.remove(remove);
        saveArenaData();
        refreshEditorSetupHolograms();
        return result;
    }

    public void addChest(Chest chest, ChestPlacementType cpt) {
        ArrayList<CoordLoc> list;
        if (cpt == ChestPlacementType.NORMAL) {
            list = chests;
        } else {
            list = centerChests;
        }
        InventoryHolder ih = chest.getInventory().getHolder();
        if (ih instanceof DoubleChest) {
            DoubleChest dc = (DoubleChest) ih;
            Chest left = (Chest) dc.getLeftSide();
            Chest right = (Chest) dc.getRightSide();
            CoordLoc locLeft = new CoordLoc(left.getX(), left.getY(), left.getZ());
            CoordLoc locRight = new CoordLoc(right.getX(), right.getY(), right.getZ());
            if (!(list.contains(locLeft) || list.contains(locRight))) {
                addChest(locLeft, cpt, true);
            }
        } else {
            CoordLoc loc = new CoordLoc(chest.getX(), chest.getY(), chest.getZ());
            if (!list.contains(loc)) {
                addChest(loc, cpt, true);
            }
        }
    }

    private void addChest(CoordLoc loc, ChestPlacementType cpt, boolean save) {
        ArrayList<CoordLoc> list;
        if (cpt == ChestPlacementType.NORMAL) {
            list = chests;
        } else {
            list = centerChests;
        }
        list.add(loc);
        if (save) {
            saveArenaData();
            refreshEditorSetupHolograms();
        }
    }

    public void removeChest(Chest chest) {
        InventoryHolder ih = chest.getInventory().getHolder();
        if (ih instanceof DoubleChest) {
            DoubleChest dc = (DoubleChest) ih;
            Chest left = (Chest) dc.getLeftSide();
            Chest right = (Chest) dc.getRightSide();
            CoordLoc locLeft = new CoordLoc(left.getX(), left.getY(), left.getZ());
            CoordLoc locRight = new CoordLoc(right.getX(), right.getY(), right.getZ());
            chests.remove(locLeft);
            centerChests.remove(locLeft);
            chests.remove(locRight);
            centerChests.remove(locRight);
        } else {
            CoordLoc loc = new CoordLoc(chest.getX(), chest.getY(), chest.getZ());
            chests.remove(loc);
            centerChests.remove(loc);
        }
        saveArenaData();
        refreshEditorSetupHolograms();

    }

    public int registerAllMapChestsAsNormal() {
        World world = getCurrentWorld();
        if (world == null) {
            return 0;
        }

        int mapSize = MythicSkywars.getCfg().getMaxMapSize();
        int max = mapSize / 2;
        int min = -mapSize / 2;
        Chunk cMin = world.getBlockAt(min, 0, min).getChunk();
        Chunk cMax = world.getBlockAt(max, 0, max).getChunk();

        Set<CoordLoc> detectedChests = new LinkedHashSet<>();
        for (int cx = cMin.getX(); cx < cMax.getX(); cx++) {
            for (int cz = cMin.getZ(); cz < cMax.getZ(); cz++) {
                Chunk currentChunk = world.getChunkAt(cx, cz);
                currentChunk.load(true);
                for (BlockState blockState : currentChunk.getTileEntities()) {
                    if (blockState instanceof Chest) {
                        Chest chest = (Chest) blockState;
                        InventoryHolder ih = chest.getInventory().getHolder();
                        if (ih instanceof DoubleChest) {
                            DoubleChest dc = (DoubleChest) ih;
                            Chest left = (Chest) dc.getLeftSide();
                            Chest right = (Chest) dc.getRightSide();
                            detectedChests.add(new CoordLoc(left.getX(), left.getY(), left.getZ()));
                            detectedChests.add(new CoordLoc(right.getX(), right.getY(), right.getZ()));
                        } else {
                            detectedChests.add(new CoordLoc(chest.getX(), chest.getY(), chest.getZ()));
                        }
                    }
                }
            }
        }

        chests.clear();
        centerChests.clear();
        chests.addAll(detectedChests);
        saveArenaData();
        refreshEditorSetupHolograms();
        return chests.size();
    }

    public CoordLoc getSpectateSpawn() {
        return spectateSpawn;
    }

    public void setSpectateSpawn(Location location) {
        spectateSpawn = new CoordLoc(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        saveArenaData();
        refreshEditorSetupHolograms();
    }

    public void clearSpectateSpawn() {
        spectateSpawn = null;
        saveArenaData();
        refreshEditorSetupHolograms();
    }

    public void saveMap(CommandSender toMsg) {
        exitEditMode(toMsg, true);
    }

    public void exitEditMode(CommandSender toMsg, boolean saveChanges) {
        boolean success = false;
        Location respawn = MythicSkywars.getCfg().getSpawn();
        World editWorld = MythicSkywars.get().getServer().getWorld(name);
        if (editWorld != null) {
            for (Player player : editWorld.getPlayers()) {
                player.teleport(respawn, TeleportCause.PLUGIN);
                PlayerStat.resetScoreboard(player);
                if (MythicSkywars.getCfg().lobbyBoardEnabled() && !MythicSkywars.getCfg().bungeeMode()) {
                    PlayerStat.updateScoreboard(player, "lobbyboard");
                }
            }
            File source = MythicSkywars.getWM().getWorldFolder(name);
            MythicSkywars.getWM().unloadWorld(name, saveChanges);
            if (saveChanges) {
                File dataDirectory = new File(MythicSkywars.get().getDataFolder(), "maps");
                File target = new File(dataDirectory, name);
                MythicSkywars.getWM().deleteWorld(target);
                MythicSkywars.getWM().copyWorld(source, target);
                if (toMsg != null) {
                    toMsg.sendMessage(new Messaging.MessageFormatter().setVariable("mapname", name).format("maps.saved"));
                    toMsg.sendMessage(new Messaging.MessageFormatter().format("maps.register-reminder"));
                }
                saveArenaData();
            } else if (toMsg != null) {
                toMsg.sendMessage(new Messaging.MessageFormatter().setVariable("mapname", name).format("maps.editor.left-without-saving"));
            }
            MythicSkywars.getWM().deleteWorld(source);
            inEditing = false;
            success = true;
        }

        if (toMsg != null && !success) {
            toMsg.sendMessage(new Messaging.MessageFormatter().setVariable("mapname", name).format("error.map-not-in-edit"));
        }
    }

    public ArrayList<MatchEvent> getEvents() {
        return events;
    }

    public ArrayList<CoordLoc> getDeathMatchSpawns() {
        return deathMatchSpawns;
    }

    public HashMap<TeamCard, List<CoordLoc>> getSpawnLocations() {
        return this.spawnLocations;
    }

    public void removeDMSpawnBlocks() {
        for (CoordLoc loc : deathMatchSpawns) {
            World world = getCurrentWorld();
            Location loca = new Location(world, loc.getX(), loc.getY(), loc.getZ());
            world.getBlockAt(loca).setType(Material.AIR);
        }
    }

    public ArrayList<String> getDeathMatchWaiters() {
        return deathMatchWaiters;
    }

    public void addDeathMatchWaiter(Player player) {
        if (player != null) {
            deathMatchWaiters.add(player.getUniqueId().toString());
        }
    }

    public void clearDeathMatchWaiters() {
        deathMatchWaiters.clear();
    }

    public ArrayList<String> getAnvils() {
        return anvils;
    }

    public void addCrate(Location loc, int max) {
        crates.add(new Crate(loc, max));
    }

    public void removeCrates() {
        for (Crate crate : crates) {
            if (crate.getLocation() != null) {
                crate.getLocation().getWorld().getBlockAt(crate.getLocation()).setType(Material.AIR);
            }
        }
        crates.clear();
    }

    public ArrayList<Crate> getCrates() {
        return crates;
    }

    public Cage getCage() {
        return cage;
    }

    public void setCage(CageType next) {
        switch (next) {
            case CUBE:
                this.cage = new CubeCage();
                break;
            case DOME:
                this.cage = new DomeCage();
                break;
            case PYRAMID:
                this.cage = new PyramidCage();
                break;
            case SPHERE:
                this.cage = new SphereCage();
                break;
            // Case STANDARD is the same as default
            default:
                this.cage = new StandardCage();
        }
        saveArenaData();
    }

    public ArrayList<TeamCard> getTeamCards() {
        return teamCards;
    }

    GameQueue getJoinQueue() {
        return joinQueue;
    }

    public TeamCard getTeamCard(Player player) {
        for (TeamCard tCard : teamCards) {
            if (tCard.containsPlayer(player.getUniqueId()) != null) {
                return tCard;
            }
        }
        return null;
    }

    public int getNumTeamsOut() {
        int count = 0;
        for (TeamCard tCard : teamCards) {
            if (tCard.isEliminated()) {

                count++;
            }
        }
        return count;
    }

    public int getTeamsLeft() {
        return teamCards.size() - getNumTeamsOut();
    }

    public int getTeamSize() {
        return teamSize;
    }

    public void setTeamSize(int size) {
        teamSize = size;
        for (TeamCard tCard : teamCards) {
            tCard.updateCard(size);
        }
        saveArenaData();
    }

    public TeamCard getWinningTeam() {
        for (TeamCard tCard : teamCards) {
            if (!tCard.isEliminated()) {
                return tCard;
            }
        }
        return null;
    }

    public int getFullTeams() {
        int count = 0;
        for (TeamCard tCard : teamCards) {
            if (tCard.isFull()) {
                count++;
            }
        }
        return count;
    }

    public String getCurrentChest() {
        return currentChest;
    }

    public void setCurrentChest(String voteString) {
        currentChest = voteString;
    }

    public String getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(String voteString) {
        currentTime = voteString;
    }

    public String getCurrentHealth() {
        return currentHealth;
    }

    public void setCurrentHealth(String voteString) {
        currentHealth = voteString;
    }

    public String getCurrentWeather() {
        return currentWeather;
    }

    public void setCurrentWeather(String voteString) {
        currentWeather = voteString;
    }

    public String getCurrentModifier() {
        return currentModifier;
    }

    public void setCurrentModifier(String voteString) {
        currentModifier = voteString;
    }

    public List<String> getWinners() {
        return winners;
    }

    public GameBoard getGameBoard() {
        return gameboard;
    }

    public TeamCard getTeamCardFromName(String name) {
        for (TeamCard tCard : teamCards) {
            if (tCard.getTeamName().equalsIgnoreCase(name)) {
                return tCard;
            }
        }
        return null;
    }

    public boolean getProjectilesOnly() {
        return projectilesOnly;
    }

    public void setProjectilesOnly(boolean b) {
        projectilesOnly = b;
    }

    public boolean isProjectileSpleefEnabled() {
        return projectileSpleefEnabled;
    }

    public void setProjectileSpleefEnabled(boolean b) {
        projectileSpleefEnabled = b;
    }

    public boolean isDoubleDamageEnabled() {
        return doubleDamageEnabled;
    }

    public void setDoubleDamageEnabled(boolean b) {
        doubleDamageEnabled = b;
    }

    public boolean allowFriendlyFire() {
        return allowFriendlyFire;
    }

    public ChestPlacementType getChestPlacementType() {
        return chestPlacementType;
    }

    public void setChestPlacementType(ChestPlacementType chestPlacementType) {
        this.chestPlacementType = chestPlacementType;
    }

    public boolean isDisableDamage() {
        return disableDamage;
    }

    public void setDisableDamage(boolean b) {
        disableDamage = b;
    }

    public CoordLoc getWaitingLobbySpawn() {
        return waitingLobbySpawn;
    }

    public boolean isLuckyModeEnabled() {
        return luckyModeEnabled;
    }

    public void setLuckyModeEnabled(boolean luckyModeEnabled) {
        this.luckyModeEnabled = luckyModeEnabled;
    }

    @Nullable
    public LuckyBlockHook.LuckyProfile getActiveLuckyProfile() {
        return activeLuckyProfile;
    }

    public void setActiveLuckyProfile(@Nullable LuckyBlockHook.LuckyProfile activeLuckyProfile) {
        this.activeLuckyProfile = activeLuckyProfile;
    }

    public void setWaitingLobbySpawn(Location location) {
        waitingLobbySpawn = new CoordLoc(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        saveArenaData();
        refreshEditorSetupHolograms();
    }

    public void clearWaitingLobbySpawn() {
        waitingLobbySpawn = null;
        saveArenaData();
        refreshEditorSetupHolograms();
    }

    private void refreshEditorSetupHolograms() {
        if (isEditing()) {
            ArenaSetupHologramManager.get().requestRefresh(this);
        }
    }

    public void addWaitingPlayer(Player player) {
        waitingPlayers.add(player.getUniqueId());
    }

    public void removeWaitingPlayer(UUID uid) {
        waitingPlayers.remove(uid);
    }

    public void clearWaitingPlayers() {
        waitingPlayers = Lists.newArrayList();
    }

    public ArrayList<UUID> getWaitingPlayers() {
        return waitingPlayers;
    }

    public boolean isJoinQueued(UUID uuid) {
        return joinQueue != null && joinQueue.containsPlayer(uuid);
    }

    public int getPlayerKills(Player p) {
        return playerKills.getOrDefault(p, 0);
    }

    public void increaseDisplayedKillsVar(Player p) {
        int newKills = getPlayerKills(p) + 1;
        playerKills.remove(p);
        playerKills.put(p, newKills);
    }

    public MatchEvent getNextEvent() {
        MatchEvent latest = null;
        int earliestStartTime = Integer.MAX_VALUE;
        for (MatchEvent event : getEvents()) {
            int startTime = event.getStartTime();
            if (event.isEnabled() && startTime > getTimer()) {
                if (startTime < earliestStartTime) {
                    earliestStartTime = startTime;
                    latest = event;
                }
            }
        }

        return latest;
    }

    public void setEnvironment(String env) {
        this.environment = env;
    }

    public static class TeamCardComparator implements Comparator<TeamCard> {
        @Override
        public int compare(TeamCard f1, TeamCard f2) {
            return Integer.compare(f1.getEmptySlots(), f2.getEmptySlots());
        }
    }

    public static class GameMapCreationResult {

        private boolean validName;
        private World world;

        public GameMapCreationResult(boolean validNameIn, @Nullable World worldIn) {
            this.validName = validNameIn;
            this.world = worldIn;
        }

        public boolean isValidName() {
            return this.validName;
        }
        @Nullable
        public World getWorld() {
            return this.world;
        }
    }
}
