package com.dankscripts.sgjatlantis_cityshield.mixin;

import com.google.gson.JsonObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Allows the production-prop angles used by the shield controller model. */
@Mixin(targets = "net.minecraft.client.renderer.block.model.BlockElement$Deserializer", remap = false)
public abstract class ShieldControllerModelAngleMixin {
    @Inject(method = "getAngle", at = @At("HEAD"), cancellable = true, remap = false)
    private void sgjatlantisCityShield$allowControllerAngles(JsonObject json,
                                                              CallbackInfoReturnable<Float> callback) {
        if (json == null || !json.has("angle")) return;
        float angle = json.get("angle").getAsFloat();
        if (near(angle, -8.0F) || near(angle, -35.0F)
            || near(angle, -18.0F) || near(angle, 18.0F)) callback.setReturnValue(angle);
    }

    private static boolean near(float value, float expected) {
        return Math.abs(value - expected) < 0.001F;
    }
}
