package com.mustangdoc.sgjpatch.menu;

import com.mustangdoc.sgjpatch.init.SGJPatchBlocks;
import com.mustangdoc.sgjpatch.init.SGJPatchMenus;
import com.mustangdoc.sgjpatch.standalone.AtlantisDHDEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.povstalec.sgjourney.common.menu.DHDCrystalMenu;
import org.jetbrains.annotations.NotNull;

public class AtlantisDHDCrystalMenu extends DHDCrystalMenu<AtlantisDHDEntity> {
    private final AtlantisDHDEntity atlantisDHD;
    private final Level atlantisLevel;

    public AtlantisDHDCrystalMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                inventory,
                (AtlantisDHDEntity) inventory.player.level().getBlockEntity(extraData.readBlockPos())
        );
    }

    public AtlantisDHDCrystalMenu(int containerId, Inventory inventory, AtlantisDHDEntity blockEntity) {
        super(SGJPatchMenus.ATLANTIS_DHD_CRYSTAL.get(), containerId, inventory, blockEntity);
        this.atlantisDHD = blockEntity;
        this.atlantisLevel = inventory.player.level();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(
                ContainerLevelAccess.create(atlantisLevel, atlantisDHD.getBlockPos()),
                player,
                SGJPatchBlocks.ATLANTIS_DHD.get()
        );
    }
}
