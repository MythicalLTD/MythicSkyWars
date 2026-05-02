package systems.mythical.mythicskywars.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MythicSkywarsReloadPreLoadEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    public MythicSkywarsReloadPreLoadEvent() { }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

}
