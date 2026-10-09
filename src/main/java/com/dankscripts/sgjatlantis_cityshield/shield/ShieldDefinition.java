package com.dankscripts.sgjatlantis_cityshield.shield;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Objects;

public final class ShieldDefinition {
    public static final int MIN_RADIUS = 16;
    public static final int MAX_RADIUS = 16384;
    public static final int MIN_HEIGHT = 8;
    public static final int MAX_HEIGHT = 2048;
    public static final int MAX_MARGIN = 4096;

    private final ResourceLocation dimension;
    private final long controllerPos;
    private long emitterPos = Long.MIN_VALUE;

    private int centerX;
    private int centerY;
    private int centerZ;
    private int radiusX = 500;
    private int radiusZ = 500;
    private int upperHeight = 300;
    private int lowerDepth = 40;
    private int margin = 10;

    private int committedCenterX;
    private int committedCenterY;
    private int committedCenterZ;
    private int committedRadiusX = 500;
    private int committedRadiusZ = 500;
    private int committedUpperHeight = 300;
    private int committedLowerDepth = 40;
    private int committedMargin = 10;
    private boolean preview;
    private boolean active;
    private long stateChangeGameTime;

    public ShieldDefinition(ResourceKey<Level> dimension, BlockPos controller) {
        this(dimension.location(), controller.asLong());
        centerX = committedCenterX = controller.getX();
        centerY = committedCenterY = controller.getY();
        centerZ = committedCenterZ = controller.getZ();
    }

    private ShieldDefinition(ResourceLocation dimension, long controllerPos) {
        this.dimension = dimension;
        this.controllerPos = controllerPos;
    }

    public String key() {
        return dimension + "@" + controllerPos;
    }

    public ResourceLocation dimension() { return dimension; }
    public BlockPos controllerPos() { return BlockPos.of(controllerPos); }
    public boolean hasEmitter() { return emitterPos != Long.MIN_VALUE; }
    public BlockPos emitterPos() { return hasEmitter() ? BlockPos.of(emitterPos) : null; }
    public int centerX() { return centerX; }
    public int centerY() { return centerY; }
    public int centerZ() { return centerZ; }
    public int radiusX() { return radiusX; }
    public int radiusZ() { return radiusZ; }
    public int upperHeight() { return upperHeight; }
    public int lowerDepth() { return lowerDepth; }
    public int margin() { return margin; }
    public boolean preview() { return preview; }
    public boolean active() { return active; }
    public long stateChangeGameTime() { return stateChangeGameTime; }

    public void setEmitter(BlockPos emitter) {
        emitterPos = emitter == null ? Long.MIN_VALUE : emitter.asLong();
    }

    public void clearEmitter() {
        emitterPos = Long.MIN_VALUE;
    }

    public boolean dirty() {
        return centerX != committedCenterX || centerY != committedCenterY || centerZ != committedCenterZ
                || radiusX != committedRadiusX || radiusZ != committedRadiusZ
                || upperHeight != committedUpperHeight || lowerDepth != committedLowerDepth
                || margin != committedMargin;
    }

    public int getField(int field) {
        return switch (field) {
            case 0 -> centerX;
            case 1 -> centerY;
            case 2 -> centerZ;
            case 3 -> radiusX;
            case 4 -> radiusZ;
            case 5 -> upperHeight;
            case 6 -> lowerDepth;
            case 7 -> margin;
            default -> 0;
        };
    }

    public void adjust(int field, int delta) {
        setField(field, getField(field) + delta);
    }

    public void setField(int field, int value) {
        switch (field) {
            case 0 -> centerX = value;
            case 1 -> centerY = value;
            case 2 -> centerZ = value;
            case 3 -> radiusX = clamp(value, MIN_RADIUS, MAX_RADIUS);
            case 4 -> radiusZ = clamp(value, MIN_RADIUS, MAX_RADIUS);
            case 5 -> upperHeight = clamp(value, MIN_HEIGHT, MAX_HEIGHT);
            case 6 -> lowerDepth = clamp(value, 0, MAX_HEIGHT);
            case 7 -> margin = clamp(value, 0, MAX_MARGIN);
            default -> { }
        }
    }

    public void setCenter(BlockPos pos) {
        centerX = pos.getX();
        centerY = pos.getY();
        centerZ = pos.getZ();
    }

    public void setPreview(boolean preview) {
        this.preview = preview && !active;
    }

    public void togglePreview() {
        if (!active) preview = !preview;
    }

    public void toggleActive(long gameTime) {
        setActive(!active, gameTime);
    }

    public void setActive(boolean active, long gameTime) {
        if (this.active == active) return;
        if (active) {
            commit();
            preview = false;
        }
        this.active = active;
        stateChangeGameTime = gameTime;
    }

    public void commit() {
        committedCenterX = centerX;
        committedCenterY = centerY;
        committedCenterZ = centerZ;
        committedRadiusX = radiusX;
        committedRadiusZ = radiusZ;
        committedUpperHeight = upperHeight;
        committedLowerDepth = lowerDepth;
        committedMargin = margin;
    }

    public void cancel() {
        centerX = committedCenterX;
        centerY = committedCenterY;
        centerZ = committedCenterZ;
        radiusX = committedRadiusX;
        radiusZ = committedRadiusZ;
        upperHeight = committedUpperHeight;
        lowerDepth = committedLowerDepth;
        margin = committedMargin;
    }

    public void applyMark(SurveyMark mark, BlockPos pos) {
        switch (mark) {
            case CENTER -> setCenter(pos);
            case NORTH, SOUTH -> radiusZ = clamp(Math.abs(pos.getZ() - centerZ) + margin, MIN_RADIUS, MAX_RADIUS);
            case EAST, WEST -> radiusX = clamp(Math.abs(pos.getX() - centerX) + margin, MIN_RADIUS, MAX_RADIUS);
            case TOP -> upperHeight = clamp(pos.getY() - centerY + margin, MIN_HEIGHT, MAX_HEIGHT);
            case BOTTOM -> lowerDepth = clamp(centerY - pos.getY() + margin, 0, MAX_HEIGHT);
            case BOUNDARY -> fitBoundary(pos);
        }
    }

    private void fitBoundary(BlockPos pos) {
        double targetX = Math.abs((double) pos.getX() - centerX) + margin;
        double targetZ = Math.abs((double) pos.getZ() - centerZ) + margin;
        double normalizedX = targetX / radiusX;
        double normalizedZ = targetZ / radiusZ;
        double requiredScale = Math.sqrt(normalizedX * normalizedX + normalizedZ * normalizedZ);
        if (requiredScale <= 1.0) return;

        radiusX = clamp((int) Math.ceil(radiusX * requiredScale), MIN_RADIUS, MAX_RADIUS);
        radiusZ = clamp((int) Math.ceil(radiusZ * requiredScale), MIN_RADIUS, MAX_RADIUS);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Dimension", dimension.toString());
        tag.putLong("Controller", controllerPos);
        if (hasEmitter()) tag.putLong("Emitter", emitterPos);
        tag.putInt("CenterX", centerX);
        tag.putInt("CenterY", centerY);
        tag.putInt("CenterZ", centerZ);
        tag.putInt("RadiusX", radiusX);
        tag.putInt("RadiusZ", radiusZ);
        tag.putInt("UpperHeight", upperHeight);
        tag.putInt("LowerDepth", lowerDepth);
        tag.putInt("Margin", margin);
        tag.putInt("CommittedCenterX", committedCenterX);
        tag.putInt("CommittedCenterY", committedCenterY);
        tag.putInt("CommittedCenterZ", committedCenterZ);
        tag.putInt("CommittedRadiusX", committedRadiusX);
        tag.putInt("CommittedRadiusZ", committedRadiusZ);
        tag.putInt("CommittedUpperHeight", committedUpperHeight);
        tag.putInt("CommittedLowerDepth", committedLowerDepth);
        tag.putInt("CommittedMargin", committedMargin);
        tag.putBoolean("Preview", preview);
        tag.putBoolean("Active", active);
        tag.putLong("StateChangeGameTime", stateChangeGameTime);
        return tag;
    }

    public static ShieldDefinition load(CompoundTag tag) {
        ShieldDefinition definition = new ShieldDefinition(new ResourceLocation(tag.getString("Dimension")), tag.getLong("Controller"));
        if (tag.contains("Emitter")) definition.emitterPos = tag.getLong("Emitter");
        definition.centerX = tag.getInt("CenterX");
        definition.centerY = tag.getInt("CenterY");
        definition.centerZ = tag.getInt("CenterZ");
        definition.radiusX = tag.getInt("RadiusX");
        definition.radiusZ = tag.getInt("RadiusZ");
        definition.upperHeight = tag.getInt("UpperHeight");
        definition.lowerDepth = tag.getInt("LowerDepth");
        definition.margin = tag.getInt("Margin");
        definition.committedCenterX = tag.getInt("CommittedCenterX");
        definition.committedCenterY = tag.getInt("CommittedCenterY");
        definition.committedCenterZ = tag.getInt("CommittedCenterZ");
        definition.committedRadiusX = tag.getInt("CommittedRadiusX");
        definition.committedRadiusZ = tag.getInt("CommittedRadiusZ");
        definition.committedUpperHeight = tag.getInt("CommittedUpperHeight");
        definition.committedLowerDepth = tag.getInt("CommittedLowerDepth");
        definition.committedMargin = tag.getInt("CommittedMargin");
        definition.preview = tag.getBoolean("Preview");
        definition.active = tag.getBoolean("Active");
        definition.stateChangeGameTime = tag.getLong("StateChangeGameTime");
        return definition;
    }

    /**
     * Rehomes retained shield geometry to a newly placed controller. The center
     * coordinates deliberately remain in world space so moving the hardware does
     * not drag a city-sized survey away from Atlantis. A portable controller is
     * always restored offline.
     */
    public static ShieldDefinition restoreAt(ResourceKey<Level> dimension, BlockPos controller,
                                             CompoundTag retained, long gameTime) {
        ShieldDefinition stored = load(retained);
        ShieldDefinition restored = new ShieldDefinition(dimension, controller);
        restored.emitterPos = stored.emitterPos;

        restored.centerX = stored.centerX;
        restored.centerY = stored.centerY;
        restored.centerZ = stored.centerZ;
        restored.radiusX = clamp(stored.radiusX, MIN_RADIUS, MAX_RADIUS);
        restored.radiusZ = clamp(stored.radiusZ, MIN_RADIUS, MAX_RADIUS);
        restored.upperHeight = clamp(stored.upperHeight, MIN_HEIGHT, MAX_HEIGHT);
        restored.lowerDepth = clamp(stored.lowerDepth, 0, MAX_HEIGHT);
        restored.margin = clamp(stored.margin, 0, MAX_MARGIN);

        restored.committedCenterX = stored.committedCenterX;
        restored.committedCenterY = stored.committedCenterY;
        restored.committedCenterZ = stored.committedCenterZ;
        restored.committedRadiusX = clamp(stored.committedRadiusX, MIN_RADIUS, MAX_RADIUS);
        restored.committedRadiusZ = clamp(stored.committedRadiusZ, MIN_RADIUS, MAX_RADIUS);
        restored.committedUpperHeight = clamp(stored.committedUpperHeight, MIN_HEIGHT, MAX_HEIGHT);
        restored.committedLowerDepth = clamp(stored.committedLowerDepth, 0, MAX_HEIGHT);
        restored.committedMargin = clamp(stored.committedMargin, 0, MAX_MARGIN);

        restored.preview = stored.preview;
        restored.active = false;
        restored.stateChangeGameTime = gameTime;
        return restored;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(dimension);
        buffer.writeLong(controllerPos);
        for (int i = 0; i < 8; i++) buffer.writeInt(getField(i));
        buffer.writeBoolean(preview);
        buffer.writeBoolean(dirty());
        buffer.writeBoolean(active);
        buffer.writeLong(stateChangeGameTime);
        buffer.writeBoolean(hasEmitter());
        if (hasEmitter()) buffer.writeLong(emitterPos);
    }

    public static ShieldDefinition decode(FriendlyByteBuf buffer) {
        ShieldDefinition definition = new ShieldDefinition(buffer.readResourceLocation(), buffer.readLong());
        for (int i = 0; i < 8; i++) definition.setField(i, buffer.readInt());
        definition.preview = buffer.readBoolean();
        if (!buffer.readBoolean()) definition.commit();
        definition.active = buffer.readBoolean();
        definition.stateChangeGameTime = buffer.readLong();
        if (buffer.readBoolean()) definition.emitterPos = buffer.readLong();
        return definition;
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof ShieldDefinition other && key().equals(other.key());
    }

    @Override
    public int hashCode() {
        return Objects.hash(key());
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
