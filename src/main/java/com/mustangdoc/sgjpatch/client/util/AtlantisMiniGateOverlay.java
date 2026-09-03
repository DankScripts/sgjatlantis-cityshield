package com.mustangdoc.sgjpatch.client.util;

import com.mustangdoc.sgjpatch.standalone.AtlantisDHDMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.resources.ResourceLocation;
import net.povstalec.sgjourney.common.block_entities.stargate.PegasusStargateEntity;
import net.povstalec.sgjourney.common.sgjourney.Address;

import java.util.Locale;

/**
 * Draws the locked mini Pegasus gate around the shield control.
 */
public final class AtlantisMiniGateOverlay {
    private static final int GATE_SIZE = 118;
    private static final int GATE_OFFSET = 32;
    private static final int ORB_SIZE = 74;
    private static final int ORB_OFFSET = 10;
    private static final ResourceLocation GATE_BASE = ResourceLocation.fromNamespaceAndPath(
            "sgjpatch", "textures/gui/mini_gate/early_port/gate_base.png");
    private static final ResourceLocation IDLE_SYMBOLS = ResourceLocation.fromNamespaceAndPath(
            "sgjpatch", "textures/gui/mini_gate/early_port/idle_symbols.png");
    private static final ResourceLocation SHIELD_ORB = ResourceLocation.fromNamespaceAndPath(
            "sgjpatch", "textures/gui/atlantis_shield_button_off_large.png");

    private AtlantisMiniGateOverlay() {}

    public static void render(
            AtlantisDHDMenu menu, AbstractButton button, GuiGraphics graphics) {
        if (button == null)
            return;

        int x = button.getX();
        int y = button.getY();
        int gateX = x - GATE_OFFSET;
        int gateY = y - GATE_OFFSET;
        blit(graphics, GATE_BASE, gateX, gateY, GATE_SIZE);

        if (!(menu.getDHD().stargateCache.get() instanceof PegasusStargateEntity gate)) {
            blit(graphics, IDLE_SYMBOLS, gateX, gateY, GATE_SIZE);
            blit(graphics, SHIELD_ORB, x - ORB_OFFSET, y - ORB_OFFSET, ORB_SIZE);
            return;
        }

        Address encodedSymbols = gate.getEncodedSymbols();
        int lockedSymbols = Math.min(gate.getAddress().getLength(), 9);
        int liveSymbols = Math.max(lockedSymbols, gate.addressBuffer.getLength());
        if (liveSymbols == 0 && !gate.isSymbolSpinning() && !gate.isConnected())
            blit(graphics, IDLE_SYMBOLS, gateX, gateY, GATE_SIZE);

        for (int index = 0; index < lockedSymbols; ++index) {
            int symbol = encodedSymbols.symbolAt(index);
            int position = gate.getChevronPosition(index + 1);
            if (validSymbol(symbol) && position >= 0)
                blit(graphics, symbolTexture(symbol, mirrorPosition(position)),
                        gateX, gateY, GATE_SIZE);
        }

        if (gate.isSymbolSpinning()) {
            int index = gate.symbolBuffer;
            int symbol = symbolAt(encodedSymbols, index);
            if (!validSymbol(symbol))
                symbol = symbolAt(gate.addressBuffer, index);
            int position = Math.floorMod(gate.getCurrentSymbol(), 36);
            if (validSymbol(symbol) && !isLockedPosition(gate, lockedSymbols, position))
                blit(graphics, symbolTexture(symbol, mirrorPosition(position)),
                        gateX, gateY, GATE_SIZE);
        }

        for (int index = 0; index < lockedSymbols; ++index) {
            int position = gate.getChevronPosition(index + 1);
            if (position < 0)
                continue;

            int slot = Math.floorMod(Math.floorDiv(mirrorPosition(position), 4), 9);
            blit(graphics, chevronTexture(slot), gateX, gateY, GATE_SIZE);
        }

        blit(graphics, SHIELD_ORB, x - ORB_OFFSET, y - ORB_OFFSET, ORB_SIZE);
    }

    private static boolean isLockedPosition(
            PegasusStargateEntity gate, int lockedSymbols, int position) {
        for (int index = 0; index < lockedSymbols; ++index) {
            int lockedPosition = gate.getChevronPosition(index + 1);
            if (lockedPosition >= 0 && Math.floorMod(lockedPosition, 36) == position)
                return true;
        }
        return false;
    }

    private static boolean validSymbol(int symbol) {
        return symbol >= 0 && symbol <= 38;
    }

    private static int symbolAt(Address address, int index) {
        return index >= 0 && index < address.getLength()
                ? address.symbolAt(index)
                : -1;
    }

    private static int mirrorPosition(int position) {
        return Math.floorMod(-position, 36);
    }

    private static ResourceLocation symbolTexture(int symbol, int position) {
        return ResourceLocation.fromNamespaceAndPath("sgjpatch", String.format(Locale.ROOT,
                "textures/gui/mini_gate/early_port/s%02d_p%02d.png",
                symbol, position));
    }

    private static ResourceLocation chevronTexture(int slot) {
        return ResourceLocation.fromNamespaceAndPath("sgjpatch", String.format(Locale.ROOT,
                "textures/gui/mini_gate/early_port/chevron_slot_%d.png", slot));
    }

    private static void blit(GuiGraphics graphics, ResourceLocation texture, int x, int y, int size) {
        graphics.blit(texture, x, y, 0F, 0F, size, size, size, size);
    }
}
