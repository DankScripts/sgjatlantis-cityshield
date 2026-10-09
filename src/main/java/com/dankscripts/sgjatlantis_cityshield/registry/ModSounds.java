package com.dankscripts.sgjatlantis_cityshield.registry;

import com.dankscripts.sgjatlantis_cityshield.AtlantisCityShield;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.function.Supplier;

public final class ModSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, AtlantisCityShield.MOD_ID);

    public static final Supplier<SoundEvent> SHIELD_RAISE = SOUNDS.register("shield_raise",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(AtlantisCityShield.MOD_ID, "shield_raise")));

    private ModSounds() {}

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }
}
