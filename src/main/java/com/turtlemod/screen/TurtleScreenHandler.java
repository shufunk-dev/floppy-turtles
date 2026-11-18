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

    public TurtleScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(TOTAL_TURTLE_SLOTS), new ArrayPropertyDelegate(7));
    }

    public TurtleScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(TurtleMod.TURTLE_SCREEN_HANDLER, syncId);
        checkSize(inventory, TOTAL_TURTLE_SLOTS);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
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

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (this.inventory instanceof TurtleBlockEntity turtle) {
            turtle.onButtonPressed(id);
            return true;
        }
        return false;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
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
