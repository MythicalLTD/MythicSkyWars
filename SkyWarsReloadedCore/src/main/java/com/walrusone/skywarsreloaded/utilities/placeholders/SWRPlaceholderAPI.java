package com.walrusone.skywarsreloaded.utilities.placeholders;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.enums.LeaderType;
import com.walrusone.skywarsreloaded.enums.MatchState;
import com.walrusone.skywarsreloaded.game.GameMap;
import com.walrusone.skywarsreloaded.managers.Leaderboard;
import com.walrusone.skywarsreloaded.managers.PlayerStat;
import com.walrusone.skywarsreloaded.utilities.LevelManager;
import com.walrusone.skywarsreloaded.utilities.PrestigeManager;
import com.walrusone.skywarsreloaded.utilities.Util;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

/**
 * PlaceholderAPI expansion {@code swr}. Player placeholders (require an online player context):
 * <ul>
 *   <li>{@code %swr_wins%}, {@code %swr_losses%}, {@code %swr_kills%}, {@code %swr_deaths%}, {@code %swr_xp%}</li>
 *   <li>{@code %swr_games_played%}, {@code %swr_games%} — wins + losses</li>
 *   <li>{@code %swr_kill_death%} — kills ÷ deaths (0 deaths → shows kills as {@code n.00}; both 0 → {@code 0.00})</li>
 *   <li>{@code %swr_win_loss%} — wins ÷ losses (0 losses → {@code 0.00} unless wins only, then wins as {@code n.00})</li>
 *   <li>{@code %swr_level%}</li>
 *   <li>{@code %swr_prestige_prefix%}, {@code %swr_level_display_prefix%}, {@code %swr_prestige_id%}</li>
 *   <li>{@code %swr_time%}, {@code %swr_players_playing%}, {@code %swr_players_waiting%}, …</li>
 * </ul>
 */
public class SWRPlaceholderAPI extends PlaceholderExpansion {

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String getIdentifier() {
        return "swr";
    }

    @Override
    public String getAuthor() {
        return "Devmart";
    }

    @Override
    public String getVersion() {
        return SkyWarsReloaded.get().getDescription().getVersion();
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player p, String identifier) {
        if (p == null) {
            return "";
        }
        if (identifier == null || identifier.isEmpty()) {
            return null;
        }
        String id = identifier.toLowerCase(Locale.ROOT);

        if ("time".equals(id)) {
            GameMap gameMap = SkyWarsReloaded.get().getMatchManager().getPlayerMap(p);
            if (gameMap != null && gameMap.getMatchState() == MatchState.PLAYING) {
                return "" + gameMap.getTimer();
            }
            return "0";
        }
        if ("players_playing".equals(id)) {
            int total = 0;
            for (GameMap map : SkyWarsReloaded.getGameMapMgr().getMapsCopy()) {
                if (map.getMatchState() == MatchState.PLAYING) {
                    total += map.getAlivePlayers().size();
                }
            }
            return String.valueOf(total);
        }
        if ("players_waiting".equals(id)) {
            int total = 0;
            for (GameMap map : SkyWarsReloaded.getGameMapMgr().getMapsCopy()) {
                MatchState state = map.getMatchState();
                if (state == MatchState.WAITINGLOBBY || state == MatchState.WAITINGSTART) {
                    total += map.getAllPlayers().size();
                }
            }
            return String.valueOf(total);
        }
        if ("players_playing_solo".equals(id)) {
            int total = 0;
            for (GameMap map : SkyWarsReloaded.getGameMapMgr().getMapsCopy()) {
                if (map.getTeamSize() == 1 && map.getMatchState() == MatchState.PLAYING) {
                    total += map.getAlivePlayers().size();
                }
            }
            return String.valueOf(total);
        }
        if ("players_playing_team".equals(id)) {
            int total = 0;
            for (GameMap map : SkyWarsReloaded.getGameMapMgr().getMapsCopy()) {
                if (map.getTeamSize() > 1 && map.getMatchState() == MatchState.PLAYING) {
                    total += map.getAlivePlayers().size();
                }
            }
            return String.valueOf(total);
        }
        if ("players_waiting_solo".equals(id)) {
            int total = 0;
            for (GameMap map : SkyWarsReloaded.getGameMapMgr().getMapsCopy()) {
                if (map.getTeamSize() == 1) {
                    MatchState state = map.getMatchState();
                    if (state == MatchState.WAITINGLOBBY || state == MatchState.WAITINGSTART) {
                        total += map.getAllPlayers().size();
                    }
                }
            }
            return String.valueOf(total);
        }
        if ("players_waiting_team".equals(id)) {
            int total = 0;
            for (GameMap map : SkyWarsReloaded.getGameMapMgr().getMapsCopy()) {
                if (map.getTeamSize() > 1) {
                    MatchState state = map.getMatchState();
                    if (state == MatchState.WAITINGLOBBY || state == MatchState.WAITINGSTART) {
                        total += map.getAllPlayers().size();
                    }
                }
            }
            return String.valueOf(total);
        }

        PlayerStat stat = SkyWarsReloaded.get().getPlayerStat(p);
        if (stat == null) {
            if ("kill_death".equals(id) || "win_loss".equals(id)) {
                return "0.00";
            }
            if ("level".equals(id)) {
                return "" + Util.get().getPlayerLevel(p, false);
            }
            if ("prestige_prefix".equals(id) || "level_display_prefix".equals(id)) {
                return "";
            }
            if ("prestige_id".equals(id)) {
                return "icon1";
            }
            if ("wins".equals(id) || "losses".equals(id) || "kills".equals(id) || "deaths".equals(id) || "xp".equals(id)
                    || "souls".equals(id) || "total_souls".equals(id)
                    || "soulwell_usages".equals(id) || "soul_well_usages".equals(id)
                    || "soulwell_legendaries".equals(id) || "soul_well_legendaries".equals(id)
                    || "soulwell_rares".equals(id) || "soul_well_rares".equals(id)
                    || "soulwell_souls_gathered".equals(id) || "souls_gathered".equals(id)
                    || "soulwell_souls_purchased".equals(id) || "souls_purchased".equals(id)
                    || "games_played".equals(id) || "games".equals(id)) {
                return "0";
            }
            return null;
        }

        if ("wins".equals(id)) {
            return Integer.toString(stat.getWins());
        }
        if ("losses".equals(id)) {
            return Integer.toString(stat.getLosses());
        }
        if ("kills".equals(id)) {
            return Integer.toString(stat.getKills());
        }
        if ("deaths".equals(id)) {
            return Integer.toString(stat.getDeaths());
        }
        if ("xp".equals(id)) {
            return Integer.toString(stat.getXp());
        }
        if ("souls".equals(id) || "total_souls".equals(id)) {
            return Integer.toString(stat.getSouls());
        }
        if ("soulwell_usages".equals(id) || "soul_well_usages".equals(id)) {
            return Integer.toString(stat.getSoulWellUsages());
        }
        if ("soulwell_legendaries".equals(id) || "soul_well_legendaries".equals(id)) {
            return Integer.toString(stat.getSoulWellLegendaries());
        }
        if ("soulwell_rares".equals(id) || "soul_well_rares".equals(id)) {
            return Integer.toString(stat.getSoulWellRares());
        }
        if ("soulwell_souls_gathered".equals(id) || "souls_gathered".equals(id)) {
            return Integer.toString(stat.getSoulWellSoulsGathered());
        }
        if ("soulwell_souls_purchased".equals(id) || "souls_purchased".equals(id)) {
            return Integer.toString(stat.getSoulWellSoulsPurchased());
        }
        if ("games_played".equals(id) || "games".equals(id)) {
            return Integer.toString(stat.getWins() + stat.getLosses());
        }
        if ("level".equals(id)) {
            return Integer.toString(Util.get().getPlayerLevel(p, false));
        }
        int swLevel = LevelManager.get().getLevelForXp(stat.getXp());
        if ("prestige_id".equals(id)) {
            return stat.getPrestigeIcon();
        }
        if ("prestige_prefix".equals(id)) {
            return PrestigeManager.get().isEnabled()
                    ? PrestigeManager.get().translatePrefixForStat(stat, swLevel, stat.getPrestigeIcon()) : "";
        }
        if ("level_display_prefix".equals(id)) {
            return LevelManager.get().getDisplayPrefixForPlayer(stat, p, swLevel);
        }
        if ("kill_death".equals(id)) {
            return formatKillDeathRatio(stat);
        }
        if ("win_loss".equals(id)) {
            return formatWinLossRatio(stat);
        }
        return null;
    }

    private static String formatKillDeathRatio(PlayerStat stat) {
        int kills = stat.getKills();
        int deaths = stat.getDeaths();
        if (deaths <= 0) {
            if (kills <= 0) {
                return "0.00";
            }
            return String.format(Locale.US, "%1$,.2f", (double) kills);
        }
        return String.format(Locale.US, "%1$,.2f", (double) kills / (double) deaths);
    }

    private static String formatWinLossRatio(PlayerStat stat) {
        int wins = stat.getWins();
        int losses = stat.getLosses();
        if (losses <= 0) {
            if (wins <= 0) {
                return "0.00";
            }
            return String.format(Locale.US, "%1$,.2f", (double) wins);
        }
        return String.format(Locale.US, "%1$,.2f", (double) wins / (double) losses);
    }

    public static String getLeaderBoardVariable(String var, @Nullable LeaderType type) {
        String[] parts = var.split("_");
        if (SkyWarsReloaded.getLB() != null) {
            List<Leaderboard.LeaderData> topList = SkyWarsReloaded.getLB().getTopList(type);

            if (topList != null && Util.get().isInteger(parts[1])) {
                int playerLeaderboardRank;
                try {
                    playerLeaderboardRank = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    return "INVALID-INPUT-(NAN)";
                }

                int playerLeaderboardIndex = playerLeaderboardRank - 1;
                if (playerLeaderboardIndex < 0) {
                    return "INVALID-INPUT-(RANK<1)";
                }

                if (topList.size() > playerLeaderboardIndex) {
                    String firstPartLower = parts[0].toLowerCase(Locale.ROOT);
                    Leaderboard.LeaderData row = topList.get(playerLeaderboardIndex);

                    switch (firstPartLower) {
                        case "wins":
                            return "" + row.getWins();
                        case "losses":
                            return "" + row.getLoses();
                        case "kills":
                            return "" + row.getKills();
                        case "deaths":
                            return "" + row.getDeaths();
                        case "xp":
                            return "" + row.getXp();
                        case "player":
                            return "" + row.getName();
                        case "games_played":
                        case "games":
                            return "" + (row.getLoses() + row.getWins());
                        case "kill_death": {
                            int k = row.getKills();
                            int d = row.getDeaths();
                            if (d <= 0) {
                                return k <= 0 ? "0.00" : String.format(Locale.US, "%1$,.2f", (double) k);
                            }
                            return String.format(Locale.US, "%1$,.2f", (double) k / (double) d);
                        }
                        case "win_loss": {
                            int w = row.getWins();
                            int l = row.getLoses();
                            if (l <= 0) {
                                return w <= 0 ? "0.00" : String.format(Locale.US, "%1$,.2f", (double) w);
                            }
                            return String.format(Locale.US, "%1$,.2f", (double) w / (double) l);
                        }
                        default:
                            break;
                    }
                }
            }
        }
        return "NO DATA";
    }
}
