package systems.mythical.mythicskywars.api.command;

import systems.mythical.mythicskywars.commands.BaseCmd;

public interface SWRCmdManagerAPI {

    void registerCommand(BaseCmd commandIn);

    void unregisterCommand(BaseCmd commandIn);

    BaseCmd getSubCommand(String name);

}
