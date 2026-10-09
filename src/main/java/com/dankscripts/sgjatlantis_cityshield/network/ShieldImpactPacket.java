package com.dankscripts.sgjatlantis_cityshield.network;

import com.dankscripts.sgjatlantis_cityshield.client.ClientShieldState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import com.dankscripts.sgjatlantis_cityshield.AtlantisCityShield;

public record ShieldImpactPacket(ResourceLocation dimension, long controllerPos,
                                 double x, double y, double z, float strength, long gameTime) implements CustomPacketPayload {
    public static final Type<ShieldImpactPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AtlantisCityShield.MOD_ID, "shield_impact"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShieldImpactPacket> STREAM_CODEC = StreamCodec.of((buffer, packet) -> encode(packet, buffer), ShieldImpactPacket::decode);

    @Override
    public Type<ShieldImpactPacket> type() { return TYPE; }
    public static void encode(ShieldImpactPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeLong(packet.controllerPos);
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeFloat(packet.strength);
        buffer.writeLong(packet.gameTime);
    }

    public static ShieldImpactPacket decode(FriendlyByteBuf buffer) {
        return new ShieldImpactPacket(buffer.readResourceLocation(), buffer.readLong(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readFloat(), buffer.readLong());
    }

    public static void handle(ShieldImpactPacket packet, IPayloadContext context) {
        context.enqueueWork(() ->
                ClientShieldState.addImpact(packet.dimension + "@" + packet.controllerPos,
                        packet.x, packet.y, packet.z, packet.strength, packet.gameTime));
    }
}
