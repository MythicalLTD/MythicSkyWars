package systems.mythical.mythicskywars.menus;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.enums.MatchState;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.managers.GameMapManager;
import systems.mythical.mythicskywars.managers.MatchManager;
import systems.mythical.mythicskywars.utilities.Messaging;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;

public class SpectateSingleMenu {

    private static final String menuName = new Messaging.MessageFormatter().format("menu.spectatesinglegame-menu-title");

    public SpectateSingleMenu() {

        int menuSize = 45;
        Inventory menu = Bukkit.createInventory(null, menuSize + 9, menuName);
        ArrayList<Inventory> invs = new ArrayList<>();
        invs.add(menu);

        MythicSkywars.getIC().create("spectatesinglemenu", invs, event -> {
            Player player = event.getPlayer();
            GameMap gMap = MatchManager.get().getPlayerMap(player);
            if (gMap != null) {
                return;
            }

            String name = event.getName();
            if (name.equalsIgnoreCase(MythicSkywars.getNMS().getItemName(MythicSkywars.getIM().getItem("exitMenuItem")))) {
                player.closeInventory();
                return;
            }

            // Search button
            if (org.bukkit.ChatColor.stripColor(name).equalsIgnoreCase("Search")) {
                player.closeInventory();
                player.sendMessage(new systems.mythical.mythicskywars.utilities.Messaging.MessageFormatter().format("items.search-prompt"));
                MapSearchListener.startSearch(player, "solo");
                return;
            }

            if (!JoinSingleMenu.arenaSlots.containsKey(event.getSlot())) {
                return;
            }

            gMap = MythicSkywars.getGameMapMgr().getMap(JoinSingleMenu.arenaSlots.get(event.getSlot()));
            if (gMap == null) {
                return;
            }

            if (player.hasPermission("sw.spectate")) {
                player.closeInventory();
                if (gMap.getMatchState() != MatchState.OFFLINE && gMap.getMatchState() != MatchState.ENDING) {
                    GameMap finalGMap = gMap;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            MythicSkywars.get().getPlayerManager().addSpectator(finalGMap, player);
                        }
                    }.runTaskLater(MythicSkywars.get(), 5);
                }
            }
        });
    }

}