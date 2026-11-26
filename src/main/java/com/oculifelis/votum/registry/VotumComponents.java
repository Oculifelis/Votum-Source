package com.oculifelis.votum.registry;

import com.mojang.serialization.Codec;
import com.oculifelis.votum.Votum;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class VotumComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Votum.MODID);

    /** The wax colour of a Sealing Wax stack. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Identifier>> WAX =
        COMPONENTS.registerComponentType("wax", b -> b
            .persistent(Identifier.CODEC)
            .networkSynchronized(Identifier.STREAM_CODEC));

    /** The pact a warrant, ring or pendant holds. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Identifier>> PACT =
        COMPONENTS.registerComponentType("pact", b -> b
            .persistent(Identifier.CODEC)
            .networkSynchronized(Identifier.STREAM_CODEC));

    /** Set on a ring or pendant whose wearer broke its clause. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> CRACKED =
        COMPONENTS.registerComponentType("cracked", b -> b
            .persistent(Codec.BOOL)
            .networkSynchronized(ByteBufCodecs.BOOL));

    private VotumComponents() {}

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }
}
