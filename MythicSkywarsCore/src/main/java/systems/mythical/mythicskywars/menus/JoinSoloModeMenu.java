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

import java.util.ArrayList;
import java.util.List;

public class JoinSoloModeMenu {

    private static final String MENU_ID = "joinsolomodemenu";
    private static final int SLOT_NORMAL = 11;
    private static final int SLOT_LUCKY = 15;

    public JoinSoloModeMenu() {
        String title = new Messaging.MessageFormatter().format("menu.joinsolo-mode-menu-title");
        Inventory menu = Bukkit.createInventory(null, 36, title);
        ArrayList<Inventory> invs = new ArrayList<>();
        invs.add(menu);

        List<String> normalLore = new ArrayList<>();
        normalLore.add(new Messaging.MessageFormatter().format("menu.joinsolo-mode-normal-lore1"));
        normalLore.add(new Messaging.MessageFormatter().format("menu.joinsolo-mode-normal-lore2"));
        ItemStack normal = MythicSkywars.getNMS().getItemStack(
                MythicSkywars.getIM().getItem("singlemenu"),
                normalLore,
                new Messaging.MessageFormatter().format("items.joinsolo-normal"));

        List<String> luckyLore = new ArrayList<>();
        luckyLore.add(new Messaging.MessageFormatter().format("menu.joinsolo-mode-lucky-lore1"));
        luckyLore.add(new Messaging.MessageFormatter().format("menu.joinsolo-mode-lucky-lore2"));
        luckyLore.add(new Messaging.MessageFormatter().format("menu.joinsolo-mode-lucky-lore3"));
        ItemStack lucky = MythicSkywars.getNMS().getItemStack(
                new ItemStack(Material.GOLD_BLOCK, 1),
                luckyLore,
                new Messaging.MessageFormatter().format("items.joinsolo-lucky"));
        boolean luckyAvailable = LuckyBlockHook.isAvailable();
        if (!luckyAvailable) {
            lucky = new ItemStack(Material.AIR, 1);
        }

        menu.setItem(SLOT_NORMAL, normal);
        menu.setItem(SLOT_LUCKY, lucky);

        MythicSkywars.getIC().create(MENU_ID, invs, event -> {
            Player player = event.getPlayer();
            if (event.getSlot() == SLOT_NORMAL) {
                JoinSingleMenu.showFor(player, false);
                return;
            }
            if (event.getSlot() == SLOT_LUCKY) {
                if (!LuckyBlockHook.isAvailable()) {
                    Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getErrorSound(), 1, 1);
                    player.sendMessage(new Messaging.MessageFormatter().format("error.could-not-join2"));
                    return;
                }
                JoinSingleMenu.showFor(player, true);
                return;
            }
            String name = event.getName();
            if (name.equalsIgnoreCase(MythicSkywars.getNMS().getItemName(MythicSkywars.getIM().getItem("exitMenuItem")))) {
                player.closeInventory();
                return;
            }
            Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getErrorSound(), 1, 1);
        });
    }
}
