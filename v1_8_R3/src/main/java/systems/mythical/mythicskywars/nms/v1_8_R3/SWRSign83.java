package systems.mythical.mythicskywars.nms.v1_8_R3;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.enums.MatchState;
import systems.mythical.mythicskywars.game.GameMap;
import systems.mythical.mythicskywars.utilities.Messaging;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.material.Directional;
import org.bukkit.material.MaterialData;

public class SWRSign83 implements systems.mythical.mythicskywars.game.signs.SWRSign {

    private final String gameName;
    private final org.bukkit.Location location;

    public SWRSign83(String name, Location location) {
        this.gameName = name;
        this.location = location;
    }

    @Override
    public void setMaterial(GameMap gMap, Block attachedBlock) {
        if (attachedBlock == null) return;
        attachedBlock.getWorld().loadChunk(attachedBlock.getChunk());

        if (gMap.getMatchState().equals(MatchState.WAITINGSTART) || gMap.getMatchState().equals(MatchState.WAITINGLOBBY)) {
            updateBlock(attachedBlock, "blockwaiting");
        } else if (gMap.getMatchState().equals(MatchState.PLAYING)) {
            updateBlock(attachedBlock, "blockplaying");
        } else if (gMap.getMatchState().equals(MatchState.ENDING)) {
            updateBlock(attachedBlock, "blockending");
        } else {
            updateBlock(attachedBlock, "blockoffline");
        }
    }

    @Override
    public void updateBlock(Block block, String item) {
        if (block == null) return;
        block.setType(MythicSkywars.getIM().getItem(item).getType());

        if (MythicSkywars.getNMS().getVersion() < 13 && (
                MythicSkywars.getIM().getItem(item).getType().equals(Material.valueOf("WOOL")) ||
                        MythicSkywars.getIM().getItem(item).getType().equals(Material.valueOf("STAINED_GLASS")) ||
                        MythicSkywars.getIM().getItem(item).getType().equals(Material.valueOf("STAINED_CLAY"))
        )) {
            MythicSkywars.getNMS().setBlockWithColor(
                    block.getWorld(), block.getX(), block.getY(), block.getZ(),
                    MythicSkywars.getIM().getItem(item).getType(),
                    MythicSkywars.getIM().getItem(item).getData().getData()
            );
        }
    }

    @Override
    public Block getAttachedBlock(Block b) {
        final MaterialData mData = b.getState().getData();

        BlockFace face = null;
        if (mData instanceof org.bukkit.material.Sign) {
            face = ((org.bukkit.material.Sign) mData).getAttachedFace();
            if (face == null) face = BlockFace.DOWN;
        } else if (mData instanceof Directional) {
            face = ((Directional) mData).getFacing().getOppositeFace();
        }

        if (face == null) return null;
        return b.getRelative(face);
    }

    @Override
    public void update() {
        GameMap gMap = MythicSkywars.get().getGameMapManager().getMap(gameName);
        Location loc = location.clone();

        if (loc.getBlock() == null) {
            return;
        }

        if (loc.getBlock().getType().name().contains("SIGN") || loc.clone().add(0, 1, 0).getBlock().getType().name().contains("SIGN")) {
            Block attachedBlock;
            Sign sign = (Sign) loc.getBlock().getState();
            if (sign == null) return;

            if (loc.getBlock().getType().name().contains("HANGING")) {
                attachedBlock = loc.clone().add(0, 1, 0).getBlock();
            } else if (loc.getBlock().getType().name().contains("WALL")) {
                attachedBlock = getAttachedBlock(loc.getBlock());
            } else {
                attachedBlock = loc.clone().add(0, -1, 0).getBlock();
            }

            setMaterial(gMap, attachedBlock);

            sign.getBlock().getChunk().load();
            if (gMap != null) {
                formatSign(gMap, sign);
            }
            sign.update(true);
        }
    }

    private void formatSign(GameMap gMap, Sign sign) {
        String state;
        if (gMap.getMatchState().equals(MatchState.WAITINGSTART) || gMap.getMatchState().equals(MatchState.WAITINGLOBBY)) {
            state = new Messaging.MessageFormatter().format("signs.joinable");
        } else if (gMap.getMatchState().equals(MatchState.PLAYING)) {
            state = new Messaging.MessageFormatter().format("signs.playing");
        } else if (gMap.getMatchState().equals(MatchState.ENDING)) {
            state = new Messaging.MessageFormatter().format("signs.ending");
        } else {
            state = new Messaging.MessageFormatter().format("signs.offline");
        }

        String team = gMap.getTeamSize() > 1 ? "team" : "";

        int playerCount;
        if (gMap.getMatchState() == MatchState.WAITINGLOBBY) {
            playerCount = gMap.getWaitingPlayers().size();
        } else {
            playerCount = gMap.getPlayerCount();
        }

        for (int i = 0; i < 4; i++) {
            sign.setLine(i, new Messaging.MessageFormatter()
                    .setVariable("matchstate", state)
                    .setVariable("mapname", gMap.getDisplayName().toUpperCase())
                    .setVariable("playercount", "" + playerCount)
                    .setVariable("maxplayers", "" + gMap.getMaxPlayers())
                    .setVariable("teamsize", "" + gMap.getTeamSize()).format("signs.line" + (i + 1) + team)
            );
        }
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public String getName() {
        return gameName;
    }

}
