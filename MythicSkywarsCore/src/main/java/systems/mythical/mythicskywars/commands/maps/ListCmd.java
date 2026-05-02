package systems.mythical.mythicskywars.commands.maps;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.utilities.Messaging;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class ListCmd extends systems.mythical.mythicskywars.commands.BaseCmd {
    public ListCmd(String t) {
        type = t;
        forcePlayer = false;
        cmdName = "list";
        alias = new String[]{"l"};
        argLength = 1;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        sender.sendMessage(new Messaging.MessageFormatter().format("maps.listHeader"));
        List<GameMap> maps = MythicSkywars.getGameMapMgr().getMapsCopy();
        if (maps.isEmpty()) {
            sender.sendMessage(new Messaging.MessageFormatter().format("maps.noArenas"));
            return true;
        }

        for (GameMap map : maps) {
            if (map.isRegistered()) {
                sender.sendMessage(new Messaging.MessageFormatter().setVariable("filename", map.getName()).setVariable("displayname", map.getDisplayName()).setVariable("status", ChatColor.GREEN + "REGISTERED").format("maps.listResult"));
            } else {
                sender.sendMessage(new Messaging.MessageFormatter().setVariable("filename", map.getName()).setVariable("displayname", map.getDisplayName()).setVariable("status", ChatColor.RED + "UNREGISTERED").format("maps.listResult"));
            }
        }
        return true;
    }
}
