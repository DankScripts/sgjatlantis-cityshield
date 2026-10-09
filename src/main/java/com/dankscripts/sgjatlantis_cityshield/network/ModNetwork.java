package com.dankscripts.sgjatlantis_cityshield.network;

import com.dankscripts.sgjatlantis_cityshield.AtlantisCityShield;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL = "6";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AtlantisCityShield.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private ModNetwork() {}

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, EditShieldPacket.class, EditShieldPacket::encode,
                EditShieldPacket::decode, EditShieldPacket::handle);
        CHANNEL.registerMessage(id++, ShieldSyncPacket.class, ShieldSyncPacket::encode,
                ShieldSyncPacket::decode, ShieldSyncPacket::handle);
        CHANNEL.registerMessage(id++, ShieldImpactPacket.class, ShieldImpactPacket::encode,
                ShieldImpactPacket::decode, ShieldImpactPacket::handle);
    }
}
