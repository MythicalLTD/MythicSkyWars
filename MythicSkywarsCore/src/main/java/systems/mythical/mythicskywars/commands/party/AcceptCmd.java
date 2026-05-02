package systems.mythical.mythicskywars.commands.party;

import systems.mythical.mythicskywars.utilities.Messaging;
import systems.mythical.mythicskywars.utilities.Party;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AcceptCmd extends systems.mythical.mythicskywars.commands.BaseCmd {
    public AcceptCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "accept";
        alias = new String[]{"a"};
        argLength = 1;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        Party party = Party.getParty(player);
        if (party != null) {
            player.sendMessage(new Messaging.MessageFormatter().format("party.alreadyinparty"));
            return true;
        }

        party = Party.getPartyOfInvite(player);
        if (party == null) {
            player.sendMessage(new Messaging.MessageFormatter().format("party.noinvite"));
            return true;
        }

        boolean result = party.acceptInvite(player);
        if (result) {
            player.sendMessage(new Messaging.MessageFormatter().setVariable("partyname", party.getPartyName()).format("party.youjoined"));
        } else {
            player.sendMessage(new Messaging.MessageFormatter().setVariable("partyname", party.getPartyName()).format("party.partyisfull-nojoin"));
        }


        return true;
    }
}
