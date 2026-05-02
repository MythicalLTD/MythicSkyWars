package systems.mythical.mythicskywars.events;

import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.game.TeamCard;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MythicSkywarsSelectTeamEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private Player player;
    private GameMap map;
    private TeamCard team;

    public MythicSkywarsSelectTeamEvent(Player p, GameMap game, TeamCard team) {
        this.player = p;
        this.map = game;
        this.team = team;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public Player getPlayer() {
        return player;
    }

    public GameMap getGame() {
        return map;
    }

    public TeamCard getTeam() {
        return team;
    }
}
