package systems.mythical.mythicskywars.game;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.managers.MatchManager;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ConcurrentLinkedQueue;


public class GameQueue {
    private ConcurrentLinkedQueue<PlayerCard> queue = new ConcurrentLinkedQueue<>();
    private GameMap map;
    private BukkitRunnable runnableQueue;

    GameQueue(GameMap g) {
        map = g;
    }

    public void add(PlayerCard pCard) {
        queue.add(pCard);
    }

    private void sendToGame() {
        if (MythicSkywars.get().isEnabled()) {
            final PlayerCard pCard = queue.poll();
            if (pCard == null) return;
            if (pCard.getUUID() == null) return;
            if (MythicSkywars.getCfg().debugEnabled()) {
                MythicSkywars.get().getLogger().info("#GameQueue:sendToGame: pCard uuid " + pCard.getUUID());
            }
            MatchManager.get().teleportToArena(map, pCard);
        }
    }

    public void start() {
        queue.clear();
        runnableQueue = new BukkitRunnable() {
            @Override
            public void run() {
                if (queue.isEmpty()) return;
                sendToGame();
            }
        };
        runnableQueue.runTaskTimer(MythicSkywars.get(), 0, 2);
    }

    public void kill() {
        if (runnableQueue != null) {
            runnableQueue.cancel();
            runnableQueue = null;
            queue.clear();
        }
    }

    public boolean containsPlayer(java.util.UUID uuid) {
        if (uuid == null) {
            return false;
        }
        for (PlayerCard card : queue) {
            if (card != null && uuid.equals(card.getUUID())) {
                return true;
            }
        }
        return false;
    }
}
