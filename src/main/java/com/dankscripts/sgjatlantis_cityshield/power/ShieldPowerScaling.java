package com.dankscripts.sgjatlantis_cityshield.power;

import com.dankscripts.sgjatlantis_cityshield.config.ShieldConfig;
import com.dankscripts.sgjatlantis_cityshield.shield.ShieldDefinition;

/** Power follows the volume of the surveyed ellipsoid, not its diameter. */
public final class ShieldPowerScaling {
    private static final double REFERENCE_VOLUME_FACTOR = 500.0 * 500.0 * 340.0;

    private ShieldPowerScaling() { }

    public static long zpmDraw(ShieldDefinition definition) {
        return scale(ShieldConfig.ZPM_DRAW_PER_TICK.get(), definition);
    }

    public static int generatorDraw(ShieldDefinition definition) {
        return (int) Math.min(Integer.MAX_VALUE, scale(ShieldConfig.GENERATOR_FE_PER_TICK.get(), definition));
    }

    private static long scale(long base, ShieldDefinition definition) {
        if (definition == null || !ShieldConfig.SCALE_DRAW_BY_VOLUME.get()) return Math.max(1L, base);
        double factor = (double) definition.radiusX() * definition.radiusZ()
                * (definition.upperHeight() + (double) definition.lowerDepth()) / REFERENCE_VOLUME_FACTOR;
        factor = Math.max(0.5, factor);
        double draw = base * factor;
        return draw >= Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(1L, Math.round(draw));
    }
}
