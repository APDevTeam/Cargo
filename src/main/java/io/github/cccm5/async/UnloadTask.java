package io.github.cccm5.async;

import com.degitise.minevid.dtlTraders.guis.items.TradableGUIItem;
import io.github.cccm5.CargoMain;
import io.github.cccm5.config.Config;
import io.github.cccm5.util.CraftInventoryUtil;
import net.countercraft.movecraft.craft.PlayerCraft;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

import java.util.List;

public class UnloadTask extends CargoTask {
    public UnloadTask(PlayerCraft craft, TradableGUIItem item) {
        super(craft, item);
    }

    public void execute() {
        List<Inventory> invs = CraftInventoryUtil.getInventories(craft, item.getMainItem(), Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL);
        Inventory inv = invs.get(0);
        int count = 0;
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) != null && inv.getItem(i).isSimilar(item.getMainItem())) {
                count += inv.getItem(i).getAmount();
                inv.setItem(i, null);
            }
        }
        originalPilot.sendMessage(Config.SUCCESS_TAG + "Unloaded " + count + " worth $"
                + String.format("%.2f", count * item.getTradePrice()) + " took a tax of "
                + String.format("%.2f", Config.unloadTax * count * item.getTradePrice()));
        CargoMain.getEconomy().depositPlayer(originalPilot,
                count * item.getTradePrice() * (1 - Config.unloadTax));

        if (invs.size() <= 1) {
            this.cancel();
            CargoMain.getQue().remove(originalPilot);
            originalPilot.sendMessage(Config.SUCCESS_TAG + "All cargo unloaded");
            return;
        }
        new ProcessingTask(originalPilot, item, invs.size()).runTaskTimer(CargoMain.getInstance(), 0, 20);
    }
}
