package com.mustangdoc.sgjpatch.mixin;

import com.mustangdoc.sgjpatch.power.ConcealedFloorDhdPowerBridge;
import com.mustangdoc.sgjpatch.standalone.AtlantisDHDEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.povstalec.sgjourney.common.block_entities.tech.CableBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CableBlockEntity.class, remap = false)
public abstract class LargeNaquadahCableDhdPowerBridgeMixin {
    @Inject(method = "getConnectedSides", at = @At("RETURN"))
    private void sgjpatch$addVirtualDhdOutput(
            CallbackInfoReturnable<List<Direction>> callback) {
        if (!((Object) this instanceof CableBlockEntity.LargeNaquadahCable))
            return;

        BlockEntity cable = (BlockEntity) (Object) this;
        Level level = cable.getLevel();
        BlockPos cablePos = cable.getBlockPos();
        if (level == null)
            return;

        AtlantisDHDEntity dhd = ConcealedFloorDhdPowerBridge
            .findDhdOnConnectedCablePath(level, cablePos);
        if (dhd == null || !ConcealedFloorDhdPowerBridge.isDesignatedOutputCable(
            level, cablePos, dhd))
            return;

        Direction virtualSide = ConcealedFloorDhdPowerBridge.findVirtualSide(
                level, cablePos);
        List<Direction> connectedSides = callback.getReturnValue();
        if (virtualSide != null && connectedSides != null
                && !connectedSides.contains(virtualSide))
            connectedSides.add(virtualSide);
    }
}