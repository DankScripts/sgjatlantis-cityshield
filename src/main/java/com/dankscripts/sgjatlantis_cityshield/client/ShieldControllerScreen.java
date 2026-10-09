package com.dankscripts.sgjatlantis_cityshield.client;

import com.dankscripts.sgjatlantis_cityshield.menu.ShieldControllerMenu;
import com.dankscripts.sgjatlantis_cityshield.network.EditShieldPacket;
import com.dankscripts.sgjatlantis_cityshield.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ShieldControllerScreen extends AbstractContainerScreen<ShieldControllerMenu> {
    private static final Component[] LABELS = {
            Component.translatable("screen.sgjatlantis_cityshield.center_x"),
            Component.translatable("screen.sgjatlantis_cityshield.center_y"),
            Component.translatable("screen.sgjatlantis_cityshield.center_z"),
            Component.translatable("screen.sgjatlantis_cityshield.radius_x"),
            Component.translatable("screen.sgjatlantis_cityshield.radius_z"),
            Component.translatable("screen.sgjatlantis_cityshield.upper_height"),
            Component.translatable("screen.sgjatlantis_cityshield.lower_depth"),
            Component.translatable("screen.sgjatlantis_cityshield.margin")
    };

    private int step = 10;
    private Button stepButton;
    private Button previewButton;
    private Button activeButton;
    private Button commitButton;
    private Button cancelButton;
    private Button settingsButton;
    private Button backButton;
    private Button upgradesButton;
    private Button upgradesBackButton;
    private boolean settingsOpen;
    private final List<Button> geometryButtons = new ArrayList<>();
    private final List<Button> settingsWidgets = new ArrayList<>();

    public ShieldControllerScreen(ShieldControllerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 310;
        imageHeight = 288;
        titleLabelX = 10;
        titleLabelY = 8;
        inventoryLabelY = 1000;
    }

    @Override
    protected void init() {
        super.init();
        geometryButtons.clear();
        settingsWidgets.clear();
        int startY = topPos + 28;
        for (int field = 0; field < 8; field++) {
            final int selectedField = field;
            int rowY = startY + field * 20;
            Button minus = addRenderableWidget(Button.builder(Component.literal("−"), button -> adjust(selectedField, -step))
                    .bounds(leftPos + 200, rowY, 32, 18).build());
            Button plus = addRenderableWidget(Button.builder(Component.literal("+"), button -> adjust(selectedField, step))
                    .bounds(leftPos + 236, rowY, 32, 18).build());
            geometryButtons.add(minus);
            geometryButtons.add(plus);
            settingsWidgets.add(minus);
            settingsWidgets.add(plus);
        }

        stepButton = addRenderableWidget(Button.builder(stepLabel(), button -> cycleStep())
                .bounds(leftPos + 200, topPos + 192, 68, 20).build());
        settingsWidgets.add(stepButton);
        previewButton = addRenderableWidget(Button.builder(previewLabel(), button -> send(EditShieldPacket.Action.TOGGLE_PREVIEW, 0, 0))
                .bounds(leftPos + 10, topPos + 192, 82, 20).build());
        settingsWidgets.add(previewButton);
        commitButton = addRenderableWidget(Button.builder(Component.translatable("screen.sgjatlantis_cityshield.commit"),
                        button -> send(EditShieldPacket.Action.COMMIT, 0, 0))
                .bounds(leftPos + 10, topPos + 216, 90, 20).build());
        settingsWidgets.add(commitButton);
        cancelButton = addRenderableWidget(Button.builder(Component.translatable("screen.sgjatlantis_cityshield.cancel"),
                        button -> send(EditShieldPacket.Action.CANCEL, 0, 0))
                .bounds(leftPos + 106, topPos + 216, 90, 20).build());
        settingsWidgets.add(cancelButton);
        backButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.sgjatlantis_cityshield.back_to_control"),
                        button -> setSettingsOpen(false))
                .bounds(leftPos + 200, topPos + 216, 68, 20).build());
        settingsWidgets.add(backButton);

        activeButton = addRenderableWidget(Button.builder(activeLabel(),
                        button -> send(EditShieldPacket.Action.TOGGLE_ACTIVE, 0, 0))
                .bounds(leftPos + 55, topPos + 112, 200, 28).build());
        settingsButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.sgjatlantis_cityshield.settings"),
                        button -> setSettingsOpen(true))
                .bounds(leftPos + 95, topPos + 150, 120, 22).build());
        upgradesButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.sgjatlantis_cityshield.upgrades"),
                        button -> setUpgradePage(true))
                .bounds(leftPos + 75, topPos + 178, 160, 22).build());
        upgradesBackButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.sgjatlantis_cityshield.back_to_control"),
                        button -> setUpgradePage(false))
                .bounds(leftPos + 120, topPos + 230, 70, 20).build());
        updatePageVisibility();
    }

    private void setUpgradePage(boolean open) {
        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null) return;
        int page = open ? 1 : 0;
        if (!menu.clickMenuButton(minecraft.player, page)) return;
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, page);
        settingsOpen = false;
        updatePageVisibility();
    }

    private void setSettingsOpen(boolean open) {
        settingsOpen = open;
        updatePageVisibility();
    }

    private void updatePageVisibility() {
        boolean upgrades = menu.upgradesOpen();
        boolean operator = menu.operatorControls();
        settingsWidgets.forEach(button -> button.visible = settingsOpen && !upgrades && operator);
        if (activeButton != null) activeButton.visible = !settingsOpen && !upgrades && operator;
        if (settingsButton != null) settingsButton.visible = !settingsOpen && !upgrades && operator;
        if (upgradesButton != null) {
            upgradesButton.visible = !settingsOpen && !upgrades;
            upgradesButton.active = minecraft != null && minecraft.player != null
                    && menu.canUseUpgrades(minecraft.player);
        }
        if (upgradesBackButton != null) upgradesBackButton.visible = upgrades && operator;
    }

    private void adjust(int field, int amount) {
        send(EditShieldPacket.Action.ADJUST, field, amount);
    }

    private void send(EditShieldPacket.Action action, int field, int amount) {
        ModNetwork.CHANNEL.sendToServer(new EditShieldPacket(menu.controllerPos(), action, field, amount));
    }

    private void cycleStep() {
        step = step == 1 ? 10 : step == 10 ? 50 : step == 50 ? 100 : step == 100 ? 250 : 1;
        stepButton.setMessage(stepLabel());
    }

    private Component stepLabel() {
        return Component.translatable("screen.sgjatlantis_cityshield.step", step);
    }

    private Component previewLabel() {
        return Component.translatable(menu.preview()
                ? "screen.sgjatlantis_cityshield.preview_on"
                : "screen.sgjatlantis_cityshield.preview_off");
    }

    private Component activeLabel() {
        return Component.translatable(menu.active()
                ? "screen.sgjatlantis_cityshield.deactivate"
                : "screen.sgjatlantis_cityshield.activate");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (previewButton != null) previewButton.setMessage(previewLabel());
        if (activeButton != null) activeButton.setMessage(activeLabel());
        boolean editable = !menu.active();
        geometryButtons.forEach(button -> button.active = editable);
        if (previewButton != null) previewButton.active = editable;
        if (commitButton != null) commitButton.active = editable;
        if (cancelButton != null) cancelButton.active = editable;
        updatePageVisibility();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xE0101720);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + 23, 0xD0187187);
        graphics.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xFF55DDF4);
        if (menu.upgradesOpen()) {
            // AbstractContainerMenu stores these in a NonNullList on Forge 1.20.1.
            // Preserve that runtime descriptor when producing release classes.
            for (net.minecraft.world.inventory.Slot slot : menu.slots) {
                int x = leftPos + slot.x - 1;
                int y = topPos + slot.y - 1;
                graphics.fill(x, y, x + 18, y + 18, 0xFF091118);
                graphics.renderOutline(x, y, 18, 18, slot.index < 8 ? 0xFF55DDF4 : 0xFF42606A);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xE8FCFF, false);
        Component status = Component.translatable(menu.active()
                ? "screen.sgjatlantis_cityshield.active_status"
                : menu.dirty()
                    ? "screen.sgjatlantis_cityshield.uncommitted"
                    : "screen.sgjatlantis_cityshield.committed");
        int statusColor = menu.active() ? 0x55DDF4 : menu.dirty() ? 0xFFD36A : 0x6AFFB7;

        if (menu.upgradesOpen()) {
            graphics.drawCenteredString(font, Component.translatable("screen.sgjatlantis_cityshield.upgrades"),
                    imageWidth / 2, 33, 0x77EDFF);
            graphics.drawCenteredString(font, Component.translatable("screen.sgjatlantis_cityshield.communication_range",
                    menu.communicationRange()), imageWidth / 2, 50, 0x6AFFB7);
            graphics.drawCenteredString(font, Component.translatable("screen.sgjatlantis_cityshield.crystal_bonuses"),
                    imageWidth / 2, 68, 0xBDECF4);
            graphics.drawCenteredString(font, Component.translatable("screen.sgjatlantis_cityshield.crystal_rules"),
                    imageWidth / 2, 82, 0x83AEB8);
            graphics.drawString(font, Component.translatable("container.inventory"), 75, 134, 0xBDECF4, false);
            if (menu.active()) graphics.drawCenteredString(font,
                    Component.translatable("screen.sgjatlantis_cityshield.range_warning"), imageWidth / 2, 122, 0xFFD36A);
        } else if (settingsOpen) {
            int startY = 32;
            for (int field = 0; field < LABELS.length; field++) {
                int y = startY + field * 20;
                graphics.drawString(font, LABELS[field], 10, y, 0xBDECF4, false);
                String value = Integer.toString(menu.field(field));
                graphics.drawString(font, value, 186 - font.width(value), y, 0xFFFFFF, false);
            }
            graphics.drawString(font, status, 10, 244, statusColor, false);
        } else {
            Component controlTitle = Component.translatable(menu.active()
                    ? "screen.sgjatlantis_cityshield.field_online"
                    : "screen.sgjatlantis_cityshield.field_standby");
            int controlColor = menu.active() ? 0x77EDFF : 0xA7CBD1;
            graphics.drawCenteredString(font, controlTitle, imageWidth / 2, 47, controlColor);
            graphics.drawCenteredString(font, status, imageWidth / 2, 68, statusColor);
            graphics.drawCenteredString(font,
                    Component.translatable("screen.sgjatlantis_cityshield.control_hint"),
                    imageWidth / 2, 88, 0x83AEB8);
            renderPowerMonitor(graphics);
        }

        graphics.drawString(font, Component.translatable("screen.sgjatlantis_cityshield.large_cable"),
                10, 258, 0xBDECF4, false);
        Component cableStatus = Component.translatable(menu.largeCableConnected()
                ? "screen.sgjatlantis_cityshield.cable_connected"
                : "screen.sgjatlantis_cityshield.cable_missing");
        int cableColor = menu.largeCableConnected() ? 0x6AFFB7 : 0xFF765F;
        graphics.drawString(font, cableStatus, imageWidth - 10 - font.width(cableStatus), 258,
                cableColor, false);

        graphics.drawString(font, Component.translatable("screen.sgjatlantis_cityshield.zero_point_grid"),
                10, 270, 0xBDECF4, false);
        Component powerStatus = Component.translatable(menu.zeroPointPowerAvailable()
                ? "screen.sgjatlantis_cityshield.grid_ready"
                : "screen.sgjatlantis_cityshield.grid_unpowered");
        int powerColor = menu.zeroPointPowerAvailable() ? 0x6AFFB7 : 0xFF765F;
        graphics.drawString(font, powerStatus, imageWidth - 10 - font.width(powerStatus), 270,
                powerColor, false);
    }

    private void renderPowerMonitor(GuiGraphics graphics) {
        long stored = Math.max(0L, menu.connectedZpmEnergy());
        long capacity = Math.max(0L, menu.connectedZpmCapacity());
        long draw = Math.max(0L, menu.currentPowerDraw());

        graphics.fill(42, 204, 268, 252, 0xB0081118);
        graphics.renderOutline(42, 204, 226, 48, 0xAA2EB8CE);
        graphics.drawString(font,
                Component.translatable("screen.sgjatlantis_cityshield.power_monitor"),
                52, 208, 0x77EDFF, false);

        String runtime = formatRuntime(stored, draw);
        graphics.drawString(font, runtime, 258 - font.width(runtime), 208,
                draw > 0L ? 0xFFD36A : 0xA7CBD1, false);

        int filled = capacity <= 0L ? 0
                : (int) Math.min(198L, Math.round((double) stored * 198.0D / capacity));
        graphics.fill(56, 219, 254, 225, 0xFF17252B);
        if (filled > 0) graphics.fill(56, 219, 56 + filled, 225, 0xFF33CFE8);
        graphics.renderOutline(56, 219, 198, 6, 0xFF4A8995);

        drawMonitorLine(graphics, 230,
                Component.translatable("screen.sgjatlantis_cityshield.zpm_supply"),
                capacity > 0L ? formatEnergy(stored) : Component.translatable(
                        "screen.sgjatlantis_cityshield.unavailable").getString(),
                capacity > 0L ? 0xBDECF4 : 0xFF765F);
        drawMonitorLine(graphics, 242,
                Component.translatable("screen.sgjatlantis_cityshield.live_draw"),
                formatEnergy(draw) + "/t", draw > 0L ? 0x6AFFB7 : 0xA7CBD1);
    }

    private void drawMonitorLine(GuiGraphics graphics, int y, Component label,
                                 String value, int valueColor) {
        graphics.drawString(font, label, 52, y, 0x83AEB8, false);
        graphics.drawString(font, value, 258 - font.width(value), y, valueColor, false);
    }

    private static String formatEnergy(long energy) {
        if (energy >= 1_000_000_000_000_000L) return formatUnit(energy, 1_000_000_000_000_000L, "PFE");
        if (energy >= 1_000_000_000_000L) return formatUnit(energy, 1_000_000_000_000L, "TFE");
        if (energy >= 1_000_000_000L) return formatUnit(energy, 1_000_000_000L, "GFE");
        if (energy >= 1_000_000L) return formatUnit(energy, 1_000_000L, "MFE");
        if (energy >= 1_000L) return formatUnit(energy, 1_000L, "kFE");
        return energy + " FE";
    }

    private static String formatUnit(long energy, long unit, String suffix) {
        return String.format(Locale.ROOT, "%.2f %s", (double) energy / unit, suffix);
    }

    private static String formatRuntime(long stored, long draw) {
        if (draw <= 0L) return Component.translatable(
                "screen.sgjatlantis_cityshield.standby").getString();
        long totalSeconds = stored / draw / 20L;
        long days = totalSeconds / 86_400L;
        long hours = totalSeconds % 86_400L / 3_600L;
        long minutes = totalSeconds % 3_600L / 60L;
        long seconds = totalSeconds % 60L;
        if (days > 0L) return days + "d " + hours + "h";
        if (hours > 0L) return hours + "h " + minutes + "m";
        if (minutes > 0L) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
