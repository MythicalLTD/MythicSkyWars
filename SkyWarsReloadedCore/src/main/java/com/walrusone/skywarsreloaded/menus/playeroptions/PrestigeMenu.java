package com.walrusone.skywarsreloaded.menus.playeroptions;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.database.DataStorage;
import com.walrusone.skywarsreloaded.managers.PlayerStat;
import com.walrusone.skywarsreloaded.utilities.LevelManager;
import com.walrusone.skywarsreloaded.utilities.Messaging;
import com.walrusone.skywarsreloaded.utilities.PrestigeManager;
import com.walrusone.skywarsreloaded.utilities.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI for selecting a prestige cosmetic from {@code levels.yml}.
 */
public final class PrestigeMenu {

    private static final Map<java.util.UUID, Integer> CURRENT_PAGE = new HashMap<>();

    private PrestigeMenu() {
    }

    public static void open(Player player) {
        PrestigeManager pm = PrestigeManager.get();
        if (!pm.isEnabled()) {
            player.sendMessage(new Messaging.MessageFormatter().format("levels.prestige-disabled"));
            return;
        }
        PlayerStat ps = PlayerStat.getPlayerStats(player);
        if (ps == null || !ps.isInitialized()) {
            player.sendMessage(new Messaging.MessageFormatter().format("stats.not-ready"));
            return;
        }
        CURRENT_PAGE.put(player.getUniqueId(), 1);
        showPage(player, ps, pm, 1);
    }

    private static void showPage(Player player, PlayerStat ps, PrestigeManager pm, int page) {
        int size = pm.getMenuSlots(SkyWarsReloaded.get());
        int maxPage = maxPage(pm);
        page = Math.min(Math.max(1, page), maxPage);
        CURRENT_PAGE.put(player.getUniqueId(), page);

        String title = new Messaging.MessageFormatter().format("menu.prestige-title");
        Inventory inv = Bukkit.createInventory(null, size, title);

        PrestigeManager.PrestigeEntry selected =
                pm.getEntry(ps.getPrestigeIcon() != null ? ps.getPrestigeIcon() : "icon1");
        if (selected == null) {
            selected = pm.defaultEntryOrNull();
        }

        int levelNum = LevelManager.get().getLevelForXp(ps.getXp());

        for (PrestigeManager.PrestigeEntry def : pm.getAllEntries()) {
            if (def.page != page) {
                continue;
            }
            if (def.slot < 0 || def.slot >= size) {
                continue;
            }
            List<String> lore = pm.loreLines(player, ps, def, selected, levelNum);
            String itemName = ChatColor.translateAlternateColorCodes('&',
                    def.displayName
                            .replace("<level>", Integer.toString(levelNum))
                            .replace("{level}", Integer.toString(levelNum)));
            ItemStack stack = SkyWarsReloaded.getNMS().getItemStack(
                    new ItemStack(def.material, 1),
                    lore,
                    itemName);
            inv.setItem(def.slot, stack);
        }

        int closeSlot = size - 5;
        int prevSlot = size - 6;
        int nextSlot = size - 4;

        inv.setItem(closeSlot, navButton("items.prestige-close-name", "items.prestige-close-lore",
                "items.prestige-nav-close-material", Material.BARRIER, page, maxPage));

        if (maxPage > 1 && page > 1) {
            inv.setItem(prevSlot, navButton("items.prestige-prev-page-name", "items.prestige-prev-page-lore",
                    "items.prestige-nav-prev-material", Material.ARROW, page, maxPage));
        }
        if (maxPage > 1 && page < maxPage) {
            inv.setItem(nextSlot, navButton("items.prestige-next-page-name", "items.prestige-next-page-lore",
                    "items.prestige-nav-next-material", Material.ARROW, page, maxPage));
        }

        ArrayList<Inventory> invs = new ArrayList<>();
        invs.add(inv);
        SkyWarsReloaded.getIC().create(player, invs, event -> {
            PlayerStat pst = PlayerStat.getPlayerStats(player);
            PrestigeManager pman = PrestigeManager.get();
            if (pst == null || !pman.isEnabled()) {
                return;
            }

            int slot = event.getSlot();
            int p = CURRENT_PAGE.getOrDefault(player.getUniqueId(), 1);
            int mx = maxPage(pman);

            ItemStack cur = inv.getItem(slot);
            if (cur != null && slot == closeSlot) {
                Material want = readMaterialKey("items.prestige-nav-close-material", Material.BARRIER);
                if (cur.getType() == want) {
                    player.closeInventory();
                }
                return;
            }

            if (cur != null && slot == prevSlot && p > 1) {
                Material want = readMaterialKey("items.prestige-nav-prev-material", Material.ARROW);
                if (cur.getType() == want) {
                    showPage(player, pst, pman, p - 1);
                }
                return;
            }
            if (cur != null && slot == nextSlot && p < mx) {
                Material want = readMaterialKey("items.prestige-nav-next-material", Material.ARROW);
                if (cur.getType() == want) {
                    showPage(player, pst, pman, p + 1);
                }
                return;
            }

            PrestigeManager.PrestigeEntry hit = findBySlot(pman, slot, p);
            if (hit == null) {
                return;
            }
            if (!pman.meetsRequirements(player, pst, hit)) {
                player.sendMessage(new Messaging.MessageFormatter().format("levels.prestige-locked"));
                Util.get().playSound(player, player.getLocation(), SkyWarsReloaded.getCfg().getErrorSound(), 1, 1);
                return;
            }
            pst.setPrestigeIcon(hit.id);
            DataStorage.get().saveStats(pst);
            player.sendMessage(new Messaging.MessageFormatter()
                    .setVariable("prestige", ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', hit.displayName)))
                    .format("levels.prestige-updated"));
            Util.get().playSound(player, player.getLocation(),
                    SkyWarsReloaded.getCfg().getConfirmeSelctionSound(), 1, 1);
            showPage(player, pst, pman, p);
        }, false);

        SkyWarsReloaded.getIC().show(player, null);
    }

    private static PrestigeManager.PrestigeEntry findBySlot(PrestigeManager pm, int slot, int page) {
        for (PrestigeManager.PrestigeEntry def : pm.getAllEntries()) {
            if (def.page == page && def.slot == slot) {
                return def;
            }
        }
        return null;
    }

    private static int maxPage(PrestigeManager pm) {
        int mx = 1;
        for (PrestigeManager.PrestigeEntry def : pm.getAllEntries()) {
            mx = Math.max(mx, def.page);
        }
        return mx;
    }

    private static ItemStack navButton(String nameKey, String loreKey, String matKey, Material fallback, int page, int maxPage) {
        List<String> loreLines = SkyWarsReloaded.getMessaging().getFile().getStringList(loreKey);
        List<String> loreOut = new ArrayList<>();
        if (loreLines == null || loreLines.isEmpty()) {
            loreOut.add(ChatColor.GRAY + "{page} / {max_page}");
        } else {
            for (String line : loreLines) {
                loreOut.add(ChatColor.translateAlternateColorCodes('&', line
                        .replace("{page}", Integer.toString(page))
                        .replace("{max_page}", Integer.toString(maxPage))));
            }
        }
        String nameRaw = SkyWarsReloaded.getMessaging().getMessage(nameKey);
        if (nameRaw == null) {
            nameRaw = "&8Nav";
        }
        String name = ChatColor.translateAlternateColorCodes('&', nameRaw
                .replace("{page}", Integer.toString(page))
                .replace("{max_page}", Integer.toString(maxPage)));
        Material mat = readMaterialKey(matKey, fallback);
        return SkyWarsReloaded.getNMS().getItemStack(new ItemStack(mat, 1), loreOut, name);
    }

    private static Material readMaterialKey(String key, Material fallback) {
        String name = SkyWarsReloaded.getMessaging().getMessage(key);
        if (name == null || name.trim().isEmpty()) {
            return fallback;
        }
        Material m = Material.matchMaterial(name.trim());
        if (m != null) {
            return m;
        }
        try {
            return Material.valueOf(name.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (Exception ex) {
            return fallback;
        }
    }
}
