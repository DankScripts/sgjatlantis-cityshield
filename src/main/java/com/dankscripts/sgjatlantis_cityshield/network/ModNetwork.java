package com.dankscripts.sgjatlantis_cityshield.network;

import com.dankscripts.sgjatlantis_cityshield.AtlantisCityShield;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ModNetwork {
    private static final String PROTOCOL = "6";

    private ModNetwork() {}

        public static void register(RegisterPayloadHandlersEvent event) {
                var registrar = event.registrar(PROTOCOL);
                registrar.playToServer(EditShieldPacket.TYPE, EditShieldPacket.STREAM_CODEC, EditShieldPacket::handle);
                registrar.playToClient(ShieldSyncPacket.TYPE, ShieldSyncPacket.STREAM_CODEC, ShieldSyncPacket::handle);
                registrar.playToClient(ShieldImpactPacket.TYPE, ShieldImpactPacket.STREAM_CODEC, ShieldImpactPacket::handle);
    }
}
