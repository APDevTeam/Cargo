package io.github.cccm5.util;

import com.degitise.minevid.dtlTraders.Main;
import com.degitise.minevid.dtlTraders.guis.AGUI;
import com.degitise.minevid.dtlTraders.guis.gui.TradeGUI;
import com.degitise.minevid.dtlTraders.guis.gui.TradeGUIPage;
import com.degitise.minevid.dtlTraders.guis.items.AGUIItem;
import com.degitise.minevid.dtlTraders.guis.items.TradableGUIItem;
import com.degitise.minevid.dtlTraders.utils.citizens.TraderTrait;
import io.github.cccm5.CargoTrait;
import io.github.cccm5.config.Config;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.trait.Trait;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.util.MathUtils;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class NPCUtil {
    @NotNull
    public static List<NPC> getNPCsWithTrait(Class<? extends Trait> c){
        List<NPC> npcs = new ArrayList<>();
        for(NPCRegistry registry : net.citizensnpcs.api.CitizensAPI.getNPCRegistries())
            for(NPC npc : registry)
                if(npc.hasTrait(c))
                    npcs.add(npc);
        return npcs;
    }

    @NotNull
    public static List<NPC> getNPCsInRange(MovecraftLocation center) {
        List<NPC> result = new ArrayList<>();
        for (NPC npc :NPCUtil.getNPCsWithTrait(CargoTrait.class)) {
            if (!npc.isSpawned())
                continue;

            MovecraftLocation npcLocation = MathUtils.bukkit2MovecraftLoc(npc.getEntity().getLocation());
            double distanceSquared;
            if (Config.cardinalDistance) {
                distanceSquared = Math.abs(center.getX() - npcLocation.getX()) + Math.abs(center.getZ() - npcLocation.getZ());
                distanceSquared *= distanceSquared;
            }
            else {
                distanceSquared = center.distanceSquared(npcLocation);
            }

            if (distanceSquared <= (Config.scanRange * Config.scanRange)) {
                result.add(npc);
            }
        }
        return result;
    }

    @Nullable
    public static TradableGUIItem getUnloadItem(@NotNull List<NPC> nearbyMerchants, ItemStack compareItem, Main dtlTradersPlugin) {
        TradableGUIItem result = null;
        for (NPC cargoMerchant : nearbyMerchants) {
            if (result != null)
                break;
            String guiName = cargoMerchant.getTrait(TraderTrait.class).getGUIName();
            AGUI gui = dtlTradersPlugin.getGuiListService().getGUI(guiName);
            TradeGUI tradeGUI = (TradeGUI) gui;
            result = null;
            for (TradeGUIPage page : tradeGUI.getPages()) {
                if (page == null) continue;
                for (AGUIItem tempItem : page.getItems("sell")) {
                    if (!(tempItem instanceof TradableGUIItem)) continue;
                    if (tempItem.getMainItem().isSimilar(compareItem)) {
                        if (tempItem.getMainItem().getAmount() > 1)
                            continue;
                        result = (TradableGUIItem) tempItem;
                        break;
                    }
                }
                if (result == null || result.getTradePrice() == 0.0) {
                    return null;
                }
            }
        }
        return result;
    }

    @Nullable
    public static TradableGUIItem getLoadItem(@NotNull List<NPC> nearbyMerchants, ItemStack compareItem, Main dtlTradersPlugin) {
        TradableGUIItem result = null;
        for (NPC cargoMerchant : nearbyMerchants) {
            String guiName = cargoMerchant.getTrait(TraderTrait.class).getGUIName();
            AGUI gui = dtlTradersPlugin.getGuiListService().getGUI(guiName);
            TradeGUI tradeGUI = (TradeGUI) gui;
            for (TradeGUIPage page : tradeGUI.getPages()) {
                if (page == null)
                    continue;

                for (AGUIItem tempItem : page.getItems("buy")) {
                    if (!(tempItem instanceof TradableGUIItem))
                        continue;

                    TradableGUIItem tradeItem = (TradableGUIItem) tempItem;
                    if (tradeItem.getMainItem().isSimilar(compareItem)) {
                        if (tempItem.getMainItem().getAmount() > 1)
                            continue;

                        result = tradeItem;
                        break;
                    }
                }
                if (result != null)
                    break;
            }
            if (result != null)
                break;
        }
        return result;
    }
}
