package com.leclowndu93150.wakes;

import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.particle.ModParticles;
import com.leclowndu93150.wakes.particle.custom.SplashCloudParticle;
import com.leclowndu93150.wakes.particle.custom.SplashPlaneParticle;
import com.leclowndu93150.wakes.render.SplashPlaneRenderer;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;

@OnlyIn(Dist.CLIENT)
public class ClientRegistry {
    public static void init() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, WakesConfig.CLIENT_SPEC, WakesClient.MOD_ID + "-client.toml");

        SplashPlaneRenderer.init();

        modEventBus.register(ClientModEvents.class);

        if (FMLLoader.getLoadingModList().getModFileById("iris") != null) {
            try {
                WakesClient.areShadersEnabled = IrisApi.getInstance().getConfig().areShadersEnabled();
            } catch (Exception e) {
                WakesClient.LOGGER.error("Failed to check Iris shader status", e);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD, modid = WakesClient.MOD_ID)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(ModParticles.SPLASH_PLANE.get(), SplashPlaneParticle.Factory::new);
            event.registerSpriteSet(ModParticles.SPLASH_CLOUD.get(), SplashCloudParticle.Factory::new);
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            SplashPlaneRenderer.setup();
        }
    }
}