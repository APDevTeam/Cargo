package io.github.cccm5.listener;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.jetbrains.annotations.NotNull;

public class SignPlaceListener implements Listener {
    @EventHandler
    public void onSignPlace(@NotNull SignChangeEvent e) {
        String header = ChatColor.stripColor(e.getLine(0));
        if (!header.equalsIgnoreCase("[Load]") && !header.equalsIgnoreCase("[UnLoad]"))
            return;

        e.setLine(0,ChatColor.DARK_AQUA + (ChatColor.stripColor(e.getLine(0))).replaceAll("u","U").replaceAll("l","L"));
    }
}
