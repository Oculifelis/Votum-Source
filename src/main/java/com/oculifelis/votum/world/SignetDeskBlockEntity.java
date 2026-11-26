package com.oculifelis.votum.world;

import com.oculifelis.votum.data.PactDef;
import com.oculifelis.votum.data.VotumTables;
import com.oculifelis.votum.item.PactItem;
import com.oculifelis.votum.item.SealPieceItem;
import com.oculifelis.votum.registry.VotumBlockEntities;
import com.oculifelis.votum.registry.VotumComponents;
import com.oculifelis.votum.registry.VotumItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.function.Supplier;

/**
 * The desk holds one item (a blank warrant, a fulfilled warrant or a cracked piece) and up to four wax of one colour.
 * <ul>
 *   <li>Sealing: blank warrant, then wax, then a signet. The pact that signet and wax name pops out as a sealed warrant.</li>
 *   <li>Pressing: a fulfilled warrant, then a blank ring or pendant of the pact's kind. The piece comes out holding the pact.</li>
 *   <li>Re-stamping: a cracked piece, then two wax of its pact's colour. The piece comes out whole.</li>
 * </ul>
 */
public class SignetDeskBlockEntity extends VotumBlockEntity {
    public static final int MAX_WAX = 4;
    public static final int RESTAMP_WAX = 2;

    private ItemStack held = ItemStack.EMPTY;
    private Identifier wax;
    private int waxCount;

    public SignetDeskBlockEntity(BlockPos pos, BlockState state) {
        super(VotumBlockEntities.SIGNET_DESK.get(), pos, state);
    }

    public ItemStack held() {
        return held;
    }

    public Identifier wax() {
        return wax;
    }

    public int waxCount() {
        return waxCount;
    }

    public InteractionResult useItem(ItemStack stack, Player player) {
        if (level == null) return InteractionResult.PASS;
        boolean client = level.isClientSide();

        if (held.isEmpty()) {
            boolean placeable = stack.is(VotumItems.BLANK_WARRANT.get()) || stack.is(VotumItems.FULFILLED_WARRANT.get())
                || (stack.getItem() instanceof SealPieceItem && VotumItems.isCracked(stack));
            if (!placeable) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (!client) {
                held = stack.copyWithCount(1);
                stack.consume(1, player);
                sound(SoundEvents.BOOK_PUT, 1.0F);
                update();
            }
            return InteractionResult.SUCCESS;
        }

        Identifier stackWax = stack.is(VotumItems.SEALING_WAX.get()) ? VotumItems.waxOf(stack) : null;
        if (stackWax != null && acceptsWax()) {
            if (waxCount >= MAX_WAX || (wax != null && !wax.equals(stackWax))) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (held.getItem() instanceof SealPieceItem) {
                PactDef pact = PactItem.pactOf(held);
                if (pact == null || !pact.wax().equals(stackWax)) {
                    if (!client) player.sendOverlayMessage(Component.translatable("message.votum.desk_wrong_wax"));
                    return InteractionResult.SUCCESS;
                }
            }
            if (!client) {
                wax = stackWax;
                waxCount++;
                stack.consume(1, player);
                sound(SoundEvents.HONEYCOMB_WAX_ON, 1.2F);
                if (held.getItem() instanceof SealPieceItem && waxCount >= RESTAMP_WAX) restamp(player);
                else update();
            }
            return InteractionResult.SUCCESS;
        }

        if (held.is(VotumItems.BLANK_WARRANT.get()) && isSignet(stack.getItem()) && waxCount > 0) {
            if (!client) seal(stack.getItem(), player);
            return InteractionResult.SUCCESS;
        }

        if (held.is(VotumItems.FULFILLED_WARRANT.get()) && stack.getItem() instanceof SealPieceItem piece
            && VotumItems.pactOf(stack) == null) {
            if (!client) press(stack, piece, player);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    private boolean acceptsWax() {
        return held.is(VotumItems.BLANK_WARRANT.get()) || held.getItem() instanceof SealPieceItem;
    }

    private void seal(Item signet, Player player) {
        PactDef pact = VotumTables.pactFor(signet, wax);
        if (pact == null) {
            player.sendOverlayMessage(Component.translatable("message.votum.desk_no_pact"));
            return;
        }
        waxCount--;
        held = ItemStack.EMPTY;
        give(player, VotumItems.withPact(VotumItems.SEALED_WARRANT.get(), pact.id()));
        returnWax(player);
        sound(SoundEvents.ANVIL_LAND, 1.8F);
        update();
    }

    private void press(ItemStack stack, SealPieceItem piece, Player player) {
        PactDef pact = PactItem.pactOf(held);
        if (pact == null) return;
        if (pact.piece() != piece.piece()) {
            player.sendOverlayMessage(Component.translatable(pact.piece() == PactDef.Piece.RING
                ? "message.votum.desk_needs_ring" : "message.votum.desk_needs_pendant"));
            return;
        }
        ItemStack out = stack.copyWithCount(1);
        out.set(VotumComponents.PACT.get(), pact.id());
        out.remove(VotumComponents.CRACKED.get());
        stack.consume(1, player);
        held = ItemStack.EMPTY;
        give(player, out);
        sound(SoundEvents.ANVIL_LAND, 1.6F);
        update();
    }

    private void restamp(Player player) {
        ItemStack out = held.copy();
        out.remove(VotumComponents.CRACKED.get());
        held = ItemStack.EMPTY;
        waxCount -= RESTAMP_WAX;
        give(player, out);
        returnWax(player);
        sound(SoundEvents.ANVIL_LAND, 1.6F);
        update();
    }

    public InteractionResult takeOut(Player player) {
        if (level == null || (held.isEmpty() && waxCount == 0)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            give(player, held);
            held = ItemStack.EMPTY;
            returnWax(player);
            sound(SoundEvents.BOOK_PUT, 0.8F);
            update();
        }
        return InteractionResult.SUCCESS;
    }

    private void returnWax(Player player) {
        if (waxCount > 0 && wax != null) give(player, VotumItems.wax(wax, waxCount));
        waxCount = 0;
        wax = null;
    }

    private static boolean isSignet(Item item) {
        for (Supplier<Item> s : VotumItems.signets()) {
            if (s.get() == item) return true;
        }
        for (PactDef p : VotumTables.pacts()) {
            if (p.signetItem() == item) return true;
        }
        return false;
    }

    private void sound(net.minecraft.sounds.SoundEvent event, float pitch) {
        if (level != null) level.playSound(null, worldPosition, event, SoundSource.BLOCKS, 0.7F, pitch);
    }

    private void update() {
        int contents;
        if (held.isEmpty()) contents = 0;
        else if (held.is(VotumItems.BLANK_WARRANT.get())) contents = waxCount > 0 ? 2 : 1;
        else contents = 3;
        BlockState s = getBlockState();
        if (level != null && s.getValue(SignetDeskBlock.CONTENTS) != contents) {
            level.setBlock(worldPosition, s.setValue(SignetDeskBlock.CONTENTS, contents), 3);
        }
        changed();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) {
            double x = pos.getX() + 0.5, y = pos.getY() + 0.8, z = pos.getZ() + 0.5;
            if (!held.isEmpty()) Containers.dropItemStack(level, x, y, z, held);
            if (waxCount > 0 && wax != null) Containers.dropItemStack(level, x, y, z, VotumItems.wax(wax, waxCount));
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (!held.isEmpty()) out.store("held", ItemStack.CODEC, held);
        if (wax != null) out.putString("wax", wax.toString());
        out.putInt("wax_count", waxCount);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        held = in.read("held", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        String w = in.getStringOr("wax", "");
        wax = w.isEmpty() ? null : Identifier.tryParse(w);
        waxCount = in.getIntOr("wax_count", 0);
        rerenderOnClient();
    }
}
