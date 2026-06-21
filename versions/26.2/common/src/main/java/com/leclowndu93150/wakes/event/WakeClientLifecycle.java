package com.leclowndu93150.wakes.event;

import com.leclowndu93150.wakes.simulation.WakeHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public class WakeClientLifecycle {
    public static void onClientTick() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            WakeHandler.kill();
        } else if (WakeHandler.getInstance(client.level).isEmpty()) {
            WakeHandler.init(client.level);
        }
    }

    public static void onLevelUnload(ClientLevel level) {
        WakeHandler.killDimension(level.dimension());
    }
}
