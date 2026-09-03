package com.mustangdoc.sgjpatch.client.util;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Draws the locked decorative table controls without changing menu behavior. */
public final class AtlantisGuiOverlay {
    private static final ResourceLocation CRYSTAL = ResourceLocation.fromNamespaceAndPath(
            "sgjpatch", "textures/gui/atlantis_dhd_control_crystal.png");
    private static final ResourceLocation BAR = ResourceLocation.fromNamespaceAndPath(
            "sgjpatch", "textures/gui/atlantis_dhd_control_bar.png");
    private static final int[][] CONTROLS = {
            {70, 70, 44, 34}, {123, 70, 44, 34}, {176, 70, 44, 34},
            {70, 117, 44, 34}, {123, 117, 44, 34}, {176, 117, 44, 34},
            {114, 224, 52, 18}, {180, 224, 52, 18}
    };

    private AtlantisGuiOverlay() {}

        public static void renderControls(
                        GuiGraphics graphics, int left, int top, boolean powered) {
                if (!powered)
                        return;

        for (int index = 0; index < CONTROLS.length; index++) {
            int[] control = CONTROLS[index];
            ResourceLocation texture = index < 6 ? CRYSTAL : BAR;
            graphics.blit(texture, left + control[0], top + control[1],
                    control[2], control[3], 0F, 0F,
                    control[2], control[3], control[2], control[3]);
        }
    }
}
