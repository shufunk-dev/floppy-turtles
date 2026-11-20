package com.turtlemod.item;

import com.turtlemod.app.AppType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class FloppyDiskItem extends Item {
    private final AppType appType;

    public FloppyDiskItem(Properties properties, AppType appType) {
        super(properties);
        this.appType = appType;
    }

    public AppType getAppType() {
        return appType;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> textConsumer, TooltipFlag flag) {
        if (appType == AppType.NONE) {
            textConsumer.accept(Component.literal("Blank Floppy Disk").withStyle(ChatFormatting.GRAY));
            textConsumer.accept(Component.literal("Craft with tools/items to program an app.").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            textConsumer.accept(Component.literal("App: ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(appType.getDisplayName()).withStyle(ChatFormatting.AQUA)));
            textConsumer.accept(Component.literal(appType.getDescription()).withStyle(ChatFormatting.GRAY));
            if (appType.getIconItem() != null && appType.getIconItem() != Items.AIR) {
                textConsumer.accept(Component.literal("Requires: ").withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal(appType.getIconItem().getName(stack).getString()).withStyle(ChatFormatting.WHITE)));
            }
            textConsumer.accept(Component.literal("Fuel Cost: 1 unit / action").withStyle(ChatFormatting.GREEN));
        }
    }
}
