package systems.mythical.mythicskywars.utilities;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.events.MythicSkywarsJoinEvent;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.game.TeamCard;
import systems.mythical.mythicskywars.managers.MatchManager;
import systems.mythical.mythicskywars.managers.PlayerStat;
import de.simonsator.partyandfriends.api.pafplayers.PAFPlayer;
import de.simonsator.partyandfriends.api.party.PlayerParty;
import de.simonsator.partyandfriends.api.party.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PartyAndFriendsHook implements Listener {

    public PartyAndFriendsHook() {
        if (MythicSkywars.getCfg().debugEnabled()) {
            MythicSkywars.get().getLogger().info("PartyAndFriends Hook is enabled");
        }
    }

    @EventHandler
    public void onSkyWarsJoin(MythicSkywarsJoinEvent e) {
        if (MythicSkywars.getCfg().debugEnabled()) {
            MythicSkywars.get().getLogger().info("PartyAndFriendsHook::onSkyWarsJoin");
        }
        if (e.getGame().getTeamSize() == 1) return;
        Player target = e.getPlayer();

//        PAFPlayer pafPlayer = PAFPlayerManager.getInstance().getPlayer(target.getUniqueId());
        PlayerParty party = PartyManager.getInstance().getParty(target.getUniqueId());
        if (party != null) {
            if (MythicSkywars.getCfg().debugEnabled()) {
                MythicSkywars.get().getLogger().info("PartyAndFriendsHook::onSkyWarsJoin party is not null");
            }
            if (!party.getLeader().getUniqueId().equals(target.getUniqueId())) {
                if (MythicSkywars.getCfg().debugEnabled()) {
                    MythicSkywars.get().getLogger().info("PartyAndFriendsHook::onSkyWarsJoin " + target.getName() + " is not party owner");
                }
                // player is not the owner
                Player owner = Bukkit.getPlayer(party.getLeader().getUniqueId());
                GameMap map = MatchManager.get().getPlayerMap(owner);
                if (map != null) {
                    TeamCard tc = map.getTeamCard(owner);
                    if (tc != null) {
                        if (tc.getEmptySlots() != 0) {
                            tc.sendReservation(target, PlayerStat.getPlayerStats(target));
                            return;
                        }
                    }
                }
            }

            for (PAFPlayer p : party.getAllPlayers()) {
                Player player = Bukkit.getPlayer(p.getUniqueId());
                if (player != null) {
                    GameMap map = MatchManager.get().getPlayerMap(player);
                    if (map != null && map.getTeamCard(player) == null) {
                        int size = 0;
                        for (TeamCard tc : map.getTeamCards()) {
                            if (tc.getPlayersSize() <= size && tc.getEmptySlots() != 0) {
                                tc.sendReservation(player, PlayerStat.getPlayerStats(player));
                                break;
                            }
                            size = tc.getPlayersSize();
                        }
                    }
                }
            }

        } else {
            if (MythicSkywars.getCfg().debugEnabled()) {
                MythicSkywars.get().getLogger().info("PartyAndFriendsHook::onSkyWarsJoin party is null!");
            }
        }

    }

}
