package systems.mythical.mythicskywars.nms.v1_19_R1;

import systems.mythical.mythicskywars.game.signs.SWRSign;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.EnderChest;

public class NMSHandler extends systems.mythical.mythicskywars.nms.v1_18_R2.NMSHandler {

    public void playChestAction(Block block, boolean open) {
        Location location = block.getLocation();
        if (location.getWorld() == null) return;

        if (!(block.getState() instanceof EnderChest enderChest)) return;

        if (open) enderChest.open();
        else enderChest.close();
    }

    public int getVersion() {
        return 19;
    }

    @Override
    public SWRSign createSWRSign(String name, Location location) {
        return new SWRSign_19_0(name, location);
    }
}
