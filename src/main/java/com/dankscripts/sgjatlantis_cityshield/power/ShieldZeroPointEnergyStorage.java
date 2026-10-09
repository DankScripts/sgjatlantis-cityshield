package com.dankscripts.sgjatlantis_cityshield.power;

import com.dankscripts.sgjatlantis_cityshield.config.ShieldConfig;
import net.povstalec.sgjourney.common.capabilities.SGJourneyEnergy;

/**
 * A one-tick zero-point buffer that participates in SGJourney's native energy
 * network. Ordinary FE is deliberately rejected; only SGJourney's dedicated
 * zero-point transfer path can fill this storage.
 */
public final class ShieldZeroPointEnergyStorage extends SGJourneyEnergy {
    private final Runnable changed;

    public ShieldZeroPointEnergyStorage(Runnable changed) {
        super(currentDraw(), currentDraw(), 0L);
        this.changed = changed;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        syncLimits();
        if (!simulate) return 0;
        long accepted = Math.min((long) maxReceive,
                Math.min(maxReceive(), getTrueMaxEnergyStored() - getTrueEnergyStored()));
        return regularEnergy(Math.max(0L, accepted));
    }

    @Override
    public long receiveLongEnergy(long maxReceive, boolean simulate) {
        return 0L;
    }

    @Override
    public long receiveZeroPointEnergy(long maxReceive, boolean simulate) {
        syncLimits();
        return super.receiveLongEnergy(maxReceive, simulate);
    }

    @Override
    public void onEnergyChanged(long difference, boolean simulate) {
        if (!simulate) changed.run();
    }

    public long consumeTick() {
        syncLimits();
        return depleteEnergy(currentDraw(), false);
    }

    public long requiredPerTick() {
        return currentDraw();
    }

    public boolean hasPower() {
        syncLimits();
        return getTrueEnergyStored() > 0L;
    }

    public long remainingCapacity() {
        syncLimits();
        return Math.max(0L, getTrueMaxEnergyStored() - getTrueEnergyStored());
    }

    private void syncLimits() {
        long draw = currentDraw();
        capacity = draw;
        maxReceive = draw;
        maxExtract = 0L;
        if (energy > capacity) energy = capacity;
        if (energy < 0L) energy = 0L;
    }

    private static long currentDraw() {
        return Math.max(1L, ShieldConfig.ZPM_DRAW_PER_TICK.get());
    }
}
