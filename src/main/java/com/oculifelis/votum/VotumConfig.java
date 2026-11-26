package com.oculifelis.votum;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class VotumConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MAX_SWORN = BUILDER
        .comment("How many pacts a player can have sworn and unfinished at once.")
        .defineInRange("maxSworn", 3, 1, 64);

    public static final ModConfigSpec.IntValue BRAZIER_SECONDS = BUILDER
        .comment("Seconds a Seal Brazier takes to melt a batch of wax.")
        .defineInRange("brazierSeconds", 10, 0, 600);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private VotumConfig() {}

    public static int maxSworn() {
        return SPEC.isLoaded() ? MAX_SWORN.get() : MAX_SWORN.getDefault();
    }

    public static int brazierTicks() {
        return 20 * (SPEC.isLoaded() ? BRAZIER_SECONDS.get() : BRAZIER_SECONDS.getDefault());
    }
}
