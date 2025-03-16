package io.github.cccm5.commands;

import io.github.cccm5.config.Config;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class CargoCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("cargo"))
            return false;

        if (!sender.hasPermission("Cargo.cargo")) {
            sender.sendMessage(Config.ERROR_TAG + "You don't have permission to do that!");
            return true;
        }

        sender.sendMessage(ChatColor.WHITE + "--[ " + ChatColor.DARK_AQUA + "  Movecraft Cargo " + ChatColor.WHITE + " ]--");
        sender.sendMessage(ChatColor.DARK_AQUA + "Scan Range: " + ChatColor.WHITE + Config.scanRange + " Blocks");
        sender.sendMessage(ChatColor.DARK_AQUA + "Transfer Delay: " + ChatColor.WHITE + Config.delay + " ticks");
        sender.sendMessage(ChatColor.DARK_AQUA + "Unload Tax: " + ChatColor.WHITE + String.format("%.2f",100*Config.unloadTax) + "%");
        sender.sendMessage(ChatColor.DARK_AQUA + "Load Tax: " + ChatColor.WHITE + String.format("%.2f",100*Config.loadTax) + "%");
        sender.sendMessage(ChatColor.DARK_AQUA + "Distance Type: " + ChatColor.WHITE + (Config.cardinalDistance ? "Cardinal" : "Direct"));

        return true;
    }
}
