package io.github.cccm5.util;

import com.degitise.minevid.dtlTraders.Main;
import com.degitise.minevid.dtlTraders.guis.AGUI;
import com.degitise.minevid.dtlTraders.guis.gui.TradeGUI;
import com.degitise.minevid.dtlTraders.guis.gui.TradeGUIPage;
import com.degitise.minevid.dtlTraders.guis.items.AGUIItem;
import com.degitise.minevid.dtlTraders.guis.items.TradableGUIItem;
import com.degitise.minevid.dtlTraders.utils.citizens.TraderTrait;
import io.github.cccm5.CargoMain;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        if (Config.debug) {
            CargoMain.getInstance().getLogger().info("Found " + result.size() + " merchants:");
            for (NPC npc : result) {
                CargoMain.getInstance().getLogger().info("- " + npc.getId() + ": " + npc.getName());
            }
        }
        return result;
    }

    @NotNull
    public static Set<TradableGUIItem> getItems(@NotNull List<NPC> merchants, ItemStack item, Main dtlTradersPlugin, String shopMode) {
        Set<TradableGUIItem> result = new HashSet<>();
        for (NPC merchant : merchants) {
            String guiName = merchant.getTrait(TraderTrait.class).getGUIName();
            AGUI gui = dtlTradersPlugin.getGuiListService().getGUI(guiName);
            if (!(gui instanceof TradeGUI))
                continue;

            for (TradeGUIPage page : ((TradeGUI) gui).getPages()) {
                for (AGUIItem guiItem : page.getItems(shopMode)) {
                    if (!(guiItem instanceof TradableGUIItem tradableItem))
                        continue;
                    if (!guiItem.getMainItem().isSimilar(item) || guiItem.getMainItem().getAmount() > 1)
                        continue;

                    result.add(tradableItem);
                    if (Config.debug) {
                        CargoMain.getInstance().getLogger().info("Found for $" + tradableItem.getTradePrice() + " in " + merchant.getId() + "/" + page.getPageName());
                    }
                }
            }
        }
        return result;
    }
}
