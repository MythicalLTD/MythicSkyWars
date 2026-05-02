package systems.mythical.mythicskywars.listeners;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.managers.MatchManager;
import systems.mythical.mythicskywars.managers.PlayerStat;
import systems.mythical.mythicskywars.menus.playeroptions.ProjectileEffectOption;
import systems.mythical.mythicskywars.menus.playeroptions.objects.ParticleEffect;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileLaunchEvent;

public class ParticleEffectListener implements org.bukkit.event.Listener {
    public ParticleEffectListener() {
    }

    @EventHandler
    public void projectileLaunch(ProjectileLaunchEvent e) {
        Projectile projectile = e.getEntity();
        if ((((projectile instanceof Snowball)) || ((projectile instanceof Egg)) || ((projectile instanceof Arrow))) &&
                ((projectile.getShooter() instanceof Player))) {
            Player player = (Player) projectile.getShooter();
            GameMap gMap = MatchManager.get().getPlayerMap(player);
            if (gMap != null) {
                PlayerStat ps = PlayerStat.getPlayerStats(player.getUniqueId());
                if (ps != null) {
                    String key = ps.getProjectileEffect();
                    ProjectileEffectOption peo = (ProjectileEffectOption) ProjectileEffectOption.getPlayerOptionByKey(key);
                    if (peo != null) {
                        java.util.List<ParticleEffect> effects = peo.getEffects();
                        if ((key != null) && (effects != null) &&
                                (!key.equalsIgnoreCase("none"))) {
                            MythicSkywars.getOM().addProjectile(projectile, effects);
                        }
                    }
                }
            }
        }
    }
}
