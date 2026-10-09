package com.dankscripts.sgjatlantis_cityshield.block;

import com.dankscripts.sgjatlantis_cityshield.AtlantisCityShield;
import com.dankscripts.sgjatlantis_cityshield.menu.ShieldControllerMenu;
import com.dankscripts.sgjatlantis_cityshield.registry.ModBlockEntities;
import com.dankscripts.sgjatlantis_cityshield.shield.ShieldDefinition;
import com.dankscripts.sgjatlantis_cityshield.shield.ShieldMath;
import com.dankscripts.sgjatlantis_cityshield.shield.ShieldSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.Nullable;

public final class ShieldControllerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int DATA_COUNT = 25;
    public static final String PORTABLE_DEFINITION_TAG = "RetainedShieldDefinition";
    public static final int BASE_COMMUNICATION_DISTANCE = 16;
    public static final int COMMUNICATION_CRYSTAL_SLOTS = 8;
    public static final int MAX_COMMUNICATION_DISTANCE =
            BASE_COMMUNICATION_DISTANCE + COMMUNICATION_CRYSTAL_SLOTS * 32;

    private static final ResourceLocation COMMUNICATION_CRYSTAL =
            ResourceLocation.fromNamespaceAndPath("sgjourney", "communication_crystal");
    private static final ResourceLocation ADVANCED_COMMUNICATION_CRYSTAL =
            ResourceLocation.fromNamespaceAndPath("sgjourney", "advanced_communication_crystal");

    private final ItemStackHandler communicationCrystals = new ItemStackHandler(COMMUNICATION_CRYSTAL_SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isCommunicationCrystal(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private ItemStack legacyZpm = ItemStack.EMPTY;
    private boolean controllerChunkForced;
    private boolean chunkTicketStateKnown;
    private CompoundTag retainedShieldDefinition;

    public ShieldControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHIELD_CONTROLLER.get(), pos, state);
    }

    public ShieldDefinition definition() {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return ShieldSavedData.get(serverLevel.getServer()).getOrCreate(serverLevel, worldPosition);
    }

    public ContainerData createDataAccess() {
        return ShieldControllerMenu.dataAccess(definition(), this::hasLargeNaquadahCable,
                this::hasZeroPointPower, this::currentPowerDraw,
                this::connectedZpmEnergy, this::connectedZpmCapacity);
    }

    @Nullable
    public ShieldEmitterBlockEntity linkedEmitter(boolean allowNewLink) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        ShieldDefinition definition = definition();
        if (definition == null) return null;
        return ShieldSavedData.get(serverLevel.getServer())
                .resolveEmitter(serverLevel, definition, this, allowNewLink);
    }

    public boolean hasLargeNaquadahCable() {
        ShieldEmitterBlockEntity emitter = linkedEmitter(true);
        return emitter != null && emitter.hasLargeNaquadahCable();
    }

    public boolean hasZeroPointPower() {
        ShieldEmitterBlockEntity emitter = linkedEmitter(true);
        return emitter != null && emitter.hasZeroPointPower();
    }

    public long currentPowerDraw() {
        ShieldEmitterBlockEntity emitter = linkedEmitter(true);
        return emitter == null ? 0L : emitter.currentPowerDraw();
    }

    public long connectedZpmEnergy() {
        ShieldEmitterBlockEntity emitter = linkedEmitter(true);
        return emitter == null ? 0L : emitter.connectedZpmEnergy();
    }

    public long connectedZpmCapacity() {
        ShieldEmitterBlockEntity emitter = linkedEmitter(true);
        return emitter == null ? 0L : emitter.connectedZpmCapacity();
    }

    public int maxCommunicationDistance() {
        int distance = BASE_COMMUNICATION_DISTANCE;
        for (int slot = 0; slot < communicationCrystals.getSlots(); slot++) {
            distance += communicationRangeIncrease(communicationCrystals.getStackInSlot(slot));
        }
        return distance;
    }

    public net.minecraft.world.Container communicationInventory() {
        return new com.dankscripts.sgjatlantis_cityshield.menu.CommunicationCrystalContainer(communicationCrystals, this);
    }

    public long maxCommunicationDistanceSqr() {
        long distance = maxCommunicationDistance();
        return distance * distance;
    }

    public boolean canCommunicateWith(BlockPos emitterPos) {
        return emitterPos != null && worldPosition.distSqr(emitterPos) <= maxCommunicationDistanceSqr();
    }

    public static boolean isCommunicationCrystal(ItemStack stack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return COMMUNICATION_CRYSTAL.equals(itemId) || ADVANCED_COMMUNICATION_CRYSTAL.equals(itemId);
    }

    public boolean installCommunicationCrystal(ItemStack source) {
        if (!isCommunicationCrystal(source)) return false;
        for (int slot = 0; slot < communicationCrystals.getSlots(); slot++) {
            if (!communicationCrystals.getStackInSlot(slot).isEmpty()) continue;
            ItemStack installed = source.copy();
            installed.setCount(1);
            communicationCrystals.setStackInSlot(slot, installed);
            return true;
        }
        return false;
    }

    public ItemStack removeLastCommunicationCrystal() {
        for (int slot = communicationCrystals.getSlots() - 1; slot >= 0; slot--) {
            if (!communicationCrystals.getStackInSlot(slot).isEmpty()) {
                return communicationCrystals.extractItem(slot, 1, false);
            }
        }
        return ItemStack.EMPTY;
    }

    public void dropCommunicationCrystals() {
        if (level == null) return;
        for (int slot = 0; slot < communicationCrystals.getSlots(); slot++) {
            ItemStack stack = communicationCrystals.extractItem(slot,
                    communicationCrystals.getStackInSlot(slot).getCount(), false);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX() + 0.5,
                        worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, stack);
            }
        }
    }

    public void setShieldActive(boolean active, long gameTime) {
        ShieldDefinition definition = definition();
        if (definition == null) return;
        ShieldEmitterBlockEntity emitter = linkedEmitter(active);
        if (active && emitter == null) return;
        definition.setActive(active, gameTime);
        setVisualState(active
                ? ShieldControllerBlock.VisualState.FORMING
                : ShieldControllerBlock.VisualState.STANDBY);
        forceControllerChunk(active);
        if (emitter != null) emitter.setShieldActive(active);
        if (level instanceof ServerLevel serverLevel) {
            ShieldSavedData.get(serverLevel.getServer()).changed();
        }
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state,
                                  ShieldControllerBlockEntity controller) {
        controller.ejectLegacyZpm(level);
        ShieldDefinition definition = controller.definition();
        if (definition == null || !definition.active()) {
            controller.setVisualState(controller.hasZeroPointPower()
                    ? ShieldControllerBlock.VisualState.WARNING
                    : ShieldControllerBlock.VisualState.STANDBY);
            controller.forceControllerChunk(false);
            return;
        }

        ShieldEmitterBlockEntity emitter = controller.linkedEmitter(false);
        if (emitter == null) {
            controller.setShieldActive(false, level.getGameTime());
            return;
        }

        controller.forceControllerChunk(true);
        boolean connectedPower = emitter.hasZeroPointPower();
        controller.setVisualState(!connectedPower
                ? ShieldControllerBlock.VisualState.STANDBY
                : ShieldMath.materializationCoverage(definition, level.getGameTime()) < 0.999
                    ? ShieldControllerBlock.VisualState.FORMING
                    : emitter.isRecentlyPowered()
                        ? ShieldControllerBlock.VisualState.ACTIVE
                        : ShieldControllerBlock.VisualState.STANDBY);
    }

    public static int communicationRangeIncrease(ItemStack stack) {
        if (stack.isEmpty() || hasProgrammedFrequency(stack)) return 0;
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (ADVANCED_COMMUNICATION_CRYSTAL.equals(itemId)) return 32;
        return COMMUNICATION_CRYSTAL.equals(itemId) ? 16 : 0;
    }

    private static boolean hasProgrammedFrequency(ItemStack stack) {
        return net.povstalec.sgjourney.common.items.crystals.CommunicationCrystalItem.hasFrequency(stack);
    }

    private void setVisualState(ShieldControllerBlock.VisualState visualState) {
        if (level == null) return;
        BlockState state = getBlockState();
        if (!state.hasProperty(ShieldControllerBlock.VISUAL_STATE)
                || state.getValue(ShieldControllerBlock.VISUAL_STATE) == visualState) return;
        level.setBlock(worldPosition, state.setValue(ShieldControllerBlock.VISUAL_STATE, visualState),
                net.minecraft.world.level.block.Block.UPDATE_ALL);
    }

    public ItemStack removeLegacyZpm() {
        ItemStack result = legacyZpm;
        legacyZpm = ItemStack.EMPTY;
        setChanged();
        return result;
    }

    public CompoundTag capturePortableDefinition() {
        if (retainedShieldDefinition != null && !retainedShieldDefinition.isEmpty()) return retainedShieldDefinition.copy();
        if (!(level instanceof ServerLevel serverLevel)) return new CompoundTag();
        ShieldDefinition definition = ShieldSavedData.get(serverLevel.getServer()).find(serverLevel, worldPosition);
        if (definition == null) return new CompoundTag();
        retainedShieldDefinition = definition.save();
        retainedShieldDefinition.putBoolean("Active", false);
        return retainedShieldDefinition.copy();
    }

    public boolean writePortableData(ItemStack stack) {
        CompoundTag retained = capturePortableDefinition();
        if (retained.isEmpty()) return false;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(PORTABLE_DEFINITION_TAG, retained));
        return true;
    }

    @Nullable
    public RestoredController restorePortableDefinition(ServerLevel serverLevel, ItemStack stack) {
        CompoundTag itemTag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (itemTag == null || !itemTag.contains(PORTABLE_DEFINITION_TAG, Tag.TAG_COMPOUND)) return null;
        CompoundTag retained = itemTag.getCompound(PORTABLE_DEFINITION_TAG).copy();
        if (!retained.contains("Dimension") || !retained.contains("Controller")) return null;
        String oldDimension = retained.getString("Dimension");
        long oldController = retained.getLong("Controller");
        retained.putBoolean("Active", false);
        ShieldSavedData.get(serverLevel.getServer()).restore(serverLevel, worldPosition, retained,
                serverLevel.getGameTime());
        itemTag.remove(PORTABLE_DEFINITION_TAG);
        if (itemTag.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
        else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(itemTag));
        retainedShieldDefinition = null;
        setChanged();
        return new RestoredController(oldDimension, oldController);
    }

    public record RestoredController(String oldDimension, long oldController) { }

    public void releaseChunkTicket() { forceControllerChunk(false); }

    private void forceControllerChunk(boolean force) {
        if (!(level instanceof ServerLevel serverLevel)
                || chunkTicketStateKnown && controllerChunkForced == force) return;
        AtlantisCityShield.CHUNK_TICKETS.forceChunk(serverLevel, worldPosition,
                worldPosition.getX() >> 4, worldPosition.getZ() >> 4, force, true);
        controllerChunkForced = force;
        chunkTicketStateKnown = true;
    }

    private void ejectLegacyZpm(ServerLevel level) {
        if (legacyZpm.isEmpty()) return;
        Containers.dropItemStack(level, worldPosition.getX() + 0.5,
                worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, legacyZpm);
        legacyZpm = ItemStack.EMPTY;
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.sgjatlantis_cityshield.shield_controller");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ShieldControllerMenu(containerId, inventory, worldPosition, createDataAccess());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("CommunicationCrystals", communicationCrystals.serializeNBT(registries));
        if (!legacyZpm.isEmpty()) tag.put("LegacyZpm", legacyZpm.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("CommunicationCrystals", Tag.TAG_COMPOUND)) {
            communicationCrystals.deserializeNBT(registries, tag.getCompound("CommunicationCrystals"));
        }
        if (tag.contains("LegacyZpm")) legacyZpm = ItemStack.parseOptional(registries, tag.getCompound("LegacyZpm"));
        if (tag.contains("ZpmInventory")) {
            ItemStackHandler oldInventory = new ItemStackHandler(1);
            oldInventory.deserializeNBT(registries, tag.getCompound("ZpmInventory"));
            if (legacyZpm.isEmpty()) legacyZpm = oldInventory.getStackInSlot(0).copy();
        }
    }
}
