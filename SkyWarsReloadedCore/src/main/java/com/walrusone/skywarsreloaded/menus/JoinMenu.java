package com.walrusone.skywarsreloaded.menus;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.enums.MatchState;
import com.walrusone.skywarsreloaded.game.GameMap;
import com.walrusone.skywarsreloaded.managers.MatchManager;
import com.walrusone.skywarsreloaded.utilities.LuckyBlockHook;
import com.walrusone.skywarsreloaded.utilities.Messaging;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

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
        ItemStack single = SkyWarsReloaded.getNMS().getItemStack(SkyWarsReloaded.getIM().getItem("singlemenu"), lores,
                new Messaging.MessageFormatter().format("items.joinsingle"));

        lores.clear();
        lores.add(new Messaging.MessageFormatter().format("menu.joinloreteam1"));
        lores.add(new Messaging.MessageFormatter().format("menu.joinloreteam2"));
        ItemStack team = SkyWarsReloaded.getNMS().getItemStack(SkyWarsReloaded.getIM().getItem("teammenu"), lores,
                new Messaging.MessageFormatter().format("items.jointeam"));

        lores.clear();
        lores.add(new Messaging.MessageFormatter().format("items.click-to-rejoin"));
        ItemStack playAgain = SkyWarsReloaded.getNMS().getItemStack(SkyWarsReloaded.getIM().getItem("playAgainItem"), lores,
                new Messaging.MessageFormatter().format("items.rejoin-game-item"));

        invs.get(0).setItem(SkyWarsReloaded.getCfg().getSingleSlot(), single);
        invs.get(0).setItem(SkyWarsReloaded.getCfg().getTeamSlot(), team);
        if (SkyWarsReloaded.getCfg().isPlayAgainItemEnabled()) {
            invs.get(0).setItem(REJOIN_MENU_SLOT, playAgain);
        }

        SkyWarsReloaded.getIC().create("joinmenu", invs, event -> {
            Player player = event.getPlayer();
            GameMap gMap = MatchManager.get().getPlayerMap(player);
            if (gMap != null && gMap.getMatchState() != MatchState.ENDING) {
                return;
            }
            String name = event.getName();
            if (name.equalsIgnoreCase(SkyWarsReloaded.getNMS().getItemName(SkyWarsReloaded.getIM().getItem("exitMenuItem")))) {
                player.closeInventory();
                return;
            }
            if (event.getSlot() == SkyWarsReloaded.getCfg().getSingleSlot()) {
                if (LuckyBlockHook.isAvailable()) {
                    SkyWarsReloaded.getIC().show(player, "joinsolomodemenu");
                } else {
                    JoinSingleMenu.showFor(player, false);
                }
                return;
            }

            if (event.getSlot() == SkyWarsReloaded.getCfg().getTeamSlot()) {
                if (!SkyWarsReloaded.getIC().hasViewers("jointeammenu")) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            SkyWarsReloaded.getIC().getMenu("jointeammenu").update();
                        }
                    }.runTaskLater(SkyWarsReloaded.get(), 5);
                }
                SkyWarsReloaded.getIC().show(player, "jointeammenu");
                return;
            }

            if (SkyWarsReloaded.getCfg().isPlayAgainItemEnabled()
                    && event.getSlot() == REJOIN_MENU_SLOT) {
                player.closeInventory();
                if (!MatchManager.get().tryRejoin(player)) {
                    player.sendMessage(new Messaging.MessageFormatter().format("error.rejoin-not-available"));
                }
            }
        });
    }
}