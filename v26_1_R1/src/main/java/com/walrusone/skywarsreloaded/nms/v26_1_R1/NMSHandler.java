package com.walrusone.skywarsreloaded.nms.v26_1_R1;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;

import java.util.Locale;

public class NMSHandler extends com.walrusone.skywarsreloaded.nms.v1_21_R1.NMSHandler {

    @Override
    public int getVersion() {
        return 26;
    }

    @Override
    public PotionEffectType getPotionEffectTypeByName(String... name) {
        for (String n : name) {
            try {
                PotionEffectType type = Registry.EFFECT.get(NamespacedKey.fromString(n.toLowerCase(Locale.ROOT)));
                if (type != null) {
                    return type;
                }
            } catch (Exception e) {
                // Registry API may have changed, try next name
            }
        }
        return null;
    }

    @Override
    public Enchantment getEnchantmentByName(String... name) {
        for (String n : name) {
            try {
                Enchantment enchantment = Registry.ENCHANTMENT.get(NamespacedKey.fromString(n.toLowerCase(Locale.ROOT)));
                if (enchantment != null) {
                    return enchantment;
                }
            } catch (Exception e) {
                // Registry API may have changed, try next name
            }
        }
        return null;
    }

}
