package systems.mythical.mythicskywars.api.impl;

import systems.mythical.mythicskywars.api.SWREventAPI;
import systems.mythical.mythicskywars.api.MythicSkywarsAPI;

public class SWREventImpl implements SWREventAPI {

    MythicSkywarsAPI swrAPI;

    public SWREventImpl(MythicSkywarsAPI swrAPIIn) {
        this.swrAPI = swrAPIIn;
    }
    
}
