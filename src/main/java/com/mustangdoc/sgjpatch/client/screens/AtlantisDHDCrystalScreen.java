package com.mustangdoc.sgjpatch.client.screens;

import com.mustangdoc.sgjpatch.menu.AtlantisDHDCrystalMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.povstalec.sgjourney.client.screens.dhd.DHDCrystalScreen;

public class AtlantisDHDCrystalScreen extends DHDCrystalScreen<AtlantisDHDCrystalMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "sgjourney", "textures/gui/dhd/pegasus/pegasus_dhd_crystal_gui.png"
    );

    public AtlantisDHDCrystalScreen(AtlantisDHDCrystalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TEXTURE);
    }
}
