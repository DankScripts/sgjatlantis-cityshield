package com.dankscripts.sgjatlantis_cityshield.client;

import com.dankscripts.sgjatlantis_cityshield.shield.ShieldDefinition;

import java.util.ArrayList;
import java.util.List;

public final class ClientShieldState {
    private static final long IMPACT_LIFETIME_TICKS = 36L;
    private static final int MAX_IMPACTS = 32;
    private static volatile List<ShieldDefinition> definitions = List.of();
    private static final List<ShieldImpact> impacts = new ArrayList<>();

    private ClientShieldState() {}

    public static void replace(List<ShieldDefinition> newDefinitions) {
        definitions = List.copyOf(newDefinitions);
        ShieldActivationSoundClient.onShieldSync(definitions);
    }

    public static List<ShieldDefinition> definitions() {
        return definitions;
    }

    public static void addImpact(String shieldKey, double x, double y, double z,
                                 float strength, long gameTime) {
        impacts.add(new ShieldImpact(shieldKey, x, y, z, Math.max(0.1F, Math.min(2.5F, strength)), gameTime));
        while (impacts.size() > MAX_IMPACTS) impacts.remove(0);
    }

    public static List<ShieldImpact> activeImpacts(String shieldKey, long gameTime) {
        impacts.removeIf(impact -> gameTime < impact.gameTime
                || gameTime - impact.gameTime > IMPACT_LIFETIME_TICKS);
        return impacts.stream().filter(impact -> impact.shieldKey.equals(shieldKey)).toList();
    }

    public static void clear() {
        definitions = List.of();
        impacts.clear();
        ShieldActivationSoundClient.clear();
    }

    public record ShieldImpact(String shieldKey, double x, double y, double z,
                               float strength, long gameTime) {}
}
