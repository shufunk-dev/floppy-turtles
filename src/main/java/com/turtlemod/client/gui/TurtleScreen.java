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
        }).dimensions(startX + 38, startY + 51, 32, 15).build());

        // Button 1: Stop
        this.addDrawableChild(ButtonWidget.builder(Text.literal("STOP"), btn -> {
            if (this.client != null && this.client.interactionManager != null) {
                this.client.interactionManager.clickButton(this.handler.syncId, 1);
            }
        }).dimensions(startX + 74, startY + 51, 32, 15).build());
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int startX = this.x;
        int startY = this.y;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, startX, startY, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);

        // Fuel Bar: (startX + 38, startY + 69, width: 68, height: 5)
        int fuel = this.handler.getFuel();
        int maxFuel = this.handler.getMaxFuel();
        int fuelBarWidth = Math.min(68, (int) ((fuel / (float) maxFuel) * 68));

        // Fuel bar background
        context.fill(startX + 38, startY + 69, startX + 106, startY + 74, 0xFF222222);
        // Fuel bar filled
        if (fuelBarWidth > 0) {
            context.fill(startX + 38, startY + 69, startX + 38 + fuelBarWidth, startY + 74, 0xFFFF7700);
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, this.titleX, 6, 0x404040, false);

        int statusId = this.handler.getStatusId();
        TurtleStatus status = TurtleStatus.fromIndex(statusId);
        AppType app = AppType.fromIndex(this.handler.getAppTypeId());

        // 1. Program Name (y = 15)
        if (app == AppType.NONE) {
            context.drawText(this.textRenderer, Text.literal("No Disk"), 38, 15, 0x888888, false);
        } else {
            String appName = switch (app) {
                case MINER -> "3x3 Miner";
                case QUARRY -> "Quarry 3x3";
                case BUILDER -> "Builder";
                case LUMBERJACK -> "Tree Feller";
                case FARMER -> "Harvester";
                case SENTRY -> "Sentry";
                default -> app.name();
            };
            context.drawText(this.textRenderer, Text.literal(appName), 38, 15, 0x0044AA, false);
        }

        // 2. Status Label (y = 25)
        String statusText;
        int statusColor;
        if (app == AppType.NONE) {
            statusText = "Insert Disk";
            statusColor = 0xAA6600;
        } else if (status == TurtleStatus.NO_TOOL) {
            statusText = switch (app) {
                case MINER, QUARRY -> "Need Pickaxe";
                case LUMBERJACK -> "Need Axe";
                case FARMER -> "Need Hoe";
                case SENTRY -> "Need Sword";
                default -> "Need Tool";
            };
            statusColor = 0xDD4400;
        } else if (status == TurtleStatus.OUT_OF_FUEL) {
            statusText = "Need Fuel";
            statusColor = 0xCC0000;
        } else if (status == TurtleStatus.RUNNING) {
            statusText = "Running " + this.handler.getProgress() + "%";
            statusColor = 0x008800;
        } else if (status == TurtleStatus.COMPLETED) {
            statusText = "Done!";
            statusColor = 0x0088AA;
        } else if (status == TurtleStatus.PAUSED) {
            statusText = "Paused";
            statusColor = 0x888800;
        } else if (status == TurtleStatus.BLOCKED) {
            statusText = "Blocked";
            statusColor = 0xCC0000;
        } else {
            statusText = "Idle";
            statusColor = 0x666666;
        }
        context.drawText(this.textRenderer, Text.literal(statusText), 38, 25, statusColor, false);

        // 3. Distance Setting Display (centered between minus and plus, y = 38)
        String distStr = this.handler.getConfigDistance() + "m";
        int distWidth = this.textRenderer.getWidth(distStr);
        int centerX = 52 + (40 - distWidth) / 2;
        context.drawText(this.textRenderer, Text.literal(distStr), centerX, 38, 0x222222, false);

        // 4. Fuel Display at Top Right Header (y = 6)
        int fuel = this.handler.getFuel();
        String fuelStr = fuel + "F";
        int fuelWidth = this.textRenderer.getWidth(fuelStr);
        context.drawText(this.textRenderer, Text.literal(fuelStr), 106 - fuelWidth, 6, 0xD45500, false);

        // Player Inventory label
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 0x404040, false);
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

        // Hover over Program Name (startX + 38..106, startY + 14..24)
        if (x >= startX + 38 && x <= startX + 106 && y >= startY + 14 && y <= startY + 24) {
            AppType app = AppType.fromIndex(this.handler.getAppTypeId());
            if (app != AppType.NONE) {
                context.drawTooltip(this.textRenderer, java.util.List.of(
                    Text.literal("§9§l" + app.getDisplayName() + "§r"),
                    Text.literal("§7" + app.getDescription() + "§r")
                ), x, y);
            } else {
                context.drawTooltip(this.textRenderer, Text.literal("Insert a Floppy Disk into the top-left slot"), x, y);
            }
        }

        // Hover over Distance Area (startX + 38..106, startY + 35..49)
        if (x >= startX + 52 && x <= startX + 92 && y >= startY + 35 && y <= startY + 49) {
            context.drawTooltip(this.textRenderer, java.util.List.of(
                Text.literal("§6Target Distance: " + this.handler.getConfigDistance() + " blocks§r"),
                Text.literal("§7Click [-] or [+] to configure§r")
            ), x, y);
        }

        // Hover over Fuel Bar (startX + 38..106, startY + 68..75)
        if (x >= startX + 38 && x <= startX + 106 && y >= startY + 68 && y <= startY + 75) {
            int fuel = this.handler.getFuel();
            int maxFuel = this.handler.getMaxFuel();
            context.drawTooltip(this.textRenderer, java.util.List.of(
                Text.literal("§6Fuel Reserve: " + fuel + " / " + maxFuel + "§r"),
                Text.literal("§7Insert Coal or Charcoal into the bottom-left fuel slot§r")
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
