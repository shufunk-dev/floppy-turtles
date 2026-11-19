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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

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
        // Check dedicated disk slot (Slot 1)
        ItemStack diskStack = items.get(TurtleScreenHandler.DISK_SLOT);
        if (diskStack.getItem() instanceof FloppyDiskItem diskItem && diskItem.getAppType() != AppType.NONE) {
            return diskItem.getAppType();
        }
        // Fallback: check storage slots (Slots 3-11)
        for (int i = TurtleScreenHandler.STORAGE_START; i < TurtleScreenHandler.STORAGE_START + TurtleScreenHandler.STORAGE_COUNT; i++) {
            ItemStack s = items.get(i);
            if (s.getItem() instanceof FloppyDiskItem d && d.getAppType() != AppType.NONE) {
                return d.getAppType();
            }
        }
        return AppType.NONE;
    }

    public ItemStack getToolStack() {
        // Check dedicated tool slot (Slot 2)
        ItemStack tool = items.get(TurtleScreenHandler.TOOL_SLOT);
        if (!tool.isEmpty()) return tool;

        // Fallback: check storage slots (Slots 3-11) for any tool
        for (int i = TurtleScreenHandler.STORAGE_START; i < TurtleScreenHandler.STORAGE_START + TurtleScreenHandler.STORAGE_COUNT; i++) {
            ItemStack s = items.get(i);
            if (!s.isEmpty() && isToolItem(s)) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    public boolean hasRequiredTool(AppType app) {
        ItemStack tool = getToolStack();
        return switch (app) {
            case MINER, QUARRY -> isPickaxe(tool);
            case LUMBERJACK -> isAxe(tool);
            case FARMER -> isHoe(tool);
            case SENTRY -> isSword(tool);
            default -> true;
        };
    }

    public static boolean isPickaxe(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.isOf(Items.WOODEN_PICKAXE) || stack.isOf(Items.STONE_PICKAXE) ||
               stack.isOf(Items.IRON_PICKAXE) || stack.isOf(Items.GOLDEN_PICKAXE) ||
               stack.isOf(Items.DIAMOND_PICKAXE) || stack.isOf(Items.NETHERITE_PICKAXE);
    }

    public static boolean isAxe(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.isOf(Items.WOODEN_AXE) || stack.isOf(Items.STONE_AXE) ||
               stack.isOf(Items.IRON_AXE) || stack.isOf(Items.GOLDEN_AXE) ||
               stack.isOf(Items.DIAMOND_AXE) || stack.isOf(Items.NETHERITE_AXE);
    }

    public static boolean isHoe(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.isOf(Items.WOODEN_HOE) || stack.isOf(Items.STONE_HOE) ||
               stack.isOf(Items.IRON_HOE) || stack.isOf(Items.GOLDEN_HOE) ||
               stack.isOf(Items.DIAMOND_HOE) || stack.isOf(Items.NETHERITE_HOE);
    }

    public static boolean isSword(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.isOf(Items.WOODEN_SWORD) || stack.isOf(Items.STONE_SWORD) ||
               stack.isOf(Items.IRON_SWORD) || stack.isOf(Items.GOLDEN_SWORD) ||
               stack.isOf(Items.DIAMOND_SWORD) || stack.isOf(Items.NETHERITE_SWORD);
    }

    private static boolean isToolItem(ItemStack s) {
        return isPickaxe(s) || isAxe(s) || isHoe(s) || isSword(s) ||
               s.isOf(Items.WOODEN_SHOVEL) || s.isOf(Items.STONE_SHOVEL) ||
               s.isOf(Items.IRON_SHOVEL) || s.isOf(Items.DIAMOND_SHOVEL) || s.isOf(Items.NETHERITE_SHOVEL);
    }

    public PropertyDelegate getPropertyDelegate() {
        return propertyDelegate;
    }

    public void onButtonPressed(int buttonId) {
        switch (buttonId) {
            case 0 -> { // START
                AppType app = getAppType();
                if (app == AppType.NONE) {
                    this.status = TurtleStatus.IDLE;
                    if (this.world != null) {
                        this.world.playSound(null, this.pos, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.BLOCKS, 1.0f, 0.5f);
                    }
                    markDirty();
                    return;
                }

                // Check and refuel
                if (this.fuel <= 0) {
                    tryRefuel();
                }
                if (this.fuel <= 0) {
                    this.status = TurtleStatus.OUT_OF_FUEL;
                    if (this.world != null) {
                        this.world.playSound(null, this.pos, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.BLOCKS, 1.0f, 0.5f);
                    }
                    markDirty();
                    return;
                }

                // Check required tool
                if (!hasRequiredTool(app)) {
                    this.status = TurtleStatus.NO_TOOL;
                    if (this.world != null) {
                        this.world.playSound(null, this.pos, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.BLOCKS, 1.0f, 0.5f);
                    }
                    markDirty();
                    return;
                }

                // Reset progress if starting fresh after completion
                if (this.status == TurtleStatus.COMPLETED) {
                    this.stepsMoved = 0;
                    this.progress = 0;
                    this.quarryLayer = 0;
                    this.quarryIndex = 0;
                }

                this.status = TurtleStatus.RUNNING;
                this.actionCooldown = 5;
                if (this.world != null) {
                    this.world.playSound(null, this.pos, SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.BLOCKS, 1.0f, 1.0f);
                }
                markDirty();
            }
            case 1 -> { // STOP / PAUSE
                this.status = TurtleStatus.PAUSED;
                if (this.world != null) {
                    this.world.playSound(null, this.pos, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.BLOCKS, 1.0f, 1.0f);
                }
                markDirty();
            }
            case 2 -> { // PARAM - (distance - 4)
                this.configDistance = Math.max(4, this.configDistance - 4);
                markDirty();
            }
            case 3 -> { // PARAM + (distance + 4)
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

        AppType currentApp = entity.getAppType();

        // If program disk was removed while running, halt to idle
        if (currentApp == AppType.NONE && entity.status == TurtleStatus.RUNNING) {
            entity.status = TurtleStatus.IDLE;
            entity.markDirty();
            return;
        }

        // Only tick actions if player started the program
        if (entity.status != TurtleStatus.RUNNING) {
            return;
        }

        // Auto refuel when low while running
        if (entity.fuel < 5) {
            entity.tryRefuel();
        }

        // Check fuel depletion
        if (entity.fuel <= 0) {
            entity.status = TurtleStatus.OUT_OF_FUEL;
            entity.markDirty();
            return;
        }

        // Check required tool while running
        if (!entity.hasRequiredTool(currentApp)) {
            entity.status = TurtleStatus.NO_TOOL;
            entity.markDirty();
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

    public int getFuelGain(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.isOf(Items.COAL) || stack.isOf(Items.CHARCOAL)) return 80;
        if (stack.isOf(Items.COAL_BLOCK)) return 800;
        if (stack.isOf(Items.BLAZE_ROD)) return 120;
        if (stack.isOf(Items.LAVA_BUCKET)) return 1000;
        if (stack.isOf(Items.OAK_PLANKS) || stack.isOf(Items.OAK_LOG) || stack.isOf(Items.STICK)) return 15;

        if (this.world != null) {
            try {
                int ticks = this.world.getFuelRegistry().getFuelTicks(stack);
                if (ticks > 0) return Math.max(1, ticks / 20);
            } catch (Exception ignored) {}
        }
        return 0;
    }

    public void tryRefuel() {
        if (this.world == null) return;

        // Check dedicated fuel slot (Slot 0) first
        ItemStack fuelStack = this.items.get(TurtleScreenHandler.FUEL_SLOT);
        int gain = getFuelGain(fuelStack);
        int slotUsed = TurtleScreenHandler.FUEL_SLOT;

        // If empty, check storage slots (Slots 3-11)
        if (gain <= 0) {
            for (int i = TurtleScreenHandler.STORAGE_START; i < TurtleScreenHandler.STORAGE_START + TurtleScreenHandler.STORAGE_COUNT; i++) {
                ItemStack s = this.items.get(i);
                int g = getFuelGain(s);
                if (g > 0) {
                    gain = g;
                    fuelStack = s;
                    slotUsed = i;
                    break;
                }
            }
        }

        if (gain > 0 && this.fuel + gain <= this.maxFuel) {
            this.fuel += gain;
            ItemStack remainder = fuelStack.isOf(Items.LAVA_BUCKET) ? new ItemStack(Items.BUCKET) : fuelStack.getRecipeRemainder();
            fuelStack.decrement(1);
            if (fuelStack.isEmpty() && !remainder.isEmpty()) {
                this.items.set(slotUsed, remainder);
            }
            if (this.status == TurtleStatus.OUT_OF_FUEL) {
                this.status = TurtleStatus.RUNNING;
            }
            markDirty();
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
                for (int i = 0; i < TOTAL_SLOTS; i++) {
                    if (this.items.get(i) == tool) {
                        this.items.set(i, ItemStack.EMPTY);
                        break;
                    }
                }
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
        if (targetState.isAir() || targetState.isLiquid()) return true;
        if (targetState.getHardness(world, targetPos) < 0) {
            return false; // Bedrock or unbreakable
        }

        // If block requires tool and tool is missing, notify status
        ItemStack tool = getToolStack();
        if (targetState.isToolRequired() && tool.isEmpty()) {
            this.status = TurtleStatus.NO_TOOL;
            markDirty();
            return false;
        }

        BlockEntity targetBe = world.getBlockEntity(targetPos);
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

    public TurtleBlockEntity move(ServerWorld world, Direction dir) {
        if (!consumeFuel(1)) return null;

        BlockPos targetPos = this.pos.offset(dir);
        if (!world.isInBuildLimit(targetPos)) {
            this.status = TurtleStatus.BLOCKED;
            return null;
        }

        BlockState targetState = world.getBlockState(targetPos);
        if (!targetState.isAir() && !targetState.isLiquid() && !targetState.isReplaceable()) {
            this.status = TurtleStatus.BLOCKED;
            return null;
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

            // Retarget any players viewing this turtle's GUI to the moved turtle!
            for (ServerPlayerEntity player : world.getPlayers()) {
                if (player.currentScreenHandler instanceof TurtleScreenHandler handler) {
                    if (handler.getTurtle() == this) {
                        handler.setTurtle(targetTurtle);
                    }
                }
            }

            targetTurtle.markDirty();
            return targetTurtle;
        }
        return null;
    }

    public void copyFrom(TurtleBlockEntity source) {
        this.items = source.items;
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

        Direction left = facing.rotateYCounterclockwise();
        Direction right = facing.rotateYClockwise();
        BlockPos sliceCenter = this.pos.offset(facing);

        // Mine 3x3 slice in front of the turtle:
        // dy: 0 (foot), 1 (eye), 2 (ceiling)
        // dxz: left (-1), center (0), right (+1)
        for (int dy = 0; dy <= 2; dy++) {
            for (int offset = -1; offset <= 1; offset++) {
                BlockPos target = sliceCenter.up(dy);
                if (offset == -1) target = target.offset(left);
                else if (offset == 1) target = target.offset(right);

                BlockState state = world.getBlockState(target);
                if (!state.isAir() && !state.isLiquid()) {
                    if (!dig(world, target)) {
                        // Blocked by bedrock or missing tool
                        if (this.status != TurtleStatus.NO_TOOL) {
                            this.status = TurtleStatus.BLOCKED;
                        }
                        markDirty();
                        return;
                    }
                }
            }
        }

        // Clear any falling sand/gravel that dropped into the center movement path
        for (int i = 0; i < 5; i++) {
            BlockState centerState = world.getBlockState(sliceCenter);
            if (centerState.isAir() || centerState.isLiquid()) break;
            if (!dig(world, sliceCenter)) break;
        }
        for (int i = 0; i < 5; i++) {
            BlockState aboveCenterState = world.getBlockState(sliceCenter.up());
            if (aboveCenterState.isAir() || aboveCenterState.isLiquid()) break;
            if (!dig(world, sliceCenter.up())) break;
        }

        TurtleBlockEntity movedTurtle = move(world, facing);
        if (movedTurtle != null) {
            movedTurtle.stepsMoved++;
            movedTurtle.progress = (movedTurtle.stepsMoved * 100) / movedTurtle.configDistance;

            // Place a torch every 7 blocks on the floor behind the turtle
            if (movedTurtle.stepsMoved % 7 == 0) {
                movedTurtle.placeTorch(world);
            }

            if (movedTurtle.stepsMoved >= movedTurtle.configDistance) {
                movedTurtle.status = TurtleStatus.COMPLETED;
            }
            movedTurtle.markDirty();
        }
    }

    private boolean placeTorch(ServerWorld world) {
        BlockPos backPos = this.pos.offset(getFacing().getOpposite());
        BlockState floorState = world.getBlockState(backPos.down());

        if (!world.getBlockState(backPos).isAir() || !floorState.isSolid()) {
            return false;
        }

        // 1. Try to use torches from storage (Slots 3-11)
        for (int i = TurtleScreenHandler.STORAGE_START; i < TurtleScreenHandler.STORAGE_START + TurtleScreenHandler.STORAGE_COUNT; i++) {
            ItemStack stack = this.items.get(i);
            if (!stack.isEmpty() && (stack.isOf(Items.TORCH) || stack.isOf(Items.SOUL_TORCH))) {
                BlockState torchState = stack.isOf(Items.SOUL_TORCH)
                        ? Blocks.SOUL_TORCH.getDefaultState()
                        : Blocks.TORCH.getDefaultState();
                world.setBlockState(backPos, torchState, Block.NOTIFY_ALL);
                stack.decrement(1);
                if (stack.isEmpty()) {
                    this.items.set(i, ItemStack.EMPTY);
                }
                world.playSound(null, backPos, SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.BLOCKS, 1.0f, 1.0f);
                markDirty();
                return true;
            }
        }

        // 2. Fallback: synthesize torch from fuel reserve
        if (this.fuel >= 1) {
            world.setBlockState(backPos, Blocks.TORCH.getDefaultState(), Block.NOTIFY_ALL);
            consumeFuel(1);
            world.playSound(null, backPos, SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.BLOCKS, 1.0f, 1.0f);
            markDirty();
            return true;
        }

        return false;
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
                TurtleBlockEntity movedTurtle = move(world, Direction.DOWN);
                if (movedTurtle != null) {
                    movedTurtle.markDirty();
                }
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

        TurtleBlockEntity movedTurtle = move(world, facing);
        if (movedTurtle != null) {
            movedTurtle.stepsMoved++;
            movedTurtle.progress = (movedTurtle.stepsMoved * 100) / movedTurtle.configDistance;
            if (movedTurtle.stepsMoved >= movedTurtle.configDistance) {
                movedTurtle.status = TurtleStatus.COMPLETED;
            }
            movedTurtle.markDirty();
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

        // Check front pos or front pos up for tree trunk
        BlockPos trunkPos = null;
        if (frontState.isIn(BlockTags.LOGS)) {
            trunkPos = frontPos;
        } else if (world.getBlockState(frontPos.up()).isIn(BlockTags.LOGS)) {
            trunkPos = frontPos.up();
        }

        if (trunkPos != null) {
            fellTree(world, trunkPos);
            this.stepsMoved++;
            this.progress = Math.min(100, (this.stepsMoved * 100) / this.configDistance);
            if (this.stepsMoved >= this.configDistance) {
                this.status = TurtleStatus.COMPLETED;
            }
            markDirty();
            return;
        }

        // Clear any leaves in front or eye level so turtle isn't blocked while walking
        if (frontState.isIn(BlockTags.LEAVES)) {
            dig(world, frontPos);
        }
        if (world.getBlockState(frontPos.up()).isIn(BlockTags.LEAVES)) {
            dig(world, frontPos.up());
        }

        if (this.stepsMoved >= this.configDistance) {
            this.status = TurtleStatus.COMPLETED;
            markDirty();
            return;
        }

        // Try moving forward; if blocked by a 1-block natural terrain rise, step up onto it
        TurtleBlockEntity movedTurtle = move(world, facing);
        if (movedTurtle == null) {
            BlockPos upTarget = frontPos.up();
            BlockState frontStateNow = world.getBlockState(frontPos);
            if (!frontStateNow.isAir() && world.getBlockState(upTarget).isAir()) {
                TurtleBlockEntity upTurtle = move(world, Direction.UP);
                if (upTurtle != null) {
                    movedTurtle = upTurtle.move(world, facing);
                }
            }
        }

        // If moved forward and there is a 1-block drop, step down to hug the terrain
        if (movedTurtle != null) {
            BlockPos below = movedTurtle.pos.down();
            if (world.getBlockState(below).isAir()) {
                TurtleBlockEntity downTurtle = movedTurtle.move(world, Direction.DOWN);
                if (downTurtle != null) {
                    movedTurtle = downTurtle;
                }
            }

            movedTurtle.stepsMoved++;
            movedTurtle.progress = (movedTurtle.stepsMoved * 100) / movedTurtle.configDistance;
            if (movedTurtle.stepsMoved >= movedTurtle.configDistance) {
                movedTurtle.status = TurtleStatus.COMPLETED;
            }
            movedTurtle.markDirty();
        }
    }

    private void fellTree(ServerWorld world, BlockPos startPos) {
        Set<BlockPos> visitedLogs = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> leavesToBreak = new HashSet<>();

        queue.add(startPos);
        visitedLogs.add(startPos);

        int maxLogs = 128;
        int startY = startPos.getY();

        // 1. BFS to find all connected logs in the tree and adjacent leaves
        while (!queue.isEmpty() && visitedLogs.size() < maxLogs) {
            BlockPos current = queue.poll();

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos neighbor = current.add(dx, dy, dz);

                        // Don't search deep into the ground
                        if (neighbor.getY() < startY - 1) continue;
                        // Don't search too far horizontally from the trunk (within 10 blocks)
                        if (Math.abs(neighbor.getX() - startPos.getX()) > 10 || Math.abs(neighbor.getZ() - startPos.getZ()) > 10) continue;

                        BlockState nState = world.getBlockState(neighbor);
                        if (nState.isIn(BlockTags.LOGS)) {
                            if (visitedLogs.add(neighbor)) {
                                queue.add(neighbor);
                            }
                        } else if (nState.isIn(BlockTags.LEAVES)) {
                            leavesToBreak.add(neighbor);
                        }
                    }
                }
            }
        }

        // 2. Expand leaves by up to 2 blocks around detected leaves to capture full canopy
        for (int step = 0; step < 2; step++) {
            Set<BlockPos> extraLeaves = new HashSet<>();
            for (BlockPos leaf : leavesToBreak) {
                for (Direction dir : Direction.values()) {
                    BlockPos nLeaf = leaf.offset(dir);
                    if (!leavesToBreak.contains(nLeaf) && world.getBlockState(nLeaf).isIn(BlockTags.LEAVES)) {
                        extraLeaves.add(nLeaf);
                    }
                }
            }
            leavesToBreak.addAll(extraLeaves);
        }

        // 3. Break all logs from top to bottom
        List<BlockPos> sortedLogs = new ArrayList<>(visitedLogs);
        sortedLogs.sort((a, b) -> Integer.compare(b.getY(), a.getY()));
        for (BlockPos logPos : sortedLogs) {
            dig(world, logPos);
        }

        // 4. Break all canopy leaves
        for (BlockPos leafPos : leavesToBreak) {
            BlockState leafState = world.getBlockState(leafPos);
            if (leafState.isIn(BlockTags.LEAVES)) {
                List<ItemStack> drops = Block.getDroppedStacks(leafState, world, leafPos, null, null, ItemStack.EMPTY);
                world.breakBlock(leafPos, false);
                for (ItemStack drop : drops) {
                    ItemStack leftover = insertItem(drop);
                    if (!leftover.isEmpty()) {
                        ItemScatterer.spawn(world, this.pos.getX(), this.pos.getY(), this.pos.getZ(), leftover);
                    }
                }
            }
        }

        // 5. Vacuum all nearby drops into the turtle
        Box vacuumBox = new Box(startPos).expand(12, 16, 12);
        List<ItemEntity> drops = world.getEntitiesByClass(ItemEntity.class, vacuumBox, e -> e.isAlive());
        for (ItemEntity drop : drops) {
            ItemStack leftover = insertItem(drop.getStack());
            if (leftover.isEmpty()) {
                drop.discard();
            } else {
                drop.setStack(leftover);
            }
        }

        world.playSound(null, startPos, SoundEvents.BLOCK_WOOD_BREAK, SoundCategory.BLOCKS, 1.0f, 0.8f);
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

        TurtleBlockEntity movedTurtle = move(world, facing);
        if (movedTurtle != null) {
            movedTurtle.stepsMoved++;
            movedTurtle.progress = (movedTurtle.stepsMoved * 100) / movedTurtle.configDistance;
            if (movedTurtle.stepsMoved >= movedTurtle.configDistance) {
                movedTurtle.status = TurtleStatus.COMPLETED;
            }
            movedTurtle.markDirty();
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
        return Text.literal("Turtle");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        TurtleScreenHandler.DelegatingInventory inv = new TurtleScreenHandler.DelegatingInventory(this);
        TurtleScreenHandler.DelegatingPropertyDelegate props = new TurtleScreenHandler.DelegatingPropertyDelegate(this.propertyDelegate);
        TurtleScreenHandler handler = new TurtleScreenHandler(syncId, playerInventory, inv, props);
        handler.setTurtle(this);
        return handler;
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
