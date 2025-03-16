package io.github.cccm5;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.trait.Trait;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;


public class Utils
{
    public static ArrayList<NPC> getNPCsWithTrait(Class<? extends Trait> c){
        ArrayList<NPC> npcs = new ArrayList<>();
        for(NPCRegistry registry : net.citizensnpcs.api.CitizensAPI.getNPCRegistries())
            for(NPC npc : registry)
                if(npc.hasTrait(c))
                    npcs.add(npc);
        return npcs;
    }
    
    

}
