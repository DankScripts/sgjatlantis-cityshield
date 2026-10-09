package com.dankscripts.sgjatlantis_cityshield.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.povstalec.sgjourney.common.block_entities.tech.CableBlockEntity;
import net.povstalec.sgjourney.common.block_entities.zpm.ZPMHubEntity;
import net.povstalec.sgjourney.common.capabilities.ZeroPointEnergy;
import net.povstalec.sgjourney.common.data.ConduitNetworks;
import net.povstalec.sgjourney.common.init.ItemInit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Couples an emitter to an SGJourney Large Naquadah Cable through up to two
 * intervening blocks. Directly adjacent cables still use SGJourney's native
 * push transfer; this bridge supplies the same one-tick buffer when a floor or
 * wall prevents the cable from touching the emitter.
 */
public final class LargeNaquadahCablePowerBridge {
    public static final int MAX_INTERVENING_BLOCKS = 2;
    private static final int MAX_CABLE_DISTANCE = MAX_INTERVENING_BLOCKS + 1;
    private static final int ROUTE_RESCAN_INTERVAL = 20;
    private static final ResourceLocation LARGE_NAQUADAH_CABLE =
            ResourceLocation.fromNamespaceAndPath("sgjourney", "large_naquadah_cable");

    private final List<PowerRoute> routes = new ArrayList<>();
    private long lastRouteScan = Long.MIN_VALUE;

    public boolean hasNearbyCable(ServerLevel level, BlockPos controllerPos) {
        return !nearbyCables(level, controllerPos).isEmpty();
    }

    public static boolean isDirectCable(Level level, BlockPos controllerPos, Direction side) {
        return isLargeNaquadahCable(level, controllerPos.relative(side));
    }

    public boolean hasZeroPointPower(ServerLevel level, BlockPos controllerPos) {
        discardInvalidRoutes(level, controllerPos);
        if (routeScanDue(level)) refreshRoutes(level, controllerPos);
        return anyRouteHasPower();
    }

    public long fillFromConnectedZpm(ServerLevel level, BlockPos controllerPos,
                                     ShieldZeroPointEnergyStorage destination) {
        if (destination.remainingCapacity() <= 0L) return 0L;

        discardInvalidRoutes(level, controllerPos);
        if (routeScanDue(level)) refreshRoutes(level, controllerPos);
        return fillFromCachedRoutes(destination);
    }

    /**
     * Returns the live aggregate energy and capacity of every ZPM connected to
     * this Large Naquadah Cable network. Route discovery de-duplicates hubs,
     * so each installed ZPM is counted once even when several cable paths can
     * reach the same hub.
     */
    public PowerSnapshot powerSnapshot(ServerLevel level, BlockPos controllerPos) {
        discardInvalidRoutes(level, controllerPos);
        if (routeScanDue(level)) refreshRoutes(level, controllerPos);

        long stored = 0L;
        long capacity = 0L;
        for (PowerRoute route : routes) {
            ZeroPointEnergy zpm = zeroPointEnergy(route.hub());
            if (zpm == null) continue;
            stored = saturatedAdd(stored, Math.max(0L, totalRemainingEnergy(zpm)));
            capacity = saturatedAdd(capacity, Math.max(0L, totalEnergyCapacity(zpm)));
        }
        return new PowerSnapshot(stored, capacity);
    }

    private static long saturatedAdd(long left, long right) {
        if (right <= 0L) return left;
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }

    private long fillFromCachedRoutes(ShieldZeroPointEnergyStorage destination) {
        long totalReceived = 0L;
        for (PowerRoute route : routes) {
            long missing = destination.remainingCapacity();
            if (missing <= 0L) break;

            ZeroPointEnergy zpm = zeroPointEnergy(route.hub());
            if (zpm == null) continue;

            long transferLimit = Math.min(route.inputCable().maxTransfer(),
                    Math.min(route.hubCable().maxTransfer(), route.hub().getMaxEnergyExtract()));
            long requested = Math.min(missing, transferLimit);
            long simulatedExtract = zpm.extractLongEnergy(requested, true);
            long accepted = destination.receiveZeroPointEnergy(simulatedExtract, true);
            if (accepted <= 0L) continue;

            long extracted = zpm.extractLongEnergy(accepted, false);
            long actuallyReceived = destination.receiveZeroPointEnergy(extracted, false);
            if (actuallyReceived > 0L) {
                route.hub().setChanged();
                totalReceived += actuallyReceived;
            }
        }
        return totalReceived;
    }

    private boolean anyRouteHasPower() {
        for (PowerRoute route : routes) {
            ZeroPointEnergy zpm = zeroPointEnergy(route.hub());
            if (zpm == null) continue;
            long transferLimit = Math.min(route.inputCable().maxTransfer(),
                    Math.min(route.hubCable().maxTransfer(), route.hub().getMaxEnergyExtract()));
            if (transferLimit > 0L && zpm.extractLongEnergy(1L, true) > 0L) return true;
        }
        return false;
    }

    private void refreshRoutes(ServerLevel level, BlockPos controllerPos) {
        routes.clear();
        lastRouteScan = level.getGameTime();

        Set<BlockPos> scannedCables = new HashSet<>();
        Set<BlockPos> discoveredHubs = new HashSet<>();
        for (BlockPos inputPos : nearbyCables(level, controllerPos)) {
            if (!scannedCables.add(inputPos)) continue;

            List<CableBlockEntity> network = ConduitNetworks.findConnectedCables(
                level, inputPos);
            for (CableBlockEntity cable : network) scannedCables.add(cable.getBlockPos());
            CableBlockEntity inputCable = network.stream()
                .filter(CableBlockEntity::canTransferZeroPointEnergy).findFirst().orElse(null);
            if (inputCable == null) continue;

            for (CableBlockEntity hubCable : network) {
                if (!isLargeNaquadahCable(level, hubCable.getBlockPos())
                        || !hubCable.canTransferZeroPointEnergy()) continue;
                BlockPos hubPos = hubCable.getBlockPos().above();
                if (!discoveredHubs.add(hubPos)) continue;
                BlockEntity blockEntity = level.getBlockEntity(hubPos);
                if (blockEntity instanceof ZPMHubEntity hub) {
                    routes.add(new PowerRoute(inputPos, inputCable, hubCable, hub));
                }
            }
        }
    }

    private void discardInvalidRoutes(ServerLevel level, BlockPos controllerPos) {
        routes.removeIf(route -> !route.isValid(level, controllerPos));
    }

    private boolean routeScanDue(ServerLevel level) {
        return lastRouteScan == Long.MIN_VALUE
                || level.getGameTime() - lastRouteScan >= ROUTE_RESCAN_INTERVAL;
    }

    private static List<BlockPos> nearbyCables(ServerLevel level, BlockPos controllerPos) {
        List<BlockPos> cables = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            for (int distance = 1; distance <= MAX_CABLE_DISTANCE; distance++) {
                BlockPos cablePos = controllerPos.relative(direction, distance);
                if (!level.hasChunkAt(cablePos) || !isLargeNaquadahCable(level, cablePos)) continue;
                cables.add(cablePos);
            }
        }
        return cables;
    }

    private static boolean isWithinCouplingRange(BlockPos controllerPos, BlockPos cablePos) {
        int dx = Math.abs(cablePos.getX() - controllerPos.getX());
        int dy = Math.abs(cablePos.getY() - controllerPos.getY());
        int dz = Math.abs(cablePos.getZ() - controllerPos.getZ());
        int nonZeroAxes = (dx > 0 ? 1 : 0) + (dy > 0 ? 1 : 0) + (dz > 0 ? 1 : 0);
        int distance = dx + dy + dz;
        return nonZeroAxes == 1 && distance >= 1 && distance <= MAX_CABLE_DISTANCE;
    }

    private static boolean isLargeNaquadahCable(Level level, BlockPos pos) {
        return LARGE_NAQUADAH_CABLE.equals(BuiltInRegistries.BLOCK.getKey(
                level.getBlockState(pos).getBlock()));
    }

    private static ZeroPointEnergy zeroPointEnergy(ZPMHubEntity hub) {
        IItemHandler inventory = hub.getItemHandler();
        if (inventory == null || inventory.getSlots() < 1) return null;
        ItemStack stack = inventory.getStackInSlot(0);
        return stack.is(ItemInit.ZPM.get()) ? hub.zpmEnergy : null;
    }

    /**
     * SGJourney exposes the energy remaining in the current entropy segment.
     * Include every untouched segment so the monitor shows the ZPM's full
     * remaining charge instead of resetting once per entropy level.
     */
    private static long totalRemainingEnergy(ZeroPointEnergy zpm) {
        long currentSegment = Math.max(0L, zpm.getTrueEnergyStored());
        long perEntropyLevel = Math.max(0L, zpm.getTrueMaxEnergyStored());
        int fullSegmentsRemaining = Math.max(0, 999 - zpm.getEntropy());
        return saturatedAdd(currentSegment,
                saturatedMultiply(perEntropyLevel, fullSegmentsRemaining));
    }

    private static long totalEnergyCapacity(ZeroPointEnergy zpm) {
        return saturatedMultiply(Math.max(0L, zpm.getTrueMaxEnergyStored()), 1000);
    }

    private static long saturatedMultiply(long value, int multiplier) {
        if (value <= 0L || multiplier <= 0) return 0L;
        if (value > Long.MAX_VALUE / multiplier) return Long.MAX_VALUE;
        return value * multiplier;
    }

    private record PowerRoute(BlockPos coupledCablePos, CableBlockEntity inputCable,
                              CableBlockEntity hubCable, ZPMHubEntity hub) {
        private boolean isValid(ServerLevel level, BlockPos controllerPos) {
            BlockPos inputPos = coupledCablePos;
            BlockPos hubCablePos = hubCable.getBlockPos();
            if (!isWithinCouplingRange(controllerPos, inputPos)
                    || !isLargeNaquadahCable(level, inputPos)
                    || !isLargeNaquadahCable(level, hubCablePos)
                    || level.getBlockEntity(inputCable.getBlockPos()) != inputCable
                    || level.getBlockEntity(hubCablePos) != hubCable
                    || level.getBlockEntity(hubCablePos.above()) != hub
                    || !inputCable.canTransferZeroPointEnergy()
                    || !hubCable.canTransferZeroPointEnergy()) return false;

            if (inputCable.getBlockPos().equals(hubCablePos)) return true;
            return inputCable.networkID() != 0 && inputCable.networkID() == hubCable.networkID();
        }
    }

    public record PowerSnapshot(long stored, long capacity) { }
}
