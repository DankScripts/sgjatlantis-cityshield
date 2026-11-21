package com.dankscripts.sgjatlantis_cityshield.block;

import com.dankscripts.sgjatlantis_cityshield.AtlantisCityShield;
import com.dankscripts.sgjatlantis_cityshield.power.LargeNaquadahCablePowerBridge;
import com.dankscripts.sgjatlantis_cityshield.power.GeneratorEnergyBuffer;
import com.dankscripts.sgjatlantis_cityshield.power.ShieldPowerScaling;
import com.dankscripts.sgjatlantis_cityshield.config.ShieldConfig;
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
    private final GeneratorEnergyBuffer generatorBuffer = new GeneratorEnergyBuffer(
            () -> level == null ? Long.MIN_VALUE : level.getGameTime());

    private long lastFloorZpmTick = Long.MIN_VALUE;
    private boolean floorAt(Direction side) {
        if (level == null || side == null) return false;
        net.minecraft.resources.ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(worldPosition.relative(side)).getBlock());
        return (id.getNamespace().equals("sgjatlantis_dhd") || id.getNamespace().equals("sgjadditions_capacity_patch")
                || id.getNamespace().equals("stargate_atlantis"))
                && (id.getPath().equals("atlantis_floor_conduit") || id.getPath().startsWith("atlantis_") && id.getPath().endsWith("_floor_conduit"));
    }
    private boolean hasFloorConnection() {
        for (Direction side : Direction.values()) if (floorAt(side)) return true;
        return false;
    }
    private final net.povstalec.sgjourney.common.capabilities.SGJourneyEnergy floorPort =
            new net.povstalec.sgjourney.common.capabilities.SGJourneyEnergy(Long.MAX_VALUE,Long.MAX_VALUE,0L) {
        @Override public int receiveEnergy(int amount, boolean simulate) {
            if (simulate) return (int)Math.min(Integer.MAX_VALUE,Math.max(zeroPointBuffer.receiveEnergy(amount,true), receiveLongEnergy(amount,true)));
            return (int)receiveLongEnergy(amount,false);
        }
        @Override public long receiveLongEnergy(long amount, boolean simulate) {
            return ShieldConfig.ALLOW_GENERATOR_POWER.get() ? generatorBuffer.port(Direction.DOWN).receiveEnergy((int)Math.min(Integer.MAX_VALUE,Math.max(0L,amount)),simulate) : 0L;
        }
        @Override public long receiveZeroPointEnergy(long amount, boolean simulate) {
            // A simulated offer is still a live heartbeat from the conduit network.
            // Record it even when the one-tick buffer is already full and accepts zero.
            if (amount > 0L && level != null) lastFloorZpmTick = level.getGameTime();
            long accepted=zeroPointBuffer.receiveZeroPointEnergy(amount,simulate);
            return accepted;
        }
        @Override public void onEnergyChanged(long amount,boolean simulate) {}
    };

    private boolean previousTickWasPartialPower;
    private long lastPoweredGameTime = Long.MIN_VALUE;
    private boolean emitterChunkForced;
    private boolean chunkTicketStateKnown;
    private long lastConsumedPerTick;
    private int pendingImpacts;

    public void recordImpact() {
        pendingImpacts = Math.min(4, pendingImpacts + 1);
    }
    private long monitorStoredEnergy;
    private long monitorEnergyCapacity;
    private long lastMonitorRefreshGameTime = Long.MIN_VALUE;

    public ShieldEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHIELD_EMITTER.get(), pos, state);
    }

    public boolean hasLargeNaquadahCable() {
        return level instanceof ServerLevel serverLevel
                && (cablePowerBridge.hasNearbyCable(serverLevel, worldPosition) || hasFloorConnection());
    }

    public boolean hasZeroPointPower() {
        // Controller indication and activation require a live ZPM source, not
        // energy left behind in the emitter's one-tick buffer.
        if (!(level instanceof ServerLevel serverLevel)) return false;
        if (cablePowerBridge.hasZeroPointPower(serverLevel, worldPosition)) return true;
        if (!hasFloorConnection()) return false;
        refreshPowerMonitor();
        return monitorStoredEnergy > 0L || zeroPointBuffer.hasPower()
                && lastFloorZpmTick != Long.MIN_VALUE
                && serverLevel.getGameTime() - lastFloorZpmTick <= 1L;
    }

    public boolean hasUsablePower() {
        if (hasZeroPointPower()) return true;
        if (!(level instanceof ServerLevel serverLevel) || !ShieldConfig.ALLOW_GENERATOR_POWER.get()) return false;
        ShieldDefinition definition = ShieldSavedData.get(serverLevel.getServer()).findByEmitter(serverLevel, worldPosition);
        generatorBuffer.setRequired(ShieldPowerScaling.generatorDraw(definition));
        return generatorBuffer.hasFullTick();
    }

    public boolean hasPowerConnection() {
        return hasLargeNaquadahCable() || ShieldConfig.ALLOW_GENERATOR_POWER.get()
                && (generatorBuffer.hasFullTick() || isRecentlyPowered());
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
        // The cable bridge cannot see hubs connected through Atlantis floor conduits.
        // Query the conduit only when it is present, without making this mod depend on DHD.
        if (monitorEnergyCapacity == 0L) {
            for (Direction side : Direction.values()) {
                if (!floorAt(side)) continue;
                var floor = serverLevel.getBlockEntity(worldPosition.relative(side));
                if (floor == null) continue;
                try {
                    long[] charge = (long[]) floor.getClass().getMethod("connectedHubCharge").invoke(floor);
                    if (charge.length >= 2 && charge[1] > 0L) {
                        monitorStoredEnergy = charge[0];
                        monitorEnergyCapacity = charge[1];
                        break;
                    }
                } catch (ReflectiveOperationException ignored) {
                    // Older DHD builds do not expose a source charge reading.
                }
            }
        }
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
            emitter.pendingImpacts = 0;
            emitter.generatorBuffer.consumeTick();
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
        long standingDraw = ShieldPowerScaling.zpmDraw(definition);
        long burst = 5_000_000L * emitter.pendingImpacts;
        emitter.pendingImpacts = 0;
        emitter.zeroPointBuffer.setRequiredPerTick(standingDraw > Long.MAX_VALUE - burst ? Long.MAX_VALUE : standingDraw + burst);
        emitter.generatorBuffer.setRequired(ShieldPowerScaling.generatorDraw(definition));
        int consumedGenerator = emitter.generatorBuffer.consumeTick();
        boolean generatorPowered = ShieldConfig.ALLOW_GENERATOR_POWER.get()
                && consumedGenerator >= ShieldPowerScaling.generatorDraw(definition);
        long consumedZpm = 0L;
        if (!generatorPowered) {
            emitter.cablePowerBridge.fillFromConnectedZpm(level, pos, emitter.zeroPointBuffer);
            consumedZpm = emitter.zeroPointBuffer.consumeTick();
        }
        long consumed = generatorPowered ? consumedGenerator : consumedZpm;
        emitter.lastConsumedPerTick = consumed;
        long required = emitter.zeroPointBuffer.requiredPerTick();
        boolean partial = consumedZpm > 0L && consumedZpm < required;
        boolean powered = generatorPowered || emitter.hasLargeNaquadahCable() && consumedZpm > 0L
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
        if (side != null && level != null) {
            if (floorAt(side)) return floorPort;
            if (LargeNaquadahCablePowerBridge.isDirectCable(level, worldPosition, side)) return zeroPointBuffer;
            if (ShieldConfig.ALLOW_GENERATOR_POWER.get()) return generatorBuffer.port(side);
        }
        return null;
    }
}
