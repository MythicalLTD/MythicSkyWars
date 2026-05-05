package systems.mythical.mythicskywars.commands;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.api.command.SWRCmdManagerAPI;
import systems.mythical.mythicskywars.commands.admin.*;
import systems.mythical.mythicskywars.commands.player.*;
import systems.mythical.mythicskywars.utilities.Messaging;
import systems.mythical.mythicskywars.utilities.Util;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class MainCmdManager implements CommandExecutor, SWRCmdManagerAPI {
    private List<BaseCmd> admincmds = new ArrayList<>();
    private List<BaseCmd> pcmds = new ArrayList<>();
    private static MainCmdManager cm;

    public MainCmdManager() {
        cm = this;
        admincmds.add(new ReloadCmd("sw"));
        admincmds.add(new ChestAddCmd("sw"));
        admincmds.add(new ChestEditCmd("sw"));
        admincmds.add(new SetStatsCmd("sw"));
        admincmds.add(new ClearStatsCmd("sw"));
        admincmds.add(new SetSpawnCmd("sw"));
        admincmds.add(new StartCmd("sw"));
        admincmds.add(new UpdateTopCmd("sw"));
        admincmds.add(new HoloAddCmd("sw"));
        admincmds.add(new HoloRemoveCmd("sw"));
        admincmds.add(new SWSelectCmd("sw"));
        admincmds.add(new SWSendCmd("sw"));
        admincmds.add(new LobbyWaterPortalCmd("sw"));
        admincmds.add(new SoulWellCmd("sw"));
        admincmds.add(new SWEcoCmd("sw"));
        admincmds.add(new MigrateUSWCmd("sw"));
        admincmds.add(new CheckUpdatesCmd("sw"));
        admincmds.add(new UpdateCmd("sw"));
        admincmds.add(new FeatherCmd("sw"));
        admincmds.add(new LunarCmd("sw"));
        admincmds.add(new LabyModCmd("sw"));

        pcmds.add(new SWJoinCmd("sw"));
        pcmds.add(new SWJoinMenuCmd("sw"));
        pcmds.add(new SWQuitCmd("sw"));
        pcmds.add(new SWStatsCmd("sw"));
        pcmds.add(new SWTopCmd("sw"));
        pcmds.add(new SWCoinsCmd("sw"));
        pcmds.add(new SWOptionsCmd("sw"));
        pcmds.add(new SWPrestigeCmd("sw"));
        pcmds.add(new SWPerksCmd("sw"));
        pcmds.add(new SWSpectateCmd("sw"));
        pcmds.add(new SWLobbyTeleportCmd("sw"));
        pcmds.add(new SWBypassLobbySlotsCmd("sw"));

        if (MythicSkywars.getCfg().winsoundMenuEnabled()) {
            pcmds.add(new SWWinsoundCmd("sw"));
        }
        if (MythicSkywars.getCfg().killsoundMenuEnabled()) {
            pcmds.add(new SWKillsoundCmd("sw"));
        }
        if (MythicSkywars.getCfg().tauntsMenuEnabled()) {
            pcmds.add(new SWTauntCmd("sw"));
        }
        if (MythicSkywars.getCfg().projectileMenuEnabled()) {
            pcmds.add(new SWProjectileCmd("sw"));
        }
        if (MythicSkywars.getCfg().particleMenuEnabled()) {
            pcmds.add(new SWParticleCmd("sw"));
        }
        if (MythicSkywars.getCfg().glassMenuEnabled()) {
            pcmds.add(new SWGlassCmd("sw"));
        }
    }

    public static List<BaseCmd> getCommands() {
        List<BaseCmd> a = new ArrayList<>(cm.admincmds);
        a.addAll(cm.pcmds);
        return a;
    }

    public boolean onCommand(CommandSender s, Command command, String label, String[] args) {
        if (args.length == 0 || getCommand(args[0]) == null) {
            s.sendMessage(new Messaging.MessageFormatter().format("helpList.header"));
            sendHelp(admincmds, s, "1");
            sendHelp(pcmds, s, "2");
            s.sendMessage(new Messaging.MessageFormatter().format("helpList.footer"));
        } else getCommand(args[0]).processCmd(s, args);
        return true;
    }

    private void sendHelp(List<BaseCmd> cmds, CommandSender s, String num) {
        int count = 0;
        for (BaseCmd cmd : cmds) {
            if (Util.get().hasPerm(cmd.getType(), s, cmd.cmdName)) {
                count++;
                if (count == 1) {
                    s.sendMessage(" ");
                    s.sendMessage(new Messaging.MessageFormatter().format("helpList.sw.header" + num));
                }
                String helpLine = new Messaging.MessageFormatter().format("helpList.sw." + cmd.cmdName);
                // Tolerate accidental "&/" in disk messages.yml entries.
                s.sendMessage(helpLine.replace("&/", "/"));
            }
        }
    }

    public BaseCmd getCommand(String s) {
        BaseCmd cmd;
        cmd = getCmd(admincmds, s);
        if (cmd == null) cmd = getCmd(pcmds, s);
        return cmd;
    }

    private BaseCmd getCmd(List<BaseCmd> cmds, String s) {
        for (BaseCmd cmd : cmds) {
            if (cmd.cmdName.equalsIgnoreCase(s)) {
                return cmd;
            }
            if (cmd.alias != null) {
                for (String alias : cmd.alias) {
                    if (alias != null && alias.equalsIgnoreCase(s)) {
                        return cmd;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void registerCommand(BaseCmd commandIn) {
        registerCommand(commandIn, 0);
    }

    public void registerCommand(BaseCmd commandIn, int type) {
        if (commandIn == null || type < 0 || type > 1) return;
        switch (type) {
            case 0:
                pcmds.add(commandIn);
                break;
            case 1:
                admincmds.add(commandIn);
                break;
        }
    }

    @Override
    public void unregisterCommand(BaseCmd commandIn) {
        unregisterCommand(commandIn, 0);
    }

    @Override
    public BaseCmd getSubCommand(String name) {
        return null;
    }

    public void unregisterCommand(BaseCmd commandIn, int type) {
        if (commandIn == null || type < 0 || type > 1) return;
        switch (type) {
            case 0:
                pcmds.remove(commandIn);
                break;
            case 1:
                admincmds.remove(commandIn);
                break;
        }
    }
}
