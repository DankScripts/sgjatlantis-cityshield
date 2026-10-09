package com.dankscripts.sgjatlantis_cityshield.network;

import com.dankscripts.sgjatlantis_cityshield.client.ClientShieldState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ShieldImpactPacket(ResourceLocation dimension, long controllerPos,
                                 double x, double y, double z, float strength, long gameTime) {
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

    public static void handle(ShieldImpactPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientShieldState.addImpact(packet.dimension + "@" + packet.controllerPos,
                        packet.x, packet.y, packet.z, packet.strength, packet.gameTime)));
        context.setPacketHandled(true);
    }
}
