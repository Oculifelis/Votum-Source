package com.oculifelis.votum.registry;

import com.oculifelis.votum.Votum;
import com.oculifelis.votum.world.CharterPostBlock;
import com.oculifelis.votum.world.SealBrazierBlock;
import com.oculifelis.votum.world.SignetDeskBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public final class VotumBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, Votum.MODID);
    private static final List<DeferredHolder<Item, BlockItem>> BLOCK_ITEMS = new ArrayList<>();

    public static final DeferredHolder<Block, SealBrazierBlock> SEAL_BRAZIER = register("seal_brazier", SealBrazierBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(3.0F, 6.0F).sound(SoundType.COPPER)
            .requiresCorrectToolForDrops().noOcclusion());
    public static final DeferredHolder<Block, SignetDeskBlock> SIGNET_DESK = register("signet_desk", SignetDeskBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredHolder<Block, CharterPostBlock> CHARTER_POST = register("charter_post", CharterPostBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion());

    private VotumBlocks() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }

    public static List<DeferredHolder<Item, BlockItem>> blockItems() {
        return Collections.unmodifiableList(BLOCK_ITEMS);
    }

    private static <B extends Block> DeferredHolder<Block, B> register(String name, Function<BlockBehaviour.Properties, B> factory,
                                                                      Supplier<BlockBehaviour.Properties> props) {
        Identifier id = Votum.id(name);
        DeferredHolder<Block, B> block = BLOCKS.register(name, () -> factory.apply(props.get().setId(ResourceKey.create(Registries.BLOCK, id))));
        BLOCK_ITEMS.add(VotumItems.ITEMS.register(name, () -> new BlockItem(block.get(),
            new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).useBlockDescriptionPrefix())));
        return block;
    }
}
