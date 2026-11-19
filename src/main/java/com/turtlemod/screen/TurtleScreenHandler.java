package com.turtlemod.screen;

import com.turtlemod.TurtleMod;
import com.turtlemod.block.entity.TurtleBlockEntity;
import com.turtlemod.item.FloppyDiskItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class TurtleScreenHandler extends ScreenHandler {
    public static final int FUEL_SLOT = 0;
    public static final int DISK_SLOT = 1;
    public static final int TOOL_SLOT = 2;
    public static final int STORAGE_START = 3;
    public static final int STORAGE_COUNT = 9;
    public static final int TOTAL_TURTLE_SLOTS = 12;

    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;
    private TurtleBlockEntity turtle;

    public TurtleScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(TOTAL_TURTLE_SLOTS), new ArrayPropertyDelegate(7));
    }

    public TurtleScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(TurtleMod.TURTLE_SCREEN_HANDLER, syncId);
        checkSize(inventory, TOTAL_TURTLE_SLOTS);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        if (inventory instanceof TurtleBlockEntity tbe) {
            this.turtle = tbe;
        } else if (inventory instanceof DelegatingInventory delegating) {
            this.turtle = delegating.getDelegate();
        }
        inventory.onOpen(playerInventory.player);

        // Slot 0: Fuel Slot
        this.addSlot(new Slot(inventory, FUEL_SLOT, 18, 62));

        // Slot 1: Disk Slot
        this.addSlot(new Slot(inventory, DISK_SLOT, 18, 18) {
            @Override
            public boolean canInsert(ItemStack stack) {
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

        this.addProperties(propertyDelegate);
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
        this.sendContentUpdates();
    }

    public TurtleBlockEntity getTurtle() {
        return this.turtle;
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
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
    public boolean canUse(PlayerEntity player) {
        if (this.turtle != null) {
            if (player.getEntityWorld() != this.turtle.getWorld()) return false;
            return player.squaredDistanceTo(this.turtle.getPos().toCenterPos()) <= 100.0;
        }
        return this.inventory.canPlayerUse(player);
    }

    public static class DelegatingInventory implements Inventory {
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
        public int size() {
            return delegate != null ? delegate.size() : TOTAL_TURTLE_SLOTS;
        }

        @Override
        public boolean isEmpty() {
            return delegate == null || delegate.isEmpty();
        }

        @Override
        public ItemStack getStack(int slot) {
            return delegate != null ? delegate.getStack(slot) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeStack(int slot, int amount) {
            return delegate != null ? delegate.removeStack(slot, amount) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeStack(int slot) {
            return delegate != null ? delegate.removeStack(slot) : ItemStack.EMPTY;
        }

        @Override
        public void setStack(int slot, ItemStack stack) {
            if (delegate != null) {
                delegate.setStack(slot, stack);
            }
        }

        @Override
        public void markDirty() {
            if (delegate != null) {
                delegate.markDirty();
            }
        }

        @Override
        public boolean canPlayerUse(PlayerEntity player) {
            if (delegate == null || delegate.isRemoved()) return false;
            return player.squaredDistanceTo(delegate.getPos().toCenterPos()) <= 100.0;
        }

        @Override
        public void clear() {
            if (delegate != null) {
                delegate.clear();
            }
        }
    }

    public static class DelegatingPropertyDelegate implements PropertyDelegate {
        private PropertyDelegate delegate;

        public DelegatingPropertyDelegate(PropertyDelegate delegate) {
            this.delegate = delegate;
        }

        public void setDelegate(PropertyDelegate delegate) {
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
        public int size() {
            return delegate != null ? delegate.size() : 7;
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            if (invSlot < TOTAL_TURTLE_SLOTS) {
                // Moving from Turtle to Player
                if (!this.insertItem(originalStack, TOTAL_TURTLE_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from Player to Turtle
                if (originalStack.getItem() instanceof FloppyDiskItem) {
                    if (!this.insertItem(originalStack, DISK_SLOT, DISK_SLOT + 1, false)) {
                        if (!this.insertItem(originalStack, STORAGE_START, STORAGE_START + STORAGE_COUNT, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                } else if (!this.insertItem(originalStack, STORAGE_START, STORAGE_START + STORAGE_COUNT, false)) {
                    if (!this.insertItem(originalStack, FUEL_SLOT, FUEL_SLOT + 1, false)) {
                        if (!this.insertItem(originalStack, TOOL_SLOT, TOOL_SLOT + 1, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }
        return newStack;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.inventory.onClose(player);
    }
}
