package systems.mythical.mythicskywars.api;

import systems.mythical.mythicskywars.enums.GameType;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.managers.MatchManager;
import org.bukkit.entity.Player;

import java.util.List;

public interface SWRGameAPI {

    /**
     * Get a list of games that are available to play on.
     * @return List GameMap with found games
     */
    List<GameMap> getPlayableGames();

    /**
     * Get a list of playable games with the target game type.
     * @param type GameType of games to search for
     * @return List GameMap with found games
     */
    List<GameMap> getPlayableGames(GameType type);

    /**
     * Get a list of all games available.
     * @return List GameMap of found games
     */
    List<GameMap> getGames();

    /**
     * Get a list of all games with the target GameType.
     * @param type GameType of games to search for.
     * @return List GameMap of found games
     */
    List<GameMap> getGames(GameType type);

    /**
     * Get a game with the target name.
     * @param name Name of the arena.
     * @return null if not found, GameMap if found.
     */
    GameMap getGame(String name);

    /**
     * Get a list of games sorted on their names.
     * @return Sorted List GameMap
     */
    List<GameMap> getSortedGames();

    /**
     * Get the game a player is in.
     * @param player Player to search up
     * @return GameMap of target player
     */
    GameMap getGame(Player player);

    /**
     * Get the MatchManager class instance.
     * @return MatchManager instance.
     */
    MatchManager getMatchManager();

}
