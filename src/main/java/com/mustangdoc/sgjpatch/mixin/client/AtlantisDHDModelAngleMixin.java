package com.mustangdoc.sgjpatch.mixin.client;

import com.google.gson.JsonObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Preserve the exact non-vanilla element rotations used by the accepted Atlantis DHD model. */
@Mixin(targets = "net.minecraft.client.renderer.block.model.BlockElement$Deserializer")
public class AtlantisDHDModelAngleMixin {
    @Inject(method = "getAngle", at = @At("HEAD"), cancellable = true, remap = false)
    private void sgjpatch$allowAtlantisTriangleAngles(
            JsonObject json,
            CallbackInfoReturnable<Float> cir) {
        if (json == null || json.get("angle") == null)
            return;

        float angle = json.get("angle").getAsFloat();
        if (sgjpatch$isAtlantisAngle(angle))
            cir.setReturnValue(angle);
    }

    private static boolean sgjpatch$isAtlantisAngle(float angle) {
        return near(angle, -57.51492F) || near(angle, 57.51492F)
                || near(angle, -60.98254F) || near(angle, 60.98254F)
                || near(angle, -59.91350F) || near(angle, 59.91350F)
                || near(angle, -61.89373F) || near(angle, 61.89373F);
    }

    private static boolean near(float a, float b) {
        return Math.abs(a - b) < 0.0002F;
    }
}
