package com.leclowndu93150.wakes.event;

import com.leclowndu93150.wakes.simulation.WakeHandler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus =  Mod.EventBusSubscriber.Bus.FORGE)
public class WakeClientTicker {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft client = Minecraft.getInstance();
        if(event.phase != TickEvent.Phase.START) return;
        if (client.level == null) {
            WakeHandler.kill();
        } else if (WakeHandler.getInstance().isEmpty()) {
            WakeHandler.init(client.level);
        }
    }
}