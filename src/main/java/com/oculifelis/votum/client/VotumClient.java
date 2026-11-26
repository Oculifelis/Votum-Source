package com.oculifelis.votum.client;

import com.mojang.serialization.MapCodec;
import com.oculifelis.votum.Votum;
import com.oculifelis.votum.data.VotumTables;
import com.oculifelis.votum.data.WaxDef;
import com.oculifelis.votum.registry.VotumBlocks;
import com.oculifelis.votum.registry.VotumItems;
import com.oculifelis.votum.world.SealBrazierBlock;
import com.oculifelis.votum.world.SealBrazierBlockEntity;
import com.oculifelis.votum.world.SignetDeskBlock;
import com.oculifelis.votum.world.SignetDeskBlockEntity;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

import java.util.List;
import java.util.Set;

/** Wax colours on the wax item and on wax sitting in a brazier or on a desk. */
@Mod(value = Votum.MODID, dist = Dist.CLIENT)
public final class VotumClient {
    /** Colour used when no wax is known, a warm sealing red. */
    public static final int FALLBACK = 0xFFA3262A;

    public VotumClient(IEventBus modBus) {
        modBus.addListener(VotumClient::itemTints);
        modBus.addListener(VotumClient::blockTints);
    }

    static int colorOf(Identifier wax) {
        WaxDef def = wax == null ? null : VotumTables.wax(wax);
        return def == null ? FALLBACK : 0xFF000000 | def.color();
    }

    private static void itemTints(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(Votum.id("wax"), WaxTint.CODEC);
    }

    private static void blockTints(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(List.of(new BlockTintSource() {
            @Override
            public int color(BlockState state) {
                return FALLBACK;
            }

            @Override
            public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
                return level.getBlockEntity(pos) instanceof SealBrazierBlockEntity be ? colorOf(be.wax()) : FALLBACK;
            }

            @Override
            public Set<Property<?>> relevantProperties() {
                return Set.of(SealBrazierBlock.FILL);
            }
        }), VotumBlocks.SEAL_BRAZIER.get());
        event.register(List.of(new BlockTintSource() {
            @Override
            public int color(BlockState state) {
                return FALLBACK;
            }

            @Override
            public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
                return level.getBlockEntity(pos) instanceof SignetDeskBlockEntity be ? colorOf(be.wax()) : FALLBACK;
            }

            @Override
            public Set<Property<?>> relevantProperties() {
                return Set.of(SignetDeskBlock.CONTENTS);
            }
        }), VotumBlocks.SIGNET_DESK.get());
    }

    /** Item model tint {@code votum:wax}: the colour of the wax the stack holds. */
    public record WaxTint() implements ItemTintSource {
        public static final MapCodec<WaxTint> CODEC = MapCodec.unit(new WaxTint());

        @Override
        public int calculate(ItemStack stack, ClientLevel level, LivingEntity entity) {
            return colorOf(VotumItems.waxOf(stack));
        }

        @Override
        public MapCodec<? extends ItemTintSource> type() {
            return CODEC;
        }
    }
}
