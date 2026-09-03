package com.mustangdoc.sgjpatch.block;

import com.mojang.serialization.MapCodec;
import com.mustangdoc.sgjpatch.init.SGJPatchBlockEntities;
import com.mustangdoc.sgjpatch.init.SGJPatchBlocks;
import com.mustangdoc.sgjpatch.menu.AtlantisDHDCrystalMenu;
import com.mustangdoc.sgjpatch.standalone.AtlantisDHDEntity;
import com.mustangdoc.sgjpatch.standalone.AtlantisDHDMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.povstalec.sgjourney.common.blocks.dhd.CrystalDHDBlock;
import net.povstalec.sgjourney.common.misc.NetworkUtils;

import javax.annotation.Nullable;

public class AtlantisDHDBlock extends CrystalDHDBlock implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final MapCodec<AtlantisDHDBlock> CODEC = simpleCodec(AtlantisDHDBlock::new);

    public AtlantisDHDBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<AtlantisDHDBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> state) {
        state.add(FACING).add(WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        return super.getStateForPlacement(context).setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AtlantisDHDEntity(pos, state);
    }

    @Override
    public void use(Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide())
            return;

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof AtlantisDHDEntity dhd))
            throw new IllegalStateException("Atlantis DHD block entity is missing");

        if (player.isShiftKeyDown() && dhd.hasPermissions(player, true)) {
            MenuProvider provider = new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("screen.sgjourney.dhd");
                }

                @Override
                public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player menuPlayer) {
                    return new AtlantisDHDCrystalMenu(windowId, inventory, dhd);
                }
            };
            NetworkUtils.openMenu((ServerPlayer) player, provider, dhd.getBlockPos());
        } else {
            MenuProvider provider = new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("screen.sgjourney.dhd");
                }

                @Override
                public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player menuPlayer) {
                    return new AtlantisDHDMenu(windowId, inventory, dhd);
                }
            };
            NetworkUtils.openMenu((ServerPlayer) player, provider, dhd.getBlockPos());
        }
    }

    @Override
    public Block getDHD() {
        return SGJPatchBlocks.ATLANTIS_DHD.get();
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, SGJPatchBlockEntities.ATLANTIS_DHD.get(), AtlantisDHDEntity::tick);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> Block.box(0, 0, 0, 32, 20, 16);
            case EAST -> Block.box(0, 0, 0, 16, 20, 32);
            case WEST -> Block.box(0, 0, -16, 16, 20, 16);
            case SOUTH -> Block.box(-16, 0, 0, 16, 20, 16);
            default -> Block.box(0, 0, 0, 16, 20, 16);
        };
    }
}
