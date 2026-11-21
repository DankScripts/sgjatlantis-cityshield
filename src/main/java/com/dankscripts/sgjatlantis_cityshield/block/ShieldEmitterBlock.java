package com.dankscripts.sgjatlantis_cityshield.block;

import com.dankscripts.sgjatlantis_cityshield.shield.ShieldDefinition;
import com.dankscripts.sgjatlantis_cityshield.shield.ShieldSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class ShieldEmitterBlock extends BaseEntityBlock {
    // Rounded, stepped envelope of the baked emitter, wholly inside its block.
    private static final VoxelShape SHAPE = Shapes.or(
            roundSection(7.6, 0.0, 2.5),
            roundSection(5.9, 2.5, 7.5),
            roundSection(4.3, 7.5, 10.0),
            roundSection(3.0, 10.0, 13.5),
            roundSection(1.8, 13.5, 16.0)).optimize();

    private static VoxelShape roundSection(double radius, double bottom, double top) {
        VoxelShape result = Shapes.empty();
        for (int strip = 0; strip < 12; strip++) {
            double left = -radius + 2.0 * radius * strip / 12.0;
            double right = -radius + 2.0 * radius * (strip + 1) / 12.0;
            double midpoint = (left + right) * 0.5;
            double reach = Math.sqrt(radius * radius - midpoint * midpoint);
            result = Shapes.or(result, box(8.0 + left, bottom, 8.0 - reach,
                    8.0 + right, top, 8.0 + reach));
        }
        return result;
    }

    public ShieldEmitterBlock(Properties properties) { super(properties); }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShieldEmitterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type,
                com.dankscripts.sgjatlantis_cityshield.registry.ModBlockEntities.SHIELD_EMITTER.get(),
                (serverLevel, pos, blockState, emitter) ->
                        ShieldEmitterBlockEntity.serverTick((ServerLevel) serverLevel, pos, blockState, emitter));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!(level instanceof ServerLevel serverLevel)) return;
        ShieldSavedData data = ShieldSavedData.get(serverLevel.getServer());
        data.registerEmitter(serverLevel, pos);
        ShieldDefinition linked = data.findByEmitter(serverLevel, pos);
        if (linked == null) linked = data.linkEmitterToNearestController(serverLevel, pos);
        if (placer instanceof Player player) {
            player.displayClientMessage(linked == null
                    ? net.minecraft.network.chat.Component.translatable(
                            "message.sgjatlantis_cityshield.emitter_unlinked")
                    : net.minecraft.network.chat.Component.translatable(
                            "message.sgjatlantis_cityshield.emitter_linked",
                            linked.controllerPos().getX(), linked.controllerPos().getY(),
                            linked.controllerPos().getZ()), true);
        }
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!oldState.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            if (level.getBlockEntity(pos) instanceof ShieldEmitterBlockEntity emitter) emitter.releaseChunkTicket();
            ShieldSavedData.get(serverLevel.getServer()).unregisterEmitter(
                    serverLevel, pos, serverLevel.getGameTime());
        }
        super.onRemove(oldState, level, pos, newState, moving);
    }
}
