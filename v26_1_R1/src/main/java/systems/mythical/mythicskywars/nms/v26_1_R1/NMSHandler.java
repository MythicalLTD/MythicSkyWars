package systems.mythical.mythicskywars.nms.v26_1_R1;

import org.bukkit.GameRule;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class NMSHandler extends systems.mythical.mythicskywars.nms.v1_21_R1.NMSHandler {

    // Map old game rule names to new Minecraft 1.21.11+ registry key names (snake_case, no namespace prefix)
    private static final Map<String, String> GAME_RULE_MAPPINGS = new HashMap<>();
    
    static {
        GAME_RULE_MAPPINGS.put("doMobSpawning", "spawning_mobs");
        GAME_RULE_MAPPINGS.put("mobGriefing", "mob_griefing");
        GAME_RULE_MAPPINGS.put("doFireTick", "fire_spread_radius_around_player");
        GAME_RULE_MAPPINGS.put("showDeathMessages", "show_death_messages");
        GAME_RULE_MAPPINGS.put("announceAdvancements", "show_advancement_messages");
        GAME_RULE_MAPPINGS.put("doDaylightCycle", "advance_time");
        GAME_RULE_MAPPINGS.put("doWeatherCycle", "advance_weather");
        GAME_RULE_MAPPINGS.put("keepInventory", "keep_inventory");
        GAME_RULE_MAPPINGS.put("naturalRegeneration", "natural_health_regeneration");
        GAME_RULE_MAPPINGS.put("doTileDrops", "tile_drops");
        GAME_RULE_MAPPINGS.put("randomTickSpeed", "random_tick_speed");
    }

    @Override
    public int getVersion() {
        return 26;
    }

    @Override
    public void setGameRule(World world, String ruleName, String value) {
        // Map old game rule names to new ones for Minecraft 1.21.11+
        String mappedRuleName = GAME_RULE_MAPPINGS.getOrDefault(ruleName, ruleName);
        
        // Special handling for doFireTick: was Boolean, now maps to Integer rule
        // fire_spread_radius_around_player: 0 = disabled, -1 = unlimited spread
        if (ruleName.equals("doFireTick")) {
            try {
                GameRule<?> gameRule = Registry.GAME_RULE.get(NamespacedKey.minecraft("fire_spread_radius_around_player"));
                if (gameRule != null) {
                    @SuppressWarnings("unchecked")
                    GameRule<Integer> intRule = (GameRule<Integer>) gameRule;
                    int fireValue = value.equalsIgnoreCase("true") ? -1 : 0;
                    world.setGameRule(intRule, fireValue);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return;
        }
        
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
        
        // Apply using Registry API (GameRule is now an interface in 1.21.11+)
        try {
            // Normalize the rule name to a valid NamespacedKey
            String keyName = mappedRuleName.toLowerCase(Locale.ROOT);
            // Strip minecraft: prefix if present
            if (keyName.startsWith("minecraft:")) {
                keyName = keyName.substring("minecraft:".length());
            }
            // Convert camelCase to snake_case for legacy names
            keyName = camelToSnake(keyName);
            
            GameRule<?> gameRule = Registry.GAME_RULE.get(NamespacedKey.minecraft(keyName));
            if (gameRule == null) {
                // Try the original name as-is (already snake_case)
                gameRule = Registry.GAME_RULE.get(NamespacedKey.minecraft(mappedRuleName.toLowerCase(Locale.ROOT)));
            }
            
            if (gameRule == null) {
                throw new Exception("Invalid GameRule: " + mappedRuleName + " (original: " + ruleName + ", resolved key: " + keyName + ")");
            }
            
            if (valueBool == null) {
                if (valueInt == null)
                    throw new Exception("Invalid GameRule value provided: " + mappedRuleName + " (original: " + ruleName + ") -> " + value);
                @SuppressWarnings("unchecked")
                GameRule<Integer> intRule = (GameRule<Integer>) gameRule;
                world.setGameRule(intRule, valueInt);
            } else {
                @SuppressWarnings("unchecked")
                GameRule<Boolean> boolRule = (GameRule<Boolean>) gameRule;
                world.setGameRule(boolRule, valueBool);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private static String camelToSnake(String camel) {
        if (camel == null || camel.isEmpty()) return camel;
        // If it already contains underscores, assume it's already snake_case
        if (camel.contains("_")) return camel;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) sb.append('_');
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
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
