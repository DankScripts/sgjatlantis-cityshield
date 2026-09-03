package com.mustangdoc.sgjpatch.client.screens;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mustangdoc.sgjpatch.config.SGJPatchClientConfig;
import com.mustangdoc.sgjpatch.client.widgets.AtlantisTriangleEngageButton;
import com.mustangdoc.sgjpatch.client.widgets.AtlantisTriangleSymbolButton;
import com.mustangdoc.sgjpatch.client.util.AtlantisGuiOverlay;
import com.mustangdoc.sgjpatch.client.util.AtlantisLayoutDefaults;
import com.mustangdoc.sgjpatch.client.util.AtlantisMiniGateOverlay;
import com.mustangdoc.sgjpatch.standalone.AtlantisDHDMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.fml.loading.FMLPaths;
import net.povstalec.sgjourney.client.screens.dhd.AbstractDHDScreen;
import net.povstalec.sgjourney.client.widgets.dhd.DHDBigButton;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NeoForge 1.21.1 port of the locked 720x360 Atlantis table GUI.
 * Gameplay behavior remains the existing 1.21.1 implementation; only the
 * accepted GUI presentation and button positions are applied here.
 */
public class AtlantisDHDScreenFixed extends AbstractDHDScreen<AtlantisDHDMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            "sgjpatch", "textures/gui/atlantis_dhd_clean.png"
    );
    private static final ResourceLocation OUTER_UP = ResourceLocation.fromNamespaceAndPath(
            "sgjpatch", "textures/gui/atlantis_dhd_crystal_outer_green_up.png"
    );
    private static final ResourceLocation OUTER_DOWN = ResourceLocation.fromNamespaceAndPath(
            "sgjpatch", "textures/gui/atlantis_dhd_crystal_outer_green_down.png"
    );
    private static final ResourceLocation CENTER_UP = ResourceLocation.fromNamespaceAndPath(
            "sgjpatch", "textures/gui/atlantis_dhd_crystal_up.png"
    );
    private static final ResourceLocation CENTER_DOWN = ResourceLocation.fromNamespaceAndPath(
            "sgjpatch", "textures/gui/atlantis_dhd_crystal_down.png"
    );
    private static final int[] ROW_COUNTS = {4, 5, 6, 7, 6, 5, 5};
    private static final int PANEL_X_OFFSET = 310;
    private static final int PANEL_Y_OFFSET = 76;
    private static final int PANEL_W = 166;
    private static final int PANEL_H = 168;
    private static final int RAW_TRI_SIZE = 32;
    private static final int X_STEP = 26;
    private static final int Y_STEP = 22;
    private static final int KEY_F8 = 297;
    private static final int KEY_LEFT = 263;
    private static final int KEY_RIGHT = 262;
    private static final int KEY_UP = 265;
    private static final int KEY_DOWN = 264;
    private static final int MOD_SHIFT = 1;
    private static final int MOD_CONTROL = 2;
    private static final String OFFSETS_FILE = "sgjpatch-atlantis-dialer-scaled2.json";

    private AbstractButton circleButton;
    private final List<CenterControl> centerControls = new ArrayList<>(4);
    private CenterOffsets centerOffsets;
    private boolean centerEditMode;
    private int selectedCenterControl = -1;

    public AtlantisDHDScreenFixed(AtlantisDHDMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, BACKGROUND);
        this.imageWidth = 720;
        this.imageHeight = 360;
    }

    @Override
    public void init() {
        super.init();
        centerControls.clear();
        centerOffsets = CenterOffsets.load();
        centerEditMode = false;
        selectedCenterControl = -1;
        addAtlantisTableLayout();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(
                BACKGROUND,
                leftPos,
                topPos,
                0F,
                0F,
                imageWidth,
                imageHeight,
                imageWidth,
                imageHeight
        );
    }

    private void addAtlantisTableLayout() {
        List<Triangle> triangles = new ArrayList<>(38);
        for (int row = 0; row < ROW_COUNTS.length; row++) {
            int count = ROW_COUNTS[row];
            int rowOffsetX = ((7 - count) * X_STEP) / 2;
            for (int col = 0; col < count; col++) {
                int rawX = rowOffsetX + col * X_STEP;
                int rawY = row * Y_STEP;
                boolean up = ((row + col) & 1) == 0;
                triangles.add(new Triangle(rawX, rawY, up));
            }
        }

        int minX = triangles.stream().mapToInt(Triangle::x).min().orElse(0);
        int minY = triangles.stream().mapToInt(Triangle::y).min().orElse(0);
        int maxX = triangles.stream().mapToInt(t -> t.x + RAW_TRI_SIZE).max().orElse(RAW_TRI_SIZE);
        int maxY = triangles.stream().mapToInt(t -> t.y + RAW_TRI_SIZE).max().orElse(RAW_TRI_SIZE);
        int bboxW = maxX - minX;
        int bboxH = maxY - minY;

        int panelX = leftPos + PANEL_X_OFFSET;
        int panelY = topPos + PANEL_Y_OFFSET;
        int dx = panelX + (PANEL_W - bboxW) / 2 - minX;
        int dy = panelY + (PANEL_H - bboxH) / 2 - minY;

        double cx = triangles.stream().mapToDouble(t -> t.x + RAW_TRI_SIZE / 2.0).average().orElse(0.0);
        double cy = triangles.stream().mapToDouble(t -> t.y + RAW_TRI_SIZE / 2.0).average().orElse(0.0);
        triangles.sort(
                Comparator.<Triangle>comparingDouble(t -> -radiusSquared(t, cx, cy))
                        .thenComparingDouble(t -> clockwiseFromTop(t, cx, cy))
        );

        // 38 grid locations. Symbols 8 and 33 are intentionally pulled into the four-button center cluster.
        for (int index = 0; index < triangles.size(); index++) {
            int symbol = index + 1;
            if (symbol == 8 || symbol == 33)
                continue;

            Triangle triangle = triangles.get(index);
            int yNudge = (symbol == 4 || symbol == 9) ? -1 : 0;
            boolean up = triangle.up ^ AtlantisLayoutDefaults.flip(index);
            addRenderableWidget(new AtlantisTriangleSymbolButton(
                    dx + triangle.x + AtlantisLayoutDefaults.dx(index),
                    dy + triangle.y + yNudge + AtlantisLayoutDefaults.dy(index),
                    42,
                    42,
                    menu,
                    symbol,
                    up,
                    up ? OUTER_UP : OUTER_DOWN,
                    20F
            ));
        }

        // Accepted center anchor from beta.1: location 33 in the radial ordering.
        Triangle centerAnchor = triangles.get(32);
        int centerX = dx + centerAnchor.x;
        int centerY = dy + centerAnchor.y + 18;

        // R17b's locked four-button center cluster uses 16x16 buttons rendered
        // from the standard 20px crystal frames. Keep these smaller than the
        // outer triangles so their overlapping bounds form the intended diamond.
        addCenterControl(
                new AtlantisTriangleSymbolButton(
                        centerX + 8, centerY - 2, 16, 20, menu, 8, true, CENTER_UP, 14F),
                7,
                centerX + 8,
                centerY - 2,
                "Top / symbol 8"
        );
        addCenterControl(
                new AtlantisTriangleSymbolButton(
                        centerX, centerY + 14, 16, 20, menu, 0, true, CENTER_UP, 14F),
                38,
                centerX,
                centerY + 14,
                "Lower-left / origin"
        );
        addCenterControl(
                new AtlantisTriangleSymbolButton(
                        centerX + 16, centerY + 14, 16, 20, menu, 33, true, CENTER_UP, 14F),
                32,
                centerX + 16,
                centerY + 14,
                "Lower-right / symbol 33"
        );

        addCenterControl(new AtlantisTriangleEngageButton(
                centerX + 8,
                centerY + 13,
                16,
                20,
                false,
                CENTER_DOWN,
                () -> {
                    menu.engageStargate();
                    onClose();
                }
        ), 39, centerX + 8, centerY + 13, "Engage");

        // Locked R13 shield/minigate anchor.
        DHDBigButton.Pegasus shieldButton = new DHDBigButton.Pegasus(
                leftPos + 521,
                topPos + 66,
                menu,
                button -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    if (minecraft.gameMode != null)
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, AtlantisDHDMenu.BUTTON_TOGGLE_SHIELD);
                }
        );
        shieldButton.setTooltip(Tooltip.create(Component.literal("Toggle Shield")));
        circleButton = shieldButton;
        addRenderableWidget(shieldButton);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        AtlantisGuiOverlay.renderControls(
            graphics,
            leftPos,
            topPos,
            menu.getDHD().getEnergyStorage().hasEnergy(1L));
        AtlantisMiniGateOverlay.render(menu, circleButton, graphics);
        renderCenterEditor(graphics);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == KEY_F8) {
            if (!SGJPatchClientConfig.f8EditorEnabled()) {
                centerEditMode = false;
                selectedCenterControl = -1;
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
            centerEditMode = !centerEditMode;
            selectedCenterControl = -1;
            return true;
        }

        if (centerEditorActive() && selectedCenterControl >= 0) {
            int dx = 0;
            int dy = 0;
            switch (keyCode) {
                case KEY_LEFT -> dx = -1;
                case KEY_RIGHT -> dx = 1;
                case KEY_UP -> dy = -1;
                case KEY_DOWN -> dy = 1;
                default -> {
                    return super.keyPressed(keyCode, scanCode, modifiers);
                }
            }

            int step = (modifiers & MOD_CONTROL) != 0 ? 10
                    : (modifiers & MOD_SHIFT) != 0 ? 5 : 1;
            moveSelectedCenterControl(dx * step, dy * step);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!centerEditorActive())
            return super.mouseClicked(mouseX, mouseY, button);

        if (button == 0) {
            for (int index = centerControls.size() - 1; index >= 0; index--) {
                if (centerControls.get(index).button().isMouseOver(mouseX, mouseY)) {
                    selectedCenterControl = index;
                    return true;
                }
            }
            selectedCenterControl = -1;
        }
        return true;
    }

    private void addCenterControl(AbstractButton button, int offsetIndex, int baseX, int baseY, String label) {
        CenterOffset offset = centerOffsets.get(offsetIndex);
        button.setX(baseX + offset.dx);
        button.setY(baseY + offset.dy);
        centerControls.add(new CenterControl(button, offsetIndex, baseX, baseY, label));
        addRenderableWidget(button);
    }

    private void moveSelectedCenterControl(int dx, int dy) {
        CenterControl control = centerControls.get(selectedCenterControl);
        CenterOffset offset = centerOffsets.get(control.offsetIndex());
        offset.dx += dx;
        offset.dy += dy;
        control.button().setX(control.baseX() + offset.dx);
        control.button().setY(control.baseY() + offset.dy);
        centerOffsets.save();
    }

    private void renderCenterEditor(GuiGraphics graphics) {
        if (!centerEditorActive())
            return;

        int panelX = leftPos + 292;
        int panelY = topPos + 48;
        graphics.fill(panelX, panelY, panelX + 388, panelY + 24, 0xD0101010);
        graphics.drawString(
                Minecraft.getInstance().font,
                "F8 CENTER EDITOR - click a center triangle; arrows move; Shift=5; Ctrl=10",
                panelX + 4,
                panelY + 3,
                0xFFFFFFFF,
                false
        );

        String selection = selectedCenterControl >= 0
                ? "Selected: " + centerControls.get(selectedCenterControl).label()
                : "Selected: none";
        graphics.drawString(
                Minecraft.getInstance().font,
                selection,
                panelX + 4,
                panelY + 13,
                selectedCenterControl >= 0 ? 0xFFFFD54F : 0xFFB0B0B0,
                false
        );

        if (selectedCenterControl >= 0) {
            AbstractButton selected = centerControls.get(selectedCenterControl).button();
            int x0 = selected.getX() - 2;
            int y0 = selected.getY() - 2;
            int x1 = selected.getX() + selected.getWidth() + 2;
            int y1 = selected.getY() + selected.getHeight() + 2;
            int color = 0xFFFFD54F;
            graphics.fill(x0, y0, x1, y0 + 1, color);
            graphics.fill(x0, y1 - 1, x1, y1, color);
            graphics.fill(x0, y0, x0 + 1, y1, color);
            graphics.fill(x1 - 1, y0, x1, y1, color);
        }
    }

    private boolean centerEditorActive() {
        return centerEditMode && SGJPatchClientConfig.f8EditorEnabled();
    }

    private static double radiusSquared(Triangle t, double cx, double cy) {
        double x = t.x + RAW_TRI_SIZE / 2.0 - cx;
        double y = t.y + RAW_TRI_SIZE / 2.0 - cy;
        return x * x + y * y;
    }

    private static double clockwiseFromTop(Triangle t, double cx, double cy) {
        double x = t.x + RAW_TRI_SIZE / 2.0 - cx;
        double y = t.y + RAW_TRI_SIZE / 2.0 - cy;
        double angle = Math.atan2(x, -y);
        return angle < 0 ? angle + Math.PI * 2.0 : angle;
    }

    private record CenterControl(
            AbstractButton button,
            int offsetIndex,
            int baseX,
            int baseY,
            String label
    ) {}

    private static final class CenterOffsets {
        private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

        private final Path path;
        private final Map<Integer, CenterOffset> offsets = new HashMap<>();

        private CenterOffsets(Path path) {
            this.path = path;
        }

        private static CenterOffsets load() {
            Path path = FMLPaths.CONFIGDIR.get().resolve(OFFSETS_FILE);
            CenterOffsets result = new CenterOffsets(path);
            if (!Files.isRegularFile(path))
                return result;

            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                    try {
                        JsonObject value = entry.getValue().getAsJsonObject();
                        CenterOffset offset = new CenterOffset();
                        if (value.has("dx"))
                            offset.dx = value.get("dx").getAsInt();
                        if (value.has("dy"))
                            offset.dy = value.get("dy").getAsInt();
                        if (value.has("flip"))
                            offset.flip = value.get("flip").getAsBoolean();
                        result.offsets.put(Integer.parseInt(entry.getKey()), offset);
                    }
                    catch (NumberFormatException | IllegalStateException ignored) {
                        // Preserve a usable editor even if an unrelated entry is malformed.
                    }
                }
            }
            catch (IOException | IllegalStateException ignored) {
                // Invalid or unavailable files fall back to the locked R17b defaults.
            }
            return result;
        }

        private CenterOffset get(int index) {
            return offsets.computeIfAbsent(index, CenterOffsets::defaultOffset);
        }

        private static CenterOffset defaultOffset(int index) {
            return switch (index) {
                case 7 -> new CenterOffset(3, 12, false);
                case 38 -> new CenterOffset(1, 18, false);
                case 32 -> new CenterOffset(5, 18, false);
                case 39 -> new CenterOffset(3, 14, false);
                default -> new CenterOffset();
            };
        }

        private void save() {
            try {
                Files.createDirectories(path.getParent());
                Files.writeString(path, GSON.toJson(offsets), StandardCharsets.UTF_8);
            }
            catch (IOException ignored) {
                // Editing remains functional for the session if persistence fails.
            }
        }
    }

    private static final class CenterOffset {
        private int dx;
        private int dy;
        @SuppressWarnings("unused")
        private boolean flip;

        private CenterOffset() {}

        private CenterOffset(int dx, int dy, boolean flip) {
            this.dx = dx;
            this.dy = dy;
            this.flip = flip;
        }
    }

    private record Triangle(int x, int y, boolean up) {}
}
