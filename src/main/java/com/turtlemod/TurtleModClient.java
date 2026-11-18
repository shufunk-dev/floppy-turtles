package com.turtlemod;

import com.turtlemod.client.gui.TurtleScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class TurtleModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(TurtleMod.TURTLE_SCREEN_HANDLER, TurtleScreen::new);
    }
}
