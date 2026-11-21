package com.dankscripts.sgjatlantis_cityshield.shield;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class ShieldControlAccess {
    private ShieldControlAccess() {}

    public static boolean canEdit(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return false;
        if (serverPlayer.hasPermissions(2)) return true;
        MinecraftServer server = serverPlayer.server;
        if (server.isDedicatedServer()) return false;
        GameProfile host = server.getSingleplayerProfile();
        return host != null && serverPlayer.getUUID().equals(host.getId());
    }
}
