package systems.mythical.mythicskywars.commands.player;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.commands.BaseCmd;
import systems.mythical.mythicskywars.enums.LeaderType;
import systems.mythical.mythicskywars.managers.Leaderboard;
import systems.mythical.mythicskywars.utilities.Messaging;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.StringJoiner;

public class SWTopCmd extends BaseCmd {
    public SWTopCmd(String t) {
        this.type = t;
        this.forcePlayer = true;
        this.cmdName = "top";
        this.alias = new String[] {"leaderboard"};
        this.argLength = 1;
        this.maxArgs = 2;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        String leaderType = args.length < 2 ? "WINS" : args[1].toUpperCase();
        LeaderType selectedType;
        try {
            selectedType = LeaderType.valueOf(leaderType);
        } catch (IllegalArgumentException ex) {
            StringJoiner types = new StringJoiner(", ");
            for (String add : MythicSkywars.get().getLeaderTypes()) {
                types.add(add);
            }
            player.sendMessage(new Messaging.MessageFormatter().setVariable("validtypes", types.toString()).format("leaderboard.invalidtype"));
            return true;
        }

        if (MythicSkywars.get().getLeaderTypes().contains(leaderType)) {
            if (!MythicSkywars.getLB().loaded(selectedType)) {
                player.sendMessage(new Messaging.MessageFormatter().format("leaderboard.updating"));
                return true;
            }

            List<Leaderboard.LeaderData> top = MythicSkywars.getLB().getTopList(selectedType);

            player.sendMessage(new Messaging.MessageFormatter().format("leaderboard.header"));
            player.sendMessage(new Messaging.MessageFormatter().format("leaderboard.header2"));
            if (top.size() == 0) {
                player.sendMessage(new Messaging.MessageFormatter().format("leaderboard.no-data"));
            }

            for (int i = 0; i < top.size(); i++) {
                Leaderboard.LeaderData playerData = top.get(i);
                player.sendMessage(new Messaging.MessageFormatter().setVariable("rank", "" + (i + 1))
                        .setVariable("player", playerData.getName())
                        .setVariable("wins", "" + playerData.getWins())
                        .setVariable("losses", "" + playerData.getLoses())
                        .setVariable("kills", "" + playerData.getKills())
                        .setVariable("deaths", "" + playerData.getDeaths())
                        .setVariable("xp", "" + playerData.getXp())
                        .format("leaderboard.player-data"));
            }

            player.sendMessage(new Messaging.MessageFormatter().format("leaderboard.footer"));
            return true;
        }

        StringJoiner types = new StringJoiner(", ");
        for (String add : MythicSkywars.get().getLeaderTypes()) {
            types.add(add);
        }

        player.sendMessage(new Messaging.MessageFormatter().setVariable("validtypes", types.toString()).format("leaderboard.invalidtype"));
        return true;
    }
}
