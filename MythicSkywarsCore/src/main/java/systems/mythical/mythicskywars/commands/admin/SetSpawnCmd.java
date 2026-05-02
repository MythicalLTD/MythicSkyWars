package systems.mythical.mythicskywars.commands.admin;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.utilities.Messaging;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSpawnCmd extends systems.mythical.mythicskywars.commands.BaseCmd {
    public SetSpawnCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "setspawn";
        alias = new String[]{"sspawn"};
        argLength = 1;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        org.bukkit.Location spawn = player.getLocation();
        MythicSkywars.getCfg().setSpawn(spawn);
        MythicSkywars.getCfg().save();
        player.sendMessage(new Messaging.MessageFormatter().format("command.spawnset"));
        return true;
    }
}
