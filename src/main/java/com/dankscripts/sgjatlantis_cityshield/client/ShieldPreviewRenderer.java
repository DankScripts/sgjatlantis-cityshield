package com.dankscripts.sgjatlantis_cityshield.client;

import com.dankscripts.sgjatlantis_cityshield.AtlantisCityShield;
import com.dankscripts.sgjatlantis_cityshield.block.ShieldControllerBlock;
import com.dankscripts.sgjatlantis_cityshield.shield.ShieldDefinition;
import com.dankscripts.sgjatlantis_cityshield.shield.ShieldMath;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Matrix4f;

import java.util.List;

@EventBusSubscriber(modid = AtlantisCityShield.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ShieldPreviewRenderer {
    private static final double DISSIPATE_TICKS = 24.0;
    private static final double HOLOGRAM_RADIUS_X = 0.22;
    private static final double HOLOGRAM_RADIUS_Z = 0.19;
    private static final double HOLOGRAM_HEIGHT = 0.14;

    private ShieldPreviewRenderer() {}

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        ResourceLocation dimension = minecraft.level.dimension().location();
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();

        for (ShieldDefinition definition : ClientShieldState.definitions()) {
            if (!definition.dimension().equals(dimension)) continue;
            ShieldVisualState visualState = visualState(minecraft.level.getGameTime(), definition);
            if (visualState.coverage() > 0.001 && visualState.dissolve() < 0.999) {
                renderActiveShield(event.getPoseStack(), cameraPos, definition, visualState,
                        minecraft.level.getGameTime());
            }
            if (definition.preview()) renderPreview(event.getPoseStack(), cameraPos, definition);
            if (definition.active()) renderControllerHologram(event.getPoseStack(), cameraPos,
                    definition, visualState, minecraft.level.getGameTime());
        }
    }

    private static void renderControllerHologram(PoseStack poseStack, Vec3 cameraPos,
                                                   ShieldDefinition definition,
                                                   ShieldVisualState visualState, long gameTime) {
        Minecraft minecraft = Minecraft.getInstance();
        BlockPos controller = definition.controllerPos();
        if (minecraft.level == null || !minecraft.level.hasChunkAt(controller)) return;
        BlockState blockState = minecraft.level.getBlockState(controller);
        if (!(blockState.getBlock() instanceof ShieldControllerBlock)) return;

        Direction facing = blockState.getValue(ShieldControllerBlock.FACING);
        double forwardX = facing.getStepX() * -0.146;
        double forwardZ = facing.getStepZ() * -0.146;
        double dx = controller.getX() + 0.5 + forwardX - cameraPos.x;
        double dy = controller.getY() + 1.105 - cameraPos.y;
        double dz = controller.getZ() + 0.5 + forwardZ - cameraPos.z;
        if (dx * dx + dy * dy + dz * dz > 4096.0) return;

        double formation = clamp01(visualState.coverage());
        int segments = 28;
        int bands = 8;

        poseStack.pushPose();
        poseStack.translate(dx, dy, dz);
        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder surface = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int band = 0; band < bands; band++) {
            double latitude1 = Math.PI * 0.5 * band / bands;
            double latitude2 = Math.PI * 0.5 * (band + 1) / bands;
            for (int segment = 0; segment < segments; segment++) {
                double longitude1 = Math.PI * 2.0 * segment / segments;
                double longitude2 = Math.PI * 2.0 * (segment + 1) / segments;
                hologramPoint(surface, matrix, latitude1, longitude1,
                        HOLOGRAM_RADIUS_X, HOLOGRAM_RADIUS_Z, HOLOGRAM_HEIGHT,
                        gameTime, formation);
                hologramPoint(surface, matrix, latitude2, longitude1,
                        HOLOGRAM_RADIUS_X, HOLOGRAM_RADIUS_Z, HOLOGRAM_HEIGHT,
                        gameTime, formation);
                hologramPoint(surface, matrix, latitude2, longitude2,
                        HOLOGRAM_RADIUS_X, HOLOGRAM_RADIUS_Z, HOLOGRAM_HEIGHT,
                        gameTime, formation);
                hologramPoint(surface, matrix, latitude1, longitude2,
                        HOLOGRAM_RADIUS_X, HOLOGRAM_RADIUS_Z, HOLOGRAM_HEIGHT,
                        gameTime, formation);
            }
        }
        renderProjectionBeam(surface, matrix);
        draw(surface);

        RenderSystem.lineWidth(1.5F);
        BufferBuilder lines = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        for (int ring = 0; ring <= 4; ring++) {
            double latitude = Math.PI * 0.5 * ring / 4.0;
            for (int segment = 0; segment < segments; segment++) {
                double longitude1 = Math.PI * 2.0 * segment / segments;
                double longitude2 = Math.PI * 2.0 * (segment + 1) / segments;
                hologramLinePoint(lines, matrix, latitude, longitude1,
                        HOLOGRAM_RADIUS_X, HOLOGRAM_RADIUS_Z, HOLOGRAM_HEIGHT, formation);
                hologramLinePoint(lines, matrix, latitude, longitude2,
                        HOLOGRAM_RADIUS_X, HOLOGRAM_RADIUS_Z, HOLOGRAM_HEIGHT, formation);
            }
        }
        for (int longitude = 0; longitude < 8; longitude++) {
            double angle = Math.PI * 2.0 * longitude / 8.0;
            for (int band = 0; band < bands; band++) {
                double latitude1 = Math.PI * 0.5 * band / bands;
                double latitude2 = Math.PI * 0.5 * (band + 1) / bands;
                hologramLinePoint(lines, matrix, latitude1, angle,
                        HOLOGRAM_RADIUS_X, HOLOGRAM_RADIUS_Z, HOLOGRAM_HEIGHT, formation);
                hologramLinePoint(lines, matrix, latitude2, angle,
                        HOLOGRAM_RADIUS_X, HOLOGRAM_RADIUS_Z, HOLOGRAM_HEIGHT, formation);
            }
        }
        draw(lines);

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static void renderProjectionBeam(BufferBuilder buffer, Matrix4f matrix) {
        float baseX = 0.055F;
        float baseZ = 0.035F;
        float topX = 0.19F;
        float topZ = 0.145F;
        float baseY = 0.005F;
        float topY = 0.115F;

        beamQuad(buffer, matrix,
                -baseX, baseY, -baseZ, baseX, baseY, -baseZ,
                topX, topY, -topZ, -topX, topY, -topZ);
        beamQuad(buffer, matrix,
                baseX, baseY, -baseZ, baseX, baseY, baseZ,
                topX, topY, topZ, topX, topY, -topZ);
        beamQuad(buffer, matrix,
                baseX, baseY, baseZ, -baseX, baseY, baseZ,
                -topX, topY, topZ, topX, topY, topZ);
        beamQuad(buffer, matrix,
                -baseX, baseY, baseZ, -baseX, baseY, -baseZ,
                -topX, topY, -topZ, -topX, topY, topZ);
    }

    private static void beamQuad(BufferBuilder buffer, Matrix4f matrix,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3,
                                 float x4, float y4, float z4) {
        beamVertex(buffer, matrix, x1, y1, z1, 0.18F);
        beamVertex(buffer, matrix, x2, y2, z2, 0.18F);
        beamVertex(buffer, matrix, x3, y3, z3, 0.04F);
        beamVertex(buffer, matrix, x4, y4, z4, 0.04F);
    }

    private static void beamVertex(BufferBuilder buffer, Matrix4f matrix,
                                   float x, float y, float z, float alpha) {
        buffer.addVertex(matrix, x, y, z).setColor(0.24F, 0.9F, 1.0F, alpha);
    }

    private static void hologramPoint(BufferBuilder buffer, Matrix4f matrix,
                                       double latitude, double longitude,
                                       double radiusX, double radiusZ, double height,
                                       long gameTime, double formation) {
        double horizontal = Math.cos(latitude);
        float x = (float) (radiusX * horizontal * Math.cos(longitude));
        float y = (float) (height * Math.sin(latitude));
        float z = (float) (radiusZ * horizontal * Math.sin(longitude));
        double wave = 0.5 + 0.5 * Math.sin(longitude * 5.0 + latitude * 9.0 + gameTime * 0.22);
        double pulse = 0.72 + 0.28 * Math.sin(gameTime * 0.19);
        double frontLatitude = formation * Math.PI * 0.5;
        double formationFade = clamp01((frontLatitude - latitude) * 24.0 + 1.0);
        float alpha = (float) ((0.07 + wave * 0.08) * pulse * formationFade);
        buffer.addVertex(matrix, x, y, z).setColor(0.12F, 0.82F, 1.0F, alpha);
    }

    private static void hologramLinePoint(BufferBuilder buffer, Matrix4f matrix,
                                           double latitude, double longitude,
                                           double radiusX, double radiusZ, double height,
                                           double formation) {
        double horizontal = Math.cos(latitude);
        float x = (float) (radiusX * horizontal * Math.cos(longitude));
        float y = (float) (height * Math.sin(latitude));
        float z = (float) (radiusZ * horizontal * Math.sin(longitude));
        double frontLatitude = formation * Math.PI * 0.5;
        double formationFade = clamp01((frontLatitude - latitude) * 24.0 + 1.0);
        buffer.addVertex(matrix, x, y, z).setColor(0.38F, 0.94F, 1.0F,
            (float) (0.62 * formationFade));
    }

    private static ShieldVisualState visualState(long gameTime, ShieldDefinition definition) {
        if (!definition.active() && definition.stateChangeGameTime() == 0L) {
            return new ShieldVisualState(0.0, 1.0, false);
        }
        long elapsed = Math.max(0L, gameTime - definition.stateChangeGameTime());
        if (definition.active()) {
            double coverage = ShieldMath.materializationCoverage(definition, gameTime);
            return new ShieldVisualState(coverage, 0.0, coverage < 0.999);
        }
        return new ShieldVisualState(1.0, Math.min(1.0, elapsed / DISSIPATE_TICKS), false);
    }

    private static void renderPreview(PoseStack poseStack, Vec3 cameraPos, ShieldDefinition definition) {
        double dx = definition.centerX() + 0.5 - cameraPos.x;
        double dy = definition.centerY() + 0.5 - cameraPos.y;
        double dz = definition.centerZ() + 0.5 - cameraPos.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int segments = distance < 256 ? 96 : distance < 768 ? 64 : 32;
        int latitudeRings = distance < 768 ? 12 : 8;
        int longitudeLines = distance < 768 ? 24 : 16;

        poseStack.pushPose();
        poseStack.translate(dx, dy, dz);
        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.lineWidth(distance < 256 ? 2.0F : 1.25F);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        float red = 0.18F;
        float green = 0.82F;
        float blue = 1.0F;
        float alpha = 0.42F;

        for (int ring = 0; ring <= latitudeRings; ring++) {
            double v = -Math.PI / 2.0 + Math.PI * ring / latitudeRings;
            for (int segment = 0; segment < segments; segment++) {
                double a1 = Math.PI * 2.0 * segment / segments;
                double a2 = Math.PI * 2.0 * (segment + 1) / segments;
                point(buffer, matrix, definition, v, a1, red, green, blue, alpha);
                point(buffer, matrix, definition, v, a2, red, green, blue, alpha);
            }
        }

        int verticalSegments = Math.max(16, segments / 2);
        for (int longitude = 0; longitude < longitudeLines; longitude++) {
            double angle = Math.PI * 2.0 * longitude / longitudeLines;
            for (int segment = 0; segment < verticalSegments; segment++) {
                double v1 = -Math.PI / 2.0 + Math.PI * segment / verticalSegments;
                double v2 = -Math.PI / 2.0 + Math.PI * (segment + 1) / verticalSegments;
                point(buffer, matrix, definition, v1, angle, red, green, blue, alpha);
                point(buffer, matrix, definition, v2, angle, red, green, blue, alpha);
            }
        }

        draw(buffer);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static void renderActiveShield(PoseStack poseStack, Vec3 cameraPos, ShieldDefinition definition,
                                           ShieldVisualState visualState, long gameTime) {
        double dx = definition.centerX() + 0.5 - cameraPos.x;
        double dy = definition.centerY() + 0.5 - cameraPos.y;
        double dz = definition.centerZ() + 0.5 - cameraPos.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int segments = distance < 256 ? 96 : distance < 768 ? 64 : 40;
        int latitudeBands = distance < 256 ? 32 : distance < 768 ? 24 : 16;
        double minimumLatitude = -Math.PI / 2.0;
        double maximumLatitude = minimumLatitude + Math.PI * visualState.coverage();
        double cameraLocalX = -dx;
        double cameraLocalY = -dy;
        double cameraLocalZ = -dz;
        boolean cameraInside = isInsideShield(definition, cameraLocalX, cameraLocalY, cameraLocalZ);
        List<ClientShieldState.ShieldImpact> impacts = ClientShieldState.activeImpacts(definition.key(), gameTime);

        poseStack.pushPose();
        poseStack.translate(dx, dy, dz);
        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder surface = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int band = 0; band < latitudeBands; band++) {
            double latitude1 = minimumLatitude + Math.PI * band / latitudeBands;
            if (latitude1 >= maximumLatitude) break;
            double latitude2 = Math.min(maximumLatitude,
                    minimumLatitude + Math.PI * (band + 1) / latitudeBands);
            for (int segment = 0; segment < segments; segment++) {
                double longitude1 = Math.PI * 2.0 * segment / segments;
                double longitude2 = Math.PI * 2.0 * (segment + 1) / segments;
                if (!cameraInside && !surfaceFacesCamera(definition,
                        (latitude1 + latitude2) * 0.5, (longitude1 + longitude2) * 0.5,
                        cameraLocalX, cameraLocalY, cameraLocalZ)) continue;
                activePoint(surface, matrix, definition, latitude1, longitude1, gameTime,
                        visualState.dissolve(), cameraLocalX, cameraLocalY, cameraLocalZ, impacts);
                activePoint(surface, matrix, definition, latitude2, longitude1, gameTime,
                        visualState.dissolve(), cameraLocalX, cameraLocalY, cameraLocalZ, impacts);
                activePoint(surface, matrix, definition, latitude2, longitude2, gameTime,
                        visualState.dissolve(), cameraLocalX, cameraLocalY, cameraLocalZ, impacts);
                activePoint(surface, matrix, definition, latitude1, longitude2, gameTime,
                        visualState.dissolve(), cameraLocalX, cameraLocalY, cameraLocalZ, impacts);
            }
        }
        draw(surface);

        if (visualState.materializing()) {
            RenderSystem.lineWidth(distance < 512 ? 3.0F : 2.0F);
            BufferBuilder front = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
            for (int segment = 0; segment < segments; segment++) {
                double longitude1 = Math.PI * 2.0 * segment / segments;
                double longitude2 = Math.PI * 2.0 * (segment + 1) / segments;
                if (!cameraInside && !surfaceFacesCamera(definition, maximumLatitude,
                        (longitude1 + longitude2) * 0.5,
                        cameraLocalX, cameraLocalY, cameraLocalZ)) continue;
                activeFrontPoint(front, matrix, definition, maximumLatitude, longitude1);
                activeFrontPoint(front, matrix, definition, maximumLatitude, longitude2);
            }
            draw(front);
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private record ShieldVisualState(double coverage, double dissolve, boolean materializing) {}

    private static void activePoint(BufferBuilder buffer, Matrix4f matrix, ShieldDefinition definition,
                                    double latitude, double longitude, long gameTime, double dissolve,
                                    double cameraX, double cameraY, double cameraZ,
                                    List<ClientShieldState.ShieldImpact> impacts) {
        double horizontal = Math.cos(latitude);
        float x = (float) (definition.radiusX() * horizontal * Math.cos(longitude));
        float z = (float) (definition.radiusZ() * horizontal * Math.sin(longitude));
        double height = latitude >= 0 ? definition.upperHeight() : definition.lowerDepth();
        float y = (float) (height * Math.sin(latitude));

        double waveA = Math.sin(longitude * 8.0 + latitude * 5.0 + gameTime * 0.075);
        double waveB = Math.sin(longitude * -5.0 + latitude * 11.0 - gameTime * 0.105);
        double waveC = Math.sin((longitude + latitude) * 16.0 + gameTime * 0.145);
        double energy = clamp01(0.5 + waveA * 0.23 + waveB * 0.17 + waveC * 0.10);

        double normalX = x / ((double) definition.radiusX() * definition.radiusX());
        double normalHeight = Math.max(1.0, height);
        double normalY = y / (normalHeight * normalHeight);
        double normalZ = z / ((double) definition.radiusZ() * definition.radiusZ());
        double normalLength = Math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
        double viewX = cameraX - x;
        double viewY = cameraY - y;
        double viewZ = cameraZ - z;
        double viewLength = Math.sqrt(viewX * viewX + viewY * viewY + viewZ * viewZ);
        double facing = normalLength == 0.0 || viewLength == 0.0 ? 1.0
                : Math.abs((normalX * viewX + normalY * viewY + normalZ * viewZ) / (normalLength * viewLength));
        double rim = Math.pow(1.0 - Math.min(1.0, facing), 2.2);
        double impactGlow = impactGlow(definition, x, y, z, gameTime, impacts);

        double fade = 1.0 - dissolve;
        double breakdown = dissolve <= 0.0 ? 1.0
                : 0.68 + 0.32 * Math.abs(Math.sin(gameTime * 0.82 + longitude * 13.0 + latitude * 9.0));
        double shutdownFlash = dissolve > 0.0 ? 0.30 * Math.sin(Math.min(1.0, dissolve * 3.0) * Math.PI) : 0.0;
        float alpha = (float) Math.min(0.94,
                (0.16 + energy * 0.13 + rim * 0.26 + shutdownFlash + impactGlow * 0.62) * fade * breakdown);
        float red = (float) Math.min(0.88,
                0.04 + energy * 0.10 + shutdownFlash * 0.35 + impactGlow * 0.58);
        float green = (float) Math.min(1.0,
                0.56 + energy * 0.30 + shutdownFlash * 0.55 + impactGlow * 0.42);
        buffer.addVertex(matrix, x, y, z).setColor(red, green, 1.0F, alpha);
    }

    private static double impactGlow(ShieldDefinition definition, double x, double y, double z,
                                     long gameTime, List<ClientShieldState.ShieldImpact> impacts) {
        if (impacts.isEmpty()) return 0.0;
        double vertexHeight = Math.max(1.0, y >= 0.0 ? definition.upperHeight() : definition.lowerDepth());
        double vertexX = x / definition.radiusX();
        double vertexY = y / vertexHeight;
        double vertexZ = z / definition.radiusZ();
        double vertexLength = Math.sqrt(vertexX * vertexX + vertexY * vertexY + vertexZ * vertexZ);
        double glow = 0.0;

        for (ClientShieldState.ShieldImpact impact : impacts) {
            double localImpactX = impact.x() - definition.centerX() - 0.5;
            double localImpactY = impact.y() - definition.centerY() - 0.5;
            double localImpactZ = impact.z() - definition.centerZ() - 0.5;
            double impactHeight = Math.max(1.0,
                    localImpactY >= 0.0 ? definition.upperHeight() : definition.lowerDepth());
            double impactX = localImpactX / definition.radiusX();
            double impactY = localImpactY / impactHeight;
            double impactZ = localImpactZ / definition.radiusZ();
            double impactLength = Math.sqrt(impactX * impactX + impactY * impactY + impactZ * impactZ);
            if (vertexLength == 0.0 || impactLength == 0.0) continue;

            double dot = (vertexX * impactX + vertexY * impactY + vertexZ * impactZ)
                    / (vertexLength * impactLength);
            double angle = Math.acos(Math.max(-1.0, Math.min(1.0, dot)));
            double age = Math.max(0.0, gameTime - impact.gameTime());
            double progress = Math.min(1.0, age / 36.0);
            double ringRadius = 0.025 + progress * 0.34;
            double ringWidth = 0.035 + progress * 0.018;
            double ringDistance = (angle - ringRadius) / ringWidth;
            double ring = Math.exp(-ringDistance * ringDistance) * (1.0 - progress);
            double coreWidth = 0.075 + progress * 0.04;
            double core = Math.exp(-(angle * angle) / (coreWidth * coreWidth))
                    * Math.max(0.0, 1.0 - age / 10.0);
            glow += impact.strength() * (ring + core * 0.8);
        }
        return Math.min(1.4, glow);
    }

    private static boolean isInsideShield(ShieldDefinition definition,
                                          double x, double y, double z) {
        double verticalRadius = Math.max(1.0,
                y >= 0.0 ? definition.upperHeight() : definition.lowerDepth());
        double normalizedX = x / definition.radiusX();
        double normalizedY = y / verticalRadius;
        double normalizedZ = z / definition.radiusZ();
        return normalizedX * normalizedX + normalizedY * normalizedY + normalizedZ * normalizedZ <= 1.0;
    }

    private static boolean surfaceFacesCamera(ShieldDefinition definition,
                                              double latitude, double longitude,
                                              double cameraX, double cameraY, double cameraZ) {
        double horizontal = Math.cos(latitude);
        double x = definition.radiusX() * horizontal * Math.cos(longitude);
        double z = definition.radiusZ() * horizontal * Math.sin(longitude);
        double height = Math.max(1.0,
                latitude >= 0.0 ? definition.upperHeight() : definition.lowerDepth());
        double y = height * Math.sin(latitude);

        double normalX = x / ((double) definition.radiusX() * definition.radiusX());
        double normalY = y / (height * height);
        double normalZ = z / ((double) definition.radiusZ() * definition.radiusZ());
        double viewX = cameraX - x;
        double viewY = cameraY - y;
        double viewZ = cameraZ - z;
        return normalX * viewX + normalY * viewY + normalZ * viewZ >= 0.0;
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static void activeFrontPoint(BufferBuilder buffer, Matrix4f matrix, ShieldDefinition definition,
                                         double latitude, double longitude) {
        double horizontal = Math.cos(latitude);
        float x = (float) (definition.radiusX() * horizontal * Math.cos(longitude));
        float z = (float) (definition.radiusZ() * horizontal * Math.sin(longitude));
        double height = latitude >= 0 ? definition.upperHeight() : definition.lowerDepth();
        float y = (float) (height * Math.sin(latitude));
        buffer.addVertex(matrix, x, y, z).setColor(0.62F, 0.94F, 1.0F, 0.9F);
    }

    private static void point(BufferBuilder buffer, Matrix4f matrix, ShieldDefinition definition,
                              double latitude, double longitude, float red, float green, float blue, float alpha) {
        double horizontal = Math.cos(latitude);
        float x = (float) (definition.radiusX() * horizontal * Math.cos(longitude));
        float z = (float) (definition.radiusZ() * horizontal * Math.sin(longitude));
        double height = latitude >= 0 ? definition.upperHeight() : definition.lowerDepth();
        float y = (float) (height * Math.sin(latitude));
        buffer.addVertex(matrix, x, y, z).setColor(red, green, blue, alpha);
    }

    private static void draw(BufferBuilder buffer) {
        var mesh = buffer.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientShieldState.clear();
    }
}
