package systems.mythical.mythicskywars.menus;

import com.google.common.collect.Lists;
import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.enums.ChestPlacementType;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.game.TeamCard;
import systems.mythical.mythicskywars.menus.gameoptions.objects.CoordLoc;
import systems.mythical.mythicskywars.utilities.Messaging;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public final class ArenaSetupMenu {
    private ArenaSetupMenu() {
    }

    private static final String ITEM_NAME = ChatColor.LIGHT_PURPLE + "Arena Setup Tool";
    private static final String TEAM_SPAWNER_ITEM_NAME = ChatColor.GOLD + "Team Spawner Tool";
    private static final String DEATHMATCH_SPAWNER_ITEM_NAME = ChatColor.GREEN + "Deathmatch Spawner Tool";

    public static ItemStack createToolItem() {
        return MythicSkywars.getNMS().getItemStack(
                new ItemStack(Material.BLAZE_ROD, 1),
                Lists.newArrayList(
                        ChatColor.GRAY + "Right click to open arena setup",
                        ChatColor.GRAY + "Right click a chest to toggle type"
                ),
                ITEM_NAME
        );
    }

    public static ItemStack createTeamSpawnerItem() {
        return MythicSkywars.getNMS().getItemStack(
                new ItemStack(Material.NETHER_STAR, 1),
                Lists.newArrayList(
                        ChatColor.GRAY + "Right click to teleport up and set team spawn",
                        ChatColor.GRAY + "Checks 6 blocks above for free 3x3 space"
                ),
                TEAM_SPAWNER_ITEM_NAME
        );
    }

    public static ItemStack createDeathmatchSpawnerItem() {
        return MythicSkywars.getNMS().getItemStack(
                new ItemStack(Material.EMERALD, 1),
                Lists.newArrayList(
                        ChatColor.GRAY + "Right click to set deathmatch spawn",
                        ChatColor.GRAY + "Places an emerald block at your location"
                ),
                DEATHMATCH_SPAWNER_ITEM_NAME
        );
    }

    public static boolean isTool(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        return stack.isSimilar(createToolItem()) || stack.isSimilar(createTeamSpawnerItem()) || stack.isSimilar(createDeathmatchSpawnerItem());
    }

    public static boolean isTeamSpawnerTool(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        return stack.isSimilar(createTeamSpawnerItem());
    }

    public static boolean isDeathmatchSpawnerTool(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        return stack.isSimilar(createDeathmatchSpawnerItem());
    }

    public static void giveTool(Player player) {
        player.getInventory().setItem(0, createToolItem());
    }

    public static void open(Player player, GameMap gMap) {
        Inventory inv = Bukkit.createInventory(null, 45, new Messaging.MessageFormatter().setVariable("map", gMap.getName()).format("menu.arena-setup-title"));
        inv.setItem(10, MythicSkywars.getNMS().getItemStack(new ItemStack(Material.DIAMOND_BLOCK), Lists.newArrayList(msg("maps.editor.item.add-player-spawn.lore")), msg("maps.editor.item.add-player-spawn.name")));
        inv.setItem(11, MythicSkywars.getNMS().getItemStack(new ItemStack(MythicSkywars.getNMS().getMaterial("ENDER_CHEST")), Lists.newArrayList(
                new Messaging.MessageFormatter().setVariable("mode", getChestModeName(gMap)).format("maps.editor.item.chest-mode.lore-current"),
                msg("maps.editor.item.chest-mode.lore-toggle")
        ), msg("maps.editor.item.chest-mode.name")));
        inv.setItem(12, MythicSkywars.getNMS().getItemStack(new ItemStack(Material.EMERALD_BLOCK), Lists.newArrayList(msg("maps.editor.item.add-deathmatch-spawn.lore")), msg("maps.editor.item.add-deathmatch-spawn.name")));
        inv.setItem(13, MythicSkywars.getNMS().getItemStack(new ItemStack(MythicSkywars.getNMS().getMaterial("EYE_OF_ENDER")), Lists.newArrayList(msg("maps.editor.item.set-look-direction.lore")), msg("maps.editor.item.set-look-direction.name")));
        inv.setItem(14, MythicSkywars.getNMS().getItemStack(new ItemStack(Material.ENDER_PEARL), Lists.newArrayList(msg("maps.editor.item.set-spectate-spawn.lore")), msg("maps.editor.item.set-spectate-spawn.name")));
        inv.setItem(15, MythicSkywars.getNMS().getItemStack(new ItemStack(Material.FLINT_AND_STEEL), Lists.newArrayList(
                new Messaging.MessageFormatter().setVariable("state", gMap.allowFriendlyFire() ? msg("maps.editor.state.enabled") : msg("maps.editor.state.disabled")).format("maps.editor.item.friendly-fire.lore-current"),
                msg("maps.editor.item.friendly-fire.lore-toggle")
        ), msg("maps.editor.item.friendly-fire.name")));
        inv.setItem(16, MythicSkywars.getNMS().getItemStack(new ItemStack(MythicSkywars.getNMS().getMaterial("ENDER_PORTAL_FRAME")), Lists.newArrayList(msg("maps.editor.item.set-waiting-spawn.lore")), msg("maps.editor.item.set-waiting-spawn.name")));

        inv.setItem(19, MythicSkywars.getNMS().getItemStack(new ItemStack(Material.NETHER_STAR), Lists.newArrayList(
                ChatColor.GRAY + "Get the Team Spawner tool",
                ChatColor.GRAY + "Right click with it to auto-set spawns"
        ), ChatColor.GOLD + "Get Team Spawner Tool"));

        inv.setItem(25, MythicSkywars.getNMS().getItemStack(new ItemStack(Material.EMERALD), Lists.newArrayList(
                ChatColor.GRAY + "Get the Deathmatch Spawner tool",
                ChatColor.GRAY + "Right click to set deathmatch spawns"
        ), ChatColor.GREEN + "Get Deathmatch Spawner Tool"));

        inv.setItem(20, MythicSkywars.getNMS().getItemStack(new ItemStack(MythicSkywars.getNMS().getMaterial("REDSTONE_COMPARATOR")), Lists.newArrayList(
                new Messaging.MessageFormatter().setVariable("size", String.valueOf(gMap.getTeamSize())).format("maps.editor.item.team-size.lore-current"),
                msg("maps.editor.item.team-size.lore-left"),
                msg("maps.editor.item.team-size.lore-right")
        ), msg("maps.editor.item.team-size.name")));
        inv.setItem(21, MythicSkywars.getNMS().getItemStack(new ItemStack(Material.DIAMOND_HELMET), Lists.newArrayList(
                new Messaging.MessageFormatter().setVariable("count", String.valueOf(gMap.getMinTeams())).format("maps.editor.item.min-players.lore-current"),
                msg("maps.editor.item.min-players.lore-left"),
                msg("maps.editor.item.min-players.lore-right")
        ), msg("maps.editor.item.min-players.name")));
        inv.setItem(22, MythicSkywars.getNMS().getItemStack(new ItemStack(Material.CHEST), Lists.newArrayList(msg("maps.editor.item.normalize-chests.lore")), msg("maps.editor.item.normalize-chests.name")));
        inv.setItem(23, MythicSkywars.getNMS().getItemStack(new ItemStack(Material.MAP), Lists.newArrayList(msg("maps.editor.item.open-arena-manager.lore")), msg("maps.editor.item.open-arena-manager.name")));
        inv.setItem(24, MythicSkywars.getNMS().getItemStack(new ItemStack(Material.SHEARS), Lists.newArrayList(msg("maps.editor.item.remove-deathmatch-spawn.lore")), msg("maps.editor.item.remove-deathmatch-spawn.name")));

        inv.setItem(30, MythicSkywars.getNMS().getItemStack(new ItemStack(Material.NAME_TAG), Lists.newArrayList(
                ChatColor.GRAY + "Current: " + ChatColor.WHITE + gMap.getDisplayName(),
                ChatColor.GRAY + "Click to rename arena"
        ), ChatColor.YELLOW + "Rename Arena"));
        inv.setItem(31, MythicSkywars.getNMS().getItemStack(MythicSkywars.getNMS().getBlankPlayerHead(), Lists.newArrayList(
                ChatColor.GRAY + "Current: " + ChatColor.WHITE + (gMap.getDesigner() == null || gMap.getDesigner().isEmpty() ? "Not set" : gMap.getDesigner()),
                ChatColor.GRAY + "Left click to type a creator name",
                ChatColor.GRAY + "Right click to set to yourself"
        ), ChatColor.YELLOW + "Set Creator"));
        inv.setItem(32, MythicSkywars.getNMS().getItemStack(new ItemStack(Material.ITEM_FRAME), Lists.newArrayList(
                ChatColor.GRAY + "Click to set the item in your",
                ChatColor.GRAY + "hand as the map icon"
        ), ChatColor.YELLOW + "Set Map Icon"));

        inv.setItem(39, MythicSkywars.getNMS().getItemStack(new ItemStack(Material.BOOK), Lists.newArrayList(msg("maps.editor.item.save-exit.lore")), msg("maps.editor.item.save-exit.name")));
        inv.setItem(41, MythicSkywars.getNMS().getItemStack(new ItemStack(Material.BARRIER), Lists.newArrayList(msg("maps.editor.item.exit-no-save.lore")), msg("maps.editor.item.exit-no-save.name")));
        player.openInventory(inv);
    }

    public static boolean handleClick(Player player, GameMap gMap, String title, int slot, ClickType clickType) {
        if (title == null || !isArenaSetupMenuTitle(title)) {
            return false;
        }
        if (gMap == null || !gMap.isEditing()) {
            player.closeInventory();
            return true;
        }
        if (slot == 39) {
            gMap.exitEditMode(player, true);
            player.closeInventory();
            return true;
        }
        if (slot == 41) {
            gMap.exitEditMode(player, false);
            player.closeInventory();
            return true;
        }

        switch (slot) {
            case 19:
                player.getInventory().addItem(createTeamSpawnerItem());
                player.sendMessage(ChatColor.GREEN + "Team Spawner tool given! Right click to teleport up and set team spawn.");
                player.closeInventory();
                return true;
            case 25:
                player.getInventory().addItem(createDeathmatchSpawnerItem());
                player.sendMessage(ChatColor.GREEN + "Deathmatch Spawner tool given! Right click to set deathmatch spawn.");
                player.closeInventory();
                return true;
            case 10:
                if (clickType == ClickType.RIGHT) {
                    removeTeamSpawnsAtLocation(player, gMap);
                    return true;
                }
                if (gMap.getTeamSize() == 1 || !MythicSkywars.getCfg().isUseSeparateCages()) {
                    int teamIndex = gMap.getTeamCards().size();
                    TeamCard team = gMap.getTeamCardByIndex(teamIndex);
                    if (team == null) {
                        gMap.addTeamCard(Lists.newArrayList(new CoordLoc(player.getLocation())));
                    } else {
                        gMap.addSpawnLocationForTeam(team, player.getLocation());
                    }
                    player.getLocation().getBlock().setType(Material.DIAMOND_BLOCK);
                    player.sendMessage(new Messaging.MessageFormatter().setVariable("mapname", gMap.getDisplayName()).setVariable("num", "" + gMap.getMaxPlayers()).format("maps.addSpawn"));
                } else {
                    int teamIndex = gMap.getTeamCards().size() + 1;
                    gMap.addTeamCard(Lists.newArrayList(new CoordLoc(player.getLocation())));
                    player.getLocation().getBlock().setType(Material.DIAMOND_BLOCK);
                    player.sendMessage(new Messaging.MessageFormatter().setVariable("team", String.valueOf(teamIndex)).format("maps.editor.team-spawn-added"));
                }
                return true;
            case 11:
                if (gMap.getChestPlacementType() == ChestPlacementType.NORMAL) {
                    gMap.setChestPlacementType(ChestPlacementType.CENTER);
                } else {
                    gMap.setChestPlacementType(ChestPlacementType.NORMAL);
                }
                player.sendMessage(new Messaging.MessageFormatter().setVariable("mode", getChestModeName(gMap)).format("maps.editor.chest-mode-set"));
                reopen(player, gMap);
                return true;
            case 12:
                gMap.addDeathMatchSpawn(player.getLocation());
                player.getLocation().getBlock().setType(Material.EMERALD_BLOCK);
                player.sendMessage(new Messaging.MessageFormatter()
                        .setVariable("num", "" + gMap.getDeathMatchSpawns().size())
                        .setVariable("mapname", gMap.getDisplayName())
                        .format("maps.addDeathSpawn"));
                return true;
            case 13:
                gMap.setLookDirection(player.getLocation());
                player.sendMessage(new Messaging.MessageFormatter().setVariable("mapname", gMap.getDisplayName()).format("maps.setLookDirection"));
                return true;
            case 14:
                if (clickType == ClickType.RIGHT) {
                    gMap.clearSpectateSpawn();
                    player.sendMessage(msg("maps.editor.spectate-spawn-cleared"));
                } else {
                    gMap.setSpectateSpawn(player.getLocation());
                    player.sendMessage(new Messaging.MessageFormatter().setVariable("mapname", gMap.getDisplayName()).format("maps.specSpawn"));
                }
                return true;
            case 15:
                gMap.setFriendlyFire(!gMap.allowFriendlyFire());
                player.sendMessage(new Messaging.MessageFormatter().setVariable("state", gMap.allowFriendlyFire() ? msg("maps.editor.state.enabled") : msg("maps.editor.state.disabled")).format("maps.editor.friendly-fire-state"));
                reopen(player, gMap);
                return true;
            case 16:
                if (gMap.getTeamSize() <= 1) {
                    player.sendMessage(msg("maps.editor.waiting-spawn-disabled-solo"));
                    return true;
                }
                if (clickType == ClickType.RIGHT) {
                    gMap.clearWaitingLobbySpawn();
                    player.sendMessage(msg("maps.editor.waiting-spawn-cleared"));
                } else {
                    gMap.setWaitingLobbySpawn(player.getLocation());
                    player.sendMessage(new Messaging.MessageFormatter().setVariable("mapname", gMap.getDisplayName()).format("maps.waitingLobbySpawn"));
                }
                return true;
            case 20:
                if (clickType == ClickType.RIGHT) {
                    if (gMap.getTeamSize() > 1) {
                        gMap.setTeamSize(gMap.getTeamSize() - 1);
                    }
                } else {
                    if (gMap.getTeamSize() < 8) {
                        gMap.setTeamSize(gMap.getTeamSize() + 1);
                    }
                }
                int adjustedMaxPlayers = gMap.getMaxPlayers();
                if (adjustedMaxPlayers >= 2 && gMap.getMinTeams() > adjustedMaxPlayers) {
                    gMap.setMinTeams(adjustedMaxPlayers);
                }
                reopen(player, gMap);
                return true;
            case 21:
                int maxPlayers = gMap.getMaxPlayers();
                if (maxPlayers < 2) {
                    player.sendMessage(msg("maps.editor.min-players-unavailable"));
                    return true;
                }
                if (clickType == ClickType.RIGHT) {
                    if (gMap.getMinTeams() > 2) {
                        gMap.setMinTeams(gMap.getMinTeams() - 1);
                    }
                } else {
                    if (gMap.getMinTeams() < maxPlayers) {
                        gMap.setMinTeams(gMap.getMinTeams() + 1);
                    }
                }
                reopen(player, gMap);
                return true;
            case 22:
                int converted = gMap.registerAllMapChestsAsNormal();
                player.sendMessage(new Messaging.MessageFormatter()
                        .setVariable("count", String.valueOf(converted))
                        .format("maps.editor.all-chests-normalized"));
                reopen(player, gMap);
                return true;
            case 23:
                MythicSkywars.getIC().show(player, gMap.getArenaKey());
                return true;
            case 24:
                if (gMap.removeDeathMatchSpawn(player.getLocation())) {
                    player.sendMessage(new Messaging.MessageFormatter()
                            .setVariable("num", "" + (gMap.getDeathMatchSpawns().size() + 1))
                            .setVariable("mapname", gMap.getDisplayName())
                            .format("maps.deathSpawnRemoved"));
                } else {
                    player.sendMessage(msg("maps.editor.no-deathmatch-spawn-at-location"));
                }
                return true;
            case 30:
                // Rename arena - prompt via chat using existing ChatListener system
                player.closeInventory();
                player.sendMessage(ChatColor.GREEN + "Type the new arena display name in chat. (20 second timeout)");
                systems.mythical.mythicskywars.listeners.ChatListener.setTime(player.getUniqueId(), System.currentTimeMillis());
                systems.mythical.mythicskywars.listeners.ChatListener.setSetting(player.getUniqueId(), gMap.getName() + ":display");
                return true;
            case 31:
                // Set creator - left click to type, right click to set to yourself
                if (clickType == ClickType.RIGHT) {
                    gMap.setCreator(player.getName());
                    player.sendMessage(ChatColor.GREEN + "Arena creator set to: " + ChatColor.GOLD + player.getName());
                    reopen(player, gMap);
                } else {
                    player.closeInventory();
                    player.sendMessage(ChatColor.GREEN + "Type the creator name in chat. (20 second timeout)");
                    systems.mythical.mythicskywars.listeners.ChatListener.setTime(player.getUniqueId(), System.currentTimeMillis());
                    systems.mythical.mythicskywars.listeners.ChatListener.setSetting(player.getUniqueId(), gMap.getName() + ":creator");
                }
                return true;
            case 32:
                // Set map icon to item in hand
                ItemStack handItem = player.getInventory().getItemInMainHand();
                if (handItem == null || handItem.getType() == Material.AIR) {
                    player.sendMessage(ChatColor.RED + "Hold an item in your main hand to set it as the map icon!");
                } else {
                    setMapIcon(gMap, handItem.getType());
                    player.sendMessage(ChatColor.GREEN + "Map icon set to: " + ChatColor.GOLD + handItem.getType().name());
                }
                reopen(player, gMap);
                return true;
            default:
                return true;
        }
    }

    private static String getChestModeName(GameMap gMap) {
        if (gMap.getChestPlacementType() == ChestPlacementType.CENTER) {
            return msg("maps.editor.chest-mode-center");
        }
        return msg("maps.editor.chest-mode-normal");
    }

    private static String msg(String key) {
        return new Messaging.MessageFormatter().format(key);
    }

    private static void removeTeamSpawnsAtLocation(Player player, GameMap gMap) {
        Map<TeamCard, List<Integer>> removed = gMap.removeSpawnsAtLocation(player.getLocation());
        if (removed.isEmpty()) {
            player.sendMessage(msg("maps.editor.no-spawn-at-location"));
            return;
        }
        for (Map.Entry<TeamCard, List<Integer>> removedTeamLocs : removed.entrySet()) {
            int teamCardPos = gMap.getTeamCardPosition(removedTeamLocs.getKey());
            if (teamCardPos == -1) {
                teamCardPos = gMap.getTeamCards().size();
            }
            for (Integer spawnIndex : removedTeamLocs.getValue()) {
                player.sendMessage(new Messaging.MessageFormatter()
                        .setVariable("num", "" + (spawnIndex + 1))
                        .setVariable("team", "" + (teamCardPos + 1))
                        .setVariable("mapname", gMap.getDisplayName())
                        .format("maps.spawnRemoved"));
            }
        }
    }

    public static boolean isArenaSetupMenu(String title) {
        return isArenaSetupMenuTitle(title);
    }

    /**
     * Reopens the menu on the next tick to avoid Bukkit inventory update issues.
     */
    private static void reopen(Player player, GameMap gMap) {
        Bukkit.getScheduler().runTask(MythicSkywars.get(), () -> open(player, gMap));
    }

    private static boolean isArenaSetupMenuTitle(String title) {
        String strippedTitle = ChatColor.stripColor(title);
        String expectedPrefix = ChatColor.stripColor(new Messaging.MessageFormatter().setVariable("map", "").format("menu.arena-setup-title"));
        return strippedTitle != null && expectedPrefix != null && strippedTitle.startsWith(expectedPrefix);
    }

    private static void setMapIcon(GameMap gMap, Material material) {
        java.io.File dataDirectory = new java.io.File(MythicSkywars.get().getDataFolder(), "mapsData");
        java.io.File mapFile = new java.io.File(dataDirectory, gMap.getName() + ".yml");
        if (mapFile.exists()) {
            org.bukkit.configuration.file.FileConfiguration fc = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(mapFile);
            fc.set("enableCustomJoinMenuItem", true);
            fc.set("customJoinMenuItem", material.name());
            try {
                fc.save(mapFile);
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
    }
}
