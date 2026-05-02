package systems.mythical.mythicskywars.managers;

import com.google.common.collect.Maps;
import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.enums.GameType;
import systems.mythical.mythicskywars.game.Crate;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.menus.playeroptions.*;
import systems.mythical.mythicskywars.menus.playeroptions.objects.ParticleEffect;
import systems.mythical.mythicskywars.utilities.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class PlayerOptionsManager {

    private final Map<Projectile, List<ParticleEffect>> projectileMap = Maps.newConcurrentMap();
    private final Map<UUID, List<ParticleEffect>> playerMap = Maps.newConcurrentMap();
    private final List<ParticleEffect> crateEffects = new ArrayList<>();

    public PlayerOptionsManager() {

        if (MythicSkywars.getCfg().particlesEnabled()) {
            crateEffects.add(new ParticleEffect("CRIT", 0, 2, 0, 8, 4));
            crateEffects.add(new ParticleEffect("CRIT_MAGIC", 0, 2, 0, 8, 4));
            MythicSkywars.get().getServer().getScheduler().scheduleSyncRepeatingTask(MythicSkywars.get(), () -> {
                for (Projectile projectile : projectileMap.keySet()) {
                    if (projectile.isDead()) {
                        projectileMap.remove(projectile);
                    } else {
                        List<ParticleEffect> effects = projectileMap.get(projectile);
                        doEffects(projectile.getLocation(), effects, true);
                    }
                }
                for (UUID p : playerMap.keySet()) {
                    Player player = Bukkit.getPlayer(p);
                    if (player == null) {
                        playerMap.remove(p);
                    } else {
                        List<ParticleEffect> effects = playerMap.get(p);
                        doEffects(player.getLocation(), effects, false);
                    }
                }

                for (GameMap gMap : MythicSkywars.getGameMapMgr().getPlayableArenas(GameType.ALL)) {
                    for (Crate crate : gMap.getCrates()) {
                        doEffects(crate.getEntity().getLocation(), crateEffects, false);
                    }
                }
            }, MythicSkywars.getCfg().getTicksPerUpdate(), MythicSkywars.getCfg().getTicksPerUpdate());
        }
        GlassColorOption.loadPlayerOptions();
        ParticleEffectOption.loadPlayerOptions();
        ProjectileEffectOption.loadPlayerOptions();
        WinSoundOption.loadPlayerOptions();
        KillSoundOption.loadPlayerOptions();
        TauntOption.loadPlayerOptions();
    }

    /*Handles projectile effects*/

    public void addProjectile(Projectile p, List<ParticleEffect> e) {
        projectileMap.put(p, e);
    }

    void addPlayer(UUID p, List<ParticleEffect> e) {
        playerMap.put(p, e);
    }

    void removePlayer(UUID p) {
        playerMap.remove(p);
    }

    private void doEffects(Location location, List<ParticleEffect> effects, boolean isProjectile) {
        Random random = new Random();
        if (isProjectile) {
            for (ParticleEffect p : effects) {
                Util.get().sendParticles(location.getWorld(), p.getType(), (float) location.getX(), (float) location.getY(), (float) location.getZ(), 0, 0, 0, getData(p), 2);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < 3; i++) {
                            Util.get().sendParticles(location.getWorld(), p.getType(), (float) location.getX(), (float) location.getY(), (float) location.getZ(), (float) (random.nextFloat() * (0.5 - -0.5) + -0.5), (float) (random.nextFloat() * (0.5 - -0.5) + -0.5), (float) (random.nextFloat() * (0.5 - -0.5) + -0.5), getData(p), 1);
                        }
                    }
                }.runTaskLater(MythicSkywars.get(), 3);
            }
        } else {
            for (ParticleEffect p : effects) {
                Util.get().sendParticles(location.getWorld(), p.getType(), (float) location.getX(), (float) location.getY(), (float) location.getZ(), random.nextFloat(), random.nextFloat() * (p.getOffsetYU() - p.getOffsetYL()) + p.getOffsetYL(), random.nextFloat(), getData(p), random.nextInt((p.getAmountU() - p.getAmountL()) + p.getAmountL()) + 1);
            }
        }
    }

    private float getData(ParticleEffect p) {
        Random random = new Random();
        float data = p.getData();
        if (p.getData() == -1) {
            data = random.nextFloat();
        }
        return data;
    }

}