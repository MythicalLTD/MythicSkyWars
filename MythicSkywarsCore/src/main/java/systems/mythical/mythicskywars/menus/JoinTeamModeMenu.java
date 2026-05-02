package systems.mythical.mythicskywars.menus;

import com.google.common.collect.Lists;
import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.enums.GameType;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.utilities.LuckyBlockHook;
import systems.mythical.mythicskywars.utilities.Messaging;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Auto-detects available team sizes from registered maps and builds a mode selection menu.
 * If only one team size exists, skips the menu and goes straight to the map list.
 */
public class JoinTeamModeMenu {

    private static final String MENU_ID = "jointeammodemenu";

    /** Currently selected team filter per player (0 = all). */
    private static final Map<UUID, Integer> teamSizeFilter = new ConcurrentHashMap<>();
    private static final Set<UUID> luckyTeamSelection = ConcurrentHashMap.newKeySet();
    private static final int LUCKY_SLOT = 22;

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
        Inventory menu = Bukkit.createInventory(null, 27, title);
        ArrayList<Inventory> invs = new ArrayList<>();
        invs.add(menu);

        MythicSkywars.getIC().create(MENU_ID, invs, event -> {
            Player player = event.getPlayer();
            int slot = event.getSlot();
            String clickedName = event.getName();

            if (clickedName != null && clickedName.equalsIgnoreCase(
                    MythicSkywars.getNMS().getItemName(MythicSkywars.getIM().getItem("exitMenuItem")))) {
                player.closeInventory();
                return;
            }

            // Check if the clicked slot corresponds to a team size button
            // Slots 10-16 are used for team size buttons
            if (slot >= 10 && slot <= 16) {
                // Extract team size from the item amount (we set it as the team size)
                ItemStack clicked = event.getItem();
                if (clicked != null && clicked.getType() != Material.AIR) {
                    int teamSize = clicked.getAmount();
                    if (teamSize >= 2) {
                        teamSizeFilter.put(player.getUniqueId(), teamSize);
                        luckyTeamSelection.remove(player.getUniqueId());
                        openTeamMenu(player);
                    }
                }
            }

            // Lucky Teams button
            if (slot == LUCKY_SLOT) {
                if (!LuckyBlockHook.isAvailable()) {
                    return;
                }
                teamSizeFilter.put(player.getUniqueId(), 0);
                luckyTeamSelection.add(player.getUniqueId());
                openTeamMenu(player);
            }
        });

        // Set up periodic refresh to update the menu with current team sizes
        MythicSkywars.getIC().getMenu(MENU_ID).setUpdate(() -> {
            if (!MythicSkywars.getIC().hasViewers(MENU_ID)) return;
            refreshMenu();
        });
    }

    private static void refreshMenu() {
        ArrayList<Inventory> invs = MythicSkywars.getIC().getMenu(MENU_ID).getInventories();
        if (invs.isEmpty()) return;
        Inventory inv = invs.get(0);

        // Clear content slots
        for (int i = 0; i < 18; i++) {
            inv.setItem(i, new ItemStack(Material.AIR));
        }

        // Detect available team sizes
        TreeSet<Integer> teamSizes = getAvailableTeamSizes();

        if (teamSizes.isEmpty()) {
            // No team maps available
            List<String> lore = Lists.newArrayList(ChatColor.GRAY + "No team maps are registered.");
            inv.setItem(13, MythicSkywars.getNMS().getItemStack(
                    new ItemStack(Material.BARRIER, 1), lore,
                    ChatColor.RED + "No Team Maps"));
            return;
        }

        // Place buttons for each team size, centered in the menu
        List<Integer> sizes = new ArrayList<>(teamSizes);
        int[] slots = getCenteredSlots(sizes.size());

        for (int i = 0; i < sizes.size() && i < slots.length; i++) {
            int teamSize = sizes.get(i);
            int slot = slots[i];

            String modeName = getTeamSizeDisplayName(teamSize);
            Material icon = getTeamSizeIcon(teamSize);

            // Count available maps for this team size
            int mapCount = 0;
            for (GameMap gMap : MythicSkywars.getGameMapMgr().getPlayableArenas(GameType.TEAM)) {
                if (gMap.getTeamSize() == teamSize) {
                    mapCount++;
                }
            }

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Teams of " + teamSize + " players");
            lore.add(ChatColor.GRAY + "" + mapCount + " map" + (mapCount != 1 ? "s" : "") + " available");
            lore.add("");
            lore.add(ChatColor.GREEN + "Click to browse!");

            // Use team size as item amount so we can read it back on click
            ItemStack item = new ItemStack(icon, teamSize);
            inv.setItem(slot, MythicSkywars.getNMS().getItemStack(item, lore,
                    ChatColor.translateAlternateColorCodes('&', modeName)));
        }

        // Lucky Teams button (bottom row, slot 22)
        if (LuckyBlockHook.isAvailable()) {
            List<String> luckyLore = new ArrayList<>();
            luckyLore.add(ChatColor.GRAY + "Lucky Block team mode");
            luckyLore.add(ChatColor.YELLOW + "Chests replaced by LuckyBlocks");
            luckyLore.add("");
            luckyLore.add(ChatColor.GREEN + "Click to browse!");
            inv.setItem(LUCKY_SLOT, MythicSkywars.getNMS().getItemStack(
                    new ItemStack(Material.GOLD_BLOCK, 1), luckyLore,
                    ChatColor.GOLD + "Lucky Teams"));
        }
    }

    /**
     * Shows the team mode menu, or skips it if only one team size is available AND no lucky mode.
     */
    public static void showFor(Player player) {
        TreeSet<Integer> teamSizes = getAvailableTeamSizes();
        boolean luckyAvailable = LuckyBlockHook.isAvailable();

        if (teamSizes.size() <= 1 && !luckyAvailable) {
            // Only one team size and no lucky — skip the mode menu, go straight to map list
            if (!teamSizes.isEmpty()) {
                teamSizeFilter.put(player.getUniqueId(), teamSizes.first());
            } else {
                teamSizeFilter.put(player.getUniqueId(), 0);
            }
            luckyTeamSelection.remove(player.getUniqueId());
            openTeamMenuStatic(player);
            return;
        }

        // Multiple team sizes or lucky available — show the selection menu
        refreshMenu();
        MythicSkywars.getIC().show(player, MENU_ID);
    }

    private void openTeamMenu(Player player) {
        openTeamMenuStatic(player);
    }

    private static void openTeamMenuStatic(Player player) {
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

    private static TreeSet<Integer> getAvailableTeamSizes() {
        TreeSet<Integer> sizes = new TreeSet<>();
        for (GameMap gMap : MythicSkywars.getGameMapMgr().getPlayableArenas(GameType.TEAM)) {
            if (gMap.getTeamSize() > 1) {
                sizes.add(gMap.getTeamSize());
            }
        }
        return sizes;
    }

    private static int[] getCenteredSlots(int count) {
        // Center items in the middle row (slots 10-16)
        switch (count) {
            case 1: return new int[]{13};
            case 2: return new int[]{11, 15};
            case 3: return new int[]{11, 13, 15};
            case 4: return new int[]{10, 12, 14, 16};
            case 5: return new int[]{10, 11, 13, 15, 16};
            case 6: return new int[]{10, 11, 12, 14, 15, 16};
            default: return new int[]{10, 11, 12, 13, 14, 15, 16};
        }
    }

    private static String getTeamSizeDisplayName(int teamSize) {
        switch (teamSize) {
            case 2: return "&aDuos";
            case 3: return "&bTrios";
            case 4: return "&dSquads";
            default: return "&eTeams of " + teamSize;
        }
    }

    private static Material getTeamSizeIcon(int teamSize) {
        switch (teamSize) {
            case 2: return Material.IRON_SWORD;
            case 3: return Material.IRON_CHESTPLATE;
            case 4: return Material.DIAMOND_SWORD;
            default: return Material.NETHER_STAR;
        }
    }
}
