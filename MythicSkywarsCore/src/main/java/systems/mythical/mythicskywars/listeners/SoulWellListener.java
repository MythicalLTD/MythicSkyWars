package systems.mythical.mythicskywars.listeners;

import systems.mythical.mythicskywars.menus.soulwell.SoulWellService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

/**
 * Blocks interaction during Soul Well spin animation (non-IconMenu inventory).
 */
public class SoulWellListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (SoulWellService.isSpinning(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();
        if (SoulWellService.titlesMatch(title, SoulWellService.spinningTitleRaw())) {
            SoulWellService.onInventoryClose(player);
        }
    }
}
