package com.turtlemod.block.entity;

import com.turtlemod.TurtleMod;
import com.turtlemod.app.AppType;
import com.turtlemod.app.TurtleStatus;
import com.turtlemod.block.TurtleBlock;
import com.turtlemod.item.FloppyDiskItem;
import com.turtlemod.screen.TurtleScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TurtleBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int TOTAL_SLOTS = 12;
    public static final int ACTION_INTERVAL = 15; // Ticks between actions (~0.75s)

    private NonNullList<ItemStack> items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);

    private int fuel = 0;
    private int maxFuel = 10000;
    private TurtleStatus status = TurtleStatus.IDLE;
    private int configDistance = 16;
    private int stepsMoved = 0;
    private int progress = 0;
    private int actionCooldown = 0;
    private int quarryLayer = 0;
    private int quarryIndex = 0;
    private int farmIndex = 0;
    private int farmRow = -1;
    private int farmMoveStep = 0;
    private boolean isMoving = false;

    public static final int[][] ROOM_PRESETS = {
            { 3, 3, 3 },
            { 5, 5, 5 },
            { 7, 4, 7 },
            { 8, 4, 8 },
            { 9, 5, 9 },
            { 10, 5, 10 },
            { 10, 10, 10 },
            { 12, 5, 12 },
            { 16, 5, 16 },
            { 16, 8, 16 },
            { 16, 10, 16 },
            { 20, 6, 20 },
            { 24, 8, 24 },
            { 32, 6, 32 },
            { 32, 10, 32 }
    };

    private int roomWidth = 5;
    private int roomHeight = 5;
    private int roomLength = 5;
    private int roomConfigMode = 0; // 0 = Preset, 1 = Width, 2 = Height, 3 = Length
    private int excavatorSlice = 1; // dz (1 .. roomLength)
    private int excavatorY = 4; // dy (roomHeight - 1 down to 0)
    private int excavatorX = -2; // dx (minDx .. maxDx)

    private final ContainerData propertyDelegate = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> fuel;
                case 1 -> maxFuel;
                case 2 -> status.getId();
                case 3 -> getAppType().ordinal();
                case 4 -> getFacing().get3DDataValue();
                case 5 -> progress;
                case 6 -> configDistance;
                case 7 -> roomWidth;
                case 8 -> roomHeight;
                case 9 -> roomLength;
                case 10 -> roomConfigMode;
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
                case 7 -> roomWidth = Math.max(1, Math.min(32, value));
                case 8 -> roomHeight = Math.max(1, Math.min(10, value));
                case 9 -> roomLength = Math.max(1, Math.min(32, value));
                case 10 -> roomConfigMode = (value >= 0 && value <= 3) ? value : 0;
            }
        }

        @Override
        public int getCount() {
            return 11;
        }
    };

    public TurtleBlockEntity(BlockPos pos, BlockState state) {
        super(TurtleMod.TURTLE_BLOCK_ENTITY, pos, state);
    }

    public boolean isMoving() {
        return isMoving;
    }

    public Direction getFacing() {
        if (this.getBlockState().hasProperty(TurtleBlock.FACING)) {
            return this.getBlockState().getValue(TurtleBlock.FACING);
        }
        return Direction.NORTH;
    }

    public void setFacing(Direction newFacing) {
        if (this.level != null && this.getBlockState().hasProperty(TurtleBlock.FACING)) {
            this.level.setBlock(this.worldPosition, this.getBlockState().setValue(TurtleBlock.FACING, newFacing),
                    Block.UPDATE_ALL);
        }
    }

    public AppType getAppType() {
        // Check dedicated disk slot (Slot 1)
        ItemStack diskStack = items.get(TurtleScreenHandler.DISK_SLOT);
        if (diskStack.getItem() instanceof FloppyDiskItem diskItem && diskItem.getAppType() != AppType.NONE) {
            return diskItem.getAppType();
        }
        // Fallback: check storage slots (Slots 3-11)
        for (int i = TurtleScreenHandler.STORAGE_START; i < TurtleScreenHandler.STORAGE_START
                + TurtleScreenHandler.STORAGE_COUNT; i++) {
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
        if (!tool.isEmpty())
            return tool;

        // Fallback: check storage slots (Slots 3-11) for any tool
        for (int i = TurtleScreenHandler.STORAGE_START; i < TurtleScreenHandler.STORAGE_START
                + TurtleScreenHandler.STORAGE_COUNT; i++) {
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
            case MINER, QUARRY, ROOM_CARVER -> isPickaxe(tool);
            case LUMBERJACK -> isAxe(tool);
            case FARMER, FARM_CREATOR -> isHoe(tool);
            case SENTRY -> isSword(tool);
            default -> true;
        };
    }

    public static boolean isPickaxe(ItemStack stack) {
        if (stack.isEmpty())
            return false;
        return stack.is(Items.WOODEN_PICKAXE) || stack.is(Items.STONE_PICKAXE) ||
                stack.is(Items.IRON_PICKAXE) || stack.is(Items.GOLDEN_PICKAXE) ||
                stack.is(Items.DIAMOND_PICKAXE) || stack.is(Items.NETHERITE_PICKAXE);
    }

    public static boolean isAxe(ItemStack stack) {
        if (stack.isEmpty())
            return false;
        return stack.is(Items.WOODEN_AXE) || stack.is(Items.STONE_AXE) ||
                stack.is(Items.IRON_AXE) || stack.is(Items.GOLDEN_AXE) ||
                stack.is(Items.DIAMOND_AXE) || stack.is(Items.NETHERITE_AXE);
    }

    public static boolean isHoe(ItemStack stack) {
        if (stack.isEmpty())
            return false;
        return stack.is(Items.WOODEN_HOE) || stack.is(Items.STONE_HOE) ||
                stack.is(Items.IRON_HOE) || stack.is(Items.GOLDEN_HOE) ||
                stack.is(Items.DIAMOND_HOE) || stack.is(Items.NETHERITE_HOE);
    }

    public static boolean isSword(ItemStack stack) {
        if (stack.isEmpty())
            return false;
        return stack.is(Items.WOODEN_SWORD) || stack.is(Items.STONE_SWORD) ||
                stack.is(Items.IRON_SWORD) || stack.is(Items.GOLDEN_SWORD) ||
                stack.is(Items.DIAMOND_SWORD) || stack.is(Items.NETHERITE_SWORD);
    }

    private static boolean isToolItem(ItemStack s) {
        return isPickaxe(s) || isAxe(s) || isHoe(s) || isSword(s) ||
                s.is(Items.WOODEN_SHOVEL) || s.is(Items.STONE_SHOVEL) ||
                s.is(Items.IRON_SHOVEL) || s.is(Items.DIAMOND_SHOVEL) || s.is(Items.NETHERITE_SHOVEL);
    }

    public ContainerData getPropertyDelegate() {
        return propertyDelegate;
    }

    public void onButtonPressed(int buttonId) {
        switch (buttonId) {
            case 0 -> { // START
                AppType app = getAppType();
                if (app == AppType.NONE) {
                    this.status = TurtleStatus.IDLE;
                    if (this.level != null) {
                        this.level.playSound(null, this.worldPosition, SoundEvents.NOTE_BLOCK_BASS.value(),
                                SoundSource.BLOCKS, 1.0f, 0.5f);
                    }
                    setChanged();
                    return;
                }

                // Check and refuel
                if (this.fuel <= 0) {
                    tryRefuel();
                }
                if (this.fuel <= 0) {
                    this.status = TurtleStatus.OUT_OF_FUEL;
                    if (this.level != null) {
                        this.level.playSound(null, this.worldPosition, SoundEvents.NOTE_BLOCK_BASS.value(),
                                SoundSource.BLOCKS, 1.0f, 0.5f);
                    }
                    setChanged();
                    return;
                }

                // Check required tool
                if (!hasRequiredTool(app)) {
                    this.status = TurtleStatus.NO_TOOL;
                    if (this.level != null) {
                        this.level.playSound(null, this.worldPosition, SoundEvents.NOTE_BLOCK_BASS.value(),
                                SoundSource.BLOCKS, 1.0f, 0.5f);
                    }
                    setChanged();
                    return;
                }

                // Reset progress if starting fresh after completion
                if (this.status == TurtleStatus.COMPLETED) {
                    this.stepsMoved = 0;
                    this.progress = 0;
                    this.quarryLayer = 0;
                    this.quarryIndex = 0;
                    this.farmIndex = 0;
                    this.farmRow = -1;
                    this.farmMoveStep = 0;
                    resetRoomCarverCoordinates();
                }

                this.status = TurtleStatus.RUNNING;
                this.actionCooldown = 5;
                if (this.level != null) {
                    this.level.playSound(null, this.worldPosition, SoundEvents.NOTE_BLOCK_BELL.value(),
                            SoundSource.BLOCKS, 1.0f, 1.0f);
                }
                setChanged();
            }
            case 1 -> { // STOP / PAUSE
                this.status = TurtleStatus.PAUSED;
                if (this.level != null) {
                    this.level.playSound(null, this.worldPosition, SoundEvents.NOTE_BLOCK_BASS.value(),
                            SoundSource.BLOCKS, 1.0f, 1.0f);
                }
                setChanged();
            }
            case 2 -> { // PARAM -
                AppType app = getAppType();
                if (app == AppType.ROOM_CARVER) {
                    adjustRoomDimensions(-1);
                } else if (app == AppType.FARM_CREATOR) {
                    this.configDistance = Math.max(1, this.configDistance - 1);
                } else if (app == AppType.BUILDER) {
                    if (this.configDistance == 5)
                        this.configDistance = 3;
                    else if (this.configDistance == 3)
                        this.configDistance = 1;
                    else
                        this.configDistance = 1;
                } else if (app == AppType.FARMER) {
                    this.configDistance = Math.max(9, this.configDistance - 9);
                } else if (app == AppType.SENTRY) {
                    this.configDistance = Math.max(3, this.configDistance - 1);
                } else {
                    this.configDistance = Math.max(4, this.configDistance - 4);
                }
                setChanged();
            }
            case 3 -> { // PARAM +
                AppType app = getAppType();
                if (app == AppType.ROOM_CARVER) {
                    adjustRoomDimensions(1);
                } else if (app == AppType.FARM_CREATOR) {
                    this.configDistance = Math.min(3, this.configDistance + 1);
                } else if (app == AppType.BUILDER) {
                    if (this.configDistance == 1)
                        this.configDistance = 3;
                    else if (this.configDistance == 3)
                        this.configDistance = 5;
                    else
                        this.configDistance = 5;
                } else if (app == AppType.FARMER) {
                    this.configDistance = Math.min(72, this.configDistance + 9);
                } else if (app == AppType.SENTRY) {
                    this.configDistance = Math.min(10, this.configDistance + 1);
                } else {
                    this.configDistance = Math.min(128, this.configDistance + 4);
                }
                setChanged();
            }
            case 4 -> { // RESET
                this.stepsMoved = 0;
                this.progress = 0;
                this.quarryLayer = 0;
                this.quarryIndex = 0;
                this.farmIndex = 0;
                this.farmRow = -1;
                this.farmMoveStep = 0;
                resetRoomCarverCoordinates();
                this.status = TurtleStatus.IDLE;
                setChanged();
            }
            case 5 -> { // TOGGLE CONFIG MODE (Room Carver: Presets -> Width -> Height -> Length)
                AppType app = getAppType();
                if (app == AppType.ROOM_CARVER) {
                    this.roomConfigMode = (this.roomConfigMode + 1) % 4;
                    if (this.level != null) {
                        this.level.playSound(null, this.worldPosition, SoundEvents.NOTE_BLOCK_BELL.value(),
                                SoundSource.BLOCKS, 1.0f, 1.5f);
                    }
                    setChanged();
                }
            }
        }
    }

    public static void tick(Level world, BlockPos pos, BlockState state, TurtleBlockEntity entity) {
        if (world.isClientSide() || entity.isMoving)
            return;

        AppType currentApp = entity.getAppType();

        // If program disk was removed while running, halt to idle
        if (currentApp == AppType.NONE && entity.status == TurtleStatus.RUNNING) {
            entity.status = TurtleStatus.IDLE;
            entity.setChanged();
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
            entity.setChanged();
            return;
        }

        // Check required tool while running
        if (!entity.hasRequiredTool(currentApp)) {
            entity.status = TurtleStatus.NO_TOOL;
            entity.setChanged();
            return;
        }

        if (entity.actionCooldown > 0) {
            entity.actionCooldown--;
            return;
        }

        entity.actionCooldown = ACTION_INTERVAL;
        entity.executeCurrentApp((ServerLevel) world);
    }

    private void executeCurrentApp(ServerLevel serverWorld) {
        AppType app = getAppType();
        switch (app) {
            case MINER -> executeMiner(serverWorld);
            case QUARRY -> executeQuarry(serverWorld);
            case BUILDER -> executeBuilder(serverWorld);
            case LUMBERJACK -> executeLumberjack(serverWorld);
            case FARMER -> executeFarmer(serverWorld);
            case FARM_CREATOR -> executeFarmCreator(serverWorld);
            case SENTRY -> executeSentry(serverWorld);
            case ROOM_CARVER -> executeRoomCarver(serverWorld);
            default -> {
                this.status = TurtleStatus.IDLE;
                setChanged();
            }
        }
    }

    public int getFuelGain(ItemStack stack) {
        if (stack.isEmpty())
            return 0;
        if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL))
            return 80;
        if (stack.is(Items.COAL_BLOCK))
            return 800;
        if (stack.is(Items.BLAZE_ROD))
            return 120;
        if (stack.is(Items.LAVA_BUCKET))
            return 1000;
        if (stack.is(Items.OAK_PLANKS) || stack.is(Items.OAK_LOG) || stack.is(Items.STICK))
            return 15;

        if (stack.has(DataComponents.COOKING_FUEL)) {
            return 80;
        }
        return 0;
    }

    public void tryRefuel() {
        if (this.level == null)
            return;

        // Check dedicated fuel slot (Slot 0) first
        ItemStack fuelStack = this.items.get(TurtleScreenHandler.FUEL_SLOT);
        int gain = getFuelGain(fuelStack);
        int slotUsed = TurtleScreenHandler.FUEL_SLOT;

        // If empty, check storage slots (Slots 3-11)
        if (gain <= 0) {
            for (int i = TurtleScreenHandler.STORAGE_START; i < TurtleScreenHandler.STORAGE_START
                    + TurtleScreenHandler.STORAGE_COUNT; i++) {
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
            ItemStack remainder = fuelStack.is(Items.LAVA_BUCKET) ? new ItemStack(Items.BUCKET) : ItemStack.EMPTY;
            fuelStack.shrink(1);
            if (fuelStack.isEmpty() && !remainder.isEmpty()) {
                this.items.set(slotUsed, remainder);
            }
            if (this.status == TurtleStatus.OUT_OF_FUEL) {
                this.status = TurtleStatus.RUNNING;
            }
            setChanged();
        }
    }

    private boolean consumeFuel(int amount) {
        if (this.fuel >= amount) {
            this.fuel -= amount;
            setChanged();
            return true;
        }
        tryRefuel();
        if (this.fuel >= amount) {
            this.fuel -= amount;
            setChanged();
            return true;
        }
        this.status = TurtleStatus.OUT_OF_FUEL;
        setChanged();
        return false;
    }

    private void damageTool() {
        ItemStack tool = getToolStack();
        if (!tool.isEmpty() && this.level instanceof ServerLevel sw) {
            tool.hurtAndBreak(1, sw, null, item -> {
            });
            if (tool.isEmpty()) {
                for (int i = 0; i < TOTAL_SLOTS; i++) {
                    if (this.items.get(i) == tool) {
                        this.items.set(i, ItemStack.EMPTY);
                        break;
                    }
                }
            }
            setChanged();
        }
    }

    public ItemStack insertItem(ItemStack stack) {
        if (stack.isEmpty())
            return ItemStack.EMPTY;
        for (int i = TurtleScreenHandler.STORAGE_START; i < TurtleScreenHandler.STORAGE_START
                + TurtleScreenHandler.STORAGE_COUNT; i++) {
            ItemStack slotStack = this.items.get(i);
            if (ItemStack.isSameItemSameComponents(slotStack, stack)) {
                int space = slotStack.getMaxStackSize() - slotStack.getCount();
                int toAdd = Math.min(space, stack.getCount());
                if (toAdd > 0) {
                    slotStack.grow(toAdd);
                    stack.shrink(toAdd);
                    if (stack.isEmpty()) {
                        setChanged();
                        return ItemStack.EMPTY;
                    }
                }
            }
        }
        for (int i = TurtleScreenHandler.STORAGE_START; i < TurtleScreenHandler.STORAGE_START
                + TurtleScreenHandler.STORAGE_COUNT; i++) {
            ItemStack slotStack = this.items.get(i);
            if (slotStack.isEmpty()) {
                this.items.set(i, stack.copy());
                setChanged();
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    public boolean dig(ServerLevel world, BlockPos targetPos) {
        BlockState targetState = world.getBlockState(targetPos);
        if (targetState.isAir() || !targetState.getFluidState().isEmpty())
            return true;
        if (targetState.getDestroySpeed(world, targetPos) < 0) {
            return false; // Bedrock or unbreakable
        }

        // If block requires tool and tool is missing, notify status
        ItemStack tool = getToolStack();
        if (targetState.requiresCorrectToolForDrops() && tool.isEmpty()) {
            this.status = TurtleStatus.NO_TOOL;
            setChanged();
            return false;
        }

        BlockEntity targetBe = world.getBlockEntity(targetPos);
        List<ItemStack> drops = Block.getDrops(targetState, world, targetPos, targetBe, null, tool);

        world.destroyBlock(targetPos, false);
        damageTool();

        for (ItemStack drop : drops) {
            ItemStack leftover = insertItem(drop);
            if (!leftover.isEmpty()) {
                Containers.dropItemStack(world, this.worldPosition.getX(), this.worldPosition.getY(),
                        this.worldPosition.getZ(), leftover);
            }
        }
        return true;
    }

    public TurtleBlockEntity move(ServerLevel world, Direction dir) {
        if (!consumeFuel(1))
            return null;

        BlockPos targetPos = this.worldPosition.relative(dir);
        if (world.isOutsideBuildHeight(targetPos)) {
            return null;
        }

        BlockState targetState = world.getBlockState(targetPos);
        // Protect crops: never allow moving down into a crop
        if (dir == Direction.DOWN && targetState.getBlock() instanceof CropBlock) {
            return null;
        }

        // Break non-solid obstacles (flowers, tall grass, ferns, saplings) so turtle
        // can enter
        if (!targetState.isAir() && targetState.getFluidState().isEmpty() && !targetState.canBeReplaced()) {
            if (targetState.getCollisionShape(world, targetPos).isEmpty()) {
                dig(world, targetPos);
                targetState = world.getBlockState(targetPos);
            }
        }

        if (!targetState.isAir() && targetState.getFluidState().isEmpty() && !targetState.canBeReplaced()) {
            return null;
        }

        // Relocate turtle block and data
        this.isMoving = true;
        BlockState currentState = this.getBlockState();

        world.removeBlockEntity(this.worldPosition);
        world.setBlock(this.worldPosition, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        world.setBlock(targetPos, currentState, Block.UPDATE_ALL);

        BlockEntity newBe = world.getBlockEntity(targetPos);
        if (newBe instanceof TurtleBlockEntity targetTurtle) {
            targetTurtle.copyFrom(this);
            targetTurtle.isMoving = false;

            // Retarget any players viewing this turtle's GUI to the moved turtle!
            for (ServerPlayer player : world.players()) {
                if (player.containerMenu instanceof TurtleScreenHandler handler) {
                    if (handler.getTurtle() == this) {
                        handler.setTurtle(targetTurtle);
                    }
                }
            }

            targetTurtle.setChanged();
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
        this.farmIndex = source.farmIndex;
        this.farmRow = source.farmRow;
        this.farmMoveStep = source.farmMoveStep;
        this.roomWidth = source.roomWidth;
        this.roomHeight = source.roomHeight;
        this.roomLength = source.roomLength;
        this.roomConfigMode = source.roomConfigMode;
        this.excavatorSlice = source.excavatorSlice;
        this.excavatorY = source.excavatorY;
        this.excavatorX = source.excavatorX;
        this.actionCooldown = source.actionCooldown;
    }

    // --- APP IMPLEMENTATIONS ---

    private void executeMiner(ServerLevel world) {
        Direction facing = getFacing();
        if (this.stepsMoved >= this.configDistance) {
            this.status = TurtleStatus.COMPLETED;
            setChanged();
            return;
        }

        Direction left = facing.getCounterClockWise();
        Direction right = facing.getClockWise();
        BlockPos sliceCenter = this.worldPosition.relative(facing);

        // Mine 3x3 slice in front of the turtle:
        // dy: 0 (foot), 1 (eye), 2 (ceiling)
        // dxz: left (-1), center (0), right (+1)
        for (int dy = 0; dy <= 2; dy++) {
            for (int offset = -1; offset <= 1; offset++) {
                BlockPos target = sliceCenter.above(dy);
                if (offset == -1)
                    target = target.relative(left);
                else if (offset == 1)
                    target = target.relative(right);

                BlockState state = world.getBlockState(target);
                if (!state.isAir() && state.getFluidState().isEmpty()) {
                    if (!dig(world, target)) {
                        // Blocked by bedrock or missing tool
                        if (this.status != TurtleStatus.NO_TOOL) {
                            this.status = TurtleStatus.BLOCKED;
                        }
                        setChanged();
                        return;
                    }
                }
            }
        }

        // Clear any falling sand/gravel that dropped into the center movement path
        for (int i = 0; i < 5; i++) {
            BlockState centerState = world.getBlockState(sliceCenter);
            if (centerState.isAir() || !centerState.getFluidState().isEmpty())
                break;
            if (!dig(world, sliceCenter))
                break;
        }
        for (int i = 0; i < 5; i++) {
            BlockState aboveCenterState = world.getBlockState(sliceCenter.above());
            if (aboveCenterState.isAir() || !aboveCenterState.getFluidState().isEmpty())
                break;
            if (!dig(world, sliceCenter.above()))
                break;
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
            movedTurtle.setChanged();
        }
    }

    private boolean placeTorch(ServerLevel world) {
        BlockPos backPos = this.worldPosition.relative(getFacing().getOpposite());
        BlockState floorState = world.getBlockState(backPos.below());

        if (!world.getBlockState(backPos).isAir() || !floorState.isSolid()) {
            return false;
        }

        // 1. Try to use torches from storage (Slots 3-11)
        for (int i = TurtleScreenHandler.STORAGE_START; i < TurtleScreenHandler.STORAGE_START
                + TurtleScreenHandler.STORAGE_COUNT; i++) {
            ItemStack stack = this.items.get(i);
            if (!stack.isEmpty() && (stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH))) {
                BlockState torchState = stack.is(Items.SOUL_TORCH)
                        ? Blocks.SOUL_TORCH.defaultBlockState()
                        : Blocks.TORCH.defaultBlockState();
                world.setBlock(backPos, torchState, Block.UPDATE_ALL);
                stack.shrink(1);
                if (stack.isEmpty()) {
                    this.items.set(i, ItemStack.EMPTY);
                }
                world.playSound(null, backPos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
                setChanged();
                return true;
            }
        }

        // 2. Fallback: synthesize torch from fuel reserve
        if (this.fuel >= 1) {
            world.setBlock(backPos, Blocks.TORCH.defaultBlockState(), Block.UPDATE_ALL);
            consumeFuel(1);
            world.playSound(null, backPos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
            setChanged();
            return true;
        }

        return false;
    }

    private void executeQuarry(ServerLevel world) {
        if (this.quarryLayer >= this.configDistance) {
            this.status = TurtleStatus.COMPLETED;
            setChanged();
            return;
        }

        int dx = (this.quarryIndex % 3) - 1;
        int dz = (this.quarryIndex / 3) - 1;
        BlockPos targetPos = this.worldPosition.offset(dx, -1, dz);

        if (!world.getBlockState(targetPos).isAir()) {
            if (!dig(world, targetPos)) {
                this.status = TurtleStatus.COMPLETED; // Reached bedrock
                setChanged();
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
                    movedTurtle.setChanged();
                }
            } else {
                this.status = TurtleStatus.COMPLETED;
            }
        }
        setChanged();
    }

    private void executeBuilder(ServerLevel world) {
        Direction facing = getFacing();
        Direction left = facing.getCounterClockWise();

        if (this.stepsMoved >= 64) {
            this.status = TurtleStatus.COMPLETED;
            setChanged();
            return;
        }

        int width = (this.configDistance == 3 || this.configDistance == 5) ? this.configDistance : 1;

        // 1. Pave foundation floor blocks underneath the bridge row ahead
        int[] floorOffsets = switch (width) {
            case 3 -> new int[] { -1, 0, 1 };
            case 5 -> new int[] { -2, -1, 0, 1, 2 };
            default -> new int[] { 0 };
        };

        boolean neededPaving = false;
        for (int offset : floorOffsets) {
            BlockPos floorPos = this.worldPosition.relative(facing).relative(left, offset).below();
            BlockState floorState = world.getBlockState(floorPos);
            if (floorState.isAir() || !floorState.getFluidState().isEmpty() || floorState.canBeReplaced()) {
                neededPaving = true;
                boolean placed = placeSolidBlock(world, floorPos);
                if (!placed) {
                    this.status = TurtleStatus.NO_TOOL; // Shows "Need Blocks!"
                    setChanged();
                    return;
                }
            }
        }

        // 2. If 5-Wide mode, place guard rails on the outer edges (-2 and +2)
        if (width == 5) {
            for (int offset : new int[] { -2, 2 }) {
                BlockPos railPos = this.worldPosition.relative(facing).relative(left, offset);
                BlockState railState = world.getBlockState(railPos);
                if (railState.isAir() || !railState.getFluidState().isEmpty() || railState.canBeReplaced()) {
                    placeRailingBlock(world, railPos);
                }
            }
        }

        // 3. Clear walkway clearance (2 blocks high for walking corridor)
        int[] walkOffsets = switch (width) {
            case 3, 5 -> new int[] { -1, 0, 1 };
            default -> new int[] { 0 };
        };

        for (int offset : walkOffsets) {
            BlockPos walkFoot = this.worldPosition.relative(facing).relative(left, offset);
            BlockPos walkHead = walkFoot.above();
            if (!world.getBlockState(walkFoot).isAir() && world.getBlockState(walkFoot).getFluidState().isEmpty()) {
                dig(world, walkFoot);
            }
            if (!world.getBlockState(walkHead).isAir() && world.getBlockState(walkHead).getFluidState().isEmpty()) {
                dig(world, walkHead);
            }
        }

        // 4. Advance forward 1 block
        TurtleBlockEntity movedTurtle = move(world, facing);
        if (movedTurtle != null) {
            movedTurtle.stepsMoved++;
            movedTurtle.progress = Math.min(100, (movedTurtle.stepsMoved * 100) / 64);

            // Auto-Touchdown: If we have moved at least 3 blocks, and the row ahead is
            // naturally solid ground across all offsets, we have bridged the gap!
            if (movedTurtle.stepsMoved >= 3 && !neededPaving) {
                boolean allAheadSolid = true;
                for (int offset : floorOffsets) {
                    BlockPos aheadFloor = movedTurtle.worldPosition.relative(facing).relative(left, offset).below();
                    BlockState s = world.getBlockState(aheadFloor);
                    if (s.isAir() || !s.getFluidState().isEmpty() || s.canBeReplaced()) {
                        allAheadSolid = false;
                        break;
                    }
                }
                if (allAheadSolid) {
                    movedTurtle.status = TurtleStatus.COMPLETED;
                    movedTurtle.progress = 100;
                    world.playSound(null, movedTurtle.worldPosition, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                            SoundSource.BLOCKS, 1.0f, 1.0f);
                    movedTurtle.setChanged();
                    return;
                }
            }

            if (movedTurtle.stepsMoved >= 64) {
                movedTurtle.status = TurtleStatus.COMPLETED;
                movedTurtle.progress = 100;
                world.playSound(null, movedTurtle.worldPosition, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                        SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            movedTurtle.setChanged();
        } else {
            this.status = TurtleStatus.BLOCKED;
            setChanged();
        }
    }

    private boolean placeSolidBlock(ServerLevel world, BlockPos targetPos) {
        // First pass: try standard solid blocks (excluding fences and walls)
        for (int i = TurtleScreenHandler.STORAGE_START; i < TurtleScreenHandler.STORAGE_START
                + TurtleScreenHandler.STORAGE_COUNT; i++) {
            ItemStack stack = this.items.get(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();
                BlockState state = block.defaultBlockState();
                if (state.is(BlockTags.FENCES) || state.is(BlockTags.WALLS)) {
                    continue;
                }
                if (state.isSolid()) {
                    world.setBlock(targetPos, state, Block.UPDATE_ALL);
                    stack.shrink(1);
                    if (stack.isEmpty()) {
                        this.items.set(i, ItemStack.EMPTY);
                    }
                    setChanged();
                    return true;
                }
            }
        }
        // Fallback: any solid block
        for (int i = TurtleScreenHandler.STORAGE_START; i < TurtleScreenHandler.STORAGE_START
                + TurtleScreenHandler.STORAGE_COUNT; i++) {
            ItemStack stack = this.items.get(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();
                BlockState state = block.defaultBlockState();
                if (state.isSolid()) {
                    world.setBlock(targetPos, state, Block.UPDATE_ALL);
                    stack.shrink(1);
                    if (stack.isEmpty()) {
                        this.items.set(i, ItemStack.EMPTY);
                    }
                    setChanged();
                    return true;
                }
            }
        }
        return false;
    }

    private boolean placeRailingBlock(ServerLevel world, BlockPos targetPos) {
        // First priority: any fence or wall
        for (int i = TurtleScreenHandler.STORAGE_START; i < TurtleScreenHandler.STORAGE_START
                + TurtleScreenHandler.STORAGE_COUNT; i++) {
            ItemStack stack = this.items.get(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();
                BlockState state = block.defaultBlockState();
                if (state.is(BlockTags.FENCES) || state.is(BlockTags.WALLS)) {
                    world.setBlock(targetPos, state, Block.UPDATE_ALL);
                    stack.shrink(1);
                    if (stack.isEmpty()) {
                        this.items.set(i, ItemStack.EMPTY);
                    }
                    setChanged();
                    return true;
                }
            }
        }
        // Fallback: any solid block
        for (int i = TurtleScreenHandler.STORAGE_START; i < TurtleScreenHandler.STORAGE_START
                + TurtleScreenHandler.STORAGE_COUNT; i++) {
            ItemStack stack = this.items.get(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();
                BlockState state = block.defaultBlockState();
                if (state.isSolid()) {
                    world.setBlock(targetPos, state, Block.UPDATE_ALL);
                    stack.shrink(1);
                    if (stack.isEmpty()) {
                        this.items.set(i, ItemStack.EMPTY);
                    }
                    setChanged();
                    return true;
                }
            }
        }
        return false;
    }

    private void executeLumberjack(ServerLevel world) {
        Direction facing = getFacing();
        Direction left = facing.getCounterClockWise();

        if (this.stepsMoved >= this.configDistance) {
            this.status = TurtleStatus.COMPLETED;
            setChanged();
            return;
        }

        // 1. Search in a radius around and in front of the turtle for any tree trunks
        // to fell
        // Forward: 0..3 blocks, Sideways: -2..2 blocks (5 blocks wide), Vertical: -1..3
        // blocks
        BlockPos closestTrunk = null;
        double minDistance = Double.MAX_VALUE;

        for (int fwd = 0; fwd <= 3; fwd++) {
            for (int side = -2; side <= 2; side++) {
                for (int dy = -1; dy <= 3; dy++) {
                    BlockPos target = this.worldPosition.relative(facing, fwd).relative(left, side).above(dy);
                    BlockState state = world.getBlockState(target);
                    if (state.is(BlockTags.LOGS)) {
                        double d = this.worldPosition.distSqr(target);
                        if (d < minDistance) {
                            minDistance = d;
                            closestTrunk = target;
                        }
                    }
                }
            }
        }

        if (closestTrunk != null) {
            fellTree(world, closestTrunk);
            setChanged();
            return; // Tree felled; next tick will check for any other nearby trees before moving
        }

        // 2. Clear leaves and non-solid foliage in path and above turtle head
        BlockPos frontPos = this.worldPosition.relative(facing);
        clearFoliage(world, this.worldPosition.above());
        clearFoliage(world, frontPos);
        clearFoliage(world, frontPos.above());

        // 3. Move forward along the path (with natural terrain hill climbing)
        TurtleBlockEntity movedTurtle = move(world, facing);
        if (movedTurtle == null) {
            // Check if there is a 1-block rise we can step up onto
            BlockPos upPos = this.worldPosition.above();
            BlockPos upTarget = frontPos.above();
            clearFoliage(world, upPos);
            clearFoliage(world, upTarget);

            if (isPassable(world, upPos) && isPassable(world, upTarget)) {
                TurtleBlockEntity upTurtle = move(world, Direction.UP);
                if (upTurtle != null) {
                    movedTurtle = upTurtle.move(world, facing);
                    if (movedTurtle == null) {
                        upTurtle.move(world, Direction.DOWN);
                    }
                }
            }
        }

        // 4. If moved forward and there is a 1-block drop, step down onto the terrain
        if (movedTurtle != null) {
            BlockPos below = movedTurtle.worldPosition.below();
            if (isPassable(world, below)) {
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
            movedTurtle.setChanged();
        } else {
            this.status = TurtleStatus.BLOCKED;
            setChanged();
        }
    }

    private boolean isPassable(ServerLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.isAir() || !state.getFluidState().isEmpty() || state.canBeReplaced())
            return true;
        return state.getCollisionShape(world, pos).isEmpty();
    }

    private void clearFoliage(ServerLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.is(BlockTags.LEAVES) || (!state.isAir() && state.getCollisionShape(world, pos).isEmpty())) {
            dig(world, pos);
        }
    }

    private void fellTree(ServerLevel world, BlockPos startPos) {
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
                        if (dx == 0 && dy == 0 && dz == 0)
                            continue;
                        BlockPos neighbor = current.offset(dx, dy, dz);

                        // Don't search deep into the ground
                        if (neighbor.getY() < startY - 1)
                            continue;
                        // Don't search too far horizontally from the trunk (within 10 blocks)
                        if (Math.abs(neighbor.getX() - startPos.getX()) > 10
                                || Math.abs(neighbor.getZ() - startPos.getZ()) > 10)
                            continue;

                        BlockState nState = world.getBlockState(neighbor);
                        if (nState.is(BlockTags.LOGS)) {
                            if (visitedLogs.add(neighbor)) {
                                queue.add(neighbor);
                            }
                        } else if (nState.is(BlockTags.LEAVES)) {
                            leavesToBreak.add(neighbor);
                        }
                    }
                }
            }
        }

        // 2. Expand leaves by up to 2 blocks around detected leaves to capture full
        // canopy
        for (int step = 0; step < 2; step++) {
            Set<BlockPos> extraLeaves = new HashSet<>();
            for (BlockPos leaf : leavesToBreak) {
                for (Direction dir : Direction.values()) {
                    BlockPos nLeaf = leaf.relative(dir);
                    if (!leavesToBreak.contains(nLeaf) && world.getBlockState(nLeaf).is(BlockTags.LEAVES)) {
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
            if (leafState.is(BlockTags.LEAVES)) {
                List<ItemStack> drops = Block.getDrops(leafState, world, leafPos, null, null, ItemStack.EMPTY);
                world.destroyBlock(leafPos, false);
                for (ItemStack drop : drops) {
                    ItemStack leftover = insertItem(drop);
                    if (!leftover.isEmpty()) {
                        Containers.dropItemStack(world, this.worldPosition.getX(), this.worldPosition.getY(),
                                this.worldPosition.getZ(), leftover);
                    }
                }
            }
        }

        // 5. Vacuum all nearby drops into the turtle
        AABB vacuumBox = new AABB(startPos).inflate(12, 16, 12);
        List<ItemEntity> drops = world.getEntitiesOfClass(ItemEntity.class, vacuumBox, e -> e.isAlive());
        for (ItemEntity drop : drops) {
            ItemStack leftover = insertItem(drop.getItem());
            if (leftover.isEmpty()) {
                drop.discard();
            } else {
                drop.setItem(leftover);
            }
        }

        world.playSound(null, startPos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0f, 0.8f);
    }

    public boolean hasWaterBucket() {
        for (int i = TurtleScreenHandler.STORAGE_START; i < TurtleScreenHandler.STORAGE_START
                + TurtleScreenHandler.STORAGE_COUNT; i++) {
            if (this.items.get(i).is(Items.WATER_BUCKET))
                return true;
        }
        return false;
    }

    private boolean placeWaterSource(ServerLevel world, BlockPos waterPos) {
        for (int i = TurtleScreenHandler.STORAGE_START; i < TurtleScreenHandler.STORAGE_START
                + TurtleScreenHandler.STORAGE_COUNT; i++) {
            ItemStack stack = this.items.get(i);
            if (stack.is(Items.WATER_BUCKET)) {
                this.items.set(i, new ItemStack(Items.BUCKET));
                world.setBlock(waterPos, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
                world.playSound(null, waterPos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
                setChanged();
                return true;
            }
        }
        return false;
    }

    private boolean isTillable(BlockState state) {
        return state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK) ||
                state.is(Blocks.DIRT_PATH) || state.is(Blocks.COARSE_DIRT) ||
                state.is(Blocks.ROOTED_DIRT);
    }

    private BlockState getCropStateForSeed(ItemStack stack) {
        if (stack.is(Items.WHEAT_SEEDS))
            return Blocks.WHEAT.defaultBlockState();
        if (stack.is(Items.CARROT))
            return Blocks.CARROTS.defaultBlockState();
        if (stack.is(Items.POTATO))
            return Blocks.POTATOES.defaultBlockState();
        if (stack.is(Items.BEETROOT_SEEDS))
            return Blocks.BEETROOTS.defaultBlockState();
        if (stack.is(Items.MELON_SEEDS))
            return Blocks.MELON_STEM.defaultBlockState();
        if (stack.is(Items.PUMPKIN_SEEDS))
            return Blocks.PUMPKIN_STEM.defaultBlockState();
        if (stack.is(Items.TORCHFLOWER_SEEDS))
            return Blocks.TORCHFLOWER_CROP.defaultBlockState();
        if (stack.is(Items.PITCHER_POD))
            return Blocks.PITCHER_CROP.defaultBlockState();
        return null;
    }

    private BlockState consumeSeedAndGetCrop() {
        for (int i = TurtleScreenHandler.STORAGE_START; i < TurtleScreenHandler.STORAGE_START
                + TurtleScreenHandler.STORAGE_COUNT; i++) {
            ItemStack stack = this.items.get(i);
            if (!stack.isEmpty()) {
                BlockState crop = getCropStateForSeed(stack);
                if (crop != null) {
                    stack.shrink(1);
                    if (stack.isEmpty()) {
                        this.items.set(i, ItemStack.EMPTY);
                    }
                    setChanged();
                    return crop;
                }
            }
        }
        return null;
    }

    private void consumeSeedForCrop(CropBlock cropBlock) {
        Item seedItem = cropBlock.asItem();
        for (int i = TurtleScreenHandler.STORAGE_START; i < TurtleScreenHandler.STORAGE_START
                + TurtleScreenHandler.STORAGE_COUNT; i++) {
            ItemStack stack = this.items.get(i);
            if (stack.is(seedItem)) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    this.items.set(i, ItemStack.EMPTY);
                }
                setChanged();
                return;
            }
        }
    }

    private void executeFarmCreator(ServerLevel world) {
        if (!hasRequiredTool(AppType.FARM_CREATOR)) {
            this.status = TurtleStatus.NO_TOOL;
            setChanged();
            return;
        }

        int targetFarms = Math.min(3, Math.max(1, this.configDistance));

        // 1. Center water block placement
        if (this.farmRow == -1) {
            BlockPos waterPos = this.worldPosition.below();
            if (world.getBlockState(waterPos).is(Blocks.WATER)) {
                this.farmRow = 0;
                setChanged();
                return;
            }

            if (!consumeFuel(1))
                return;
            dig(world, waterPos);
            if (placeWaterSource(world, waterPos)) {
                this.farmRow = 0;
                setChanged();
            } else {
                this.status = TurtleStatus.NO_TOOL; // "Need Water!"
                setChanged();
            }
            return;
        }

        // 2. Till and plant row by row (dz = farmRow - 4, from -4 to +4)
        if (this.farmRow >= 0 && this.farmRow <= 8) {
            if (!consumeFuel(1))
                return;

            Direction facing = getFacing();
            Direction left = facing.getCounterClockWise();
            int dz = this.farmRow - 4;

            for (int dx = -4; dx <= 4; dx++) {
                if (dx == 0 && dz == 0)
                    continue; // Skip center water block under turtle

                BlockPos groundPos = this.worldPosition.relative(facing, dz).relative(left, dx).below();
                BlockPos cropPos = groundPos.above();

                // Clear surface obstruction (grass, flowers, snow, dirt bumps, etc.)
                BlockState cropState = world.getBlockState(cropPos);
                if (!cropState.isAir()
                        && (cropState.getCollisionShape(world, cropPos).isEmpty() || cropState.canBeReplaced()
                                || cropState.is(Blocks.DIRT) || cropState.is(Blocks.GRASS_BLOCK))) {
                    dig(world, cropPos);
                }

                // Till ground into farmland
                BlockState gState = world.getBlockState(groundPos);
                if (isTillable(gState)) {
                    world.setBlock(groundPos, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
                    damageTool();
                    world.playSound(null, groundPos, SoundEvents.HOE_TILL.value(), SoundSource.BLOCKS, 0.8f, 1.0f);
                }

                // Plant seed if ground is farmland and air above is clear
                if (world.getBlockState(groundPos).is(Blocks.FARMLAND) && world.getBlockState(cropPos).isAir()) {
                    BlockState seedCrop = consumeSeedAndGetCrop();
                    if (seedCrop != null) {
                        world.setBlock(cropPos, seedCrop, Block.UPDATE_ALL);
                        world.playSound(null, cropPos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 0.8f, 1.0f);
                    }
                }
            }

            int totalUnits = targetFarms * 10;
            int currentUnits = this.farmIndex * 10 + (this.farmRow + 1);
            this.progress = Math.min(100, (currentUnits * 100) / totalUnits);
            this.farmRow++;
            setChanged();
            return;
        }

        // 3. Advance to next farm center (9 blocks forward) or complete
        if (this.farmRow == 9) {
            if (this.farmIndex + 1 < targetFarms) {
                if (!hasWaterBucket()) {
                    this.status = TurtleStatus.NO_TOOL; // "Need Water!"
                    setChanged();
                    return;
                }

                Direction facing = getFacing();

                // On start of advance, lift UP 1 block to hover safely above the newly planted
                // crops
                if (this.farmMoveStep == 0 && !world.getBlockState(this.worldPosition.below()).isAir()) {
                    TurtleBlockEntity up = move(world, Direction.UP);
                    if (up != null) {
                        up.setChanged();
                    }
                    return;
                }

                // Try to move forward 1 block (hovering)
                TurtleBlockEntity movedTurtle = move(world, facing);

                // If forward is blocked by elevated terrain, step UP 1 block to climb the hill!
                if (movedTurtle == null) {
                    TurtleBlockEntity up = move(world, Direction.UP);
                    if (up != null) {
                        movedTurtle = up.move(world, facing);
                    }
                }

                if (movedTurtle != null) {
                    movedTurtle.farmMoveStep++;
                    if (movedTurtle.farmMoveStep >= 9) {
                        // Arrived at the new farm center 9 blocks forward!
                        // Descend until resting directly on top of solid ground
                        TurtleBlockEntity landedTurtle = movedTurtle;
                        for (int d = 0; d < 5 && world.getBlockState(landedTurtle.worldPosition.below()).isAir(); d++) {
                            TurtleBlockEntity down = landedTurtle.move(world, Direction.DOWN);
                            if (down == null)
                                break;
                            landedTurtle = down;
                        }

                        landedTurtle.farmMoveStep = 0;
                        landedTurtle.farmIndex++;
                        landedTurtle.farmRow = -1; // Place water for next farm!
                        landedTurtle.setChanged();
                    } else {
                        movedTurtle.setChanged();
                    }
                } else {
                    this.status = TurtleStatus.BLOCKED;
                    setChanged();
                }
            } else {
                this.status = TurtleStatus.COMPLETED;
                this.progress = 100;
                world.playSound(null, this.worldPosition, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS,
                        1.0f, 1.0f);
                setChanged();
            }
        }
    }

    private void executeFarmer(ServerLevel world) {
        Direction facing = getFacing();
        Direction left = facing.getCounterClockWise();

        if (!hasRequiredTool(AppType.FARMER)) {
            this.status = TurtleStatus.NO_TOOL;
            setChanged();
            return;
        }

        // 1. Hover lift on initial start: if sitting directly on ground/farmland, lift
        // up 1 block above crops
        if (this.stepsMoved == 0) {
            BlockState below = world.getBlockState(this.worldPosition.below());
            if (below.isSolid() || below.is(Blocks.FARMLAND)) {
                TurtleBlockEntity up = move(world, Direction.UP);
                if (up != null) {
                    up.executeFarmer(world);
                }
                return;
            }
        }

        // 2. Scan 9-wide swath below hovering turtle
        boolean foundAnyCropsOrFarmland = false;
        for (int offset = -4; offset <= 4; offset++) {
            BlockPos p1 = this.worldPosition.relative(left, offset).below();
            BlockPos p2 = p1.below();

            BlockPos targetCrop = null;
            BlockState cropState = null;

            if (world.getBlockState(p1).getBlock() instanceof CropBlock) {
                targetCrop = p1;
                cropState = world.getBlockState(p1);
            } else if (world.getBlockState(p2).getBlock() instanceof CropBlock) {
                targetCrop = p2;
                cropState = world.getBlockState(p2);
            }

            if (targetCrop != null && cropState != null && cropState.getBlock() instanceof CropBlock cropBlock) {
                foundAnyCropsOrFarmland = true;
                if (cropBlock.isMaxAge(cropState)) {
                    dig(world, targetCrop);
                    world.setBlock(targetCrop, cropBlock.getStateForAge(0), Block.UPDATE_ALL);
                    consumeSeedForCrop(cropBlock);
                    damageTool();
                    world.playSound(null, targetCrop, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 0.8f, 1.0f);
                }
            } else if (world.getBlockState(p1).is(Blocks.FARMLAND) || world.getBlockState(p2).is(Blocks.FARMLAND)) {
                foundAnyCropsOrFarmland = true;
            }
        }

        // 3. Vacuum drops in 5-block radius into turtle
        AABB vacuumBox = new AABB(this.worldPosition).inflate(5, 3, 5);
        List<ItemEntity> drops = world.getEntitiesOfClass(ItemEntity.class, vacuumBox, e -> e.isAlive());
        for (ItemEntity drop : drops) {
            ItemStack leftover = insertItem(drop.getItem());
            if (leftover.isEmpty()) {
                drop.discard();
            } else {
                drop.setItem(leftover);
            }
        }

        // 4. Auto-stop if we left the farm (no crops or farmland detected after
        // advancing into the field)
        if (this.stepsMoved >= 2 && !foundAnyCropsOrFarmland) {
            this.status = TurtleStatus.COMPLETED;
            this.progress = 100;
            world.playSound(null, this.worldPosition, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.0f,
                    1.0f);
            setChanged();
            return;
        }

        // 5. Move forward 1 block (hovering)
        // Check if the terrace ahead rises up:
        // If there are crops or farmland directly ahead at the turtle's hover height,
        // we must step UP 1 block first so we hover above the elevated crops!
        BlockPos frontPos = this.worldPosition.relative(facing);
        BlockState frontState = world.getBlockState(frontPos);
        boolean frontHasElevatedCrops = frontState.getBlock() instanceof CropBlock || frontState.is(Blocks.FARMLAND);

        if (!frontHasElevatedCrops) {
            for (int offset = -4; offset <= 4; offset++) {
                BlockState s = world.getBlockState(frontPos.relative(left, offset));
                if (s.getBlock() instanceof CropBlock || s.is(Blocks.FARMLAND)) {
                    frontHasElevatedCrops = true;
                    break;
                }
            }
        }

        TurtleBlockEntity activeTurtle = this;
        if (frontHasElevatedCrops) {
            TurtleBlockEntity up = activeTurtle.move(world, Direction.UP);
            if (up != null) {
                activeTurtle = up;
            }
        }

        TurtleBlockEntity movedTurtle = activeTurtle.move(world, facing);
        if (movedTurtle == null) {
            // Check if terrain stepped up 1 block (e.g. wall/cliff)
            TurtleBlockEntity up = activeTurtle.move(world, Direction.UP);
            if (up != null) {
                movedTurtle = up.move(world, facing);
            }
        }

        if (movedTurtle != null) {
            // If the terrace stepped down (both 1 and 2 blocks below are air), adjust hover
            // down
            if (world.getBlockState(movedTurtle.worldPosition.below()).isAir()
                    && world.getBlockState(movedTurtle.worldPosition.below().below()).isAir()) {
                TurtleBlockEntity down = movedTurtle.move(world, Direction.DOWN);
                if (down != null) {
                    movedTurtle = down;
                }
            }

            movedTurtle.stepsMoved++;
            movedTurtle.progress = Math.min(100, (movedTurtle.stepsMoved * 100) / movedTurtle.configDistance);
            if (movedTurtle.stepsMoved >= movedTurtle.configDistance) {
                movedTurtle.status = TurtleStatus.COMPLETED;
                movedTurtle.progress = 100;
                world.playSound(null, movedTurtle.worldPosition, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                        SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            movedTurtle.setChanged();
        } else {
            this.status = TurtleStatus.BLOCKED;
            setChanged();
        }
    }

    private float getSwordDamage(ItemStack sword) {
        if (sword.is(Items.NETHERITE_SWORD))
            return 8.0f;
        if (sword.is(Items.DIAMOND_SWORD))
            return 7.0f;
        if (sword.is(Items.IRON_SWORD))
            return 6.0f;
        if (sword.is(Items.STONE_SWORD))
            return 5.0f;
        return 4.0f;
    }

    private void executeSentry(ServerLevel world) {
        if (!hasRequiredTool(AppType.SENTRY)) {
            this.status = TurtleStatus.NO_TOOL;
            setChanged();
            return;
        }

        int radius = Math.max(3, Math.min(10, this.configDistance));
        AABB searchBox = new AABB(this.worldPosition).inflate(radius, 3, radius);
        List<Mob> monsters = world.getEntitiesOfClass(Mob.class, searchBox, e -> e.isAlive() && e instanceof Monster);

        if (!monsters.isEmpty()) {
            Mob target = monsters.get(0);
            if (consumeFuel(1)) {
                ItemStack tool = getToolStack();
                float dmg = getSwordDamage(tool);
                target.hurtServer((ServerLevel) world, world.damageSources().generic(), dmg);
                damageTool();
                world.playSound(null, this.worldPosition, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.BLOCKS, 1.0f,
                        1.0f);

                // Suck up nearby item drops
                List<ItemEntity> items = world.getEntitiesOfClass(ItemEntity.class, searchBox, e -> e.isAlive());
                for (ItemEntity drop : items) {
                    ItemStack leftover = insertItem(drop.getItem());
                    if (leftover.isEmpty()) {
                        drop.discard();
                    } else {
                        drop.setItem(leftover);
                    }
                }
                setChanged();
            }
        }
    }

    private void executeRoomCarver(ServerLevel world) {
        if (!hasRequiredTool(AppType.ROOM_CARVER)) {
            this.status = TurtleStatus.NO_TOOL;
            setChanged();
            return;
        }

        if (this.excavatorSlice > this.roomLength) {
            this.status = TurtleStatus.COMPLETED;
            this.progress = 100;
            world.playSound(null, this.worldPosition, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.0f,
                    1.0f);
            setChanged();
            return;
        }

        Direction facing = getFacing();
        Direction right = facing.getClockWise();

        int minDx = -(this.roomWidth - 1) / 2;
        int maxDx = minDx + this.roomWidth - 1;
        int totalBlocks = this.roomWidth * this.roomHeight * this.roomLength;

        // Skip up to 16 air/fluid blocks in one action to avoid idling across empty
        // space
        for (int step = 0; step < 16; step++) {
            if (this.excavatorSlice > this.roomLength) {
                this.status = TurtleStatus.COMPLETED;
                this.progress = 100;
                world.playSound(null, this.worldPosition, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS,
                        1.0f, 1.0f);
                setChanged();
                return;
            }

            BlockPos targetPos = this.worldPosition.relative(facing, this.excavatorSlice)
                    .above(this.excavatorY)
                    .relative(right, this.excavatorX);

            BlockState state = world.getBlockState(targetPos);
            boolean isBlockSolid = !state.isAir() && state.getFluidState().isEmpty();

            if (isBlockSolid) {
                if (!consumeFuel(1))
                    return;

                if (!dig(world, targetPos)) {
                    if (this.status != TurtleStatus.NO_TOOL) {
                        this.status = TurtleStatus.BLOCKED;
                    }
                    setChanged();
                    return;
                }

                // If falling blocks (sand/gravel) fell into this position
                int fallingGuard = 0;
                while (world.getBlockState(targetPos).getBlock() instanceof FallingBlock && fallingGuard < 10) {
                    dig(world, targetPos);
                    fallingGuard++;
                }

                advanceRoomCarverCoordinates(minDx, maxDx, totalBlocks, world);
                setChanged();
                return; // 1 solid block mined per action cooldown
            }

            advanceRoomCarverCoordinates(minDx, maxDx, totalBlocks, world);
        }
        setChanged();
    }

    private void advanceRoomCarverCoordinates(int minDx, int maxDx, int totalBlocks, ServerLevel world) {
        this.excavatorX++;
        if (this.excavatorX > maxDx) {
            this.excavatorX = minDx;
            this.excavatorY--;
            if (this.excavatorY < 0) {
                // Completed slice
                if (this.excavatorSlice % 5 == 0 || this.excavatorSlice == this.roomLength) {
                    placeRoomTorch(world, this.excavatorSlice);
                }
                vacuumRoomDrops(world);
                this.excavatorY = this.roomHeight - 1;
                this.excavatorSlice++;
            }
        }

        long completedBlocks = (long) (this.excavatorSlice - 1) * this.roomHeight * this.roomWidth
                + (long) (this.roomHeight - 1 - this.excavatorY) * this.roomWidth
                + (long) (this.excavatorX - minDx);
        this.progress = totalBlocks > 0 ? (int) Math.min(100, Math.max(0, (completedBlocks * 100L) / totalBlocks)) : 0;
    }

    private void placeRoomTorch(ServerLevel world, int slice) {
        Direction facing = getFacing();
        BlockPos torchPos = this.worldPosition.relative(facing, slice);
        BlockState floorState = world.getBlockState(torchPos.below());

        if (world.getBlockState(torchPos).isAir() && floorState.isSolid()) {
            for (int i = TurtleScreenHandler.STORAGE_START; i < TurtleScreenHandler.STORAGE_START
                    + TurtleScreenHandler.STORAGE_COUNT; i++) {
                ItemStack stack = this.items.get(i);
                if (!stack.isEmpty() && (stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH))) {
                    BlockState torchState = stack.is(Items.SOUL_TORCH)
                            ? Blocks.SOUL_TORCH.defaultBlockState()
                            : Blocks.TORCH.defaultBlockState();
                    world.setBlock(torchPos, torchState, Block.UPDATE_ALL);
                    world.playSound(null, torchPos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
                    stack.shrink(1);
                    setChanged();
                    break;
                }
            }
        }
    }

    private void vacuumRoomDrops(ServerLevel world) {
        Direction facing = getFacing();
        Direction right = facing.getClockWise();
        int minDx = -(this.roomWidth - 1) / 2;
        int maxDx = minDx + this.roomWidth - 1;

        BlockPos corner1 = this.worldPosition.relative(facing, 1).relative(right, minDx);
        BlockPos corner2 = this.worldPosition.relative(facing, this.roomLength).above(this.roomHeight).relative(right,
                maxDx);
        AABB box = new AABB(corner1).minmax(new AABB(corner2)).inflate(1.0);

        List<ItemEntity> items = world.getEntitiesOfClass(ItemEntity.class, box, e -> e.isAlive());
        for (ItemEntity itemEntity : items) {
            ItemStack leftover = insertItem(itemEntity.getItem());
            if (leftover.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(leftover);
            }
        }
    }

    private int findClosestRoomPresetIndex() {
        for (int i = 0; i < ROOM_PRESETS.length; i++) {
            if (ROOM_PRESETS[i][0] == this.roomWidth &&
                    ROOM_PRESETS[i][1] == this.roomHeight &&
                    ROOM_PRESETS[i][2] == this.roomLength) {
                return i;
            }
        }
        int currentVol = this.roomWidth * this.roomHeight * this.roomLength;
        int closestIdx = 1;
        int minDiff = Integer.MAX_VALUE;
        for (int i = 0; i < ROOM_PRESETS.length; i++) {
            int presetVol = ROOM_PRESETS[i][0] * ROOM_PRESETS[i][1] * ROOM_PRESETS[i][2];
            int diff = Math.abs(presetVol - currentVol);
            if (diff < minDiff) {
                minDiff = diff;
                closestIdx = i;
            }
        }
        return closestIdx;
    }

    private void adjustRoomDimensions(int delta) {
        if (this.roomConfigMode == 0) { // Preset mode
            int idx = findClosestRoomPresetIndex();
            int newIdx = Math.max(0, Math.min(ROOM_PRESETS.length - 1, idx + delta));
            this.roomWidth = ROOM_PRESETS[newIdx][0];
            this.roomHeight = ROOM_PRESETS[newIdx][1];
            this.roomLength = ROOM_PRESETS[newIdx][2];
        } else if (this.roomConfigMode == 1) { // Width mode (1 .. 32)
            this.roomWidth = Math.max(1, Math.min(32, this.roomWidth + delta));
        } else if (this.roomConfigMode == 2) { // Height mode (1 .. 10)
            this.roomHeight = Math.max(1, Math.min(10, this.roomHeight + delta));
        } else if (this.roomConfigMode == 3) { // Length mode (1 .. 32)
            this.roomLength = Math.max(1, Math.min(32, this.roomLength + delta));
        }
        resetRoomCarverCoordinates();
        setChanged();
    }

    private void resetRoomCarverCoordinates() {
        this.excavatorSlice = 1;
        this.excavatorY = this.roomHeight - 1;
        this.excavatorX = -(this.roomWidth - 1) / 2;
    }

    // --- INVENTORY IMPLEMENTATION ---

    @Override
    public int getContainerSize() {
        return TOTAL_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty())
                return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(this.items, slot, amount);
        if (!result.isEmpty())
            setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = ContainerHelper.takeItem(this.items, slot);
        if (!result.isEmpty())
            setChanged();
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        if (slot == TurtleScreenHandler.DISK_SLOT && this.status != TurtleStatus.RUNNING) {
            if (stack.getItem() instanceof FloppyDiskItem diskItem && diskItem.getAppType() != AppType.NONE) {
                this.configDistance = diskItem.getAppType().getDefaultDistance();
                this.stepsMoved = 0;
                this.progress = 0;
                this.quarryLayer = 0;
                this.quarryIndex = 0;
                this.farmIndex = 0;
                this.farmRow = -1;
                this.farmMoveStep = 0;
                this.roomWidth = 5;
                this.roomHeight = 5;
                this.roomLength = 5;
                this.roomConfigMode = 0;
                resetRoomCarverCoordinates();
                this.status = TurtleStatus.IDLE;
            }
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.items.clear();
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Turtle");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        TurtleScreenHandler.DelegatingInventory inv = new TurtleScreenHandler.DelegatingInventory(this);
        TurtleScreenHandler.DelegatingPropertyDelegate props = new TurtleScreenHandler.DelegatingPropertyDelegate(
                this.propertyDelegate);
        TurtleScreenHandler handler = new TurtleScreenHandler(syncId, playerInventory, inv, props);
        handler.setTurtle(this);
        return handler;
    }

    // --- DATA SERIALIZATION ---

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.putInt("fuel", this.fuel);
        output.putInt("maxFuel", this.maxFuel);
        output.putInt("status", this.status.getId());
        output.putInt("configDistance", this.configDistance);
        output.putInt("stepsMoved", this.stepsMoved);
        output.putInt("progress", this.progress);
        output.putInt("quarryLayer", this.quarryLayer);
        output.putInt("quarryIndex", this.quarryIndex);
        output.putInt("farmIndex", this.farmIndex);
        output.putInt("farmRow", this.farmRow);
        output.putInt("farmMoveStep", this.farmMoveStep);
        output.putInt("roomWidth", this.roomWidth);
        output.putInt("roomHeight", this.roomHeight);
        output.putInt("roomLength", this.roomLength);
        output.putInt("roomConfigMode", this.roomConfigMode);
        output.putInt("excavatorSlice", this.excavatorSlice);
        output.putInt("excavatorY", this.excavatorY);
        output.putInt("excavatorX", this.excavatorX);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        this.fuel = input.getIntOr("fuel", 0);
        this.maxFuel = input.getIntOr("maxFuel", 10000);
        this.status = TurtleStatus.fromIndex(input.getIntOr("status", 0));
        this.configDistance = input.getIntOr("configDistance", 16);
        this.stepsMoved = input.getIntOr("stepsMoved", 0);
        this.progress = input.getIntOr("progress", 0);
        this.quarryLayer = input.getIntOr("quarryLayer", 0);
        this.quarryIndex = input.getIntOr("quarryIndex", 0);
        this.farmIndex = input.getIntOr("farmIndex", 0);
        this.farmRow = input.getIntOr("farmRow", -1);
        this.farmMoveStep = input.getIntOr("farmMoveStep", 0);
        this.roomWidth = input.getIntOr("roomWidth", 5);
        this.roomHeight = input.getIntOr("roomHeight", 5);
        this.roomLength = input.getIntOr("roomLength", 5);
        this.roomConfigMode = input.getIntOr("roomConfigMode", 0);
        this.excavatorSlice = input.getIntOr("excavatorSlice", 1);
        this.excavatorY = input.getIntOr("excavatorY", 4);
        this.excavatorX = input.getIntOr("excavatorX", -2);
    }
}