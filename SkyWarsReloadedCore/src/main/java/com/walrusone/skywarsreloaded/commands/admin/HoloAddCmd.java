package com.walrusone.skywarsreloaded.commands.admin;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.commands.BaseCmd;
import com.walrusone.skywarsreloaded.enums.LeaderType;
import com.walrusone.skywarsreloaded.utilities.Messaging;
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
        if (SkyWarsReloaded.getCfg().hologramsEnabled()) {
            if (Bukkit.getPluginManager().isPluginEnabled("DecentHolograms") && SkyWarsReloaded.getHoloManager() != null) {
                LeaderType type = LeaderType.matchType(args[1].toUpperCase());
                if (type == null || !SkyWarsReloaded.get().getLeaderTypes().contains(type.toString())) {
                    StringJoiner types = new StringJoiner(", ");
                    for (String add : SkyWarsReloaded.get().getLeaderTypes()) {
                        types.add(add);
                    }
                    player.sendMessage(new Messaging.MessageFormatter().setVariable("validtypes", types.toString()).format("leaderboard.invalidtype"));
                    return true;
                }
                String format = args[2];
                List<String> formatKeys = SkyWarsReloaded.getHoloManager().getFormats(type);
                if (formatKeys != null && formatKeys.contains(format)) {
                    SkyWarsReloaded.getHoloManager().createLeaderHologram(player.getEyeLocation(), type, format);
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