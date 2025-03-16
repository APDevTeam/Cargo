package io.github.cccm5.util;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.trait.Trait;

import java.util.ArrayList;

public class NPCUtil {
    public static ArrayList<NPC> getNPCsWithTrait(Class<? extends Trait> c){
        ArrayList<NPC> npcs = new ArrayList<>();
        for(NPCRegistry registry : net.citizensnpcs.api.CitizensAPI.getNPCRegistries())
            for(NPC npc : registry)
                if(npc.hasTrait(c))
                    npcs.add(npc);
        return npcs;
    }
}
