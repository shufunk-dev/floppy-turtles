package com.turtlemod.client.gui;

import com.turtlemod.app.AppType;
import com.turtlemod.app.TurtleStatus;
import com.turtlemod.screen.TurtleScreenHandler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class TurtleScreen extends HandledScreen<TurtleScreenHandler> {
    private static final Identifier TEXTURE = Identifier.of("turtle", "textures/gui/turtle_gui.png");

    public TurtleScreen(TurtleScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int startX = this.x;
        int startY = this.y;

        // Button 2: Param - (distance - 4)
        this.addDrawableChild(ButtonWidget.builder(Text.literal("-"), btn -> {
            if (this.client != null && this.client.interactionManager != null) {
                this.client.interactionManager.clickButton(this.handler.syncId, 2);
            }
        }).dimensions(startX + 38, startY + 35, 14, 14).build());

        // Button 3: Param + (distance + 4)
        this.addDrawableChild(ButtonWidget.builder(Text.literal("+"), btn -> {
            if (this.client != null && this.client.interactionManager != null) {
                this.client.interactionManager.clickButton(this.handler.syncId, 3);
            }
        }).dimensions(startX + 92, startY + 35, 14, 14).build());

        // Button 0: Start
        this.addDrawableChild(ButtonWidget.builder(Text.literal("START"), btn -> {
            if (this.client != null && this.client.interactionManager != null) {
                this.client.interactionManager.clickButton(this.handler.syncId, 0);
            }
        }).dimensions(startX + 38, startY + 52, 33, 16).build());

        // Button 1: Stop
        this.addDrawableChild(ButtonWidget.builder(Text.literal("STOP"), btn -> {
            if (this.client != null && this.client.interactionManager != null) {
                this.client.interactionManager.clickButton(this.handler.syncId, 1);
            }
        }).dimensions(startX + 73, startY + 52, 33, 16).build());
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int startX = this.x;
        int startY = this.y;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, startX, startY, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);

        // Fuel Bar: in top inset box (startX + 40, startY + 16, width: 64, height: 6)
        int fuel = this.handler.getFuel();
        int maxFuel = this.handler.getMaxFuel();
        int fuelBarWidth = Math.min(64, (int) ((fuel / (float) maxFuel) * 64));

        // Fuel bar background
        context.fill(startX + 40, startY + 16, startX + 104, startY + 22, 0xFF262626);
        // Fuel bar filled with orange
        if (fuelBarWidth > 0) {
            context.fill(startX + 40, startY + 16, startX + 40 + fuelBarWidth, startY + 22, 0xFFFF7700);
        }

        // Distance Counter Display Box: between [-] and [+] (startX + 54, startY + 35, width 36, height 14)
        context.fill(startX + 54, startY + 35, startX + 90, startY + 49, 0xFF3A3A3A);
        context.fill(startX + 55, startY + 36, startX + 89, startY + 48, 0xFF141414);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // 1. Header Title: "Turtle" on the left (x=8, y=6)
        context.drawText(this.textRenderer, this.title, this.titleX, 6, 0xFF404040, false);

        int statusId = this.handler.getStatusId();
        TurtleStatus status = TurtleStatus.fromIndex(statusId);
        AppType app = AppType.fromIndex(this.handler.getAppTypeId());

        // 2. Fuel count text in header on far right above storage grid (y=6, x=166 - width)
        int fuel = this.handler.getFuel();
        String fuelStr = fuel + "F";
        int fuelWidth = this.textRenderer.getWidth(fuelStr);
        context.drawText(this.textRenderer, Text.literal(fuelStr), 166 - fuelWidth, 6, 0xFFD45500, false);

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
                case SENTRY -> "Sentry";
                default -> app.name();
            };
        }
        int nameWidth = this.textRenderer.getWidth(appName);
        int nameX = 38 + Math.max(0, (68 - nameWidth) / 2);
        context.drawText(this.textRenderer, Text.literal(appName), nameX, 25, 0xFF003388, false);

        // 4. Distance number in the LCD box (between [-] and [+], y=35..49, box is at x=54..90)
        String distStr = this.handler.getConfigDistance() + "m";
        int distWidth = this.textRenderer.getWidth(distStr);
        int textX = 54 + (36 - distWidth) / 2;
        context.drawText(this.textRenderer, Text.literal(distStr), textX, 39, 0xFF00FF88, false);

        // 5. Status line: Centered below START/STOP buttons (y=70, x=38..106)
        String statusText = switch (status) {
            case NO_TOOL -> (app == AppType.MINER ? "Need Pickaxe!" : "Need Tool!");
            case OUT_OF_FUEL -> "Out of Fuel!";
            case RUNNING -> "Running (" + this.handler.getProgress() + "%)";
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
        int statusWidth = this.textRenderer.getWidth(statusText);
        int statusX = 38 + Math.max(0, (68 - statusWidth) / 2);
        context.drawText(this.textRenderer, Text.literal(statusText), statusX, 70, statusColor, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawMouseoverTooltip(DrawContext context, int x, int y) {
        super.drawMouseoverTooltip(context, x, y);

        int startX = this.x;
        int startY = this.y;

        // Hover over Fuel Bar (startX + 40..104, startY + 16..22)
        if (x >= startX + 40 && x <= startX + 104 && y >= startY + 16 && y <= startY + 22) {
            int fuel = this.handler.getFuel();
            int maxFuel = this.handler.getMaxFuel();
            context.drawTooltip(this.textRenderer, java.util.List.of(
                Text.literal("§6Fuel Gauge: " + fuel + " / " + maxFuel + "§r"),
                Text.literal("§7Insert Coal or Charcoal into the bottom-left fuel slot§r")
            ), x, y);
        }

        // Hover over Program & Status (startX + 38..106, startY + 23..34)
        if (x >= startX + 38 && x <= startX + 106 && y >= startY + 23 && y <= startY + 34) {
            AppType app = AppType.fromIndex(this.handler.getAppTypeId());
            TurtleStatus status = TurtleStatus.fromIndex(this.handler.getStatusId());
            if (app != AppType.NONE) {
                context.drawTooltip(this.textRenderer, java.util.List.of(
                    Text.literal("§9§l" + app.getDisplayName() + "§r §8(" + status.getLabel() + ")§r"),
                    Text.literal("§7" + app.getDescription() + "§r")
                ), x, y);
            } else {
                context.drawTooltip(this.textRenderer, Text.literal("Insert a Floppy Disk into the top-left slot"), x, y);
            }
        }

        // Hover over Distance Counter (startX + 54..90, startY + 35..49)
        if (x >= startX + 54 && x <= startX + 90 && y >= startY + 35 && y <= startY + 49) {
            context.drawTooltip(this.textRenderer, java.util.List.of(
                Text.literal("§6Target Distance: " + this.handler.getConfigDistance() + " blocks§r"),
                Text.literal("§7Click [-] or [+] to configure§r")
            ), x, y);
        }

        // Hover over Status Line (startX + 38..106, startY + 68..79)
        if (x >= startX + 38 && x <= startX + 106 && y >= startY + 68 && y <= startY + 79) {
            TurtleStatus status = TurtleStatus.fromIndex(this.handler.getStatusId());
            AppType app = AppType.fromIndex(this.handler.getAppTypeId());
            context.drawTooltip(this.textRenderer, java.util.List.of(
                Text.literal("§eStatus: " + status.getLabel() + "§r"),
                Text.literal("§7Progress: " + this.handler.getProgress() + "% (" + app.getDisplayName() + ")§r")
            ), x, y);
        }

        // Empty Slot Tooltips
        if (this.focusedSlot != null && !this.focusedSlot.hasStack()) {
            if (this.focusedSlot.id == TurtleScreenHandler.DISK_SLOT) {
                context.drawTooltip(this.textRenderer, java.util.List.of(
                    Text.literal("§6Program Disk Slot§r"),
                    Text.literal("§7Insert a Floppy Disk (e.g. Miner) here§r")
                ), x, y);
            } else if (this.focusedSlot.id == TurtleScreenHandler.TOOL_SLOT) {
                AppType app = AppType.fromIndex(this.handler.getAppTypeId());
                String req = switch (app) {
                    case MINER, QUARRY -> "§eRequires a Pickaxe to mine§r";
                    case LUMBERJACK -> "§eRequires an Axe to fell trees§r";
                    case FARMER -> "§eRequires a Hoe to harvest crops§r";
                    case SENTRY -> "§eRequires a Sword to attack mobs§r";
                    default -> "§7Insert Pickaxe, Axe, Shovel, or Sword§r";
                };
                context.drawTooltip(this.textRenderer, java.util.List.of(
                    Text.literal("§6Tool Slot§r"),
                    Text.literal(req)
                ), x, y);
            } else if (this.focusedSlot.id == TurtleScreenHandler.FUEL_SLOT) {
                context.drawTooltip(this.textRenderer, java.util.List.of(
                    Text.literal("§6Fuel Slot§r"),
                    Text.literal("§7Insert Coal, Charcoal, or Lava Bucket§r")
                ), x, y);
            }
        }
    }
}
