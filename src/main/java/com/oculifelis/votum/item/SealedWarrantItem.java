package com.oculifelis.votum.item;

import com.oculifelis.votum.data.PactDef;
import com.oculifelis.votum.pact.Pacts;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Using a sealed warrant swears its pact. */
public class SealedWarrantItem extends PactItem {
    public SealedWarrantItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        PactDef pact = pactOf(stack);
        if (pact == null) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp && Pacts.swear(sp, pact)) {
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }
}
