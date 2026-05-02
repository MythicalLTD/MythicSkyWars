package systems.mythical.mythicskywars.menus;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.enums.MatchState;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.managers.MatchManager;
import systems.mythical.mythicskywars.utilities.Messaging;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;

public class TeamSpectateMenu {

    public TeamSpectateMenu(GameMap gMap) {
        String menuName = new Messaging.MessageFormatter().setVariable("mapname", gMap.getDisplayName()).format("menu.teamspectate-menu-title");
        int menuSize = 27;
        Inventory menu = Bukkit.createInventory(null, menuSize + 9, menuName);
        ArrayList<Inventory> invs = new ArrayList<>();
        invs.add(menu);

        MythicSkywars.getIC().create(gMap.getName() + "teamspectate", invs, event -> {
            Player player = event.getPlayer();

            String name = event.getName();
            if (name.equalsIgnoreCase(MythicSkywars.getNMS().getItemName(MythicSkywars.getIM().getItem("exitMenuItem")))) {
                if (!MythicSkywars.getIC().hasViewers("spectateteammenu")) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            MythicSkywars.getIC().getMenu("jointeammenu").update();
                        }
                    }.runTaskLater(MythicSkywars.get(), 5);
                }
                MythicSkywars.getIC().show(player, "spectateteammenu");
                return;
            }

            if (player.hasPermission("sw.spectate")) {
                player.closeInventory();
                if (gMap.getMatchState() != MatchState.OFFLINE && gMap.getMatchState() != MatchState.ENDING) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            MythicSkywars.get().getPlayerManager().addSpectator(gMap, player);
                        }
                    }.runTaskLater(MythicSkywars.get(), 5);
                }
            }
        });
    }

}