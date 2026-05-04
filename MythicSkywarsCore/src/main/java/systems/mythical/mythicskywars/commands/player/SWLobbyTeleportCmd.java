package systems.mythical.mythicskywars.commands.player;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.commands.BaseCmd;
import systems.mythical.mythicskywars.utilities.Messaging;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SWLobbyTeleportCmd extends BaseCmd {
    public SWLobbyTeleportCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "lobby";
        alias = new String[]{"spawn"};
        argLength = 1;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        if (MythicSkywars.getCfg().getSpawn() != null) {
            player.teleport(MythicSkywars.getCfg().getSpawn());
            player.sendMessage(new Messaging.MessageFormatter().format("command.teleported-to-spawn"));
        } else {
            player.sendMessage(new Messaging.MessageFormatter().format("error.nospawn"));
        }
        return true;
    }
}
