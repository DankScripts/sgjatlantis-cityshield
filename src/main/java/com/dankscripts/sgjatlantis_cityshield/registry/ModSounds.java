package com.dankscripts.sgjatlantis_cityshield.registry;

import com.dankscripts.sgjatlantis_cityshield.AtlantisCityShield;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, AtlantisCityShield.MOD_ID);

    public static final RegistryObject<SoundEvent> SHIELD_RAISE = SOUNDS.register("shield_raise",
            () -> SoundEvent.createVariableRangeEvent(
                    new ResourceLocation(AtlantisCityShield.MOD_ID, "shield_raise")));

    private ModSounds() {}

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }
}
