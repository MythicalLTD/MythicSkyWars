package systems.mythical.mythicskywars.menus;

import com.google.common.collect.Lists;
import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.enums.GameType;
import systems.mythical.mythicskywars.enums.MatchState;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.managers.MatchManager;
import systems.mythical.mythicskywars.utilities.Messaging;
import systems.mythical.mythicskywars.utilities.Party;
import systems.mythical.mythicskywars.utilities.SWRServer;
import systems.mythical.mythicskywars.utilities.LuckyBlockHook;
import systems.mythical.mythicskywars.utilities.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class JoinSingleMenu {

    private static final String menuName = new Messaging.MessageFormatter().format("menu.joinsinglegame-menu-title");
    private static int menuSize = 45;
    /** Bottom-row slot: left of exit (IconMenu uses size - 5 for exit). */
    private static final int AUTOJOIN_SLOT_FROM_END = 6;
    public static Map<Integer, String> arenaSlots = new HashMap<>();
    private static final Set<UUID> luckySoloSelection = new HashSet<>();

    public static void showFor(Player player, boolean luckyMode) {
        if (luckyMode && !LuckyBlockHook.isAvailable()) {
            player.sendMessage(new Messaging.MessageFormatter().format("error.could-not-join2"));
            Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getErrorSound(), 1, 1);
            luckySoloSelection.remove(player.getUniqueId());
            return;
        }
        if (luckyMode) {
            luckySoloSelection.add(player.getUniqueId());
        } else {
            luckySoloSelection.remove(player.getUniqueId());
        }
        if (!MythicSkywars.getIC().hasViewers("joinsinglemenu")) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    MythicSkywars.getIC().getMenu("joinsinglemenu").update();
                }
            }.runTaskLater(MythicSkywars.get(), 5);
        }
        MythicSkywars.getIC().show(player, "joinsinglemenu");
    }

    private static boolean wantsLucky(Player player) {
        return player != null && luckySoloSelection.contains(player.getUniqueId());
    }

    /** Prefer WAITINGSTART (countdown) over WAITINGLOBBY, then busier games. */
    private static final Comparator<SWRServer> AUTOJOIN_SERVER_ORDER = new Comparator<SWRServer>() {
        @Override
        public int compare(SWRServer a, SWRServer b) {
            int ra = rankServerWaiting(a.getMatchState());
            int rb = rankServerWaiting(b.getMatchState());
            if (ra != rb) {
                return Integer.compare(ra, rb);
            }
            return Integer.compare(b.getPlayerCount(), a.getPlayerCount());
        }
    };

    private static int rankServerWaiting(MatchState s) {
        if (s == MatchState.WAITINGSTART) {
            return 0;
        }
        if (s == MatchState.WAITINGLOBBY) {
            return 1;
        }
        return 99;
    }

    private static ItemStack createAutojoinSoloItem() {
        List<String> lore = new ArrayList<>();
        lore.add(new Messaging.MessageFormatter().format("menu.autojoin-solo-lore1"));
        return MythicSkywars.getNMS().getItemStack(
                new ItemStack(Material.NETHER_STAR, 1),
                lore,
                new Messaging.MessageFormatter().format("menu.autojoin-solo-title"));
    }

    private static void placeAutojoinSoloButton(Iterable<Inventory> inventories) {
        ItemStack button = createAutojoinSoloItem();
        for (Inventory inv : inventories) {
            inv.setItem(inv.getSize() - AUTOJOIN_SLOT_FROM_END, button.clone());
        }
    }

    private static void placeNavButtons(ArrayList<Inventory> inventories) {
        for (int i = 0; i < inventories.size(); i++) {
            Inventory inv = inventories.get(i);
            int size = inv.getSize();
            // Always place exit button
            inv.setItem(size - 5, MythicSkywars.getIM().getItem("exitMenuItem"));
            // Search button (slot size - 7, two left of exit)
            ItemStack searchItem = MythicSkywars.getNMS().getItemStack(
                    new ItemStack(searchIconMaterial(), 1),
                    java.util.Collections.singletonList(ChatColor.GRAY + "Type a map name to filter"),
                    ChatColor.GREEN + "Search");
            inv.setItem(size - 7, searchItem);
            // Previous button (first slot of bottom row) - skip for first page
            if (i > 0) {
                inv.setItem(size - 9, MythicSkywars.getIM().getItem("prevPageItem"));
            }
            // Next button (last slot of bottom row) - skip for last page
            if (i < inventories.size() - 1) {
                inv.setItem(size - 1, MythicSkywars.getIM().getItem("nextPageItem"));
            }
        }
    }

    private static Material searchIconMaterial() {
        try {
            return Material.valueOf("NAME_TAG");
        } catch (IllegalArgumentException e) {
            return Material.PAPER;
        }
    }

    private static void performAutojoinSolo(Player player) {
        if (!player.hasPermission("sw.join")) {
            player.sendMessage(new Messaging.MessageFormatter().format("error.nopermission"));
            return;
        }
        Party party = Party.getParty(player);
        boolean lucky = wantsLucky(player);
        if (lucky && !LuckyBlockHook.isAvailable()) {
            player.sendMessage(new Messaging.MessageFormatter().format("error.could-not-join2"));
            Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getErrorSound(), 1, 1);
            luckySoloSelection.remove(player.getUniqueId());
            return;
        }
        if (lucky && party != null) {
            player.sendMessage(new Messaging.MessageFormatter().format("error.lucky-solo-only"));
            return;
        }
        if (MythicSkywars.getCfg().bungeeMode() && MythicSkywars.getCfg().isLobbyServer()) {
            if (party != null && !party.getLeader().equals(player.getUniqueId())) {
                player.sendMessage(new Messaging.MessageFormatter().format("party.onlyleader"));
                return;
            }
            ArrayList<SWRServer> candidates = Lists.newArrayList();
            for (SWRServer s : SWRServer.getServersCopy()) {
                if (s.getTeamSize() != 1) {
                    continue;
                }
                MatchState st = s.getMatchState();
                if (st != MatchState.WAITINGSTART && st != MatchState.WAITINGLOBBY) {
                    continue;
                }
                if (party != null) {
                    if (party.getLeader().equals(player.getUniqueId()) && s.canAddParty(party)) {
                        candidates.add(s);
                    }
                } else if (s.canAddPlayer()) {
                    candidates.add(s);
                }
            }
            Collections.sort(candidates, AUTOJOIN_SERVER_ORDER);
            for (SWRServer server : candidates) {
                player.closeInventory();
                if (party != null) {
                    server.setPlayerCount(server.getPlayerCount() + party.getSize() - 1);
                    server.updateSigns();
                    for (int i = 0; i < party.getSize(); i++) {
                        Player m = Bukkit.getPlayer(party.getMembers().get(i));
                        if (m != null) {
                            MythicSkywars.get().sendBungeeMsg(m, "Connect", server.getServerName());
                        }
                    }
                } else {
                    server.setPlayerCount(server.getPlayerCount() + 1);
                    server.updateSigns();
                    MythicSkywars.get().sendBungeeMsg(player, "Connect", server.getServerName());
                }
                return;
            }
            Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getErrorSound(), 1, 1);
            player.sendMessage(new Messaging.MessageFormatter().format("error.could-not-join2"));
            return;
        }
        if (party != null) {
            if (!party.getLeader().equals(player.getUniqueId())) {
                player.sendMessage(new Messaging.MessageFormatter().format("party.onlyleader"));
                return;
            }
            player.closeInventory();
            if (MatchManager.get().joinGame(party, GameType.SINGLE, lucky) == null) {
                player.sendMessage(new Messaging.MessageFormatter().format("error.could-not-join2"));
                Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getErrorSound(), 1, 1);
            }
        } else {
            player.closeInventory();
            if (MatchManager.get().joinGame(player, GameType.SINGLE, lucky, true) == null) {
                player.sendMessage(new Messaging.MessageFormatter().format("error.could-not-join2"));
                Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getErrorSound(), 1, 1);
            }
        }
    }


    public JoinSingleMenu() {

        Inventory menu = Bukkit.createInventory(null, menuSize + 9, menuName);
        ArrayList<Inventory> invs = new ArrayList<>();
        invs.add(menu);

        Runnable update = () -> {
            if ((MythicSkywars.getIC().hasViewers("joinsinglemenu") || MythicSkywars.getIC().hasViewers("spectatesinglemenu"))) {
                ArrayList<GameMap> normalGames = MythicSkywars.getGameMapMgr().getPlayableArenas(GameType.SINGLE);
                ArrayList<SWRServer> bungeeGames = Lists.newArrayList();
                
                for (SWRServer s : SWRServer.getServersCopy()) {
                    if (s.getTeamSize() == 1) {
                        bungeeGames.add(s);
                    }
                }
                
                ArrayList<Inventory> invs1 = MythicSkywars.getIC().getMenu("joinsinglemenu").getInventories();

                for (Inventory inv : invs1) {
                    for (int i = 0; i < menuSize; i++) {
                        inv.setItem(i, new ItemStack(Material.AIR, 1));
                    }
                }

                int gameSize = MythicSkywars.getCfg().bungeeMode() && MythicSkywars.getCfg().isLobbyServer() ? bungeeGames.size() : normalGames.size();
                
                for (int iii = 0; iii < gameSize; iii++) {
                    int invent = Math.floorDiv(iii, menuSize);
                    if (invs1.isEmpty() || invs1.size() < invent + 1) {
                        invs1.add(Bukkit.createInventory(null, menuSize + 9, menuName));
                    }

                    MatchState state;
                    int alivePlayers = 0;
                    int maxPlayers = 0;
                    String displayName = "";
                    int teamsize = 1;
                    String name = "";
                    
                    GameMap gMap = null;
                    SWRServer server = null;
                    if (!MythicSkywars.getCfg().bungeeMode() || !MythicSkywars.getCfg().isLobbyServer()) {
                        gMap = normalGames.get(iii);
                        state = gMap.getMatchState();
                        alivePlayers = gMap.getAlivePlayers().size();
                        maxPlayers = gMap.getMaxPlayers();
                        displayName = gMap.getDisplayName();
                        teamsize = gMap.getTeamSize();
                        name = gMap.getName();
                    }
                    else {
                        server = bungeeGames.get(iii);
                        state = server.getMatchState();
                        alivePlayers = server.getPlayerCount();
                        maxPlayers = server.getMaxPlayers();
                        displayName = server.getDisplayName();
                        teamsize = server.getTeamSize();
                        name = server.getServerName();
                    }

                    

                    List<String> loreList = Lists.newLinkedList();
                    if (state != MatchState.OFFLINE) {
                        if (state == MatchState.WAITINGSTART || state == MatchState.WAITINGLOBBY) {
                            for (String a : MythicSkywars.getMessaging().getFile().getStringList("menu.join_menu.lore.waiting-start")) {
                                loreList.add(ChatColor.translateAlternateColorCodes('&',
                                        a.replace("{playercount}", "" + alivePlayers)
                                                .replace("{maxplayers}", "" + maxPlayers)
                                                .replace("{arena}", displayName)
                                                .replace("{teamsize}", teamsize + "")
                                                .replace("{aliveplayers}", alivePlayers + "")
                                                .replace("{name}", name)
                                ));
                            }
                        } else if (state.equals(MatchState.PLAYING)) {
                            for (String a : MythicSkywars.getMessaging().getFile().getStringList("menu.join_menu.lore.playing")) {
                                loreList.add(ChatColor.translateAlternateColorCodes('&',
                                        a.replace("{playercount}", "" + alivePlayers)
                                                .replace("{maxplayers}", "" + maxPlayers)
                                                .replace("{arena}", displayName)
                                                .replace("{teamsize}", teamsize + "")
                                                .replace("{aliveplayers}", alivePlayers + "")
                                                .replace("{name}", name)
                                ));
                            }
                        } else if (state.equals(MatchState.ENDING)) {
                            for (String a : MythicSkywars.getMessaging().getFile().getStringList("menu.join_menu.lore.ending")) {
                                loreList.add(ChatColor.translateAlternateColorCodes('&',
                                        a.replace("{playercount}", "" + alivePlayers)
                                                .replace("{maxplayers}", "" + maxPlayers)
                                                .replace("{arena}", displayName)
                                                .replace("{teamsize}", teamsize + "")
                                                .replace("{aliveplayers}", alivePlayers + "")
                                                .replace("{name}", name)
                                ));
                            }
                        }

                        double xy = ((double) (alivePlayers / maxPlayers));

                        ItemStack gameIcon = MythicSkywars.getNMS().getItemStack(MythicSkywars.getIM().getItem("blockwaiting"), loreList, ChatColor.translateAlternateColorCodes('&', displayName));

                        ItemStack customIcon = null;
                        if (gMap != null && gMap.getCustomJoinMenuItemEnabled()) {
                            customIcon = gMap.getCustomJoinMenuItem();
                        } else {
                            if (state.equals(MatchState.PLAYING)) {
                                customIcon = MythicSkywars.getIM().getItem("blockplaying");
                            } else if (state.equals(MatchState.ENDING)) {
                                customIcon = MythicSkywars.getIM().getItem("blockending");
                            } else if (state.equals(MatchState.WAITINGSTART) || state.equals(MatchState.WAITINGLOBBY)) {
                                customIcon = MythicSkywars.getIM().getItem("almostfull");
                                if (xy < 0.25) {
                                    customIcon = MythicSkywars.getIM().getItem("almostempty");
                                } else if (xy < 0.5) {
                                    customIcon = MythicSkywars.getIM().getItem("halffull");
                                } else if (xy < 0.75) {
                                    customIcon = MythicSkywars.getIM().getItem("threefull");
                                }
                            }
                        }

                        if (state.equals(MatchState.PLAYING)) {
                            gameIcon = MythicSkywars.getNMS().getItemStack(customIcon, loreList, ChatColor.translateAlternateColorCodes('&',
                                    new Messaging.MessageFormatter()
                                    .setVariable("playercount", "" + alivePlayers)
                                            .setVariable("maxplayers", "" + maxPlayers)
                                            .setVariable("arena", displayName)
                                            .setVariable("teamsize", teamsize + "")
                                            .setVariable("aliveplayers", alivePlayers + "")
                                            .setVariable("name", name)
                                            .format("menu.join_menu.item_title.playing"))
                            );
                        } else if (state.equals(MatchState.ENDING)) {
                            gameIcon = MythicSkywars.getNMS().getItemStack(customIcon, loreList, ChatColor.translateAlternateColorCodes('&',
                                    new Messaging.MessageFormatter()
                                            .setVariable("playercount", "" + alivePlayers)
                                            .setVariable("maxplayers", "" + maxPlayers)
                                            .setVariable("arena", displayName)
                                            .setVariable("teamsize", teamsize + "")
                                            .setVariable("aliveplayers", alivePlayers + "")
                                            .setVariable("name", name)
                                            .format("menu.join_menu.item_title.ending"))

                            );
                        } else if (state == MatchState.WAITINGSTART || state == MatchState.WAITINGLOBBY) {
                            gameIcon = MythicSkywars.getNMS().getItemStack(customIcon, loreList, ChatColor.translateAlternateColorCodes('&',
                                    new Messaging.MessageFormatter()
                                            .setVariable("playercount", "" + alivePlayers)
                                            .setVariable("maxplayers", "" + maxPlayers)
                                            .setVariable("arena", displayName)
                                            .setVariable("teamsize", teamsize + "")
                                            .setVariable("aliveplayers", alivePlayers + "")
                                            .setVariable("name", name)
                                            .format("menu.join_menu.item_title.waiting-start"))

                            );
                            if (xy < 0.75) {
                                gameIcon = MythicSkywars.getNMS().getItemStack(customIcon, loreList, ChatColor.translateAlternateColorCodes('&',
                                        new Messaging.MessageFormatter()
                                                .setVariable("playercount", "" + alivePlayers)
                                                .setVariable("maxplayers", "" + maxPlayers)
                                                .setVariable("arena", displayName)
                                                .setVariable("teamsize", teamsize + "")
                                                .setVariable("aliveplayers", alivePlayers + "")
                                                .setVariable("name", name)
                                                .format("menu.join_menu.item_title.waiting-start"))

                                );
                            }
                            if (xy < 0.50) {
                                gameIcon = MythicSkywars.getNMS().getItemStack(customIcon, loreList, ChatColor.translateAlternateColorCodes('&',
                                        new Messaging.MessageFormatter()
                                                .setVariable("playercount", "" + alivePlayers)
                                                .setVariable("maxplayers", "" + maxPlayers)
                                                .setVariable("arena", displayName)
                                                .setVariable("teamsize", teamsize + "")
                                                .setVariable("aliveplayers", alivePlayers + "")
                                                .setVariable("name", name)
                                                .format("menu.join_menu.item_title.waiting-start"))

                                );
                            }
                            if (xy < 0.25) {
                                gameIcon = MythicSkywars.getNMS().getItemStack(customIcon, loreList, ChatColor.translateAlternateColorCodes('&',
                                        new Messaging.MessageFormatter()
                                                .setVariable("playercount", "" + alivePlayers)
                                                .setVariable("maxplayers", "" + maxPlayers)
                                                .setVariable("arena", displayName)
                                                .setVariable("teamsize", teamsize + "")
                                                .setVariable("aliveplayers", alivePlayers + "")
                                                .setVariable("name", name)
                                                .format("menu.join_menu.item_title.waiting-start"))

                                );
                            }
                        }
                        invs1.get(invent).setItem(iii % menuSize, gameIcon);
                        arenaSlots.put(iii % menuSize, name);
                    }
                }
                if (MythicSkywars.getCfg().spectateMenuEnabled() && !MythicSkywars.getCfg().bungeeMode()) {
                    ArrayList<Inventory> specs = MythicSkywars.getIC().getMenu("spectatesinglemenu").getInventories();
                    int i = 0;
                    for (Inventory inv : invs1) {
                        if (i >= specs.size()) {
                            specs.add(Bukkit.createInventory(null, menuSize + 9, new Messaging.MessageFormatter().format("menu.spectatesinglegame-menu-title")));
                        }
                        // Copy all slots including bottom nav row
                        for (int slot = 0; slot < inv.getSize(); slot++) {
                            specs.get(i).setItem(slot, inv.getItem(slot));
                        }
                        i++;
                    }
                }
                placeNavButtons(invs1);
            }
        };

        MythicSkywars.getIC().create("joinsinglemenu", invs, event -> {
            Player player = event.getPlayer();
            GameMap gMap = MatchManager.get().getPlayerMap(player);
            if (gMap != null && gMap.getMatchState() != MatchState.ENDING) {
                return;
            }

            String name = event.getName();
            if (name.equalsIgnoreCase(MythicSkywars.getNMS().getItemName(MythicSkywars.getIM().getItem("exitMenuItem")))) {
                player.closeInventory();
                return;
            }

            // Search button clicked
            if (ChatColor.stripColor(name).equalsIgnoreCase("Search")) {
                player.closeInventory();
                player.sendMessage(ChatColor.GREEN + "[SkyWars] " + ChatColor.GRAY + "Type a map name to search (or 'cancel' to cancel):");
                MapSearchListener.startSearch(player, "solo");
                return;
            }

            if (!arenaSlots.containsKey(event.getSlot())) {
                return;
            }

            gMap = null;
            SWRServer server = null;
            MatchState state;

            if (MythicSkywars.getCfg().bungeeMode() && MythicSkywars.getCfg().isLobbyServer()) {
                server = SWRServer.getServer(arenaSlots.get(event.getSlot()));
                if (server == null) {
                    return;
                }
                state = server.getMatchState();
            }
            else {
                gMap= MythicSkywars.getGameMapMgr().getMap(arenaSlots.get(event.getSlot()));
                if (gMap == null) {
                    return;
                }
                state = gMap.getMatchState();
            }



            if (state != MatchState.WAITINGSTART && state != MatchState.WAITINGLOBBY) {
                Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getErrorSound(), 1, 1);
                return;
            }

            if (player.hasPermission("sw.join")) {
                boolean joined;
                Party party = Party.getParty(player);
                if (party != null) {
                    for (UUID memberId : party.getMembers()) {
                        Player member = Bukkit.getPlayer(memberId);
                        if (member != null) {
                            MatchManager.get().leavePostGameScreenIfNeeded(member);
                        }
                    }
                } else {
                    MatchManager.get().leavePostGameScreenIfNeeded(player);
                }
                if (party != null) {
                    if (party.getLeader().equals(player.getUniqueId())) {
                        if (wantsLucky(player)) {
                            player.sendMessage(new Messaging.MessageFormatter().format("error.lucky-solo-only"));
                            return;
                        }
                        if (gMap != null && gMap.canAddParty(party)) {
                            player.closeInventory();
                            joined = gMap.addPlayers(null, party);
                            if (!joined) {
                                player.sendMessage(new Messaging.MessageFormatter().format("error.could-not-join2"));
                            }
                        }
                        else if (server != null && server.canAddParty(party)) {
                            player.closeInventory();
                            server.setPlayerCount(server.getPlayerCount() + party.getSize()-1);
                            server.updateSigns();
                            for (int i =0; i<party.getSize();i++) {
                                MythicSkywars.get().sendBungeeMsg(Bukkit.getPlayer(party.getMembers().get(i)), "Connect", server.getServerName());
                            }
                        }
                    } else {
                        player.closeInventory();
                        player.sendMessage(new Messaging.MessageFormatter().format("party.onlyleader"));
                    }
                } else {
                    if (gMap != null && gMap.canAddPlayer(player)) {
                        player.closeInventory();
                        boolean lucky = wantsLucky(player);
                        joined = MatchManager.get().joinGame(player, GameType.SINGLE, gMap.getName(), lucky) != null;
                        if (!joined) {
                            player.sendMessage(new Messaging.MessageFormatter().format("error.could-not-join2"));
                        }
                    }
                    else if (server != null && server.canAddPlayer()) {
                        player.closeInventory();
                        server.setPlayerCount(server.getPlayerCount() + 1);
                        server.updateSigns();
                        MythicSkywars.get().sendBungeeMsg(player, "Connect", server.getServerName());
                    }
                }
            }
        });
        placeNavButtons(MythicSkywars.getIC().getMenu("joinsinglemenu").getInventories());
        MythicSkywars.getIC().getMenu("joinsinglemenu").setUpdate(update);
    }
}