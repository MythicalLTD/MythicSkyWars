package systems.mythical.mythicskywars.listeners;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.enums.MatchState;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.managers.MatchManager;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

public class SwapHandListener implements org.bukkit.event.Listener {
    public SwapHandListener() {
    }

    @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void playerSwapHand(PlayerSwapHandItemsEvent event) {
        GameMap gMap = MatchManager.get().getPlayerMap(event.getPlayer());
        if (gMap == null) {
            ItemStack item = event.getOffHandItem();
            if (item != null && ((item.equals(MythicSkywars.getIM().getItem("optionselect"))) ||
                    (item.equals(MythicSkywars.getIM().getItem("joinselect"))) ||
                    (item.equals(MythicSkywars.getIM().getItem("statsitem"))) ||
                    (item.equals(MythicSkywars.getIM().getItem("backlobbyitem"))) ||
                    (item.equals(MythicSkywars.getIM().getItem("spectateselect"))))) {
                event.setCancelled(true);
            }
        } else if ((gMap.getMatchState().equals(MatchState.WAITINGSTART)) || (gMap.getMatchState().equals(MatchState.WAITINGLOBBY)) || (gMap.getMatchState().equals(MatchState.ENDING))) {
            event.setCancelled(true);
        }
    }
}
