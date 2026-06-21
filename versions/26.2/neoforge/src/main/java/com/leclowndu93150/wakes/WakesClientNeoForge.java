package com.leclowndu93150.wakes;

import com.leclowndu93150.wakes.platform.neoforge.WakesPlatformNeoForge;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = WakesClient.MOD_ID, dist = Dist.CLIENT)
public class WakesClientNeoForge {
    public WakesClientNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        WakesPlatformNeoForge.bootstrap(modEventBus, modContainer);
        WakesClient.init();
    }
}
