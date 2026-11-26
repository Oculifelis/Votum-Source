package com.oculifelis.votum.pact;

import com.fundamentum.port.FAttach;
import com.fundamentum.port.FId;
import com.fundamentum.port.FStreamCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.oculifelis.votum.Votum;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Each player's pacts: the ones sworn and unfinished with their progress and the game time they were sworn, the ones
 * finished, and the faction standing a worn piece has added (so it can be taken back when the piece comes off).
 */
public final class PactLedger {
    /** Progress on one sworn pact. {@code since} is the game time it was sworn. */
    public record Sworn(int progress, long since) {
        public static final Codec<Sworn> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("progress").forGetter(Sworn::progress),
            Codec.LONG.fieldOf("since").forGetter(Sworn::since)
        ).apply(i, Sworn::new));

        public static final FStreamCodec<Sworn> STREAM = FStreamCodec.composite(
            FStreamCodec.INT, Sworn::progress, FStreamCodec.LONG, Sworn::since, Sworn::new);
    }

    public record State(Map<Identifier, Sworn> sworn, List<Identifier> finished, Map<Identifier, Integer> standing) {
        public static final State EMPTY = new State(Map.of(), List.of(), Map.of());

        public static final Codec<State> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Identifier.CODEC, Sworn.CODEC).optionalFieldOf("sworn", Map.of()).forGetter(State::sworn),
            Identifier.CODEC.listOf().optionalFieldOf("finished", List.of()).forGetter(State::finished),
            Codec.unboundedMap(Identifier.CODEC, Codec.INT).optionalFieldOf("standing", Map.of()).forGetter(State::standing)
        ).apply(i, State::new));

        private static final FStreamCodec<Identifier> ID = FStreamCodec.STRING.map(Identifier::parse, Identifier::toString);

        public static final FStreamCodec<State> STREAM = FStreamCodec.composite(
            FStreamCodec.mapOf(ID, Sworn.STREAM), State::sworn,
            ID.list(), State::finished,
            FStreamCodec.mapOf(ID, FStreamCodec.INT), State::standing,
            State::new);

        public boolean isSworn(Identifier pact) {
            return sworn.containsKey(pact);
        }

        public boolean isFinished(Identifier pact) {
            return finished.contains(pact);
        }

        public State withSworn(Identifier pact, Sworn s) {
            Map<Identifier, Sworn> m = new LinkedHashMap<>(sworn);
            if (s == null) m.remove(pact);
            else m.put(pact, s);
            return new State(Map.copyOf(m), finished, standing);
        }

        public State withFinished(Identifier pact) {
            if (finished.contains(pact)) return this;
            List<Identifier> f = new ArrayList<>(finished);
            f.add(pact);
            return new State(sworn, List.copyOf(f), standing);
        }

        public State withStanding(Identifier pact, int amount) {
            Map<Identifier, Integer> m = new LinkedHashMap<>(standing);
            if (amount == 0) m.remove(pact);
            else m.put(pact, amount);
            return new State(sworn, finished, Map.copyOf(m));
        }
    }

    public static final FAttach<State> LEDGER = FAttach.builder(FId.of(Votum.MODID, "ledger"), () -> State.EMPTY)
        .codec(State.CODEC)
        .copyOnDeath()
        .sync(FAttach.Sync.OWNER, State.STREAM)
        .build();

    private PactLedger() {}

    public static void init() {
        // Loading the class registers the attachment.
    }

    public static State of(Player player) {
        State s = LEDGER.get(player);
        return s == null ? State.EMPTY : s;
    }

    public static void set(Player player, State state) {
        LEDGER.set(player, state);
    }
}
