package com.mustangdoc.sgjpatch.client.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;

public final class AtlantisTriangleEngageButton extends AbstractButton {
    private final boolean up;
    private final ResourceLocation texture;
    private final int sourceSize;
    private final Runnable onPress;

    public AtlantisTriangleEngageButton(
            int x, int y, int displaySize, int sourceSize, boolean up,
            ResourceLocation texture, Runnable onPress) {
        super(x, y, displaySize, displaySize, CommonComponents.EMPTY);
        this.up = up;
        this.texture = texture;
        this.sourceSize = sourceSize;
        this.onPress = onPress;
    }

    @Override
    public void onPress() {
        onPress.run();
    }

    @Override
    protected boolean clicked(double mouseX, double mouseY) {
        return this.active && this.visible && insideTriangle(mouseX, mouseY);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.active && this.visible && insideTriangle(mouseX, mouseY);
    }

    private boolean insideTriangle(double mouseX, double mouseY) {
        double nx = (mouseX - getX()) / (double) width;
        double ny = (mouseY - getY()) / (double) height;
        if (nx < 0.0 || nx > 1.0 || ny < 0.0 || ny > 1.0)
            return false;
        double edge = 2.0 * Math.abs(nx - 0.5);
        return up ? ny >= edge : ny <= 1.0 - edge;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.isHovered = isMouseOver(mouseX, mouseY);
        int frame = isHoveredOrFocused() ? 1 : 0;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        graphics.blit(
                texture,
                getX(), getY(), width, height,
                0F, (float) (frame * sourceSize),
                sourceSize, sourceSize,
                sourceSize, sourceSize * 3
        );
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
}
