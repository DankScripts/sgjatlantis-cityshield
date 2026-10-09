package com.dankscripts.sgjatlantis_cityshield.network;

import com.dankscripts.sgjatlantis_cityshield.AtlantisCityShield;
import com.dankscripts.sgjatlantis_cityshield.shield.ShieldSavedData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = AtlantisCityShield.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class ServerSyncEvents {
    private ServerSyncEvents() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ShieldSavedData.get(player.server).sync(player);
        }
    }

    @SubscribeEvent
    public static void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ShieldSavedData.get(player.server).sync(player);
        }
    }
}
