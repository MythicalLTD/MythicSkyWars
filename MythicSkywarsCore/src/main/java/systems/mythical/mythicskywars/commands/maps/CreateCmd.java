package systems.mythical.mythicskywars.commands.maps;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.menus.ArenaSetupMenu;
import systems.mythical.mythicskywars.utilities.Messaging;
import systems.mythical.mythicskywars.utilities.Util;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

public class CreateCmd extends systems.mythical.mythicskywars.commands.BaseCmd {
    public CreateCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "create";
        alias = new String[]{"c"};
        argLength = 2;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        if (MythicSkywars.getCfg().getSpawn() != null) {
            String worldName = args[1];
            Environment environment = Environment.NORMAL;
            if (args.length > 2) {
                if (args[2].equalsIgnoreCase("the_end")) {
                    environment = Environment.THE_END;
                } else if (args[2].equalsIgnoreCase("nether")) {
                    environment = Environment.NETHER;
                }
            }

            GameMap gMap = MythicSkywars.getGameMapMgr().getMap(worldName);
            if (gMap != null) {
                player.sendMessage(new Messaging.MessageFormatter().format("error.map-exists"));
                return true;
            }
            GameMap.GameMapCreationResult result = MythicSkywars.getGameMapMgr().createNewMap(worldName, environment);
            // Sanity check for the map name
            if (!result.isValidName()) {
                player.sendMessage(new Messaging.MessageFormatter().format("error.map-id-invalid"));
                return true;
            }
            // Sanity check for the world creation
            World resultWorld = result.getWorld();
            if (resultWorld == null) {
                player.sendMessage(new Messaging.MessageFormatter().format("error.map-world-exists"));
                return true;
            }
            player.sendMessage(new Messaging.MessageFormatter().setVariable("mapname", worldName).format("maps.created"));
            gMap = MythicSkywars.getGameMapMgr().getMap(worldName);
            if (gMap != null) {
                gMap.setEditing(true);
                resultWorld.setAutoSave(true);

                // Disable mob spawning and clear all existing entities
                MythicSkywars.getNMS().setGameRule(resultWorld, "doMobSpawning", "false");
                for (Entity entity : resultWorld.getEntities()) {
                    if (!(entity instanceof Player)) {
                        entity.remove();
                    }
                }

                resultWorld.getBlockAt(0, 75, 0).setType(Material.STONE);
                Util.get().clear(player);
                player.setGameMode(GameMode.CREATIVE);
                player.teleport(new org.bukkit.Location(resultWorld, 0.0D, 76.0D, 0.0D), PlayerTeleportEvent.TeleportCause.PLUGIN);
                player.setAllowFlight(true);
                player.setFlying(true);
                ArenaSetupMenu.giveTool(player);
                player.sendMessage(new Messaging.MessageFormatter().format("maps.editor.tool-given"));
            }
            return true;
        }

        sender.sendMessage(new Messaging.MessageFormatter().format("error.nospawn"));
        return true;
    }
}
