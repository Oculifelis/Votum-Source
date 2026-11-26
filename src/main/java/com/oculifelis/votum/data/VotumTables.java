package com.oculifelis.votum.data;

import com.fundamentum.data.DataTable;
import com.fundamentum.data.DataTables;
import com.fundamentum.recipeview.RecipeViews;
import com.oculifelis.votum.Votum;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;

public final class VotumTables {
    public static final DataTable<WaxDef> WAXES = DataTables.register(Votum.id("waxes"), "votum/waxes", WaxDef::fromJson)
        .keyField("wax")
        .merge(DataTable.Merge.DEEP)
        .synced()
        .onLoad(t -> RecipeViews.refresh());

    public static final DataTable<PactDef> PACTS = DataTables.register(Votum.id("pacts"), "votum/pacts", PactDef::fromJson)
        .keyField("pact")
        .merge(DataTable.Merge.DEEP)
        .synced()
        .onLoad(t -> RecipeViews.refresh());

    private VotumTables() {}

    public static void init() {
        // Loading the class registers the tables.
    }

    public static Collection<WaxDef> waxes() {
        return WAXES.entries().values();
    }

    public static Collection<PactDef> pacts() {
        return PACTS.entries().values();
    }

    public static WaxDef wax(Identifier id) {
        return id == null ? null : WAXES.get(id);
    }

    public static PactDef pact(Identifier id) {
        return id == null ? null : PACTS.get(id);
    }

    /** The wax a pigment item makes, or null. */
    public static WaxDef waxForPigment(ItemStack stack) {
        for (WaxDef w : waxes()) {
            if (w.isPigment(stack)) return w;
        }
        return null;
    }

    /** The pact a signet and wax colour seal together, or null. */
    public static PactDef pactFor(Item signet, Identifier wax) {
        for (PactDef p : pacts()) {
            if (p.wax().equals(wax) && p.signetItem() == signet) return p;
        }
        return null;
    }
}
