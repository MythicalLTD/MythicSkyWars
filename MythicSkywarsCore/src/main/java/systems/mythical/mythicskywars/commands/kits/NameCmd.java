package systems.mythical.mythicskywars.commands.kits;

import systems.mythical.mythicskywars.menus.gameoptions.objects.GameKit;
import systems.mythical.mythicskywars.utilities.Messaging;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class NameCmd extends systems.mythical.mythicskywars.commands.BaseCmd {
    public NameCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "name";
        alias = new String[]{"n"};
        argLength = 3;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        GameKit kit = GameKit.getKit(args[1]);
        if (kit == null) {
            player.sendMessage(new Messaging.MessageFormatter().setVariable("kit", args[1]).format("command.no-kit"));
            return true;
        }

        StringBuilder message = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            message.append(args[i]);
            message.append(" ");
        }

        kit.setName(message.toString().trim());

        GameKit.saveKit(kit);

        player.sendMessage(new Messaging.MessageFormatter().setVariable("kit", kit.getColorName()).format("command.kit-name"));
        return true;
    }
}
