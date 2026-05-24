package com.leclowndu93150.wakes.particle;

import com.leclowndu93150.wakes.WakesClient;
import com.leclowndu93150.wakes.particle.custom.SplashCloudParticle;
import com.leclowndu93150.wakes.particle.custom.SplashPlaneParticle;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import java.util.function.Supplier;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, WakesClient.MOD_ID);

    public static final Supplier<WithOwnerParticleType> SPLASH_PLANE = PARTICLE_TYPES.register(
            "splash_plane", () -> new WithOwnerParticleType(true));

    public static final Supplier<WithOwnerParticleType> SPLASH_CLOUD = PARTICLE_TYPES.register(
            "splash_cloud", () -> new WithOwnerParticleType(true));

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }

    public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(SPLASH_PLANE.get(), SplashPlaneParticle.Factory::new);
        event.registerSpriteSet(SPLASH_CLOUD.get(), SplashCloudParticle.Factory::new);
    }
}