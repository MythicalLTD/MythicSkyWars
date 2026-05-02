package systems.mythical.mythicskywars.commands.player;

import systems.mythical.mythicskywars.menus.playeroptions.PrestigeMenu;
import systems.mythical.mythicskywars.utilities.Messaging;
import systems.mythical.mythicskywars.utilities.PrestigeManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SWPrestigeCmd extends systems.mythical.mythicskywars.commands.BaseCmd {

    public SWPrestigeCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "prestige";
        argLength = 1;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        if (!PrestigeManager.get().isEnabled()) {
            player.sendMessage(new Messaging.MessageFormatter().format("levels.prestige-disabled"));
            return true;
        }
        PrestigeMenu.open(player);
        return true;
    }
}
