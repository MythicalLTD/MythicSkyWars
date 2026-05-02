package systems.mythical.mythicskywars.listeners;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.enums.MatchState;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.managers.MatchManager;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Blocks ground pickups after a match ends. Pickup events are registered reflectively so this class
 * loads on 1.8 (no {@code EntityPickupItemEvent}) and on 1.13+ (no {@code PlayerPickupItemEvent}).
 */
public final class ArenaPickupListener {

    private ArenaPickupListener() {
    }

    /**
     * Registers all pickup-related listeners supported by the current server.
     */
    public static void registerAll(JavaPlugin plugin) {
        registerLegacyPlayerPickup(plugin);
        registerModernEntityPickup(plugin);
        registerPaperAttemptPickup(plugin);
    }

    /**
     * 1.8–1.11 (and 1.12 fallback): {@code PlayerPickupItemEvent}.
     */
    @SuppressWarnings("unchecked")
    public static void registerLegacyPlayerPickup(JavaPlugin plugin) {
        try {
            Class<? extends Event> eventClass = (Class<? extends Event>) Class.forName("org.bukkit.event.player.PlayerPickupItemEvent");
            plugin.getServer().getPluginManager().registerEvent(
                    eventClass,
                    new Listener() {
                    },
                    EventPriority.HIGHEST,
                    (listener, event) -> {
                        try {
                            Player p = (Player) eventClass.getMethod("getPlayer").invoke(event);
                            if (shouldBlockPostGamePickup(p) && event instanceof Cancellable) {
                                ((Cancellable) event).setCancelled(true);
                            }
                        } catch (ReflectiveOperationException ignored) {
                        }
                    },
                    plugin,
                    false
            );
        } catch (ClassNotFoundException ignored) {
        }
    }

    /**
     * 1.12+ {@code EntityPickupItemEvent} (player entity only).
     */
    @SuppressWarnings("unchecked")
    public static void registerModernEntityPickup(JavaPlugin plugin) {
        try {
            Class<? extends Event> eventClass = (Class<? extends Event>) Class.forName("org.bukkit.event.entity.EntityPickupItemEvent");
            plugin.getServer().getPluginManager().registerEvent(
                    eventClass,
                    new Listener() {
                    },
                    EventPriority.HIGHEST,
                    (listener, event) -> {
                        try {
                            Object entity = eventClass.getMethod("getEntity").invoke(event);
                            if (!(entity instanceof Player)) {
                                return;
                            }
                            if (shouldBlockPostGamePickup((Player) entity) && event instanceof Cancellable) {
                                ((Cancellable) event).setCancelled(true);
                            }
                        } catch (ReflectiveOperationException ignored) {
                        }
                    },
                    plugin,
                    false
            );
        } catch (ClassNotFoundException ignored) {
        }
    }

    /**
     * Paper / Folia: register when available (not on compile classpath for Bukkit 1.12).
     */
    @SuppressWarnings("unchecked")
    public static void registerPaperAttemptPickup(JavaPlugin plugin) {
        String[] classNames = {
                "io.papermc.paper.event.player.PlayerAttemptPickupItemEvent",
                "com.destroystokyo.paper.event.player.PlayerAttemptPickupItemEvent",
        };
        for (String className : classNames) {
            try {
                Class<? extends Event> eventClass = (Class<? extends Event>) Class.forName(className);
                plugin.getServer().getPluginManager().registerEvent(
                        eventClass,
                        new Listener() {
                        },
                        EventPriority.HIGHEST,
                        (listener, event) -> {
                            if (!(event instanceof PlayerEvent) || !(event instanceof Cancellable)) {
                                return;
                            }
                            Player p = ((PlayerEvent) event).getPlayer();
                            if (shouldBlockPostGamePickup(p)) {
                                ((Cancellable) event).setCancelled(true);
                            }
                        },
                        plugin,
                        false
                );
                return;
            } catch (ClassNotFoundException ignored) {
                // try next package name
            }
        }
    }

    /**
     * True if this player must not receive ground loot (post-game screen).
     */
    public static boolean shouldBlockPostGamePickup(Player player) {
        if (player == null) {
            return false;
        }
        GameMap map = MatchManager.get().getPlayerMap(player);
        if (map != null && map.getMatchState() == MatchState.ENDING) {
            return true;
        }
        World pw = player.getWorld();
        if (MythicSkywars.getGameMapMgr() == null) {
            return false;
        }
        for (GameMap g : MythicSkywars.getGameMapMgr().getMapsCopy()) {
            if (g.getMatchState() != MatchState.ENDING) {
                continue;
            }
            World cw = g.getCurrentWorld();
            if (cw != null && cw.equals(pw)) {
                return true;
            }
        }
        return false;
    }
}
