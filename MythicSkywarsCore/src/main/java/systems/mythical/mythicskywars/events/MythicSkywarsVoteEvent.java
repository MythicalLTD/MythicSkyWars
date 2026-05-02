package systems.mythical.mythicskywars.events;

import systems.mythical.mythicskywars.enums.Vote;
import systems.mythical.mythicskywars.game.GameMap;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MythicSkywarsVoteEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private Player player;
    private GameMap map;
    private Vote vote;

    public MythicSkywarsVoteEvent(Player p, GameMap game, Vote vote) {
        this.player = p;
        this.map = game;
        this.vote = vote;
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

    public Vote getVote() {
        return vote;
    }

}
