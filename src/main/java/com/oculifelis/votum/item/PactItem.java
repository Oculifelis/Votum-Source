package com.oculifelis.votum.item;

import com.oculifelis.votum.data.PactDef;
import com.oculifelis.votum.data.VotumTables;
import com.oculifelis.votum.registry.VotumItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** An item that carries a pact: warrants, rings and pendants. Its name and tooltip come from the pact. */
public class PactItem extends Item {
    public PactItem(Properties props) {
        super(props);
    }

    @Override
    public Component getName(ItemStack stack) {
        PactDef pact = pactOf(stack);
        if (pact == null) return super.getName(stack);
        return Component.translatable(getDescriptionId() + ".named", Component.translatable(pact.nameKey()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> out, TooltipFlag flag) {
        PactDef pact = pactOf(stack);
        if (pact == null) {
            Identifier id = VotumItems.pactOf(stack);
            if (id != null) out.accept(Component.translatable("tooltip.votum.unknown_pact", id.toString()).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        describe(pact, out);
    }

    public static void describe(PactDef pact, Consumer<Component> out) {
        out.accept(Component.translatable("tooltip.votum.oath", Component.translatable(pact.oathKey())).withStyle(ChatFormatting.GRAY));
        out.accept(Component.translatable("tooltip.votum.clause", Component.translatable(pact.clauseKey())).withStyle(ChatFormatting.RED));
        out.accept(Component.translatable("tooltip.votum.boon", Component.translatable(pact.boonKey())).withStyle(ChatFormatting.GOLD));
    }

    public static PactDef pactOf(ItemStack stack) {
        Identifier id = VotumItems.pactOf(stack);
        return id == null ? null : VotumTables.pact(id);
    }
}
