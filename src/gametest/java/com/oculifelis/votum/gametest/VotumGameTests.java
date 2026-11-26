package com.oculifelis.votum.gametest;

import com.fundamentum.slots.Slots;
import com.mojang.authlib.GameProfile;
import com.oculifelis.votum.Votum;
import com.oculifelis.votum.VotumConfig;
import com.oculifelis.votum.data.PactDef;
import com.oculifelis.votum.data.VotumTables;
import com.oculifelis.votum.pact.PactLedger;
import com.oculifelis.votum.pact.Pacts;
import com.oculifelis.votum.registry.VotumBlocks;
import com.oculifelis.votum.registry.VotumItems;
import com.oculifelis.votum.world.CharterPostBlockEntity;
import com.oculifelis.votum.world.SealBrazierBlockEntity;
import com.oculifelis.votum.world.SignetDeskBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/** Server-side checks of the tables, the three blocks, swearing, breaking and the worn boons. */
@EventBusSubscriber(modid = Votum.MODID)
public final class VotumGameTests {
    private record Test(Consumer<GameTestHelper> body, int maxTicks) {}

    private static final Map<String, Test> TESTS = new LinkedHashMap<>();

    static {
        TESTS.put("tables_loaded", new Test(VotumGameTests::tablesLoaded, 20));
        TESTS.put("brazier_melts_wax", new Test(VotumGameTests::brazierMeltsWax, VotumConfig.brazierTicks() + 60));
        TESTS.put("desk_seals_warrant", new Test(VotumGameTests::deskSealsWarrant, 20));
        TESTS.put("charter_post_copies", new Test(VotumGameTests::postCopies, 20));
        TESTS.put("swear_and_kill_progress", new Test(VotumGameTests::swearAndKill, 40));
        TESTS.put("clause_voids_pact", new Test(VotumGameTests::clauseVoids, 40));
        TESTS.put("sworn_limit", new Test(VotumGameTests::swornLimit, 20));
        TESTS.put("fulfil_and_press", new Test(VotumGameTests::fulfilAndPress, 20));
        TESTS.put("worn_piece_boon", new Test(VotumGameTests::wornBoon, 40));
    }

    private VotumGameTests() {}

    @SubscribeEvent
    public static void registerFunctions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, helper ->
            TESTS.forEach((name, test) -> helper.register(Votum.id(name), test.body())));
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        TESTS.forEach((name, test) -> {
            Holder<TestEnvironmentDefinition<?>> env =
                event.registerEnvironment(Votum.id("env_" + name), new TestEnvironmentDefinition.AllOf(List.of()));
            event.registerTest(Votum.id(name), new FunctionGameTestInstance(
                ResourceKey.create(Registries.TEST_FUNCTION, Votum.id(name)),
                new TestData<>(env, Votum.id("empty"), test.maxTicks(), 0, true)));
        });
    }

    private static void check(GameTestHelper helper, boolean ok, String message) {
        if (!ok) helper.fail(message);
    }

    private static PactDef pact(GameTestHelper helper, String path) {
        PactDef p = VotumTables.pact(Votum.id(path));
        if (p == null) helper.fail("missing pact " + path);
        return p;
    }

    private static Item pieceFor(PactDef pact) {
        return pact.piece() == PactDef.Piece.RING ? VotumItems.SIGNET_RING.get() : VotumItems.SEAL_PENDANT.get();
    }

    /** A player never added to the player list, so Curios does not try to sync over a missing connection. */
    private static ServerPlayer testPlayer(GameTestHelper helper) {
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "votum_test"));
        Vec3 at = helper.absoluteVec(new Vec3(1.5, 2, 1.5));
        player.setPos(at.x, at.y, at.z);
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(inv -> {
            if (inv.getCurios().isEmpty()) inv.reset();
        });
        return player;
    }

    private static int count(ServerPlayer player, Item item, Identifier pact) {
        int n = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(item) && (pact == null || pact.equals(VotumItems.pactOf(s)))) n += s.getCount();
        }
        return n;
    }

    private static void killAs(GameTestHelper helper, ServerPlayer player, EntityType<? extends LivingEntity> type) {
        LivingEntity victim = helper.spawn(type, new BlockPos(2, 2, 2));
        ServerLevel level = helper.getLevel();
        victim.hurtServer(level, level.damageSources().playerAttack(player), 10000.0F);
        check(helper, victim.isDeadOrDying(), type + " did not die");
    }

    // --- data ---

    private static void tablesLoaded(GameTestHelper helper) {
        check(helper, VotumTables.waxes().size() == 6, "expected 6 waxes, got " + VotumTables.waxes().size());
        check(helper, VotumTables.pacts().size() == 6, "expected 6 pacts, got " + VotumTables.pacts().size());
        for (String p : List.of("grave_watch", "shepherds_compact", "merchants_bond", "silk_truce", "drowned_accord", "ender_covenant")) {
            PactDef def = pact(helper, p);
            check(helper, def.signetItem() != null, p + " has no signet item");
            check(helper, VotumTables.wax(def.wax()) != null, p + " names a missing wax");
            check(helper, VotumTables.pactFor(def.signetItem(), def.wax()) == def, p + " is not found by signet and wax");
        }
        check(helper, VotumTables.waxForPigment(new ItemStack(Items.REDSTONE)) != null, "redstone is not a wax pigment");
        helper.succeed();
    }

    // --- blocks ---

    private static void brazierMeltsWax(GameTestHelper helper) {
        ServerPlayer player = testPlayer(helper);
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos.below(), Blocks.MAGMA_BLOCK);
        helper.setBlock(pos, VotumBlocks.SEAL_BRAZIER.get());
        SealBrazierBlockEntity be = helper.getBlockEntity(pos, SealBrazierBlockEntity.class);
        check(helper, be.hasHeat(), "brazier on magma has no heat");
        be.useItem(new ItemStack(Items.HONEYCOMB, 1), player);
        be.useItem(new ItemStack(Items.HONEYCOMB, 1), player);
        check(helper, be.honey() == 2, "expected 2 honey, got " + be.honey());
        be.useItem(new ItemStack(Items.REDSTONE), player);
        check(helper, Votum.id("crimson").equals(be.wax()), "redstone did not start crimson wax: " + be.wax());
        check(helper, be.remaining() > 0, "wax melted at once");
        be.takeOut(player);
        check(helper, be.honey() == 2, "wax came out before it melted");
        helper.runAfterDelay(VotumConfig.brazierTicks() + 5, () -> {
            check(helper, be.remaining() == 0, "wax still melting");
            be.takeOut(player);
            check(helper, count(player, VotumItems.SEALING_WAX.get(), null) == 2, "expected 2 wax out");
            check(helper, be.honey() == 0 && be.wax() == null, "brazier not emptied");
            helper.succeed();
        });
    }

    private static void deskSealsWarrant(GameTestHelper helper) {
        ServerPlayer player = testPlayer(helper);
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, VotumBlocks.SIGNET_DESK.get());
        SignetDeskBlockEntity be = helper.getBlockEntity(pos, SignetDeskBlockEntity.class);
        PactDef grave = pact(helper, "grave_watch");
        be.useItem(new ItemStack(VotumItems.BLANK_WARRANT.get()), player);
        be.useItem(VotumItems.wax(grave.wax(), 2), player);
        check(helper, be.waxCount() == 1, "expected 1 wax on the desk, got " + be.waxCount());
        ItemStack signet = new ItemStack(grave.signetItem());
        be.useItem(signet, player);
        check(helper, count(player, VotumItems.SEALED_WARRANT.get(), grave.id()) == 1, "no sealed Grave Watch warrant");
        check(helper, be.held().isEmpty() && be.waxCount() == 0, "desk not cleared after sealing");
        check(helper, !signet.isEmpty(), "signet was used up");
        helper.succeed();
    }

    private static void postCopies(GameTestHelper helper) {
        ServerPlayer player = testPlayer(helper);
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, VotumBlocks.CHARTER_POST.get());
        BlockPos abs = helper.absolutePos(pos);
        BlockState state = helper.getLevel().getBlockState(abs);
        state.getBlock().setPlacedBy(helper.getLevel(), abs, state, player, new ItemStack(VotumBlocks.CHARTER_POST.get()));
        CharterPostBlockEntity be = helper.getBlockEntity(pos, CharterPostBlockEntity.class);
        check(helper, be.isOwner(player), "placer does not own the post");
        PactDef grave = pact(helper, "grave_watch");
        be.useItem(VotumItems.withPact(VotumItems.SEALED_WARRANT.get(), grave.id()), player);
        check(helper, grave.id().equals(be.pinned()), "warrant not pinned");
        player.getInventory().add(VotumItems.wax(grave.wax(), 1));
        be.useItem(new ItemStack(VotumItems.BLANK_WARRANT.get()), player);
        check(helper, count(player, VotumItems.SEALED_WARRANT.get(), grave.id()) == 1, "post did not copy the warrant");
        check(helper, count(player, VotumItems.SEALING_WAX.get(), null) == 0, "copy did not use the wax");
        helper.succeed();
    }

    // --- pacts ---

    private static void swearAndKill(GameTestHelper helper) {
        ServerPlayer player = testPlayer(helper);
        PactDef grave = pact(helper, "grave_watch");
        check(helper, Pacts.swear(player, grave), "could not swear Grave Watch");
        check(helper, !Pacts.swear(player, grave), "swore Grave Watch twice");
        killAs(helper, player, EntityType.ZOMBIE);
        killAs(helper, player, EntityType.SKELETON);
        killAs(helper, player, EntityType.PIG);
        PactLedger.Sworn s = PactLedger.of(player).sworn().get(grave.id());
        check(helper, s != null && s.progress() == 2, "expected progress 2, got " + (s == null ? "none" : s.progress()));
        helper.succeed();
    }

    private static void clauseVoids(GameTestHelper helper) {
        ServerPlayer player = testPlayer(helper);
        PactDef shep = pact(helper, "shepherds_compact");
        Pacts.swear(player, shep);
        killAs(helper, player, EntityType.ZOMBIE);
        check(helper, PactLedger.of(player).isSworn(shep.id()), "a zombie kill voided the shepherds compact");
        killAs(helper, player, EntityType.COW);
        check(helper, !PactLedger.of(player).isSworn(shep.id()), "killing a cow did not void the shepherds compact");
        helper.succeed();
    }

    private static void swornLimit(GameTestHelper helper) {
        ServerPlayer player = testPlayer(helper);
        List<String> ids = List.of("grave_watch", "shepherds_compact", "silk_truce", "drowned_accord");
        int max = VotumConfig.maxSworn();
        for (int i = 0; i < max; i++) {
            check(helper, Pacts.swear(player, pact(helper, ids.get(i))), "could not swear pact " + (i + 1));
        }
        check(helper, !Pacts.swear(player, pact(helper, ids.get(max))), "swore more than " + max + " pacts");
        helper.succeed();
    }

    private static void fulfilAndPress(GameTestHelper helper) {
        ServerPlayer player = testPlayer(helper);
        PactDef grave = pact(helper, "grave_watch");
        Pacts.swear(player, grave);
        Pacts.advance(player, grave, grave.oath().goal());
        check(helper, PactLedger.of(player).isFinished(grave.id()), "pact not finished at its goal");
        check(helper, count(player, VotumItems.FULFILLED_WARRANT.get(), grave.id()) == 1, "no fulfilled warrant given");
        check(helper, !Pacts.swear(player, grave), "a finished pact was sworn again");

        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, VotumBlocks.SIGNET_DESK.get());
        SignetDeskBlockEntity be = helper.getBlockEntity(pos, SignetDeskBlockEntity.class);
        be.useItem(VotumItems.withPact(VotumItems.FULFILLED_WARRANT.get(), grave.id()), player);
        Item piece = pieceFor(grave);
        be.useItem(new ItemStack(piece), player);
        check(helper, count(player, piece, grave.id()) == 1, "no pressed piece for Grave Watch");
        helper.succeed();
    }

    private static void wornBoon(GameTestHelper helper) {
        ServerPlayer player = testPlayer(helper);
        PactDef shep = pact(helper, "shepherds_compact");
        double before = player.getAttributeValue(Attributes.LUCK);
        check(helper, Slots.size(player, Pacts.SLOT) == 2, "expected 2 seal slots, got " + Slots.size(player, Pacts.SLOT));
        Slots.set(player, Pacts.SLOT, 0, VotumItems.withPact(pieceFor(shep), shep.id()));
        Pacts.recompute(player);
        check(helper, Pacts.worn(player).contains(shep), "worn piece not counted");
        check(helper, Math.abs(player.getAttributeValue(Attributes.LUCK) - before - 1.0) < 0.01,
            "luck did not rise by 1: " + before + " -> " + player.getAttributeValue(Attributes.LUCK));
        killAs(helper, player, EntityType.SHEEP);
        check(helper, Pacts.worn(player).isEmpty(), "breaking the clause did not crack the piece");
        check(helper, Math.abs(player.getAttributeValue(Attributes.LUCK) - before) < 0.01, "luck stayed after the crack");
        helper.succeed();
    }
}
