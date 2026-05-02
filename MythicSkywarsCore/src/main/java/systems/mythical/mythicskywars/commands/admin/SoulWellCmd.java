package systems.mythical.mythicskywars.commands.admin;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.commands.BaseCmd;
import systems.mythical.mythicskywars.menus.soulwell.SoulWellMenu;
import systems.mythical.mythicskywars.utilities.Messaging;
import systems.mythical.mythicskywars.utilities.SoulWellManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Set;

public class SoulWellCmd extends BaseCmd {

    public SoulWellCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "soulwell";
        alias = new String[]{"swell"};
        argLength = 1;
    }

    @Override
    public boolean run(CommandSender sender, Player player, String[] args) {
        if (!player.hasPermission("sw.admin")) {
            sender.sendMessage(new Messaging.MessageFormatter().format("error.nopermission"));
            return true;
        }
        SoulWellManager manager = MythicSkywars.getSoulWellManager();
        if (manager == null) {
            sender.sendMessage(new Messaging.MessageFormatter().format("command.soulwell.unavailable"));
            return true;
        }
        if (args.length == 1) {
            sendUsage(sender);
            return true;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        if ("setwell".equals(sub)) {
            Block target = player.getTargetBlock((Set<Material>) null, 6);
            if (target == null || target.getType() == Material.AIR) {
                sender.sendMessage(new Messaging.MessageFormatter().format("command.soulwell.look-at-block"));
                return true;
            }
            manager.setWellLocation(target.getLocation());
            sender.sendMessage(manager.describeWell());
            sender.sendMessage(new Messaging.MessageFormatter().format("command.soulwell.holo-refreshed"));
            return true;
        }
        if ("clear".equals(sub)) {
            manager.setWellLocation(null);
            sender.sendMessage(new Messaging.MessageFormatter().format("command.soulwell.cleared"));
            return true;
        }
        if ("holo".equals(sub) || "setholo".equals(sub) || "refreshholo".equals(sub)) {
            manager.refreshHologram();
            sender.sendMessage(new Messaging.MessageFormatter().format("command.soulwell.holo-refreshed"));
            return true;
        }
        if ("open".equals(sub)) {
            Player target = player;
            if (args.length >= 3) {
                target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(new Messaging.MessageFormatter().format("command.player-not-found"));
                    return true;
                }
            }
            SoulWellMenu.openForced(target);
            if (manager.isClickDebugEnabled()) {
                MythicSkywars.get().getLogger().info("[SoulWellDebug] Force-open requested by "
                        + player.getName() + " for " + target.getName()
                        + ", soulwell.enabled=" + MythicSkywars.getCfg().isSoulWellEnabled());
            }
            sender.sendMessage(new Messaging.MessageFormatter().setVariable("player", target.getName()).format("command.soulwell.opened-for"));
            return true;
        }
        if ("debug".equals(sub)) {
            if (args.length < 3) {
                sender.sendMessage(new Messaging.MessageFormatter().setVariable("state", String.valueOf(manager.isClickDebugEnabled())).format("command.soulwell.debug-state"));
                sender.sendMessage(new Messaging.MessageFormatter().format("command.soulwell.usage-debug"));
                return true;
            }
            String mode = args[2].toLowerCase(Locale.ROOT);
            boolean enable;
            if ("on".equals(mode) || "true".equals(mode) || "1".equals(mode)) {
                enable = true;
            } else if ("off".equals(mode) || "false".equals(mode) || "0".equals(mode)) {
                enable = false;
            } else {
                sender.sendMessage(new Messaging.MessageFormatter().format("command.soulwell.usage-debug"));
                return true;
            }
            manager.setClickDebugEnabled(enable);
            sender.sendMessage(new Messaging.MessageFormatter().setVariable("state", String.valueOf(enable)).format("command.soulwell.debug-set"));
            return true;
        }
        if ("info".equals(sub)) {
            sender.sendMessage(manager.describeWell());
            return true;
        }
        sendUsage(sender);
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(new Messaging.MessageFormatter().format("command.soulwell.usage-header"));
        sender.sendMessage(new Messaging.MessageFormatter().format("command.soulwell.usage-setwell"));
        sender.sendMessage(new Messaging.MessageFormatter().format("command.soulwell.usage-holo"));
        sender.sendMessage(new Messaging.MessageFormatter().format("command.soulwell.usage-info"));
        sender.sendMessage(new Messaging.MessageFormatter().format("command.soulwell.usage-clear"));
        sender.sendMessage(new Messaging.MessageFormatter().format("command.soulwell.usage-open"));
        sender.sendMessage(new Messaging.MessageFormatter().format("command.soulwell.usage-debug"));
    }
}

