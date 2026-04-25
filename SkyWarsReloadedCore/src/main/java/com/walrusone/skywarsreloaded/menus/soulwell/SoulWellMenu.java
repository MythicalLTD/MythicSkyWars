package com.walrusone.skywarsreloaded.menus.soulwell;

import com.google.common.collect.Lists;
import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.managers.PlayerStat;
import com.walrusone.skywarsreloaded.utilities.Messaging;
import com.walrusone.skywarsreloaded.utilities.SoulWellManager;
import com.walrusone.skywarsreloaded.utilities.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class SoulWellMenu {

    private SoulWellMenu() {
    }

    public static void open(Player player) {
        open(player, false);
    }

    public static void openForced(Player player) {
        open(player, true);
    }

    private static void open(Player player, boolean forced) {
        if (!forced && !SkyWarsReloaded.getCfg().isSoulWellEnabled()) {
            return;
        }
        SoulWellManager manager = SkyWarsReloaded.getSoulWellManager();
        if (!forced && (manager == null || !manager.canUseSoulWell(player))) {
            player.sendMessage(new Messaging.MessageFormatter().format("soulwell.not-in-lobby"));
            return;
        }
        PlayerStat ps = PlayerStat.getPlayerStats(player);
        int souls = ps != null ? ps.getSouls() : 0;
        int per = SkyWarsReloaded.getCfg().getSoulWellSoulsPerSpin();
        int max = SkyWarsReloaded.getCfg().getSoulWellMaxSpinsAtOnce();

        int menuSize = 27;
        String menuName = new Messaging.MessageFormatter().format("menu.soul-well-title");
        Inventory inv = Bukkit.createInventory(null, menuSize + 9, menuName);

        ItemStack info = skullInfo(player, souls);
        inv.setItem(4, info);

        for (int i = 0; i < max && i < 5; i++) {
            int rolls = i + 1;
            int slot = 10 + i * 2;
            if (slot > 16) {
                break;
            }
            List<String> lore = Lists.newArrayList();
            lore.add(new Messaging.MessageFormatter()
                    .setVariable("souls", "" + (per * rolls))
                    .format("soulwell.spin-lore"));
            if (rolls == 1 && ps != null && ps.hasSoulWellXezbethFreeRollPending()) {
                lore.add(new Messaging.MessageFormatter().format("soulwell.xezbeth-free-lore"));
            }
            String spinName = new Messaging.MessageFormatter().setVariable("rolls", "" + rolls).format("items.soul-well-spin");
            ItemStack raw = new ItemStack(Material.NETHER_STAR, 1);
            inv.setItem(slot, SkyWarsReloaded.getNMS().getItemStack(raw, lore, ChatColor.translateAlternateColorCodes('&', spinName)));
        }

        ItemStack buy = new ItemStack(Material.EMERALD, 1);
        List<String> buyLore = Lists.newArrayList();
        buyLore.add(new Messaging.MessageFormatter().format("soulwell.buy-lore"));
        String buyName = new Messaging.MessageFormatter().format("items.soul-well-buy-souls");
        inv.setItem(22, SkyWarsReloaded.getNMS().getItemStack(buy, buyLore, ChatColor.translateAlternateColorCodes('&', buyName)));

        ArrayList<Inventory> invs = new ArrayList<>();
        invs.add(inv);

        SkyWarsReloaded.getIC().create(player, invs, event -> {
            String name = event.getName();
            if (name.equalsIgnoreCase(SkyWarsReloaded.getNMS().getItemName(SkyWarsReloaded.getIM().getItem("exitMenuItem")))) {
                player.closeInventory();
                return;
            }
            int slot = event.getSlot();
            if (slot == 22) {
                if (forced) {
                    SoulWellBuyMenu.openForced(player);
                } else {
                    SoulWellBuyMenu.open(player);
                }
                Util.get().playSound(player, player.getLocation(), SkyWarsReloaded.getCfg().getOpenOptionsMenuSound(), 1, 1);
                return;
            }
            if (slot >= 10 && slot <= 16 && (slot - 10) % 2 == 0) {
                int rolls = (slot - 10) / 2 + 1;
                if (rolls >= 1 && rolls <= max) {
                    if (forced) {
                        SoulWellService.beginSpinForced(player, rolls);
                    } else {
                        SoulWellService.beginSpin(player, rolls);
                    }
                }
            }
        });

        player.closeInventory();
        SkyWarsReloaded.getIC().show(player, null);
    }

    private static ItemStack skullInfo(Player player, int souls) {
        ItemStack skull = SkyWarsReloaded.getNMS().getBlankPlayerHead();
        org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) skull.getItemMeta();
        SkyWarsReloaded.getNMS().updateSkull(meta, player);
        List<String> lore = Lists.newArrayList();
        lore.add(new Messaging.MessageFormatter().setVariable("souls", "" + souls).format("soulwell.you-have-souls"));
        String title = new Messaging.MessageFormatter().format("items.soul-well-info");
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', title));
        meta.setLore(lore);
        skull.setItemMeta(meta);
        return skull;
    }
}
