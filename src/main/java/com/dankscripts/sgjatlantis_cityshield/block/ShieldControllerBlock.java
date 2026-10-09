package com.dankscripts.sgjatlantis_cityshield.block;

import com.dankscripts.sgjatlantis_cityshield.item.ShieldSurveyorItem;
import com.dankscripts.sgjatlantis_cityshield.registry.ModItems;
import com.dankscripts.sgjatlantis_cityshield.shield.ShieldDefinition;
import com.dankscripts.sgjatlantis_cityshield.shield.ShieldSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.Item;
import net.minecraft.world.ItemInteractionResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ShieldControllerBlock extends BaseEntityBlock {
    public static final MapCodec<ShieldControllerBlock> CODEC = simpleCodec(ShieldControllerBlock::new);

    @Override
    protected MapCodec<ShieldControllerBlock> codec() { return CODEC; }

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<VisualState> VISUAL_STATE =
            EnumProperty.create("visual_state", VisualState.class);
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            box(-8.0, 13.45, 0.0, 24.0, 20.0, 16.25),
            box(-5.2, 0.0, 1.2, -2.2, 14.7, 16.25),
            box(18.2, 0.0, 1.2, 21.2, 14.7, 16.25),
            box(-2.1, 6.35, 6.5, 18.1, 8.6, 9.75));
    private static final VoxelShape EAST_SHAPE = rotateShape(NORTH_SHAPE, 1);
    private static final VoxelShape SOUTH_SHAPE = rotateShape(NORTH_SHAPE, 2);
    private static final VoxelShape WEST_SHAPE = rotateShape(NORTH_SHAPE, 3);

    public ShieldControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(VISUAL_STATE, VisualState.STANDBY));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, VISUAL_STATE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // One clean outer selection outline; keep the detailed physical collision below.
        return Shapes.create(getCollisionShape(state, level, pos, context).bounds());
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case EAST -> EAST_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        // The selection cuboid contains open air; it must not hide neighboring block faces.
        return Shapes.empty();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShieldControllerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, com.dankscripts.sgjatlantis_cityshield.registry.ModBlockEntities.SHIELD_CONTROLLER.get(),
                (serverLevel, pos, blockState, controller) ->
                        ShieldControllerBlockEntity.serverTick((ServerLevel) serverLevel, pos, blockState, controller));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel serverLevel) {
            if (level.getBlockEntity(pos) instanceof ShieldControllerBlockEntity controller) {
                ShieldControllerBlockEntity.RestoredController restored =
                        controller.restorePortableDefinition(serverLevel, stack);
                if (restored != null) {
                    int rebound = placer instanceof Player player
                            ? ShieldSurveyorItem.rebindMatching(player, restored.oldDimension(),
                                    restored.oldController(), serverLevel, pos)
                            : 0;
                    if (placer instanceof Player player) {
                        Component message = rebound > 0
                                ? Component.translatable(
                                        "message.sgjatlantis_cityshield.controller_restored_rebound", rebound)
                                : Component.translatable("message.sgjatlantis_cityshield.controller_restored");
                        player.displayClientMessage(message, false);
                    }
                    return;
                }
            }
            ShieldSavedData data = ShieldSavedData.get(serverLevel.getServer());
            data.getOrCreate(serverLevel, pos).setPreview(true);
            data.changed();
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof ShieldControllerBlockEntity controller) {
            for (ItemStack drop : drops) {
                if (drop.is(ModItems.SHIELD_CONTROLLER_ITEM.get())) controller.writePortableData(drop);
            }
        }
        return drops;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.getAbilities().instabuild
                && level.getBlockEntity(pos) instanceof ShieldControllerBlockEntity controller) {
            ItemStack portableController = new ItemStack(ModItems.SHIELD_CONTROLLER_ITEM.get());
            if (controller.writePortableData(portableController)) {
                popResource(level, pos, portableController);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        CompoundTag itemTag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (itemTag != null && itemTag.contains(
                ShieldControllerBlockEntity.PORTABLE_DEFINITION_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag retained = itemTag.getCompound(ShieldControllerBlockEntity.PORTABLE_DEFINITION_TAG);
            tooltip.add(Component.translatable("tooltip.sgjatlantis_cityshield.controller.retained"));
            tooltip.add(Component.translatable("tooltip.sgjatlantis_cityshield.controller.geometry",
                    retained.getInt("RadiusX"), retained.getInt("RadiusZ"),
                    retained.getInt("UpperHeight"), retained.getInt("LowerDepth")));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return interact(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof ShieldSurveyorItem) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        interact(state, level, pos, player, hand, hit);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.sgjatlantis_cityshield.operator_only"), true);
            return InteractionResult.CONSUME;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ShieldControllerBlockEntity controller) {
            ItemStack held = player.getItemInHand(hand);
            if (ShieldControllerBlockEntity.isCommunicationCrystal(held)) {
                if (controller.installCommunicationCrystal(held)) {
                    if (!player.getAbilities().instabuild) held.shrink(1);
                    player.displayClientMessage(Component.translatable(
                            "message.sgjatlantis_cityshield.communication_crystal_installed",
                            controller.maxCommunicationDistance()), true);
                } else {
                    player.displayClientMessage(Component.translatable(
                            "message.sgjatlantis_cityshield.communication_crystals_full"), true);
                }
                return InteractionResult.CONSUME;
            }
            if (player.isShiftKeyDown() && held.isEmpty()) {
                ItemStack removed = controller.removeLastCommunicationCrystal();
                if (removed.isEmpty()) {
                    player.displayClientMessage(Component.translatable(
                            "message.sgjatlantis_cityshield.no_communication_crystals"), true);
                } else {
                    if (!player.getInventory().add(removed)) popResource(level, pos, removed);
                    ShieldDefinition definition = controller.definition();
                    if (definition != null && definition.active() && definition.hasEmitter()
                            && !controller.canCommunicateWith(definition.emitterPos())) {
                        controller.setShieldActive(false, level.getGameTime());
                    }
                    player.displayClientMessage(Component.translatable(
                            "message.sgjatlantis_cityshield.communication_crystal_removed",
                            controller.maxCommunicationDistance()), true);
                }
                return InteractionResult.CONSUME;
            }
        }
        if (blockEntity instanceof MenuProvider provider) {
            serverPlayer.openMenu(provider, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!oldState.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            if (level.getBlockEntity(pos) instanceof ShieldControllerBlockEntity controller) {
                controller.capturePortableDefinition();
                controller.releaseChunkTicket();
                ItemStack zpm = controller.removeLegacyZpm();
                if (!zpm.isEmpty()) {
                    net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), zpm);
                }
                controller.dropCommunicationCrystals();
            }
            ShieldSavedData.get(serverLevel.getServer()).remove(level, pos);
        }
        super.onRemove(oldState, level, pos, newState, moving);
    }

    private static VoxelShape rotateShape(VoxelShape source, int quarterTurns) {
        VoxelShape shape = source;
        for (int turn = 0; turn < quarterTurns; turn++) {
            VoxelShape rotated = Shapes.empty();
            final VoxelShape[] result = {rotated};
            shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                    result[0] = Shapes.or(result[0],
                            Shapes.box(1.0 - maxZ, minY, minX, 1.0 - minZ, maxY, maxX)));
            shape = result[0];
        }
        return shape;
    }

    public enum VisualState implements StringRepresentable {
        STANDBY("standby"),
        FORMING("forming"),
        ACTIVE("active"),
        WARNING("warning");

        private final String serializedName;

        VisualState(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}
