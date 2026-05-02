package systems.mythical.mythicskywars.api.impl;

import systems.mythical.mythicskywars.MythicSkywars;
import systems.mythical.mythicskywars.api.SWRCommandAPI;
import systems.mythical.mythicskywars.api.SWREventAPI;
import systems.mythical.mythicskywars.api.SWRGameAPI;
import systems.mythical.mythicskywars.api.MythicSkywarsAPI;

public class MythicSkywarsImpl implements MythicSkywarsAPI {


    private final MythicSkywars plugin;
    private SWRCommandAPI swrCmdAPI;
    private SWREventAPI swrEventAPI;
    private SWRGameAPI swrGameAPI;

    public MythicSkywarsImpl() {
        this.plugin = MythicSkywars.get();
        this.swrGameAPI = new SWRGameImpl(this);
    }

    @Override
    public MythicSkywars getPlugin() {
        return this.plugin;
    }

    @Override
    public SWRCommandAPI getCommandAPI() {
        return null;
    }

    @Override
    public SWREventAPI getEventAPI() {
        return null;
    }

    @Override
    public SWRGameAPI getGameAPI() {
        return this.swrGameAPI;
    }

}
