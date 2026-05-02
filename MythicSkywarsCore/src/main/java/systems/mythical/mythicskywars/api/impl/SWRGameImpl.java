package systems.mythical.mythicskywars.api.impl;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.api.SWRGameAPI;
import systems.mythical.mythicskywars.api.MythicSkywarsAPI;
import systems.mythical.mythicskywars.enums.GameType;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.managers.GameMapManager;
import systems.mythical.mythicskywars.managers.MatchManager;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class SWRGameImpl implements SWRGameAPI {

    private final MythicSkywarsAPI swrAPI;
    private final MatchManager matchManager;

    public SWRGameImpl(MythicSkywarsAPI swrAPIIn) {
        this.swrAPI = swrAPIIn;
        this.matchManager = MatchManager.get();
    }

    @Override
    public List<GameMap> getPlayableGames() {
        return MythicSkywars.getGameMapMgr().getPlayableArenas(GameType.ALL);
    }

    @Override
    public List<GameMap> getPlayableGames(GameType type) {
        return MythicSkywars.getGameMapMgr().getPlayableArenas(type);
    }

    @Override
    public List<GameMap> getGames() {
        return MythicSkywars.getGameMapMgr().getMapsCopy();
    }

    @Override
    public List<GameMap> getGames(GameType type) {
        List<GameMap> maps = MythicSkywars.getGameMapMgr().getMapsCopy();
        if (type == GameType.ALL) return maps;
        return maps.stream()
                .filter(gameMap ->
                        type == GameType.SINGLE ? gameMap.getTeamSize() == 1 : gameMap.getTeamSize() > 1)
                .collect(Collectors.toList());
    }

    @Override
    public GameMap getGame(String name) {
        return MythicSkywars.getGameMapMgr().getMap(name);
    }

    @Override
    public List<GameMap> getSortedGames() {
        return MythicSkywars.getGameMapMgr().getSortedArenas();
    }

    @Override
    public GameMap getGame(Player player) {
        return this.matchManager.getPlayerMap(player);
    }

    @Override
    public MatchManager getMatchManager() {
        return matchManager;
    }
}
