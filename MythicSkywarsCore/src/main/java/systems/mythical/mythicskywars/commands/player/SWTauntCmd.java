package systems.mythical.mythicskywars.commands.player;

import systems.mythical.mythicskywars.commands.BaseCmd;
import systems.mythical.mythicskywars.menus.playeroptions.OptionSelectionMenu;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SWTauntCmd extends BaseCmd {
    public SWTauntCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "taunt";
        alias = new String[]{"t"};
        argLength = 1;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        new OptionSelectionMenu(player, systems.mythical.mythicskywars.enums.PlayerOptions.TAUNT, true);
        return true;
    }
}
