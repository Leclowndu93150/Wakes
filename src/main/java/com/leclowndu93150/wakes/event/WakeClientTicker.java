package com.leclowndu93150.wakes.event;

import com.leclowndu93150.wakes.simulation.WakeHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

@EventBusSubscriber(bus =  EventBusSubscriber.Bus.GAME)
public class WakeClientTicker {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            WakeHandler.kill();
        } else if (WakeHandler.getInstance(client.level).isEmpty()) {
            WakeHandler.init(client.level);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ClientLevel clientLevel) {
            WakeHandler.killDimension(clientLevel.dimension());
        }
    }
}