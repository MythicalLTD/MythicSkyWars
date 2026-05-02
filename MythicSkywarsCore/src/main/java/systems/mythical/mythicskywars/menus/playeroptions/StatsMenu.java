package systems.mythical.mythicskywars.menus.playeroptions;

import com.google.common.collect.Lists;
import systems.mythical.mythicskywars.MythicSkywars;
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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class StatsMenu {

    private StatsMenu() {
    }

    public static void open(Player player) {
        FileConfiguration cfg = MythicSkywars.get().getConfig();
        if (!cfg.getBoolean("stats-menu.enabled", true)) {
            player.sendMessage(new Messaging.MessageFormatter().format("stats.not-ready"));
            return;
        }

        PlayerStat ps = PlayerStat.getPlayerStats(player);
        if (ps == null || !ps.isInitialized()) {
            player.sendMessage(new Messaging.MessageFormatter().format("stats.not-ready"));
            return;
        }

        int size = normalizeSize(cfg.getInt("stats-menu.size", 27));
        String title = new Messaging.MessageFormatter().format("menu.stats-menu-title");
        Inventory inv = Bukkit.createInventory(null, size, title);

        int profileSlot = clampSlot(cfg.getInt("stats-menu.slots.profile", 11), size);
        int economySlot = clampSlot(cfg.getInt("stats-menu.slots.economy", 13), size);
        int soulwellSlot = clampSlot(cfg.getInt("stats-menu.slots.soulwell", 15), size);
        int closeSlot = clampSlot(cfg.getInt("stats-menu.slots.close", 22), size);

        inv.setItem(profileSlot, profileItem(player, ps));

        if (cfg.getBoolean("stats-menu.show.economy", true)) {
            Material ecoMat = readMaterial(cfg.getString("stats-menu.items.economy-material", "GOLD_INGOT"), Material.GOLD_INGOT);
            inv.setItem(economySlot, buildInfoItem(ecoMat, "menu.stats-menu-economy-name", "menu.stats-menu-economy-lore", player, ps));
        }
        if (cfg.getBoolean("stats-menu.show.soulwell", true)) {
            Material soulMat = readMaterial(cfg.getString("stats-menu.items.soulwell-material", "NETHER_STAR"), Material.NETHER_STAR);
            inv.setItem(soulwellSlot, buildInfoItem(soulMat, "menu.stats-menu-soulwell-name", "menu.stats-menu-soulwell-lore", player, ps));
        }

        Material closeMat = readMaterial(cfg.getString("stats-menu.items.close-material", "BARRIER"), Material.BARRIER);
        inv.setItem(closeSlot, buildInfoItem(closeMat, "menu.stats-menu-close-name", "menu.stats-menu-close-lore", player, ps));

        ArrayList<Inventory> invs = new ArrayList<>();
        invs.add(inv);
        MythicSkywars.getIC().create(player, invs, event -> {
            int slot = event.getSlot();
            if (slot == closeSlot) {
                player.closeInventory();
            }
        });
        player.closeInventory();
        MythicSkywars.getIC().show(player, null);
    }

    private static ItemStack profileItem(Player player, PlayerStat ps) {
        ItemStack skull = MythicSkywars.getNMS().getBlankPlayerHead();
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        MythicSkywars.getNMS().updateSkull(meta, player);
        meta.setDisplayName(color(new Messaging.MessageFormatter().format("menu.stats-menu-profile-name")));
        meta.setLore(coloredLore("menu.stats-menu-profile-lore", player, ps));
        skull.setItemMeta(meta);
        return skull;
    }

    private static ItemStack buildInfoItem(Material material, String nameKey, String loreKey, Player player, PlayerStat ps) {
        String name = color(new Messaging.MessageFormatter().format(nameKey));
        List<String> lore = coloredLore(loreKey, player, ps);
        return MythicSkywars.getNMS().getItemStack(new ItemStack(material, 1), lore, name);
    }

    private static List<String> coloredLore(String key, Player player, PlayerStat ps) {
        List<String> out = Lists.newArrayList();
        for (String line : MythicSkywars.getMessaging().getFile().getStringList(key)) {
            out.add(color(applyVariables(line, player, ps)));
        }
        return out;
    }

    private static String applyVariables(String text, Player player, PlayerStat ps) {
        return text
                .replace("{player}", ps.getPlayerName())
                .replace("{wins}", Integer.toString(ps.getWins()))
                .replace("{losses}", Integer.toString(ps.getLosses()))
                .replace("{kills}", Integer.toString(ps.getKills()))
                .replace("{deaths}", Integer.toString(ps.getDeaths()))
                .replace("{xp}", Integer.toString(ps.getXp()))
                .replace("{souls}", Integer.toString(ps.getSouls()))
                .replace("{soulwell_usages}", Integer.toString(ps.getSoulWellUsages()))
                .replace("{soulwell_legendaries}", Integer.toString(ps.getSoulWellLegendaries()))
                .replace("{soulwell_rares}", Integer.toString(ps.getSoulWellRares()))
                .replace("{soulwell_souls_gathered}", Integer.toString(ps.getSoulWellSoulsGathered()))
                .replace("{soulwell_souls_purchased}", Integer.toString(ps.getSoulWellSoulsPurchased()))
                .replace("{killdeath}", ratio(ps.getKills(), ps.getDeaths()))
                .replace("{winloss}", ratio(ps.getWins(), ps.getLosses()))
                .replace("{level}", Integer.toString(Util.get().getPlayerLevel(player)))
                .replace("{balance}", balance(player));
    }

    private static String balance(Player player) {
        if (MythicSkywars.getCfg().economyEnabled() && VaultUtils.get().isEconomyAvailable()) {
            return String.format(Locale.ENGLISH, "%.2f", VaultUtils.get().getBalance(player));
        }
        return "0.00";
    }

    private static String ratio(int top, int bottom) {
        if (top <= 0) {
            return "0.00";
        }
        if (bottom <= 0) {
            return String.format(Locale.ENGLISH, "%.2f", (double) top);
        }
        return String.format(Locale.ENGLISH, "%.2f", ((double) top) / ((double) bottom));
    }

    private static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private static int normalizeSize(int raw) {
        int size = Math.max(9, raw);
        return ((size + 8) / 9) * 9;
    }

    private static int clampSlot(int slot, int size) {
        if (slot < 0) {
            return 0;
        }
        if (slot >= size) {
            return size - 1;
        }
        return slot;
    }

    private static Material readMaterial(String name, Material fallback) {
        if (name == null || name.trim().isEmpty()) {
            return fallback;
        }
        Material material = Material.matchMaterial(name.trim());
        return material != null ? material : fallback;
    }
}
