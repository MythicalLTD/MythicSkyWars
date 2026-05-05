package systems.mythical.mythicskywars.listeners;

import systems.mythical.mythicskywars.clients.feather.FeatherClientBridge;
import systems.mythical.mythicskywars.clients.labymod.LabyModBridge;
import systems.mythical.mythicskywars.clients.lunar.LunarLobbyWaterPortalApollo;
import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.enums.PlayerRemoveReason;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.managers.PlayerStat;
import systems.mythical.mythicskywars.utilities.Party;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerQuitListener implements org.bukkit.event.Listener {
    public PlayerQuitListener() {
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        try {
            LunarLobbyWaterPortalApollo.onQuit(player);
        } catch (NoClassDefFoundError e) {
            // Apollo not available
        }
        try {
            FeatherClientBridge.onQuit(player);
        } catch (NoClassDefFoundError e) {
            // Feather not available
        }
        try {
            LabyModBridge.onQuit(player);
        } catch (NoClassDefFoundError e) {
            // LabyMod not available
        }
        Party party = Party.getParty(player);
        if (party != null) {
            party.removeMember(player);
        }

        GameMap playerMap = MythicSkywars.get().getMatchManager().getPlayerMap(player);
        if (playerMap != null) {
            MythicSkywars.get().getMatchManager().markRejoinCandidate(player, playerMap);
            MythicSkywars.get().getPlayerManager().removePlayer(
                    player, PlayerRemoveReason.PLAYER_QUIT_SERVER, null, true);
        }

        PlayerStat pStats = PlayerStat.getPlayerStats(uuid);
        // Don't remove the player stats if the game they were in isn't done yet.
        if (pStats != null && playerMap == null) {
            pStats.saveStats(() -> PlayerStat.removePlayer(uuid.toString()));
        }
        PlayerStat.resetScoreboard(player);
    }
}
