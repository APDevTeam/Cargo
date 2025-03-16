package io.github.cccm5.commands;

import io.github.cccm5.CargoMain;
import io.github.cccm5.config.Config;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LoadCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) { // Plugin
        if (!command.getName().equalsIgnoreCase("load"))
            return false;

        if (!(sender instanceof Player)) {
            sender.sendMessage(Config.ERROR_TAG + "You need to be a player to execute that command!");
            return true;
        }

        CargoMain.getInstance().load((Player) sender);
        return true;
    }
}
