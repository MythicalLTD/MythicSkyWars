package systems.mythical.mythicskywars.commands.player;

import systems.mythical.mythicskywars.commands.BaseCmd;
import systems.mythical.mythicskywars.managers.LobbyBypassManager;
import systems.mythical.mythicskywars.utilities.Messaging;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SWBypassLobbySlotsCmd extends BaseCmd {
    public SWBypassLobbySlotsCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "bypasslobbyslots";
        alias = new String[]{"bypassLobbySlots"};
        argLength = 1;
    }

    @Override
    public boolean run(CommandSender sender, Player player, String[] args) {
        boolean enabled = LobbyBypassManager.toggle(player);
        if (enabled) {
            player.sendMessage(new Messaging.MessageFormatter().format("command.bypass-lobby-slots-enabled"));
        } else {
            player.sendMessage(new Messaging.MessageFormatter().format("command.bypass-lobby-slots-disabled"));
        }
        return true;
    }
}
