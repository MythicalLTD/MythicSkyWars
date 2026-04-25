package com.walrusone.skywarsreloaded.commands.admin;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.commands.BaseCmd;
import com.walrusone.skywarsreloaded.enums.GameType;
import com.walrusone.skywarsreloaded.utilities.LobbyWaterPortalManager;
import com.walrusone.skywarsreloaded.utilities.Messaging;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public class LobbyWaterPortalCmd extends BaseCmd {

    public LobbyWaterPortalCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "lobbywaterportal";
        alias = new String[]{"lwp"};
        argLength = 1;
    }

    @Override
    public boolean run(CommandSender sender, Player player, String[] args) {
        LobbyWaterPortalManager manager = SkyWarsReloaded.getLobbyWaterPortals();
        if (manager == null) {
            sender.sendMessage(new Messaging.MessageFormatter().format("command.lobbywaterportal.unavailable"));
            return true;
        }

        if (args.length == 1) {
            sendUsage(sender);
            return true;
        }

        String sub = args[1].toLowerCase(Locale.ROOT);
        if ("create".equals(sub)) {
            if (args.length < 3) {
                sender.sendMessage(new Messaging.MessageFormatter().format("command.lobbywaterportal.usage-create"));
                return true;
            }
            String name = args[2];
            LobbyWaterPortalManager.PortalRegion before = manager.get(name);
            manager.create(name, player.getWorld().getName());
            sender.sendMessage(before == null
                    ? new Messaging.MessageFormatter().setVariable("name", name).format("command.lobbywaterportal.created")
                    : new Messaging.MessageFormatter().setVariable("name", name).format("command.lobbywaterportal.exists"));
            return true;
        }

        if ("delete".equals(sub) || "remove".equals(sub)) {
            if (args.length < 3) {
                sender.sendMessage(new Messaging.MessageFormatter().format("command.lobbywaterportal.usage-delete"));
                return true;
            }
            boolean deleted = manager.delete(args[2]);
            sender.sendMessage(deleted
                    ? new Messaging.MessageFormatter().setVariable("name", args[2]).format("command.lobbywaterportal.deleted")
                    : new Messaging.MessageFormatter().setVariable("name", args[2]).format("command.lobbywaterportal.not-found"));
            return true;
        }

        if ("pos1".equals(sub) || "pos2".equals(sub)) {
            if (args.length < 3) {
                sender.sendMessage(new Messaging.MessageFormatter().setVariable("sub", sub).format("command.lobbywaterportal.usage-pos"));
                return true;
            }
            String name = args[2];
            LobbyWaterPortalManager.PortalRegion portal = manager.get(name);
            if (portal == null) {
                sender.sendMessage(new Messaging.MessageFormatter().setVariable("name", name).format("command.lobbywaterportal.not-found"));
                return true;
            }
            if ("pos1".equals(sub)) {
                manager.setPos1(name, player.getLocation());
            } else {
                manager.setPos2(name, player.getLocation());
            }
            sender.sendMessage(new Messaging.MessageFormatter().setVariable("sub", sub).setVariable("name", name).format("command.lobbywaterportal.set-pos"));
            return true;
        }

        if ("type".equals(sub)) {
            if (args.length < 4) {
                sender.sendMessage(new Messaging.MessageFormatter().format("command.lobbywaterportal.usage-type"));
                return true;
            }
            String name = args[2];
            LobbyWaterPortalManager.PortalRegion portal = manager.get(name);
            if (portal == null) {
                sender.sendMessage(new Messaging.MessageFormatter().setVariable("name", name).format("command.lobbywaterportal.not-found"));
                return true;
            }
            String typeRaw = args[3].toLowerCase(Locale.ROOT);
            GameType type;
            if ("single".equals(typeRaw) || "solo".equals(typeRaw)) {
                type = GameType.SINGLE;
            } else if ("team".equals(typeRaw)) {
                type = GameType.TEAM;
            } else if ("all".equals(typeRaw)) {
                type = GameType.ALL;
            } else {
                sender.sendMessage(new Messaging.MessageFormatter().format("command.lobbywaterportal.invalid-type"));
                return true;
            }
            manager.setType(name, type);
            sender.sendMessage(new Messaging.MessageFormatter().setVariable("name", name).setVariable("type", type.name().toLowerCase(Locale.ROOT)).format("command.lobbywaterportal.set-type"));
            return true;
        }

        if ("list".equals(sub)) {
            sender.sendMessage(new Messaging.MessageFormatter().format("command.lobbywaterportal.list-header"));
            if (manager.all().isEmpty()) {
                sender.sendMessage(new Messaging.MessageFormatter().format("command.lobbywaterportal.list-empty"));
                return true;
            }
            for (LobbyWaterPortalManager.PortalRegion portal : manager.all()) {
                sender.sendMessage(new Messaging.MessageFormatter().setVariable("entry", LobbyWaterPortalManager.formatPortal(portal)).format("command.lobbywaterportal.list-entry"));
            }
            return true;
        }

        sendUsage(sender);
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(new Messaging.MessageFormatter().format("command.lobbywaterportal.usage-header"));
        sender.sendMessage(new Messaging.MessageFormatter().format("command.lobbywaterportal.usage-create"));
        sender.sendMessage(new Messaging.MessageFormatter().format("command.lobbywaterportal.usage-pos1"));
        sender.sendMessage(new Messaging.MessageFormatter().format("command.lobbywaterportal.usage-pos2"));
        sender.sendMessage(new Messaging.MessageFormatter().format("command.lobbywaterportal.usage-type"));
        sender.sendMessage(new Messaging.MessageFormatter().format("command.lobbywaterportal.usage-delete"));
        sender.sendMessage(new Messaging.MessageFormatter().format("command.lobbywaterportal.usage-list"));
    }
}
