package systems.mythical.mythicskywars.game.cages;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.enums.MatchState;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.game.PlayerCard;
import systems.mythical.mythicskywars.game.TeamCard;
import systems.mythical.mythicskywars.game.cages.schematics.SchematicCage;
import systems.mythical.mythicskywars.managers.PlayerStat;
import systems.mythical.mythicskywars.menus.gameoptions.objects.CoordLoc;
import systems.mythical.mythicskywars.menus.playeroptions.GlassColorOption;
import systems.mythical.mythicskywars.utilities.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public abstract class Cage {

    ArrayList<CoordLoc> bottomCoordOffsets = new ArrayList<>();
    ArrayList<CoordLoc> middleCoordOffsets = new ArrayList<>();
    ArrayList<CoordLoc> topCoordOffsets = new ArrayList<>();
    CageType cageType;

    private boolean canScheduleTasks() {
        MythicSkywars plugin = MythicSkywars.get();
        return plugin != null && plugin.isEnabled();
    }

    public void createSpawnPlatforms(GameMap gMap) {
        World world = gMap.getCurrentWorld();
        for (List<CoordLoc> coords : gMap.getSpawnLocations().values()) {
            for (CoordLoc loc1 : coords) {
                int x = loc1.getX();
                int y = loc1.getY();
                int z = loc1.getZ();
                for (CoordLoc loc : bottomCoordOffsets) {
                    world.getBlockAt(x + loc.getX(), y + loc.getY(), z + loc.getZ()).setType(Material.GLASS);
                }
                Runnable middleLayer = () -> {
                    for (CoordLoc loc : middleCoordOffsets) {
                        world.getBlockAt(x + loc.getX(), y + loc.getY(), z + loc.getZ()).setType(Material.GLASS);
                    }
                };
                Runnable topLayer = () -> {
                    for (CoordLoc loc : topCoordOffsets) {
                        world.getBlockAt(x + loc.getX(), y + loc.getY(), z + loc.getZ()).setType(Material.GLASS);
                    }
                };
                if (canScheduleTasks()) {
                    Bukkit.getScheduler().runTaskLater(MythicSkywars.get(), middleLayer, 7L);
                    Bukkit.getScheduler().runTaskLater(MythicSkywars.get(), topLayer, 14L);
                } else {
                    middleLayer.run();
                    topLayer.run();
                }
            }
        }
    }

    public boolean setGlassColor(GameMap gMap, TeamCard tCard) {
        if (gMap.getMatchState() == MatchState.WAITINGSTART) {
            if (tCard != null) {
                //for (CoordLoc loc1 : tCard.getSpawn()) {
                    World world = gMap.getCurrentWorld();
                    Random rand = new Random();

                    ArrayList<MaterialWithByte> colors = new ArrayList<>();

                    if (gMap.getTeamSize() > 1 && !MythicSkywars.getCfg().usePlayerGlassColors()) {
                        colors.add(new MaterialWithByte(MythicSkywars.getNMS().getColorItem(MythicSkywars.getCfg().getTeamMaterial(), tCard.getByte()).getType(), tCard.getByte()));
                    } else {
                        for (PlayerCard p : tCard.getPlayerCards()) {
                            Player player = p.getPlayer();
                            if (player != null) {
                                PlayerStat pStat = PlayerStat.getPlayerStats(player);
                                if (pStat != null) {
                                    String col = pStat.getGlassColor() == null ? "none" : pStat.getGlassColor().toLowerCase();
                                    byte cByte = Util.get().getByteFromColor(col);
                                    GlassColorOption color = (GlassColorOption) GlassColorOption.getPlayerOptionByKey(col);
                                    if (MythicSkywars.getCfg().isUseSeparateCages()) {
                                        colors.clear();
                                    }
                                    if (color != null) {
                                        colors.add(new MaterialWithByte(color.getItem().getType(), cByte));
                                    } else {
                                        if (cByte <= -1) {
                                            colors.add(new MaterialWithByte(Material.GLASS, cByte));
                                        } else {
                                            colors.add(new MaterialWithByte(MythicSkywars.getNMS().getColorItem("STAINED_GLASS", cByte).getType(), cByte));
                                        }
                                    }

                                    CoordLoc loc1 = p.getSpawn();
                                    int x = loc1.getX();
                                    int y = loc1.getY();
                                    int z = loc1.getZ();

                                    for (CoordLoc loc : bottomCoordOffsets) {
                                        setBlockColor(loc, x, y, z, world, colors.get(rand.nextInt(colors.size())));
                                    }
                                    for (CoordLoc loc : middleCoordOffsets) {
                                        setBlockColor(loc, x, y, z, world, colors.get(rand.nextInt(colors.size())));
                                    }
                                    for (CoordLoc loc : topCoordOffsets) {
                                        setBlockColor(loc, x, y, z, world, colors.get(rand.nextInt(colors.size())));
                                    }
                                }
                            }
                        }
                    }
                    return true;
                //}
            }
        }
        return false;
    }

    private void setBlockColor(CoordLoc loc, int x, int y, int z, World world, MaterialWithByte materialWithByte) {
        if (world == null) return;
        if (materialWithByte.cByte <= -1) {
            world.getBlockAt(x + loc.getX(), y + loc.getY(), z + loc.getZ()).setType(materialWithByte.mat);
        } else {
            MythicSkywars.getNMS().setBlockWithColor(world, x + loc.getX(), y + loc.getY(), z + loc.getZ(), materialWithByte.mat, materialWithByte.cByte);
        }
    }

    public void removeSpawnHousing(GameMap gMap) {
        World world = gMap.getCurrentWorld();
        gMap.setAllowFallDamage(false);
        Runnable enableFallDamage = () -> gMap.setAllowFallDamage(true);
        if (canScheduleTasks()) {
            Bukkit.getScheduler().runTaskLater(MythicSkywars.get(), enableFallDamage, 100L);
        } else {
            enableFallDamage.run();
        }
        if (MythicSkywars.getCfg().debugEnabled()) {
            Util.get().logToFile("SWR[" + gMap.getName() + "] Now removing all player cage");
        }
        for (TeamCard tCard : gMap.getTeamCards()) {
            Runnable removeTeamCage = () -> {
                if (!MythicSkywars.get().isEnabled()) return;
                removeSpawnHousing(gMap, tCard, true);
            };
            if (canScheduleTasks()) {
                Bukkit.getScheduler().runTaskLater(MythicSkywars.get(), removeTeamCage, 10L);
            } else {
                removeTeamCage.run();
            }

        }
    }


    public void removeSpawnHousing(GameMap gMap, CoordLoc loc1) {
        if (loc1 == null) return;
        World world = gMap.getCurrentWorld();
        if (world == null) {
            MythicSkywars.get().getLogger().severe("The world " + gMap.getName() + " is not loaded! Failed to remove spawn housing.");
            return;
        }

        int x = loc1.getX();
        int y = loc1.getY();
        int z = loc1.getZ();

        for (CoordLoc loc : bottomCoordOffsets) {
            world.getBlockAt(x + loc.getX(), y + loc.getY(), z + loc.getZ()).setType(Material.AIR);
        }
        for (CoordLoc loc : middleCoordOffsets) {
            world.getBlockAt(x + loc.getX(), y + loc.getY(), z + loc.getZ()).setType(Material.AIR);
        }
        for (CoordLoc loc : topCoordOffsets) {
            world.getBlockAt(x + loc.getX(), y + loc.getY(), z + loc.getZ()).setType(Material.AIR);
        }
    }

    public void removeSpawnHousing(GameMap gMap, PlayerCard pCard, boolean gameStarted) {
        if (gameStarted) {
            if (gMap.getTeamSize() == 1 || MythicSkywars.getCfg().isUseSeparateCages()) {
                // todo test this
                if (Bukkit.getPluginManager().isPluginEnabled("WorldEdit")) {
                    new SchematicCage().removeSpawnPlatform(gMap, pCard.getPlayer());
                }
            }
        }
        removeSpawnHousing(gMap, pCard.getSpawn());
    }

    public void removeSpawnHousing(GameMap gMap, TeamCard tCard, boolean gameStarted) {
        if (tCard != null) {
            if (tCard.getPlayerCards() != null) {
                for (PlayerCard pCard : tCard.getPlayerCards()) {
                    removeSpawnHousing(gMap, pCard, gameStarted);
                }
            }

            for (CoordLoc loc : tCard.getSpawns()) {
                Runnable removeSpawn = () -> removeSpawnHousing(gMap, loc);
                if (canScheduleTasks()) {
                    Bukkit.getScheduler().runTask(MythicSkywars.get(), removeSpawn);
                } else {
                    removeSpawn.run();
                }
            }
        }
    }

    public CageType getType() {
        return cageType;
    }

    private class MaterialWithByte {
        private Material mat;
        private byte cByte;

        MaterialWithByte(Material mat, byte cByte) {
            this.mat = mat;
            this.cByte = cByte;
        }
    }
}