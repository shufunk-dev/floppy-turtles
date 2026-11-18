package com.turtlemod;

import com.turtlemod.app.AppType;
import com.turtlemod.block.TurtleBlock;
import com.turtlemod.block.entity.TurtleBlockEntity;
import com.turtlemod.item.FloppyDiskItem;
import com.turtlemod.screen.TurtleScreenHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TurtleMod implements ModInitializer {
    public static final String MOD_ID = "turtle";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Block & Item
    public static final Block TURTLE_BLOCK = new TurtleBlock(AbstractBlock.Settings.create().strength(2.5f).requiresTool());
    public static final Item TURTLE_BLOCK_ITEM = new BlockItem(TURTLE_BLOCK, new Item.Settings());

    // Block Entity
    public static BlockEntityType<TurtleBlockEntity> TURTLE_BLOCK_ENTITY;

    // Screen Handler
    public static ScreenHandlerType<TurtleScreenHandler> TURTLE_SCREEN_HANDLER;

    // Floppy Disks
    public static final Item DISK_BLANK = new FloppyDiskItem(new Item.Settings().maxCount(16), AppType.NONE);
    public static final Item DISK_MINER = new FloppyDiskItem(new Item.Settings().maxCount(1), AppType.MINER);
    public static final Item DISK_QUARRY = new FloppyDiskItem(new Item.Settings().maxCount(1), AppType.QUARRY);
    public static final Item DISK_BUILDER = new FloppyDiskItem(new Item.Settings().maxCount(1), AppType.BUILDER);
    public static final Item DISK_LUMBERJACK = new FloppyDiskItem(new Item.Settings().maxCount(1), AppType.LUMBERJACK);
    public static final Item DISK_FARMER = new FloppyDiskItem(new Item.Settings().maxCount(1), AppType.FARMER);
    public static final Item DISK_SENTRY = new FloppyDiskItem(new Item.Settings().maxCount(1), AppType.SENTRY);

    // Item Group
    public static final RegistryKey<ItemGroup> TURTLE_ITEM_GROUP_KEY = RegistryKey.of(Registries.ITEM_GROUP.getKey(), Identifier.of(MOD_ID, "general"));
    public static final ItemGroup TURTLE_ITEM_GROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(TURTLE_BLOCK_ITEM))
            .displayName(Text.translatable("itemGroup.turtle"))
            .entries((context, entries) -> {
                entries.add(TURTLE_BLOCK_ITEM);
                entries.add(DISK_BLANK);
                entries.add(DISK_MINER);
                entries.add(DISK_QUARRY);
                entries.add(DISK_BUILDER);
                entries.add(DISK_LUMBERJACK);
                entries.add(DISK_FARMER);
                entries.add(DISK_SENTRY);
            })
            .build();

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Turtle Mod for Minecraft 1.21.11");

        // Register Block & Item
        Registry.register(Registries.BLOCK, Identifier.of(MOD_ID, "turtle"), TURTLE_BLOCK);
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "turtle"), TURTLE_BLOCK_ITEM);

        // Register Block Entity
        TURTLE_BLOCK_ENTITY = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                Identifier.of(MOD_ID, "turtle_be"),
                FabricBlockEntityTypeBuilder.create(TurtleBlockEntity::new, TURTLE_BLOCK).build()
        );

        // Register Screen Handler
        TURTLE_SCREEN_HANDLER = Registry.register(
                Registries.SCREEN_HANDLER,
                Identifier.of(MOD_ID, "turtle_screen"),
                new ScreenHandlerType<>(TurtleScreenHandler::new, FeatureSet.empty())
        );

        // Register Disks
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "floppy_disk_blank"), DISK_BLANK);
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "floppy_disk_miner"), DISK_MINER);
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "floppy_disk_quarry"), DISK_QUARRY);
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "floppy_disk_builder"), DISK_BUILDER);
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "floppy_disk_lumberjack"), DISK_LUMBERJACK);
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "floppy_disk_farmer"), DISK_FARMER);
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "floppy_disk_sentry"), DISK_SENTRY);

        // Register Creative Tab
        Registry.register(Registries.ITEM_GROUP, TURTLE_ITEM_GROUP_KEY, TURTLE_ITEM_GROUP);
    }
}
