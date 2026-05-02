package systems.mythical.mythicskywars.listeners;

import com.google.common.collect.ImmutableList;
import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.enums.MatchState;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.managers.GameMapManager;
import systems.mythical.mythicskywars.utilities.Messaging;
import org.bukkit.event.EventHandler;
import org.bukkit.event.server.ServerListPingEvent;

public class PingListener implements org.bukkit.event.Listener {


    public PingListener() {
    }

    @EventHandler
    public void onPing(ServerListPingEvent serverListPingEvent) {
        if (MythicSkywars.getCfg().bungeeMode() && MythicSkywars.getCfg().debugEnabled()) {
            MythicSkywars.get().getLogger().info("Received server list ping from " + serverListPingEvent.getAddress());
        }
        if (MythicSkywars.get().serverLoaded()) {
            ImmutableList<GameMap> mapsCopy = MythicSkywars.getGameMapMgr().getMapsCopy();
            if (mapsCopy.size() > 0) {
                GameMap game = mapsCopy.get(0);
                serverListPingEvent.setMotd(new Messaging.MessageFormatter().setVariable("matchstate", game.getMatchState().toString())
                        .setVariable("playercount", "" + game.getPlayerCount()).setVariable("maxplayers", "" + game.getMaxPlayers())
                        .setVariable("displayname", game.getDisplayName()).format("bungee.motd"));
            } else {
                serverListPingEvent.setMotd(new Messaging.MessageFormatter().setVariable("matchstate", MatchState.ENDING.toString())
                        .setVariable("playercount", "0").setVariable("maxplayers", "0")
                        .setVariable("displayname", "null").format("bungee.motd"));
            }
        } else {
            serverListPingEvent.setMotd(new Messaging.MessageFormatter().setVariable("matchstate", MatchState.ENDING.toString())
                    .setVariable("playercount", "0").setVariable("maxplayers", "0")
                    .setVariable("displayname", "null").format("bungee.motd"));
        }
    }

}
