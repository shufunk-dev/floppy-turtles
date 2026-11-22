package com.turtlemod;

import com.turtlemod.app.AppType;
import com.turtlemod.block.TurtleBlock;
import com.turtlemod.block.entity.TurtleBlockEntity;
import com.turtlemod.item.FloppyDiskItem;
import com.turtlemod.screen.TurtleScreenHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class TurtleMod implements ModInitializer {
    public static final String MOD_ID = "turtle";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Registry Keys
    public static final ResourceKey<Block> TURTLE_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "turtle"));
    public static final ResourceKey<Item> TURTLE_BLOCK_ITEM_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "turtle"));

    public static final ResourceKey<Item> DISK_BLANK_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "floppy_disk_blank"));
    public static final ResourceKey<Item> DISK_MINER_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "floppy_disk_miner"));
    public static final ResourceKey<Item> DISK_QUARRY_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "floppy_disk_quarry"));
    public static final ResourceKey<Item> DISK_BUILDER_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "floppy_disk_builder"));
    public static final ResourceKey<Item> DISK_LUMBERJACK_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "floppy_disk_lumberjack"));
    public static final ResourceKey<Item> DISK_FARMER_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "floppy_disk_farmer"));
    public static final ResourceKey<Item> DISK_FARM_CREATOR_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "floppy_disk_farm_creator"));
    public static final ResourceKey<Item> DISK_ROOM_CARVER_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "floppy_disk_room_carver"));
    public static final ResourceKey<Item> DISK_SENTRY_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "floppy_disk_sentry"));

    // Block & Item
    public static final Block TURTLE_BLOCK = new TurtleBlock(
            BlockBehaviour.Properties.of()
                    .setId(TURTLE_BLOCK_KEY)
                    .strength(2.5f)
                    .requiresCorrectToolForDrops()
    );
    public static final Item TURTLE_BLOCK_ITEM = new BlockItem(
            TURTLE_BLOCK,
            new Item.Properties()
                    .setId(TURTLE_BLOCK_ITEM_KEY)
                    .useBlockDescriptionPrefix()
    );

    // Block Entity
    public static BlockEntityType<TurtleBlockEntity> TURTLE_BLOCK_ENTITY;

    // Screen Handler
    public static MenuType<TurtleScreenHandler> TURTLE_SCREEN_HANDLER;

    // Floppy Disks
    public static final Item DISK_BLANK = new FloppyDiskItem(new Item.Properties().setId(DISK_BLANK_KEY).stacksTo(16), AppType.NONE);
    public static final Item DISK_MINER = new FloppyDiskItem(new Item.Properties().setId(DISK_MINER_KEY).stacksTo(1), AppType.MINER);
    public static final Item DISK_QUARRY = new FloppyDiskItem(new Item.Properties().setId(DISK_QUARRY_KEY).stacksTo(1), AppType.QUARRY);
    public static final Item DISK_BUILDER = new FloppyDiskItem(new Item.Properties().setId(DISK_BUILDER_KEY).stacksTo(1), AppType.BUILDER);
    public static final Item DISK_LUMBERJACK = new FloppyDiskItem(new Item.Properties().setId(DISK_LUMBERJACK_KEY).stacksTo(1), AppType.LUMBERJACK);
    public static final Item DISK_FARMER = new FloppyDiskItem(new Item.Properties().setId(DISK_FARMER_KEY).stacksTo(1), AppType.FARMER);
    public static final Item DISK_FARM_CREATOR = new FloppyDiskItem(new Item.Properties().setId(DISK_FARM_CREATOR_KEY).stacksTo(1), AppType.FARM_CREATOR);
    public static final Item DISK_ROOM_CARVER = new FloppyDiskItem(new Item.Properties().setId(DISK_ROOM_CARVER_KEY).stacksTo(1), AppType.ROOM_CARVER);
    public static final Item DISK_SENTRY = new FloppyDiskItem(new Item.Properties().setId(DISK_SENTRY_KEY).stacksTo(1), AppType.SENTRY);

    // Item Group
    public static final ResourceKey<CreativeModeTab> TURTLE_ITEM_GROUP_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(MOD_ID, "general"));
    public static final CreativeModeTab TURTLE_ITEM_GROUP = FabricCreativeModeTab.builder()
            .icon(() -> new ItemStack(TURTLE_BLOCK_ITEM))
            .title(Component.translatable("itemGroup.turtle"))
            .displayItems((itemDisplayParameters, output) -> {
                output.accept(TURTLE_BLOCK_ITEM);
                output.accept(DISK_BLANK);
                output.accept(DISK_MINER);
                output.accept(DISK_QUARRY);
                output.accept(DISK_BUILDER);
                output.accept(DISK_LUMBERJACK);
                output.accept(DISK_FARMER);
                output.accept(DISK_FARM_CREATOR);
                output.accept(DISK_ROOM_CARVER);
                output.accept(DISK_SENTRY);
            })
            .build();

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Floppy Turtles for Minecraft 26.3");

        // Register Block & Item
        Registry.register(BuiltInRegistries.BLOCK, TURTLE_BLOCK_KEY, TURTLE_BLOCK);
        Registry.register(BuiltInRegistries.ITEM, TURTLE_BLOCK_ITEM_KEY, TURTLE_BLOCK_ITEM);

        // Register Block Entity
        TURTLE_BLOCK_ENTITY = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(MOD_ID, "turtle_be"),
                new BlockEntityType<>(TurtleBlockEntity::new, Set.of(TURTLE_BLOCK))
        );

        // Register Screen Handler / MenuType
        TURTLE_SCREEN_HANDLER = Registry.register(
                BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath(MOD_ID, "turtle_screen"),
                new MenuType<>(TurtleScreenHandler::new, FeatureFlagSet.of())
        );

        // Register Disks
        Registry.register(BuiltInRegistries.ITEM, DISK_BLANK_KEY, DISK_BLANK);
        Registry.register(BuiltInRegistries.ITEM, DISK_MINER_KEY, DISK_MINER);
        Registry.register(BuiltInRegistries.ITEM, DISK_QUARRY_KEY, DISK_QUARRY);
        Registry.register(BuiltInRegistries.ITEM, DISK_BUILDER_KEY, DISK_BUILDER);
        Registry.register(BuiltInRegistries.ITEM, DISK_LUMBERJACK_KEY, DISK_LUMBERJACK);
        Registry.register(BuiltInRegistries.ITEM, DISK_FARMER_KEY, DISK_FARMER);
        Registry.register(BuiltInRegistries.ITEM, DISK_FARM_CREATOR_KEY, DISK_FARM_CREATOR);
        Registry.register(BuiltInRegistries.ITEM, DISK_ROOM_CARVER_KEY, DISK_ROOM_CARVER);
        Registry.register(BuiltInRegistries.ITEM, DISK_SENTRY_KEY, DISK_SENTRY);

        // Register Creative Tab
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TURTLE_ITEM_GROUP_KEY, TURTLE_ITEM_GROUP);
    }
}
