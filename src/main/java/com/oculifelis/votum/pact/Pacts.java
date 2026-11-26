package com.oculifelis.votum.pact;

import com.fundamentum.faction.Factions;
import com.fundamentum.port.FId;
import com.fundamentum.slots.Slots;
import com.oculifelis.votum.Votum;
import com.oculifelis.votum.VotumConfig;
import com.oculifelis.votum.data.PactDef;
import com.oculifelis.votum.data.VotumTables;
import com.oculifelis.votum.item.PactItem;
import com.oculifelis.votum.item.SealPieceItem;
import com.oculifelis.votum.registry.VotumComponents;
import com.oculifelis.votum.registry.VotumItems;
import com.oculifelis.votum.world.CharterPostBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Swearing, progress, finishing and breaking pacts, and the boons of worn seal pieces. */
public final class Pacts {
    /** The Curios slot type seal pieces are worn in. */
    public static final String SLOT = "seal";

    private Pacts() {}

    // ---- swearing and progress ----

    public static boolean swear(ServerPlayer player, PactDef pact) {
        PactLedger.State s = PactLedger.of(player);
        Component name = Component.translatable(pact.nameKey());
        if (s.isFinished(pact.id())) {
            player.sendSystemMessage(Component.translatable("message.votum.already_finished", name).withStyle(ChatFormatting.GRAY));
            return false;
        }
        if (s.isSworn(pact.id())) {
            player.sendSystemMessage(Component.translatable("message.votum.already_sworn", name).withStyle(ChatFormatting.GRAY));
            return false;
        }
        int max = VotumConfig.maxSworn();
        if (s.sworn().size() >= max) {
            player.sendSystemMessage(Component.translatable("message.votum.too_many", max).withStyle(ChatFormatting.GRAY));
            return false;
        }
        PactLedger.set(player, s.withSworn(pact.id(), new PactLedger.Sworn(0, player.level().getGameTime())));
        player.sendSystemMessage(Component.translatable("message.votum.sworn", name).withStyle(ChatFormatting.GOLD));
        PactItem.describe(pact, player::sendSystemMessage);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.8F);
        return true;
    }

    public static void advance(ServerPlayer player, PactDef pact, int amount) {
        PactLedger.State s = PactLedger.of(player);
        PactLedger.Sworn w = s.sworn().get(pact.id());
        if (w == null) return;
        int progress = w.progress() + amount;
        if (progress >= pact.oath().goal()) {
            finish(player, pact);
            return;
        }
        PactLedger.set(player, s.withSworn(pact.id(), new PactLedger.Sworn(progress, w.since())));
        player.sendOverlayMessage(Component.translatable("message.votum.progress",
            Component.translatable(pact.nameKey()), progress, pact.oath().goal()));
    }

    public static void finish(ServerPlayer player, PactDef pact) {
        PactLedger.State s = PactLedger.of(player);
        if (!s.isSworn(pact.id())) return;
        PactLedger.set(player, s.withSworn(pact.id(), null).withFinished(pact.id()));
        ItemStack warrant = VotumItems.withPact(VotumItems.FULFILLED_WARRANT.get(), pact.id());
        if (!player.getInventory().add(warrant)) player.drop(warrant, false);
        addRep(player, pact.faction(), pact.reward());
        player.sendSystemMessage(Component.translatable("message.votum.fulfilled", Component.translatable(pact.nameKey())).withStyle(ChatFormatting.GOLD));
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.2F);
        CharterPostBlockEntity.refreshFor(player);
    }

    /** Checks abstain oaths: a day counts once 24000 ticks have passed since swearing. */
    public static void checkAbstain(ServerPlayer player) {
        long now = player.level().getGameTime();
        for (Map.Entry<Identifier, PactLedger.Sworn> e : List.copyOf(PactLedger.of(player).sworn().entrySet())) {
            PactDef pact = VotumTables.pact(e.getKey());
            if (pact == null || pact.oath().type() != PactDef.OathType.ABSTAIN) continue;
            int days = (int) ((now - e.getValue().since()) / 24000L);
            if (days >= pact.oath().days()) {
                finish(player, pact);
            } else if (days > e.getValue().progress()) {
                PactLedger.State s = PactLedger.of(player);
                PactLedger.set(player, s.withSworn(pact.id(), new PactLedger.Sworn(days, e.getValue().since())));
            }
        }
    }

    // ---- breaking ----

    /** The player broke the clause of a pact: a sworn copy is void, and worn pieces of it crack. */
    public static void breach(ServerPlayer player, PactDef pact) {
        boolean hit = false;
        PactLedger.State s = PactLedger.of(player);
        if (s.isSworn(pact.id())) {
            PactLedger.set(player, s.withSworn(pact.id(), null));
            player.sendSystemMessage(Component.translatable("message.votum.voided", Component.translatable(pact.nameKey())).withStyle(ChatFormatting.DARK_RED));
            hit = true;
        }
        if (crackWorn(player, pact)) {
            player.sendSystemMessage(Component.translatable("message.votum.cracked", Component.translatable(pact.nameKey())).withStyle(ChatFormatting.DARK_RED));
            hit = true;
        }
        if (hit) {
            addRep(player, pact.faction(), -pact.penalty());
            player.level().playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8F, 0.6F);
            recompute(player);
        }
    }

    private static boolean crackWorn(LivingEntity e, PactDef pact) {
        boolean any = false;
        int size = Slots.size(e, SLOT);
        for (int i = 0; i < size; i++) {
            ItemStack stack = Slots.get(e, SLOT, i);
            if (!pact.id().equals(VotumItems.pactOf(stack)) || VotumItems.isCracked(stack)) continue;
            ItemStack copy = stack.copy();
            copy.set(VotumComponents.CRACKED.get(), true);
            Slots.set(e, SLOT, i, copy);
            any = true;
        }
        return any;
    }

    /** Every pact this player is bound by right now: sworn ones and those of worn, uncracked pieces. */
    public static List<PactDef> bound(ServerPlayer player) {
        Map<Identifier, PactDef> out = new LinkedHashMap<>();
        for (Identifier id : PactLedger.of(player).sworn().keySet()) {
            PactDef p = VotumTables.pact(id);
            if (p != null) out.put(id, p);
        }
        for (PactDef p : worn(player)) out.putIfAbsent(p.id(), p);
        return new ArrayList<>(out.values());
    }

    // ---- boons ----

    /** The pacts of uncracked pieces worn in seal slots, each pact once. */
    public static List<PactDef> worn(LivingEntity e) {
        Map<Identifier, PactDef> out = new LinkedHashMap<>();
        if (!Slots.curiosLoaded()) return List.of();
        for (ItemStack stack : Slots.stacks(e, SLOT)) {
            if (!(stack.getItem() instanceof SealPieceItem piece) || VotumItems.isCracked(stack)) continue;
            PactDef pact = PactItem.pactOf(stack);
            if (pact != null && pact.piece() == piece.piece()) out.putIfAbsent(pact.id(), pact);
        }
        return new ArrayList<>(out.values());
    }

    public static boolean truce(LivingEntity wearer, Entity creature) {
        for (PactDef p : worn(wearer)) {
            if (p.boon().truce().matches(creature)) return true;
        }
        return false;
    }

    public static boolean gazeSafe(LivingEntity wearer) {
        for (PactDef p : worn(wearer)) {
            if (p.boon().gazeSafe()) return true;
        }
        return false;
    }

    static Identifier modifierId(PactDef pact, int index) {
        return Votum.id("pact/" + pact.id().getNamespace() + "/" + pact.id().getPath() + "/" + index);
    }

    /** Puts the attribute changes and faction standing of worn pieces in place, and takes back those no longer worn. */
    public static void recompute(LivingEntity e) {
        if (!(e instanceof ServerPlayer player)) return;
        List<PactDef> active = worn(player);
        for (PactDef pact : VotumTables.pacts()) {
            boolean on = active.contains(pact);
            List<PactDef.Mod> mods = pact.boon().modifiers();
            for (int i = 0; i < mods.size(); i++) {
                Holder<Attribute> attr = mods.get(i).resolve();
                if (attr == null) continue;
                AttributeInstance inst = player.getAttribute(attr);
                if (inst == null) continue;
                Identifier id = modifierId(pact, i);
                if (on) inst.addOrUpdateTransientModifier(new AttributeModifier(id, mods.get(i).amount(), mods.get(i).operation()));
                else inst.removeModifier(id);
            }
        }
        PactLedger.State s = PactLedger.of(player);
        PactLedger.State next = s;
        for (PactDef pact : active) {
            if (pact.faction() == null || pact.boon().standing() == 0 || s.standing().containsKey(pact.id())) continue;
            addRep(player, pact.faction(), pact.boon().standing());
            next = next.withStanding(pact.id(), pact.boon().standing());
        }
        for (Map.Entry<Identifier, Integer> given : s.standing().entrySet()) {
            boolean still = active.stream().anyMatch(p -> p.id().equals(given.getKey()));
            if (still) continue;
            PactDef pact = VotumTables.pact(given.getKey());
            if (pact != null) addRep(player, pact.faction(), -given.getValue());
            next = next.withStanding(given.getKey(), 0);
        }
        if (next != s) PactLedger.set(player, next);
    }

    static void addRep(ServerPlayer player, Identifier faction, int amount) {
        if (faction == null || amount == 0) return;
        FId id = FId.from(faction);
        if (Factions.get(id) == null) return;
        Factions.addRep(player, id, amount);
    }
}
