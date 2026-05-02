package systems.mythical.mythicskywars.api;

import systems.mythical.mythicskywars.MythicSkywars;

public interface MythicSkywarsAPI {

    MythicSkywars getPlugin();

    SWRCommandAPI getCommandAPI();

    SWREventAPI getEventAPI();

    SWRGameAPI getGameAPI();

}
