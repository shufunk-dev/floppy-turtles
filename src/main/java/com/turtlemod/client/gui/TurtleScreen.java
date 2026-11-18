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

        // Button 0: Start
        this.addDrawableChild(ButtonWidget.builder(Text.literal("START"), btn -> {
            if (this.client != null && this.client.interactionManager != null) {
                this.client.interactionManager.clickButton(this.handler.syncId, 0);
            }
        }).dimensions(startX + 40, startY + 52, 33, 16).build());

        // Button 1: Stop
        this.addDrawableChild(ButtonWidget.builder(Text.literal("STOP"), btn -> {
            if (this.client != null && this.client.interactionManager != null) {
                this.client.interactionManager.clickButton(this.handler.syncId, 1);
            }
        }).dimensions(startX + 74, startY + 52, 30, 16).build());

        // Button 2: Param -
        this.addDrawableChild(ButtonWidget.builder(Text.literal("-"), btn -> {
            if (this.client != null && this.client.interactionManager != null) {
                this.client.interactionManager.clickButton(this.handler.syncId, 2);
            }
        }).dimensions(startX + 40, startY + 35, 12, 14).build());

        // Button 3: Param +
        this.addDrawableChild(ButtonWidget.builder(Text.literal("+"), btn -> {
            if (this.client != null && this.client.interactionManager != null) {
                this.client.interactionManager.clickButton(this.handler.syncId, 3);
            }
        }).dimensions(startX + 92, startY + 35, 12, 14).build());
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int startX = this.x;
        int startY = this.y;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, startX, startY, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);

        // Fuel Bar: (startX + 40, startY + 16, width: 64, height: 6)
        int fuel = this.handler.getFuel();
        int maxFuel = this.handler.getMaxFuel();
        int fuelBarWidth = Math.min(64, (int) ((fuel / (float) maxFuel) * 64));

        // Fuel bar background
        context.fill(startX + 40, startY + 16, startX + 104, startY + 22, 0xFF333333);
        // Fuel bar filled
        if (fuelBarWidth > 0) {
            context.fill(startX + 40, startY + 16, startX + 40 + fuelBarWidth, startY + 22, 0xFFFF7700);
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, this.titleX, 6, 0x404040, false);

        int statusId = this.handler.getStatusId();
        TurtleStatus status = TurtleStatus.fromIndex(statusId);
        AppType app = AppType.fromIndex(this.handler.getAppTypeId());

        String statusText;
        int statusColor;
        if (app == AppType.NONE) {
            statusText = "Insert Disk";
            statusColor = 0xFFAA00;
        } else if (status == TurtleStatus.NO_TOOL) {
            if (app == AppType.MINER || app == AppType.QUARRY) {
                statusText = "Need Pickaxe";
            } else if (app == AppType.LUMBERJACK) {
                statusText = "Need Axe";
            } else if (app == AppType.FARMER) {
                statusText = "Need Hoe";
            } else if (app == AppType.SENTRY) {
                statusText = "Need Sword";
            } else {
                statusText = "Need Tool";
            }
            statusColor = 0xFFAA00;
        } else if (status == TurtleStatus.OUT_OF_FUEL) {
            statusText = "Need Fuel";
            statusColor = 0xFF5555;
        } else if (status == TurtleStatus.RUNNING) {
            statusText = "Running";
            statusColor = 0x55FF55;
        } else {
            statusText = status.getLabel();
            statusColor = status.getColor();
        }

        // Status text
        context.drawTextWithShadow(this.textRenderer, Text.literal(statusText), 40, 24, statusColor);

        // Config Distance / Range
        context.drawText(this.textRenderer, Text.literal(this.handler.getConfigDistance() + "m"), 56, 38, 0x333333, false);

        // Fuel tooltip / text
        int fuel = this.handler.getFuel();
        context.drawText(this.textRenderer, Text.literal(fuel + "F"), 80, 6, 0x555555, false);

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

        // Hover over fuel bar (startX + 40..104, startY + 16..22)
        if (x >= startX + 40 && x <= startX + 104 && y >= startY + 16 && y <= startY + 22) {
            int fuel = this.handler.getFuel();
            int maxFuel = this.handler.getMaxFuel();
            context.drawTooltip(this.textRenderer, Text.literal("Fuel: " + fuel + " / " + maxFuel), x, y);
        }

        // Hover over status text (startX + 40..110, startY + 24..33)
        if (x >= startX + 40 && x <= startX + 110 && y >= startY + 24 && y <= startY + 33) {
            AppType app = AppType.fromIndex(this.handler.getAppTypeId());
            TurtleStatus status = TurtleStatus.fromIndex(this.handler.getStatusId());
            if (app == AppType.NONE) {
                context.drawTooltip(this.textRenderer, Text.literal("Insert a programmed Floppy Disk to start"), x, y);
            } else if (status == TurtleStatus.NO_TOOL) {
                context.drawTooltip(this.textRenderer, Text.literal("Equip required tool in the middle tool slot"), x, y);
            } else if (status == TurtleStatus.OUT_OF_FUEL) {
                context.drawTooltip(this.textRenderer, Text.literal("Out of fuel! Insert Coal or Charcoal into fuel slot"), x, y);
            } else if (status == TurtleStatus.RUNNING) {
                context.drawTooltip(this.textRenderer, Text.literal("Running: " + app.getDisplayName() + " (" + this.handler.getProgress() + "% complete)"), x, y);
            } else {
                context.drawTooltip(this.textRenderer, Text.literal("Status: " + status.getLabel()), x, y);
            }
        }

        // Empty slot tooltips
        if (this.focusedSlot != null && !this.focusedSlot.hasStack()) {
            if (this.focusedSlot.id == TurtleScreenHandler.DISK_SLOT) {
                context.drawTooltip(this.textRenderer, java.util.List.of(
                    Text.literal("§6Program Disk Slot§r"),
                    Text.literal("§7Insert a Floppy Disk here§r")
                ), x, y);
            } else if (this.focusedSlot.id == TurtleScreenHandler.TOOL_SLOT) {
                context.drawTooltip(this.textRenderer, java.util.List.of(
                    Text.literal("§6Tool Slot§r"),
                    Text.literal("§7Insert Pickaxe, Axe, Shovel, or Sword§r")
                ), x, y);
            } else if (this.focusedSlot.id == TurtleScreenHandler.FUEL_SLOT) {
                context.drawTooltip(this.textRenderer, java.util.List.of(
                    Text.literal("§6Fuel Slot§r"),
                    Text.literal("§7Insert Coal, Charcoal, or furnace fuel§r")
                ), x, y);
            }
        }
    }
}
