package com.turtlemod.screen;

import com.turtlemod.TurtleMod;
import com.turtlemod.block.entity.TurtleBlockEntity;
import com.turtlemod.item.FloppyDiskItem;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class TurtleScreenHandler extends AbstractContainerMenu {
    public static final int FUEL_SLOT = 0;
    public static final int DISK_SLOT = 1;
    public static final int TOOL_SLOT = 2;
    public static final int STORAGE_START = 3;
    public static final int STORAGE_COUNT = 9;
    public static final int TOTAL_TURTLE_SLOTS = 12;

    private final Container inventory;
    private final ContainerData propertyDelegate;
    private TurtleBlockEntity turtle;

    public TurtleScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(TOTAL_TURTLE_SLOTS), new SimpleContainerData(7));
    }

    public TurtleScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
        super(TurtleMod.TURTLE_SCREEN_HANDLER, syncId);
        checkContainerSize(inventory, TOTAL_TURTLE_SLOTS);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        if (inventory instanceof TurtleBlockEntity tbe) {
            this.turtle = tbe;
        } else if (inventory instanceof DelegatingInventory delegating) {
            this.turtle = delegating.getDelegate();
        }
        inventory.startOpen(playerInventory.player);

        // Slot 0: Fuel Slot
        this.addSlot(new Slot(inventory, FUEL_SLOT, 18, 62));

        // Slot 1: Disk Slot
        this.addSlot(new Slot(inventory, DISK_SLOT, 18, 18) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof FloppyDiskItem;
            }
        });

        // Slot 2: Tool Slot
        this.addSlot(new Slot(inventory, TOOL_SLOT, 18, 40));

        // Slots 3-11: 3x3 Turtle Storage
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 3; ++col) {
                int index = STORAGE_START + col + row * 3;
                this.addSlot(new Slot(inventory, index, 112 + col * 18, 18 + row * 18));
            }
        }

        // Player Inventory (3 rows x 9 columns)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player Hotbar (1 row x 9 columns)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        this.addDataSlots(propertyDelegate);
    }

    public int getFuel() {
        return this.propertyDelegate.get(0);
    }

    public int getMaxFuel() {
        return Math.max(1, this.propertyDelegate.get(1));
    }

    public int getStatusId() {
        return this.propertyDelegate.get(2);
    }

    public int getAppTypeId() {
        return this.propertyDelegate.get(3);
    }

    public int getFacingId() {
        return this.propertyDelegate.get(4);
    }

    public int getProgress() {
        return this.propertyDelegate.get(5);
    }

    public int getConfigDistance() {
        return this.propertyDelegate.get(6);
    }

    public void setTurtle(TurtleBlockEntity newTurtle) {
        this.turtle = newTurtle;
        if (this.inventory instanceof DelegatingInventory delegating) {
            delegating.setDelegate(newTurtle);
        }
        if (this.propertyDelegate instanceof DelegatingPropertyDelegate delegatingProps) {
            delegatingProps.setDelegate(newTurtle.getPropertyDelegate());
        }
        this.broadcastChanges();
    }

    public TurtleBlockEntity getTurtle() {
        return this.turtle;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.turtle != null) {
            this.turtle.onButtonPressed(id);
            return true;
        }
        if (this.inventory instanceof TurtleBlockEntity tbe) {
            tbe.onButtonPressed(id);
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.turtle != null) {
            if (player.level() != this.turtle.getLevel()) return false;
            return player.distanceToSqr(Vec3.atCenterOf(this.turtle.getBlockPos())) <= 100.0;
        }
        return this.inventory.stillValid(player);
    }

    public static class DelegatingInventory implements Container {
        private TurtleBlockEntity delegate;

        public DelegatingInventory(TurtleBlockEntity delegate) {
            this.delegate = delegate;
        }

        public void setDelegate(TurtleBlockEntity delegate) {
            this.delegate = delegate;
        }

        public TurtleBlockEntity getDelegate() {
            return delegate;
        }

        @Override
        public int getContainerSize() {
            return delegate != null ? delegate.getContainerSize() : TOTAL_TURTLE_SLOTS;
        }

        @Override
        public boolean isEmpty() {
            return delegate == null || delegate.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return delegate != null ? delegate.getItem(slot) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return delegate != null ? delegate.removeItem(slot, amount) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return delegate != null ? delegate.removeItemNoUpdate(slot) : ItemStack.EMPTY;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (delegate != null) {
                delegate.setItem(slot, stack);
            }
        }

        @Override
        public void setChanged() {
            if (delegate != null) {
                delegate.setChanged();
            }
        }

        @Override
        public boolean stillValid(Player player) {
            if (delegate == null || delegate.isRemoved()) return false;
            return player.distanceToSqr(Vec3.atCenterOf(delegate.getBlockPos())) <= 100.0;
        }

        @Override
        public void clearContent() {
            if (delegate != null) {
                delegate.clearContent();
            }
        }
    }

    public static class DelegatingPropertyDelegate implements ContainerData {
        private ContainerData delegate;

        public DelegatingPropertyDelegate(ContainerData delegate) {
            this.delegate = delegate;
        }

        public void setDelegate(ContainerData delegate) {
            this.delegate = delegate;
        }

        @Override
        public int get(int index) {
            return delegate != null ? delegate.get(index) : 0;
        }

        @Override
        public void set(int index, int value) {
            if (delegate != null) {
                delegate.set(index, value);
            }
        }

        @Override
        public int getCount() {
            return delegate != null ? delegate.getCount() : 7;
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            if (invSlot < TOTAL_TURTLE_SLOTS) {
                // Moving from Turtle to Player
                if (!this.moveItemStackTo(originalStack, TOTAL_TURTLE_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from Player to Turtle
                if (originalStack.getItem() instanceof FloppyDiskItem) {
                    if (!this.moveItemStackTo(originalStack, DISK_SLOT, DISK_SLOT + 1, false)) {
                        if (!this.moveItemStackTo(originalStack, STORAGE_START, STORAGE_START + STORAGE_COUNT, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                } else if (!this.moveItemStackTo(originalStack, STORAGE_START, STORAGE_START + STORAGE_COUNT, false)) {
                    if (!this.moveItemStackTo(originalStack, FUEL_SLOT, FUEL_SLOT + 1, false)) {
                        if (!this.moveItemStackTo(originalStack, TOOL_SLOT, TOOL_SLOT + 1, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }

            if (originalStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return newStack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.inventory.stopOpen(player);
    }
}
