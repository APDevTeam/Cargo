package io.github.cccm5.async;

import com.degitise.minevid.dtlTraders.guis.items.TradableGUIItem;
import io.github.cccm5.CargoMain;
import io.github.cccm5.config.Config;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.countercraft.movecraft.craft.CraftManager;

public abstract class CargoTask extends BukkitRunnable {
    protected PlayerCraft craft;
    protected final HitBox originalLocations;
    protected final Player originalPilot;
    protected TradableGUIItem item;

    protected CargoTask(PlayerCraft craft, TradableGUIItem guiItem) {
        if (craft == null) {
            throw new IllegalArgumentException("craft must not be null");
        }
        this.craft = craft;
        if (guiItem == null)
            throw new IllegalArgumentException("item must not be null");
        else
            item = guiItem;
        originalLocations = craft.getHitBox();
        originalPilot = craft.getPilot();
    }

    @Override
    public void run() {
        if (CraftManager.getInstance().getCraftByPlayer(originalPilot) == null) {
            if (Config.debug)
                CargoMain.getInstance().getLogger().info("canceling CargoTask due to missing player/craft");
            CargoMain.getQue().remove(originalPilot);
            cancel();
            return;
        }

        if (craft.getPilot() != originalPilot) {
            originalPilot.sendMessage("Pilots changed!");
            CargoMain.getQue().remove(originalPilot);
            cancel();
            return;
        }

        if (craft.getHitBox() != originalLocations) {
            originalPilot.sendMessage("Blocks moved/changed!");
            CargoMain.getQue().remove(originalPilot);
            cancel();
            return;
        }
        if (Config.debug)
            CargoMain.getInstance().getLogger().info("Running execute method for CargoTask with address " + this + ". Pilot: "
                    + originalPilot.getName() + " CraftSize: " + originalLocations.size() + " CraftType: "
                    + craft.getType().getStringProperty(CraftType.NAME) + " StockItem: "
                    + (item.getDisplayName().length() > 0 ? item.getDisplayName()
                            : item.getMainItem().getType().name().toLowerCase()));
        execute();
    }

    protected abstract void execute();
}
