package systems.mythical.mythicskywars.menus.playeroptions;

import com.google.common.collect.Lists;
import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.enums.PlayerOptions;
import systems.mythical.mythicskywars.menus.soulwell.SoulWellMenu;
import systems.mythical.mythicskywars.perks.PerkManager;
import systems.mythical.mythicskywars.perks.PerksMenu;
import systems.mythical.mythicskywars.utilities.Messaging;
import systems.mythical.mythicskywars.utilities.PrestigeManager;
import systems.mythical.mythicskywars.utilities.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class OptionsSelectionMenu {

    private static final String menuName = new Messaging.MessageFormatter().format("menu.options-menu-title");

    public OptionsSelectionMenu(final Player player) {
        int menuSize = 27;
        Inventory inv = Bukkit.createInventory(null, menuSize + 9, menuName);

        if (MythicSkywars.getCfg().glassMenuEnabled()) {
            inv.setItem(MythicSkywars.getCfg().getGlassSlot(), MythicSkywars.getIM().getItem("glassselect"));
        }

        if (MythicSkywars.getCfg().particleMenuEnabled()) {
            inv.setItem(MythicSkywars.getCfg().getParticleSlot(), MythicSkywars.getIM().getItem("particleselect"));
        }
        if (MythicSkywars.getCfg().projectileMenuEnabled()) {
            inv.setItem(MythicSkywars.getCfg().getProjectileSlot(), MythicSkywars.getIM().getItem("projectileselect"));
        }
        if (MythicSkywars.getCfg().killsoundMenuEnabled()) {
            inv.setItem(MythicSkywars.getCfg().getKillSoundSlot(), MythicSkywars.getIM().getItem("killsoundselect"));
        }
        if (MythicSkywars.getCfg().winsoundMenuEnabled()) {
            inv.setItem(MythicSkywars.getCfg().getWinSoundSlot(), MythicSkywars.getIM().getItem("winsoundselect"));
        }
        if (MythicSkywars.getCfg().tauntsMenuEnabled()) {
            inv.setItem(MythicSkywars.getCfg().getTauntSlot(), MythicSkywars.getIM().getItem("tauntselect"));
        }

        if (MythicSkywars.getCfg().isSoulWellEnabled() && player.hasPermission("sw.soulwell")) {
            int swSlot = MythicSkywars.getCfg().getSoulWellOptionsMenuSlot();
            if (swSlot >= 0 && swSlot < menuSize) {
                ItemStack soulIcon = new ItemStack(soulWellIconMaterial(), 1);
                List<String> lore = Lists.newArrayList();
                lore.add(new Messaging.MessageFormatter().format("items.soul-well-open-lore"));
                String soulName = new Messaging.MessageFormatter().format("items.soul-well-sel");
                inv.setItem(swSlot, MythicSkywars.getNMS().getItemStack(soulIcon, lore,
                        ChatColor.translateAlternateColorCodes('&', soulName)));
            }
        }

        if (PrestigeManager.get().isEnabled() && player.hasPermission("sw.prestige")) {
            int prestigeSlot = PrestigeManager.get().getOptionsMenuSlot();
            if (prestigeSlot >= 0 && prestigeSlot < menuSize) {
                ItemStack icon = new ItemStack(Material.NETHER_STAR, 1);
                List<String> lore = Lists.newArrayList();
                lore.add(new Messaging.MessageFormatter().format("items.prestige-open-lore"));
                String prestigeName = new Messaging.MessageFormatter().format("items.prestige-sel");
                inv.setItem(prestigeSlot, MythicSkywars.getNMS().getItemStack(icon, lore,
                        ChatColor.translateAlternateColorCodes('&', prestigeName)));
            }
        }

        if (PerkManager.get().isEnabled()
                && MythicSkywars.get().getConfig().getBoolean("enabledMenus.perks", true)
                && player.hasPermission("sw.perks")) {
            int perkSlot = PerkManager.get().getOptionsMenuSlot();
            if (perkSlot >= 0 && perkSlot < menuSize) {
                ItemStack perkIcon = new ItemStack(perksIconMaterial(), 1);
                List<String> perkLore = Lists.newArrayList();
                perkLore.add(new Messaging.MessageFormatter().format("items.perks-open-lore"));
                String perkName = new Messaging.MessageFormatter().format("items.perks-sel");
                inv.setItem(perkSlot, MythicSkywars.getNMS().getItemStack(perkIcon, perkLore,
                        ChatColor.translateAlternateColorCodes('&', perkName)));
            }
        }

        ArrayList<Inventory> invs = new ArrayList<>();
        invs.add(inv);

        MythicSkywars.getIC().create(player, invs, event -> {

            String name = event.getName();

            if (name.equalsIgnoreCase(new Messaging.MessageFormatter().format("items.particle-effect-sel"))) {
                new OptionSelectionMenu(player, PlayerOptions.PARTICLEEFFECT, false);
                Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getOpenParticleMenuSound(), 1, 1);
            } else if (name.equalsIgnoreCase(new Messaging.MessageFormatter().format("items.projectile-effect-sel"))) {
                new OptionSelectionMenu(player, PlayerOptions.PROJECTILEEFFECT, false);
                Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getOpenProjectileMenuSound(), 1, 1);
            } else if (name.equalsIgnoreCase(new Messaging.MessageFormatter().format("items.killsound-sel"))) {
                new OptionSelectionMenu(player, PlayerOptions.KILLSOUND, false);
                Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getOpenKillSoundMenuSound(), 1, 1);
            } else if (name.equalsIgnoreCase(new Messaging.MessageFormatter().format("items.winsound-sel"))) {
                new OptionSelectionMenu(player, PlayerOptions.WINSOUND, false);
                Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getOpenWinSoundMenuSound(), 1, 1);
            } else if (name.equalsIgnoreCase(new Messaging.MessageFormatter().format("items.glass-sel"))) {
                new OptionSelectionMenu(player, PlayerOptions.GLASSCOLOR, false);
                Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getOpenGlassMenuSound(), 1, 1);
            } else if (name.equalsIgnoreCase(new Messaging.MessageFormatter().format("items.taunt-sel"))) {
                new OptionSelectionMenu(player, PlayerOptions.TAUNT, false);
                Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getOpenTauntMenuSound(), 1, 1);
            } else if (name.equalsIgnoreCase(new Messaging.MessageFormatter().format("items.soul-well-sel"))) {
                if (MythicSkywars.getCfg().isSoulWellEnabled() && player.hasPermission("sw.soulwell")) {
                    SoulWellMenu.open(player);
                    Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getOpenOptionsMenuSound(), 1, 1);
                }
            } else if (name.equalsIgnoreCase(new Messaging.MessageFormatter().format("items.prestige-sel"))) {
                if (PrestigeManager.get().isEnabled() && player.hasPermission("sw.prestige")) {
                    PrestigeMenu.open(player);
                    Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getOpenOptionsMenuSound(), 1, 1);
                }
            } else if (name.equalsIgnoreCase(new Messaging.MessageFormatter().format("items.perks-sel"))) {
                if (PerkManager.get().isEnabled() && player.hasPermission("sw.perks")) {
                    PerksMenu.open(player);
                    Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getOpenOptionsMenuSound(), 1, 1);
                }
            } else if (name.equalsIgnoreCase(new Messaging.MessageFormatter().format("items.exit-menu-item"))) {
                player.closeInventory();
            }
        });

        if (player != null) {
            MythicSkywars.getIC().show(player, null);
        }
    }

    private static Material soulWellIconMaterial() {
        if (MythicSkywars.getNMS().getVersion() < 13) {
            return Material.ENDER_PORTAL_FRAME;
        }
        try {
            return Material.valueOf("END_PORTAL_FRAME");
        } catch (IllegalArgumentException e) {
            return Material.NETHER_STAR;
        }
    }

    private static Material perksIconMaterial() {
        try {
            return Material.valueOf("BEACON");
        } catch (IllegalArgumentException e) {
            return Material.DIAMOND;
        }
    }
}