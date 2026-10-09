package com.dankscripts.sgjatlantis_cityshield.item;

import com.dankscripts.sgjatlantis_cityshield.block.ShieldControllerBlockEntity;
import com.dankscripts.sgjatlantis_cityshield.menu.ShieldControllerMenu;
import com.dankscripts.sgjatlantis_cityshield.shield.ShieldDefinition;
import com.dankscripts.sgjatlantis_cityshield.shield.ShieldSavedData;
import com.dankscripts.sgjatlantis_cityshield.shield.SurveyMark;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ShieldSurveyorItem extends Item {
    private static final String TAG_DIMENSION = "ShieldDimension";
    private static final String TAG_CONTROLLER = "ShieldController";
    private static final String TAG_MODE = "SurveyMode";

    public ShieldSurveyorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        if (!serverPlayer.hasPermissions(2)) {
            serverPlayer.displayClientMessage(Component.translatable("message.sgjatlantis_cityshield.operator_only"), true);
            return InteractionResult.CONSUME;
        }

        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        if (serverPlayer.isShiftKeyDown() && level.getBlockEntity(clicked) instanceof ShieldControllerBlockEntity controller) {
            bind(stack, serverLevel, clicked);
            controller.definition();
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.sgjatlantis_cityshield.bound", clicked.getX(), clicked.getY(), clicked.getZ()), false);
            return InteractionResult.CONSUME;
        }

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_CONTROLLER) || !tag.contains(TAG_DIMENSION)) {
            serverPlayer.displayClientMessage(Component.translatable("message.sgjatlantis_cityshield.not_bound"), true);
            return InteractionResult.CONSUME;
        }
        String currentDimension = serverLevel.dimension().location().toString();
        if (!currentDimension.equals(tag.getString(TAG_DIMENSION))) {
            serverPlayer.displayClientMessage(Component.translatable("message.sgjatlantis_cityshield.wrong_dimension"), true);
            return InteractionResult.CONSUME;
        }

        ShieldSavedData data = ShieldSavedData.get(serverLevel.getServer());
        ShieldDefinition definition = data.find(tag.getString(TAG_DIMENSION), tag.getLong(TAG_CONTROLLER));
        if (definition == null) {
            serverPlayer.displayClientMessage(Component.translatable("message.sgjatlantis_cityshield.missing_controller"), true);
            return InteractionResult.CONSUME;
        }

        SurveyMark mark = mode(stack);
        definition.applyMark(mark, clicked);
        definition.setPreview(true);
        data.changed();
        serverPlayer.displayClientMessage(Component.translatable("message.sgjatlantis_cityshield.marked",
                mark.label(), clicked.getX(), clicked.getY(), clicked.getZ()), false);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                SurveyMark next = mode(stack).next();
                stack.getOrCreateTag().putInt(TAG_MODE, next.ordinal());
                player.displayClientMessage(Component.translatable("message.sgjatlantis_cityshield.mode", next.label()), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (!serverPlayer.hasPermissions(2)) {
                serverPlayer.displayClientMessage(Component.translatable("message.sgjatlantis_cityshield.operator_only"), true);
                return InteractionResultHolder.consume(stack);
            }
            CompoundTag tag = stack.getTag();
            if (tag == null || !tag.contains(TAG_CONTROLLER) || !tag.contains(TAG_DIMENSION)) {
                serverPlayer.displayClientMessage(Component.translatable("message.sgjatlantis_cityshield.not_bound"), true);
                return InteractionResultHolder.consume(stack);
            }
            if (!level.dimension().location().toString().equals(tag.getString(TAG_DIMENSION))) {
                serverPlayer.displayClientMessage(Component.translatable("message.sgjatlantis_cityshield.wrong_dimension"), true);
                return InteractionResultHolder.consume(stack);
            }
            BlockPos controllerPos = BlockPos.of(tag.getLong(TAG_CONTROLLER));
            ShieldDefinition definition = ShieldSavedData.get(serverPlayer.server).find(level, controllerPos);
            if (definition == null) {
                serverPlayer.displayClientMessage(Component.translatable("message.sgjatlantis_cityshield.missing_controller"), true);
                return InteractionResultHolder.consume(stack);
            }
            if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
                return InteractionResultHolder.consume(stack);
            }
            serverLevel.getChunkAt(controllerPos);
            if (!(serverLevel.getBlockEntity(controllerPos) instanceof ShieldControllerBlockEntity controller)) {
                serverPlayer.displayClientMessage(Component.translatable("message.sgjatlantis_cityshield.missing_controller"), true);
                return InteractionResultHolder.consume(stack);
            }
            NetworkHooks.openScreen(serverPlayer,
                    new SimpleMenuProvider((containerId, inventory, menuPlayer) ->
                            new ShieldControllerMenu(containerId, inventory, controllerPos,
                                    controller.createDataAccess()),
                            Component.translatable("container.sgjatlantis_cityshield.shield_controller")),
                    buffer -> buffer.writeBlockPos(controllerPos));
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(TAG_CONTROLLER);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.sgjatlantis_cityshield.surveyor.mode", mode(stack).label()));
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_CONTROLLER)) {
            BlockPos pos = BlockPos.of(tag.getLong(TAG_CONTROLLER));
            tooltip.add(Component.translatable("tooltip.sgjatlantis_cityshield.surveyor.bound",
                    pos.getX(), pos.getY(), pos.getZ()));
        } else {
            tooltip.add(Component.translatable("tooltip.sgjatlantis_cityshield.surveyor.unbound"));
        }

        tooltip.add(Component.translatable("tooltip.sgjatlantis_cityshield.surveyor.help"));
        tooltip.add(Component.translatable("tooltip.sgjatlantis_cityshield.surveyor.guide_title"));
        tooltip.add(Component.translatable("tooltip.sgjatlantis_cityshield.surveyor.guide_1"));
        tooltip.add(Component.translatable("tooltip.sgjatlantis_cityshield.surveyor.guide_2"));
        tooltip.add(Component.translatable("tooltip.sgjatlantis_cityshield.surveyor.guide_3"));
        tooltip.add(Component.translatable("tooltip.sgjatlantis_cityshield.surveyor.guide_4"));
        tooltip.add(Component.translatable("tooltip.sgjatlantis_cityshield.surveyor.guide_order"));
        tooltip.add(Component.translatable("tooltip.sgjatlantis_cityshield.surveyor.guide_order_2"));
        tooltip.add(Component.translatable("tooltip.sgjatlantis_cityshield.surveyor.guide_center"));
        tooltip.add(Component.translatable("tooltip.sgjatlantis_cityshield.surveyor.guide_center_warning"));
        tooltip.add(Component.translatable("tooltip.sgjatlantis_cityshield.surveyor.guide_pairs"));
        tooltip.add(Component.translatable("tooltip.sgjatlantis_cityshield.surveyor.guide_5"));
        tooltip.add(Component.translatable("tooltip.sgjatlantis_cityshield.surveyor.guide_6"));
    }

    private static void bind(ItemStack stack, ServerLevel level, BlockPos pos) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_DIMENSION, level.dimension().location().toString());
        tag.putLong(TAG_CONTROLLER, pos.asLong());
    }

    public static int rebindMatching(Player player, String oldDimension, long oldController,
                                     ServerLevel newLevel, BlockPos newController) {
        int rebound = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!(stack.getItem() instanceof ShieldSurveyorItem)) continue;
            CompoundTag tag = stack.getTag();
            if (tag == null
                    || !oldDimension.equals(tag.getString(TAG_DIMENSION))
                    || oldController != tag.getLong(TAG_CONTROLLER)) continue;
            bind(stack, newLevel, newController);
            rebound++;
        }
        return rebound;
    }

    private static SurveyMark mode(ItemStack stack) {
        int ordinal = stack.getOrCreateTag().getInt(TAG_MODE);
        SurveyMark[] values = SurveyMark.values();
        return values[Math.max(0, Math.min(values.length - 1, ordinal))];
    }
}
