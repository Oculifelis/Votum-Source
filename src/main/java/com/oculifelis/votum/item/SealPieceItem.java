package com.oculifelis.votum.item;

import com.oculifelis.votum.data.PactDef;
import com.oculifelis.votum.registry.VotumItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** A signet ring or seal pendant. Pressed with a fulfilled warrant, it gives that pact's boon while worn in a seal slot. */
public class SealPieceItem extends PactItem {
    private final PactDef.Piece piece;

    public SealPieceItem(Properties props, PactDef.Piece piece) {
        super(props);
        this.piece = piece;
    }

    public PactDef.Piece piece() {
        return piece;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> out, TooltipFlag flag) {
        PactDef pact = pactOf(stack);
        if (pact == null) {
            out.accept(Component.translatable("tooltip.votum.unpressed").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (VotumItems.isCracked(stack)) {
            out.accept(Component.translatable("tooltip.votum.cracked").withStyle(ChatFormatting.DARK_RED));
        }
        out.accept(Component.translatable("tooltip.votum.boon", Component.translatable(pact.boonKey())).withStyle(ChatFormatting.GOLD));
        out.accept(Component.translatable("tooltip.votum.clause", Component.translatable(pact.clauseKey())).withStyle(ChatFormatting.RED));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return VotumItems.pactOf(stack) != null && !VotumItems.isCracked(stack);
    }
}
