package com.dankscripts.sgjatlantis_cityshield.client;

import com.dankscripts.sgjatlantis_cityshield.shield.ShieldDefinition;
import com.dankscripts.sgjatlantis_cityshield.shield.ShieldMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class ShieldWeatherClientEvents {
    private static final double WEATHER_FADE_TICKS = 12.0;

    private ShieldWeatherClientEvents() {}

    /**
     * Returns the local precipitation opacity and rain-channel volume remaining
     * beneath the shield. Both reach zero on the exact tick the upper surface
     * becomes solid at the camera's horizontal position.
     */
    public static float precipitationMultiplier() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Entity camera = minecraft.getCameraEntity();
        if (level == null || camera == null) return 1.0F;

        Vec3 cameraPosition = camera.getBoundingBox().getCenter();
        long gameTime = level.getGameTime();
        double multiplier = 1.0;

        for (ShieldDefinition definition : ClientShieldState.definitions()) {
            if (!ShieldMath.isInDimension(definition, level.dimension().location())
                    || !definition.active()
                    || !ShieldMath.contains(definition, cameraPosition)) continue;

            Vec3 upperSurface = ShieldMath.upperSurfaceAt(
                    definition, cameraPosition.x, cameraPosition.z);
            if (upperSurface == null) continue;

            double requiredCoverage = ShieldMath.surfaceCoverage(definition, upperSurface);
            double coverage = ShieldMath.materializationCoverage(definition, gameTime);
            double fadeCoverage = WEATHER_FADE_TICKS / ShieldMath.MATERIALIZE_TICKS;
            double remaining = (requiredCoverage - coverage) / fadeCoverage;
            remaining = Math.max(0.0, Math.min(1.0, remaining));
            remaining = remaining * remaining * (3.0 - 2.0 * remaining);
            multiplier = Math.min(multiplier, remaining);
        }
        return (float) multiplier;
    }

    public static boolean isCameraProtected() {
        return precipitationMultiplier() <= 0.001F;
    }
}
