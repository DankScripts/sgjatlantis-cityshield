package com.dankscripts.sgjatlantis_cityshield.network;

import com.dankscripts.sgjatlantis_cityshield.client.ClientShieldState;
import com.dankscripts.sgjatlantis_cityshield.shield.ShieldDefinition;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record ShieldSyncPacket(List<ShieldDefinition> definitions) {
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

    public static void handle(ShieldSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientShieldState.replace(packet.definitions)));
        context.setPacketHandled(true);
    }
}
