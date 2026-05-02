package systems.mythical.mythicskywars.commands.admin;

import systems.mythical.mythicskywars.commands.BaseCmd;
import systems.mythical.mythicskywars.managers.MatchManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StartCmd extends BaseCmd {
    public StartCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "start";
        alias = new String[]{"s"};
        argLength = 1;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        systems.mythical.mythicskywars.game.GameMap gMap = MatchManager.get().getPlayerMap(player);
        if (gMap != null) {
            MatchManager.get().forceStart(player);
        }
        return true;
    }
}
