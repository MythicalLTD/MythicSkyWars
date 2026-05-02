package systems.mythical.mythicskywars.commands.admin;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.utilities.Messaging;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HoloRemoveCmd extends systems.mythical.mythicskywars.commands.BaseCmd {
    public HoloRemoveCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "holoremove";
        alias = new String[]{"hr", "removeholo", "removehologram", "hologramremove"};
        argLength = 1;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        if (MythicSkywars.getCfg().hologramsEnabled() && MythicSkywars.getHoloManager() != null) {
            boolean result = MythicSkywars.getHoloManager().removeHologram(player.getLocation());
            if (result) {
                player.sendMessage(new Messaging.MessageFormatter().format("command.hologram-removed"));
                return true;
            }
            player.sendMessage(new Messaging.MessageFormatter().format("error.no-holograms-found"));
            return true;
        }
        player.sendMessage(new Messaging.MessageFormatter().format("error.holograms-not-enabled"));
        return true;
    }
}
