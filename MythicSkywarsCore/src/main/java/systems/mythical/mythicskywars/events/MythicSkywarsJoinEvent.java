package systems.mythical.mythicskywars.events;

import systems.mythical.mythicskywars.game.GameMap;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MythicSkywarsJoinEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private Player player;
    private GameMap map;

    public MythicSkywarsJoinEvent(Player p, GameMap game) {
        this.player = p;
        this.map = game;
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
}
