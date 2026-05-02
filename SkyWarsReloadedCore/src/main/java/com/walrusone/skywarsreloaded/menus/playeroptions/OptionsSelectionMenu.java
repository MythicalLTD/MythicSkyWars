package com.walrusone.skywarsreloaded.menus.playeroptions;

import com.google.common.collect.Lists;
import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.enums.PlayerOptions;
import com.walrusone.skywarsreloaded.menus.soulwell.SoulWellMenu;
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
import java.util.List;

public class OptionsSelectionMenu {

    private static final String menuName = new Messaging.MessageFormatter().format("menu.options-menu-title");

    public OptionsSelectionMenu(final Player player) {
        int menuSize = 27;
        Inventory inv = Bukkit.createInventory(null, menuSize + 9, menuName);

        if (SkyWarsReloaded.getCfg().glassMenuEnabled()) {
            inv.setItem(SkyWarsReloaded.getCfg().getGlassSlot(), SkyWarsReloaded.getIM().getItem("glassselect"));
        }

        if (SkyWarsReloaded.getCfg().particleMenuEnabled()) {
            inv.setItem(SkyWarsReloaded.getCfg().getParticleSlot(), SkyWarsReloaded.getIM().getItem("particleselect"));
        }
        if (SkyWarsReloaded.getCfg().projectileMenuEnabled()) {
            inv.setItem(SkyWarsReloaded.getCfg().getProjectileSlot(), SkyWarsReloaded.getIM().getItem("projectileselect"));
        }
        if (SkyWarsReloaded.getCfg().killsoundMenuEnabled()) {
            inv.setItem(SkyWarsReloaded.getCfg().getKillSoundSlot(), SkyWarsReloaded.getIM().getItem("killsoundselect"));
        }
        if (SkyWarsReloaded.getCfg().winsoundMenuEnabled()) {
            inv.setItem(SkyWarsReloaded.getCfg().getWinSoundSlot(), SkyWarsReloaded.getIM().getItem("winsoundselect"));
        }
        if (SkyWarsReloaded.getCfg().tauntsMenuEnabled()) {
            inv.setItem(SkyWarsReloaded.getCfg().getTauntSlot(), SkyWarsReloaded.getIM().getItem("tauntselect"));
        }

        if (SkyWarsReloaded.getCfg().isSoulWellEnabled() && player.hasPermission("sw.soulwell")) {
            int swSlot = SkyWarsReloaded.getCfg().getSoulWellOptionsMenuSlot();
            if (swSlot >= 0 && swSlot < menuSize) {
                ItemStack soulIcon = new ItemStack(soulWellIconMaterial(), 1);
                List<String> lore = Lists.newArrayList();
                lore.add(new Messaging.MessageFormatter().format("items.soul-well-open-lore"));
                String soulName = new Messaging.MessageFormatter().format("items.soul-well-sel");
                inv.setItem(swSlot, SkyWarsReloaded.getNMS().getItemStack(soulIcon, lore,
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
                inv.setItem(prestigeSlot, SkyWarsReloaded.getNMS().getItemStack(icon, lore,
                        ChatColor.translateAlternateColorCodes('&', prestigeName)));
            }
        }

        ArrayList<Inventory> invs = new ArrayList<>();
        invs.add(inv);

        SkyWarsReloaded.getIC().create(player, invs, event -> {

            String name = event.getName();

            if (name.equalsIgnoreCase(new Messaging.MessageFormatter().format("items.particle-effect-sel"))) {
                new OptionSelectionMenu(player, PlayerOptions.PARTICLEEFFECT, false);
                Util.get().playSound(player, player.getLocation(), SkyWarsReloaded.getCfg().getOpenParticleMenuSound(), 1, 1);
            } else if (name.equalsIgnoreCase(new Messaging.MessageFormatter().format("items.projectile-effect-sel"))) {
                new OptionSelectionMenu(player, PlayerOptions.PROJECTILEEFFECT, false);
                Util.get().playSound(player, player.getLocation(), SkyWarsReloaded.getCfg().getOpenProjectileMenuSound(), 1, 1);
            } else if (name.equalsIgnoreCase(new Messaging.MessageFormatter().format("items.killsound-sel"))) {
                new OptionSelectionMenu(player, PlayerOptions.KILLSOUND, false);
                Util.get().playSound(player, player.getLocation(), SkyWarsReloaded.getCfg().getOpenKillSoundMenuSound(), 1, 1);
            } else if (name.equalsIgnoreCase(new Messaging.MessageFormatter().format("items.winsound-sel"))) {
                new OptionSelectionMenu(player, PlayerOptions.WINSOUND, false);
                Util.get().playSound(player, player.getLocation(), SkyWarsReloaded.getCfg().getOpenWinSoundMenuSound(), 1, 1);
            } else if (name.equalsIgnoreCase(new Messaging.MessageFormatter().format("items.glass-sel"))) {
                new OptionSelectionMenu(player, PlayerOptions.GLASSCOLOR, false);
                Util.get().playSound(player, player.getLocation(), SkyWarsReloaded.getCfg().getOpenGlassMenuSound(), 1, 1);
            } else if (name.equalsIgnoreCase(new Messaging.MessageFormatter().format("items.taunt-sel"))) {
                new OptionSelectionMenu(player, PlayerOptions.TAUNT, false);
                Util.get().playSound(player, player.getLocation(), SkyWarsReloaded.getCfg().getOpenTauntMenuSound(), 1, 1);
            } else if (name.equalsIgnoreCase(new Messaging.MessageFormatter().format("items.soul-well-sel"))) {
                if (SkyWarsReloaded.getCfg().isSoulWellEnabled() && player.hasPermission("sw.soulwell")) {
                    SoulWellMenu.open(player);
                    Util.get().playSound(player, player.getLocation(), SkyWarsReloaded.getCfg().getOpenOptionsMenuSound(), 1, 1);
                }
            } else if (name.equalsIgnoreCase(new Messaging.MessageFormatter().format("items.prestige-sel"))) {
                if (PrestigeManager.get().isEnabled() && player.hasPermission("sw.prestige")) {
                    PrestigeMenu.open(player);
                    Util.get().playSound(player, player.getLocation(), SkyWarsReloaded.getCfg().getOpenOptionsMenuSound(), 1, 1);
                }
            } else if (name.equalsIgnoreCase(new Messaging.MessageFormatter().format("items.exit-menu-item"))) {
                player.closeInventory();
            }
        });

        if (player != null) {
            SkyWarsReloaded.getIC().show(player, null);
        }
    }

    private static Material soulWellIconMaterial() {
        if (SkyWarsReloaded.getNMS().getVersion() < 13) {
            return Material.ENDER_PORTAL_FRAME;
        }
        try {
            return Material.valueOf("END_PORTAL_FRAME");
        } catch (IllegalArgumentException e) {
            return Material.NETHER_STAR;
        }
    }
}