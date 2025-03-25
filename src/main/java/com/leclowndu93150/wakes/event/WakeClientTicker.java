package com.leclowndu93150.wakes.event;

import com.leclowndu93150.wakes.particle.ModParticles;
import com.leclowndu93150.wakes.particle.custom.SplashCloudParticle;
import com.leclowndu93150.wakes.particle.custom.SplashPlaneParticle;
import com.leclowndu93150.wakes.render.SplashPlaneRenderer;
import com.leclowndu93150.wakes.simulation.WakeHandler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(bus =  Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
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

@Mod.EventBusSubscriber(bus =  Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
class ModClientEvents{

    @SubscribeEvent
    public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.SPLASH_PLANE.get(), SplashPlaneParticle.Factory::new);
        event.registerSpriteSet(ModParticles.SPLASH_CLOUD.get(), SplashCloudParticle.Factory::new);
    }

    @SubscribeEvent
    public void onClientSetup(FMLClientSetupEvent event) {
        SplashPlaneRenderer.setup();
    }
}