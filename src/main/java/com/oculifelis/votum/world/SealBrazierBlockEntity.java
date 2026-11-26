package com.oculifelis.votum.world;

import com.oculifelis.votum.Votum;
import com.oculifelis.votum.VotumConfig;
import com.oculifelis.votum.data.VotumTables;
import com.oculifelis.votum.data.WaxDef;
import com.oculifelis.votum.registry.VotumBlockEntities;
import com.oculifelis.votum.registry.VotumItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Holds up to four honeycombs and, once a pigment goes in over heat, the wax colour and the game time the wax is ready.
 * Readiness is worked out from the time, so the brazier never ticks.
 */
public class SealBrazierBlockEntity extends VotumBlockEntity {
    public static final int MAX_HONEY = 4;
    public static final TagKey<Block> HEAT = TagKey.create(Registries.BLOCK, Votum.id("brazier_heat"));

    private int honey;
    private Identifier wax;
    private long readyAt;

    public SealBrazierBlockEntity(BlockPos pos, BlockState state) {
        super(VotumBlockEntities.SEAL_BRAZIER.get(), pos, state);
    }

    public int honey() {
        return honey;
    }

    public Identifier wax() {
        return wax;
    }

    public boolean hasHeat() {
        return level != null && level.getBlockState(worldPosition.below()).is(HEAT);
    }

    /** Ticks left until the wax can be poured, 0 when ready. */
    public long remaining() {
        if (wax == null || level == null) return 0;
        return Math.max(0, readyAt - level.getGameTime());
    }

    public InteractionResult useItem(ItemStack stack, Player player) {
        if (level == null) return InteractionResult.PASS;
        if (stack.is(Items.HONEYCOMB)) {
            if (wax != null || honey >= MAX_HONEY) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (!level.isClientSide()) {
                honey++;
                stack.consume(1, player);
                level.playSound(null, worldPosition, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 0.8F, 1.0F);
                updateFill();
            }
            return InteractionResult.SUCCESS;
        }
        WaxDef def = VotumTables.waxForPigment(stack);
        if (def == null || honey == 0 || wax != null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) {
            if (!hasHeat()) {
                player.sendOverlayMessage(Component.translatable("message.votum.brazier_cold"));
                return InteractionResult.SUCCESS;
            }
            wax = def.id();
            readyAt = level.getGameTime() + VotumConfig.brazierTicks();
            stack.consume(1, player);
            level.playSound(null, worldPosition, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.4F, 1.4F);
            updateFill();
        }
        return InteractionResult.SUCCESS;
    }

    public InteractionResult takeOut(Player player) {
        if (level == null || honey == 0) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (wax == null) {
            give(player, new ItemStack(Items.HONEYCOMB, honey));
        } else if (remaining() > 0) {
            player.sendOverlayMessage(Component.translatable("message.votum.brazier_melting", (remaining() + 19) / 20));
            return InteractionResult.SUCCESS;
        } else {
            give(player, VotumItems.wax(wax, honey));
            level.playSound(null, worldPosition, SoundEvents.HONEY_BLOCK_PLACE, SoundSource.BLOCKS, 0.8F, 1.0F);
        }
        honey = 0;
        wax = null;
        readyAt = 0;
        updateFill();
        return InteractionResult.SUCCESS;
    }

    private void updateFill() {
        int fill = wax != null ? 2 : honey > 0 ? 1 : 0;
        BlockState s = getBlockState();
        if (level != null && s.getValue(SealBrazierBlock.FILL) != fill) {
            level.setBlock(worldPosition, s.setValue(SealBrazierBlock.FILL, fill), 3);
        }
        changed();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && honey > 0) {
            ItemStack drop = wax != null && remaining() == 0 ? VotumItems.wax(wax, honey) : new ItemStack(Items.HONEYCOMB, honey);
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("honey", honey);
        if (wax != null) out.putString("wax", wax.toString());
        out.putLong("ready_at", readyAt);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        honey = in.getIntOr("honey", 0);
        String w = in.getStringOr("wax", "");
        wax = w.isEmpty() ? null : Identifier.tryParse(w);
        readyAt = in.getLongOr("ready_at", 0L);
        rerenderOnClient();
    }
}
