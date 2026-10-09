package com.dankscripts.sgjatlantis_cityshield.network;

import com.dankscripts.sgjatlantis_cityshield.block.ShieldControllerBlockEntity;
import com.dankscripts.sgjatlantis_cityshield.shield.ShieldDefinition;
import com.dankscripts.sgjatlantis_cityshield.shield.ShieldSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record EditShieldPacket(BlockPos controllerPos, Action action, int field, int amount) {
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

    public static void handle(EditShieldPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.hasPermissions(2)) return;
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
        context.setPacketHandled(true);
    }
}
