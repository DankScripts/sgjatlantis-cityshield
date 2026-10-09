package com.dankscripts.sgjatlantis_cityshield.mixin;

import com.dankscripts.sgjatlantis_cityshield.client.ShieldWeatherClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(SoundEngine.class)
public abstract class SoundEngineRainMixin {
    @Inject(method = {"tickNonPaused", "m_120326_"}, at = @At("TAIL"), remap = false)
    private void sgjatlantisCityShield$fadeEveryActiveRainChannel(CallbackInfo callback) {
        float shieldMultiplier = ShieldWeatherClientEvents.precipitationMultiplier();
        SoundEngine soundEngine = (SoundEngine) (Object) this;

        for (Map.Entry<SoundInstance, ChannelAccess.ChannelHandle> entry
                : soundEngine.instanceToChannel.entrySet()) {
            SoundInstance sound = entry.getKey();
            if (!sound.getLocation().equals(SoundEvents.WEATHER_RAIN.getLocation())
                    && !sound.getLocation().equals(SoundEvents.WEATHER_RAIN_ABOVE.getLocation())) continue;

            float vanillaVolume = Mth.clamp(sound.getVolume()
                    * Minecraft.getInstance().options.getSoundSourceVolume(sound.getSource()), 0.0F, 1.0F);
            float adjustedVolume = vanillaVolume * shieldMultiplier;
            entry.getValue().execute(channel -> channel.setVolume(adjustedVolume));
        }
    }
}
