package systems.mythical.mythicskywars.menus;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.utilities.Messaging;
import systems.mythical.mythicskywars.utilities.Util;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Mode selection menu for team games: Duos / Squads / Lucky Teams.
 * Filters the JoinTeamMenu by team size when a mode is selected.
 */
public class JoinTeamModeMenu {

    private static final String MENU_ID = "jointeammodemenu";
    private static final int SLOT_DUOS = 13;

    /** Currently selected team filter per player (0 = all). */
    private static final java.util.Map<java.util.UUID, Integer> teamSizeFilter = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Set<java.util.UUID> luckyTeamSelection = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public static int getTeamSizeFilter(Player player) {
        return teamSizeFilter.getOrDefault(player.getUniqueId(), 0);
    }

    public static boolean wantsLuckyTeam(Player player) {
        return luckyTeamSelection.contains(player.getUniqueId());
    }

    public static void clearFilter(Player player) {
        teamSizeFilter.remove(player.getUniqueId());
        luckyTeamSelection.remove(player.getUniqueId());
    }

    public JoinTeamModeMenu() {
        String title = new Messaging.MessageFormatter().format("menu.jointeam-mode-menu-title");
        Inventory menu = Bukkit.createInventory(null, 36, title);
        ArrayList<Inventory> invs = new ArrayList<>();
        invs.add(menu);

        // Duos button
        List<String> duosLore = new ArrayList<>();
        duosLore.add(new Messaging.MessageFormatter().format("menu.jointeam-mode-duos-lore1"));
        duosLore.add(new Messaging.MessageFormatter().format("menu.jointeam-mode-duos-lore2"));
        ItemStack duos = MythicSkywars.getNMS().getItemStack(
                MythicSkywars.getIM().getItem("teammenu"),
                duosLore,
                new Messaging.MessageFormatter().format("items.jointeam-duos"));
        menu.setItem(SLOT_DUOS, duos);

        MythicSkywars.getIC().create(MENU_ID, invs, event -> {
            Player player = event.getPlayer();
            int slot = event.getSlot();

            if (slot == SLOT_DUOS) {
                teamSizeFilter.put(player.getUniqueId(), 2);
                luckyTeamSelection.remove(player.getUniqueId());
                openTeamMenu(player);
                return;
            }

            String name = event.getName();
            if (name.equalsIgnoreCase(MythicSkywars.getNMS().getItemName(MythicSkywars.getIM().getItem("exitMenuItem")))) {
                player.closeInventory();
            }
        });
    }

    public static void showFor(Player player) {
        MythicSkywars.getIC().show(player, MENU_ID);
    }

    private void openTeamMenu(Player player) {
        if (!MythicSkywars.getIC().hasViewers("jointeammenu")) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    MythicSkywars.getIC().getMenu("jointeammenu").update();
                }
            }.runTaskLater(MythicSkywars.get(), 5);
        }
        MythicSkywars.getIC().show(player, "jointeammenu");
    }
}
