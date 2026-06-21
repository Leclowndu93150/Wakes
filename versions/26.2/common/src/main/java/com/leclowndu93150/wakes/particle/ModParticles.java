package com.leclowndu93150.wakes.particle;

import com.leclowndu93150.wakes.particle.custom.SplashCloudParticle;
import com.leclowndu93150.wakes.particle.custom.SplashPlaneParticle;
import com.leclowndu93150.wakes.platform.Services;

import java.util.function.Supplier;

public class ModParticles {
    public static Supplier<WithOwnerParticleType> SPLASH_PLANE;
    public static Supplier<WithOwnerParticleType> SPLASH_CLOUD;

    public static void register() {
        SPLASH_PLANE = Services.PLATFORM.registerParticleType("splash_plane", () -> new WithOwnerParticleType(true));
        SPLASH_CLOUD = Services.PLATFORM.registerParticleType("splash_cloud", () -> new WithOwnerParticleType(true));
        Services.PLATFORM.registerParticleFactory(SPLASH_PLANE, SplashPlaneParticle.Factory::new);
        Services.PLATFORM.registerParticleFactory(SPLASH_CLOUD, SplashCloudParticle.Factory::new);
    }
}
