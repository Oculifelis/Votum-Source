package com.oculifelis.votum.registry;

import com.oculifelis.votum.Votum;
import com.oculifelis.votum.data.PactDef;
import com.oculifelis.votum.data.VotumTables;
import com.oculifelis.votum.data.WaxDef;
import com.oculifelis.votum.item.PactItem;
import com.oculifelis.votum.item.SealPieceItem;
import com.oculifelis.votum.item.SealedWarrantItem;
import com.oculifelis.votum.item.SealingWaxItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public final class VotumItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, Votum.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Votum.MODID);

    public static final DeferredHolder<Item, SealingWaxItem> SEALING_WAX = register("sealing_wax", SealingWaxItem::new,
        () -> new Item.Properties().stacksTo(64));
    public static final DeferredHolder<Item, Item> BLANK_WARRANT = register("blank_warrant", Item::new,
        () -> new Item.Properties().stacksTo(16));
    public static final DeferredHolder<Item, SealedWarrantItem> SEALED_WARRANT = register("sealed_warrant", SealedWarrantItem::new,
        () -> new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final DeferredHolder<Item, PactItem> FULFILLED_WARRANT = register("fulfilled_warrant", PactItem::new,
        () -> new Item.Properties().stacksTo(1).rarity(Rarity.RARE));

    public static final DeferredHolder<Item, Item> SKULL_SIGNET = signet("skull_signet");
    public static final DeferredHolder<Item, Item> HOOF_SIGNET = signet("hoof_signet");
    public static final DeferredHolder<Item, Item> COIN_SIGNET = signet("coin_signet");
    public static final DeferredHolder<Item, Item> FANG_SIGNET = signet("fang_signet");
    public static final DeferredHolder<Item, Item> TIDE_SIGNET = signet("tide_signet");
    public static final DeferredHolder<Item, Item> EYE_SIGNET = signet("eye_signet");

    public static final DeferredHolder<Item, SealPieceItem> SIGNET_RING = register("signet_ring",
        p -> new SealPieceItem(p, PactDef.Piece.RING), () -> new Item.Properties().stacksTo(1));
    public static final DeferredHolder<Item, SealPieceItem> SEAL_PENDANT = register("seal_pendant",
        p -> new SealPieceItem(p, PactDef.Piece.PENDANT), () -> new Item.Properties().stacksTo(1));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () ->
        CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.votum"))
            .icon(() -> new ItemStack(SKULL_SIGNET.get()))
            .displayItems((params, out) -> {
                for (DeferredHolder<Item, BlockItem> b : VotumBlocks.blockItems()) out.accept(new ItemStack(b.get()));
                out.accept(new ItemStack(BLANK_WARRANT.get()));
                for (WaxDef w : VotumTables.waxes()) out.accept(wax(w.id(), 1));
                for (Supplier<Item> s : signets()) out.accept(new ItemStack(s.get()));
                out.accept(new ItemStack(SIGNET_RING.get()));
                out.accept(new ItemStack(SEAL_PENDANT.get()));
                for (PactDef p : VotumTables.pacts()) out.accept(withPact(SEALED_WARRANT.get(), p.id()));
            })
            .build());

    private VotumItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        TABS.register(bus);
    }

    public static List<Supplier<Item>> signets() {
        return List.of(SKULL_SIGNET::get, HOOF_SIGNET::get, COIN_SIGNET::get, FANG_SIGNET::get, TIDE_SIGNET::get, EYE_SIGNET::get);
    }

    public static ItemStack wax(Identifier wax, int count) {
        ItemStack s = new ItemStack(SEALING_WAX.get(), count);
        s.set(VotumComponents.WAX.get(), wax);
        return s;
    }

    public static ItemStack withPact(Item item, Identifier pact) {
        ItemStack s = new ItemStack(item);
        s.set(VotumComponents.PACT.get(), pact);
        return s;
    }

    public static Identifier waxOf(ItemStack stack) {
        return stack.isEmpty() ? null : stack.get(VotumComponents.WAX.get());
    }

    public static Identifier pactOf(ItemStack stack) {
        return stack.isEmpty() ? null : stack.get(VotumComponents.PACT.get());
    }

    public static boolean isCracked(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(VotumComponents.CRACKED.get()));
    }

    private static DeferredHolder<Item, Item> signet(String name) {
        return register(name, Item::new, () -> new Item.Properties().stacksTo(1));
    }

    private static <I extends Item> DeferredHolder<Item, I> register(String name, Function<Item.Properties, I> factory,
                                                                    Supplier<Item.Properties> props) {
        Identifier id = Votum.id(name);
        return ITEMS.register(name, () -> factory.apply(props.get().setId(ResourceKey.create(Registries.ITEM, id))));
    }
}
