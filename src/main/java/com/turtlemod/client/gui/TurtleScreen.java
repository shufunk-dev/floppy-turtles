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

        // Status text
        context.drawTextWithShadow(this.textRenderer, Text.literal(status.getLabel()), 40, 24, status.getColor());

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
}
