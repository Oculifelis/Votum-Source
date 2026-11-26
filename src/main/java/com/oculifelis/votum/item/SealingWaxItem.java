package com.oculifelis.votum.item;

import com.oculifelis.votum.data.VotumTables;
import com.oculifelis.votum.data.WaxDef;
import com.oculifelis.votum.registry.VotumItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Sealing wax. Its colour is the WAX component; the name reads "Crimson Sealing Wax" and so on. */
public class SealingWaxItem extends Item {
    public SealingWaxItem(Properties props) {
        super(props);
    }

    @Override
    public Component getName(ItemStack stack) {
        Identifier id = VotumItems.waxOf(stack);
        WaxDef wax = id == null ? null : VotumTables.wax(id);
        if (wax == null) return super.getName(stack);
        return Component.translatable(getDescriptionId() + ".named", Component.translatable(wax.langKey())).withColor(wax.color());
    }
}
