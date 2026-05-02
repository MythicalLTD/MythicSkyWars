package com.walrusone.skywarsreloaded.nms.v26_1_R1;

import org.bukkit.GameRule;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class NMSHandler extends com.walrusone.skywarsreloaded.nms.v1_21_R1.NMSHandler {

    // Map old game rule names to new Minecraft 1.21.11+ names
    private static final Map<String, String> GAME_RULE_MAPPINGS = new HashMap<>();
    
    static {
        GAME_RULE_MAPPINGS.put("doMobSpawning", "minecraft:spawn_mobs");
        GAME_RULE_MAPPINGS.put("mobGriefing", "minecraft:mob_griefing");
        GAME_RULE_MAPPINGS.put("doFireTick", "minecraft:fire_spread_radius_around_players");
        GAME_RULE_MAPPINGS.put("showDeathMessages", "minecraft:show_death_messages");
        GAME_RULE_MAPPINGS.put("announceAdvancements", "minecraft:show_advancement_messages");
        GAME_RULE_MAPPINGS.put("doDaylightCycle", "advance_time");
    }

    @Override
    public int getVersion() {
        return 26;
    }

    @Override
    public void setGameRule(World world, String ruleName, String value) {
        // Map old game rule names to new ones for Minecraft 1.21.11+
        String mappedRuleName = GAME_RULE_MAPPINGS.getOrDefault(ruleName, ruleName);
        
        // Handle bools
        Boolean valueBool = null;
        if (value.equalsIgnoreCase("true")) valueBool = true;
        else if (value.equalsIgnoreCase("false")) valueBool = false;
        
        // Handle ints
        Integer valueInt = null;
        if (valueBool == null) {
            try {
                valueInt = Integer.parseInt(value);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        
        // Apply
        try {
            if (valueBool == null) {
                GameRule<Integer> gameRule = (GameRule<Integer>) GameRule.getByName(mappedRuleName);
                if (gameRule == null || valueInt == null)
                    throw new Exception("Invalid GameRule or value provided: " + mappedRuleName + " (original: " + ruleName + ") -> " + value);
                world.setGameRule(gameRule, valueInt);
            } else {
                GameRule<Boolean> gameRule = (GameRule<Boolean>) GameRule.getByName(mappedRuleName);
                if (gameRule == null)
                    throw new Exception("Invalid GameRule: " + mappedRuleName + " (original: " + ruleName + ")");
                world.setGameRule(gameRule, valueBool);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
