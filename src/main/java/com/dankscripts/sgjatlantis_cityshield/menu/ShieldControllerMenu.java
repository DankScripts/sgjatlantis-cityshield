package com.dankscripts.sgjatlantis_cityshield.menu;

import com.dankscripts.sgjatlantis_cityshield.block.ShieldControllerBlockEntity;
import com.dankscripts.sgjatlantis_cityshield.registry.ModMenus;
import com.dankscripts.sgjatlantis_cityshield.shield.ShieldDefinition;
import com.dankscripts.sgjatlantis_cityshield.shield.ShieldSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;

import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

public final class ShieldControllerMenu extends AbstractContainerMenu {
    private final BlockPos controllerPos;
    private final ContainerData data;
    private final Player viewer;
    private boolean upgradesOpen;
    private static final int UPGRADE_SLOTS = 8;

    public ShieldControllerMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos(),
                new SimpleContainerData(ShieldControllerBlockEntity.DATA_COUNT));
    }

    public ShieldControllerMenu(int containerId, Inventory inventory, BlockPos controllerPos, ContainerData data) {
        super(ModMenus.SHIELD_CONTROLLER.get(), containerId);
        this.controllerPos = controllerPos;
        this.data = data;
        this.viewer = inventory.player;
        this.upgradesOpen = !viewer.hasPermissions(2);
        checkContainerDataCount(data, ShieldControllerBlockEntity.DATA_COUNT);
        addDataSlots(data);
        Container crystals = !viewer.level().isClientSide
                && viewer.level().getBlockEntity(controllerPos) instanceof ShieldControllerBlockEntity controller
                ? controller.communicationInventory() : new SimpleContainer(UPGRADE_SLOTS);
        for (int index = 0; index < UPGRADE_SLOTS; index++) {
            addSlot(new UpgradeSlot(crystals, index, 76 + 20 * index, 102));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new PlayerInventorySlot(inventory, 9 + row * 9 + col,
                        75 + col * 18, 146 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new PlayerInventorySlot(inventory, col, 75 + col * 18, 204));
        }
    }

    public boolean upgradesOpen() { return upgradesOpen; }
    public boolean operatorControls() { return viewer.hasPermissions(2); }
    public boolean canUseUpgrades(Player player) {
        return player.level().getBlockEntity(controllerPos) instanceof ShieldControllerBlockEntity
                && player.distanceToSqr(controllerPos.getX() + 0.5,
                        controllerPos.getY() + 0.5, controllerPos.getZ() + 0.5) <= 64.0;
    }
    public int communicationRange() {
        int range = ShieldControllerBlockEntity.BASE_COMMUNICATION_DISTANCE;
        for (int i = 0; i < UPGRADE_SLOTS; i++) {
            range += ShieldControllerBlockEntity.communicationRangeIncrease(slots.get(i).getItem());
        }
        return range;
    }
    @Override public boolean clickMenuButton(Player player, int id) {
        if (!stillValid(player)) return false;
        if (id == 1 && canUseUpgrades(player)) upgradesOpen = true;
        else if (id == 0 && player.hasPermissions(2)) upgradesOpen = false;
        else return false;
        return true;
    }

    private final class UpgradeSlot extends Slot {
        UpgradeSlot(Container container, int index, int x, int y) { super(container, index, x, y); }
        @Override public boolean isActive() { return upgradesOpen; }
        @Override public boolean mayPlace(ItemStack stack) {
            return upgradesOpen && canUseUpgrades(viewer)
                    && ShieldControllerBlockEntity.communicationRangeIncrease(stack) > 0;
        }
        @Override public boolean mayPickup(Player player) { return upgradesOpen && canUseUpgrades(player); }
        @Override public int getMaxStackSize() { return 1; }
        @Override public int getMaxStackSize(ItemStack stack) { return 1; }
    }

    private final class PlayerInventorySlot extends Slot {
        PlayerInventorySlot(Container container, int index, int x, int y) { super(container, index, x, y); }
        @Override public boolean isActive() { return upgradesOpen; }
        @Override public boolean mayPlace(ItemStack stack) { return upgradesOpen && canUseUpgrades(viewer); }
        @Override public boolean mayPickup(Player player) { return upgradesOpen && canUseUpgrades(player); }
    }

    public BlockPos controllerPos() { return controllerPos; }
    public int field(int field) { return data.get(field); }
    public boolean preview() { return data.get(8) != 0; }
    public boolean dirty() { return data.get(9) != 0; }
    public boolean active() { return data.get(10) != 0; }
    public boolean largeCableConnected() { return data.get(11) != 0; }
    public boolean zeroPointPowerAvailable() { return data.get(12) != 0; }
    public long currentPowerDraw() { return readLong(13); }
    public long connectedZpmEnergy() { return readLong(17); }
    public long connectedZpmCapacity() { return readLong(21); }

    private long readLong(int startIndex) {
        long value = 0L;
        value |= data.get(startIndex) & 0xFFFFL;
        value |= (data.get(startIndex + 1) & 0xFFFFL) << 16;
        value |= (data.get(startIndex + 2) & 0xFFFFL) << 32;
        value |= (data.get(startIndex + 3) & 0xFFFFL) << 48;
        return value;
    }

    public static ContainerData dataAccess(ShieldDefinition definition, BooleanSupplier largeCableConnected,
                                           BooleanSupplier zeroPointPowerAvailable,
                                           LongSupplier currentPowerDraw,
                                           LongSupplier connectedZpmEnergy,
                                           LongSupplier connectedZpmCapacity) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                if (definition == null) return 0;
                if (index < 8) return definition.getField(index);
                if (index == 8) return definition.preview() ? 1 : 0;
                if (index == 9) return definition.dirty() ? 1 : 0;
                if (index == 10) return definition.active() ? 1 : 0;
                if (index == 11) return largeCableConnected.getAsBoolean() ? 1 : 0;
                if (index == 12) return zeroPointPowerAvailable.getAsBoolean() ? 1 : 0;
                if (index >= 13 && index <= 16) {
                    return word(currentPowerDraw.getAsLong(), index - 13);
                }
                if (index >= 17 && index <= 20) {
                    return word(connectedZpmEnergy.getAsLong(), index - 17);
                }
                if (index >= 21 && index <= 24) {
                    return word(connectedZpmCapacity.getAsLong(), index - 21);
                }
                return 0;
            }

            private int word(long value, int wordIndex) {
                return (int) (value >>> (wordIndex * 16)) & 0xFFFF;
            }

            @Override
            public void set(int index, int value) {
                // Server authoritative. Changes arrive through validated packets.
            }

            @Override
            public int getCount() {
                return ShieldControllerBlockEntity.DATA_COUNT;
            }
        };
    }

    @Override
    public boolean stillValid(Player player) {
        if (canUseUpgrades(player)) return true;
        if (!player.hasPermissions(2) || !(player.level() instanceof ServerLevel serverLevel)) return false;
        return ShieldSavedData.get(serverLevel.getServer()).find(serverLevel, controllerPos) != null;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!upgradesOpen || !canUseUpgrades(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack source = slot.getItem();
        ItemStack original = source.copy();
        if (index < UPGRADE_SLOTS) {
            if (!moveItemStackTo(source, UPGRADE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (ShieldControllerBlockEntity.communicationRangeIncrease(source) > 0) {
            if (!moveItemStackTo(source, 0, UPGRADE_SLOTS, false)) return ItemStack.EMPTY;
        } else if (index < UPGRADE_SLOTS + 27) {
            if (!moveItemStackTo(source, UPGRADE_SLOTS + 27, slots.size(), false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(source, UPGRADE_SLOTS, UPGRADE_SLOTS + 27, false)) return ItemStack.EMPTY;
        if (source.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, source);
        return original;
    }
}
