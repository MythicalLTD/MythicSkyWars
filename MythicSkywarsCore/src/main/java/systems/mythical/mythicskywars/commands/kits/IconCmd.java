package systems.mythical.mythicskywars.commands.kits;

import systems.mythical.mythicskywars.menus.gameoptions.objects.GameKit;
import systems.mythical.mythicskywars.utilities.Messaging;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class IconCmd extends systems.mythical.mythicskywars.commands.BaseCmd {
    public IconCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "icon";
        alias = new String[]{"i"};
        argLength = 2;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        GameKit kit = GameKit.getKit(args[1]);
        if (kit == null) {
            player.sendMessage(new Messaging.MessageFormatter().setVariable("kit", args[1]).format("command.no-kit"));
            return true;
        }
        kit.setIcon(systems.mythical.mythicskywars.MythicSkywars.getNMS().getMainHandItem(player).clone());

        GameKit.saveKit(kit);

        player.sendMessage(new Messaging.MessageFormatter().setVariable("icon", kit.getIcon().getType().toString()).format("command.kit-icon"));
        return true;
    }
}
