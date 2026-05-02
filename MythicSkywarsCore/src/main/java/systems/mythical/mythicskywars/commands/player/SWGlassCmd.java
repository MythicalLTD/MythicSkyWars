package systems.mythical.mythicskywars.commands.player;

import systems.mythical.mythicskywars.commands.BaseCmd;
import systems.mythical.mythicskywars.menus.playeroptions.OptionSelectionMenu;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SWGlassCmd extends BaseCmd {
    public SWGlassCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "glass";
        alias = new String[]{"g"};
        argLength = 1;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        new OptionSelectionMenu(player, systems.mythical.mythicskywars.enums.PlayerOptions.GLASSCOLOR, true);
        return true;
    }
}
