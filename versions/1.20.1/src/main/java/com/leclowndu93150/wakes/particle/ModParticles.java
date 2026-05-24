package com.leclowndu93150.wakes.particle;

import com.leclowndu93150.wakes.WakesClient;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, WakesClient.MOD_ID);

    public static final Supplier<WithOwnerParticleType> SPLASH_PLANE = PARTICLE_TYPES.register(
            "splash_plane", () -> new WithOwnerParticleType(true));

    public static final Supplier<WithOwnerParticleType> SPLASH_CLOUD = PARTICLE_TYPES.register(
            "splash_cloud", () -> new WithOwnerParticleType(true));

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }
}