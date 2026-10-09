package com.dankscripts.sgjatlantis_cityshield.client;

import com.dankscripts.sgjatlantis_cityshield.registry.ModSounds;
import com.dankscripts.sgjatlantis_cityshield.shield.ShieldDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Plays the city-scale activation cue once for each newly raised shield. */
public final class ShieldActivationSoundClient {
    private static final Map<String, Long> OBSERVED_CHANGES = new HashMap<>();

    private ShieldActivationSoundClient() {}

    public static void onShieldSync(List<ShieldDefinition> definitions) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;

        Set<String> present = new HashSet<>();
        long clientTime = minecraft.level.getGameTime();
        for (ShieldDefinition definition : definitions) {
            present.add(definition.key());
            Long previousChange = OBSERVED_CHANGES.put(definition.key(), definition.stateChangeGameTime());
            boolean stateJustChanged = previousChange != null
                    && previousChange.longValue() != definition.stateChangeGameTime();
            long age = clientTime - definition.stateChangeGameTime();
            boolean joinedDuringActivation = previousChange == null && age >= -20L && age <= 20L;

            if (definition.active() && (stateJustChanged || joinedDuringActivation)
                    && audibleFrom(minecraft, definition)) {
                minecraft.getSoundManager().play(
                        SimpleSoundInstance.forUI(ModSounds.SHIELD_RAISE.get(), 1.0F, 1.0F));
            }
        }
        OBSERVED_CHANGES.keySet().retainAll(present);
    }

    public static void clear() {
        OBSERVED_CHANGES.clear();
    }

    private static boolean audibleFrom(Minecraft minecraft, ShieldDefinition definition) {
        if (!minecraft.level.dimension().location().equals(definition.dimension())) return false;
        double dx = minecraft.player.getX() - definition.centerX();
        double dz = minecraft.player.getZ() - definition.centerZ();
        double horizontal = dx * dx / Math.pow(definition.radiusX() + definition.margin() + 192.0, 2.0)
                + dz * dz / Math.pow(definition.radiusZ() + definition.margin() + 192.0, 2.0);
        double minY = definition.centerY() - definition.lowerDepth() - definition.margin() - 96.0;
        double maxY = definition.centerY() + definition.upperHeight() + definition.margin() + 192.0;
        return horizontal <= 1.0 && minecraft.player.getY() >= minY && minecraft.player.getY() <= maxY;
    }
}
