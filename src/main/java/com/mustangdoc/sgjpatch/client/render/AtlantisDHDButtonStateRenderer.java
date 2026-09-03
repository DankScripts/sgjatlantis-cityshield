package com.mustangdoc.sgjpatch.client.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.povstalec.sgjourney.client.resourcepack.symbols.ClientPointOfOrigin;
import net.povstalec.sgjourney.client.resourcepack.symbols.ClientSymbols;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import net.povstalec.sgjourney.common.block_entities.stargate.AbstractStargateEntity;

/**
 * Draws the connected gate variant's exact SGJourney symbol textures and the
 * encoded-state glow directly above each physical Atlantis DHD crystal, plus
 * the powered lightbar above the rear shield controls.
 */
public final class AtlantisDHDButtonStateRenderer implements BlockEntityRenderer<BlockEntity> {
    private static final String MOD_ID = "sgjadditions_capacity_patch";
    private static final String BUTTON_MAP_REVISION = "gui-slot-map-r5-direct-resource-json-scale125";
    private static final AtomicBoolean BUTTON_MAP_REVISION_LOGGED = new AtomicBoolean();
    private static final double DIALER_CENTER_X = 8.8D;
    private static final double DIALER_CENTER_Z = 6.7D;
    private static final double DIALER_SCALE = 1.25D;

    /**
     * The Blockbench crystals are stored row-by-row, while the accepted GUI
     * assigns symbols by sorting its original constellation layout radially
     * and then applies the accepted AtlantisLayoutDefaults offsets. This is
     * the exact left-to-right row map of the final GUI positions, mirrored
     * onto the physical plate as it is viewed from the front. Keeping this as
     * a literal coordinate-to-symbol map makes the GUI triangle and tabletop
     * triangle use the same symbol number. Each symbol now lives beside its
     * physical coordinates so two parallel arrays cannot drift out of sync.
     */
    private static final float[] CANDIDATE_ROTATIONS = new float[] {0.0F, 90.0F, 180.0F, 270.0F};
    private static final Map<BlockState, Float> MODEL_ROTATIONS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, List<ResourceLocation>> SYMBOL_TEXTURE_LISTS =
            new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, List<ResourceLocation>> POINT_OF_ORIGIN_TEXTURES =
            new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, SymbolMask> RESOURCE_MASKS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, SymbolMask> SPRITE_MASKS = new ConcurrentHashMap<>();
    private static final SymbolMask EMPTY_SYMBOL_MASK = new SymbolMask(1, 1, new byte[1]);

    // Coordinates are in Blockbench/Minecraft model pixels and match the
    // accepted 2.0.2 table exactly. They are divided by 16 only while drawing.
    private static final Button[] OUTER_BUTTONS = new Button[] {
            button(10.4728, 9.2420, true, 4), button(9.6364, 9.2420, false, 10),
            button(8.8000, 9.2420, true, 9), button(7.9636, 9.2420, false, 3),
            button(7.1272, 9.2420, true, 1),

            button(11.3092, 7.9710, true, 14), button(10.4728, 7.9710, false, 12),
            button(9.6364, 7.9710, true, 21), button(8.8000, 7.9710, false, 26),
            button(7.9636, 7.9710, true, 20), button(7.1272, 7.9710, false, 11),
            button(6.2908, 7.9710, true, 2),

            button(12.1456, 6.7000, true, 23), button(11.3092, 6.7000, false, 6),
            button(10.4728, 6.7000, true, 38), button(9.6364, 6.7000, false, 28),
            button(7.9636, 6.7000, false, 32), button(7.1272, 6.7000, true, 27),
            button(6.2908, 6.7000, false, 13), button(5.4544, 6.7000, true, 22),

            button(12.1456, 5.4290, false, 35), button(11.3092, 5.4290, true, 16),
            button(10.4728, 5.4290, false, 30), button(9.6364, 5.4290, true, 37),
            button(8.8000, 5.4290, false, 34), button(7.9636, 5.4290, true, 29),
            button(7.1272, 5.4290, false, 17), button(6.2908, 5.4290, true, 19),
            button(5.4544, 5.4290, false, 5),

            button(11.3092, 4.1580, false, 18), button(10.4728, 4.1580, true, 25),
            button(9.6364, 4.1580, false, 31), button(8.8000, 4.1580, true, 24),
            button(7.9636, 4.1580, false, 36), button(7.1272, 4.1580, true, 7),
            button(6.2908, 4.1580, false, 15)
    };

    // The baked tabletop is viewed with model X mirrored relative to the GUI.
    // The outer mapping already accounts for that orientation, so the two
    // lower tiny symbol crystals must do the same: GUI-left Point of Origin
    // lives at the larger model-X coordinate and GUI-right Symbol 33 at the
    // smaller coordinate. Symbol 8 (top) and Engage (middle) stay centered.
    private static final Button POINT_OF_ORIGIN = centerButton(9.1700, 6.3600, true);
    private static final Button ENGAGE = centerButton(8.8000, 6.3600, false);
    private static final Button SYMBOL_33 = centerButton(8.4300, 6.3600, true);
    private static final Button SYMBOL_8 = centerButton(8.8000, 7.0400, true);

    private static final float GOLD_RED = 1.00F;
    private static final float GOLD_GREEN = 0.66F;
    private static final float GOLD_BLUE = 0.08F;
    private static final float GOLD_ALPHA = 0.86F;

    // The physical crystals are a darker teal than the GUI buttons, so use a
    // luminous cyan line color here instead of the GUI's near-black ink. This
    // keeps every constellation legible without replacing the crystal texture.
    private static final float SYMBOL_RED = 0.30F;
    private static final float SYMBOL_GREEN = 0.95F;
    private static final float SYMBOL_BLUE = 1.00F;
    private static final float ENCODED_SYMBOL_RED = 1.00F;
    private static final float ENCODED_SYMBOL_GREEN = 0.96F;
    private static final float ENCODED_SYMBOL_BLUE = 0.72F;
        private static final ResourceLocation LIGHTBAR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                "sgjatlantis_dhd",
                "textures/block/shield_frame/atlantis_prop_lightbar.png");
            private static final float LIGHTBAR_TINT_RED = 1.00F;
            private static final float LIGHTBAR_TINT_GREEN = 1.00F;
            private static final float LIGHTBAR_TINT_BLUE = 1.00F;
        private static final double LIGHTBAR_SURFACE_OFFSET = 0.001D;
    // SGJourney's Pegasus GUI uses a 28 px symbol on a 42 px outer button and
    // a 14 px symbol on a 16 px center button. Apply those same proportions to
    // the accepted physical crystal widths instead of stretching icons until
    // neighboring constellations overlap.
    private static final double OUTER_SYMBOL_SIZE = 2.0D * 0.58000D * DIALER_SCALE * 28.0D / 42.0D;
    private static final double CENTER_SYMBOL_SIZE = 2.0D * 0.24500D * DIALER_SCALE * 14.0D / 16.0D;

    public AtlantisDHDButtonStateRenderer(BlockEntityRendererProvider.Context context) {
        if (BUTTON_MAP_REVISION_LOGGED.compareAndSet(false, true)) {
            System.out.println("[Atlantis DHD] Loaded physical button mapping " + BUTTON_MAP_REVISION);
        }
    }

    @Override
    public void render(BlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (!(blockEntity instanceof AbstractDHDEntity dhd)) {
            return;
        }
        if (!dhd.getEnergyStorage().hasEnergy(1L)) {
            return;
        }

        ClientSymbols symbols = resolveClientSymbols(dhd);
        ClientPointOfOrigin pointOfOrigin = resolveClientPointOfOrigin(dhd);
        poseStack.pushPose();
        applyModelRotation(blockEntity.getBlockState(), poseStack);
        drawPoweredLightbar(poseStack, buffers);
        drawPoweredLightbarGlow(poseStack, buffers);

        for (Button button : OUTER_BUTTONS) {
            int symbol = button.symbol();
            drawButton(poseStack, buffers, dhd, symbols, button, symbol,
                    dhd.isSymbolEncoded(symbol));
        }

        drawPointOfOrigin(poseStack, buffers, dhd, pointOfOrigin, POINT_OF_ORIGIN,
                dhd.isSymbolEncoded(0));
        drawButton(poseStack, buffers, dhd, symbols, SYMBOL_33, 33, dhd.isSymbolEncoded(33));
        drawButton(poseStack, buffers, dhd, symbols, SYMBOL_8, 8, dhd.isSymbolEncoded(8));

        if (dhd.isCenterButtonEngaged()) {
            drawGoldCrystal(poseStack, buffers, ENGAGE);
        }

        poseStack.popPose();
    }

        private static void drawPoweredLightbar(
            PoseStack poseStack, MultiBufferSource buffers) {
            VertexConsumer consumer = buffers.getBuffer(
                RenderType.entityCutoutNoCull(LIGHTBAR_TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        double minX = 0.015483220D;
        double maxX = 1.984516780D;
        double minY = 1.194248183D;
        double maxY = 1.267638168D;
        double minZ = 0.945000000D;
        double maxZ = 1.005575411D;

        texturedQuad(consumer, pose,
            maxX, maxY, minZ - LIGHTBAR_SURFACE_OFFSET, 0.0F, 0.0F,
            maxX, minY, minZ - LIGHTBAR_SURFACE_OFFSET, 0.0F, 0.06875F,
            minX, minY, minZ - LIGHTBAR_SURFACE_OFFSET, 1.0F, 0.06875F,
            minX, maxY, minZ - LIGHTBAR_SURFACE_OFFSET, 1.0F, 0.0F,
            0.0F, 0.0F, -1.0F);
        texturedQuad(consumer, pose,
            minX, maxY, maxZ + LIGHTBAR_SURFACE_OFFSET, 0.0F, 0.0F,
            minX, minY, maxZ + LIGHTBAR_SURFACE_OFFSET, 0.0F, 0.06875F,
            maxX, minY, maxZ + LIGHTBAR_SURFACE_OFFSET, 1.0F, 0.06875F,
            maxX, maxY, maxZ + LIGHTBAR_SURFACE_OFFSET, 1.0F, 0.0F,
            0.0F, 0.0F, 1.0F);
        texturedQuad(consumer, pose,
            minX, maxY + LIGHTBAR_SURFACE_OFFSET, minZ, 0.0F, 0.0F,
            minX, maxY + LIGHTBAR_SURFACE_OFFSET, maxZ, 0.0F, 0.045625F,
            maxX, maxY + LIGHTBAR_SURFACE_OFFSET, maxZ, 1.0F, 0.045625F,
            maxX, maxY + LIGHTBAR_SURFACE_OFFSET, minZ, 1.0F, 0.0F,
            0.0F, 1.0F, 0.0F);
        texturedQuad(consumer, pose,
            minX, minY - LIGHTBAR_SURFACE_OFFSET, maxZ, 0.0F, 0.0F,
            minX, minY - LIGHTBAR_SURFACE_OFFSET, minZ, 0.0F, 0.045625F,
            maxX, minY - LIGHTBAR_SURFACE_OFFSET, minZ, 1.0F, 0.045625F,
            maxX, minY - LIGHTBAR_SURFACE_OFFSET, maxZ, 1.0F, 0.0F,
            0.0F, -1.0F, 0.0F);
        }

        private static void drawPoweredLightbarGlow(
            PoseStack poseStack, MultiBufferSource buffers) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poseStack.last();
        double offset = LIGHTBAR_SURFACE_OFFSET * 2.0D;
        double minX = 0.015483220D;
        double maxX = 1.984516780D;
        double minY = 1.194248183D;
        double maxY = 1.267638168D;
        double minZ = 0.945000000D;
        double maxZ = 1.005575411D;

        lightbarGlowQuad(consumer, pose,
            maxX, maxY, minZ - offset,
            maxX, minY, minZ - offset,
            minX, minY, minZ - offset,
            minX, maxY, minZ - offset);
        lightbarGlowQuad(consumer, pose,
            minX, maxY, maxZ + offset,
            minX, minY, maxZ + offset,
            maxX, minY, maxZ + offset,
            maxX, maxY, maxZ + offset);
        lightbarGlowQuad(consumer, pose,
            minX, maxY + offset, minZ,
            minX, maxY + offset, maxZ,
            maxX, maxY + offset, maxZ,
            maxX, maxY + offset, minZ);
        lightbarGlowQuad(consumer, pose,
            minX, minY - offset, maxZ,
            minX, minY - offset, minZ,
            maxX, minY - offset, minZ,
            maxX, minY - offset, maxZ);
        }

        private static void lightbarGlowQuad(
            VertexConsumer consumer, PoseStack.Pose pose,
            double x0, double y0, double z0,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            double x3, double y3, double z3) {
        consumer.addVertex(pose.pose(), (float)x0, (float)y0, (float)z0)
            .setColor(SYMBOL_RED, SYMBOL_GREEN, SYMBOL_BLUE, 1.0F);
        consumer.addVertex(pose.pose(), (float)x1, (float)y1, (float)z1)
            .setColor(SYMBOL_RED, SYMBOL_GREEN, SYMBOL_BLUE, 1.0F);
        consumer.addVertex(pose.pose(), (float)x2, (float)y2, (float)z2)
            .setColor(SYMBOL_RED, SYMBOL_GREEN, SYMBOL_BLUE, 1.0F);
        consumer.addVertex(pose.pose(), (float)x3, (float)y3, (float)z3)
            .setColor(SYMBOL_RED, SYMBOL_GREEN, SYMBOL_BLUE, 1.0F);
        }

    private static void drawButton(PoseStack poseStack, MultiBufferSource buffers,
                                   AbstractDHDEntity dhd, ClientSymbols symbols,
                                   Button button, int symbol,
                                   boolean encoded) {
        if (encoded) {
            drawGoldCrystal(poseStack, buffers, button);
        }

        if (symbol > 0) {
            SymbolMask mask = resolveSymbolMask(dhd, symbols, symbol, button.center());
            if (mask != EMPTY_SYMBOL_MASK) {
                drawConstellation(poseStack, buffers, button, mask, encoded);
            }
        }
    }

    private static void drawPointOfOrigin(PoseStack poseStack, MultiBufferSource buffers,
                                          AbstractDHDEntity dhd,
                                          ClientPointOfOrigin pointOfOrigin, Button button,
                                          boolean encoded) {
        if (encoded) {
            drawGoldCrystal(poseStack, buffers, button);
        }

        SymbolMask mask = resolvePointOfOriginMask(dhd, pointOfOrigin);
        if (mask != EMPTY_SYMBOL_MASK) {
            drawConstellation(poseStack, buffers, button, mask, encoded);
        }
    }

    private static SymbolMask resolveSymbolMask(AbstractDHDEntity dhd, ClientSymbols symbols,
                                                int symbol, boolean center) {
        // ClientSymbols.getSymbols(...) is empty in the user's current runtime,
        // even though the server-synced ResourceKey is present. Read that exact
        // client resource JSON directly so the physical button still follows
        // lantea/tauri/Universe/More Gates and resource-pack variants.
        ResourceLocation symbolsKey = resolveSymbolsKey(dhd);
        ResourceLocation texture = symbolTextureFromResourceJson(symbolsKey, symbol);
        SymbolMask mask = loadLogicalTextureMask(texture);
        if (mask != EMPTY_SYMBOL_MASK) {
            return mask;
        }

        // Retain SGJourney's stitched registry as a secondary path for packs
        // that populate ClientSymbols normally but do not expose JSON directly.
        if (symbols != null) {
            mask = loadSpriteMask(ClientSymbols.getSprite(symbols, symbol));
            if (mask != EMPTY_SYMBOL_MASK) {
                return mask;
            }
        }

        // The accepted Atlantis GUI is the final authoritative fallback. These
        // are its exact packaged Pegasus pixels, fitted at the GUI's ratio with
        // no dilation or redrawing (unlike the rejected R14 experiment).
        return loadAcceptedGuiMask(symbol, center);
    }

    private static SymbolMask resolvePointOfOriginMask(AbstractDHDEntity dhd,
                                                       ClientPointOfOrigin pointOfOrigin) {
        ResourceLocation pointOfOriginKey = resolvePointOfOriginKey(dhd);
        ResourceLocation texture = pointOfOriginTextureFromResourceJson(pointOfOriginKey);
        SymbolMask mask = loadLogicalTextureMask(texture);
        if (mask != EMPTY_SYMBOL_MASK) {
            return mask;
        }

        if (pointOfOrigin != null) {
            mask = loadSpriteMask(ClientPointOfOrigin.getSprite(pointOfOrigin));
            if (mask != EMPTY_SYMBOL_MASK) {
                return mask;
            }
        }

        // The locked GUI displays its small Pegasus symbol 38 in this slot
        // while a live Point of Origin is unavailable.
        return loadAcceptedGuiMask(38, true);
    }

    private static ResourceLocation resolveSymbolsKey(AbstractDHDEntity dhd) {
        AbstractStargateEntity<?> gate = connectedGate(dhd);
        if (gate != null && gate.symbolInfo().symbols() != null) {
            return gate.symbolInfo().symbols().location();
        }
        return dhd.symbolInfo().symbols() == null
                ? null
                : dhd.symbolInfo().symbols().location();
    }

    private static ResourceLocation resolvePointOfOriginKey(AbstractDHDEntity dhd) {
        AbstractStargateEntity<?> gate = connectedGate(dhd);
        if (gate != null && gate.symbolInfo().pointOfOrigin() != null) {
            return gate.symbolInfo().pointOfOrigin().location();
        }
        return dhd.symbolInfo().pointOfOrigin() == null
                ? null
                : dhd.symbolInfo().pointOfOrigin().location();
    }

    private static ResourceLocation symbolTextureFromResourceJson(ResourceLocation symbolsKey,
                                                                  int symbol) {
        if (symbolsKey == null || symbol <= 0) {
            return null;
        }
        List<ResourceLocation> textures = SYMBOL_TEXTURE_LISTS.computeIfAbsent(symbolsKey,
                AtlantisDHDButtonStateRenderer::loadSymbolTextureList);
        return symbol <= textures.size() ? textures.get(symbol - 1) : null;
    }

    private static List<ResourceLocation> loadSymbolTextureList(ResourceLocation symbolsKey) {
        ResourceLocation json = ResourceLocation.fromNamespaceAndPath(symbolsKey.getNamespace(),
                "sgjourney/symbols/" + symbolsKey.getPath() + ".json");
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(json);
            if (resource.isEmpty()) {
                return List.of();
            }
            try (var reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray entries = root.getAsJsonArray("textures");
                if (entries == null) {
                    return List.of();
                }
                List<ResourceLocation> textures = new ArrayList<>(entries.size());
                for (var entry : entries) {
                    ResourceLocation texture = ResourceLocation.tryParse(entry.getAsString());
                    if (texture != null) {
                        textures.add(texture);
                    }
                }
                return List.copyOf(textures);
            }
        }
        catch (Exception ignored) {
            return List.of();
        }
    }

    private static ResourceLocation pointOfOriginTextureFromResourceJson(
            ResourceLocation pointOfOriginKey) {
        if (pointOfOriginKey == null) {
            return null;
        }
        List<ResourceLocation> cached = POINT_OF_ORIGIN_TEXTURES.computeIfAbsent(pointOfOriginKey,
                AtlantisDHDButtonStateRenderer::loadPointOfOriginTexture);
        return cached.isEmpty() ? null : cached.get(0);
    }

    private static List<ResourceLocation> loadPointOfOriginTexture(ResourceLocation pointOfOriginKey) {
        ResourceLocation json = ResourceLocation.fromNamespaceAndPath(
                pointOfOriginKey.getNamespace(),
                "sgjourney/point_of_origin/" + pointOfOriginKey.getPath() + ".json");
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(json);
            if (resource.isEmpty()) {
                return List.of();
            }
            try (var reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                ResourceLocation texture = root.has("texture")
                        ? ResourceLocation.tryParse(root.get("texture").getAsString())
                        : null;
                return texture == null ? List.of() : List.of(texture);
            }
        }
        catch (Exception ignored) {
            return List.of();
        }
    }

    private static SymbolMask loadLogicalTextureMask(ResourceLocation texture) {
        if (texture == null) {
            return EMPTY_SYMBOL_MASK;
        }
        ResourceLocation resource = ResourceLocation.fromNamespaceAndPath(texture.getNamespace(),
                "textures/" + texture.getPath() + ".png");
        return loadResourceMask(resource);
    }

    private static SymbolMask loadAcceptedGuiMask(int symbol, boolean center) {
        String path = NativePegasusTextureHelper.texturePath(symbol, !center);
        if (path == null) {
            return EMPTY_SYMBOL_MASK;
        }
        return loadResourceMask(ResourceLocation.fromNamespaceAndPath("sgjpatch", path));
    }

    private static SymbolMask loadResourceMask(ResourceLocation resource) {
        return RESOURCE_MASKS.computeIfAbsent(resource,
                AtlantisDHDButtonStateRenderer::readResourceMask);
    }

    private static SymbolMask readResourceMask(ResourceLocation resourceLocation) {
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(resourceLocation);
            if (resource.isEmpty()) {
                return EMPTY_SYMBOL_MASK;
            }
            try (var stream = resource.get().open(); NativeImage image = NativeImage.read(stream)) {
                int width = image.getWidth();
                int height = image.getHeight();
                if (width <= 0 || height <= 0) {
                    return EMPTY_SYMBOL_MASK;
                }
                byte[] alpha = new byte[width * height];
                boolean visible = false;
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int alphaByte = image.getPixelRGBA(x, y) >>> 24;
                        alpha[y * width + x] = (byte)alphaByte;
                        visible |= alphaByte != 0;
                    }
                }
                return visible ? new SymbolMask(width, height, alpha) : EMPTY_SYMBOL_MASK;
            }
        }
        catch (Exception ignored) {
            return EMPTY_SYMBOL_MASK;
        }
    }

    private static SymbolMask loadSpriteMask(TextureAtlasSprite sprite) {
        if (sprite == null) {
            return EMPTY_SYMBOL_MASK;
        }
        return SPRITE_MASKS.computeIfAbsent(sprite.contents().name(), ignored -> {
            try {
                int width = sprite.contents().width();
                int height = sprite.contents().height();
                byte[] alpha = new byte[width * height];
                boolean visible = false;
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int alphaByte = sprite.getPixelRGBA(0, x, y) >>> 24;
                        alpha[y * width + x] = (byte)alphaByte;
                        visible |= alphaByte != 0;
                    }
                }
                return visible ? new SymbolMask(width, height, alpha) : EMPTY_SYMBOL_MASK;
            }
            catch (RuntimeException ignoredException) {
                return EMPTY_SYMBOL_MASK;
            }
        });
    }

    private static ClientSymbols resolveClientSymbols(AbstractDHDEntity dhd) {
        // Prefer the connected gate itself. This avoids depending on the DHD's
        // synchronized copy during the client-side cache window and guarantees
        // that a Milky Way, Pegasus, Universe, or resource-pack variant supplies
        // its own live symbol set.
        AbstractStargateEntity<?> gate = connectedGate(dhd);
        if (gate != null) {
            ClientSymbols gateSymbols = ClientSymbols.getSymbols(gate.symbolInfo().symbols());
            if (gateSymbols != null) {
                return gateSymbols;
            }
        }
        return ClientSymbols.getSymbols(dhd.symbolInfo().symbols());
    }

    private static ClientPointOfOrigin resolveClientPointOfOrigin(AbstractDHDEntity dhd) {
        AbstractStargateEntity<?> gate = connectedGate(dhd);
        if (gate != null) {
            ClientPointOfOrigin gatePointOfOrigin = ClientPointOfOrigin.getPointOfOrigin(
                    gate.symbolInfo().pointOfOrigin());
            if (gatePointOfOrigin != null) {
                return gatePointOfOrigin;
            }
        }
        return ClientPointOfOrigin.getPointOfOrigin(dhd.symbolInfo().pointOfOrigin());
    }

    private static AbstractStargateEntity<?> connectedGate(AbstractDHDEntity dhd) {
        try {
            if (dhd.stargateCache.isPresent()) {
                return dhd.stargateCache.getCached();
            }
        }
        catch (RuntimeException ignored) {
            // The client cache can be between invalidation and refill for a
            // frame. The synchronized DHD SymbolInfo remains the safe fallback.
        }
        return null;
    }

    private static void drawGoldCrystal(PoseStack poseStack, MultiBufferSource buffers, Button button) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poseStack.last();
        double stripDepth = (button.center ? 0.09816 : 0.19334) * DIALER_SCALE;
        double[] halfWidths = button.center
                ? new double[] {0.24500, 0.20336, 0.16172, 0.12008, 0.07844, 0.03680}
                : new double[] {0.58000, 0.47934, 0.37868, 0.27802, 0.17736, 0.07670};
        double totalDepth = stripDepth * 6.0;
        double startZ = button.z - totalDepth / 2.0;
        double y = 17.985 / 16.0;

        for (int strip = 0; strip < 6; strip++) {
            int widthIndex = button.up ? strip : 5 - strip;
            double halfWidth = halfWidths[widthIndex] * DIALER_SCALE;
            double x0 = (button.x - halfWidth) / 16.0;
            double x1 = (button.x + halfWidth) / 16.0;
            double z0 = (startZ + strip * stripDepth) / 16.0;
            double z1 = (startZ + (strip + 1) * stripDepth) / 16.0;
            coloredQuad(consumer, pose, x0, y, z0, x1, z1,
                    GOLD_RED, GOLD_GREEN, GOLD_BLUE, GOLD_ALPHA);
        }
    }

    private static void drawConstellation(PoseStack poseStack, MultiBufferSource buffers,
                                          Button button, SymbolMask mask,
                                          boolean encoded) {
        if (mask == EMPTY_SYMBOL_MASK) {
            return;
        }

        VertexConsumer consumer = buffers.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poseStack.last();
        double size = button.center ? CENTER_SYMBOL_SIZE : OUTER_SYMBOL_SIZE;
        double pixelWidth = size / mask.width;
        double pixelHeight = size / mask.height;
        double startX = button.x - size / 2.0D;
        double startZ = button.z - size / 2.0D;
        double y = (encoded ? 18.045D : 18.035D) / 16.0D;
        float red = encoded ? ENCODED_SYMBOL_RED : SYMBOL_RED;
        float green = encoded ? ENCODED_SYMBOL_GREEN : SYMBOL_GREEN;
        float blue = encoded ? ENCODED_SYMBOL_BLUE : SYMBOL_BLUE;

        for (int imageY = 0; imageY < mask.height; imageY++) {
            for (int imageX = 0; imageX < mask.width; imageX++) {
                int alphaByte = Byte.toUnsignedInt(mask.alpha[imageY * mask.width + imageX]);
                if (alphaByte == 0) {
                    continue;
                }

                double x0 = (startX + imageX * pixelWidth) / 16.0D;
                double x1 = (startX + (imageX + 1) * pixelWidth) / 16.0D;
                double z0 = (startZ + imageY * pixelHeight) / 16.0D;
                double z1 = (startZ + (imageY + 1) * pixelHeight) / 16.0D;
                float alpha = Math.max(0.70F, alphaByte / 255.0F);
                coloredQuad(consumer, pose, x0, y, z0, x1, z1,
                        red, green, blue, alpha);
            }
        }
    }

    private static void coloredQuad(VertexConsumer consumer, PoseStack.Pose pose,
                                    double x0, double y, double z0, double x1, double z1,
                                    float red, float green, float blue, float alpha) {
        consumer.addVertex(pose.pose(), (float)x0, (float)y, (float)z0).setColor(red, green, blue, alpha);
        consumer.addVertex(pose.pose(), (float)x0, (float)y, (float)z1).setColor(red, green, blue, alpha);
        consumer.addVertex(pose.pose(), (float)x1, (float)y, (float)z1).setColor(red, green, blue, alpha);
        consumer.addVertex(pose.pose(), (float)x1, (float)y, (float)z0).setColor(red, green, blue, alpha);
    }

    private static void texturedQuad(
            VertexConsumer consumer, PoseStack.Pose pose,
            double x0, double y0, double z0, float u0, float v0,
            double x1, double y1, double z1, float u1, float v1,
            double x2, double y2, double z2, float u2, float v2,
            double x3, double y3, double z3, float u3, float v3,
            float normalX, float normalY, float normalZ) {
        texturedVertex(consumer, pose, x0, y0, z0, u0, v0, normalX, normalY, normalZ);
        texturedVertex(consumer, pose, x1, y1, z1, u1, v1, normalX, normalY, normalZ);
        texturedVertex(consumer, pose, x2, y2, z2, u2, v2, normalX, normalY, normalZ);
        texturedVertex(consumer, pose, x3, y3, z3, u3, v3, normalX, normalY, normalZ);
    }

    private static void texturedVertex(
            VertexConsumer consumer, PoseStack.Pose pose,
            double x, double y, double z, float u, float v,
            float normalX, float normalY, float normalZ) {
        consumer.addVertex(pose.pose(), (float)x, (float)y, (float)z)
            .setColor(
                LIGHTBAR_TINT_RED,
                LIGHTBAR_TINT_GREEN,
                LIGHTBAR_TINT_BLUE,
                1.0F)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, normalX, normalY, normalZ);
    }

    private static void applyModelRotation(BlockState state, PoseStack poseStack) {
        Float detectedRotation = MODEL_ROTATIONS.computeIfAbsent(state,
                AtlantisDHDButtonStateRenderer::detectModelRotation);
        float degrees = detectedRotation != null
                ? detectedRotation
                : facingFallbackRotation(state);

        if (degrees != 0.0F) {
            poseStack.translate(0.5D, 0.0D, 0.5D);
            poseStack.mulPose(Axis.YP.rotationDegrees(degrees));
            poseStack.translate(-0.5D, 0.0D, -0.5D);
        }
    }

    /**
     * Uses the already-baked block model as the source of truth. The Atlantis
     * table is wider than one block and has existed under both standalone and
     * legacy block definitions, so relying only on the facing property can put
     * a BER overlay on the opposite side of the console. The 240 top quads at
     * Y=17.97 are the six strips making up each of the 36 outer and four center
     * crystals. Whichever quarter-turn places our 40 known centers over those
     * quads is the exact transform Minecraft used for the visible model.
     */
    private static Float detectModelRotation(BlockState state) {
        try {
            BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
            List<SurfacePoint> crystalQuads = collectCrystalSurfacePoints(model, state);
            float bestRotation = 0.0F;
            int bestScore = -1;

            for (float rotation : CANDIDATE_ROTATIONS) {
                int score = scoreRotation(crystalQuads, rotation);
                if (score > bestScore) {
                    bestScore = score;
                    bestRotation = rotation;
                }
            }

            // A correctly baked table scores six nearby top quads per crystal.
            // Require a conservative majority before trusting the detection.
            if (bestScore >= 120) {
                return bestRotation;
            }
        }
        catch (RuntimeException ignored) {
            // Resource reloads can briefly leave the model manager between
            // states. The facing fallback keeps rendering safe in that window.
        }

        // computeIfAbsent does not cache null, so a transient reload failure is
        // retried on the next frame instead of permanently storing a guess.
        return null;
    }

    private static List<SurfacePoint> collectCrystalSurfacePoints(BakedModel model, BlockState state) {
        List<SurfacePoint> points = new ArrayList<>(240);
        collectCrystalSurfacePoints(points, model.getQuads(state, null, RandomSource.create(0L)));
        for (Direction side : Direction.values()) {
            collectCrystalSurfacePoints(points, model.getQuads(state, side, RandomSource.create(0L)));
        }
        return points;
    }

    private static void collectCrystalSurfacePoints(List<SurfacePoint> points, List<BakedQuad> quads) {
        for (BakedQuad quad : quads) {
            if (quad.getDirection() != Direction.UP) {
                continue;
            }

            int[] vertices = quad.getVertices();
            int stride = vertices.length / 4;
            if (stride < 3) {
                continue;
            }

            double x = 0.0D;
            double y = 0.0D;
            double z = 0.0D;
            for (int vertex = 0; vertex < 4; vertex++) {
                int offset = vertex * stride;
                x += Float.intBitsToFloat(vertices[offset]);
                y += Float.intBitsToFloat(vertices[offset + 1]);
                z += Float.intBitsToFloat(vertices[offset + 2]);
            }
            x *= 0.25D;
            y *= 0.25D;
            z *= 0.25D;

            if (y >= 17.94D / 16.0D && y <= 18.02D / 16.0D) {
                points.add(new SurfacePoint(x, z));
            }
        }
    }

    private static int scoreRotation(List<SurfacePoint> points, float rotation) {
        int score = 0;
        for (Button button : OUTER_BUTTONS) {
            score += nearbySurfaceCount(points, button, rotation, 0.052D * DIALER_SCALE);
        }
        score += nearbySurfaceCount(points, POINT_OF_ORIGIN, rotation, 0.030D * DIALER_SCALE);
        score += nearbySurfaceCount(points, ENGAGE, rotation, 0.030D * DIALER_SCALE);
        score += nearbySurfaceCount(points, SYMBOL_33, rotation, 0.030D * DIALER_SCALE);
        score += nearbySurfaceCount(points, SYMBOL_8, rotation, 0.030D * DIALER_SCALE);
        return score;
    }

    private static int nearbySurfaceCount(List<SurfacePoint> points, Button button,
                                          float rotation, double radius) {
        SurfacePoint center = rotatePoint(button.x / 16.0D, button.z / 16.0D, rotation);
        double radiusSquared = radius * radius;
        int count = 0;
        for (SurfacePoint point : points) {
            double dx = point.x - center.x;
            double dz = point.z - center.z;
            if (dx * dx + dz * dz <= radiusSquared) {
                count++;
            }
        }
        return count;
    }

    private static SurfacePoint rotatePoint(double x, double z, float rotation) {
        double radians = Math.toRadians(rotation);
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        double relativeX = x - 0.5D;
        double relativeZ = z - 0.5D;
        return new SurfacePoint(
                0.5D + cos * relativeX + sin * relativeZ,
                0.5D - sin * relativeX + cos * relativeZ);
    }

    private static float facingFallbackRotation(BlockState state) {
        Direction facing = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;
        return switch (facing) {
            case EAST -> -90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }

    @Override
    public boolean shouldRenderOffScreen(BlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 64;
    }

    private static Button button(double x, double z, boolean up, int symbol) {
        return new Button(scaleDialerX(x), scaleDialerZ(z), up, false, symbol);
    }

    private static Button centerButton(double x, double z, boolean up) {
        return new Button(scaleDialerX(x), scaleDialerZ(z), up, true, -1);
    }

    private static double scaleDialerX(double x) {
        return DIALER_CENTER_X + (x - DIALER_CENTER_X) * DIALER_SCALE;
    }

    private static double scaleDialerZ(double z) {
        return DIALER_CENTER_Z + (z - DIALER_CENTER_Z) * DIALER_SCALE;
    }

    private record Button(double x, double z, boolean up, boolean center, int symbol) {
    }

    private record SurfacePoint(double x, double z) {
    }

    private record SymbolMask(int width, int height, byte[] alpha) {
    }


}
