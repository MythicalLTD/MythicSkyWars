package systems.mythical.mythicskywars.commands.kits;

import systems.mythical.mythicskywars.commands.BaseCmd;
import systems.mythical.mythicskywars.utilities.Messaging;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CreateCmd extends BaseCmd {
    public CreateCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "create";
        alias = new String[]{"c"};
        argLength = 2;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        systems.mythical.mythicskywars.menus.gameoptions.objects.GameKit.newKit(player, args[1]);
        player.sendMessage(new Messaging.MessageFormatter().setVariable("kit", args[1]).format("command.kit-create"));
        return true;
    }
}
