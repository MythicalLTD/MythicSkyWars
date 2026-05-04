package systems.mythical.mythicskywars.listeners;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.enums.GameType;
import systems.mythical.mythicskywars.enums.LeaderType;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.managers.LobbyBypassManager;
import systems.mythical.mythicskywars.managers.MatchManager;
import systems.mythical.mythicskywars.utilities.Messaging;
import systems.mythical.mythicskywars.utilities.Party;
import systems.mythical.mythicskywars.utilities.SWRServer;
import systems.mythical.mythicskywars.utilities.Util;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

import java.util.UUID;

public class LobbyListener implements org.bukkit.event.Listener {


    public LobbyListener() {
    }

    private void enforceLobbyEnvironment(World world) {
        if (!MythicSkywars.getCfg().protectLobby() || !Util.get().isSpawnWorld(world)) {
            return;
        }
        if (MythicSkywars.getCfg().isLobbyForceSunny()) {
            // Only mutate weather when needed. Calling setStorm(false) while handling
            // WeatherChangeEvent re-fires that event on 1.8 and can recurse infinitely.
            if (world.hasStorm() || world.isThundering()) {
                world.setStorm(false);
                world.setThundering(false);
                world.setWeatherDuration(0);
                world.setThunderDuration(0);
            }
        }
        if (MythicSkywars.getCfg().isLobbyForceDay()) {
            world.setTime(1000L);
        }
        // Remove hostile mobs from the lobby world (preserves NPCs, armor stands, holograms, etc.)
        clearHostileMobs(world);
    }

    /**
     * Removes hostile mobs from the lobby world while preserving:
     * - Players
     * - Armor stands (used by holograms, NPCs)
     * - Entities with custom names (likely NPCs from Citizens, FancyNPCs, etc.)
     * - Entities that have metadata from NPC plugins
     */
    private void clearHostileMobs(World world) {
        for (org.bukkit.entity.Entity entity : world.getEntities()) {
            if (entity instanceof Player) continue;
            if (entity instanceof org.bukkit.entity.ArmorStand) continue;
            // Preserve named entities (NPCs, custom mobs from other plugins)
            if (entity.getCustomName() != null) continue;
            // Preserve entities marked by NPC plugins (Citizens, FancyNPCs, etc.)
            if (entity.hasMetadata("NPC")) continue;
            // Only remove living hostile/neutral mobs (monsters, animals)
            if (entity instanceof org.bukkit.entity.Monster
                    || entity instanceof org.bukkit.entity.Slime
                    || entity instanceof org.bukkit.entity.Phantom) {
                entity.remove();
            }
        }
    }

    private boolean isInSkyWarsMatch(Player player) {
        if (player == null) {
            return false;
        }
        GameMap map = MatchManager.get().getPlayerMapSafe(player);
        return map != null && map.getMatchState() != systems.mythical.mythicskywars.enums.MatchState.ENDING;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent e) {
        if ((MythicSkywars.getCfg().bungeeMode() && !MythicSkywars.getCfg().isLobbyServer())
                || (MythicSkywars.getCfg().isLobbyHideJoinQuitMessages() && Util.get().isSpawnWorld(e.getPlayer().getWorld()))) {
            e.setJoinMessage("");
        }
        enforceLobbyEnvironment(e.getPlayer().getWorld());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent e) {
        if ((MythicSkywars.getCfg().bungeeMode() && !MythicSkywars.getCfg().isLobbyServer())
                || (MythicSkywars.getCfg().isLobbyHideJoinQuitMessages() && Util.get().isSpawnWorld(e.getPlayer().getWorld()))) {
            e.setQuitMessage("");
        }
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player && isInSkyWarsMatch((Player) e.getEntity())) {
            return;
        }
        if ((MythicSkywars.getCfg().protectLobby()) && (Util.get().isSpawnWorld(e.getEntity().getWorld()))) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAnyDamage(EntityDamageEvent e) {
        if (!MythicSkywars.getCfg().protectLobby()) {
            return;
        }
        if (!(e.getEntity() instanceof Player) || !Util.get().isSpawnWorld(e.getEntity().getWorld())) {
            return;
        }
        Player player = (Player) e.getEntity();
        if (isInSkyWarsMatch(player)) {
            return;
        }
        e.setCancelled(true);
        player.setFireTicks(0);
        player.setHealth(player.getMaxHealth());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (!MythicSkywars.getCfg().protectLobby()) {
            return;
        }
        if (!Util.get().isSpawnWorld(e.getEntity().getWorld())) {
            return;
        }
        // Allow spawns from plugins (NPCs, holograms, FancyNPCs, Citizens, etc.)
        CreatureSpawnEvent.SpawnReason reason = e.getSpawnReason();
        if (reason == CreatureSpawnEvent.SpawnReason.CUSTOM
                || reason == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG
                || reason == CreatureSpawnEvent.SpawnReason.DEFAULT) {
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLobbyHunger(FoodLevelChangeEvent e) {
        if (!MythicSkywars.getCfg().protectLobby()) {
            return;
        }
        if (!(e.getEntity() instanceof Player) || !Util.get().isSpawnWorld(e.getEntity().getWorld())) {
            return;
        }
        Player player = (Player) e.getEntity();
        if (isInSkyWarsMatch(player)) {
            return;
        }
        e.setCancelled(true);
        player.setFoodLevel(20);
        player.setSaturation(20f);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLobbyMoveHeal(PlayerMoveEvent e) {
        if (!MythicSkywars.getCfg().protectLobby()) {
            return;
        }
        Player player = e.getPlayer();
        if (!Util.get().isSpawnWorld(player.getWorld())) {
            return;
        }
        if (isInSkyWarsMatch(player)) {
            return;
        }
        if (player.getLocation().getY() <= MythicSkywars.getCfg().getQuickDeathY()) {
            Location spawn = MythicSkywars.getCfg().getSpawn();
            if (spawn != null) {
                player.setFallDistance(0f);
                player.setVelocity(player.getVelocity().zero());
                player.teleport(spawn);
                player.sendMessage(new Messaging.MessageFormatter().format("game.lobby-void-teleport"));
                return;
            }
        }
        if (player.getHealth() < player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        if (player.getFoodLevel() < 20) {
            player.setFoodLevel(20);
            player.setSaturation(20f);
        }
        if (player.getRemainingAir() < player.getMaximumAir()) {
            player.setRemainingAir(player.getMaximumAir());
        }
        enforceLobbyEnvironment(player.getWorld());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLobbyWeather(WeatherChangeEvent e) {
        if (!MythicSkywars.getCfg().protectLobby() || !Util.get().isSpawnWorld(e.getWorld())) {
            return;
        }
        if (MythicSkywars.getCfg().isLobbyForceSunny() && e.toWeatherState()) {
            // Cancelling blocks rain from starting. Do not call setStorm here — on 1.8 that
            // fires another WeatherChangeEvent and can stack-overflow if combined with
            // enforceLobbyEnvironment().
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        if (isInSkyWarsMatch(player)) {
            return;
        }
        if ((MythicSkywars.getCfg().protectLobby()) && (Util.get().isSpawnWorld(player.getWorld())) &&
                (!MythicSkywars.getIC().has(player)) && !LobbyBypassManager.hasBypass(player)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        if (isInSkyWarsMatch(e.getPlayer())) {
            return;
        }
        if ((MythicSkywars.getCfg().protectLobby()) && (Util.get().isSpawnWorld(e.getPlayer().getWorld()))) {
            if (LobbyBypassManager.hasBypass(e.getPlayer())) {
                return;
            }
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void signPlaced(SignChangeEvent event) {
        //if (Util.get().isSpawnWorld(event.getBlock().getWorld())) {
        String[] lines = event.getLines();
        if ((lines[0].equalsIgnoreCase("[sw]") || lines[0].equalsIgnoreCase("[swr]") || lines[0].equalsIgnoreCase("[skywars]")) && (lines.length >= 2)) {
            if (!event.getPlayer().hasPermission("sw.signs")) {
                event.getPlayer().sendMessage(new Messaging.MessageFormatter().format("error.signs-no-perm"));
                event.setCancelled(true);
                return;
            }

            Location signLocation = event.getBlock().getLocation();

            event.setCancelled(true);
            String arenaName = lines[1];
            if (MythicSkywars.getCfg().bungeeMode()) {
                SWRServer server = SWRServer.getServer(arenaName);
                if (server != null) {
                    server.addSign(signLocation);
                    event.getPlayer().sendMessage(new Messaging.MessageFormatter().format("signs.added"));
                } else {
                    event.getPlayer().sendMessage(new Messaging.MessageFormatter().format("signs.no-map"));
                }
            } else {
                GameMap gMap = MythicSkywars.getGameMapMgr().getMap(arenaName);
                if (gMap != null) {
                    gMap.addSign(signLocation);
                    event.getPlayer().sendMessage(new Messaging.MessageFormatter().format("signs.added"));
                } else {
                    event.getPlayer().sendMessage(new Messaging.MessageFormatter().format("signs.no-map"));
                }
            }
        } else if ((lines[0].equalsIgnoreCase("[swl]")) && (lines.length >= 3)) {
            if (!event.getPlayer().hasPermission("sw.signs")) {
                event.getPlayer().sendMessage(new Messaging.MessageFormatter().format("error.signs-no-perm"));
                event.setCancelled(true);
                return;
            }

            Location signLocation = event.getBlock().getLocation();
            World w = signLocation.getWorld();
            Block b = w.getBlockAt(signLocation);

            if (!(b.getState() instanceof Sign)) {
                return;
            }

            event.setCancelled(true);
            if (MythicSkywars.get().getLeaderTypes().contains(lines[1].toUpperCase())) {
                LeaderType type = LeaderType.valueOf(lines[1].toUpperCase());
                if (Util.get().isInteger(lines[2])) {
                    if (Integer.parseInt(lines[2]) <= MythicSkywars.getCfg().getLeaderSize()) {
                        MythicSkywars.getLB().addLeaderSign(Integer.parseInt(lines[2]), type, signLocation);
                        event.getPlayer().sendMessage(new Messaging.MessageFormatter().format("signs.addedleader"));
                    } else {
                        event.getPlayer().sendMessage(new Messaging.MessageFormatter().format("signs.invalid-range"));
                    }
                } else {
                    event.getPlayer().sendMessage(new Messaging.MessageFormatter().format("error.position"));
                }
            } else {
                event.getPlayer().sendMessage(new Messaging.MessageFormatter().format("signs.invalid-type"));
            }
        }
        //}
    }

    @EventHandler
    public void signRemoved(BlockBreakEvent event) {
        if (Util.get().isSpawnWorld(event.getBlock().getWorld())) {
            Location blockLocation = event.getBlock().getLocation();
            World w = blockLocation.getWorld();
            Block b = w.getBlockAt(blockLocation);
            if ((b.getState() instanceof Sign)) {
                Sign sign = (Sign) b.getState();
                Location loc = sign.getLocation();
                boolean removed = false;

                if (!MythicSkywars.getCfg().bungeeMode()) {
                    for (GameMap map : MythicSkywars.getGameMapMgr().getMapsCopy()) {
                        if (map.hasSign(loc)) {
                            if (event.getPlayer().isSneaking()) {
                                if (!removed) {
                                    if (event.getPlayer().hasPermission("sw.signs")) {
                                        removed = map.removeSign(loc);
                                    } else {
                                        event.setCancelled(true);
                                    }
                                }
                            } else {
                                event.setCancelled(true);
                            }
                        }
                    }
                } else {
                    SWRServer server = SWRServer.getSign(loc);
                    if (server != null) {
                        if (event.getPlayer().hasPermission("sw.signs") && event.getPlayer().isSneaking()) {
                            server.removeSign(loc);
                            removed = true;
                            event.getPlayer().sendMessage(new Messaging.MessageFormatter().format("signs.remove"));
                        } else {
                            event.setCancelled(true);
                        }
                    }
                }

                if (!removed) {
                    if (event.getPlayer().hasPermission("sw.signs")) {
                        removed = MythicSkywars.getLB().removeLeaderSign(loc);
                    }
                }
                if (removed) {
                    event.getPlayer().sendMessage(new Messaging.MessageFormatter().format("signs.remove"));
                }
            }
        }
    }

    @EventHandler
    public void onPressurePlate(PlayerInteractEvent e) {
        if (Util.get().isSpawnWorld(e.getPlayer().getWorld())) {
            Player player = e.getPlayer();
            GameMap gMap = MatchManager.get().getPlayerMap(player);
            if ((gMap == null) &&
                    (e.getAction() == org.bukkit.event.block.Action.PHYSICAL) && ((e.getClickedBlock().getType() == MythicSkywars.getNMS().getMaterial("STONE_PLATE").getType()) ||
                    (

                            e.getClickedBlock().getType() == MythicSkywars.getNMS().getMaterial("IRON_PLATE").getType()) || (e.getClickedBlock().getType() == MythicSkywars.getNMS().getMaterial("GOLD_PLATE").getType())) &&
                    (MythicSkywars.getCfg().pressurePlateJoin())) {
                Location spawn = MythicSkywars.getCfg().getSpawn();
                if (spawn != null) {
                    boolean joined = false;
                    int count = 0;
                    Party party = Party.getParty(player);
                    while ((count < 4) && (!joined)) {
                        if (party != null) {
                            if (party.getLeader().equals(player.getUniqueId())) {
                                boolean tryJoin = true;
                                for (UUID uuid : party.getMembers()) {
                                    if (Util.get().isBusy(uuid)) {
                                        tryJoin = false;
                                        party.sendPartyMessage(new Messaging.MessageFormatter().setVariable("player", org.bukkit.Bukkit.getPlayer(uuid).getName()).format("party.memberbusy"));
                                    }
                                }
                                if (!tryJoin) break;
                                if (e.getClickedBlock().getType() == MythicSkywars.getNMS().getMaterial("STONE_PLATE").getType()) {
                                    joined = MatchManager.get().joinGame(party, GameType.ALL) != null;
                                } else if (e.getClickedBlock().getType() == MythicSkywars.getNMS().getMaterial("IRON_PLATE").getType()) {
                                    joined = MatchManager.get().joinGame(party, GameType.SINGLE) != null;
                                } else {
                                    joined = MatchManager.get().joinGame(party, GameType.TEAM) != null;
                                }

                            } else {
                                player.sendMessage(new Messaging.MessageFormatter().format("party.onlyleader"));
                                joined = true;
                                break;
                            }
                        } else if (e.getClickedBlock().getType() == MythicSkywars.getNMS().getMaterial("STONE_PLATE").getType()) {
                            joined = MatchManager.get().joinGame(player, GameType.ALL) != null;
                        } else if (e.getClickedBlock().getType() == MythicSkywars.getNMS().getMaterial("IRON_PLATE").getType()) {
                            joined = MatchManager.get().joinGame(player, GameType.SINGLE) != null;
                        } else {
                            joined = MatchManager.get().joinGame(player, GameType.TEAM) != null;
                        }

                        count++;
                    }
                    if (!joined) {
                        player.sendMessage(new Messaging.MessageFormatter().format("error.could-not-join"));
                    }
                } else {
                    e.getPlayer().sendMessage(new Messaging.MessageFormatter().format("error.nospawn"));
                    MythicSkywars.get().getLogger().info("It's not possible for people to join games if the skywars lobby spawn hasn't been set. Set it using '/sw setspawn'");
                }
            }
        }
    }
}
