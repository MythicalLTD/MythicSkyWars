package systems.mythical.mythicskywars.commands.admin;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores the target player chosen with {@code /sw select} for use by {@code /sw send <map>}.
 */
public final class AdminSendSelection {

    private static final ConcurrentHashMap<UUID, UUID> SENDER_TO_TARGET = new ConcurrentHashMap<>();

    private AdminSendSelection() {
    }

    public static void setSelection(UUID sender, UUID targetPlayer) {
        SENDER_TO_TARGET.put(sender, targetPlayer);
    }

    public static UUID getSelected(UUID sender) {
        return SENDER_TO_TARGET.get(sender);
    }

    public static void clearSelection(UUID sender) {
        SENDER_TO_TARGET.remove(sender);
    }
}
