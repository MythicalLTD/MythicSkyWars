package systems.mythical.mythicskywars.commands.kits;

import systems.mythical.mythicskywars.utilities.Messaging;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LoadCmd extends systems.mythical.mythicskywars.commands.BaseCmd {
    public LoadCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "load";
        alias = new String[]{"lo"};
        argLength = 2;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        systems.mythical.mythicskywars.menus.gameoptions.objects.GameKit kit = systems.mythical.mythicskywars.menus.gameoptions.objects.GameKit.getKit(args[1]);
        if (kit == null) {
            player.sendMessage(new Messaging.MessageFormatter().setVariable("kit", args[1]).format("command.no-kit"));
            return true;
        }
        systems.mythical.mythicskywars.menus.gameoptions.objects.GameKit.giveKit(player, kit);
        player.sendMessage(new Messaging.MessageFormatter().setVariable("kit", args[1]).format("command.kit-load"));
        player.sendMessage(new Messaging.MessageFormatter().format("command.kit-loadmsg"));
        return true;
    }
}
