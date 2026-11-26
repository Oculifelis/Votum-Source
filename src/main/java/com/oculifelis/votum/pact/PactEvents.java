package com.oculifelis.votum.pact;

import com.fundamentum.slots.Slots;
import com.oculifelis.votum.data.PactDef;
import com.oculifelis.votum.data.VotumTables;
import com.oculifelis.votum.data.Targets;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.EnderManAngerEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.Map;

/** Watches the deeds that move oaths forward and the ones that break clauses. */
public final class PactEvents {
    private static final int ABSTAIN_CHECK_TICKS = 600;

    private PactEvents() {}

    public static void init() {
        NeoForge.EVENT_BUS.addListener(PactEvents::onDeath);
        NeoForge.EVENT_BUS.addListener(PactEvents::onHurt);
        NeoForge.EVENT_BUS.addListener(PactEvents::onBreed);
        NeoForge.EVENT_BUS.addListener(PactEvents::onTrade);
        NeoForge.EVENT_BUS.addListener(PactEvents::onTarget);
        NeoForge.EVENT_BUS.addListener(PactEvents::onEndermanAnger);
        NeoForge.EVENT_BUS.addListener(PactEvents::onServerTick);
        NeoForge.EVENT_BUS.addListener(PactEvents::onLogin);
        NeoForge.EVENT_BUS.addListener(PactEvents::onRespawn);
        Slots.onChange(Pacts::recompute);
    }

    private static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;
        Entity source = event.getSource().getEntity();
        if (source instanceof ServerPlayer killer && killer != victim) {
            for (PactDef pact : Pacts.bound(killer)) {
                if (pact.clause().type() == PactDef.ClauseType.KILL && pact.clause().targets().matches(victim)) {
                    Pacts.breach(killer, pact);
                }
            }
            forSworn(killer, PactDef.OathType.KILL, victim);
        }
        if (victim instanceof ServerPlayer dead && source != null) {
            for (PactDef pact : Pacts.bound(dead)) {
                if (pact.clause().type() == PactDef.ClauseType.DIE_TO && pact.clause().targets().matches(source)) {
                    Pacts.breach(dead, pact);
                }
            }
        }
    }

    private static void onHurt(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker) || attacker == target) return;
        for (PactDef pact : Pacts.bound(attacker)) {
            if (pact.clause().type() == PactDef.ClauseType.HURT && pact.clause().targets().matches(target)) {
                Pacts.breach(attacker, pact);
            }
        }
    }

    private static void onBreed(BabyEntitySpawnEvent event) {
        if (event.getCausedByPlayer() instanceof ServerPlayer player && event.getParentA() != null) {
            forSworn(player, PactDef.OathType.BREED, event.getParentA());
        }
    }

    private static void onTrade(TradeWithVillagerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            forSworn(player, PactDef.OathType.TRADE, event.getAbstractVillager());
        }
    }

    private static void onTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target instanceof ServerPlayer player && Pacts.truce(player, event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private static void onEndermanAnger(EnderManAngerEvent event) {
        if (event.getPlayer() != null && !event.getPlayer().level().isClientSide() && Pacts.gazeSafe(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % ABSTAIN_CHECK_TICKS != 0) return;
        for (ServerPlayer player : List.copyOf(event.getServer().getPlayerList().getPlayers())) {
            Pacts.checkAbstain(player);
        }
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Pacts.checkAbstain(player);
            Pacts.recompute(player);
        }
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) Pacts.recompute(player);
    }

    /** Moves every sworn oath of this kind forward when the deed's creature matches (an empty list matches any). */
    private static void forSworn(ServerPlayer player, PactDef.OathType type, Entity subject) {
        for (Map.Entry<Identifier, PactLedger.Sworn> e : List.copyOf(PactLedger.of(player).sworn().entrySet())) {
            PactDef pact = VotumTables.pact(e.getKey());
            if (pact == null || pact.oath().type() != type) continue;
            Targets t = pact.oath().targets();
            if (t.isEmpty() || t.matches(subject)) Pacts.advance(player, pact, 1);
        }
    }
}
