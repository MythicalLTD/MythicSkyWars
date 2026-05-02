package systems.mythical.mythicskywars.commands.admin;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.commands.BaseCmd;
import systems.mythical.mythicskywars.enums.LeaderType;
import systems.mythical.mythicskywars.utilities.Messaging;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.StringJoiner;

public class HoloAddCmd extends BaseCmd {

    public HoloAddCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "hologram";
        alias = new String[]{"h"};
        argLength = 3; //counting cmdName
    }

    @Override
    public boolean run(CommandSender sender, Player player, String[] args) {
        if (MythicSkywars.getCfg().hologramsEnabled()) {
            if (Bukkit.getPluginManager().isPluginEnabled("DecentHolograms") && MythicSkywars.getHoloManager() != null) {
                LeaderType type = LeaderType.matchType(args[1].toUpperCase());
                if (type == null || !MythicSkywars.get().getLeaderTypes().contains(type.toString())) {
                    StringJoiner types = new StringJoiner(", ");
                    for (String add : MythicSkywars.get().getLeaderTypes()) {
                        types.add(add);
                    }
                    player.sendMessage(new Messaging.MessageFormatter().setVariable("validtypes", types.toString()).format("leaderboard.invalidtype"));
                    return true;
                }
                String format = args[2];
                List<String> formatKeys = MythicSkywars.getHoloManager().getFormats(type);
                if (formatKeys != null && formatKeys.contains(format)) {
                    MythicSkywars.getHoloManager().createLeaderHologram(player.getEyeLocation(), type, format);
                    player.sendMessage(new Messaging.MessageFormatter().setVariable("type", type.name()).setVariable("format", format).format("command.hologram-created"));
                    return true;
                }

                StringJoiner formatJoiner = new StringJoiner(", ");
                if (formatKeys != null) {
                    for (String add : formatKeys) {
                        formatJoiner.add(add);
                    }
                }
                player.sendMessage(new Messaging.MessageFormatter().setVariable("validtypes", formatJoiner.toString()).format("leaderboard.invalidformat"));
                return true;
            }
            player.sendMessage(new Messaging.MessageFormatter().format("error.holograms-plugin-missing"));
            return true;
        }
        player.sendMessage(new Messaging.MessageFormatter().format("error.holograms-not-enabled"));
        return true;
    }

}