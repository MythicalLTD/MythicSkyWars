package systems.mythical.mythicskywars.commands.player;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.commands.BaseCmd;
import systems.mythical.mythicskywars.utilities.Messaging;
import systems.mythical.mythicskywars.utilities.Util;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SWJoinMenuCmd extends BaseCmd {

    public SWJoinMenuCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "joinmenu";
        alias = new String[]{"jm"};
        argLength = 1; // /sw joinmenu <solo|team>
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        if (args.length < 2) {
            openGeneralJoinMenu(player);
            return true;
        }

        String mode = args[1].toLowerCase();

        if (!(mode.equals("solo") || mode.equals("team"))) {
            player.sendMessage((new Messaging.MessageFormatter()).format("helpList.sw.joinmenu"));
            return true;
        }

        // Open the corresponding join menu
        if (mode.equals("solo")) {
            if (!MythicSkywars.getIC().hasViewers("joinsinglemenu")) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        MythicSkywars.getIC().getMenu("joinsinglemenu").update();
                    }
                }.runTaskLater(MythicSkywars.get(), 5);
            }
            MythicSkywars.getIC().show(player, "joinsinglemenu");
        } else {
            if (!MythicSkywars.getIC().hasViewers("jointeammenu")) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        MythicSkywars.getIC().getMenu("jointeammenu").update();
                    }
                }.runTaskLater(MythicSkywars.get(), 5);
            }
            MythicSkywars.getIC().show(player, "jointeammenu");
        }

        return true;
    }

    private void openGeneralJoinMenu(Player player) {
        if (MythicSkywars.getIC().has("joinmenu")) {
            Util.get().playSound(player, player.getLocation(), MythicSkywars.getCfg().getOpenJoinMenuSound(), 1, 1);
            MythicSkywars.getIC().show(player, "joinmenu");
        } else {
            player.sendMessage(new Messaging.MessageFormatter().format("error.joinmenu-not-found"));
        }
    }
}
