package systems.mythical.mythicskywars.listeners;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.enums.PlayerRemoveReason;
import systems.mythical.mythicskywars.managers.MatchManager;
import systems.mythical.mythicskywars.utilities.Messaging;
import systems.mythical.mythicskywars.utilities.Util;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class PlayerCommandPrepocessListener implements org.bukkit.event.Listener {
    public PlayerCommandPrepocessListener() {
    }

    @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommandPrepocess(PlayerCommandPreprocessEvent e) {
        systems.mythical.mythicskywars.game.GameMap gMap = MatchManager.get().getSpectatorMap(e.getPlayer());
        String[] splited = e.getMessage().split("\\s+");
        if (gMap != null) {
            if (splited[0].equalsIgnoreCase("/spawn")) {
                e.setCancelled(true);
                MythicSkywars.get().getPlayerManager().removePlayer(
                        e.getPlayer(), PlayerRemoveReason.PLAYER_QUIT_GAME, null, false);
                return;
            }
            if (MythicSkywars.getCfg().disableCommandsSpectate()) {
                if (e.getPlayer().hasPermission("sw.allowcommands")) {
                    return;
                }
                for (String a1 : MythicSkywars.getCfg().getEnabledCommandsSpectate()) {
                    if (splited.length == 1) {
                        if (!splited[0].equalsIgnoreCase("/" + a1)) {
                        }


                    } else if ((splited[0].equalsIgnoreCase("/" + a1)) || ((splited[0] + " " + splited[1]).equalsIgnoreCase("/" + a1))) {
                        return;
                    }
                }

                e.getPlayer().sendMessage(new Messaging.MessageFormatter().format("game.command-disabled-spec"));
                e.setCancelled(true);
                return;
            }
        }

        if (splited[0].equalsIgnoreCase("/spawn")) {
            if (MatchManager.get().getPlayerMapSafe(e.getPlayer()) == null && Util.get().isSpawnWorld(e.getPlayer().getWorld())) {
                e.setCancelled(true);
                Location spawn = MythicSkywars.getCfg().getSpawn();
                if (spawn != null) {
                    e.getPlayer().setFallDistance(0f);
                    e.getPlayer().teleport(spawn);
                } else {
                    e.getPlayer().sendMessage(new Messaging.MessageFormatter().format("error.nospawn"));
                }
                return;
            }
        }

        if (MatchManager.get().getPlayerMapSafe(e.getPlayer()) != null) {
            if (e.getPlayer().hasPermission("sw.allowcommands")) {
                return;
            }
            for (String a1 : MythicSkywars.getCfg().getEnabledCommands()) {
                if (splited.length == 1) {
                    if (!splited[0].equalsIgnoreCase("/" + a1)) {
                    }

                } else if ((splited.length > 1) && (
                        (splited[0].equalsIgnoreCase("/" + a1)) || ((splited[0] + " " + splited[1]).equalsIgnoreCase("/" + a1)))) {
                    return;
                }
            }

            e.getPlayer().sendMessage(new Messaging.MessageFormatter().format("game.command-disabled"));
            e.setCancelled(true);
        }
    }
}
