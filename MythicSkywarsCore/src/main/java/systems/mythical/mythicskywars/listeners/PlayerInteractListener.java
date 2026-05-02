package systems.mythical.mythicskywars.listeners;

import com.google.common.collect.Lists;
import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.enums.ChestPlacementType;
import systems.mythical.mythicskywars.enums.GameType;
import systems.mythical.mythicskywars.enums.MatchState;
import systems.mythical.mythicskywars.enums.PlayerRemoveReason;
import systems.mythical.mythicskywars.game.Crate;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.game.TeamCard;
import systems.mythical.mythicskywars.managers.ChestRefillVisualManager;
import systems.mythical.mythicskywars.managers.LobbyBypassManager;
import systems.mythical.mythicskywars.managers.MatchManager;
import systems.mythical.mythicskywars.menus.playeroptions.StatsMenu;
import systems.mythical.mythicskywars.menus.gameoptions.KitSelectionMenu;
import systems.mythical.mythicskywars.menus.gameoptions.VotingMenu;
import systems.mythical.mythicskywars.menus.gameoptions.objects.CoordLoc;
import systems.mythical.mythicskywars.menus.playeroptions.OptionsSelectionMenu;
import systems.mythical.mythicskywars.menus.soulwell.SoulWellMenu;
import systems.mythical.mythicskywars.menus.JoinSingleMenu;
import systems.mythical.mythicskywars.menus.ArenaSetupMenu;
import systems.mythical.mythicskywars.utilities.LuckyBlockHook;
import systems.mythical.mythicskywars.utilities.Messaging;
import systems.mythical.mythicskywars.utilities.Party;
import systems.mythical.mythicskywars.utilities.SWRServer;
import systems.mythical.mythicskywars.utilities.Util;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class PlayerInteractListener implements Listener {


    Object navigationWand = "";
    Object wandItem = "";

    private static final String legacyWEItemsErrorMessage = "An error occurred while detecting player interaction! " +
            "You are using an legacy item ID in the WorldEdit configuration on a non-legacy server (1.13+)! " +
            "Multiple functions of this plugin will fail to work properly until the WorldEdit configuration is re-created or the errors are corrected.";

    public PlayerInteractListener() {
        File f = new File(MythicSkywars.get().getDataFolder().getAbsolutePath().replace("Skywars", "WorldEdit"), "config.yml");
        if (f.exists()) {
            FileConfiguration fc = YamlConfiguration.loadConfiguration(f);
            navigationWand = fc.get("navigation-wand.item");
            wandItem = fc.get("wand-item");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (MatchManager.get().getPlayerMap(player) == null) {
            return;
        }
        if (e.getItem() == null || !Bukkit.getPluginManager().isPluginEnabled("WorldEdit")) {
            return;
        }

        try {
            if (navigationWand instanceof Integer) {
                if (e.getItem().getType().getId() == (int) navigationWand) e.setCancelled(true);
            } else if (e.getItem().getType().name().equalsIgnoreCase((String) navigationWand)) {
                e.setCancelled(true);
            }

            if (wandItem instanceof Integer) {
                if (e.getItem().getType().getId() == (int) wandItem) e.setCancelled(true);
            } else if (e.getItem().getType().name().equalsIgnoreCase((String) wandItem)) {
                e.setCancelled(true);
            }
        } catch (IllegalArgumentException ex) {
            MythicSkywars.get().getLogger().severe(legacyWEItemsErrorMessage);
        }
    }


    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (MythicSkywars.getSoulWellManager() != null && MythicSkywars.getSoulWellManager().isClickDebugEnabled()) {
            if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK
                    || event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_AIR) {
                Block clicked = event.getClickedBlock();
                @SuppressWarnings("deprecation")
                Block target = player.getTargetBlock(null, 6);
                String clickedInfo = clicked == null ? "null" : (clicked.getWorld().getName() + " "
                        + clicked.getX() + "," + clicked.getY() + "," + clicked.getZ() + " " + clicked.getType().name());
                String targetInfo = target == null ? "null" : (target.getWorld().getName() + " "
                        + target.getX() + "," + target.getY() + "," + target.getZ() + " " + target.getType().name());
                MythicSkywars.get().getLogger().info("[SoulWellDebug] " + player.getName() + " action=" + event.getAction()
                        + " clicked=" + clickedInfo + " target=" + targetInfo);
            }
        }
        InventoryView v = player.getOpenInventory();
        if (v != null && v.getTopInventory() != null && v.getTopInventory().getType() != InventoryType.CRAFTING) return;

        final GameMap gameMap = MatchManager.get().getPlayerMapSafe(player);
        if (gameMap == null) {
            GameMap editorMap = MythicSkywars.getGameMapMgr().getMap(player.getWorld().getName());
            if (editorMap != null && editorMap.isEditing() && event.hasItem()
                    && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
                
                // Handle Team Spawner Tool - only trigger on RIGHT_CLICK_AIR to prevent double execution
                if (ArenaSetupMenu.isTeamSpawnerTool(event.getItem())) {
                    event.setCancelled(true);
                    if (event.getAction() == Action.RIGHT_CLICK_AIR) {
                        handleTeamSpawnerTool(player, editorMap);
                    }
                    return;
                }
                
                // Handle Deathmatch Spawner Tool
                if (ArenaSetupMenu.isDeathmatchSpawnerTool(event.getItem())) {
                    event.setCancelled(true);
                    handleDeathmatchSpawnerTool(player, editorMap);
                    return;
                }
                
                // Handle Arena Setup Menu Tool
                if (ArenaSetupMenu.isTool(event.getItem())) {
                    event.setCancelled(true);
                    // If right-clicking a chest, toggle its type instead of opening menu
                    if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null
                            && (event.getClickedBlock().getType() == Material.CHEST || event.getClickedBlock().getType() == Material.TRAPPED_CHEST)) {
                        handleChestTypeToggle(player, editorMap, event.getClickedBlock());
                        return;
                    }
                    ArenaSetupMenu.open(player, editorMap);
                    return;
                }
            }
            if ((event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK)
                    && event.getClickedBlock() != null
                    && MythicSkywars.getSoulWellManager() != null
                    ) {
                boolean directWellBlock = MythicSkywars.getSoulWellManager().isWellInteractBlock(event.getClickedBlock().getLocation());
                boolean nearWellBlock = MythicSkywars.getSoulWellManager().isNearWell(event.getClickedBlock().getLocation(), 2.0);
                if (MythicSkywars.getSoulWellManager().isClickDebugEnabled()) {
                    org.bukkit.Location wl = MythicSkywars.getSoulWellManager().getWellLocation();
                    String wlInfo = wl == null ? "null" : (wl.getWorld().getName() + " " + wl.getBlockX() + "," + wl.getBlockY() + "," + wl.getBlockZ());
                    MythicSkywars.get().getLogger().info("[SoulWellDebug] hasWell=" + MythicSkywars.getSoulWellManager().hasWellConfigured()
                            + " direct=" + directWellBlock + " near=" + nearWellBlock + " well=" + wlInfo);
                }
                if (!MythicSkywars.getSoulWellManager().hasWellConfigured()
                        && event.getClickedBlock().getType().name().contains("ENDER_PORTAL_FRAME")) {
                    event.setCancelled(true);
                    player.sendMessage(new Messaging.MessageFormatter().format("command.soulwell.not-set"));
                    return;
                }
                if (directWellBlock || nearWellBlock) {
                    event.setCancelled(true);
                    if (!MythicSkywars.getCfg().isSoulWellEnabled()) {
                        player.sendMessage(new Messaging.MessageFormatter().format("soulwell.disabled"));
                        return;
                    }
                    if (!MythicSkywars.getSoulWellManager().hasWellConfigured()) {
                        player.sendMessage(new Messaging.MessageFormatter().format("command.soulwell.not-set"));
                        return;
                    }
                    if (!player.hasPermission("sw.soulwell")) {
                        player.sendMessage(new Messaging.MessageFormatter().format("error.nopermission"));
                        return;
                    }
                    SoulWellMenu.open(player);
                    Util.get().playSound(player, event.getPlayer().getLocation(), MythicSkywars.getCfg().getOpenOptionsMenuSound(), 0.7F, 1F);
                    return;
                }
            }
            if (Util.get().isSpawnWorld(player.getWorld())) {
                if (LobbyBypassManager.hasBypass(player)) {
                    return;
                }
                if (MythicSkywars.getCfg().protectLobby()) {
                    boolean isBlockBuildClick = event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK;
                    if (player.hasPermission("sw.alterlobby") && isBlockBuildClick) {
                        // sw.alterlobby only bypasses for block place/break in lobby.
                        return;
                    }
                    event.setCancelled(true);
                }
                if (event.hasItem()) {
                    if (PlayerTeleportListener.isPlayerOnCooldown(player)) {
                        event.setCancelled(true);
                        return;
                    }

                    if (event.getItem().equals(MythicSkywars.getIM().getItem("optionselect"))) {
                        event.setCancelled(true);
                        Util.get().playSound(player, event.getPlayer().getLocation(), MythicSkywars.getCfg().getOpenOptionsMenuSound(), 0.5F, 1);
                        new OptionsSelectionMenu(player);
                    } else if (event.getItem().isSimilar(MythicSkywars.getIM().getItem("statsitem"))) {
                        event.setCancelled(true);
                        StatsMenu.open(player);
                    } else if (event.getItem().isSimilar(MythicSkywars.getIM().getItem("backlobbyitem"))) {
                        event.setCancelled(true);
                        if (MythicSkywars.getCfg().isBungeeEnabled() && MythicSkywars.getCfg().getBungeeLobby() != null
                                && !MythicSkywars.getCfg().getBungeeLobby().trim().isEmpty()) {
                            MythicSkywars.get().sendBungeeMsg(player, "Connect", MythicSkywars.getCfg().getBungeeLobby());
                        } else {
                            player.sendMessage(new Messaging.MessageFormatter().format("error.could-not-join2"));
                        }
                    } else if (event.getItem().isSimilar(MythicSkywars.getIM().getItem("joinselect"))) {
                        event.setCancelled(true);
                        handleJoinSelectItem(player);
                    } else if (event.getItem().equals(MythicSkywars.getIM().getItem("spectateselect"))) {
                        event.setCancelled(true);
                        Util.get().playSound(player, event.getPlayer().getLocation(), MythicSkywars.getCfg().getOpenSpectateMenuSound(), 1, 1);
                        if (MythicSkywars.getGameMapMgr().getPlayableArenas(GameType.TEAM).isEmpty()) {
                            if (!MythicSkywars.getIC().hasViewers("spectatesinglemenu")) {
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        MythicSkywars.getIC().getMenu("joinsinglemenu").update();
                                    }
                                }.runTaskLater(MythicSkywars.get(), 5);
                            }
                            MythicSkywars.getIC().show(player, "spectatesinglemenu");
                        } else if (MythicSkywars.getGameMapMgr().getPlayableArenas(GameType.SINGLE).isEmpty()) {
                            if (!MythicSkywars.getIC().hasViewers("spectateteammenu")) {
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        MythicSkywars.getIC().getMenu("jointeammenu").update();
                                    }
                                }.runTaskLater(MythicSkywars.get(), 5);
                            }
                            MythicSkywars.getIC().show(player, "spectateteammenu");
                        } else {
                            MythicSkywars.getIC().show(player, "spectatemenu");
                        }
                    }

                    return;
                }

            }

            if (event.getClickedBlock() != null && event.getClickedBlock().getType().toString().toUpperCase().contains("SIGN")) {
                Location loc = event.getClickedBlock().getLocation();
                boolean joined;
                if (!MythicSkywars.getCfg().bungeeMode()) {
                    for (GameMap gMap : MythicSkywars.getGameMapMgr().getMapsCopy()) {
                        if ((gMap.hasSign(loc) || (!event.getClickedBlock().getType().name().contains("WALL") && gMap.hasSign(loc.clone().add(0, -1, 0)))) && (gMap.getMatchState().equals(MatchState.WAITINGSTART) || gMap.getMatchState().equals(MatchState.WAITINGLOBBY))) {
                            if (player.hasPermission("sw.signs") && player.isSneaking()) {
                                return;
                            }
                            Party party = Party.getParty(player);
                            if (party != null) {
                                if (party.getLeader().equals(player.getUniqueId())) {
                                    joined = gMap.addPlayers(null, party);
                                    if (!joined) {
                                        player.sendMessage(new Messaging.MessageFormatter().format("error.could-not-join2"));
                                    }
                                } else {
                                    player.sendMessage(new Messaging.MessageFormatter().format("party.onlyleader"));
                                }
                            } else {
                                joined = gMap.addPlayers(null, player);
                                if (!joined) {
                                    player.sendMessage(new Messaging.MessageFormatter().format("error.could-not-join2"));
                                }
                            }
                        }
                    }
                } else {
                    // todo add party join support
                    SWRServer server = SWRServer.getSign(loc);
                    if (server != null) {
                        if ((server.getMatchState() == MatchState.WAITINGSTART || server.getMatchState().equals(MatchState.WAITINGLOBBY)) && server.getPlayerCount() < server.getMaxPlayers()) {
                            server.setPlayerCount(server.getPlayerCount() + 1);
                            server.updateSigns();
                            MythicSkywars.get().sendBungeeMsg(player, "Connect", server.getServerName());
                        }
                    }
                }
            }
        } else {
            if (gameMap.getMatchState() == MatchState.WAITINGSTART || gameMap.getMatchState().equals(MatchState.WAITINGLOBBY)) {
                if (MythicSkywars.getCfg().debugEnabled())
                    MythicSkywars.get().getLogger().info("PlayerInteractEvent::onClick isWaiting true");
                event.setCancelled(true);
                if (event.getItem() != null) {
                    if (MythicSkywars.getCfg().debugEnabled())
                        MythicSkywars.get().getLogger().info("PlayerInteractEvent::onClick item != null");
                    if (event.getItem().isSimilar(MythicSkywars.getIM().getItem("kitvote"))) {
                        if (MythicSkywars.getCfg().debugEnabled())
                            MythicSkywars.get().getLogger().info("PlayerInteractEvent::onClick kitvote");
                        if (MatchManager.get().getPlayerMap(player).getPlayerCard(player) == null) {
                            String sound = MythicSkywars.getNMS().getVersion() < 9 ? "VILLAGER_NO" : "ENTITY_VILLAGER_NO";
                            Util.get().playSound(player, player.getLocation(), sound, 1, 1);
                            MythicSkywars.getNMS().sendActionBar(player, new Messaging.MessageFormatter().format("game.select-team-before-kit"));
                            return;
                        }

                        if (MythicSkywars.getCfg().kitVotingEnabled()) {
                            MythicSkywars.getIC().show(player, gameMap.getKitVoteOption().getKey());
                        } else {
                            new KitSelectionMenu(player);
                        }
                        Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getOpenKitMenuSound(), 1, 1);
                        return;
                    } else if (event.getItem().isSimilar(MythicSkywars.getIM().getItem("votingItem"))) {
                        if (MythicSkywars.getCfg().debugEnabled())
                            MythicSkywars.get().getLogger().info("PlayerInteractEvent::onClick votingItem");
                        if (player.hasPermission("sw.votemenu")) {
                            if (MatchManager.get().getPlayerMap(player).getPlayerCard(player) == null) {
                                String sound = MythicSkywars.getNMS().getVersion() < 9 ? "VILLAGER_NO" : "ENTITY_VILLAGER_NO";
                                Util.get().playSound(player, player.getLocation(), sound, 1, 1);
                                MythicSkywars.getNMS().sendActionBar(player, new Messaging.MessageFormatter().format("game.select-team-before-voting"));
                                return;
                            }

                            new VotingMenu(player);
                            Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getOpenChestMenuSound(), 1, 1);
                        } else {
                            player.sendMessage(new Messaging.MessageFormatter().format("error.nopermission"));
                        }
                        return;
                    } else if (event.getItem().isSimilar(MythicSkywars.getIM().getItem("teamSelectItem"))) {
                        if (MythicSkywars.getCfg().debugEnabled())
                            MythicSkywars.get().getLogger().info("PlayerInteractEvent::onClick teamSelectItem");
                        MythicSkywars.getIC().show(player, gameMap.getName() + "teamselect");
                        if (MythicSkywars.getIC().has(gameMap.getName() + "teamselect")) {
                            MythicSkywars.getIC().getMenu(gameMap.getName() + "teamselect").update();
                        }
                        // TODO ADD TEAM SELECTION MENU + ADD SOUND
                        return;
                    } else if (event.getItem().isSimilar(MythicSkywars.getIM().getItem("exitGameItem"))) {
                        if (MythicSkywars.getCfg().debugEnabled())
                            MythicSkywars.get().getLogger().info("PlayerInteractEvent::onClick exitGameItem");
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                MythicSkywars.get().getPlayerManager().removePlayer(player, PlayerRemoveReason.PLAYER_QUIT_GAME, null, true);
                                // MatchManager.get().removeAlivePlayer(player, DamageCause.CUSTOM, true, true);
                            }
                        }.runTaskLater(MythicSkywars.get(), 1);
                    }
                }
                return;
            }
            if (gameMap.getMatchState() == MatchState.PLAYING) {
                if (player.getGameMode() != GameMode.SURVIVAL && !MatchManager.get().isSpectating(player)) {
                    // Self-heal odd transition case where a player stays in ADVENTURE after lobby->match transition.
                    player.setGameMode(GameMode.SURVIVAL);
                }
                if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    Block block = event.getClickedBlock();
                    if (block == null) {
                        return;
                    }
                    if (block != null && (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST)) {
                        ChestRefillVisualManager.get().onChestInteract(gameMap, block);
                    }
                    if (block.getType().equals(Material.ENDER_CHEST)) {
                        for (GameMap gMap : MythicSkywars.getGameMapMgr().getPlayableArenas(GameType.ALL)) {
                            for (Crate crate : gMap.getCrates()) {
                                if (crate.getLocation().equals(block.getLocation())) {
                                    event.setCancelled(true);
                                    if (MythicSkywars.getNMS().getVersion() < 9) {
                                        player.getWorld().playSound(player.getLocation(), Sound.valueOf("CHEST_OPEN"), 1, 1);
                                    } else {
                                        player.getWorld().playSound(player.getLocation(), Sound.valueOf("BLOCK_CHEST_OPEN"), 1, 1);
                                    }
                                    player.openInventory(crate.getInventory());
                                    MythicSkywars.get().getServer().getScheduler().runTaskLater(MythicSkywars.get(), () ->
                                            MythicSkywars.getNMS().playChestAction(block, true), 1
                                    );
                                    return;
                                }
                            }
                        }
                    }
                }
            }
            if (gameMap.getMatchState() == MatchState.ENDING) {
                event.setCancelled(true);
                if (event.getItem() == null) {
                    return;
                }
                ItemStack hand = event.getItem();
                if (MythicSkywars.getCfg().isPlayAgainItemEnabled()
                        && hand.isSimilar(MythicSkywars.getIM().getItem("playAgainItem"))) {
                    final GameType gameType = gameMap.getTeamSize() > 1 ? GameType.TEAM : GameType.SINGLE;
                    final boolean playAgainLucky = gameMap.isLuckyModeEnabled();
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (MatchManager.get().getPlayerMap(player) == null) {
                                return;
                            }
                            MythicSkywars.get().getPlayerManager().removePlayer(player, PlayerRemoveReason.PLAYER_QUIT_GAME, null, false, false);
                            if (MatchManager.get().joinGame(player, gameType, playAgainLucky, true) == null) {
                                player.sendMessage(new Messaging.MessageFormatter().format("error.could-not-join2"));
                            }
                        }
                    }.runTaskLater(MythicSkywars.get(), 1L);
                    return;
                }
                if (MythicSkywars.getCfg().isJoinGameItemEnabled()
                        && hand.isSimilar(MythicSkywars.getIM().getItem("joinselect"))) {
                    handleJoinSelectItem(player);
                    return;
                }
                if (hand.isSimilar(MythicSkywars.getIM().getItem("exitGameItem"))) {
                    if (MythicSkywars.getCfg().debugEnabled()) {
                        MythicSkywars.get().getLogger().info("PlayerInteractEvent::onClick exitGameItem (ending)");
                    }
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            MythicSkywars.get().getPlayerManager().removePlayer(player, PlayerRemoveReason.PLAYER_QUIT_GAME, null, true);
                        }
                    }.runTaskLater(MythicSkywars.get(), 1);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        Inventory inv = e.getInventory();
        InventoryView inView = e.getPlayer().getOpenInventory();
        if (inView.getTitle().equals(new Messaging.MessageFormatter().format("event.crateInv"))) {
            for (GameMap gMap : MythicSkywars.getGameMapMgr().getPlayableArenas(GameType.ALL)) {
                for (Crate crate : gMap.getCrates()) {
                    if (crate.getInventory().equals(inv) && inv.getViewers().size() <= 1) {
                        if (MythicSkywars.getNMS().getVersion() < 9) {
                            e.getPlayer().getWorld().playSound(e.getPlayer().getLocation(), Sound.valueOf("CHEST_CLOSE"), 1, 1);
                        } else {
                            e.getPlayer().getWorld().playSound(e.getPlayer().getLocation(), Sound.BLOCK_CHEST_CLOSE, 1, 1);
                        }
                        MythicSkywars.getNMS().playChestAction(crate.getLocation().getBlock(), false);
                        return;
                    }
                }
            }
        } else {
            String lootTitle = ChatColor.stripColor(inView.getTitle());
            if (lootTitle != null && lootTitle.contains(".yml")) {
                MythicSkywars.getCM().persistChestEditorInventory(inView.getTitle());
            }
        }

    }


    @EventHandler
    public void onInventoryClick(final InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            GameMap gMap = MatchManager.get().getPlayerMapSafe((Player) event.getWhoClicked());
            if (gMap == null) {
                Player clicker = (Player) event.getWhoClicked();
                if (Util.get().isSpawnWorld(clicker.getWorld()) && LobbyBypassManager.hasBypass(clicker)) {
                    return;
                }
                ItemStack item;
                ItemStack item2;
                if (event.getClick().equals(ClickType.NUMBER_KEY)) {
                    item = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
                    item2 = event.getCurrentItem();
                } else {
                    item = event.getCurrentItem();
                    item2 = event.getCurrentItem();
                }

                if (item != null && (item.equals(MythicSkywars.getIM().getItem("optionselect"))
                        || item.equals(MythicSkywars.getIM().getItem("statsitem"))
                        || item.equals(MythicSkywars.getIM().getItem("rejoinitem"))
                        || item.equals(MythicSkywars.getIM().getItem("joinselect"))
                        || item.equals(MythicSkywars.getIM().getItem("backlobbyitem"))
                        || item.equals(MythicSkywars.getIM().getItem("spectateselect"))
                        || ArenaSetupMenu.isTool(item))
                        || item2 != null && (item2.equals(MythicSkywars.getIM().getItem("optionselect"))
                        || item2.equals(MythicSkywars.getIM().getItem("statsitem"))
                        || item2.equals(MythicSkywars.getIM().getItem("rejoinitem"))
                        || item2.equals(MythicSkywars.getIM().getItem("joinselect"))
                        || item2.equals(MythicSkywars.getIM().getItem("backlobbyitem"))
                        || item2.equals(MythicSkywars.getIM().getItem("spectateselect"))
                        || ArenaSetupMenu.isTool(item2))) {
                    event.setCancelled(true);
                }
                String title = event.getView().getTitle();
                GameMap editorMap = MythicSkywars.getGameMapMgr().getMap(event.getWhoClicked().getWorld().getName());
                if (editorMap != null && editorMap.isEditing()) {
                    // If the arena setup menu is open, cancel all clicks (no item moving allowed)
                    if (ArenaSetupMenu.isArenaSetupMenu(title)) {
                        event.setCancelled(true);
                        ArenaSetupMenu.handleClick((Player) event.getWhoClicked(), editorMap, title, event.getRawSlot(), event.getClick());
                        return;
                    }
                    // Even outside the menu, prevent moving tools via shift-click or number keys
                    if (ArenaSetupMenu.isTool(event.getCurrentItem()) || ArenaSetupMenu.isTool(event.getCursor())) {
                        event.setCancelled(true);
                        return;
                    }
                    if (event.getClick().equals(ClickType.NUMBER_KEY)) {
                        ItemStack hotbarItem = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
                        if (ArenaSetupMenu.isTool(hotbarItem)) {
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
            } else {
                MatchState state = gMap.getMatchState();
                if (state == MatchState.ENDING) {
                    Inventory clicked = event.getClickedInventory();
                    Inventory top = event.getView().getTopInventory();
                    if (clicked != null && top != null && clicked.equals(top)
                            && top.getType() != InventoryType.PLAYER && top.getType() != InventoryType.CRAFTING) {
                        return;
                    }
                    event.setCancelled(true);
                } else if (state == MatchState.WAITINGSTART || state.equals(MatchState.WAITINGLOBBY)) {
                    event.setCancelled(true);
                }
            }
        }

    }

    @EventHandler
    public void onInventoryDrag(final InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // Prevent dragging in the arena setup menu
        GameMap editorMap = MythicSkywars.getGameMapMgr().getMap(player.getWorld().getName());
        if (editorMap != null && editorMap.isEditing()) {
            if (ArenaSetupMenu.isArenaSetupMenu(title)) {
                event.setCancelled(true);
                return;
            }
            // Prevent dragging setup tools in any inventory while editing
            if (ArenaSetupMenu.isTool(event.getOldCursor())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Open join / solo / team menus from the join compass (lobby or post-game).
     */
    private static void handleJoinSelectItem(Player player) {
        if (!MythicSkywars.getIC().has("joinmenu")) {
            return;
        }
        Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getOpenJoinMenuSound(), 1, 1);
        if (MythicSkywars.getCfg().isJoinCompassRandomJoin() && !MythicSkywars.getCfg().bungeeMode()) {
            Party party = Party.getParty(player);
            boolean joined = false;
            for (int count = 0; count < 4 && !joined; count++) {
                if (party != null) {
                    if (!party.getLeader().equals(player.getUniqueId())) {
                        player.sendMessage(new Messaging.MessageFormatter().format("party.onlyleader"));
                        return;
                    }
                    joined = MatchManager.get().joinGame(party, GameType.ALL) != null;
                } else {
                    joined = MatchManager.get().joinGame(player, GameType.ALL) != null;
                }
            }
            if (!joined) {
                player.sendMessage(new Messaging.MessageFormatter().format("error.could-not-join"));
            }
            return;
        }
        if (MythicSkywars.getGameMapMgr().getPlayableArenas(GameType.TEAM).isEmpty()) {
            if (LuckyBlockHook.isAvailable()) {
                MythicSkywars.getIC().show(player, "joinsolomodemenu");
            } else {
                JoinSingleMenu.showFor(player, false);
            }
        } else if (MythicSkywars.getGameMapMgr().getPlayableArenas(GameType.SINGLE).isEmpty()) {
            if (!MythicSkywars.getIC().hasViewers("jointeammenu")) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        MythicSkywars.getIC().getMenu("jointeammenu").update();
                    }
                }.runTaskLater(MythicSkywars.get(), 5);
            }
            MythicSkywars.getIC().show(player, "jointeammenu");
        } else {
            MythicSkywars.getIC().show(player, "joinmenu");
        }
    }

    @EventHandler
    public void onPlayerDropItem(final PlayerDropItemEvent event) {
        final GameMap gameMap = MatchManager.get().getPlayerMapSafe(event.getPlayer());
        if (gameMap == null) {
            // Prevent dropping arena setup tools while editing
            GameMap editorMap = MythicSkywars.getGameMapMgr().getMap(event.getPlayer().getWorld().getName());
            if (editorMap != null && editorMap.isEditing()) {
                if (ArenaSetupMenu.isTool(event.getItemDrop().getItemStack())) {
                    event.setCancelled(true);
                }
            }
            return;
        }
        if (gameMap.getMatchState() == MatchState.WAITINGSTART || gameMap.getMatchState() == MatchState.ENDING || gameMap.getMatchState().equals(MatchState.WAITINGLOBBY)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();
        Location blockLoc = e.getBlock().getLocation();

        GameMap playerPlayingMap = MatchManager.get().getPlayerMapSafe(player);
        if (playerPlayingMap == null) {
            if (e.getBlock().getType().equals(Material.CHEST) || e.getBlock().getType().equals(Material.TRAPPED_CHEST) || e.getBlock().getType().equals(Material.DIAMOND_BLOCK) || e.getBlock().getType().equals(Material.EMERALD_BLOCK)) {
                GameMap map = MythicSkywars.getGameMapMgr().getMap(player.getWorld().getName());
                if (map == null) {
                    return;
                }
                if (map.isEditing()) {
                    if (e.getBlock().getType().equals(Material.CHEST) || e.getBlock().getType().equals(Material.TRAPPED_CHEST)) {
                        Chest chest = (Chest) e.getBlock().getState();
                        final Location brokenLoc = e.getBlock().getLocation();
                        final World world = e.getBlock().getWorld();

                        // Remove from map
                        map.removeChest(chest);
                        InventoryHolder ih = chest.getInventory().getHolder();
                        if (ih instanceof DoubleChest) {
                            DoubleChest dc = (DoubleChest) ih;
                            Chest left = (Chest) dc.getLeftSide();
                            Chest right = (Chest) dc.getRightSide();
                            Location locLeft = left.getLocation();
                            Location locRight = right.getLocation();
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    world.getBlockAt(locLeft).setType(Material.AIR);
                                    world.getBlockAt(locRight).setType(Material.AIR);
                                }
                            }.runTaskLater(MythicSkywars.get(), 2L);
                        }
                        // Force-clean nearby chest blocks to avoid invisible/ghost leftovers
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                forceClearEditorChestBlocks(world, brokenLoc);
                            }
                        }.runTaskLater(MythicSkywars.get(), 1L);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                forceClearEditorChestBlocks(world, brokenLoc);
                            }
                        }.runTaskLater(MythicSkywars.get(), 6L);
                        player.sendMessage(new Messaging.MessageFormatter().setVariable("mapname", map.getDisplayName()).format("maps.removeChest"));
                    } else if (e.getBlock().getType().equals(Material.DIAMOND_BLOCK)) {
                        // Remove all spawns matching location and collect which ones were removed
                        Map<TeamCard, List<Integer>> result = map.removeSpawnsAtLocation(blockLoc);

                        // Send a message to the player for every spawn removed - this could be one or multiple
                        for (Map.Entry<TeamCard, List<Integer>> removedTeamLocs : result.entrySet()) {
                            int teamCardPos = map.getTeamCardPosition(removedTeamLocs.getKey());
                            if (teamCardPos == -1) {
                                teamCardPos = map.getTeamCards().size();
                            }
                            for (Integer spawnIndex : removedTeamLocs.getValue()) {
                                player.sendMessage(new Messaging.MessageFormatter()
                                        // Convert spawn index to human number
                                        .setVariable("num", "" + (spawnIndex + 1))
                                        // Convert team index to human number
                                        .setVariable("team", "" + (teamCardPos + 1))
                                        .setVariable("mapname", map.getDisplayName())
                                        .format("maps.spawnRemoved"));
                            }
                        }
                    } else if (e.getBlock().getType().equals(Material.EMERALD_BLOCK)) {
                        boolean result = map.removeDeathMatchSpawn(blockLoc);
                        if (result) {
                            player.sendMessage(new Messaging.MessageFormatter().setVariable("num", "" + (map.getDeathMatchSpawns().size() + 1)).setVariable("mapname", map.getDisplayName()).format("maps.deathSpawnRemoved"));
                        }
                    }
                }
            }
            return;
        }
        if (playerPlayingMap.getMatchState().equals(MatchState.WAITINGSTART) || playerPlayingMap.getMatchState().equals(MatchState.WAITINGLOBBY)) {
            e.setCancelled(true);
            new BukkitRunnable() {
                @Override
                public void run() {
                    CoordLoc spawn = playerPlayingMap.getPlayerCard(player).getSpawn();
                    player.teleport(new Location(playerPlayingMap.getCurrentWorld(), spawn.getX() + 0.5, spawn.getY() + 1, spawn.getZ() + 0.5));
                }
            }.runTaskLater(MythicSkywars.get(), 2);
        }
        if (playerPlayingMap.getMatchState().equals(MatchState.PLAYING)) {
            if (player.getGameMode() != GameMode.SURVIVAL && !MatchManager.get().isSpectating(player)) {
                player.setGameMode(GameMode.SURVIVAL);
            }
            Block block = e.getBlock();
            if (block.getType().equals(Material.ENDER_CHEST)) {
                for (Crate crate : playerPlayingMap.getCrates()) {
                    if (crate.getLocation().equals(block.getLocation())) {
                        e.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlaced(BlockPlaceEvent e) {
        GameMap gameMap = MatchManager.get().getPlayerMapSafe(e.getPlayer());
        if (gameMap != null && gameMap.getMatchState() == MatchState.PLAYING
                && e.getPlayer().getGameMode() != GameMode.SURVIVAL
                && !MatchManager.get().isSpectating(e.getPlayer())) {
            e.getPlayer().setGameMode(GameMode.SURVIVAL);
        }
        if (gameMap != null || !(e.getBlockPlaced().getState() instanceof Chest)) {
            return;
        }

        GameMap worldMap = MythicSkywars.getGameMapMgr().getMap(e.getPlayer().getWorld().getName());
        if (worldMap == null || !worldMap.isEditing()) {
            return;
        }

        Location loc = e.getBlock().getLocation();
        Player player = e.getPlayer();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!(loc.getBlock().getState() instanceof Chest)) {
                    return;
                }
                worldMap.addChest((Chest) loc.getBlock().getState(), worldMap.getChestPlacementType());
                if (worldMap.getChestPlacementType() == ChestPlacementType.NORMAL) {
                    player.sendMessage(new Messaging.MessageFormatter()
                            .setVariable("mapname", worldMap.getDisplayName())
                            .format("maps.addChest")
                    );
                } else if (worldMap.getChestPlacementType() == ChestPlacementType.CENTER) {
                    player.sendMessage(new Messaging.MessageFormatter()
                            .setVariable("mapname", worldMap.getDisplayName())
                            .format("maps.addCenterChest")
                    );
                }
            }
        }.runTaskLater(MythicSkywars.get(), 2L);
    }

    @EventHandler
    public void onPlayerWalk(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        String playerUuid = player.getUniqueId().toString();
        for (GameMap gMap : MythicSkywars.getGameMapMgr().getPlayableArenas(GameType.ALL)) {
            if (!gMap.getDeathMatchWaiters().contains(playerUuid)) {
                continue;
            }

            if (event.getFrom().getBlockX() != event.getTo().getBlockX() || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
                event.setCancelled(true);
            }
        }
    }

    private void forceClearEditorChestBlocks(World world, Location center) {
        if (world == null || center == null) {
            return;
        }
        int x = center.getBlockX();
        int y = center.getBlockY();
        int z = center.getBlockZ();
        int[][] offsets = new int[][]{
                {0, 0, 0},
                {1, 0, 0},
                {-1, 0, 0},
                {0, 0, 1},
                {0, 0, -1}
        };
        for (int[] off : offsets) {
            Block b = world.getBlockAt(x + off[0], y + off[1], z + off[2]);
            Material type = b.getType();
            if (type == Material.CHEST || type == Material.TRAPPED_CHEST) {
                b.setType(Material.AIR);
            }
        }
    }

    /**
     * Handles the Team Spawner Tool - teleports player up, checks for free space, and sets team spawn
     */
    private void handleTeamSpawnerTool(Player player, GameMap gMap) {
        Location currentLoc = player.getLocation();
        Location targetLoc = currentLoc.clone().add(0, 6, 0);
        
        // Check if there's a 3x3 free space at the target location
        if (!hasFreeCuboidSpace(targetLoc, 3, 3, 3)) {
            player.sendMessage(ChatColor.RED + "Not enough free space 6 blocks above! Need a 3x3x3 area.");
            return;
        }
        
        // Teleport player to the target location
        targetLoc.setPitch(currentLoc.getPitch());
        targetLoc.setYaw(currentLoc.getYaw());
        player.teleport(targetLoc);
        
        // Set the team spawn at the new location
        Location spawnLoc = player.getLocation();
        if (gMap.getTeamSize() == 1 || !MythicSkywars.getCfg().isUseSeparateCages()) {
            int teamIndex = gMap.getTeamCards().size();
            TeamCard team = gMap.getTeamCardByIndex(teamIndex);
            if (team == null) {
                gMap.addTeamCard(Lists.newArrayList(new CoordLoc(spawnLoc)));
            } else {
                gMap.addSpawnLocationForTeam(team, spawnLoc);
            }
            spawnLoc.getBlock().setType(Material.DIAMOND_BLOCK);
            player.sendMessage(new Messaging.MessageFormatter()
                    .setVariable("mapname", gMap.getDisplayName())
                    .setVariable("num", "" + gMap.getMaxPlayers())
                    .format("maps.addSpawn"));
        } else {
            int teamIndex = gMap.getTeamCards().size() + 1;
            gMap.addTeamCard(Lists.newArrayList(new CoordLoc(spawnLoc)));
            spawnLoc.getBlock().setType(Material.DIAMOND_BLOCK);
            player.sendMessage(new Messaging.MessageFormatter()
                    .setVariable("team", String.valueOf(teamIndex))
                    .format("maps.editor.team-spawn-added"));
        }
        
        player.sendMessage(ChatColor.GREEN + "Teleported up 6 blocks and set team spawn!");
    }
    
    /**
     * Checks if there's free space in a cuboid area
     */
    private boolean hasFreeCuboidSpace(Location center, int width, int height, int depth) {
        World world = center.getWorld();
        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();
        
        // Check a 3x3x3 area centered on the location
        int halfWidth = width / 2;
        int halfDepth = depth / 2;
        
        for (int x = centerX - halfWidth; x <= centerX + halfWidth; x++) {
            for (int y = centerY; y < centerY + height; y++) {
                for (int z = centerZ - halfDepth; z <= centerZ + halfDepth; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() != Material.AIR) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }

    /**
     * Handles the Deathmatch Spawner Tool - sets a deathmatch spawn at the player's location
     * and places an emerald block marker.
     */
    private void handleDeathmatchSpawnerTool(Player player, GameMap gMap) {
        Location loc = player.getLocation();
        gMap.addDeathMatchSpawn(loc);
        loc.getBlock().setType(Material.EMERALD_BLOCK);
        player.sendMessage(new Messaging.MessageFormatter()
                .setVariable("num", "" + gMap.getDeathMatchSpawns().size())
                .setVariable("mapname", gMap.getDisplayName())
                .format("maps.addDeathSpawn"));
    }

    /**
     * Handles toggling a chest's type between NORMAL and CENTER when right-clicked with the blaze rod.
     */
    private void handleChestTypeToggle(Player player, GameMap gMap, Block block) {
        if (!(block.getState() instanceof Chest)) {
            return;
        }
        Chest chest = (Chest) block.getState();
        CoordLoc loc = new CoordLoc(chest.getX(), chest.getY(), chest.getZ());

        // Check if it's currently a normal chest
        if (gMap.getChests().contains(loc)) {
            gMap.removeChest(chest);
            gMap.addChest(chest, ChestPlacementType.CENTER);
            player.sendMessage(ChatColor.GREEN + "Chest converted to " + ChatColor.GOLD + "CENTER" + ChatColor.GREEN + " type.");
        } else if (gMap.getCenterChests().contains(loc)) {
            gMap.removeChest(chest);
            gMap.addChest(chest, ChestPlacementType.NORMAL);
            player.sendMessage(ChatColor.GREEN + "Chest converted to " + ChatColor.AQUA + "NORMAL" + ChatColor.GREEN + " type.");
        } else {
            // Chest not registered yet, register as normal
            gMap.addChest(chest, ChestPlacementType.NORMAL);
            player.sendMessage(ChatColor.GREEN + "Chest registered as " + ChatColor.AQUA + "NORMAL" + ChatColor.GREEN + " type. Right click again to toggle.");
        }
    }
}