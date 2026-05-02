package systems.mythical.mythicskywars.menus;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.enums.MatchState;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.managers.MatchManager;
import systems.mythical.mythicskywars.utilities.LuckyBlockHook;
import systems.mythical.mythicskywars.utilities.Messaging;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class JoinMenu {

    private static final String menuName = new Messaging.MessageFormatter().format("menu.joingame-menu-title");
    // Bottom-center in the 3x9 content area.
    private static final int REJOIN_MENU_SLOT = 22;

    public JoinMenu() {
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

        lores.clear();
        lores.add(new Messaging.MessageFormatter().format("items.click-to-rejoin"));
        ItemStack playAgain = MythicSkywars.getNMS().getItemStack(MythicSkywars.getIM().getItem("playAgainItem"), lores,
                new Messaging.MessageFormatter().format("items.rejoin-game-item"));

        invs.get(0).setItem(MythicSkywars.getCfg().getSingleSlot(), single);
        invs.get(0).setItem(MythicSkywars.getCfg().getTeamSlot(), team);
        if (MythicSkywars.getCfg().isPlayAgainItemEnabled()) {
            invs.get(0).setItem(REJOIN_MENU_SLOT, playAgain);
        }

        MythicSkywars.getIC().create("joinmenu", invs, event -> {
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
            if (event.getSlot() == MythicSkywars.getCfg().getSingleSlot()) {
                if (LuckyBlockHook.isAvailable()) {
                    MythicSkywars.getIC().show(player, "joinsolomodemenu");
                } else {
                    JoinSingleMenu.showFor(player, false);
                }
                return;
            }

            if (event.getSlot() == MythicSkywars.getCfg().getTeamSlot()) {
                JoinTeamModeMenu.showFor(player);
                return;
            }

            if (MythicSkywars.getCfg().isPlayAgainItemEnabled()
                    && event.getSlot() == REJOIN_MENU_SLOT) {
                player.closeInventory();
                if (!MatchManager.get().tryRejoin(player)) {
                    player.sendMessage(new Messaging.MessageFormatter().format("error.rejoin-not-available"));
                }
            }
        });
    }
}