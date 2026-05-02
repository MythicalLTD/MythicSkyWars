package systems.mythical.mythicskywars.commands.player;

import systems.mythical.mythicskywars.perks.PerkManager;
import systems.mythical.mythicskywars.perks.PerksMenu;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SWPerksCmd extends systems.mythical.mythicskywars.commands.BaseCmd {

    public SWPerksCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "perks";
        argLength = 1;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        if (!PerkManager.get().isEnabled()) {
            player.sendMessage("§cPerks are currently disabled.");
            return true;
        }
        PerksMenu.open(player);
        return true;
    }
}
