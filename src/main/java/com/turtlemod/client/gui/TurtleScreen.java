package com.turtlemod.client.gui;

import com.turtlemod.app.AppType;
import com.turtlemod.app.TurtleStatus;
import com.turtlemod.screen.TurtleScreenHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class TurtleScreen extends AbstractContainerScreen<TurtleScreenHandler> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("turtle", "textures/gui/turtle_gui.png");

    public TurtleScreen(TurtleScreenHandler menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = 166 - 94;
    }

    @Override
    protected void init() {
        super.init();
        int startX = this.leftPos;
        int startY = this.topPos;

        // Button 2: Param - (distance - 4)
        this.addRenderableWidget(Button.builder(Component.literal("-"), btn -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 2);
            }
        }).bounds(startX + 38, startY + 35, 14, 14).build());

        // Button 3: Param + (distance + 4)
        this.addRenderableWidget(Button.builder(Component.literal("+"), btn -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 3);
            }
        }).bounds(startX + 92, startY + 35, 14, 14).build());

        // Button 0: Start
        this.addRenderableWidget(Button.builder(Component.literal("START"), btn -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
            }
        }).bounds(startX + 38, startY + 52, 33, 16).build());

        // Button 1: Stop
        this.addRenderableWidget(Button.builder(Component.literal("STOP"), btn -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
            }
        }).bounds(startX + 73, startY + 52, 33, 16).build());
    }

    @Override
    public void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        int startX = this.leftPos;
        int startY = this.topPos;
        extractor.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, startX, startY, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // Fuel Bar: in top inset box (startX + 40, startY + 16, width: 64, height: 6)
        int fuel = this.menu.getFuel();
        int maxFuel = this.menu.getMaxFuel();
        int fuelBarWidth = Math.min(64, (int) ((fuel / (float) maxFuel) * 64));

        // Fuel bar background
        extractor.fill(startX + 40, startY + 16, startX + 104, startY + 22, 0xFF262626);
        // Fuel bar filled with orange
        if (fuelBarWidth > 0) {
            extractor.fill(startX + 40, startY + 16, startX + 40 + fuelBarWidth, startY + 22, 0xFFFF7700);
        }

        // Distance Counter Display Box: between [-] and [+] (startX + 54, startY + 35, width 36, height 14)
        extractor.fill(startX + 54, startY + 35, startX + 90, startY + 49, 0xFF3A3A3A);
        extractor.fill(startX + 55, startY + 36, startX + 89, startY + 48, 0xFF141414);

        super.extractContents(extractor, mouseX, mouseY, delta);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        // 1. Header Title: "Turtle" on the left (x=8, y=6)
        extractor.text(this.font, this.title, this.titleLabelX, 6, 0xFF404040, false);

        int statusId = this.menu.getStatusId();
        TurtleStatus status = TurtleStatus.fromIndex(statusId);
        AppType app = AppType.fromIndex(this.menu.getAppTypeId());

        // 2. Fuel count text in header on far right above storage grid (y=6, x=166 - width)
        int fuel = this.menu.getFuel();
        String fuelStr = fuel + "F";
        int fuelWidth = this.font.width(fuelStr);
        extractor.text(this.font, Component.literal(fuelStr), 166 - fuelWidth, 6, 0xFFD45500, false);

        // 3. Program Name: Centered between x=38 and x=106 (y=25)
        String appName;
        if (app == AppType.NONE) {
            appName = "No Program";
        } else {
            appName = switch (app) {
                case MINER -> "3x3 Miner";
                case QUARRY -> "Quarry 3x3";
                case BUILDER -> "Bridge Builder";
                case LUMBERJACK -> "Tree Feller";
                case FARMER -> "Harvester";
                case FARM_CREATOR -> "Farm Creator";
                case SENTRY -> "Sentry";
                default -> app.name();
            };
        }
        int nameWidth = this.font.width(appName);
        int nameX = 38 + Math.max(0, (68 - nameWidth) / 2);
        extractor.text(this.font, Component.literal(appName), nameX, 25, 0xFF003388, false);

        // 4. Distance / parameter in the LCD box (between [-] and [+], y=35..49, box is at x=54..90)
        String distStr;
        if (app == AppType.FARM_CREATOR) {
            int farms = this.menu.getConfigDistance();
            distStr = (farms <= 1 ? "1 Farm" : farms + " Farms");
        } else if (app == AppType.BUILDER) {
            int w = this.menu.getConfigDistance();
            distStr = (w == 5 ? "5-Wide" : (w == 3 ? "3-Wide" : "1-Wide"));
        } else {
            distStr = this.menu.getConfigDistance() + "m";
        }
        int distWidth = this.font.width(distStr);
        int textX = 54 + (36 - distWidth) / 2;
        extractor.text(this.font, Component.literal(distStr), textX, 39, 0xFF00FF88, false);

        // 5. Status line: Centered below START/STOP buttons (y=70, x=38..106)
        String statusText = switch (status) {
            case NO_TOOL -> (app == AppType.MINER || app == AppType.QUARRY ? "Need Pickaxe!" :
                            (app == AppType.LUMBERJACK ? "Need Axe!" :
                            (app == AppType.FARMER || app == AppType.FARM_CREATOR ? "Need Hoe!" :
                            (app == AppType.BUILDER ? "Need Blocks!" :
                            (app == AppType.SENTRY ? "Need Sword!" : "Need Tool!")))));
            case OUT_OF_FUEL -> "Out of Fuel!";
            case RUNNING -> "Running (" + this.menu.getProgress() + "%)";
            case COMPLETED -> "Done!";
            case PAUSED -> "Paused";
            case BLOCKED -> "Path Blocked!";
            default -> (app == AppType.NONE ? "Insert Disk" : "Ready");
        };
        int statusColor = switch (status) {
            case NO_TOOL, OUT_OF_FUEL, BLOCKED -> 0xFFFF2222;
            case RUNNING -> 0xFF009900;
            case COMPLETED -> 0xFF0088DD;
            case PAUSED -> 0xFFBBAA00;
            default -> 0xFF555555;
        };
        int statusWidth = this.font.width(statusText);
        int statusX = 38 + Math.max(0, (68 - statusWidth) / 2);
        extractor.text(this.font, Component.literal(statusText), statusX, 70, statusColor, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        super.extractTooltip(extractor, mouseX, mouseY);

        int startX = this.leftPos;
        int startY = this.topPos;

        // Hover over Fuel Bar (startX + 40..104, startY + 16..22)
        if (mouseX >= startX + 40 && mouseX <= startX + 104 && mouseY >= startY + 16 && mouseY <= startY + 22) {
            int fuel = this.menu.getFuel();
            int maxFuel = this.menu.getMaxFuel();
            extractor.setComponentTooltipForNextFrame(this.font, List.of(
                Component.literal("§6Fuel Gauge: " + fuel + " / " + maxFuel + "§r"),
                Component.literal("§7Insert Coal or Charcoal into the bottom-left fuel slot§r")
            ), mouseX, mouseY);
        }

        // Hover over Program & Status (startX + 38..106, startY + 23..34)
        if (mouseX >= startX + 38 && mouseX <= startX + 106 && mouseY >= startY + 23 && mouseY <= startY + 34) {
            AppType app = AppType.fromIndex(this.menu.getAppTypeId());
            TurtleStatus status = TurtleStatus.fromIndex(this.menu.getStatusId());
            if (app != AppType.NONE) {
                extractor.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§9§l" + app.getDisplayName() + "§r §8(" + status.getLabel() + ")§r"),
                    Component.literal("§7" + app.getDescription() + "§r")
                ), mouseX, mouseY);
            } else {
                extractor.setTooltipForNextFrame(Component.literal("Insert a Floppy Disk into the top-left slot"), mouseX, mouseY);
            }
        }

        // Hover over Distance Counter (startX + 54..90, startY + 35..49)
        if (mouseX >= startX + 54 && mouseX <= startX + 90 && mouseY >= startY + 35 && mouseY <= startY + 49) {
            extractor.setComponentTooltipForNextFrame(this.font, List.of(
                Component.literal("§6Target Distance: " + this.menu.getConfigDistance() + " blocks§r"),
                Component.literal("§7Click [-] or [+] to configure§r")
            ), mouseX, mouseY);
        }

        // Hover over Status Line (startX + 38..106, startY + 68..79)
        if (mouseX >= startX + 38 && mouseX <= startX + 106 && mouseY >= startY + 68 && mouseY <= startY + 79) {
            TurtleStatus status = TurtleStatus.fromIndex(this.menu.getStatusId());
            AppType app = AppType.fromIndex(this.menu.getAppTypeId());
            extractor.setComponentTooltipForNextFrame(this.font, List.of(
                Component.literal("§eStatus: " + status.getLabel() + "§r"),
                Component.literal("§7Progress: " + this.menu.getProgress() + "% (" + app.getDisplayName() + ")§r")
            ), mouseX, mouseY);
        }

        // Empty Slot Tooltips
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem()) {
            if (this.hoveredSlot.index == TurtleScreenHandler.DISK_SLOT) {
                extractor.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§6Program Disk Slot§r"),
                    Component.literal("§7Insert a Floppy Disk (e.g. Miner) here§r")
                ), mouseX, mouseY);
            } else if (this.hoveredSlot.index == TurtleScreenHandler.TOOL_SLOT) {
                AppType app = AppType.fromIndex(this.menu.getAppTypeId());
                String req = switch (app) {
                    case MINER, QUARRY -> "§eRequires a Pickaxe to mine§r";
                    case LUMBERJACK -> "§eRequires an Axe to fell trees§r";
                    case FARMER -> "§eRequires a Hoe to harvest crops§r";
                    case SENTRY -> "§eRequires a Sword to attack mobs§r";
                    default -> "§7Insert Pickaxe, Axe, Shovel, or Sword§r";
                };
                extractor.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§6Tool Slot§r"),
                    Component.literal(req)
                ), mouseX, mouseY);
            } else if (this.hoveredSlot.index == TurtleScreenHandler.FUEL_SLOT) {
                extractor.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§6Fuel Slot§r"),
                    Component.literal("§7Insert Coal, Charcoal, or Lava Bucket§r")
                ), mouseX, mouseY);
            }
        }
    }
}
