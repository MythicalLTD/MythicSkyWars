package systems.mythical.mythicskywars.commands.maps;
import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.menus.gameoptions.objects.CoordLoc;
import systems.mythical.mythicskywars.utilities.Messaging;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CheckChestTypeCmd extends systems.mythical.mythicskywars.commands.BaseCmd {
    public CheckChestTypeCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "checkchest";
        alias = new String[]{"checkchesttype"};
        argLength = 2;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        String worldName = args[1];
        GameMap map = MythicSkywars.getGameMapMgr().getMap(worldName);

        if (map == null) {
            sender.sendMessage(new Messaging.MessageFormatter().format("error.map-does-not-exist"));
            return true;
        }

        Block block = player.getTargetBlock(null, 5);
        if (block == null || (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST)) {
            // error
            sender.sendMessage(new Messaging.MessageFormatter().format("error.not-looking-at-chest"));
            return true;
        }

        CoordLoc loc = new CoordLoc(block.getLocation().getBlockX(), block.getLocation().getBlockY(), block.getLocation().getBlockZ());

        if (map.getCenterChests().contains(loc)) {
            player.sendMessage(new Messaging.MessageFormatter().format("maps.chest-type-center"));
            return true;
        } else if (map.getChests().contains(loc)) {
            player.sendMessage(new Messaging.MessageFormatter().format("maps.chest-type-normal"));
            return true;
        }
        player.sendMessage(new Messaging.MessageFormatter().format("maps.chest-type-unregistered"));
        return true;
    }
}
