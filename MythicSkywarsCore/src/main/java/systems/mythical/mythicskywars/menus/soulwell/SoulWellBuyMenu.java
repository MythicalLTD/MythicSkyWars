package systems.mythical.mythicskywars.menus.soulwell;

import com.google.common.collect.Lists;
import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.database.DataStorage;
import systems.mythical.mythicskywars.managers.PlayerStat;
import systems.mythical.mythicskywars.utilities.Messaging;
import systems.mythical.mythicskywars.utilities.Util;
import systems.mythical.mythicskywars.utilities.VaultUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class SoulWellBuyMenu {

    private SoulWellBuyMenu() {
    }

    public static void open(Player player) {
        open(player, false);
    }

    public static void openForced(Player player) {
        open(player, true);
    }

    private static void open(Player player, boolean forced) {
        if (!forced && !MythicSkywars.getCfg().isSoulWellEnabled()) {
            return;
        }
        FileConfiguration cfg = MythicSkywars.get().getConfig();
        int menuSize = 27;
        String menuName = new Messaging.MessageFormatter().format("menu.soul-well-buy-title");
        Inventory inv = Bukkit.createInventory(null, menuSize + 9, menuName);
        inv.setItem(4, buildShopInfo(player));

        placeTier(inv, cfg, 11, "warlock", Material.EMERALD);
        placeTier(inv, cfg, 13, "necromancer", Material.DIAMOND);
        placeTier(inv, cfg, 15, "deathangel", Material.NETHER_STAR);
        placeDiscountUpgrade(inv, player, cfg, 29);
        placeFrameUpgrade(inv, player, cfg, 33);

        ArrayList<Inventory> invs = new ArrayList<>();
        invs.add(inv);

        MythicSkywars.getIC().create(player, invs, event -> {
            String name = event.getName();
            if (name.equalsIgnoreCase(MythicSkywars.getNMS().getItemName(MythicSkywars.getIM().getItem("exitMenuItem")))) {
                if (forced) {
                    SoulWellMenu.openForced(player);
                } else {
                    SoulWellMenu.open(player);
                }
                return;
            }
            int slot = event.getSlot();
            if (slot == 11) {
                tryPurchase(player, "warlock", forced);
            } else if (slot == 13) {
                tryPurchase(player, "necromancer", forced);
            } else if (slot == 15) {
                tryPurchase(player, "deathangel", forced);
            } else if (slot == 29) {
                tryPurchaseDiscountUpgrade(player, forced);
            } else if (slot == 33) {
                tryPurchaseFrameUpgrade(player, forced);
            }
        });

        MythicSkywars.getIC().show(player, null);
    }

    private static ItemStack buildShopInfo(Player player) {
        PlayerStat ps = PlayerStat.getPlayerStats(player);
        int souls = ps != null ? ps.getSouls() : 0;
        int spinCost = SoulWellService.calculateSpinCost(player, 1);
        List<String> lore = Lists.newArrayList();
        lore.add(new Messaging.MessageFormatter().setVariable("souls", Integer.toString(souls)).format("soulwell.you-have-souls"));
        lore.add(new Messaging.MessageFormatter().setVariable("souls", Integer.toString(spinCost)).format("soulwell.spin-lore"));
        lore.add(new Messaging.MessageFormatter().format("soulwell.shop-info-note"));
        String title = new Messaging.MessageFormatter().format("items.soul-well-shop-info");
        ItemStack raw = new ItemStack(Material.NETHER_STAR, 1);
        return MythicSkywars.getNMS().getItemStack(raw, lore, ChatColor.translateAlternateColorCodes('&', title));
    }

    private static void placeTier(Inventory inv, FileConfiguration cfg, int slot, String tierKey, Material iconMat) {
        int souls = cfg.getInt("soulwell.buy." + tierKey + ".souls", 0);
        double cost = cfg.getDouble("soulwell.buy." + tierKey + ".cost", 0);
        List<String> lore = Lists.newArrayList();
        lore.add(new Messaging.MessageFormatter().setVariable("souls", "" + souls).format("soulwell.buy-tier-souls"));
        if (canBuySoulsWithEconomy()) {
            lore.add(new Messaging.MessageFormatter().setVariable("cost", "" + (int) cost).format("soulwell.buy-tier-cost"));
        } else if (isEconomyConfiguredOn()) {
            lore.add(new Messaging.MessageFormatter().format("soulwell.buy-disabled-no-vault"));
        } else {
            lore.add(new Messaging.MessageFormatter().format("soulwell.buy-disabled-no-economy"));
        }
        String disp = new Messaging.MessageFormatter().setVariable("tier", tierDisplayKey(tierKey)).format("items.soul-well-buy-tier");
        ItemStack raw = new ItemStack(iconMat, 1);
        inv.setItem(slot, MythicSkywars.getNMS().getItemStack(raw, lore, ChatColor.translateAlternateColorCodes('&', disp)));
    }

    private static String tierDisplayKey(String tierKey) {
        if ("warlock".equalsIgnoreCase(tierKey)) {
            return new Messaging.MessageFormatter().format("soulwell.tier-warlock");
        }
        if ("necromancer".equalsIgnoreCase(tierKey)) {
            return new Messaging.MessageFormatter().format("soulwell.tier-necromancer");
        }
        if ("deathangel".equalsIgnoreCase(tierKey)) {
            return new Messaging.MessageFormatter().format("soulwell.tier-death-angel");
        }
        return tierKey;
    }

    private static void tryPurchase(Player player, String tierKey, boolean forced) {
        if (!canBuySoulsWithEconomy()) {
            if (isEconomyConfiguredOn()) {
                player.sendMessage(new Messaging.MessageFormatter().format("soulwell.buy-disabled-no-vault"));
            } else {
                player.sendMessage(new Messaging.MessageFormatter().format("soulwell.buy-disabled-no-economy"));
            }
            Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getErrorSound(), 1, 1);
            return;
        }
        FileConfiguration cfg = MythicSkywars.get().getConfig();
        int souls = cfg.getInt("soulwell.buy." + tierKey + ".souls", 0);
        double cost = cfg.getDouble("soulwell.buy." + tierKey + ".cost", 0);
        if (souls <= 0 || cost <= 0) {
            return;
        }
        if (!VaultUtils.get().canBuy(player, cost)) {
            player.sendMessage(new Messaging.MessageFormatter().withUniversalPrefixForced().format("menu.insufficientfunds"));
            Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getErrorSound(), 1, 1);
            return;
        }
        if (!VaultUtils.get().payCost(player, cost)) {
            return;
        }
        PlayerStat ps = PlayerStat.getPlayerStats(player);
        if (ps == null) {
            return;
        }
        ps.addSouls(souls);
        ps.addSoulWellSoulsPurchased(souls);
        DataStorage.get().saveStats(ps);
        player.sendMessage(new Messaging.MessageFormatter()
                .setVariable("souls", "" + souls)
                .setVariable("cost", "" + (int) cost)
                .format("soulwell.purchased-souls"));
        Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getConfirmeSelctionSound(), 1, 1);
        if (forced) {
            SoulWellMenu.openForced(player);
        } else {
            SoulWellMenu.open(player);
        }
    }

    private static void placeDiscountUpgrade(Inventory inv, Player player, FileConfiguration cfg, int slot) {
        int currentLevel = getDiscountLevel(player);
        int nextLevel = currentLevel + 1;
        List<String> lore = Lists.newArrayList();
        lore.add(new Messaging.MessageFormatter()
                .setVariable("level", Integer.toString(currentLevel))
                .setVariable("percent", Integer.toString(currentLevel * 10))
                .format("soulwell.upgrade-discount-current"));
        if (nextLevel <= 3) {
            int nextCost = cfg.getInt("soulwell.upgrades.discount-costs.level" + nextLevel, 0);
            lore.add(new Messaging.MessageFormatter()
                    .setVariable("level", Integer.toString(nextLevel))
                    .setVariable("percent", Integer.toString(nextLevel * 10))
                    .format("soulwell.upgrade-discount-next"));
            lore.add(new Messaging.MessageFormatter()
                    .setVariable("cost", Integer.toString(nextCost))
                    .format("soulwell.buy-tier-cost"));
        } else {
            lore.add(new Messaging.MessageFormatter().format("soulwell.upgrade-maxed"));
        }
        String title = new Messaging.MessageFormatter().format("items.soul-well-upgrade-discount");
        inv.setItem(slot, MythicSkywars.getNMS().getItemStack(
                new ItemStack(Material.BOOK, 1), lore, ChatColor.translateAlternateColorCodes('&', title)));
    }

    private static void placeFrameUpgrade(Inventory inv, Player player, FileConfiguration cfg, int slot) {
        boolean unlocked = hasFrameUpgrade(player);
        List<String> lore = Lists.newArrayList();
        lore.add(new Messaging.MessageFormatter().format("soulwell.upgrade-frame-desc"));
        if (unlocked) {
            lore.add(new Messaging.MessageFormatter().format("soulwell.upgrade-maxed"));
        } else {
            int cost = cfg.getInt("soulwell.upgrades.frame-cost", 0);
            lore.add(new Messaging.MessageFormatter()
                    .setVariable("cost", Integer.toString(cost))
                    .format("soulwell.buy-tier-cost"));
        }
        String title = new Messaging.MessageFormatter().format("items.soul-well-upgrade-frame");
        inv.setItem(slot, MythicSkywars.getNMS().getItemStack(
                new ItemStack(Material.STAINED_GLASS, 1, (short) 10), lore, ChatColor.translateAlternateColorCodes('&', title)));
    }

    private static void tryPurchaseDiscountUpgrade(Player player, boolean forced) {
        if (!canBuySoulsWithEconomy()) {
            if (isEconomyConfiguredOn()) {
                player.sendMessage(new Messaging.MessageFormatter().format("soulwell.buy-disabled-no-vault"));
            } else {
                player.sendMessage(new Messaging.MessageFormatter().format("soulwell.buy-disabled-no-economy"));
            }
            return;
        }
        FileConfiguration cfg = MythicSkywars.get().getConfig();
        PlayerStat ps = PlayerStat.getPlayerStats(player);
        if (ps == null) {
            return;
        }
        int currentLevel = getDiscountLevel(player);
        int nextLevel = currentLevel + 1;
        if (nextLevel > 3) {
            player.sendMessage(new Messaging.MessageFormatter().format("soulwell.upgrade-maxed"));
            return;
        }
        int cost = cfg.getInt("soulwell.upgrades.discount-costs.level" + nextLevel, 0);
        if (!VaultUtils.get().canBuy(player, cost)) {
            player.sendMessage(new Messaging.MessageFormatter().withUniversalPrefixForced().format("menu.insufficientfunds"));
            return;
        }
        if (!VaultUtils.get().payCost(player, cost)) {
            return;
        }
        ps.addPerm("sw.soulwell.upgrade.discount." + nextLevel, true);
        player.sendMessage(new Messaging.MessageFormatter()
                .setVariable("name", "Soul Discount " + (nextLevel * 10) + "%")
                .format("soulwell.reward-perk"));
        reopen(player, forced);
    }

    private static void tryPurchaseFrameUpgrade(Player player, boolean forced) {
        if (!canBuySoulsWithEconomy()) {
            if (isEconomyConfiguredOn()) {
                player.sendMessage(new Messaging.MessageFormatter().format("soulwell.buy-disabled-no-vault"));
            } else {
                player.sendMessage(new Messaging.MessageFormatter().format("soulwell.buy-disabled-no-economy"));
            }
            return;
        }
        if (hasFrameUpgrade(player)) {
            player.sendMessage(new Messaging.MessageFormatter().format("soulwell.upgrade-maxed"));
            return;
        }
        FileConfiguration cfg = MythicSkywars.get().getConfig();
        int cost = cfg.getInt("soulwell.upgrades.frame-cost", 0);
        if (!VaultUtils.get().canBuy(player, cost)) {
            player.sendMessage(new Messaging.MessageFormatter().withUniversalPrefixForced().format("menu.insufficientfunds"));
            return;
        }
        if (!VaultUtils.get().payCost(player, cost)) {
            return;
        }
        PlayerStat ps = PlayerStat.getPlayerStats(player);
        if (ps == null) {
            return;
        }
        ps.addPerm("sw.soulwell.upgrade.frames.1", true);
        player.sendMessage(new Messaging.MessageFormatter()
                .setVariable("name", "Soul Frame Animation")
                .format("soulwell.reward-perk"));
        reopen(player, forced);
    }

    private static void reopen(Player player, boolean forced) {
        Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getConfirmeSelctionSound(), 1, 1);
        if (forced) {
            SoulWellMenu.openForced(player);
        } else {
            SoulWellMenu.open(player);
        }
    }

    private static int getDiscountLevel(Player player) {
        if (player.hasPermission("sw.soulwell.upgrade.discount.3")) {
            return 3;
        }
        if (player.hasPermission("sw.soulwell.upgrade.discount.2")) {
            return 2;
        }
        if (player.hasPermission("sw.soulwell.upgrade.discount.1")) {
            return 1;
        }
        return 0;
    }

    private static boolean hasFrameUpgrade(Player player) {
        return player.hasPermission("sw.soulwell.upgrade.frames.1");
    }

    private static boolean isEconomyConfiguredOn() {
        return MythicSkywars.get().getConfig().getBoolean("economyEnabled", false);
    }

    private static boolean canBuySoulsWithEconomy() {
        return MythicSkywars.getCfg().economyEnabled() && VaultUtils.get().isEconomyAvailable();
    }
}
