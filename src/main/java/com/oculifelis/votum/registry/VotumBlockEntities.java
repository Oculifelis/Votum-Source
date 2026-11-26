package com.oculifelis.votum.registry;

import com.oculifelis.votum.Votum;
import com.oculifelis.votum.world.CharterPostBlockEntity;
import com.oculifelis.votum.world.SealBrazierBlockEntity;
import com.oculifelis.votum.world.SignetDeskBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

public final class VotumBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Votum.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SealBrazierBlockEntity>> SEAL_BRAZIER =
        BLOCK_ENTITIES.register("seal_brazier", () -> new BlockEntityType<>(SealBrazierBlockEntity::new, Set.of(VotumBlocks.SEAL_BRAZIER.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SignetDeskBlockEntity>> SIGNET_DESK =
        BLOCK_ENTITIES.register("signet_desk", () -> new BlockEntityType<>(SignetDeskBlockEntity::new, Set.of(VotumBlocks.SIGNET_DESK.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CharterPostBlockEntity>> CHARTER_POST =
        BLOCK_ENTITIES.register("charter_post", () -> new BlockEntityType<>(CharterPostBlockEntity::new, Set.of(VotumBlocks.CHARTER_POST.get())));

    private VotumBlockEntities() {}

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
