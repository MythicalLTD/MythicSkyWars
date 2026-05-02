package systems.mythical.mythicskywars.game.cages.schematics;

import com.google.common.collect.Lists;
import com.sk89q.worldedit.EditSession;
import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.game.PlayerCard;
import systems.mythical.mythicskywars.game.TeamCard;
import systems.mythical.mythicskywars.managers.PlayerStat;
import systems.mythical.mythicskywars.menus.gameoptions.objects.CoordLoc;
import systems.mythical.mythicskywars.utilities.Util;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class SchematicCage {

    public static HashMap<GameMap, HashMap<UUID, EditSession>> pastedSessions = new HashMap<>();

    public List<File> getSchematics() {
        List<File> files = Lists.newArrayList();
        File folder = new File(MythicSkywars.get().getDataFolder(), "cages");
        if (folder.exists() && folder.isDirectory()) {
            for (File f : Objects.requireNonNull(folder.listFiles())) {
                if (f.getName().endsWith(".schematic") || f.getName().endsWith(".schem")) {
                    files.add(f);
                }
            }
        }
        return files;
    }

    public boolean createSpawnPlatform(GameMap map, Player player) {
        if (!Bukkit.getPluginManager().isPluginEnabled("WorldEdit")) {
            return false;
        }

        PlayerStat pStat = PlayerStat.getPlayerStats(player);
        if (pStat != null) {
            if (map.getTeamSize() == 1 || MythicSkywars.getCfg().isUseSeparateCages()) {
                if (pastedSessions.containsKey(map)) {
                    if (pastedSessions.get(map).containsKey(player.getUniqueId())) {
                        return true;
                    }
                }


                String cage = pStat.getGlassColor();
                if (cage != null && cage.startsWith("custom-")) {
                    cage = cage.replace("custom-", "");
                    File schematicFile = null;
                    for (File f : getSchematics()) {
                        if (f.getName().equals(cage + ".schematic") || f.getName().equals(cage + ".schem")) {
                            schematicFile = f;
                        }
                    }

                    if (schematicFile == null) {
                        return false;
                    }

                    PlayerCard team = map.getPlayerCard(player);
                    CoordLoc spawn = team.getSpawn();

                    if (MythicSkywars.getCfg().debugEnabled()) {
                        Util.get().logToFile("SWR[" + map.getName() + "] Now pasting the cage for player " + player.getName() + " with schematic " + schematicFile.getName());
                    }
                    if (MythicSkywars.getNMS().getVersion() < 13) {
                        new Schematic12().pasteSchematic(schematicFile, map, spawn, player);
                    } else {
                        new Schematic13().pasteSchematic(schematicFile, map, spawn, player);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public void removeSpawnPlatform(GameMap map, Player player) {
        if (player==null || map == null) { return; }
        if (MythicSkywars.getCfg().debugEnabled()) {
            Util.get().logToFile("SWR[" + map.getName() + "] Removing the cage of player in 5 ticks" + player.getName());
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                if (pastedSessions != null && pastedSessions.containsKey(map)) {
                    if (pastedSessions.get(map) != null && pastedSessions.get(map).containsKey(player.getUniqueId())) {
                        EditSession session = pastedSessions.get(map).get(player.getUniqueId());
                        session.undo(session);
                        pastedSessions.get(map).remove(player.getUniqueId());
                        if (MythicSkywars.getCfg().debugEnabled()) {
                            Util.get().logToFile("SWR[" + map.getName() + "] Cage of " + player.getName() + " has successfully been removed");
                        }
                    }
                }
            }
        }.runTaskLater(MythicSkywars.get(), 5);
    }

}
