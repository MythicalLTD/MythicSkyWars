package systems.mythical.mythicskywars.commands.admin;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.events.MythicSkywarsReloadEvent;
import systems.mythical.mythicskywars.events.MythicSkywarsReloadPreLoadEvent;
import systems.mythical.mythicskywars.utilities.Messaging;
import systems.mythical.mythicskywars.utilities.SWRServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReloadCmd extends systems.mythical.mythicskywars.commands.BaseCmd {
    public ReloadCmd(String t) {
        type = t;
        forcePlayer = false;
        cmdName = "reload";
        alias = new String[]{"r"};
        argLength = 1;
    }

    public boolean run(CommandSender sender, Player player, String[] args) {
        MythicSkywars.get().onDisable();
        Bukkit.getPluginManager().callEvent(new MythicSkywarsReloadPreLoadEvent());
        MythicSkywars.get().load();

        if (MythicSkywars.getCfg().bungeeMode() && MythicSkywars.getCfg().isLobbyServer()) {
            MythicSkywars.get().prepareServers();

            SWRServer.updateServerSigns();
        }

        sender.sendMessage(new Messaging.MessageFormatter().format("command.reload"));
        //if (MythicSkywars.get().getUpdater().getUpdateStatus() == 1) {
        //    BaseComponent base = new TextComponent("§d§l[MythicSkywars] §aA new update has been found: §b" + MythicSkywars.get().getUpdater().getLatestVersion() + "§a. Click here to update!");
        //    base.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, MythicSkywars.get().getUpdater().getUpdateURL()));
        //    base.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{new TextComponent("§7Click here to update to the latest version!")}));
        //    if (sender instanceof Player) {
        //        MythicSkywars.getNMS().sendJSON((Player)sender, "[\"\",{\"text\":\"§d§l[MythicSkywars] §aA new update has been found: §b" + MythicSkywars.get().getUpdater().getLatestVersion() + "§a. Click here to update!\",\"clickEvent\":{\"action\":\"open_url\",\"value\":\"" + MythicSkywars.get().getUpdater().getUpdateURL() + "\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"\",\"extra\":[{\"text\":\"§7Click here to update to the latest version!\"}]}}}]");
        //    }
        //    else {
        //        sender.sendMessage(base.toPlainText());
        //    }
        //}

        Bukkit.getPluginManager().callEvent(new MythicSkywarsReloadEvent());
        return true;
    }
}
