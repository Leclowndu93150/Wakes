package com.leclowndu93150.wakes.event;

import com.leclowndu93150.wakes.simulation.WakeHandler;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber
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
}