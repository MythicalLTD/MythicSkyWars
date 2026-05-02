package systems.mythical.mythicskywars.commands.party;

import systems.mythical.mythicskywars.utilities.Messaging;
import systems.mythical.mythicskywars.utilities.Party;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DeclineCmd extends systems.mythical.mythicskywars.commands.BaseCmd {
    public DeclineCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "decline";
        alias = new String[]{"d"};
        argLength = 1;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        Party party = Party.getPartyOfInvite(player);
        if (party == null) {
            player.sendMessage(new Messaging.MessageFormatter().format("party.noinvite"));
            return true;
        }

        boolean result = party.declineInvite(player);
        if (result) {
            player.sendMessage(new Messaging.MessageFormatter().setVariable("partyname", party.getPartyName()).format("party.youdeclined"));
        }
        return true;
    }
}
