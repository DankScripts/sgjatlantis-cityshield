package com.dankscripts.sgjatlantis_cityshield.mixin;

import com.dankscripts.sgjatlantis_cityshield.client.ShieldWeatherClientEvents;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererWeatherMixin {
    @ModifyVariable(method = {"renderSnowAndRain", "m_109703_"},
            at = @At(value = "STORE"), index = 9, remap = false)
    private float sgjatlantisCityShield$fadePrecipitationRendering(float original) {
        return original * ShieldWeatherClientEvents.precipitationMultiplier();
    }

    @ModifyVariable(method = {"tickRain", "m_109693_"},
            at = @At(value = "STORE"), index = 2, remap = false)
    private float sgjatlantisCityShield$fadePrecipitationEffects(float original) {
        return original * ShieldWeatherClientEvents.precipitationMultiplier();
    }
}
