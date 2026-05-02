package systems.mythical.mythicskywars.menus;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.managers.MatchManager;
import systems.mythical.mythicskywars.utilities.Messaging;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class SpectateMenu {

    private static final String menuName = new Messaging.MessageFormatter().format("menu.spectategame-menu-title");

    public SpectateMenu() {
        int menuSize = 27;
        Inventory menu = Bukkit.createInventory(null, menuSize + 9, menuName);
        ArrayList<Inventory> invs = new ArrayList<>();
        invs.add(menu);

        List<String> lores = new ArrayList<>();
        lores.add(new Messaging.MessageFormatter().format("menu.joinloresingle1"));
        lores.add(new Messaging.MessageFormatter().format("menu.joinloresingle2"));
        ItemStack single = MythicSkywars.getNMS().getItemStack(MythicSkywars.getIM().getItem("singlemenu"), lores,
                new Messaging.MessageFormatter().format("items.joinsingle"));

        lores.clear();
        lores.add(new Messaging.MessageFormatter().format("menu.joinloreteam1"));
        lores.add(new Messaging.MessageFormatter().format("menu.joinloreteam2"));
        ItemStack team = MythicSkywars.getNMS().getItemStack(MythicSkywars.getIM().getItem("teammenu"), lores,
                new Messaging.MessageFormatter().format("items.jointeam"));

        invs.get(0).setItem(MythicSkywars.getCfg().getSingleSlot(), single);
        invs.get(0).setItem(MythicSkywars.getCfg().getTeamSlot(), team);

        MythicSkywars.getIC().create("spectatemenu", invs, event -> {
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
            if (event.getSlot() == MythicSkywars.getCfg().getSingleSlot()) {
                if (!MythicSkywars.getIC().hasViewers("joinsinglemenu")) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            MythicSkywars.getIC().getMenu("joinsinglemenu").update();
                        }
                    }.runTaskLater(MythicSkywars.get(), 5);
                }
                MythicSkywars.getIC().show(player, "spectatesinglemenu");
                return;
            }

            if (event.getSlot() == MythicSkywars.getCfg().getTeamSlot()) {
                if (!MythicSkywars.getIC().hasViewers("jointeammenu")) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            MythicSkywars.getIC().getMenu("jointeammenu").update();
                        }
                    }.runTaskLater(MythicSkywars.get(), 5);
                }
                MythicSkywars.getIC().show(player, "spectateteammenu");
            }
        });
    }
}