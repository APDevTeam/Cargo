package io.github.cccm5.async;

import com.degitise.minevid.dtlTraders.guis.items.TradableGUIItem;
import io.github.cccm5.CargoMain;
import io.github.cccm5.config.Config;
import io.github.cccm5.util.CraftInventoryUtil;
import net.countercraft.movecraft.craft.PlayerCraft;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class LoadTask extends CargoTask {
    public LoadTask(PlayerCraft craft, TradableGUIItem item) {
        super(craft, item);
    }

    protected void execute() {
        List<Inventory> invs = CraftInventoryUtil.getInventoriesWithSpace(craft, item.getMainItem(), Material.CHEST,
                Material.TRAPPED_CHEST, Material.BARREL);

        Inventory inv = invs.get(0);
        int loaded = 0;
        for (int i = 0; i < inv.getSize(); i++)
            if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR
                    || inv.getItem(i).isSimilar(item.getMainItem())) {
                int maxCount = (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR)
                        ? item.getMainItem().getMaxStackSize()
                        : inv.getItem(i).getMaxStackSize() - inv.getItem(i).getAmount();
                if (CargoMain.getEconomy().getBalance(originalPilot) > item.getTradePrice() * maxCount
                        * (1 + Config.loadTax)) {
                    loaded += maxCount;
                    ItemStack tempItem = item.getMainItem().clone();
                    tempItem.setAmount(tempItem.getMaxStackSize());
                    inv.setItem(i, tempItem);

                } else {
                    maxCount = (int) (CargoMain.getEconomy().getBalance(originalPilot)
                            / (item.getTradePrice() * (1 + Config.loadTax)));
                    this.cancel();
                    CargoMain.getQue().remove(originalPilot);
                    originalPilot.sendMessage(Config.SUCCESS_TAG + "You ran out of money!");
                    if (maxCount <= 0) {
                        if (Config.debug) {
                            CargoMain.getInstance().getLogger().info("Balance: " + CargoMain.getEconomy().getBalance(originalPilot)
                                    + ". maxCount: " + maxCount + ".");
                        }
                        originalPilot.sendMessage(Config.SUCCESS_TAG + "Loaded " + loaded + " items worth $"
                                + String.format("%.2f", loaded * item.getTradePrice()) + " took a tax of "
                                + String.format("%.2f", Config.loadTax * loaded * item.getTradePrice()));
                        return;
                    }
                    ItemStack tempItem = item.getMainItem().clone();
                    if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR)
                        tempItem.setAmount(maxCount);
                    else
                        tempItem.setAmount(inv.getItem(i).getAmount() + maxCount);
                    inv.setItem(i, tempItem);
                    loaded += maxCount;
                    if (Config.debug) {
                        CargoMain.getInstance().getLogger().info("Balance: " + CargoMain.getEconomy().getBalance(originalPilot)
                                + ". maxCount: " + maxCount + ". Actual stack-size: " + tempItem.getAmount());
                    }
                    originalPilot.sendMessage(Config.SUCCESS_TAG + "Loaded " + loaded + " items worth $"
                            + String.format("%.2f", loaded * item.getTradePrice()) + " took a tax of "
                            + String.format("%.2f", Config.loadTax * loaded * item.getTradePrice()));
                    CargoMain.getEconomy().withdrawPlayer(originalPilot,
                            loaded * item.getTradePrice() * (1 + Config.loadTax));
                    return;
                }
                CargoMain.getEconomy().withdrawPlayer(originalPilot,
                        maxCount * item.getTradePrice() * (1 + Config.loadTax));
            }

        originalPilot.sendMessage(Config.SUCCESS_TAG + "Loaded " + loaded + " items worth $"
                + String.format("%.2f", loaded * item.getTradePrice()) + " took a tax of "
                + String.format("%.2f", Config.loadTax * loaded * item.getTradePrice()));

        if (invs.size() <= 1) {
            this.cancel();
            CargoMain.getQue().remove(originalPilot);
            originalPilot.sendMessage(Config.SUCCESS_TAG + "All cargo loaded");
            return;
        }
        new ProcessingTask(originalPilot, item, invs.size()).runTaskTimer(CargoMain.getInstance(), 0, 20);
    }
}
