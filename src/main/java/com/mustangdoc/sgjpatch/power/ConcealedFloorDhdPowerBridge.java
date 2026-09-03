package com.mustangdoc.sgjpatch.power;

import com.mustangdoc.sgjpatch.standalone.AtlantisDHDEntity;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.povstalec.sgjourney.common.block_entities.tech.CableBlockEntity;
import net.povstalec.sgjourney.common.blocks.tech.CableBlock;

/**
 * Exposes the Atlantis DHD through the decorative floor block above a buried
 * Large Naquadah Cable. SGJourney can then use its normal cable output path,
 * including its native zero-point-energy transfer method.
 */
public final class ConcealedFloorDhdPowerBridge {
    private static final int SEARCH_RADIUS = 4;
    private static final int MAX_DHD_HEIGHT_ABOVE_FLOOR = 2;
    private static final int MAX_CONNECTED_CABLE_BLOCKS = 1024;

    private ConcealedFloorDhdPowerBridge() {}

    public static IEnergyStorage getFloorEnergyCapability(
            Level level, BlockPos queriedPos, BlockEntity blockEntity, Direction side) {
        if (blockEntity != null || side == null)
            return null;

        if (side == Direction.DOWN
                && isLargeCable(level, queriedPos.below())) {
            AtlantisDHDEntity dhd = findNearbyDhd(level, queriedPos);
            if (dhd != null)
                return dhd.getEnergyHandler(Direction.DOWN);
        }

        BlockPos cablePos = queriedPos.relative(side);
        BlockEntity cable = level.getBlockEntity(cablePos);
        if (!(cable instanceof CableBlockEntity.LargeNaquadahCable))
            return null;

        Direction virtualSide = findVirtualSide(level, cablePos);
        if (virtualSide == null || !cablePos.relative(virtualSide).equals(queriedPos))
            return null;

        AtlantisDHDEntity dhd = findDhdOnConnectedCablePath(level, cablePos);
        return dhd == null || !isDesignatedOutputCable(level, cablePos, dhd)
            ? null
            : dhd.getEnergyHandler(Direction.DOWN);
    }

    public static Direction findVirtualSide(Level level, BlockPos cablePos) {
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = cablePos.relative(direction);
            if (level.getBlockEntity(adjacentPos) == null
                    && !isLargeCable(level, adjacentPos))
                return direction;
        }
        return null;
    }

    public static AtlantisDHDEntity findDhdOnConnectedCablePath(
            Level level, BlockPos cablePos) {
        if (!isLargeCable(level, cablePos))
            return null;

        Queue<BlockPos> pending = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        pending.add(cablePos);

        while (!pending.isEmpty() && visited.size() < MAX_CONNECTED_CABLE_BLOCKS) {
            BlockPos currentPos = pending.remove();
            if (!visited.add(currentPos) || !isLargeCable(level, currentPos))
                continue;

            BlockPos floorPos = currentPos.above();
            if (!level.getBlockState(floorPos).isAir()
                    && level.getBlockEntity(floorPos) == null) {
                AtlantisDHDEntity dhd = findNearbyDhd(level, floorPos);
                if (dhd != null)
                    return dhd;
            }

            for (Direction direction : Direction.values()) {
                BlockPos connectedPos = currentPos.relative(direction);
                if (!visited.contains(connectedPos))
                    pending.add(connectedPos);
            }
        }
        return null;
    }

    public static boolean isDesignatedOutputCable(
            Level level, BlockPos cablePos, AtlantisDHDEntity dhd) {
        BlockPos floorPos = findFloorAboveNearbyCable(level, dhd.getBlockPos());
        if (floorPos == null)
            return false;

        Queue<BlockPos> pending = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        pending.add(floorPos.below());

        while (!pending.isEmpty() && visited.size() < MAX_CONNECTED_CABLE_BLOCKS) {
            BlockPos currentPos = pending.remove();
            if (!visited.add(currentPos) || !isLargeCable(level, currentPos))
                continue;

            if (level.getBlockEntity(currentPos)
                    instanceof CableBlockEntity.LargeNaquadahCable)
                return currentPos.equals(cablePos);

            for (Direction direction : Direction.values()) {
                BlockPos connectedPos = currentPos.relative(direction);
                if (!visited.contains(connectedPos))
                    pending.add(connectedPos);
            }
        }
        return false;
    }

    public static BlockPos findFloorAboveNearbyCable(Level level, BlockPos dhdPos) {
        for (int distance = 0; distance <= SEARCH_RADIUS; ++distance) {
            for (int dy = 1; dy <= 8; ++dy) {
                for (int dx = -distance; dx <= distance; ++dx) {
                    for (int dz = -distance; dz <= distance; ++dz) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != distance)
                            continue;

                        BlockPos cablePos = dhdPos.offset(dx, -dy, dz);
                        if (level.getBlockState(cablePos).getBlock()
                            instanceof CableBlock.LargeNaquadahCable)
                            return cablePos.above();
                    }
                }
            }
        }
        return null;
    }

    private static AtlantisDHDEntity findNearbyDhd(Level level, BlockPos floorPos) {
        for (int distance = 0; distance <= SEARCH_RADIUS; ++distance) {
            for (int dy = 0; dy <= MAX_DHD_HEIGHT_ABOVE_FLOOR; ++dy) {
                for (int dx = -distance; dx <= distance; ++dx) {
                    for (int dz = -distance; dz <= distance; ++dz) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != distance)
                            continue;

                        BlockEntity candidate = level.getBlockEntity(
                                floorPos.offset(dx, dy, dz));
                        if (candidate instanceof AtlantisDHDEntity dhd)
                            return dhd;
                    }
                }
            }
        }
        return null;
    }

    private static boolean isLargeCable(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock()
                instanceof CableBlock.LargeNaquadahCable;
    }
}
