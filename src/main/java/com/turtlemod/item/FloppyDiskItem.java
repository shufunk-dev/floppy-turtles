package com.turtlemod.item;

import com.turtlemod.app.AppType;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

public class FloppyDiskItem extends Item {
    private final AppType appType;

    public FloppyDiskItem(Settings settings, AppType appType) {
        super(settings);
        this.appType = appType;
    }

    public AppType getAppType() {
        return appType;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        if (appType == AppType.NONE) {
            textConsumer.accept(Text.literal("Blank Floppy Disk").formatted(Formatting.GRAY));
            textConsumer.accept(Text.literal("Craft with tools/items to program an app.").formatted(Formatting.DARK_GRAY));
        } else {
            textConsumer.accept(Text.literal("App: ").formatted(Formatting.GOLD)
                    .append(Text.literal(appType.getDisplayName()).formatted(Formatting.AQUA)));
            textConsumer.accept(Text.literal(appType.getDescription()).formatted(Formatting.GRAY));
            if (appType.getIconItem() != null && appType.getIconItem() != net.minecraft.item.Items.AIR) {
                textConsumer.accept(Text.literal("Requires: ").formatted(Formatting.YELLOW)
                        .append(Text.literal(appType.getIconItem().getName().getString()).formatted(Formatting.WHITE)));
            }
            textConsumer.accept(Text.literal("Fuel Cost: 1 unit / action").formatted(Formatting.GREEN));
        }
    }
}
