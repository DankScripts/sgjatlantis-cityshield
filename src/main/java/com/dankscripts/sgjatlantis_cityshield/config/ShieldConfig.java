package com.dankscripts.sgjatlantis_cityshield.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ShieldConfig {
    public static final long ALPHA16_ZPM_DRAW_PER_TICK = 1_000_000_000L;
    public static final long ALPHA17_ZPM_DRAW_PER_TICK = 500_000_000L;
    public static final long ALPHA18_ZPM_DRAW_PER_TICK = 100_000_000L;
    public static final long DEFAULT_ZPM_DRAW_PER_TICK = 20_000_000L;
    public static final int ALPHA18_GENERATOR_FE_PER_TICK = 100_000;
    public static final int DEFAULT_GENERATOR_FE_PER_TICK = 1_000;
    public static final int ALPHA18_GENERATOR_FE_PER_SIDE = 25_000;
    public static final int DEFAULT_GENERATOR_FE_PER_SIDE = 250;

    public enum EntityContactMode {
        MAJOR_DAMAGE,
        INSTANT_DEATH
    }

    public enum RedstoneMode { DISABLED, HIGH_ENABLE, HIGH_DISABLE }

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.EnumValue<EntityContactMode> ENTITY_CONTACT_MODE;
    public static final ForgeConfigSpec.DoubleValue MAJOR_CONTACT_DAMAGE;
    public static final ForgeConfigSpec.BooleanValue CREATIVE_PLAYERS_BYPASS;
    public static final ForgeConfigSpec.LongValue ZPM_DRAW_PER_TICK;
    public static final ForgeConfigSpec.BooleanValue SCALE_DRAW_BY_VOLUME;
    public static final ForgeConfigSpec.EnumValue<RedstoneMode> REDSTONE_MODE;
    public static final ForgeConfigSpec.BooleanValue ALLOW_GENERATOR_POWER;
    public static final ForgeConfigSpec.IntValue GENERATOR_FE_PER_TICK;
    public static final ForgeConfigSpec.IntValue GENERATOR_FE_PER_SIDE;
    private static final ForgeConfigSpec.BooleanValue HALF_DRAW_MIGRATION_APPLIED;
    private static final ForgeConfigSpec.BooleanValue HUNDRED_MFE_MIGRATION_APPLIED;
    private static final ForgeConfigSpec.BooleanValue CITY_SCALE_POWER_MIGRATION_APPLIED;
    private static final ForgeConfigSpec.BooleanValue IMPACT_POWER_MIGRATION_APPLIED;

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
                        "The city-scale default is 1 MFE/t. Server owners may set any positive value here.",
                        "The cable may touch the controller or have up to two ordinary blocks directly between them.",
                        "A connected SGJourney ZPM Hub supplies this path; ordinary FE uses the separate generator setting.")
                .defineInRange("zpmDrawPerTick", DEFAULT_ZPM_DRAW_PER_TICK, 1L, Long.MAX_VALUE);

        SCALE_DRAW_BY_VOLUME = builder
                .comment("Scale ZPM and generator draw with the shield's surveyed ellipsoid volume.",
                        "The original 500 x 500 x 340 shield uses the configured base draw.",
                        "A smaller shield can drop as low as 1 percent of the base draw.")
                .define("scaleDrawByVolume", true);

        ALLOW_GENERATOR_POWER = builder
                .comment("Allow ordinary FE generators or cables attached directly to the emitter as an alternative to a ZPM hub.")
                .define("allowGeneratorPower", true);
        GENERATOR_FE_PER_TICK = builder
                .comment("FE/t required for a standard-size shield when using ordinary generators. Scales with volume.")
                .defineInRange("generatorFePerTick", DEFAULT_GENERATOR_FE_PER_TICK, 1, 1_000_000_000);
        GENERATOR_FE_PER_SIDE = builder
                .comment("Maximum FE/t accepted from each emitter face. At default settings, at least four powered faces are needed.")
                .defineInRange("generatorFePerSide", DEFAULT_GENERATOR_FE_PER_SIDE, 1, 1_000_000_000);

        HALF_DRAW_MIGRATION_APPLIED = builder
                .comment("Internal migration marker. Existing Alpha 16 default power configs are updated once.")
                .define("alpha17HalfDrawMigrationApplied", false);

        HUNDRED_MFE_MIGRATION_APPLIED = builder
                .comment("Internal migration marker. Existing Alpha 16/17 default power configs are updated once.")
                .define("alpha18HundredMfeMigrationApplied", false);

        CITY_SCALE_POWER_MIGRATION_APPLIED = builder
                .comment("Internal migration marker. Exact previous defaults are reduced for city-scale power networks.")
                .define("cityScalePowerMigrationApplied", false);

        IMPACT_POWER_MIGRATION_APPLIED = builder
                .comment("Internal marker for the shield standing draw update.")
                .define("impactPowerMigrationApplied", false);

        builder.pop();

        builder.comment("Optional redstone control for the shield controller.").push("redstone");
        REDSTONE_MODE = builder
                .comment("DISABLED keeps manual GUI control; HIGH_ENABLE follows a high signal; HIGH_DISABLE follows a low signal.",
                        "A redstone signal cannot activate the shield without an emitter and sufficient power.")
                .defineEnum("mode", RedstoneMode.DISABLED);
        builder.pop();
        SPEC = builder.build();
    }

    public static boolean migratePowerDraw() {
        boolean changed = false;
        boolean upgradeStandingDraw = !IMPACT_POWER_MIGRATION_APPLIED.get();
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
        if (!CITY_SCALE_POWER_MIGRATION_APPLIED.get()) {
            if (ZPM_DRAW_PER_TICK.get() == ALPHA18_ZPM_DRAW_PER_TICK) {
                ZPM_DRAW_PER_TICK.set(DEFAULT_ZPM_DRAW_PER_TICK);
            }
            if (GENERATOR_FE_PER_TICK.get() == ALPHA18_GENERATOR_FE_PER_TICK) {
                GENERATOR_FE_PER_TICK.set(DEFAULT_GENERATOR_FE_PER_TICK);
            }
            if (GENERATOR_FE_PER_SIDE.get() == ALPHA18_GENERATOR_FE_PER_SIDE) {
                GENERATOR_FE_PER_SIDE.set(DEFAULT_GENERATOR_FE_PER_SIDE);
            }
            CITY_SCALE_POWER_MIGRATION_APPLIED.set(true);
            changed = true;
        }
        if (upgradeStandingDraw) {
            if (ZPM_DRAW_PER_TICK.get() == 1_000_000L) ZPM_DRAW_PER_TICK.set(DEFAULT_ZPM_DRAW_PER_TICK);
            IMPACT_POWER_MIGRATION_APPLIED.set(true);
            changed = true;
        }
        return changed;
    }

    private ShieldConfig() {}
}
