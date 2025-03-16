package io.github.cccm5.listener;

import io.github.cccm5.CargoMain;
import org.bukkit.ChatColor;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

public class SignClickListener implements Listener {
    @EventHandler
    public void onSignClick(@NotNull PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        BlockState state = e.getClickedBlock().getState();
        if (!(state instanceof Sign))
            return;

        Sign sign = (Sign) e.getClickedBlock().getState();
        if (sign.getLine(0).equals(ChatColor.DARK_AQUA + "[UnLoad]")) {
            CargoMain.getInstance().unload(e.getPlayer());
            return;
        }
        if (sign.getLine(0).equals(ChatColor.DARK_AQUA + "[Load]")) {
            CargoMain.getInstance().load(e.getPlayer());
        }
    }
}
