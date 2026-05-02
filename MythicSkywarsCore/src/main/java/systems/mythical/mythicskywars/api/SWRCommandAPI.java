package systems.mythical.mythicskywars.api;

import systems.mythical.mythicskywars.commands.*;

public interface SWRCommandAPI {

    MainCmdManager getMainCommandManager();

    KitCmdManager getKitCommandManager();

    MapCmdManager getMapCommandManager();

    PartyCmdManager getPartyCommandManager();

}
