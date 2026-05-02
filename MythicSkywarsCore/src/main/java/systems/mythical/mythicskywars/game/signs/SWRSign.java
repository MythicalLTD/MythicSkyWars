package systems.mythical.mythicskywars.game.signs;

import systems.mythical.mythicskywars.game.GameMap;
import org.bukkit.Location;
import org.bukkit.block.Block;

public interface SWRSign {

    void setMaterial(GameMap gMap, Block attachedBlock);

    void updateBlock(Block block, String item);

    Block getAttachedBlock(final Block b);

    void update();

    Location getLocation();

    String getName();

}
