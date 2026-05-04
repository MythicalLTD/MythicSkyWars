package systems.mythical.mythicskywars.managers;

import com.google.common.collect.ImmutableList;
import systems.mythical.mythicskywars.clients.lunar.LunarApolloBridge;
import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.enums.GameType;
import systems.mythical.mythicskywars.enums.MatchState;
import systems.mythical.mythicskywars.enums.PlayerRemoveReason;
import systems.mythical.mythicskywars.enums.ScoreVar;
import systems.mythical.mythicskywars.events.MythicSkywarsWinEvent;
import systems.mythical.mythicskywars.game.*;
import systems.mythical.mythicskywars.game.cages.schematics.SchematicCage;
import systems.mythical.mythicskywars.matchevents.MatchEvent;
import systems.mythical.mythicskywars.menus.MapSearchListener;
import systems.mythical.mythicskywars.menus.gameoptions.objects.CoordLoc;
import systems.mythical.mythicskywars.menus.gameoptions.objects.GameKit;
import systems.mythical.mythicskywars.menus.playeroptions.ParticleEffectOption;
import systems.mythical.mythicskywars.menus.playeroptions.WinSoundOption;
import systems.mythical.mythicskywars.menus.playeroptions.objects.ParticleEffect;
import systems.mythical.mythicskywars.utilities.LuckyBlockBreakAttachment;
import systems.mythical.mythicskywars.utilities.LuckyBlockHook;
import systems.mythical.mythicskywars.utilities.Messaging;
import systems.mythical.mythicskywars.utilities.Party;
import systems.mythical.mythicskywars.utilities.LevelManager;
import systems.mythical.mythicskywars.utilities.Util;
import systems.mythical.mythicskywars.utilities.VaultUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MatchManager {

    private static MatchManager instance = null;
    private static final Map<UUID, RejoinState> rejoinStates = new HashMap<>();

    private int waitTime;
    private int gameTime;
    //private String debugName;
    private boolean debug;

    public MatchManager() {
        debug = MythicSkywars.getCfg().debugEnabled();
        instance = this;
    }

    public static MatchManager get() {
        if (MatchManager.instance == null) {
            MatchManager.instance = new MatchManager();
        }
        return MatchManager.instance;
    }

    @SuppressWarnings("unused") // API
    public static void setMatchManager(MatchManager manager) {
        MatchManager.instance = manager;
    }

    /**
     * Include online player into game
     * @param player The player to add
     * @param type Type of game (teams, single, etc..)
     * @return GameMap The map that was successfully joined or null
     */
    public GameMap joinGame(Player player, GameType type) {
        return joinGame(player, type, false, true);
    }

    /**
     * @param allowSpectatorBypassOnFailure when false, players with {@code sw.admin.joinBypass} are not
     *                                      forced in as spectators if no arena accepts them (e.g. lobby portal).
     */
    public GameMap joinGame(Player player, GameType type, boolean allowSpectatorBypassOnFailure) {
        return joinGame(player, type, false, allowSpectatorBypassOnFailure);
    }

    public GameMap joinGame(Player player, GameType type, boolean luckyMode, boolean allowSpectatorBypassOnFailure) {
        GameMap currentMap = getPlayerMap(player);
        if (currentMap != null) {
            return currentMap;
        }

        ArrayList<GameMap> games = MythicSkywars.getGameMapMgr().getPlayableArenas(type);
        ArrayList<GameMap> eligible = new ArrayList<>();
        for (final GameMap gameMap : games) {
            if (MythicSkywars.getCfg().debugEnabled()) {
                MythicSkywars.get().getLogger().info("#joinGame: --game: " + gameMap.getName());
            }
            if (gameMap.canAddPlayer(player) && canJoinLuckyMode(gameMap, luckyMode)) {
                eligible.add(gameMap);
            }
        }
        eligible = prioritizeAutojoinMaps(eligible);

        GameMap map = null;
        for (final GameMap gameMap : eligible) {
            if (luckyMode && gameMap.getPlayerCount() == 0) {
                gameMap.setLuckyModeEnabled(true);
            }
            if (gameMap.addPlayers(null, player)) {
                map = gameMap;
                break;
            }
        }

        if (map == null) {
            if (MythicSkywars.getCfg().debugEnabled()) {
                MythicSkywars.get().getLogger().info("#joinGame: --map = null:");
            }
            // Allow joining as spec if you have permission (menus/commands only; not lobby water portal)
            if (allowSpectatorBypassOnFailure && player.hasPermission("sw.admin.joinBypass")) { // TODO: Not fully tested, issues may arise
                if (!games.isEmpty()) {
                    GameMap gameMap = games.get(0);
                    MythicSkywars.get().getPlayerManager().addSpectator(gameMap, player);
                    map = gameMap;
                }
            }

        }
        return map;
    }

    /**
     * Join a specific registered arena by internal name or display name (bypasses auto-pick order).
     */
    @Nullable
    public GameMap joinGame(Player player, GameType type, String mapName) {
        return joinGame(player, type, mapName, false);
    }

    @Nullable
    public GameMap joinGame(Player player, GameType type, String mapName, boolean luckyMode) {
        GameMap currentMap = getPlayerMap(player);
        if (currentMap != null) {
            return currentMap;
        }

        if (mapName == null || mapName.trim().isEmpty()) {
            return joinGame(player, type, luckyMode, true);
        }
        String token = ChatColor.stripColor(mapName).trim();
        GameMap map = MythicSkywars.getGameMapMgr().getMap(token);
        if (map == null) {
            map = MythicSkywars.getGameMapMgr().getMapByDisplayName(token);
        }
        if (map == null || !map.isRegistered()) {
            return null;
        }
        if (type == GameType.SINGLE && map.getTeamSize() != 1) {
            return null;
        }
        if (type == GameType.TEAM && map.getTeamSize() <= 1) {
            return null;
        }
        if (!map.canAddPlayer(player)) {
            return null;
        }
        if (!canJoinLuckyMode(map, luckyMode)) {
            return null;
        }
        if (luckyMode && map.getPlayerCount() == 0) {
            map.setLuckyModeEnabled(true);
        }
        if (map.addPlayers(null, player)) {
            return map;
        }
        return null;
    }

    /**
     * Party leader joins a specific arena by name or display name.
     */
    @Nullable
    public GameMap joinGame(Party party, GameType type, String mapName) {
        return joinGame(party, type, mapName, false);
    }

    @Nullable
    public GameMap joinGame(Party party, GameType type, String mapName, boolean luckyMode) {
        if (mapName == null || mapName.trim().isEmpty()) {
            return joinGame(party, type, luckyMode);
        }
        String token = ChatColor.stripColor(mapName).trim();
        GameMap map = MythicSkywars.getGameMapMgr().getMap(token);
        if (map == null) {
            map = MythicSkywars.getGameMapMgr().getMapByDisplayName(token);
        }
        if (map == null || !map.isRegistered()) {
            return null;
        }
        if (type == GameType.SINGLE && map.getTeamSize() != 1) {
            return null;
        }
        if (type == GameType.TEAM && map.getTeamSize() <= 1) {
            return null;
        }
        if (!map.canAddParty(party)) {
            return null;
        }
        if (!canJoinLuckyMode(map, luckyMode)) {
            return null;
        }
        if (luckyMode && map.getPlayerCount() == 0) {
            map.setLuckyModeEnabled(true);
        }
        if (map.addPlayers(null, party)) {
            return map;
        }
        return null;
    }

    /**
     * Prefer arenas in {@link MatchState#WAITINGSTART} (countdown / about to start), then
     * {@link MatchState#WAITINGLOBBY}; among equals prefer more players already queued.
     */
    private static int getEffectivePlayerCount(GameMap map) {
        int count = map.getPlayerCount();
        if (map.getMatchState() == MatchState.WAITINGLOBBY) {
            count += map.getWaitingPlayers().size();
        }
        return count;
    }

    private static void sortMapsForAutojoin(List<GameMap> maps) {
        Collections.sort(maps, new Comparator<GameMap>() {
            @Override
            public int compare(GameMap a, GameMap b) {
                int ra = rankWaitingState(a.getMatchState());
                int rb = rankWaitingState(b.getMatchState());
                if (ra != rb) {
                    return Integer.compare(ra, rb);
                }
                return Integer.compare(getEffectivePlayerCount(b), getEffectivePlayerCount(a));
            }
        });
    }

    /**
     * Prefer arenas that already have players; if all are empty, randomize empty arenas.
     * For team mode (WAITINGLOBBY), also counts players in the waiting list so that
     * auto-join funnels new players into arenas that already have people queued.
     */
    private static ArrayList<GameMap> prioritizeAutojoinMaps(List<GameMap> maps) {
        ArrayList<GameMap> withPlayers = new ArrayList<>();
        ArrayList<GameMap> empty = new ArrayList<>();
        for (GameMap map : maps) {
            int effectiveCount = map.getPlayerCount();
            // In WAITINGLOBBY, players are in the waiting list, not assigned to team cards yet
            if (map.getMatchState() == MatchState.WAITINGLOBBY) {
                effectiveCount += map.getWaitingPlayers().size();
            }
            if (effectiveCount > 0) {
                withPlayers.add(map);
            } else {
                empty.add(map);
            }
        }
        if (!withPlayers.isEmpty()) {
            sortMapsForAutojoin(withPlayers);
            return withPlayers;
        }
        Collections.shuffle(empty);
        return empty;
    }

    private static int rankWaitingState(MatchState s) {
        if (s == MatchState.WAITINGSTART) {
            return 0;
        }
        if (s == MatchState.WAITINGLOBBY) {
            return 1;
        }
        return 99;
    }

    /**
     * Add all players from party into game
     * @param party The party
     * @param type The game type
     * @return GameMap The map that was successfully joined or null
     */
    public GameMap joinGame(Party party, GameType type) {
        return joinGame(party, type, false);
    }

    public GameMap joinGame(Party party, GameType type, boolean luckyMode) {
        ArrayList<GameMap> games;
        if (type == GameType.ALL) {
            games = MythicSkywars.getGameMapMgr().getPlayableArenas(GameType.ALL);
        } else if (type == GameType.SINGLE) {
            games = MythicSkywars.getGameMapMgr().getPlayableArenas(GameType.SINGLE);
        } else {
            games = MythicSkywars.getGameMapMgr().getPlayableArenas(GameType.TEAM);
        }

        ArrayList<GameMap> eligible = new ArrayList<>();
        for (final GameMap gameMap : games) {
            if (gameMap.canAddParty(party) && canJoinLuckyMode(gameMap, luckyMode)) {
                eligible.add(gameMap);
            }
        }
        eligible = prioritizeAutojoinMaps(eligible);

        GameMap map = null;
        for (final GameMap gameMap : eligible) {
            if (luckyMode && gameMap.getPlayerCount() == 0) {
                gameMap.setLuckyModeEnabled(true);
            }
            if (gameMap.addPlayers(null, party)) {
                map = gameMap;
                break;
            }
        }

        // Return which map we joined
        return map;
    }

    private static boolean canJoinLuckyMode(GameMap gameMap, boolean luckyMode) {
        if (gameMap == null) {
            return false;
        }
        // Lucky mode is only supported for solo (teamSize == 1)
        if (luckyMode && gameMap.getTeamSize() > 1) {
            return false;
        }
        if (luckyMode && !LuckyBlockHook.isAvailable()) {
            return false;
        }
        if (gameMap.getPlayerCount() == 0) {
            return true;
        }
        return gameMap.isLuckyModeEnabled() == luckyMode;
    }

    public void start(final GameMap gameMap) {
        debug = MythicSkywars.getCfg().debugEnabled();
        if (gameMap == null) {
            return;
        }
        gameMap.removeDMSpawnBlocks();
        this.setWaitTime(MythicSkywars.getCfg().getWaitTimer());
        this.setGameTime();
        if (gameMap.getTeamSize() == 1) {
            gameMap.setMatchState(MatchState.WAITINGSTART);
        } else {
            gameMap.setMatchState(MatchState.WAITINGLOBBY);
        }
        gameMap.update();
        gameMap.getGameBoard().updateScoreboard();
        this.waitStart(gameMap);
    }


    public void message(final GameMap gameMap, final String message) {
        this.message(gameMap, message, null);
    }

    public void message(@NotNull final GameMap gameMap, final String message, @Nullable Player skip) {
        World w = gameMap.getCurrentWorld();
        if (w == null) return;
        List<Player> worldPlayers = w.getPlayers();
        if (worldPlayers != null && !worldPlayers.isEmpty()) {
            if (debug) {
                Util.get().logToFile(getDebugName(gameMap) + ChatColor.YELLOW + "Message from [" + gameMap.getName() + "]: " + message);
            }
            for (Player player : worldPlayers) {
                if (player != null && player != skip) {
                    player.sendMessage(message);
                }
            }
        }
    }

    public void teleportToArena(final GameMap gameMap, PlayerCard pCard) {
        if (pCard == null || pCard.getPlayer() == null) {
            return;
        }
        if (!gameMap.getMatchState().equals(MatchState.WAITINGLOBBY) && !gameMap.getMatchState().equals(MatchState.WAITINGSTART)) {
            pCard.reset();
            return;
        }
        if (gameMap.getMatchState().equals(MatchState.WAITINGSTART)) {
            TeamCard tc = pCard.getTeamCard();
            if (tc == null || tc.getSpawns() == null || pCard.getSpawn() == null) {
                MythicSkywars.get().getLogger().warning("teleportToArena: Player " + pCard.getPlayer().getName() + " has no spawn assigned on map " + gameMap.getName() + "! Resetting.");
                pCard.reset();
                return;
            }
        }

        Player player = pCard.getPlayer();
        // Cancel any active arena search so chat isn't intercepted
        MapSearchListener.cancelSearch(player);

        if (PlayerData.getPlayerData(player.getUniqueId()) == null) {
            PlayerData.getAllPlayerData().add(new PlayerData(player));
        }
        World world = gameMap.getCurrentWorld();
        Location spawn;

        if (gameMap.getMatchState().equals(MatchState.WAITINGLOBBY)) {
            CoordLoc lobbySpawn = gameMap.getWaitingLobbySpawn();
            spawn = new Location(world, lobbySpawn.getX() + 0.5, lobbySpawn.getY() + 1, lobbySpawn.getZ() + 0.5);

            if (debug) {
                Util.get().logToFile(getDebugName(gameMap) + ChatColor.YELLOW + "Teleporting " + player.getName() + " to the waiting lobby on map " + gameMap.getName());
            }

            if (!world.isChunkLoaded(world.getChunkAt(spawn))) {
                world.loadChunk(world.getChunkAt(spawn));
            }
        } else {
            CoordLoc sspawn = pCard.getSpawn();
            if (debug) {
                Util.get().logToFile(getDebugName(gameMap) + ChatColor.YELLOW + "Teleporting " + player.getName() + " to Skywars on map " + gameMap.getName());
            }
            spawn = new Location(world, sspawn.getX() + 0.5, sspawn.getY() + 1, sspawn.getZ() + 0.5);
            PlayerStat pStat = PlayerStat.getPlayerStats(player);
            if (pStat != null &&
                    pStat.getGlassColor() != null &&
                    pStat.getGlassColor().startsWith("custom-")
            ) {
                spawn = new Location(world, sspawn.getX() + 0.5, sspawn.getY() + 0.25, sspawn.getZ() + 0.5);
            }

            // Ensure the chunk at the spawn is loaded before teleporting
            if (!world.isChunkLoaded(world.getChunkAt(spawn))) {
                world.loadChunk(world.getChunkAt(spawn));
            }
        }

        // Trust the configured spawn location — the cage system places blocks at this
        // exact position. Do NOT run safe-spawn resolution here as it incorrectly rejects
        // glass cages (transparent blocks) and relocates players to the ground.
        player.teleport(spawn, TeleportCause.END_PORTAL);


        if (MythicSkywars.getCfg().getLookDirectionEnabled() && gameMap.getMatchState().equals(MatchState.WAITINGSTART)) {
            if (gameMap.getCurrentWorld() == player.getWorld()) {
                CoordLoc coordLoc = gameMap.getLookDirection();
                Location location = new Location(gameMap.getCurrentWorld(), coordLoc.getX(), coordLoc.getY(), coordLoc.getZ());
                Vector vec = location.clone().subtract(player.getEyeLocation()).toVector();
                Location locationFinal = player.getLocation().setDirection(vec);
                player.teleport(locationFinal, TeleportCause.END_PORTAL);
            }
        }

        Util.get().clear(player);
        player.setGameMode(GameMode.ADVENTURE);
        if (MythicSkywars.getCfg().debugEnabled()) MythicSkywars.get().getLogger().info("MatchManager::teleportToArena allowing flight for " + player.getName() + " to prevent falling... (will be removed in 2s)");
        player.setVelocity(new Vector(0, 0, 0));
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setFlySpeed(0f);
        new BukkitRunnable() {
            @Override
            public void run() {
                preparePlayer(player, gameMap);
            }
        }.runTaskLater(MythicSkywars.get(), 5);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (MythicSkywars.getCfg().debugEnabled()) MythicSkywars.get().getLogger().info("MatchManager::teleportToArena removing flight for " + player.getName());
                player.setFlying(false);
                player.setAllowFlight(false);
                player.setFlySpeed(0.1f);
            }
        }.runTaskLater(MythicSkywars.get(), 40);

        // Load player stats & options
        PlayerStat ps = PlayerStat.getPlayerStats(player.getUniqueId());
        if (ps != null) {
            String key = ps.getParticleEffect();
            ParticleEffectOption effect = (ParticleEffectOption) ParticleEffectOption.getPlayerOptionByKey(key);
            if (effect != null) {
                List<ParticleEffect> effects = effect.getEffects();
                MythicSkywars.getOM().addPlayer(player.getUniqueId(), effects);
            }
        }

        if (!gameMap.getAlivePlayers().contains(player) || gameMap.getTeamSize() == 1) {
            // Add 1 to join index since we want 1-max and not 0-maxIndex
            String playerCount = gameMap.getMatchState() == MatchState.WAITINGLOBBY ? gameMap.getWaitingPlayers().size() + "" : String.valueOf(pCard.getJoinIndex() + 1); // String.valueOf(gameMap.getAllPlayers().size());
            // Send join message to all
            for (final Player p : gameMap.getAllPlayers()) {
                p.sendMessage(new Messaging.MessageFormatter().setVariable("player", player.getDisplayName())
                        .setVariable("players", playerCount)
                        .setVariable("playercount", playerCount)
                        .setVariable("maxplayers", "" + gameMap.getMaxPlayers()).format("game.waitstart-joined-the-game"));
            }
        }

        for (final Player p : gameMap.getAlivePlayers()) {
            if (!p.equals(player)) {
                Util.get().playSound(p, p.getLocation(), MythicSkywars.getCfg().getJoinSound(), 1, 1);
            }
        }

        if (debug) {
            if (gameMap.getAlivePlayers().size() < gameMap.getMinTeams()) {
                Util.get().logToFile(getDebugName(gameMap) + ChatColor.YELLOW + "Waiting for More Players on map " + gameMap.getName());
            } else {
                Util.get().logToFile(getDebugName(gameMap) + ChatColor.YELLOW + "Starting Countdown for SkyWars Match on map " + gameMap.getName());
            }
        }

        gameMap.setMatchState(gameMap.getMatchState());
        if (MythicSkywars.getCfg().titlesEnabled()) {
            String subtitle = new Messaging.MessageFormatter()
                    .setVariable("map", gameMap.getName())
                    .setVariable("designer", gameMap.getDesigner())
                    .setVariable("creator", gameMap.getDesigner())
                    .format("titles.join-subtitle");
            String maintitle = new Messaging.MessageFormatter()
                    .setVariable("map", gameMap.getDisplayName())
                    .setVariable("designer", gameMap.getDesigner())
                    .setVariable("creator", gameMap.getDesigner()).format("titles.join-title");


            Util.get().sendTitle(player, 15, 60, 15, maintitle,
                    subtitle);
        }
    }

    private void preparePlayer(Player player, GameMap gameMap) {
        if (debug) {
            Util.get().logToFile(getDebugName(gameMap) + ChatColor.YELLOW + "Preparing " + player.getName() + " for SkyWars");
        }
        // Safety: if the player enters while match already switched to PLAYING, force SURVIVAL.
        // This avoids rare "first match stuck in ADVENTURE" state caused by timing between teleport/prepare/start.
        if (gameMap.getMatchState() == MatchState.PLAYING && player.getGameMode() != GameMode.SURVIVAL) {
            player.setGameMode(GameMode.SURVIVAL);
        }
        player.setFoodLevel(20);
        player.setSaturation(0f);
        player.setHealth(20.0);
        player.setExp(0.0f);
        player.setLevel(0);

        /*if (!MythicSkywars.getNMS().removeFromScoreboardCollection(player.getScoreboard())) { //1.13+
            player.setScoreboard(MythicSkywars.get().getServer().getScoreboardManager().getNewScoreboard());
        }*/
        gameMap.getGameBoard().updateScoreboard();

        Util.get().clear(player);
        player.getInventory().setBoots(new ItemStack(Material.AIR, 1));
        player.getInventory().setChestplate(new ItemStack(Material.AIR, 1));
        player.getInventory().setHelmet(new ItemStack(Material.AIR, 1));
        player.getInventory().setLeggings(new ItemStack(Material.AIR, 1));

        if (MythicSkywars.getCfg().areKitsEnabled()) {
            ItemStack kitItem = MythicSkywars.getIM().getItem("kitvote");
            player.getInventory().setItem(MythicSkywars.getCfg().getKitVotePos(), kitItem);
        }

        if (MythicSkywars.getCfg().votingEnabled()) {
            ItemStack timeItem = MythicSkywars.getIM().getItem("votingItem");
            player.getInventory().setItem(MythicSkywars.getCfg().getVotingPos(), timeItem);
        }

        if (gameMap.getTeamSize() > 1 && gameMap.getMatchState() == MatchState.WAITINGLOBBY) {
            ItemStack teamItem = MythicSkywars.getIM().getItem("teamSelectItem");
            player.getInventory().setItem(MythicSkywars.getCfg().getTeamSelectPos(), teamItem);
        }

        ItemStack exitItem = MythicSkywars.getIM().getItem("exitGameItem");
        player.getInventory().setItem(MythicSkywars.getCfg().getExitPos(), exitItem);

        if (debug) {
            Util.get().logToFile(getDebugName(gameMap) + ChatColor.YELLOW + "Finished Preparing " + player.getName() + " for SkyWars on map " + gameMap.getName());
        }
        LuckyBlockBreakAttachment.applyIfLuckyPlaying(player, gameMap);
    }

    private void waitStart(final GameMap gameMap) {
        if (debug) {
            Util.get().logToFile(getDebugName(gameMap) + ChatColor.YELLOW + "Waiting to start Skywars match... (countdown)");
        }
        gameMap.setTimer(this.getWaitTime());
        new BukkitRunnable() {
            public void run() {

                // If we are not waiting for a game start (aka in game or ending) then cancel everything here
                if (gameMap.getMatchState() != MatchState.WAITINGSTART && gameMap.getMatchState() != MatchState.WAITINGLOBBY) {
                    this.cancel();
                    return;
                }

                if (gameMap.getMatchState().equals(MatchState.WAITINGSTART)) {
                    int queuedPlayers = gameMap.getPlayerCount();
                    // if there is at least one player per team OR forcestart is triggered while at least one player is present
                    if (queuedPlayers >= gameMap.getMinTeams() || (gameMap.getForceStart() && queuedPlayers > 0)) {
                        if (gameMap.getTimer() <= 0) {
                            this.cancel();
                            gameMap.setTimer(0);
                            for (final Player player : gameMap.getAlivePlayers()) {
                                Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getCountdownSound(), 1, 1F);
                            }
                            MatchManager.this.startMatch(gameMap);
                        } else {
                            if (gameMap.getTimer() <= 5 && gameMap.getMatchState() != MatchState.ENDING) {
                                for (final Player player : gameMap.getAlivePlayers()) {
                                    if (MythicSkywars.getCfg().titlesEnabled()) {
                                        Util.get().sendTitle(player, 2, 20, 2, new Messaging.MessageFormatter().
                                                        setVariable("time", "" + gameMap.getTimer()).format("titles.warmup-title"),
                                                new Messaging.MessageFormatter().format("titles.warmup-subtitle"));
                                    }
                                    if (gameMap.getTimer() == 5) {
                                        Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getCountdownSound(), 1, 0.5F);
                                    } else if (gameMap.getTimer() == 4) {
                                        Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getCountdownSound(), 1, 0.6F);
                                    } else if (gameMap.getTimer() == 3) {
                                        Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getCountdownSound(), 1, 0.7F);
                                    } else if (gameMap.getTimer() == 2) {
                                        Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getCountdownSound(), 1, 0.8F);
                                    } else if (gameMap.getTimer() == 1) {
                                        Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getCountdownSound(), 1, 0.9F);
                                    }
                                }
                            }
                            // Announce every 5 seconds OR every second if under or equal to 5
                            if (gameMap.getTimer() % 5 == 0 || gameMap.getTimer() <= 5) {
                                MatchManager.this.announceTimer(gameMap);
                            }
                        }
                        // Decrease the timer unless we are already at 0
                        gameMap.setTimer(gameMap.getTimer() > 0 ? gameMap.getTimer() - 1 : 0);
                    } else { // if not at least 1 player per team AND force start is not triggered
                        // Reset the timer
                        gameMap.setTimer(waitTime);
                    }
                } else { // If not in waitingstart state (aka are we in a lobby mode?)
                    int queuedPlayers = gameMap.getWaitingPlayers().size();

                    // if there is at least one player per team OR forcestart is triggered while at least one player is present
                    if (queuedPlayers >= gameMap.getMinTeams() || (gameMap.getForceStart() && queuedPlayers > 0)) {
                        if (gameMap.getTimer() <= 0) {

                            // Team assigning for players that did not choose a team manually.
                            if (MythicSkywars.getCfg().isBalanceUnselectedPlayersAcrossTeams()) {
                                assignUnselectedWaitingPlayersBalanced(gameMap);
                            } else {
                                assignUnselectedWaitingPlayersRandom(gameMap);
                            }

                            // Safety: kick any players that failed to get assigned to a team back to lobby
                            for (UUID waitingUuid : ImmutableList.copyOf(gameMap.getWaitingPlayers())) {
                                Player wp = Bukkit.getPlayer(waitingUuid);
                                if (wp != null && gameMap.getTeamCard(wp) == null) {
                                    MythicSkywars.get().getLogger().warning("Player " + wp.getName() + " could not be assigned to a team on " + gameMap.getName() + ". Sending back to lobby.");
                                    MythicSkywars.get().getPlayerManager().removePlayer(wp, PlayerRemoveReason.PLAYER_QUIT_GAME, null, false, false);
                                }
                            }

                            // Remove all players from waiting lobby state and set the game to waiting start (in cages mode)
                            gameMap.clearWaitingPlayers();
                            gameMap.setMatchState(MatchState.WAITINGSTART);
                            // Send all player tp their respective start cages
                            for (final Player player : gameMap.getAlivePlayers()) {
                                Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getCountdownSound(), 1, 1F);

                                gameMap.getCage().removeSpawnHousing(gameMap, gameMap.getPlayerCard(player), false);
                                boolean b = new SchematicCage().createSpawnPlatform(gameMap, player);
                                if (!b) {
                                    gameMap.getCage().setGlassColor(gameMap, gameMap.getTeamCard(player));
                                }
                                // This just runs the teleport-to-cage operation
                                teleportToArena(gameMap, gameMap.getPlayerCard(player));
                            }
                            // Wait 15 seconds before starting the game
                            gameMap.setTimer(15); // todo make this editable
                        } else { // if (gameMap.getTimer() <= 0)
                            gameMap.setTimer(gameMap.getTimer() - 1);
                        }
                    } else { // if not at least 1 player per team AND force start is not triggered
                        gameMap.setTimer(waitTime);
                    }
                }
            }
        }.runTaskTimer(MythicSkywars.get(), 0L, 20L);
    }

    private void assignUnselectedWaitingPlayersBalanced(GameMap gameMap) {
        for (UUID waitingUuid : ImmutableList.copyOf(gameMap.getWaitingPlayers())) {
            Player player = Bukkit.getPlayer(waitingUuid);
            if (player == null || gameMap.getTeamCard(player) != null) {
                continue;
            }

            // Fill teams sequentially: pick the team with the MOST players that still has room.
            // This ensures teams get filled up completely before moving to the next one.
            TeamCard targetTeam = gameMap.getTeamCards().stream()
                    .filter(card -> card.getEmptySlots() > 0)
                    .max(Comparator.comparingInt(TeamCard::getPlayersSize))
                    .orElse(null);
            if (targetTeam != null) {
                TeamCard result = targetTeam.sendReservation(player, PlayerStat.getPlayerStats(player));
                if (result == null) {
                    MythicSkywars.get().getLogger().warning("Failed to assign " + player.getName() + " to team on map " + gameMap.getName() + " (sendReservation returned null)");
                }
            } else {
                MythicSkywars.get().getLogger().warning("No team with empty slots for " + player.getName() + " on map " + gameMap.getName());
            }
        }
    }

    private void assignUnselectedWaitingPlayersRandom(GameMap gameMap) {
        for (UUID waitingUuid : ImmutableList.copyOf(gameMap.getWaitingPlayers())) {
            Player player = Bukkit.getPlayer(waitingUuid);
            if (player == null || gameMap.getTeamCard(player) != null) {
                continue;
            }

            List<TeamCard> cards = new ArrayList<>(gameMap.getTeamCards());
            Collections.shuffle(cards);
            for (TeamCard card : cards) {
                if (card.getEmptySlots() > 0) {
                    card.sendReservation(player, PlayerStat.getPlayerStats(player));
                    break;
                }
            }
        }
    }

    public void forceStart(Player player) {
        GameMap gameMap = this.getPlayerMap(player);
        gameMap.setForceStart(true);
    }

    /**
     * If the player is still on a map in ENDING (post-win screen), remove them so they can join another arena from a menu.
     */
    public void leavePostGameScreenIfNeeded(Player player) {
        if (player == null) {
            return;
        }
        GameMap cur = getPlayerMap(player);
        if (cur != null && cur.getMatchState() == MatchState.ENDING) {
            MythicSkywars.get().getPlayerManager().removePlayer(player, PlayerRemoveReason.PLAYER_QUIT_GAME, null, false, false);
        }
    }

    /**
     * Removes the player from their current SkyWars map the same way as {@code /sw quit}.
     *
     * @return false if the player was not tracked on any arena
     */
    public boolean quitCurrentGame(Player player) {
        if (player == null) {
            return false;
        }
        GameMap map = getPlayerMap(player);
        if (map == null) {
            map = getSpectatorMap(player);
        }
        if (map == null) {
            return false;
        }
        if (!map.getSpectators().contains(player.getUniqueId())) {
            markRejoinCandidate(player, map);
        }
        if (map.getTeamCard(player) == null && map.getSpectators().contains(player.getUniqueId())) {
            MythicSkywars.get().getPlayerManager().removePlayer(
                    player, PlayerRemoveReason.PLAYER_QUIT_GAME, null, false);
        } else {
            MythicSkywars.get().getPlayerManager().removePlayer(
                    player, PlayerRemoveReason.PLAYER_QUIT_GAME, null, true);
        }
        return true;
    }

    private void startMatch(final GameMap gameMap) {
        if (debug) {
            Util.get().logToFile(getDebugName(gameMap) + ChatColor.YELLOW + "Starting SkyWars Match");
        }
        for (Player player : gameMap.getAlivePlayers()) {
            player.closeInventory();
            player.getInventory().clear();
            player.setGameMode(GameMode.SURVIVAL);
            if (MythicSkywars.getCfg().titlesEnabled()) {
                Util.get().sendTitle(player, 5, 60, 5, new Messaging.MessageFormatter().setVariable("map", gameMap.getDisplayName()).format("titles.start-title"),
                        new Messaging.MessageFormatter().setVariable("map", gameMap.getDisplayName()).format("titles.start-subtitle"));
            }
        }
        if (gameMap.getMatchState() != MatchState.ENDING) {
            this.matchCountdown(gameMap);
        }
        Bukkit.getScheduler().runTaskLater(MythicSkywars.get(), () -> {
            for (Player player : gameMap.getAlivePlayers()) {
                if (player != null && player.getGameMode() != GameMode.SURVIVAL) {
                    player.setGameMode(GameMode.SURVIVAL);
                }
            }
        }, 20L);
        gameMap.getChestOption().completeOption();
        if (MythicSkywars.getCfg().isTimeVoteEnabled()) {
            gameMap.getTimeOption().completeOption();
        }
        if (MythicSkywars.getCfg().isWeatherVoteEnabled()) {
            gameMap.getWeatherOption().completeOption();
        }
        if (MythicSkywars.getCfg().isModifierVoteEnabled()) {
            gameMap.getModifierOption().completeOption();
        }
        if (MythicSkywars.getCfg().isHealthVoteEnabled()) {
            gameMap.getHealthOption().completeOption();
        }
        selectKit(gameMap);
        LuckyBlockHook.giveLuckyModeHandItems(gameMap);
        if (gameMap.isLuckyModeEnabled()) {
            LuckyBlockBreakAttachment.refreshAlivePlayers(gameMap);
        }
        gameMap.getCage().removeSpawnHousing(gameMap);
        gameMap.removeWaitingLobbyPlatform();
        gameMap.getWaitingPlayers().clear();

        if (MythicSkywars.getCfg().getEnablePVPTimer() && MythicSkywars.getCfg().getPVPTimerTime() >= 1) {
            gameMap.setDisableDamage(true);
            LunarApolloBridge.showPvpProtectionCooldowns(
                    gameMap, MythicSkywars.getCfg().getPVPTimerTime());

            Bukkit.getScheduler().scheduleSyncDelayedTask(MythicSkywars.get(), () -> {
                gameMap.setDisableDamage(false);
                gameMap.getWaitingPlayers().clear();
                for (Player player : gameMap.getAlivePlayers()) {
                    LunarApolloBridge.clearPvpCooldown(player);
                    LunarApolloBridge.notifyPvpEnabled(player, gameMap);
                    if (!MythicSkywars.getMessaging().getFile().getString("game.pvp-timer-disabled-message").isEmpty()) {
                        player.sendMessage(new Messaging.MessageFormatter().setVariable("player", player.getName()).setVariable("arena", gameMap.getName()).format("game.pvp-timer-disabled-message"));
                    }
                    if (!MythicSkywars.getMessaging().getFile().getString("game.pvp-timer-disabled-title").isEmpty()) {
                        String[] lines = new Messaging.MessageFormatter().setVariable("player", player.getName()).setVariable("arena", gameMap.getName()).format("game.pvp-timer-disabled-title").split("\\\\n");
                        if (lines.length == 1) {
                            MythicSkywars.getNMS().sendTitle(player, 20, 50, 20, lines[0], "");
                        } else {
                            MythicSkywars.getNMS().sendTitle(player, 20, 50, 20, lines[0], lines[1]);
                        }
                    }
                }
            }, 20L * MythicSkywars.getCfg().getPVPTimerTime());
        } else {
            for (Player player : gameMap.getAlivePlayers()) {
                LunarApolloBridge.notifyPvpEnabled(player, gameMap);
            }
        }
    }

    private void selectKit(GameMap gameMap) {
        if (MythicSkywars.getCfg().kitVotingEnabled()) {
            gameMap.getKitVoteOption().getVotedKit();
            for (Player player : gameMap.getAlivePlayers()) {
                GameKit.giveKit(player, gameMap.getKit());
            }
        } else {
            for (Player player : gameMap.getAlivePlayers()) {
                GameKit selectedKit = gameMap.getSelectedKit(player);

                if (selectedKit == null) {
                    // 1. Carga el kit guardado
                    String savedKitName = KitStorage.getSavedKit(player.getUniqueId());
                    if (savedKitName != null) {
                        selectedKit = GameKit.getKitByName(savedKitName);
                    }

                    // 2. Si no hay guardado, asigna el kit por defecto de la config
                    if (selectedKit == null) {
                        selectedKit = GameKit.getKitByName(MythicSkywars.getCfg().getDefaultKit());
                    }
                }

                if (selectedKit != null) {
                    GameKit.giveKit(player, selectedKit);
                }
            }
        }
    }


    private void matchCountdown(final GameMap gameMap) {
        if (debug) {
            Util.get().logToFile(getDebugName(gameMap) + ChatColor.YELLOW + "SkyWars Match started countdown");
        }
        if (gameMap.getMatchState() == MatchState.ENDING) {
            return;
        }
        gameMap.setMatchState(MatchState.PLAYING);
        gameMap.getGameBoard().updateScoreboard();
        gameMap.update();
        gameMap.setTimer(this.getGameTime());
        LunarApolloBridge.onMatchPlaying(gameMap);
        LunarApolloBridge.notifyMatchStarted(gameMap);

        new BukkitRunnable() {
            public void run() {
                if (gameMap.getMatchState() == MatchState.ENDING) {
                    this.cancel();
                } else {
                    for (MatchEvent event : gameMap.getEvents()) {
                        if (event.isEnabled() && event.willFire() && !event.hasFired()) {
                            if (event.getStartTime() <= gameMap.getTimer()) {
                                event.doEvent();
                            } else {
                                if (event.announceEnabled()) {
                                    event.announceTimer();
                                }
                            }
                        }
                    }
                    if (MythicSkywars.getCfg().isChestRefillEnabled()) {
                        int refillInterval = MythicSkywars.getCfg().getChestRefillIntervalSeconds();
                        if (refillInterval > 0 && gameMap.getTimer() > 0 && gameMap.getTimer() % refillInterval == 0) {
                            gameMap.getChestOption().completeOption();
                            ChestRefillVisualManager.get().onRefill(gameMap);
                        }
                    }
                }
                if (gameMap.isThunder()) {
                    if (gameMap.getStrikeCounter() == gameMap.getNextStrike()) {
                        World mapWorld = gameMap.getCurrentWorld();
                        int hitPlayer = new Random().nextInt(100);
                        if (hitPlayer <= 10) {
                            int size = gameMap.getAlivePlayers().size();
                            Player player = gameMap.getAlivePlayers().get(new Random().nextInt(size));
                            mapWorld.strikeLightning(player.getLocation());
                        } else {
                            int x = Util.get().getRandomNum(-150, 150);
                            int z = Util.get().getRandomNum(-150, 150);
                            int y = Util.get().getRandomNum(20, 50);
                            mapWorld.strikeLightningEffect(new Location(mapWorld, x, y, z));
                        }
                        gameMap.setNextStrike(Util.get().getRandomNum(3, 20));
                        gameMap.setStrikeCounter(0);
                    } else {
                        gameMap.setStrikeCounter(gameMap.getStrikeCounter() + 1);
                    }
                }
                if (gameMap.getTimer() > 0 && gameMap.getTimer() % 60 == 0) {
                    int perMinute = LevelManager.get().getXpReward("per-minute", 10);
                    int perTeammate = LevelManager.get().getXpReward("per-teammate", 5);
                    for (Player alive : gameMap.getAlivePlayers()) {
                        PlayerStat ps = PlayerStat.getPlayerStats(alive);
                        if (ps == null) {
                            continue;
                        }
                        int teammates = 0;
                        TeamCard tc = gameMap.getTeamCard(alive);
                        if (tc != null) {
                            teammates = Math.max(0, tc.getPlayersSize() - 1);
                        }
                        int gain = Math.max(0, perMinute + (teammates * perTeammate));
                        if (gain > 0) {
                            ps.addXp(alive, gain);
                        }
                    }
                }
                gameMap.setTimer(gameMap.getTimer() + 1);
                gameMap.getGameBoard().updateScoreboardVar(ScoreVar.TIME);
            }
        }.runTaskTimer(MythicSkywars.get(), 0L, 20L);
    }

    private void won(final GameMap gameMap, final TeamCard winners) {
        MythicSkywars plugin = MythicSkywars.get();
        Server server = plugin.getServer();

        if (debug) {
            Util.get().logToFile(getDebugName(gameMap) + ChatColor.YELLOW + "SkyWars Match has been won");
        }

        if (winners != null) {
            if (debug) {
                Util.get().logToFile(getDebugName(gameMap) + ChatColor.YELLOW + winners.getTeamName() + "Won the Match");
            }

            for (PlayerCard winner : winners.getPlayerCards()) {
                if (winner != null && winner.getPlayer() != null) {
                    gameMap.addWinner(winner.getPlayer().getName());
                }
            }
            final String winner = MythicSkywars.getCfg().usePlayerNames() ? winners.getPlayerNames() : winners.getTeamName();
            final String map = gameMap.getDisplayName();

            // Make sure winners are placed #1
            winners.setPlace(1);

            // Losers
            for (TeamCard teamCard : gameMap.getTeamCards()) {
                if (teamCard != winners) {
                    for (PlayerCard pCard : teamCard.getPlayerCards()) {
                        UUID pLoserUuid = pCard.getUUID();

                        // Skip invalid player cards
                        if (pLoserUuid == null) continue;

                        final PlayerStat loserData = PlayerStat.getPlayerStats(pLoserUuid.toString());

                        // Aquí se asigna la derrota
                        if (loserData == null) {
                            server.getScheduler().runTaskAsynchronously(plugin, () -> {
                                PlayerStat pStats = new PlayerStat(pLoserUuid, server.getOfflinePlayer(pLoserUuid).getName());
                                pStats.loadStats(() -> {
                                    pStats.setLosts(pStats.getLosses() + 1);
                                    pStats.saveStats(() -> PlayerStat.removePlayer(pStats.getId()));
                                });
                            });
                        } else {
                            loserData.setLosts(loserData.getLosses() + 1);
                        }
                    }
                }
            }

            // Winners
            boolean broadcastSent = false;
            for (PlayerCard pCard : winners.getPlayerCards()) {
                Player pWinner = pCard.getPlayer();

                if (pWinner != null) {
                    final PlayerStat winnerData = PlayerStat.getPlayerStats(pWinner.getUniqueId().toString());
                    if (winnerData != null) {
                        winnerData.setWins(winnerData.getWins() + 1);
                        if (MythicSkywars.getCfg().isSoulWellEnabled()
                                && pWinner.hasPermission(MythicSkywars.getCfg().getSoulWellXezbethPermission())) {
                            winnerData.setSoulWellXezbethFreeRollPending(true);
                            pWinner.sendMessage(new Messaging.MessageFormatter().format("soulwell.xezbeth-granted"));
                        }
                        if (MythicSkywars.getCfg().isSoulWellEnabled()) {
                            int soulsPerWin = Math.max(0, MythicSkywars.getCfg().getSoulWellSoulsPerWin());
                            if (soulsPerWin > 0) {
                                final int winMultiplier = Util.get().getMultiplier(pWinner);
                                winnerData.addSouls(soulsPerWin * winMultiplier);
                                winnerData.addSoulWellSoulsGathered(soulsPerWin * winMultiplier);
                            }
                        }
                        final int multiplier = Util.get().getMultiplier(pWinner);
                        int winXp = LevelManager.get().getXpReward("game-win", 100);
                        winnerData.addXp(pWinner, multiplier * winXp);
                        if (MythicSkywars.getCfg().economyEnabled()) {
                            VaultUtils.get().give(pWinner, multiplier * MythicSkywars.getCfg().getWinnerEco());
                        }
                        WinSoundOption sound = (WinSoundOption) WinSoundOption.getPlayerOptionByKey(winnerData.getWinSound());
                        if (sound != null) {
                            sound.playSound(pWinner.getLocation());
                        }

                        Util.get().sendActionBar(pWinner, new Messaging.MessageFormatter().setVariable("xp", "" + multiplier * winXp).format("game.win-actionbar"));
                        Util.get().doCommands(MythicSkywars.getCfg().getWinCommands(), pWinner);
                        if (MythicSkywars.getCfg().getEnableFlightOnWin()) {
                            pWinner.setAllowFlight(true);
                            pWinner.setFlying(true);
                        }
                        if (MythicSkywars.getCfg().getClearInventoryOnWin()) {
                            pWinner.getInventory().clear();
                        }
                        Bukkit.getPluginManager().callEvent(new MythicSkywarsWinEvent(winnerData, gameMap));
                    }

                    // Only broadcast once per team, not per player
                    if (!broadcastSent && MythicSkywars.getCfg().enableWinMessage()) {
                        server.broadcastMessage(new Messaging.MessageFormatter()
                                .setVariable("player1", winner).setVariable("map", map).format("game.broadcast-win"));
                        broadcastSent = true;
                    }
                    if (MythicSkywars.getCfg().titlesEnabled()) {
                        Util.get().sendTitle(pWinner, 5, 80, 5, new Messaging.MessageFormatter().format("titles.endgame-title-won"), new Messaging.MessageFormatter().format("titles.endgame-subtitle-won"));
                    }
                    if (MythicSkywars.getCfg().fireworksEnabled()) {
                        Util.get().fireworks(pWinner, 5, MythicSkywars.getCfg().getFireWorksPer5Tick());
                    }
                    if (MythicSkywars.getCfg().particlesEnabled()) {
                        List<String> particles = new ArrayList<>();
                        particles.add("FIREWORKS_SPARK");
                        Util.get().surroundParticles(pWinner, 1, particles, 8, 0);
                    }
                    pWinner.sendMessage(new Messaging.MessageFormatter()
                            .setVariable("map", gameMap.getName()).format("game.won"));
                    LunarApolloBridge.notifyVictory(pWinner, gameMap);
                }
            }

        }
        if (gameMap.getMatchState() != MatchState.OFFLINE) {
            gameMap.setMatchState(MatchState.ENDING);
            gameMap.getGameBoard().updateScoreboard();
            for (MatchEvent mEvent : gameMap.getEvents()) {
                if (mEvent.isEnabled() && mEvent.hasFired()) {
                    mEvent.endEvent(true);
                }
            }
        }
        this.endGame(gameMap);
    }

    private void endGame(final GameMap gameMap) {
        if (debug) {
            Util.get().logToFile(getDebugName(gameMap) + ChatColor.YELLOW + "SkyWars Match Has Ended - Waiting for teleport");
        }
        LunarApolloBridge.onMatchEnd(gameMap);
        gameMap.update();
        gameMap.setTimer(0);
        if (MythicSkywars.get().isEnabled() && !gameMap.getMatchState().equals(MatchState.OFFLINE)) {
            gameMap.clearDroppedItemsInArenaWorld();
            // Save all player stats
            for (final Player player : gameMap.getAllPlayers()) {
                player.closeInventory();
                player.getInventory().clear();
                player.getInventory().setArmorContents(new ItemStack[] {null, null, null, null});
                player.setGameMode(GameMode.ADVENTURE);
                player.setAllowFlight(false);
                player.setFlying(false);
                player.setFireTicks(0);
                player.setFoodLevel(20);
                player.setSaturation(20f);
                player.setHealth(player.getMaxHealth());

                int playPos = MythicSkywars.getCfg().getPlayAgainPos();
                int exitPos = MythicSkywars.getCfg().getExitPos();
                int joinPos = MythicSkywars.getCfg().getJoinSlot();

                if (MythicSkywars.getCfg().isPlayAgainItemEnabled()) {
                    ItemStack playAgainItem = MythicSkywars.getIM().getItem("playAgainItem");
                    player.getInventory().setItem(playPos, playAgainItem);
                }
                ItemStack exitItem = MythicSkywars.getIM().getItem("exitGameItem");
                if (!MythicSkywars.getCfg().isPlayAgainItemEnabled() || exitPos != playPos) {
                    player.getInventory().setItem(exitPos, exitItem);
                }
                if (MythicSkywars.getCfg().isJoinGameItemEnabled()) {
                    if (joinPos != playPos && joinPos != exitPos) {
                        player.getInventory().setItem(joinPos, MythicSkywars.getIM().getItem("joinselect"));
                    }
                }
                player.setCanPickupItems(false);

                new BukkitRunnable() {
                    public void run() {
                        String uuidStr = player.getUniqueId().toString();
                        PlayerStat toSave = PlayerStat.getPlayerStats(uuidStr);
                        if (toSave != null) {
                            toSave.saveStats();
                            // If player is no longer online, delete cache
                            if (!player.isOnline()) PlayerStat.removePlayer(uuidStr);
                        }
                    }
                }.runTaskAsynchronously(MythicSkywars.get());
            }
            // Clear all players
            new BukkitRunnable() {
                private int i = 0;
                private int maxI = MythicSkywars.getCfg().getTimeAfterMatch();

                public void run() {
                    if (i < maxI && gameMap.getCurrentWorld().getPlayers().size() > 0) {
                        i++;
                        return;
                    }
                    // Clear timer if done countdown or no players are online
                    this.cancel();
                    // Clear Spectators
                    ImmutableList<UUID> spectatorUUIDs = ImmutableList.copyOf(gameMap.getSpectators());
                    for (final UUID uuid : spectatorUUIDs) {
                        Player player = Bukkit.getServer().getPlayer(uuid);
                        if (player != null)
                            MythicSkywars.get().getPlayerManager().removePlayer(
                                    player,
                                    PlayerRemoveReason.OTHER,
                                    null,
                                    false);
                    }
                    gameMap.getSpectators().clear();
                    // Clear Alive players
                    for (final Player player : gameMap.getAlivePlayers()) {
                        if (player != null) {
                            if (PlayerData.getPlayerData(player.getUniqueId()) != null) {
                                PlayerData pd = PlayerData.getPlayerData(player.getUniqueId());
                                if (pd != null) {
                                    pd.setTaggedBy(null);
                                }
                            }
                            MythicSkywars.get().getPlayerManager().removePlayer(player, PlayerRemoveReason.PLAYER_QUIT_GAME, null, false);
                            // MatchManager.this.removeAlivePlayer(player, DamageCause.CUSTOM, true, true);
                        }
                    }
                    // Refresh map 5s after clearing all players in case some plugins do bad things
                    new BukkitRunnable() {
                        public void run() {
                            if (MythicSkywars.getCfg().bungeeMode()) {
                                Util.get().doCommands(MythicSkywars.getCfg().getGameEndCommands(), null);
                            }
                            gameMap.refreshMap();
                            if (debug) {
                                Util.get().logToFile(getDebugName(gameMap) + ChatColor.YELLOW + "SkyWars Match Has Ended - Arena has been refreshed");
                            }
                        }
                    }.runTaskLater(MythicSkywars.get(), 5 * 20L);
                }
            }.runTaskTimer(MythicSkywars.get(), 0, 20L);
        }
    }

    public void checkForWin(GameMap gameMap) {
        int teamsLeft = gameMap.getTeamsLeft();
        if (MythicSkywars.getCfg().debugEnabled())
            MythicSkywars.get().getLogger().info("MatchManager::checkForWin teamsLeft: " + teamsLeft);
        if (teamsLeft <= 1) {
            if (teamsLeft == 1) {
                this.won(gameMap, gameMap.getWinningTeam());
            } else {
                this.won(gameMap, null);
            }
        }
    }

    public GameMap getPlayerMap(final Player player) {
        if (player != null) {
            for (final GameMap gameMap : MythicSkywars.getGameMapMgr().getMapsCopy()) {
                if (gameMap.getAllPlayers().contains(player)) return gameMap;
            }
        }
        return null;
    }

    /**
     * Returns the player's map and auto-cleans stale membership links.
     * A stale link means SWR still tracks the player in a map, while the player is in lobby world.
     */
    public GameMap getPlayerMapSafe(final Player player) {
        GameMap map = getPlayerMap(player);
        if (player == null || map == null) {
            return map;
        }
        if (!Util.get().isSpawnWorld(player.getWorld())) {
            return map;
        }
        UUID playerId = player.getUniqueId();
        if (map.getWaitingPlayers().contains(playerId) || map.isJoinQueued(playerId)) {
            // Player is in transition to arena from lobby world; do not wipe reservation.
            return map;
        }
        // In lobby world but still tracked in arena state -> stale; remove from that map.
        MatchState state = map.getMatchState();
        if (state == MatchState.WAITINGLOBBY || state == MatchState.WAITINGSTART || state == MatchState.PLAYING || state == MatchState.ENDING) {
            map.removePlayer(player.getUniqueId());
            if (MythicSkywars.getCfg().debugEnabled()) {
                MythicSkywars.get().getLogger().warning("Cleaned stale arena membership for " + player.getName()
                        + " from map " + map.getName() + " while in lobby world.");
            }
            return null;
        }
        return map;
    }

    public GameMap getDeadPlayerMap(final Player v0) {
        if (v0 != null) {
            for (final GameMap gameMap : MythicSkywars.getGameMapMgr().getMapsCopy()) {
                if (gameMap.mapContainsDead(v0.getUniqueId())) {
                    return gameMap;
                }
            }
        }
        return null;
    }

    public GameMap getSpectatorMap(final Player player) {
        UUID uuid = null;
        if (player != null) {
            uuid = player.getUniqueId();
        }

        if (uuid != null) {
            for (final GameMap gameMap : MythicSkywars.getGameMapMgr().getMapsCopy()) {
                for (final UUID id : gameMap.getSpectators()) {
                    if (uuid.equals(id)) {
                        return gameMap;
                    }
                }
            }
        }
        return null;
    }

    public boolean isSpectating(final Player player) {
        return this.getSpectatorMap(player) != null;
    }

    public void markRejoinCandidate(Player player, GameMap map) {
        if (player == null || map == null || !MythicSkywars.getCfg().isRejoinEnabled()) {
            return;
        }
        if (map.getMatchState() != MatchState.PLAYING
                && map.getMatchState() != MatchState.WAITINGSTART
                && map.getMatchState() != MatchState.WAITINGLOBBY) {
            return;
        }
        TeamCard teamCard = map.getTeamCard(player);
        int teamIndex = teamCard == null ? -1 : map.getTeamCardPosition(teamCard);
        boolean wasSpectator = map.getSpectators().contains(player.getUniqueId());
        PlayerCard pCard = map.getPlayerCard(player);
        boolean wasDead = pCard != null && pCard.isDead();
        Location location = player.getLocation() == null ? null : player.getLocation().clone();
        ItemStack[] inv = cloneItems(player.getInventory().getContents());
        ItemStack[] armor = cloneItems(player.getInventory().getArmorContents());
        float exp = player.getExp();
        int level = player.getLevel();
        double health = player.getHealth();
        int food = player.getFoodLevel();
        float saturation = player.getSaturation();
        long expiresAt = System.currentTimeMillis() + MythicSkywars.getCfg().getRejoinWindowSeconds() * 1000L;
        synchronized (rejoinStates) {
            rejoinStates.put(player.getUniqueId(), new RejoinState(
                    map.getName(), expiresAt, teamIndex, wasSpectator, wasDead, map.isLuckyModeEnabled(),
                    location, inv, armor, exp, level, health, food, saturation));
        }
    }

    public boolean hasPendingRejoin(Player player) {
        if (player == null || !MythicSkywars.getCfg().isRejoinEnabled()) {
            return false;
        }
        RejoinState state;
        synchronized (rejoinStates) {
            state = rejoinStates.get(player.getUniqueId());
        }
        if (state == null || System.currentTimeMillis() > state.expiresAtMillis) {
            clearRejoin(player);
            return false;
        }
        GameMap map = findMapByName(state.mapName);
        if (map == null) {
            clearRejoin(player);
            return false;
        }
        MatchState ms = map.getMatchState();
        if (ms != MatchState.PLAYING && ms != MatchState.WAITINGSTART && ms != MatchState.WAITINGLOBBY) {
            clearRejoin(player);
            return false;
        }
        return true;
    }

    public boolean tryRejoin(Player player) {
        if (!hasPendingRejoin(player)) {
            return false;
        }
        RejoinState state;
        synchronized (rejoinStates) {
            state = rejoinStates.get(player.getUniqueId());
        }
        if (state == null) {
            return false;
        }
        GameMap map = findMapByName(state.mapName);
        if (map == null) {
            clearRejoin(player);
            return false;
        }
        boolean joined = false;
        if (map.getMatchState() == MatchState.WAITINGLOBBY || map.getMatchState() == MatchState.WAITINGSTART) {
            joined = map.addPlayers(null, player);
        } else if (map.getMatchState() == MatchState.PLAYING) {
            if (state.wasSpectator || state.wasDead) {
                joined = rejoinAsSpectator(player, map);
            } else {
                joined = tryRejoinAsAlivePlayer(player, map, state);
                if (!joined) {
                    joined = rejoinAsSpectator(player, map);
                }
            }
        }
        if (joined) {
            message(map, new Messaging.MessageFormatter()
                    .setVariable("player", player.getDisplayName())
                    .setVariable("playercount", String.valueOf(map.getPlayerCount()))
                    .setVariable("players", String.valueOf(map.getPlayerCount()))
                    .setVariable("maxplayers", String.valueOf(map.getMaxPlayers()))
                    .format("game.rejoined-the-game"), player);
            clearRejoin(player);
        }
        return joined;
    }

    public void clearRejoin(Player player) {
        if (player == null) {
            return;
        }
        synchronized (rejoinStates) {
            rejoinStates.remove(player.getUniqueId());
        }
    }

    private GameMap findMapByName(String name) {
        if (name == null) {
            return null;
        }
        for (GameMap map : MythicSkywars.getGameMapMgr().getMapsCopy()) {
            if (name.equalsIgnoreCase(map.getName())) {
                return map;
            }
        }
        return null;
    }

    private boolean rejoinAsSpectator(Player player, GameMap map) {
        MythicSkywars.get().getPlayerManager().addSpectator(map, player);
        return true;
    }

    private boolean tryRejoinAsAlivePlayer(Player player, GameMap map, RejoinState state) {
        if (player == null || map == null || map.getMatchState() != MatchState.PLAYING) {
            return false;
        }
        PlayerStat ps = PlayerStat.getPlayerStats(player.getUniqueId());
        if (ps == null || !ps.isInitialized()) {
            return false;
        }

        TeamCard selected = null;
        int preferredTeamIndex = state.teamIndex;
        if (preferredTeamIndex >= 0) {
            TeamCard preferred = map.getTeamCardByIndex(preferredTeamIndex);
            if (preferred != null && preferred.getEmptySlots() > 0) {
                selected = preferred.sendReservation(player, ps);
            }
        }
        if (selected == null) {
            for (TeamCard card : map.getTeamCards()) {
                if (card.getEmptySlots() > 0) {
                    selected = card.sendReservation(player, ps);
                    if (selected != null) {
                        break;
                    }
                }
            }
        }
        if (selected == null) {
            return false;
        }

        PlayerCard pCard = map.getPlayerCard(player);
        if (pCard == null || pCard.getSpawn() == null || map.getCurrentWorld() == null) {
            return false;
        }
        if (PlayerData.getPlayerData(player.getUniqueId()) == null) {
            PlayerData.getAllPlayerData().add(new PlayerData(player));
        }
        Location spawn = new Location(
                map.getCurrentWorld(),
                pCard.getSpawn().getX() + 0.5,
                pCard.getSpawn().getY() + 1,
                pCard.getSpawn().getZ() + 0.5);
        spawn = resolveSafeTeleportLocation(map, spawn, "rejoin");
        player.teleport(spawn, TeleportCause.PLUGIN);
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);
        map.getSpectators().remove(player.getUniqueId());
        if (state.location != null && state.location.getWorld() != null
                && map.getCurrentWorld() != null
                && state.location.getWorld().equals(map.getCurrentWorld())) {
            player.teleport(state.location, TeleportCause.PLUGIN);
        }
        if (state.inventory != null) {
            player.getInventory().setContents(cloneItems(state.inventory));
        }
        if (state.armor != null) {
            player.getInventory().setArmorContents(cloneItems(state.armor));
        }
        player.setExp(state.exp);
        player.setLevel(state.level);
        player.setFoodLevel(Math.max(1, state.food));
        player.setSaturation(Math.max(0f, state.saturation));
        double hp = Math.max(1.0, Math.min(player.getMaxHealth(), state.health));
        player.setHealth(hp);
        map.getGameBoard().updateScoreboard();
        return true;
    }

    private ItemStack[] cloneItems(ItemStack[] items) {
        if (items == null) {
            return null;
        }
        ItemStack[] clone = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) {
            clone[i] = items[i] == null ? null : items[i].clone();
        }
        return clone;
    }

    private Location resolveSafeTeleportLocation(GameMap map, Location preferred, String reason) {
        if (preferred == null || preferred.getWorld() == null) {
            return preferred;
        }
        if (isSafeStandingLocation(preferred)) {
            return preferred;
        }

        World world = preferred.getWorld();
        int baseX = preferred.getBlockX();
        int baseZ = preferred.getBlockZ();
        int maxY = world.getMaxHeight() - 2;

        for (int radius = 0; radius <= 6; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int x = baseX + dx;
                    int z = baseZ + dz;
                    int groundY = world.getHighestBlockYAt(x, z);
                    if (groundY < 1 || groundY >= maxY) {
                        continue;
                    }
                    Location candidate = new Location(world, x + 0.5, groundY + 1, z + 0.5, preferred.getYaw(), preferred.getPitch());
                    if (isSafeStandingLocation(candidate)) {
                        if (debug) {
                            Util.get().logToFile(getDebugName(map) + ChatColor.YELLOW
                                    + "Unsafe spawn detected (" + reason + "), moving player to safe location: "
                                    + candidate.getBlockX() + "," + candidate.getBlockY() + "," + candidate.getBlockZ());
                        }
                        return candidate;
                    }
                }
            }
        }

        // Last-resort fallback: use map waiting lobby spawn if it exists and is safe.
        CoordLoc waitingLobby = map == null ? null : map.getWaitingLobbySpawn();
        if (waitingLobby != null && world.equals(map.getCurrentWorld())) {
            Location lobbyFallback = new Location(world, waitingLobby.getX() + 0.5, waitingLobby.getY() + 1, waitingLobby.getZ() + 0.5, preferred.getYaw(), preferred.getPitch());
            if (isSafeStandingLocation(lobbyFallback)) {
                if (debug) {
                    Util.get().logToFile(getDebugName(map) + ChatColor.YELLOW + "Unsafe spawn detected (" + reason + "), using waiting lobby fallback.");
                }
                return lobbyFallback;
            }
        }

        return preferred;
    }

    private boolean isSafeStandingLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        World world = location.getWorld();
        int y = location.getBlockY();
        if (y <= 1 || y >= world.getMaxHeight() - 1) {
            return false;
        }

        Material feet = world.getBlockAt(location.getBlockX(), y, location.getBlockZ()).getType();
        Material head = world.getBlockAt(location.getBlockX(), y + 1, location.getBlockZ()).getType();
        Material ground = world.getBlockAt(location.getBlockX(), y - 1, location.getBlockZ()).getType();

        // Accept any solid block as ground — glass/stained glass are valid (cages use them)
        return !isSolidOrLiquid(feet) && !isSolidOrLiquid(head) && ground.isSolid();
    }

    private boolean isSolidOrLiquid(Material material) {
        if (material == null) return false;
        if (material.isSolid()) return true;
        String name = material.name();
        return name.contains("WATER") || name.contains("LAVA");
    }

    private int getGameTime() {
        return gameTime;
    }

    private int getWaitTime() {
        return waitTime;
    }

    private void setWaitTime(int waitTime) {
        this.waitTime = waitTime;
    }

    private void setGameTime() {
        this.gameTime = 0;
    }

    // UTILS

    public String getDebugName(GameMap gameMap) {
        return ChatColor.RED + "SWR[" + (gameMap != null ? gameMap.getName() : "null") + "] ";
    }

    private void announceTimer(final GameMap gameMap) {
        final int v1 = gameMap.getTimer();
        String time;
        if (v1 % 60 == 0) {
            time = v1 / 60 + " " + ((v1 > 60) ? new Messaging.MessageFormatter().format("timer.minutes") : new Messaging.MessageFormatter().format("timer.minute"));
        } else {
            if (v1 >= 60 || (v1 % 10 != 0 && v1 >= 10) || v1 <= 0) {
                return;
            }
            time = v1 + " " + ((v1 > 1) ? new Messaging.MessageFormatter().format("timer.seconds") : new Messaging.MessageFormatter().format("timer.second"));
        }
        this.message(gameMap, new Messaging.MessageFormatter().setVariable("time", time).format("timer.wait-timer"), null);
    }

    private static final class RejoinState {
        private final String mapName;
        private final long expiresAtMillis;
        private final int teamIndex;
        private final boolean wasSpectator;
        private final boolean wasDead;
        private final boolean luckyMode;
        private final Location location;
        private final ItemStack[] inventory;
        private final ItemStack[] armor;
        private final float exp;
        private final int level;
        private final double health;
        private final int food;
        private final float saturation;

        private RejoinState(String mapName, long expiresAtMillis, int teamIndex, boolean wasSpectator,
                            boolean wasDead, boolean luckyMode, Location location, ItemStack[] inventory, ItemStack[] armor,
                            float exp, int level, double health, int food, float saturation) {
            this.mapName = mapName;
            this.expiresAtMillis = expiresAtMillis;
            this.teamIndex = teamIndex;
            this.wasSpectator = wasSpectator;
            this.wasDead = wasDead;
            this.luckyMode = luckyMode;
            this.location = location;
            this.inventory = inventory;
            this.armor = armor;
            this.exp = exp;
            this.level = level;
            this.health = health;
            this.food = food;
            this.saturation = saturation;
        }
    }
}