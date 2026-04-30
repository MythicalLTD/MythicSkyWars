package com.walrusone.skywarsreloaded.listeners;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.enums.MatchState;
import com.walrusone.skywarsreloaded.game.GameMap;
import com.walrusone.skywarsreloaded.game.PlayerCard;
import com.walrusone.skywarsreloaded.game.TeamCard;
import com.walrusone.skywarsreloaded.managers.MatchManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SummonedMobListener implements Listener {
    private static final String META_OWNER = "swr_summon_owner";
    private static final String META_MAP = "swr_summon_map";
    private static final String META_TEAM = "swr_summon_team";
    private static final long PENDING_EGG_MS = 3000L;

    private final Map<UUID, Long> pendingEggUse = new HashMap<>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onUseSpawnEgg(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        GameMap map = MatchManager.get().getPlayerMapSafe(player);
        if (map == null || map.getMatchState() != MatchState.PLAYING) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || !isSpawnEgg(item.getType())) {
            return;
        }
        pendingEggUse.put(player.getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobSpawn(CreatureSpawnEvent event) {
        if (event.getEntity() == null) {
            return;
        }
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
            return;
        }
        Player owner = findLikelyOwner(event.getLocation());
        if (owner == null) {
            return;
        }
        GameMap map = MatchManager.get().getPlayerMapSafe(owner);
        if (map == null || map.getMatchState() != MatchState.PLAYING) {
            return;
        }

        LivingEntity mob = event.getEntity();
        String ownerId = owner.getUniqueId().toString();
        String mapName = map.getName();
        TeamCard teamCard = map.getTeamCard(owner);
        String teamToken = teamCard != null ? String.valueOf(teamCard.getPosition()) : "";

        mob.setMetadata(META_OWNER, new FixedMetadataValue(SkyWarsReloaded.get(), ownerId));
        mob.setMetadata(META_MAP, new FixedMetadataValue(SkyWarsReloaded.get(), mapName));
        mob.setMetadata(META_TEAM, new FixedMetadataValue(SkyWarsReloaded.get(), teamToken));

        mob.setCustomNameVisible(true);
        mob.setCustomName(buildMobName(owner, map, teamCard, mob));

        Player enemy = findNearestEnemy(map, owner, teamCard, mob.getLocation());
        if (enemy != null && mob instanceof Creature) {
            ((Creature) mob).setTarget(enemy);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMobTarget(EntityTargetLivingEntityEvent event) {
        Entity source = event.getEntity();
        if (!(source instanceof Creature) || !source.hasMetadata(META_OWNER)) {
            return;
        }
        LivingEntity target = event.getTarget();
        if (!(target instanceof Player)) {
            return;
        }
        Player targetPlayer = (Player) target;

        UUID ownerUuid = parseUuid(source);
        if (ownerUuid == null) {
            return;
        }
        Player owner = Bukkit.getPlayer(ownerUuid);
        GameMap map = owner != null ? MatchManager.get().getPlayerMapSafe(owner) : MatchManager.get().getPlayerMapSafe(targetPlayer);
        if (map == null || map.getMatchState() != MatchState.PLAYING) {
            return;
        }

        TeamCard ownerTeam = owner != null ? map.getTeamCard(owner) : null;
        if (ownerUuid.equals(targetPlayer.getUniqueId()) || isOnTeam(ownerTeam, targetPlayer)) {
            event.setCancelled(true);
            Player enemy = findNearestEnemy(map, owner, ownerTeam, source.getLocation());
            if (enemy != null) {
                ((Creature) source).setTarget(enemy);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity == null || !entity.hasMetadata(META_OWNER)) {
            return;
        }
        entity.removeMetadata(META_OWNER, SkyWarsReloaded.get());
        entity.removeMetadata(META_MAP, SkyWarsReloaded.get());
        entity.removeMetadata(META_TEAM, SkyWarsReloaded.get());
    }

    private Player findLikelyOwner(org.bukkit.Location spawnLocation) {
        long now = System.currentTimeMillis();
        Player best = null;
        double closest = Double.MAX_VALUE;
        List<UUID> expired = new ArrayList<>();
        for (Map.Entry<UUID, Long> entry : pendingEggUse.entrySet()) {
            if (now - entry.getValue() > PENDING_EGG_MS) {
                expired.add(entry.getKey());
                continue;
            }
            Player player = SkyWarsReloaded.get().getServer().getPlayer(entry.getKey());
            if (player == null || !player.isOnline() || player.getWorld() != spawnLocation.getWorld()) {
                continue;
            }
            double distance = player.getLocation().distanceSquared(spawnLocation);
            if (distance < closest && distance <= 64.0D) {
                closest = distance;
                best = player;
            }
        }
        for (UUID uuid : expired) {
            pendingEggUse.remove(uuid);
        }
        if (best != null) {
            pendingEggUse.remove(best.getUniqueId());
        }
        return best;
    }

    private Player findNearestEnemy(GameMap map, Player owner, TeamCard ownerTeam, org.bukkit.Location source) {
        double bestDistance = Double.MAX_VALUE;
        Player selected = null;
        for (Player alive : map.getAlivePlayers()) {
            if (alive == null || !alive.isOnline()) {
                continue;
            }
            if (owner != null && alive.getUniqueId().equals(owner.getUniqueId())) {
                continue;
            }
            if (isOnTeam(ownerTeam, alive)) {
                continue;
            }
            double dist = alive.getLocation().distanceSquared(source);
            if (dist < bestDistance) {
                bestDistance = dist;
                selected = alive;
            }
        }
        return selected;
    }

    private String buildMobName(Player owner, GameMap map, TeamCard teamCard, LivingEntity mob) {
        String mobName = formatEntityName(mob.getType().name());
        if (teamCard == null || teamCard.getSize() <= 1) {
            return ChatColor.YELLOW + owner.getName() + "'s " + mobName;
        }

        String teammateName = null;
        for (PlayerCard playerCard : teamCard.getPlayerCards()) {
            Player teammate = SkyWarsReloaded.get().getServer().getPlayer(playerCard.getUUID());
            if (teammate != null && teammate.isOnline() && !teammate.getUniqueId().equals(owner.getUniqueId())) {
                teammateName = teammate.getName();
                break;
            }
        }
        if (teammateName == null) {
            return ChatColor.YELLOW + owner.getName() + "'s " + mobName;
        }
        return ChatColor.YELLOW + owner.getName() + " & " + teammateName + "'s " + mobName;
    }

    private boolean isOnTeam(TeamCard teamCard, Player player) {
        if (teamCard == null || player == null) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        for (PlayerCard playerCard : teamCard.getPlayerCards()) {
            if (playerId.equals(playerCard.getUUID())) {
                return true;
            }
        }
        return false;
    }

    private boolean isSpawnEgg(Material material) {
        String typeName = material.name();
        return "MONSTER_EGG".equals(typeName) || "SPAWN_EGG".equals(typeName);
    }

    private UUID parseUuid(Entity entity) {
        try {
            return UUID.fromString(entity.getMetadata(META_OWNER).get(0).asString());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String formatEntityName(String enumName) {
        String lowered = enumName.toLowerCase().replace('_', ' ');
        String[] words = lowered.split(" ");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }
}
