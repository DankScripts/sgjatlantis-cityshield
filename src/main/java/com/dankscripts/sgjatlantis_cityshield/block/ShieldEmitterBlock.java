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
    public static final com.mojang.serialization.MapCodec<ShieldEmitterBlock> CODEC = simpleCodec(ShieldEmitterBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<ShieldEmitterBlock> codec() { return CODEC; }

    private static final VoxelShape SHAPE = Shapes.or(
            box(1.0, 0.0, 1.0, 15.0, 3.0, 15.0),
            box(3.0, 3.0, 3.0, 13.0, 12.0, 13.0),
            box(1.0, 12.0, 1.0, 15.0, 16.0, 15.0));

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
