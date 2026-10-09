package com.dankscripts.sgjatlantis_cityshield.shield;

import com.dankscripts.sgjatlantis_cityshield.config.ShieldConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Central access hook for matter crossing the city shield.
 * The planned tuned personal-shield device will grant eligible players access here.
 */
public final class ShieldAccess {
    private ShieldAccess() {}

    public static boolean canCross(Entity entity, ShieldDefinition definition) {
        if (ShieldConfig.CREATIVE_PLAYERS_BYPASS.get()) {
            if (entity instanceof Player player && player.isCreative()) return true;
            if (entity.getPassengers().stream()
                    .anyMatch(passenger -> passenger instanceof Player player && player.isCreative())) return true;
        }

        // The planned frequency-tuned personal shield device will be checked here.
        return false;
    }
}
