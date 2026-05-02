package systems.mythical.mythicskywars.menus;

import com.google.common.collect.Lists;
import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.enums.GameType;
import systems.mythical.mythicskywars.enums.MatchState;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.managers.MatchManager;
import systems.mythical.mythicskywars.utilities.Messaging;
import systems.mythical.mythicskywars.utilities.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles map search: player clicks search button, types in chat, gets filtered results in a menu.
 */
public class MapSearchListener implements Listener {

    private static final Map<UUID, String> SEARCHING = new ConcurrentHashMap<>();

    /**
     * Start a search for a player.
     * @param player the player
     * @param mode "solo" or "team"
     */
    public static void startSearch(Player player, String mode) {
        SEARCHING.put(player.getUniqueId(), mode);
    }

    public static boolean isSearching(Player player) {
        return SEARCHING.containsKey(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!SEARCHING.containsKey(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        String mode = SEARCHING.remove(player.getUniqueId());
        String query = event.getMessage().trim();

        if (query.equalsIgnoreCase("cancel")) {
            player.sendMessage(new Messaging.MessageFormatter().format("items.search-cancelled"));
            return;
        }

        // Run on main thread
        new BukkitRunnable() {
            @Override
            public void run() {
                openSearchResults(player, query, mode);
            }
        }.runTask(MythicSkywars.get());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        SEARCHING.remove(event.getPlayer().getUniqueId());
    }

    private static void openSearchResults(Player player, String query, String mode) {
        GameType type = "team".equalsIgnoreCase(mode) ? GameType.TEAM : GameType.SINGLE;
        ArrayList<GameMap> allMaps = MythicSkywars.getGameMapMgr().getPlayableArenas(type);

        // Filter by query (case-insensitive contains match on name or display name)
        String lowerQuery = query.toLowerCase(Locale.ENGLISH);
        List<GameMap> results = new ArrayList<>();
        for (GameMap gMap : allMaps) {
            if (gMap.getName().toLowerCase(Locale.ENGLISH).contains(lowerQuery)
                    || gMap.getDisplayName().toLowerCase(Locale.ENGLISH).contains(lowerQuery)) {
                results.add(gMap);
            }
        }

        if (results.isEmpty()) {
            player.sendMessage(new Messaging.MessageFormatter().setVariable("query", query).format("items.search-no-results"));
            return;
        }

        // Build a results inventory
        int menuSize = Math.min(45, ((results.size() / 9) + 1) * 9);
        if (menuSize < 9) menuSize = 9;
        String title = ChatColor.DARK_GRAY + "Search: " + ChatColor.WHITE + query;
        if (title.length() > 32) {
            title = title.substring(0, 32);
        }
        Inventory inv = Bukkit.createInventory(null, menuSize + 9, title);

        Map<Integer, String> resultSlots = new HashMap<>();
        for (int i = 0; i < results.size() && i < menuSize; i++) {
            GameMap gMap = results.get(i);
            MatchState state = gMap.getMatchState();
            int players = state == MatchState.WAITINGLOBBY ? gMap.getWaitingPlayers().size() : gMap.getAlivePlayers().size();
            int max = gMap.getMaxPlayers();

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "State: " + getStateColor(state) + state.name());
            lore.add(ChatColor.GRAY + "Players: " + ChatColor.WHITE + players + "/" + max);
            if (gMap.getTeamSize() > 1) {
                lore.add(ChatColor.GRAY + "Team size: " + ChatColor.WHITE + gMap.getTeamSize());
            }
            lore.add("");
            if (state == MatchState.WAITINGSTART || state == MatchState.WAITINGLOBBY) {
                lore.add(ChatColor.GREEN + "Click to join!");
            } else if (state == MatchState.PLAYING) {
                lore.add(ChatColor.YELLOW + "Click to spectate");
            }

            Material icon = getStateIcon(state);
            ItemStack item = MythicSkywars.getNMS().getItemStack(
                    new ItemStack(icon, Math.max(1, players)),
                    lore,
                    ChatColor.translateAlternateColorCodes('&', gMap.getDisplayName()));
            inv.setItem(i, item);
            resultSlots.put(i, gMap.getName());
        }

        // Exit button
        inv.setItem(inv.getSize() - 5, MythicSkywars.getIM().getItem("exitMenuItem"));

        ArrayList<Inventory> invs = new ArrayList<>();
        invs.add(inv);

        MythicSkywars.getIC().create(player, invs, event -> {
            String clickedName = event.getName();
            if (clickedName.equalsIgnoreCase(MythicSkywars.getNMS().getItemName(MythicSkywars.getIM().getItem("exitMenuItem")))) {
                player.closeInventory();
                return;
            }

            if (!resultSlots.containsKey(event.getSlot())) {
                return;
            }

            String mapName = resultSlots.get(event.getSlot());
            GameMap gMap = MythicSkywars.getGameMapMgr().getMap(mapName);
            if (gMap == null) return;

            player.closeInventory();
            if (gMap.getMatchState() == MatchState.WAITINGSTART || gMap.getMatchState() == MatchState.WAITINGLOBBY) {
                if (player.hasPermission("sw.join")) {
                    GameType joinType = gMap.getTeamSize() > 1 ? GameType.TEAM : GameType.SINGLE;
                    MatchManager.get().joinGame(player, joinType, gMap.getName());
                }
            } else if (gMap.getMatchState() == MatchState.PLAYING) {
                if (player.hasPermission("sw.spectate")) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            MythicSkywars.get().getPlayerManager().addSpectator(gMap, player);
                        }
                    }.runTaskLater(MythicSkywars.get(), 5);
                }
            }
        });

        MythicSkywars.getIC().show(player, null);
        player.sendMessage(new Messaging.MessageFormatter().setVariable("count", "" + results.size()).setVariable("query", query).format("items.search-results"));
    }

    private static ChatColor getStateColor(MatchState state) {
        switch (state) {
            case WAITINGSTART:
            case WAITINGLOBBY:
                return ChatColor.GREEN;
            case PLAYING:
                return ChatColor.YELLOW;
            case ENDING:
                return ChatColor.RED;
            default:
                return ChatColor.GRAY;
        }
    }

    private static Material getStateIcon(MatchState state) {
        switch (state) {
            case WAITINGSTART:
            case WAITINGLOBBY:
                return Material.EMERALD_BLOCK;
            case PLAYING:
                return Material.REDSTONE_BLOCK;
            case ENDING:
                return Material.COAL_BLOCK;
            default:
                return Material.BARRIER;
        }
    }
}
