package com.turtlemod.app;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

public enum AppType {
    NONE("none", "No Program Installed", "Insert a programmed floppy disk.", Items.AIR, 0),
    MINER("miner", "3x3 Tunnel Miner", "Excavates a 3x3 tunnel ahead and places torches every 7 blocks.", Items.IRON_PICKAXE, 16),
    QUARRY("quarry", "Quarry 3x3", "Digs a 3x3 shaft straight down layer-by-layer.", Items.DIAMOND_PICKAXE, 20),
    BUILDER("builder", "Bridge Builder", "Paves bridges across gaps using blocks from inventory.", Items.STONE_BRICKS, 1),
    LUMBERJACK("lumberjack", "Tree Feller", "Chops down trees from base to canopy and collects wood.", Items.IRON_AXE, 16),
    FARMER("farmer", "Crop Harvester", "Sweeps a 9-wide swath, harvesting and replanting crops.", Items.IRON_HOE, 9),
    FARM_CREATOR("farm_creator", "Farm Creator", "Constructs a hydrated 9x9 tilled and seeded farm.", Items.WATER_BUCKET, 1),
    SENTRY("sentry", "Mob Sentry", "Patrols area and attacks hostile monsters with equipped weapon.", Items.IRON_SWORD, 10);

    private final String id;
    private final String displayName;
    private final String description;
    private final Item iconItem;
    private final int defaultDistance;

    AppType(String id, String displayName, String description, Item iconItem, int defaultDistance) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.iconItem = iconItem;
        this.defaultDistance = defaultDistance;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public Item getIconItem() {
        return iconItem;
    }

    public int getDefaultDistance() {
        return defaultDistance;
    }

    public static AppType fromIndex(int index) {
        AppType[] values = values();
        if (index >= 0 && index < values.length) {
            return values[index];
        }
        return NONE;
    }
}
