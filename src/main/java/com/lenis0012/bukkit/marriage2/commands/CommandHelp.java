package com.lenis0012.bukkit.marriage2.commands;

import com.lenis0012.bukkit.marriage2.Marriage;
import com.lenis0012.bukkit.marriage2.internal.MarriageBase;
import com.lenis0012.bukkit.marriage2.internal.MarriageCommandExecutor;

public class CommandHelp extends Command {

    public CommandHelp(Marriage marriage) {
        super(marriage, "help");
        setMinArgs(-1);
        setHidden(true);
    }

    @Override
    public void execute() {
        MarriageCommandExecutor commandExecutor = ((MarriageBase) marriage).getCommandExecutor();
        reply("Author: &alenis0012");
        reply("Redone by tylerhyperHD");
        reply("Version: &a" + marriage.getPlugin().getDescription().getVersion());
        reply("&2&m---------&2< &a&lMarriage Command Help &2>&2&m---------"); // Play around with the amount of dashes later
        for (Command command : commandExecutor.getSubCommands()) {
            if (command.isHidden()) {
                continue;
            }

            String alias = command instanceof CommandMarry ? "" : command.getAliases()[0] + " ";
            reply("&a/marry " + alias + command.getUsage() + " &f- &7" + command.getDescription());
        }

        reply("&2&m--------------------------------------------"); // Play around with the amount of dashes later
    }
}
