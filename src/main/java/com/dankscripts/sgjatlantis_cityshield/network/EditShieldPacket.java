package com.dankscripts.sgjatlantis_cityshield.network;

import com.dankscripts.sgjatlantis_cityshield.block.ShieldControllerBlockEntity;
import com.dankscripts.sgjatlantis_cityshield.shield.ShieldDefinition;
import com.dankscripts.sgjatlantis_cityshield.shield.ShieldSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import com.dankscripts.sgjatlantis_cityshield.AtlantisCityShield;

public record EditShieldPacket(BlockPos controllerPos, Action action, int field, int amount) implements CustomPacketPayload {
    public static final Type<EditShieldPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AtlantisCityShield.MOD_ID, "edit_shield"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EditShieldPacket> STREAM_CODEC = StreamCodec.of((buffer, packet) -> encode(packet, buffer), EditShieldPacket::decode);

    @Override
    public Type<EditShieldPacket> type() { return TYPE; }
    public enum Action { ADJUST, TOGGLE_PREVIEW, COMMIT, CANCEL, TOGGLE_ACTIVE }

    public static void encode(EditShieldPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.controllerPos);
        buffer.writeEnum(packet.action);
        buffer.writeByte(packet.field);
        buffer.writeInt(packet.amount);
    }

    public static EditShieldPacket decode(FriendlyByteBuf buffer) {
        return new EditShieldPacket(buffer.readBlockPos(), buffer.readEnum(Action.class), buffer.readByte(), buffer.readInt());
    }

    public static void handle(EditShieldPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.hasPermissions(2)) return;
            ShieldSavedData data = ShieldSavedData.get(player.server);
            ShieldDefinition definition = data.find(player.level(), packet.controllerPos);
            if (definition == null) return;
            switch (packet.action) {
                case ADJUST -> {
                    if (definition.active()) return;
                    if (packet.field < 0 || packet.field > 7) return;
                    int safeAmount = Math.max(-250, Math.min(250, packet.amount));
                    definition.adjust(packet.field, safeAmount);
                }
                case TOGGLE_PREVIEW -> {
                    if (!definition.active()) definition.togglePreview();
                }
                case COMMIT -> {
                    if (!definition.active()) definition.commit();
                }
                case CANCEL -> {
                    if (!definition.active()) definition.cancel();
                }
                case TOGGLE_ACTIVE -> {
                    if (!(player.level().getBlockEntity(packet.controllerPos) instanceof ShieldControllerBlockEntity controller)) {
                        return;
                    }
                    if (definition.active()) {
                        controller.setShieldActive(false, player.level().getGameTime());
                    } else if (controller.linkedEmitter(true) == null) {
                        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                                "message.sgjatlantis_cityshield.emitter_required"), true);
                    } else if (!controller.hasLargeNaquadahCable()) {
                        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                                "message.sgjatlantis_cityshield.large_cable_required"), true);
                    } else if (controller.hasZeroPointPower()) {
                        controller.setShieldActive(true, player.level().getGameTime());
                    } else {
                        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                                "message.sgjatlantis_cityshield.zero_point_power_required"), true);
                    }
                }
            }
            if (packet.action != Action.TOGGLE_ACTIVE) data.changed();
        });
    }
}
