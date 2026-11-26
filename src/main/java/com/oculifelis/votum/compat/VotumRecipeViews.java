package com.oculifelis.votum.compat;

import com.fundamentum.recipeview.RecipeViews;
import com.fundamentum.recipeview.ViewCategory;
import com.oculifelis.votum.data.PactDef;
import com.oculifelis.votum.data.VotumTables;
import com.oculifelis.votum.data.WaxDef;
import com.oculifelis.votum.registry.VotumBlocks;
import com.oculifelis.votum.registry.VotumComponents;
import com.oculifelis.votum.registry.VotumItems;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Recipe viewer pages for melting wax, sealing warrants and pressing rings and pendants, all read from the tables. */
public final class VotumRecipeViews {
    private VotumRecipeViews() {}

    public static void init() {
        ViewCategory<WaxDef> melt = RecipeViews.category("votum:melting");
        melt.title(Component.translatable("jei.votum.melting"))
            .icon(() -> new ItemStack(VotumBlocks.SEAL_BRAZIER.get()))
            .size(140, 40).arrow(70, 6, 100)
            .catalysts(() -> List.of(new ItemStack(VotumBlocks.SEAL_BRAZIER.get())));
        int honey = melt.input(10, 6), pigment = melt.input(32, 6), wax = melt.output(104, 6);
        melt.recipes(VotumTables::waxes).key(w -> w.id().toString().replace(':', '/')).fill((w, page) -> {
            page.item(honey, new ItemStack(Items.HONEYCOMB)).items(pigment, pigments(w)).item(wax, VotumItems.wax(w.id(), 1));
            page.text(Component.translatable("jei.votum.melting_heat"), 28);
        });

        ViewCategory<PactDef> seal = RecipeViews.category("votum:sealing");
        seal.title(Component.translatable("jei.votum.sealing"))
            .icon(() -> new ItemStack(VotumBlocks.SIGNET_DESK.get()))
            .size(150, 64).arrow(82, 6, 60)
            .catalysts(() -> List.of(new ItemStack(VotumBlocks.SIGNET_DESK.get())));
        int blank = seal.input(6, 6), sWax = seal.input(28, 6), signet = seal.input(50, 6), sealed = seal.output(118, 6);
        seal.recipes(VotumTables::pacts).key(p -> p.id().toString().replace(':', '/')).fill((p, page) -> {
            Item sig = p.signetItem();
            page.item(blank, new ItemStack(VotumItems.BLANK_WARRANT.get()))
                .item(sWax, VotumItems.wax(p.wax(), 1))
                .item(signet, sig == null ? ItemStack.EMPTY : new ItemStack(sig))
                .item(sealed, VotumItems.withPact(VotumItems.SEALED_WARRANT.get(), p.id()));
            page.text(Component.translatable(p.nameKey()), 28);
            page.text(Component.translatable(p.oathKey()), 40);
            page.text(Component.translatable(p.clauseKey()), 52);
        });

        ViewCategory<PactDef> press = RecipeViews.category("votum:pressing");
        press.title(Component.translatable("jei.votum.pressing"))
            .icon(() -> new ItemStack(VotumItems.SIGNET_RING.get()))
            .size(140, 52).arrow(60, 6, 40)
            .catalysts(() -> List.of(new ItemStack(VotumBlocks.SIGNET_DESK.get())));
        int fulfilled = press.input(10, 6), piece = press.input(32, 6), pressed = press.output(104, 6);
        press.recipes(VotumTables::pacts).key(p -> p.id().toString().replace(':', '/')).fill((p, page) -> {
            Item pieceItem = p.piece() == PactDef.Piece.RING ? VotumItems.SIGNET_RING.get() : VotumItems.SEAL_PENDANT.get();
            page.item(fulfilled, VotumItems.withPact(VotumItems.FULFILLED_WARRANT.get(), p.id()))
                .item(piece, new ItemStack(pieceItem))
                .item(pressed, VotumItems.withPact(pieceItem, p.id()));
            page.text(Component.translatable(p.boonKey()), 28);
            page.text(Component.translatable("jei.votum.restamp"), 40);
        });

        RecipeViews.subtypes(VotumItems.SEALING_WAX, s -> String.valueOf(s.get(VotumComponents.WAX.get())));
        RecipeViews.subtypes(VotumItems.SEALED_WARRANT, s -> String.valueOf(s.get(VotumComponents.PACT.get())));
        RecipeViews.subtypes(VotumItems.FULFILLED_WARRANT, s -> String.valueOf(s.get(VotumComponents.PACT.get())));

        RecipeViews.info(() -> List.of(new ItemStack(VotumBlocks.CHARTER_POST.get())),
            () -> List.of(Component.translatable("jei.votum.charter_post")));
        RecipeViews.info(() -> List.of(new ItemStack(VotumItems.SIGNET_RING.get()), new ItemStack(VotumItems.SEAL_PENDANT.get())),
            () -> List.of(Component.translatable("jei.votum.seal_piece")));
    }

    private static List<ItemStack> pigments(WaxDef w) {
        List<ItemStack> out = new ArrayList<>();
        String p = w.pigment();
        if (p.startsWith("#")) {
            Identifier tag = Identifier.tryParse(p.substring(1));
            if (tag != null) {
                for (Holder<Item> h : BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, tag))) {
                    out.add(new ItemStack(h));
                }
            }
        } else {
            Item item = w.pigmentItem();
            if (item != null) out.add(new ItemStack(item));
        }
        return out;
    }
}
