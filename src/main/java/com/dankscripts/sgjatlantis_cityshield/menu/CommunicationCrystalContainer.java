package com.dankscripts.sgjatlantis_cityshield.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/** A live view of the controller's saved inventory, never a second copy. */
public final class CommunicationCrystalContainer implements Container {
    private final ItemStackHandler inventory;
    private final net.minecraft.world.level.block.entity.BlockEntity owner;

    public CommunicationCrystalContainer(ItemStackHandler inventory, net.minecraft.world.level.block.entity.BlockEntity owner) {
        this.inventory = inventory;
        this.owner = owner;
    }

    @Override public int getContainerSize() { return inventory.getSlots(); }
    @Override public boolean isEmpty() {
        for (int i = 0; i < getContainerSize(); i++) if (!getItem(i).isEmpty()) return false;
        return true;
    }
    @Override public ItemStack getItem(int slot) { return inventory.getStackInSlot(slot); }
    @Override public ItemStack removeItem(int slot, int count) {
        return inventory.extractItem(slot, count, false);
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        return inventory.extractItem(slot, Integer.MAX_VALUE, false);
    }
    @Override public void setItem(int slot, ItemStack stack) { inventory.setStackInSlot(slot, stack); }
    // Mutations above invoke ItemStackHandler.onContentsChanged and mark the BE dirty.
    @Override public void setChanged() { owner.setChanged(); }
    @Override public boolean stillValid(Player player) { return true; }
    @Override public int getMaxStackSize() { return 1; }
    @Override public void clearContent() {
        for (int i = 0; i < getContainerSize(); i++) inventory.setStackInSlot(i, ItemStack.EMPTY);
    }
}
