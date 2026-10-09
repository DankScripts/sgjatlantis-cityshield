package com.dankscripts.sgjatlantis_cityshield.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ShieldConfig {
    public static final long ALPHA16_ZPM_DRAW_PER_TICK = 1_000_000_000L;
    public static final long ALPHA17_ZPM_DRAW_PER_TICK = 500_000_000L;
    public static final long DEFAULT_ZPM_DRAW_PER_TICK = 100_000_000L;

    public enum EntityContactMode {
        MAJOR_DAMAGE,
        INSTANT_DEATH
    }

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.EnumValue<EntityContactMode> ENTITY_CONTACT_MODE;
    public static final ForgeConfigSpec.DoubleValue MAJOR_CONTACT_DAMAGE;
    public static final ForgeConfigSpec.BooleanValue CREATIVE_PLAYERS_BYPASS;
    public static final ForgeConfigSpec.LongValue ZPM_DRAW_PER_TICK;
    private static final ForgeConfigSpec.BooleanValue HALF_DRAW_MIGRATION_APPLIED;
    private static final ForgeConfigSpec.BooleanValue HUNDRED_MFE_MIGRATION_APPLIED;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Atlantis city shield matter-barrier behavior.")
                .push("matterBarrier");

        ENTITY_CONTACT_MODE = builder
                .comment("What happens when an unauthorized entity touches a formed shield segment.",
                        "MAJOR_DAMAGE applies majorContactDamage and repels survivors.",
                        "INSTANT_DEATH immediately kills or removes the entity.")
                .defineEnum("entityContactMode", EntityContactMode.MAJOR_DAMAGE);

        MAJOR_CONTACT_DAMAGE = builder
                .comment("Damage applied per collision in MAJOR_DAMAGE mode.",
                        "20 damage equals 10 hearts. The default 40 damage equals 20 hearts.")
                .defineInRange("majorContactDamage", 40.0, 1.0, 1000000.0);

        CREATIVE_PLAYERS_BYPASS = builder
                .comment("When true, creative-mode players may cross the shield without damage.",
                        "When false, creative players are treated like every other unauthorized entity.")
                .define("creativePlayersBypassShield", false);

        builder.pop();

        builder.comment("Atlantis city shield power requirements.")
                .push("power");

        ZPM_DRAW_PER_TICK = builder
                .comment("Continuous zero-point energy draw from the connected Large Naquadah Cable while the city shield is active, in FE per tick.",
                        "The default is 100 MFE/t. Server owners may set any positive value here.",
                        "The cable may touch the controller or have up to two ordinary blocks directly between them.",
                        "Ordinary FE is rejected; power must arrive through a connected SGJourney ZPM Hub.")
                .defineInRange("zpmDrawPerTick", DEFAULT_ZPM_DRAW_PER_TICK, 1L, Long.MAX_VALUE);

        HALF_DRAW_MIGRATION_APPLIED = builder
                .comment("Internal migration marker. Existing Alpha 16 default power configs are updated once.")
                .define("alpha17HalfDrawMigrationApplied", false);

        HUNDRED_MFE_MIGRATION_APPLIED = builder
                .comment("Internal migration marker. Existing Alpha 16/17 default power configs are updated once.")
                .define("alpha18HundredMfeMigrationApplied", false);

        builder.pop();
        SPEC = builder.build();
    }

    public static boolean migratePowerDraw() {
        boolean changed = false;
        if (!HALF_DRAW_MIGRATION_APPLIED.get()) {
            if (ZPM_DRAW_PER_TICK.get() == ALPHA16_ZPM_DRAW_PER_TICK) {
                ZPM_DRAW_PER_TICK.set(ALPHA17_ZPM_DRAW_PER_TICK);
            }
            HALF_DRAW_MIGRATION_APPLIED.set(true);
            changed = true;
        }
        if (!HUNDRED_MFE_MIGRATION_APPLIED.get()) {
            long draw = ZPM_DRAW_PER_TICK.get();
            if (draw == ALPHA16_ZPM_DRAW_PER_TICK || draw == ALPHA17_ZPM_DRAW_PER_TICK) {
                ZPM_DRAW_PER_TICK.set(DEFAULT_ZPM_DRAW_PER_TICK);
            }
            HUNDRED_MFE_MIGRATION_APPLIED.set(true);
            changed = true;
        }
        return changed;
    }

    private ShieldConfig() {}
}
