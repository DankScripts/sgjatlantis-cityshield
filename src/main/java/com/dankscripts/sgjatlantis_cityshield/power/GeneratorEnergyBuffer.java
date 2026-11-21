package com.dankscripts.sgjatlantis_cityshield.power;

import com.dankscripts.sgjatlantis_cityshield.config.ShieldConfig;
import net.minecraft.core.Direction;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.LongSupplier;

/** A one-tick FE input. Each face has its own transfer limit. */
public final class GeneratorEnergyBuffer {
    private final LongSupplier gameTime;
    private final Map<Direction, Port> ports = new EnumMap<>(Direction.class);
    private int required = 1;
    private int stored;
    private long quotaTick = Long.MIN_VALUE;

    public GeneratorEnergyBuffer(LongSupplier gameTime) {
        this.gameTime = gameTime;
        for (Direction side : Direction.values()) ports.put(side, new Port(side));
    }

    public IEnergyStorage port(Direction side) { return ports.get(side); }

    public void setRequired(int amount) {
        required = Math.max(1, amount);
        stored = Math.min(stored, required);
    }

    public boolean hasFullTick() { return stored >= required; }

    public int consumeTick() {
        int consumed = Math.min(stored, required);
        stored = 0;
        return consumed;
    }

    private void refreshQuota() {
        long tick = gameTime.getAsLong();
        if (tick == quotaTick) return;
        quotaTick = tick;
        for (Port port : ports.values()) port.receivedThisTick = 0;
    }

    private final class Port implements IEnergyStorage {
        private final Direction side;
        private int receivedThisTick;

        private Port(Direction side) { this.side = side; }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (!ShieldConfig.ALLOW_GENERATOR_POWER.get() || maxReceive <= 0) return 0;
            refreshQuota();
            int sideLimit = ShieldConfig.GENERATOR_FE_PER_SIDE.get();
            int accepted = Math.max(0, Math.min(maxReceive,
                    Math.min(required - stored, sideLimit - receivedThisTick)));
            if (!simulate && accepted > 0) {
                stored += accepted;
                receivedThisTick += accepted;
            }
            return accepted;
        }

        @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return stored; }
        @Override public int getMaxEnergyStored() { return required; }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return ShieldConfig.ALLOW_GENERATOR_POWER.get(); }
    }
}
