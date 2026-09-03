package com.mustangdoc.sgjpatch.standalone;

import com.mustangdoc.sgjpatch.init.SGJPatchBlocks;
import com.mustangdoc.sgjpatch.init.SGJPatchMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.povstalec.sgjourney.common.menu.AbstractDHDMenu;

public class AtlantisDHDMenu extends AbstractDHDMenu<AtlantisDHDEntity> {
    public static final int BUTTON_TOGGLE_SHIELD = 1000;
    private final AtlantisDHDEntity atlantisDHD;
    private final Level atlantisLevel;

    public AtlantisDHDMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                inventory,
                (AtlantisDHDEntity) inventory.player.level().getBlockEntity(extraData.readBlockPos())
        );
    }

    public AtlantisDHDMenu(int containerId, Inventory inventory, AtlantisDHDEntity dhd) {
        super(SGJPatchMenus.ATLANTIS_DHD.get(), containerId, inventory, dhd);
        this.atlantisDHD = dhd;
        this.atlantisLevel = inventory.player.level();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(
                ContainerLevelAccess.create(atlantisLevel, atlantisDHD.getBlockPos()),
                player,
                SGJPatchBlocks.ATLANTIS_DHD.get()
        );
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_TOGGLE_SHIELD) {
            if (!player.level().isClientSide())
                atlantisDHD.toggleConnectedShield();
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    public AtlantisDHDEntity getDHD() {
        return atlantisDHD;
    }
}
