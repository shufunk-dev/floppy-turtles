package com.turtlemod.block.entity;

import com.turtlemod.TurtleMod;
import com.turtlemod.app.AppType;
import com.turtlemod.app.TurtleStatus;
import com.turtlemod.block.TurtleBlock;
import com.turtlemod.item.FloppyDiskItem;
import com.turtlemod.screen.TurtleScreenHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TurtleBlockEntity extends BlockEntity implements Inventory, NamedScreenHandlerFactory {
    public static final int TOTAL_SLOTS = 12;
    public static final int ACTION_INTERVAL = 15; // Ticks between actions (~0.75s)

    private DefaultedList<ItemStack> items = DefaultedList.ofSize(TOTAL_SLOTS, ItemStack.EMPTY);

    private int fuel = 0;
    private int maxFuel = 10000;
    private TurtleStatus status = TurtleStatus.IDLE;
    private int configDistance = 16;
    private int stepsMoved = 0;
    private int progress = 0;
    private int actionCooldown = 0;
    private int quarryLayer = 0;
    private int quarryIndex = 0;
    private boolean isMoving = false;

    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> fuel;
                case 1 -> maxFuel;
                case 2 -> status.getId();
                case 3 -> getAppType().ordinal();
                case 4 -> getFacing().getIndex();
                case 5 -> progress;
                case 6 -> configDistance;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> fuel = value;
                case 1 -> maxFuel = value;
                case 2 -> status = TurtleStatus.fromIndex(value);
                case 6 -> configDistance = value;
            }
        }

        @Override
        public int size() {
            return 7;
        }
    };

    public TurtleBlockEntity(BlockPos pos, BlockState state) {
        super(TurtleMod.TURTLE_BLOCK_ENTITY, pos, state);
    }

    public boolean isMoving() {
        return isMoving;
    }

    public Direction getFacing() {
        if (this.getCachedState().contains(TurtleBlock.FACING)) {
            return this.getCachedState().get(TurtleBlock.FACING);
        }
        return Direction.NORTH;
    }

    public void setFacing(Direction newFacing) {
        if (world != null && this.getCachedState().contains(TurtleBlock.FACING)) {
            world.setBlockState(pos, this.getCachedState().with(TurtleBlock.FACING, newFacing), Block.NOTIFY_ALL);
        }
    }

    public AppType getAppType() {
        ItemStack diskStack = items.get(TurtleScreenHandler.DISK_SLOT);
        if (diskStack.getItem() instanceof FloppyDiskItem diskItem) {
            return diskItem.getAppType();
        }
        return AppType.NONE;
    }

    public ItemStack getToolStack() {
        return items.get(TurtleScreenHandler.TOOL_SLOT);
    }

    public PropertyDelegate getPropertyDelegate() {
        return propertyDelegate;
    }

    public void onButtonPressed(int buttonId) {
        switch (buttonId) {
            case 0 -> { // START
                if (getAppType() != AppType.NONE) {
                    if (this.fuel <= 0) {
                        tryRefuel();
                    }
                    if (this.fuel > 0) {
                        this.status = TurtleStatus.RUNNING;
                        this.actionCooldown = 5;
                    } else {
                        this.status = TurtleStatus.OUT_OF_FUEL;
                    }
                } else {
                    this.status = TurtleStatus.IDLE;
                }
                markDirty();
            }
            case 1 -> { // STOP
                this.status = TurtleStatus.PAUSED;
                markDirty();
            }
            case 2 -> { // PARAM -
                this.configDistance = Math.max(2, this.configDistance - 4);
                markDirty();
            }
            case 3 -> { // PARAM +
                this.configDistance = Math.min(128, this.configDistance + 4);
                markDirty();
            }
            case 4 -> { // RESET
                this.stepsMoved = 0;
                this.progress = 0;
                this.quarryLayer = 0;
                this.quarryIndex = 0;
                this.status = TurtleStatus.IDLE;
                markDirty();
            }
        }
    }

    public static void tick(World world, BlockPos pos, BlockState state, TurtleBlockEntity entity) {
        if (world.isClient() || entity.isMoving) return;

        // Auto refuel when low
        if (entity.fuel < 20) {
            entity.tryRefuel();
        }

        // Validate floppy disk insertion
        AppType currentApp = entity.getAppType();
        if (currentApp == AppType.NONE && entity.status == TurtleStatus.RUNNING) {
            entity.status = TurtleStatus.IDLE;
            entity.markDirty();
            return;
        }

        if (entity.status != TurtleStatus.RUNNING) {
            return;
        }

        if (entity.actionCooldown > 0) {
            entity.actionCooldown--;
            return;
        }

        entity.actionCooldown = ACTION_INTERVAL;
        entity.executeCurrentApp((ServerWorld) world);
    }

    private void executeCurrentApp(ServerWorld serverWorld) {
        AppType app = getAppType();
        switch (app) {
            case MINER -> executeMiner(serverWorld);
            case QUARRY -> executeQuarry(serverWorld);
            case BUILDER -> executeBuilder(serverWorld);
            case LUMBERJACK -> executeLumberjack(serverWorld);
            case FARMER -> executeFarmer(serverWorld);
            case SENTRY -> executeSentry(serverWorld);
            default -> {
                this.status = TurtleStatus.IDLE;
                markDirty();
            }
        }
    }

    private void tryRefuel() {
        if (this.world == null) return;
        ItemStack fuelStack = this.items.get(TurtleScreenHandler.FUEL_SLOT);
        if (!fuelStack.isEmpty() && this.world.getFuelRegistry().isFuel(fuelStack)) {
            int ticks = this.world.getFuelRegistry().getFuelTicks(fuelStack);
            int gain = Math.max(1, ticks / 20);
            if (this.fuel + gain <= this.maxFuel) {
                this.fuel += gain;
                ItemStack remainder = fuelStack.getRecipeRemainder();
                fuelStack.decrement(1);
                if (fuelStack.isEmpty() && !remainder.isEmpty()) {
                    this.items.set(TurtleScreenHandler.FUEL_SLOT, remainder);
                }
                if (this.status == TurtleStatus.OUT_OF_FUEL) {
                    this.status = TurtleStatus.RUNNING;
                }
                markDirty();
            }
        }
    }

    private boolean consumeFuel(int amount) {
        if (this.fuel >= amount) {
            this.fuel -= amount;
            markDirty();
            return true;
        }
        tryRefuel();
        if (this.fuel >= amount) {
            this.fuel -= amount;
            markDirty();
            return true;
        }
        this.status = TurtleStatus.OUT_OF_FUEL;
        markDirty();
        return false;
    }

    private void damageTool() {
        ItemStack tool = getToolStack();
        if (!tool.isEmpty() && this.world instanceof ServerWorld sw) {
            tool.damage(1, sw, null, item -> {});
            if (tool.isEmpty()) {
                this.items.set(TurtleScreenHandler.TOOL_SLOT, ItemStack.EMPTY);
            }
            markDirty();
        }
    }

    public ItemStack insertItem(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        for (int i = TurtleScreenHandler.STORAGE_START; i < TurtleScreenHandler.STORAGE_START + TurtleScreenHandler.STORAGE_COUNT; i++) {
            ItemStack slotStack = this.items.get(i);
            if (ItemStack.areItemsAndComponentsEqual(slotStack, stack)) {
                int space = slotStack.getMaxCount() - slotStack.getCount();
                int toAdd = Math.min(space, stack.getCount());
                if (toAdd > 0) {
                    slotStack.increment(toAdd);
                    stack.decrement(toAdd);
                    if (stack.isEmpty()) {
                        markDirty();
                        return ItemStack.EMPTY;
                    }
                }
            }
        }
        for (int i = TurtleScreenHandler.STORAGE_START; i < TurtleScreenHandler.STORAGE_START + TurtleScreenHandler.STORAGE_COUNT; i++) {
            ItemStack slotStack = this.items.get(i);
            if (slotStack.isEmpty()) {
                this.items.set(i, stack.copy());
                markDirty();
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    public boolean dig(ServerWorld world, BlockPos targetPos) {
        BlockState targetState = world.getBlockState(targetPos);
        if (targetState.isAir()) return true;
        if (targetState.getHardness(world, targetPos) < 0) {
            return false; // Bedrock or unbreakable
        }

        BlockEntity targetBe = world.getBlockEntity(targetPos);
        ItemStack tool = getToolStack();
        List<ItemStack> drops = Block.getDroppedStacks(targetState, world, targetPos, targetBe, null, tool);

        world.breakBlock(targetPos, false);
        damageTool();

        for (ItemStack drop : drops) {
            ItemStack leftover = insertItem(drop);
            if (!leftover.isEmpty()) {
                ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), leftover);
            }
        }
        return true;
    }

    public boolean move(ServerWorld world, Direction dir) {
        if (!consumeFuel(1)) return false;

        BlockPos targetPos = this.pos.offset(dir);
        if (!world.isInBuildLimit(targetPos)) {
            this.status = TurtleStatus.BLOCKED;
            return false;
        }

        BlockState targetState = world.getBlockState(targetPos);
        if (!targetState.isAir() && !targetState.isLiquid() && !targetState.isReplaceable()) {
            this.status = TurtleStatus.BLOCKED;
            return false;
        }

        // Relocate turtle block and data
        this.isMoving = true;
        BlockState currentState = this.getCachedState();

        world.removeBlockEntity(this.pos);
        world.setBlockState(this.pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        world.setBlockState(targetPos, currentState, Block.NOTIFY_ALL);

        BlockEntity newBe = world.getBlockEntity(targetPos);
        if (newBe instanceof TurtleBlockEntity targetTurtle) {
            targetTurtle.copyFrom(this);
            targetTurtle.isMoving = false;
            targetTurtle.markDirty();
        }
        return true;
    }

    public void copyFrom(TurtleBlockEntity source) {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            this.items.set(i, source.items.get(i).copy());
        }
        this.fuel = source.fuel;
        this.maxFuel = source.maxFuel;
        this.status = source.status;
        this.configDistance = source.configDistance;
        this.stepsMoved = source.stepsMoved;
        this.progress = source.progress;
        this.quarryLayer = source.quarryLayer;
        this.quarryIndex = source.quarryIndex;
        this.actionCooldown = source.actionCooldown;
    }

    // --- APP IMPLEMENTATIONS ---

    private void executeMiner(ServerWorld world) {
        Direction facing = getFacing();
        if (this.stepsMoved >= this.configDistance) {
            this.status = TurtleStatus.COMPLETED;
            markDirty();
            return;
        }

        BlockPos frontPos = this.pos.offset(facing);
        BlockPos frontUpPos = frontPos.up();

        if (!world.getBlockState(frontUpPos).isAir()) {
            if (!dig(world, frontUpPos)) {
                this.status = TurtleStatus.BLOCKED;
                markDirty();
                return;
            }
        }
        if (!world.getBlockState(frontPos).isAir()) {
            if (!dig(world, frontPos)) {
                this.status = TurtleStatus.BLOCKED;
                markDirty();
                return;
            }
        }

        if (move(world, facing)) {
            this.stepsMoved++;
            this.progress = (this.stepsMoved * 100) / this.configDistance;
            if (this.stepsMoved >= this.configDistance) {
                this.status = TurtleStatus.COMPLETED;
            }
        }
    }

    private void executeQuarry(ServerWorld world) {
        if (this.quarryLayer >= this.configDistance) {
            this.status = TurtleStatus.COMPLETED;
            markDirty();
            return;
        }

        int dx = (this.quarryIndex % 3) - 1;
        int dz = (this.quarryIndex / 3) - 1;
        BlockPos targetPos = this.pos.add(dx, -1, dz);

        if (!world.getBlockState(targetPos).isAir()) {
            if (!dig(world, targetPos)) {
                this.status = TurtleStatus.COMPLETED; // Reached bedrock
                markDirty();
                return;
            }
        }

        this.quarryIndex++;
        if (this.quarryIndex >= 9) {
            this.quarryIndex = 0;
            this.quarryLayer++;
            this.progress = (this.quarryLayer * 100) / this.configDistance;

            if (this.quarryLayer < this.configDistance) {
                move(world, Direction.DOWN);
            } else {
                this.status = TurtleStatus.COMPLETED;
            }
        }
        markDirty();
    }

    private void executeBuilder(ServerWorld world) {
        Direction facing = getFacing();
        if (this.stepsMoved >= this.configDistance) {
            this.status = TurtleStatus.COMPLETED;
            markDirty();
            return;
        }

        BlockPos belowTarget = this.pos.offset(facing).down();
        BlockState belowState = world.getBlockState(belowTarget);
        if (belowState.isAir() || belowState.isLiquid() || belowState.isReplaceable()) {
            boolean placed = placeSolidBlock(world, belowTarget);
            if (!placed) {
                this.status = TurtleStatus.BLOCKED; // Out of building blocks
                markDirty();
                return;
            }
        }

        BlockPos frontPos = this.pos.offset(facing);
        if (!world.getBlockState(frontPos).isAir()) {
            dig(world, frontPos);
        }

        if (move(world, facing)) {
            this.stepsMoved++;
            this.progress = (this.stepsMoved * 100) / this.configDistance;
            if (this.stepsMoved >= this.configDistance) {
                this.status = TurtleStatus.COMPLETED;
            }
        }
    }

    private boolean placeSolidBlock(ServerWorld world, BlockPos targetPos) {
        for (int i = TurtleScreenHandler.STORAGE_START; i < TurtleScreenHandler.STORAGE_START + TurtleScreenHandler.STORAGE_COUNT; i++) {
            ItemStack stack = this.items.get(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();
                BlockState state = block.getDefaultState();
                if (state.isSolid()) {
                    world.setBlockState(targetPos, state, Block.NOTIFY_ALL);
                    stack.decrement(1);
                    if (stack.isEmpty()) {
                        this.items.set(i, ItemStack.EMPTY);
                    }
                    markDirty();
                    return true;
                }
            }
        }
        return false;
    }

    private void executeLumberjack(ServerWorld world) {
        Direction facing = getFacing();
        BlockPos frontPos = this.pos.offset(facing);
        BlockState frontState = world.getBlockState(frontPos);

        if (frontState.isIn(BlockTags.LOGS)) {
            BlockPos currentPos = frontPos;
            int count = 0;
            while (count < 24 && world.getBlockState(currentPos).isIn(BlockTags.LOGS)) {
                dig(world, currentPos);
                currentPos = currentPos.up();
                count++;
            }
            this.stepsMoved++;
            this.progress = Math.min(100, (this.stepsMoved * 100) / this.configDistance);
            markDirty();
            return;
        }

        if (this.stepsMoved >= this.configDistance) {
            this.status = TurtleStatus.COMPLETED;
            markDirty();
            return;
        }

        if (move(world, facing)) {
            this.stepsMoved++;
            this.progress = (this.stepsMoved * 100) / this.configDistance;
        }
    }

    private void executeFarmer(ServerWorld world) {
        Direction facing = getFacing();
        if (this.stepsMoved >= this.configDistance) {
            this.status = TurtleStatus.COMPLETED;
            markDirty();
            return;
        }

        BlockPos frontPos = this.pos.offset(facing);
        BlockState frontState = world.getBlockState(frontPos);
        if (frontState.getBlock() instanceof CropBlock cropBlock) {
            if (cropBlock.isMature(frontState)) {
                dig(world, frontPos);
                // Replant
                world.setBlockState(frontPos, cropBlock.withAge(0), Block.NOTIFY_ALL);
            }
        }

        if (move(world, facing)) {
            this.stepsMoved++;
            this.progress = (this.stepsMoved * 100) / this.configDistance;
            if (this.stepsMoved >= this.configDistance) {
                this.status = TurtleStatus.COMPLETED;
            }
        }
    }

    private void executeSentry(ServerWorld world) {
        Box searchBox = new Box(this.pos).expand(4, 2, 4);
        List<HostileEntity> monsters = world.getEntitiesByClass(HostileEntity.class, searchBox, e -> e.isAlive());

        if (!monsters.isEmpty()) {
            HostileEntity target = monsters.get(0);
            if (consumeFuel(1)) {
                target.damage(world, world.getDamageSources().generic(), 6.0f);
                damageTool();
                world.playSound(null, this.pos, SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.BLOCKS, 1.0f, 1.0f);

                // Suck up nearby item drops
                List<ItemEntity> items = world.getEntitiesByClass(ItemEntity.class, searchBox, e -> e.isAlive());
                for (ItemEntity drop : items) {
                    ItemStack leftover = insertItem(drop.getStack());
                    if (leftover.isEmpty()) {
                        drop.discard();
                    } else {
                        drop.setStack(leftover);
                    }
                }
            }
        }
    }

    // --- INVENTORY IMPLEMENTATION ---

    @Override
    public int size() {
        return TOTAL_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(this.items, slot, amount);
        if (!result.isEmpty()) markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack result = Inventories.removeStack(this.items, slot);
        if (!result.isEmpty()) markDirty();
        return result;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return Inventory.canPlayerUse(this, player);
    }

    @Override
    public void clear() {
        this.items.clear();
        markDirty();
    }

    @Override
    public Text getDisplayName() {
        return Text.literal("Autonomous Turtle");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new TurtleScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    // --- DATA SERIALIZATION ---

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.items);
        view.putInt("fuel", this.fuel);
        view.putInt("maxFuel", this.maxFuel);
        view.putInt("status", this.status.getId());
        view.putInt("configDistance", this.configDistance);
        view.putInt("stepsMoved", this.stepsMoved);
        view.putInt("progress", this.progress);
        view.putInt("quarryLayer", this.quarryLayer);
        view.putInt("quarryIndex", this.quarryIndex);
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.items = DefaultedList.ofSize(TOTAL_SLOTS, ItemStack.EMPTY);
        Inventories.readData(view, this.items);
        this.fuel = view.getInt("fuel", 0);
        this.maxFuel = view.getInt("maxFuel", 10000);
        this.status = TurtleStatus.fromIndex(view.getInt("status", 0));
        this.configDistance = view.getInt("configDistance", 16);
        this.stepsMoved = view.getInt("stepsMoved", 0);
        this.progress = view.getInt("progress", 0);
        this.quarryLayer = view.getInt("quarryLayer", 0);
        this.quarryIndex = view.getInt("quarryIndex", 0);
    }
}
