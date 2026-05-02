package systems.mythical.mythicskywars.menus;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.utilities.LuckyBlockHook;
import systems.mythical.mythicskywars.utilities.Messaging;
import systems.mythical.mythicskywars.utilities.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
    private static final int SLOT_DUOS = 10;
    private static final int SLOT_SQUADS = 13;
    private static final int SLOT_LUCKY = 16;

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

        // Squads button
        List<String> squadsLore = new ArrayList<>();
        squadsLore.add(new Messaging.MessageFormatter().format("menu.jointeam-mode-squads-lore1"));
        squadsLore.add(new Messaging.MessageFormatter().format("menu.jointeam-mode-squads-lore2"));
        Material squadsIcon;
        try {
            squadsIcon = Material.valueOf("SHIELD");
        } catch (IllegalArgumentException e) {
            squadsIcon = Material.IRON_CHESTPLATE;
        }
        ItemStack squads = MythicSkywars.getNMS().getItemStack(
                new ItemStack(squadsIcon, 1),
                squadsLore,
                new Messaging.MessageFormatter().format("items.jointeam-squads"));
        menu.setItem(SLOT_SQUADS, squads);

        // Lucky Teams button
        List<String> luckyLore = new ArrayList<>();
        luckyLore.add(new Messaging.MessageFormatter().format("menu.jointeam-mode-lucky-lore1"));
        luckyLore.add(new Messaging.MessageFormatter().format("menu.jointeam-mode-lucky-lore2"));
        boolean luckyAvailable = LuckyBlockHook.isAvailable();
        ItemStack lucky;
        if (luckyAvailable) {
            lucky = MythicSkywars.getNMS().getItemStack(
                    new ItemStack(Material.GOLD_BLOCK, 1),
                    luckyLore,
                    new Messaging.MessageFormatter().format("items.jointeam-lucky"));
        } else {
            lucky = new ItemStack(Material.AIR, 1);
        }
        menu.setItem(SLOT_LUCKY, lucky);

        MythicSkywars.getIC().create(MENU_ID, invs, event -> {
            Player player = event.getPlayer();
            int slot = event.getSlot();

            if (slot == SLOT_DUOS) {
                teamSizeFilter.put(player.getUniqueId(), 2);
                luckyTeamSelection.remove(player.getUniqueId());
                openTeamMenu(player);
                return;
            }
            if (slot == SLOT_SQUADS) {
                // Squads = team size 3 or 4 (show all non-duo team maps)
                teamSizeFilter.put(player.getUniqueId(), 4);
                luckyTeamSelection.remove(player.getUniqueId());
                openTeamMenu(player);
                return;
            }
            if (slot == SLOT_LUCKY) {
                if (!LuckyBlockHook.isAvailable()) {
                    Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getErrorSound(), 1, 1);
                    player.sendMessage(new Messaging.MessageFormatter().format("error.could-not-join2"));
                    return;
                }
                teamSizeFilter.put(player.getUniqueId(), 0); // All team sizes for lucky
                luckyTeamSelection.add(player.getUniqueId());
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
