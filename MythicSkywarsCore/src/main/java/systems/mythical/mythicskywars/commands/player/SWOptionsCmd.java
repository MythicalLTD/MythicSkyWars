package systems.mythical.mythicskywars.commands.player;

import systems.mythical.mythicskywars.menus.playeroptions.OptionsSelectionMenu;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SWOptionsCmd extends systems.mythical.mythicskywars.commands.BaseCmd {
    public SWOptionsCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "options";
        alias = new String[]{"o"};
        argLength = 1;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        new OptionsSelectionMenu(player);
        return true;
    }
}
