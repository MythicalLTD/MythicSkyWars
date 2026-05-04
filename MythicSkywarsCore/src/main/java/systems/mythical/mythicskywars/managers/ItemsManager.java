package systems.mythical.mythicskywars.managers;

import com.google.common.collect.Lists;
import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.utilities.Messaging;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemsManager {
    private final Map<String, ItemStack> gameItems = new HashMap<>();

    public ItemsManager() {
        getMatchStartItems();
        getChestVoteItems();
        getHealthVoteItems();
        getTimeVoteItems();
        getWeatherVoteItems();
        getModifierVoteItems();
        getLobbyItem();
        getOptionItems();
        getSignItems();
    }

    public void addExtraItem(String materialref, List<String> lore, String message) {
        String mat = message;
        ItemStack item;
        if (isCustomHead(mat)) {
            item = createCustomHead(mat);
        } else {
            int data = -1;
            String matWithData = "";
            String[] matParts = mat.split(":");
            if (matParts.length == 2) {
                try {
                    matWithData = matParts[0];
                    data = Integer.parseInt(matParts[1]);
                } catch (NumberFormatException ignored) {
                    data = -1;
                }
            }
            if (data != -1) {
                item = MythicSkywars.getNMS().getColorItem(matWithData, (byte) data);
            } else {
                Material material = matchMaterialSafe(message);
                if (material == null) material = Material.BARRIER;
                item = new ItemStack(material, 1);
            }
        }

        ItemStack addItem = MythicSkywars.getNMS().getItemStack(item, lore, message);
        gameItems.put(materialref, addItem);
    }

    private void addItem(String materialref, List<String> lore, String message) {
        String mat = MythicSkywars.getCfg().getMaterial(materialref);
        ItemStack item;
        if (isCustomHead(mat)) {
            item = createCustomHead(mat);
        } else {
            int data = -1;
            String matWithData = "";
            String[] matParts = mat.split(":");
            if (matParts.length == 2) {
                try {
                    matWithData = matParts[0];
                    data = Integer.parseInt(matParts[1]);
                } catch (NumberFormatException ignored) {
                    data = -1;
                }
            }
            if (data != -1) {
                item = MythicSkywars.getNMS().getColorItem(matWithData, (byte) data);
            } else {
                Material material = matchMaterialSafe(MythicSkywars.getCfg().getMaterial(materialref));
                if (material == null) material = Material.BARRIER;
                item = new ItemStack(material, 1);
            }
        }

        ItemStack addItem = MythicSkywars.getNMS().getItemStack(item, lore, new Messaging.MessageFormatter().format(message));
        gameItems.put(materialref, addItem);
    }

    private void getSignItems() {
        List<String> lore = Lists.newArrayList();
        addItem("blockoffline", lore, "items.skywars-options");
        addItem("blockwaiting", lore, "items.skywars-options");
        addItem("blockplaying", lore, "items.skywars-options");
        addItem("blockending", lore, "items.skywars-options");
        addItem("almostfull", lore, "items.skywars-options");
        addItem("threefull", lore, "items.skywars-options");
        addItem("halffull", lore, "items.skywars-options");
        addItem("almostempty", lore, "items.skywars-options");
    }

    private void getLobbyItem() {
        List<String> lore = new ArrayList<>();
        lore.add(new Messaging.MessageFormatter().format("items.click-to-open"));

        addItem("optionselect", lore, "items.skywars-options");
        addItem("backlobbyitem", lore, "items.back-to-lobby-item");
        addItem("rejoinitem", lore, "items.rejoin-game-item");
        addItem("statsitem", lore, "items.stats-book-item");
        addItem("joinselect", lore, "items.joinmenu");
        addItem("spectateselect", lore, "items.spectatemenu");
        addItem("singlemenu", lore, "items.joinsingle");
        addItem("teammenu", lore, "items.jointeam");
    }

    private void getMatchStartItems() {
        List<String> lore = Lists.newArrayList();
        lore.add(new Messaging.MessageFormatter().format("items.click-to-open"));

        if (MythicSkywars.getCfg().kitVotingEnabled()) {
            addItem("kitvote", lore, "items.kit-vote-item");
        } else {
            addItem("kitvote", lore, "items.kit-select-item");
        }
        addItem("votingItem", lore, "items.voting-item");
        addItem("teamSelectItem", lore, "items.team-select-item");

        lore.clear();
        lore.add(new Messaging.MessageFormatter().format("items.lclick-to-open"));
        addItem("chestvote", lore, "items.chest-item");
        addItem("healthvote", lore, "items.health-item");
        addItem("nopermission", lore, "items.no-perm");
        addItem("timevote", lore, "items.time-item");
        addItem("weathervote", lore, "items.weather-item");
        addItem("modifiervote", lore, "items.modifier-item");

        lore.clear();
        lore.add(new Messaging.MessageFormatter().format("items.lclick-to-exit"));
        addItem("exitMenuItem", lore, "items.exit-menu-item");
        addItem("nextPageItem", lore, "items.next-page-item");
        addItem("prevPageItem", lore, "items.prev-page-item");

        lore.clear();
        lore.add(new Messaging.MessageFormatter().format("items.click-to-exit"));
        addItem("exitGameItem", lore, "items.exit-door-item");

        lore.clear();
        lore.add(new Messaging.MessageFormatter().format("items.click-to-play-again"));
        addItem("playAgainItem", lore, "items.play-again-item");
    }

    private void getChestVoteItems() {
        List<String> lore = Lists.newArrayList();
        lore.add(new Messaging.MessageFormatter().format("items.click-to-vote"));

        addItem("chestrandom", lore, "items.chest-random");
        addItem("chestbasic", lore, "items.chest-basic");
        addItem("chestnormal", lore, "items.chest-normal");
        addItem("chestop", lore, "items.chest-op");
    }

    private void getHealthVoteItems() {
        List<String> lore = new ArrayList();
        lore.add(new Messaging.MessageFormatter().format("items.click-to-vote"));

        addItem("healthrandom", lore, "items.health-random");
        addItem("healthfive", lore, "items.health-five");
        addItem("healthten", lore, "items.health-ten");
        addItem("healthfifteen", lore, "items.health-fifteen");
        addItem("healthtwenty", lore, "items.health-twenty");
    }

    private void getTimeVoteItems() {
        List<String> lore = new ArrayList();
        lore.add(new Messaging.MessageFormatter().format("items.click-to-vote"));

        addItem("timerandom", lore, "items.time-random");
        addItem("timedawn", lore, "items.time-dawn");
        addItem("timenoon", lore, "items.time-noon");
        addItem("timedusk", lore, "items.time-dusk");
        addItem("timemidnight", lore, "items.time-midnight");
    }

    private void getWeatherVoteItems() {
        List<String> lore = new ArrayList();
        lore.add(new Messaging.MessageFormatter().format("items.click-to-vote"));

        addItem("weatherrandom", lore, "items.weather-random");
        addItem("weathersunny", lore, "items.weather-sunny");
        addItem("weatherrain", lore, "items.weather-rain");
        addItem("weatherstorm", lore, "items.weather-storm");
        addItem("weathersnow", lore, "items.weather-snow");
    }

    private void getModifierVoteItems() {
        List<String> lore = new ArrayList();
        lore.add(new Messaging.MessageFormatter().format("items.click-to-vote"));

        addItem("modifierrandom", lore, "items.modifier-random");
        addItem("modifierspeed", lore, "items.modifier-speed");
        addItem("modifierjump", lore, "items.modifier-jump");
        addItem("modifierstrength", lore, "items.modifier-strength");
        addItem("modifiernone", lore, "items.modifier-none");
    }

    private void getOptionItems() {
        List<String> lore = new ArrayList();
        lore.add(new Messaging.MessageFormatter().format("items.lclick-to-open"));

        addItem("particleselect", lore, "items.particle-effect-sel");
        addItem("projectileselect", lore, "items.projectile-effect-sel");
        addItem("killsoundselect", lore, "items.killsound-sel");
        addItem("winsoundselect", lore, "items.winsound-sel");
        addItem("glassselect", lore, "items.glass-sel");
        addItem("tauntselect", lore, "items.taunt-sel");
    }

    public ItemStack getItem(String item) {
        return gameItems.get(item).clone();
    }

    /**
     * Safely match a material name, handling legacy names (e.g. WOOD_SWORD -> WOODEN_SWORD).
     * Uses Material.matchMaterial which handles legacy name resolution on supported versions.
     */
    private static Material matchMaterialSafe(String name) {
        if (name == null) return null;
        String upper = name.toUpperCase().trim();
        Material mat = Material.matchMaterial(upper);
        return mat;
    }

    /**
     * Checks if the material string represents a custom player head.
     * Supported formats:
     * <ul>
     *   <li>{@code head:<base64_texture>} - Base64 encoded skin texture</li>
     *   <li>{@code PLAYER_HEAD:<base64_texture>} - Alternative format</li>
     *   <li>{@code head:<player_name>} - Player name (fetches their skin)</li>
     * </ul>
     */
    private boolean isCustomHead(String mat) {
        if (mat == null) return false;
        String lower = mat.toLowerCase().trim();
        return lower.startsWith("head:") || lower.startsWith("player_head:");
    }

    /**
     * Creates a player head ItemStack with a custom texture.
     * Supports base64 texture values and player names.
     */
    @SuppressWarnings("deprecation")
    private ItemStack createCustomHead(String mat) {
        String value;
        if (mat.toLowerCase().startsWith("head:")) {
            value = mat.substring("head:".length()).trim();
        } else {
            // PLAYER_HEAD:value
            value = mat.substring("PLAYER_HEAD:".length()).trim();
        }

        ItemStack head = MythicSkywars.getNMS().getBlankPlayerHead();
        org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) head.getItemMeta();

        if (skullMeta == null) return head;

        // Determine if value is base64 texture or player name
        // Base64 textures are typically long strings (>32 chars) with no spaces
        if (value.length() > 32 && !value.contains(" ")) {
            // Treat as base64 texture - use reflection to set GameProfile
            try {
                java.util.UUID uuid = java.util.UUID.nameUUIDFromBytes(value.getBytes());
                Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
                Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");

                Object profile = gameProfileClass.getConstructor(java.util.UUID.class, String.class)
                        .newInstance(uuid, "custom_head");

                Object properties = gameProfileClass.getMethod("getProperties").invoke(profile);
                Object property = propertyClass.getConstructor(String.class, String.class)
                        .newInstance("textures", value);

                // PropertyMap extends ForwardingMultimap, use put(key, value)
                properties.getClass().getMethod("put", Object.class, Object.class)
                        .invoke(properties, "textures", property);

                java.lang.reflect.Field profileField = skullMeta.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
                profileField.set(skullMeta, profile);
            } catch (Exception e) {
                MythicSkywars.get().getLogger().warning("Failed to apply custom head texture: " + e.getMessage());
            }
        } else {
            // Treat as player name
            skullMeta.setOwner(value);
        }

        head.setItemMeta(skullMeta);
        return head;
    }
}
