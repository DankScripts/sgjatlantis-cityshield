package com.dankscripts.sgjatlantis_cityshield.network;

import com.dankscripts.sgjatlantis_cityshield.client.ClientShieldState;
import com.dankscripts.sgjatlantis_cityshield.shield.ShieldDefinition;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import com.dankscripts.sgjatlantis_cityshield.AtlantisCityShield;

import java.util.ArrayList;
import java.util.List;

public record ShieldSyncPacket(List<ShieldDefinition> definitions) implements CustomPacketPayload {
    public static final Type<ShieldSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AtlantisCityShield.MOD_ID, "shield_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShieldSyncPacket> STREAM_CODEC = StreamCodec.of((buffer, packet) -> encode(packet, buffer), ShieldSyncPacket::decode);

    @Override
    public Type<ShieldSyncPacket> type() { return TYPE; }
    public static void encode(ShieldSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.definitions.size());
        packet.definitions.forEach(definition -> definition.encode(buffer));
    }

    public static ShieldSyncPacket decode(FriendlyByteBuf buffer) {
        int size = Math.min(buffer.readVarInt(), 1024);
        List<ShieldDefinition> definitions = new ArrayList<>(size);
        for (int i = 0; i < size; i++) definitions.add(ShieldDefinition.decode(buffer));
        return new ShieldSyncPacket(definitions);
    }

    public static void handle(ShieldSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientShieldState.replace(packet.definitions));
    }
}
