package systems.mythical.mythicskywars.commands.player;

import systems.mythical.mythicskywars.commands.BaseCmd;
import systems.mythical.mythicskywars.menus.playeroptions.OptionSelectionMenu;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SWKillsoundCmd extends BaseCmd {
    public SWKillsoundCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "killsound";
        alias = new String[]{"ks"};
        argLength = 1;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        new OptionSelectionMenu(player, systems.mythical.mythicskywars.enums.PlayerOptions.KILLSOUND, true);
        return true;
    }
}
