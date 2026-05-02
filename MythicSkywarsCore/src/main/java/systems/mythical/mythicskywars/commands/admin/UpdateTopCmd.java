package systems.mythical.mythicskywars.commands.admin;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.commands.BaseCmd;
import systems.mythical.mythicskywars.database.DataStorage;
import systems.mythical.mythicskywars.enums.LeaderType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UpdateTopCmd extends BaseCmd {

    public UpdateTopCmd(String t) {
        type = t;
        forcePlayer = false;
        cmdName = "updatetop";
        alias = new String[]{"ut"};
        argLength = 1; //counting cmdName
    }

    @Override
    public boolean run(CommandSender sender, Player player, String[] args) {
        for (LeaderType type : LeaderType.values()) {
            if (MythicSkywars.getCfg().isTypeEnabled(type)) {
                DataStorage.get().updateTop(type, MythicSkywars.getCfg().getLeaderSize());
            }
        }
        return true;
    }
}