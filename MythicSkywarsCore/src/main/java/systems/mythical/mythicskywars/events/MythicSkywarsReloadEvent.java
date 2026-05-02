package systems.mythical.mythicskywars.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MythicSkywarsReloadEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    public MythicSkywarsReloadEvent() { }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

}
