package systems.mythical.mythicskywars.perks;

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
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI menu for viewing and purchasing perks.
 */
public final class PerksMenu {

    private PerksMenu() {
    }

    public static void open(Player player) {
        if (!PerkManager.get().isEnabled()) {
            return;
        }

        int menuSize = PerkManager.get().getMenuSize();
        String title = ChatColor.translateAlternateColorCodes('&', PerkManager.get().getMenuTitle());
        Inventory inv = Bukkit.createInventory(null, menuSize, title);

        PlayerStat ps = PlayerStat.getPlayerStats(player);

        for (Perk perk : PerkManager.get().getAllPerks()) {
            if (perk.isDisabled()) continue;
            int slot = perk.getSlot();
            if (slot < 0 || slot >= menuSize) continue;

            // Determine player's current level for this perk
            int currentLevel = getPlayerPerkLevel(player, perk);
            int maxLevel = perk.getMaxLevel();

            ItemStack icon = new ItemStack(perk.getIcon(), Math.max(1, currentLevel));
            List<String> lore = new ArrayList<>();

            // Add perk description lore
            for (String line : perk.getLore()) {
                PerkLevel displayLevel = perk.getLevel(Math.max(1, currentLevel));
                String formatted = line
                        .replace("{percent}", displayLevel != null ? "" + displayLevel.getPercent() : "?")
                        .replace("{duration}", displayLevel != null ? "" + displayLevel.getDuration() : "?");
                lore.add(ChatColor.translateAlternateColorCodes('&', formatted));
            }

            lore.add("");

            // Level progress
            if (currentLevel >= maxLevel) {
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Level: &a" + currentLevel + "/" + maxLevel + " &6(MAX)"));
                lore.add("");
                lore.add(ChatColor.translateAlternateColorCodes('&', "&6MAX LEVEL"));
            } else if (currentLevel > 0) {
                PerkLevel nextLevel = perk.getLevel(currentLevel + 1);
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Level: &e" + currentLevel + "/" + maxLevel));
                if (nextLevel != null) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&7Next level: &e" + nextLevel.getPercent() + "%"));
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&7Upgrade cost: &6" + nextLevel.getCost() + " coins"));
                }
                lore.add("");
                lore.add(ChatColor.translateAlternateColorCodes('&', "&eClick to Upgrade!"));
            } else {
                PerkLevel firstLevel = perk.getLevel(1);
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Level: &c0/" + maxLevel));
                if (firstLevel != null) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&7Effect: &e" + firstLevel.getPercent() + "%"));
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&7Cost: &6" + firstLevel.getCost() + " coins"));
                }
                lore.add("");
                lore.add(ChatColor.translateAlternateColorCodes('&', "&cFound it in a Soul Well!"));
                if (MythicSkywars.getCfg().economyEnabled()) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&eOr click to purchase!"));
                }
            }

            String displayName = ChatColor.translateAlternateColorCodes('&', perk.getName());
            inv.setItem(slot, MythicSkywars.getNMS().getItemStack(icon, lore, displayName));
        }

        ArrayList<Inventory> invs = new ArrayList<>();
        invs.add(inv);

        MythicSkywars.getIC().create(player, invs, event -> {
            int clickedSlot = event.getSlot();
            Perk clickedPerk = null;
            for (Perk p : PerkManager.get().getAllPerks()) {
                if (!p.isDisabled() && p.getSlot() == clickedSlot) {
                    clickedPerk = p;
                    break;
                }
            }
            if (clickedPerk == null) return;

            int currentLvl = getPlayerPerkLevel(player, clickedPerk);
            int nextLvl = currentLvl + 1;

            if (nextLvl > clickedPerk.getMaxLevel()) {
                // Already maxed
                player.sendMessage(new Messaging.MessageFormatter().format("perks.maxed"));
                Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getErrorSound(), 1, 1);
                return;
            }

            PerkLevel targetLevel = clickedPerk.getLevel(nextLvl);
            if (targetLevel == null) return;

            // Check if economy is enabled for purchasing
            if (!MythicSkywars.getCfg().economyEnabled()) {
                player.sendMessage(new Messaging.MessageFormatter().format("perks.economy-disabled"));
                Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getErrorSound(), 1, 1);
                return;
            }

            int cost = targetLevel.getCost();
            if (!VaultUtils.get().canBuy(player, cost)) {
                player.sendMessage(new Messaging.MessageFormatter()
                        .setVariable("cost", "" + cost)
                        .format("menu.insufficientfunds"));
                Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getErrorSound(), 1, 1);
                return;
            }

            if (!VaultUtils.get().payCost(player, cost)) {
                return;
            }

            // Grant the perk permission
            PlayerStat pStat = PlayerStat.getPlayerStats(player);
            if (pStat != null) {
                pStat.addPerm(clickedPerk.getPermission(nextLvl), true);
            }

            String perkName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', clickedPerk.getName()));
            player.sendMessage(new Messaging.MessageFormatter()
                    .setVariable("perk", perkName)
                    .setVariable("level", "" + nextLvl)
                    .setVariable("cost", "" + cost)
                    .format("perks.purchased"));
            Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getConfirmeSelctionSound(), 1, 1);

            // Reopen menu to show updated state
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(MythicSkywars.get(), () -> open(player), 2L);
        });

        MythicSkywars.getIC().show(player, null);
    }

    private static int getPlayerPerkLevel(Player player, Perk perk) {
        int highest = 0;
        for (int i = 1; i <= perk.getMaxLevel(); i++) {
            if (player.hasPermission(perk.getPermission(i))) {
                highest = i;
            }
        }
        return highest;
    }
}
