package systems.mythical.mythicskywars.api.impl;

import systems.mythical.mythicskywars.api.SWRCommandAPI;
import systems.mythical.mythicskywars.api.MythicSkywarsAPI;
import systems.mythical.mythicskywars.commands.KitCmdManager;
import systems.mythical.mythicskywars.commands.MainCmdManager;
import systems.mythical.mythicskywars.commands.MapCmdManager;
import systems.mythical.mythicskywars.commands.PartyCmdManager;

public class SWRCommandImpl implements SWRCommandAPI {

    MythicSkywarsAPI swrAPI;

    public SWRCommandImpl(MythicSkywarsAPI swrAPIIn) {
        this.swrAPI = swrAPIIn;
    }

    @Override
    public MainCmdManager getMainCommandManager() {
        return null; //swrAPI.getMythicSkywars();
    }

    @Override
    public KitCmdManager getKitCommandManager() {
        return null;
    }

    @Override
    public MapCmdManager getMapCommandManager() {
        return null;
    }

    @Override
    public PartyCmdManager getPartyCommandManager() {
        return null;
    }
}
