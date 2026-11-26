package com.oculifelis.votum;

import com.oculifelis.votum.compat.VotumRecipeViews;
import com.oculifelis.votum.data.VotumTables;
import com.oculifelis.votum.pact.PactEvents;
import com.oculifelis.votum.pact.PactLedger;
import com.oculifelis.votum.registry.VotumBlockEntities;
import com.oculifelis.votum.registry.VotumBlocks;
import com.oculifelis.votum.registry.VotumComponents;
import com.oculifelis.votum.registry.VotumItems;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(Votum.MODID)
public final class Votum {
    public static final String MODID = "votum";

    public Votum(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, VotumConfig.SPEC);
        VotumTables.init();
        PactLedger.init();
        VotumComponents.register(modBus);
        VotumBlocks.register(modBus);
        VotumItems.register(modBus);
        VotumBlockEntities.register(modBus);
        PactEvents.init();
        VotumRecipeViews.init();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
