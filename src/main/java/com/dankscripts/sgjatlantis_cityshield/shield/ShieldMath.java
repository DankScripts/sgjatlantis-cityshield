package com.dankscripts.sgjatlantis_cityshield.shield;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public final class ShieldMath {
    public static final long MATERIALIZE_TICKS = 120L;
    private static final int INTERSECTION_SAMPLES = 32;
    private static final int INTERSECTION_REFINEMENTS = 18;

    private ShieldMath() {}

    public static double materializationCoverage(ShieldDefinition definition, long gameTime) {
        if (!definition.active()) return 0.0;
        long elapsed = Math.max(0L, gameTime - definition.stateChangeGameTime());
        return Math.min(1.0, elapsed / (double) MATERIALIZE_TICKS);
    }

    public static boolean isOperational(ShieldDefinition definition, long gameTime) {
        return materializationCoverage(definition, gameTime) >= 1.0;
    }

    public static boolean isInDimension(ShieldDefinition definition, ResourceLocation dimension) {
        return definition.dimension().equals(dimension);
    }

    public static Vec3 center(ShieldDefinition definition) {
        return new Vec3(definition.centerX() + 0.5, definition.centerY() + 0.5,
                definition.centerZ() + 0.5);
    }

    public static boolean contains(ShieldDefinition definition, BlockPos position) {
        return contains(definition, Vec3.atCenterOf(position));
    }

    public static boolean contains(ShieldDefinition definition, Vec3 position) {
        Vec3 center = center(definition);
        double x = (position.x - center.x) / definition.radiusX();
        double localY = position.y - center.y;
        double verticalRadius = Math.max(1.0,
                localY >= 0.0 ? definition.upperHeight() : definition.lowerDepth());
        double y = localY / verticalRadius;
        double z = (position.z - center.z) / definition.radiusZ();
        return x * x + y * y + z * z <= 1.0;
    }

    /**
     * Returns the point on the upper dome directly above the supplied horizontal position.
     * A null result means that the position lies outside the shield's horizontal footprint.
     */
    public static Vec3 upperSurfaceAt(ShieldDefinition definition, double worldX, double worldZ) {
        Vec3 center = center(definition);
        double x = (worldX - center.x) / definition.radiusX();
        double z = (worldZ - center.z) / definition.radiusZ();
        double horizontal = x * x + z * z;
        if (horizontal > 1.0) return null;

        double y = center.y + definition.upperHeight()
                * Math.sqrt(Math.max(0.0, 1.0 - horizontal));
        return new Vec3(worldX, y, worldZ);
    }

    /**
     * Returns the fraction of the bottom-to-top formation sweep required to reach
     * a point on the shield surface.
     */
    public static double surfaceCoverage(ShieldDefinition definition, Vec3 surfacePoint) {
        Vec3 center = center(definition);
        double localY = surfacePoint.y - center.y;
        double verticalRadius = Math.max(1.0,
                localY >= 0.0 ? definition.upperHeight() : definition.lowerDepth());
        double normalizedY = Math.max(-1.0, Math.min(1.0, localY / verticalRadius));
        double latitude = Math.asin(normalizedY);
        return (latitude + Math.PI / 2.0) / Math.PI;
    }

    public static boolean isSurfaceFormed(ShieldDefinition definition, long gameTime, Vec3 surfacePoint) {
        return definition.active()
                && materializationCoverage(definition, gameTime) + 1.0E-7
                >= surfaceCoverage(definition, surfacePoint);
    }

    /**
     * Finds the first shield-boundary crossing along a segment in either direction.
     * Sampling also catches very fast objects that enter and leave the dome in one tick.
     */
    public static Vec3 boundaryIntersection(ShieldDefinition definition, Vec3 from, Vec3 to) {
        return boundaryIntersection(definition, from, to, 0L, false);
    }

    /**
     * Finds the first crossing whose exact surface segment has already been swept
     * by the moving formation front. This makes the visible field solid from its
     * first tick instead of waiting for the complete six-second animation.
     */
    public static Vec3 formedBoundaryIntersection(ShieldDefinition definition, Vec3 from, Vec3 to,
                                                  long gameTime) {
        if (!definition.active()) return null;
        return boundaryIntersection(definition, from, to, gameTime, true);
    }

    private static Vec3 boundaryIntersection(ShieldDefinition definition, Vec3 from, Vec3 to,
                                             long gameTime, boolean requireFormedSurface) {
        boolean previousInside = contains(definition, from);
        double previousT = 0.0;
        for (int sample = 1; sample <= INTERSECTION_SAMPLES; sample++) {
            double currentT = sample / (double) INTERSECTION_SAMPLES;
            boolean currentInside = contains(definition, from.lerp(to, currentT));
            if (currentInside == previousInside) {
                previousT = currentT;
                continue;
            }

            double low = previousT;
            double high = currentT;
            boolean lowInside = previousInside;
            for (int refinement = 0; refinement < INTERSECTION_REFINEMENTS; refinement++) {
                double middle = (low + high) * 0.5;
                if (contains(definition, from.lerp(to, middle)) == lowInside) low = middle;
                else high = middle;
            }
            Vec3 intersection = from.lerp(to, high);
            if (!requireFormedSurface || isSurfaceFormed(definition, gameTime, intersection)) {
                return intersection;
            }
            previousInside = currentInside;
            previousT = currentT;
        }
        return null;
    }

    public static Vec3 incomingIntersection(ShieldDefinition definition, Vec3 from, Vec3 to) {
        if (contains(definition, from)) return null;
        return boundaryIntersection(definition, from, to);
    }
}
