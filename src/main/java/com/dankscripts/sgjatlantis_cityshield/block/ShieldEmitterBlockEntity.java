package com.dankscripts.sgjatlantis_cityshield.block;

import com.dankscripts.sgjatlantis_cityshield.AtlantisCityShield;
import com.dankscripts.sgjatlantis_cityshield.power.LargeNaquadahCablePowerBridge;
import com.dankscripts.sgjatlantis_cityshield.power.ShieldZeroPointEnergyStorage;
import com.dankscripts.sgjatlantis_cityshield.registry.ModBlockEntities;
import com.dankscripts.sgjatlantis_cityshield.shield.ShieldDefinition;
import com.dankscripts.sgjatlantis_cityshield.shield.ShieldSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.world.chunk.ForcedChunkManager;
import net.minecraft.core.HolderLookup;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ShieldEmitterBlockEntity extends BlockEntity {
    private final ShieldZeroPointEnergyStorage zeroPointBuffer =
            new ShieldZeroPointEnergyStorage(this::setChanged);
    private final LargeNaquadahCablePowerBridge cablePowerBridge = new LargeNaquadahCablePowerBridge();
    private boolean previousTickWasPartialPower;
    private long lastPoweredGameTime = Long.MIN_VALUE;
    private boolean emitterChunkForced;
    private boolean chunkTicketStateKnown;
    private long lastConsumedPerTick;
    private long monitorStoredEnergy;
    private long monitorEnergyCapacity;
    private long lastMonitorRefreshGameTime = Long.MIN_VALUE;

    public ShieldEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHIELD_EMITTER.get(), pos, state);
    }

    public boolean hasLargeNaquadahCable() {
        return level instanceof ServerLevel serverLevel
                && cablePowerBridge.hasNearbyCable(serverLevel, worldPosition);
    }

    public boolean hasZeroPointPower() {
        // Controller indication and activation require a live ZPM source, not
        // energy left behind in the emitter's one-tick buffer.
        return level instanceof ServerLevel serverLevel
                && cablePowerBridge.hasZeroPointPower(serverLevel, worldPosition);
    }

    public boolean isRecentlyPowered() {
        return level instanceof ServerLevel serverLevel && lastPoweredGameTime != Long.MIN_VALUE
                && serverLevel.getGameTime() - lastPoweredGameTime <= 1L;
    }

    public long currentPowerDraw() {
        return Math.max(0L, lastConsumedPerTick);
    }

    public long connectedZpmEnergy() {
        refreshPowerMonitor();
        return monitorStoredEnergy;
    }

    public long connectedZpmCapacity() {
        refreshPowerMonitor();
        return monitorEnergyCapacity;
    }

    private void refreshPowerMonitor() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        long gameTime = serverLevel.getGameTime();
        if (lastMonitorRefreshGameTime == gameTime) return;
        LargeNaquadahCablePowerBridge.PowerSnapshot snapshot =
                cablePowerBridge.powerSnapshot(serverLevel, worldPosition);
        monitorStoredEnergy = snapshot.stored();
        monitorEnergyCapacity = snapshot.capacity();
        lastMonitorRefreshGameTime = gameTime;
    }

    public void setShieldActive(boolean active) {
        if (active) previousTickWasPartialPower = false;
        forceEmitterChunk(active);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state,
                                  ShieldEmitterBlockEntity emitter) {
        ShieldSavedData data = ShieldSavedData.get(level.getServer());
        ShieldDefinition definition = data.findByEmitter(level, pos);
        if (definition == null || !definition.active()) {
            emitter.previousTickWasPartialPower = false;
            emitter.lastConsumedPerTick = 0L;
            emitter.forceEmitterChunk(false);
            return;
        }
        BlockPos controllerPos = definition.controllerPos();
        level.getChunkAt(controllerPos);
        if (!(level.getBlockEntity(controllerPos) instanceof ShieldControllerBlockEntity controller)
                || !controller.canCommunicateWith(pos)) {
            definition.setActive(false, level.getGameTime());
            data.changed();
            emitter.forceEmitterChunk(false);
            return;
        }
        emitter.forceEmitterChunk(true);
        emitter.cablePowerBridge.fillFromConnectedZpm(level, pos, emitter.zeroPointBuffer);
        long consumed = emitter.zeroPointBuffer.consumeTick();
        emitter.lastConsumedPerTick = consumed;
        long required = emitter.zeroPointBuffer.requiredPerTick();
        boolean partial = consumed > 0L && consumed < required;
        boolean powered = emitter.hasLargeNaquadahCable() && consumed > 0L
                && (!partial || !emitter.previousTickWasPartialPower);
        emitter.previousTickWasPartialPower = partial;
        if (powered) {
            emitter.lastPoweredGameTime = level.getGameTime();
        } else {
            definition.setActive(false, level.getGameTime());
            data.changed();
            emitter.forceEmitterChunk(false);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            ShieldSavedData data = ShieldSavedData.get(serverLevel.getServer());
            data.registerEmitter(serverLevel, worldPosition);
            data.linkEmitterToNearestController(serverLevel, worldPosition);
        }
    }

    public void releaseChunkTicket() { forceEmitterChunk(false); }

    private void forceEmitterChunk(boolean force) {
        if (!(level instanceof ServerLevel serverLevel)
                || chunkTicketStateKnown && emitterChunkForced == force) return;
        AtlantisCityShield.CHUNK_TICKETS.forceChunk(serverLevel, worldPosition,
                worldPosition.getX() >> 4, worldPosition.getZ() >> 4, force, true);
        emitterChunkForced = force;
        chunkTicketStateKnown = true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("ZeroPointBuffer", zeroPointBuffer.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("ZeroPointBuffer")) zeroPointBuffer.deserializeNBT(registries, tag.get("ZeroPointBuffer"));
    }

    public @Nullable IEnergyStorage energyForSide(@Nullable Direction side) {
        if (side != null && level != null
                && LargeNaquadahCablePowerBridge.isDirectCable(level, worldPosition, side)) {
            return zeroPointBuffer;
        }
        return null;
    }
}
