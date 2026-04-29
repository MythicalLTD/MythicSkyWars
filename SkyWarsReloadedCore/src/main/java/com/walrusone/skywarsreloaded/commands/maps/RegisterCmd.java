package com.walrusone.skywarsreloaded.commands.maps;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.game.GameMap;
import com.walrusone.skywarsreloaded.utilities.Messaging;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RegisterCmd extends com.walrusone.skywarsreloaded.commands.BaseCmd {

    public RegisterCmd(String t) {
        type = t;
        forcePlayer = false;
        cmdName = "register";
        alias = new String[]{"reg"};
        argLength = 2;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        String worldName = args[1];
        GameMap gMap = SkyWarsReloaded.getGameMapMgr().getMap(worldName);
        if (gMap != null) {
            gMap.setRegistered(true);
            int registeredStatus = gMap.registerMap(sender);
            if (registeredStatus == 0) {
                sender.sendMessage(new Messaging.MessageFormatter().setVariable("mapname", gMap.getDisplayName()).format("maps.registered"));
            } else {
                if (registeredStatus == 1) {
                    sender.sendMessage(new Messaging.MessageFormatter().setVariable("mapname", gMap.getName()).format("error.map-register-unbalanced-spawns"));
                }
                else if (registeredStatus == 2) {
                    sender.sendMessage(new Messaging.MessageFormatter().setVariable("mapname", gMap.getName()).format("error.map-register-not-enough-spawns"));
                }
                else if (registeredStatus == 3) {
                    sender.sendMessage(new Messaging.MessageFormatter().setVariable("mapname", gMap.getName()).format("error.map-register-missing-spec"));
                }
                else if (registeredStatus == 4) {
                    sender.sendMessage(new Messaging.MessageFormatter().setVariable("mapname", gMap.getName()).format("error.map-register-missing-lobby"));
                }
                else {
                    sender.sendMessage(new Messaging.MessageFormatter().format("error.map-failed-to-register"));
                }
            }
        } else {
            sender.sendMessage(new Messaging.MessageFormatter().format("error.map-register-not-exist"));
        }
        return true;
    }
}
