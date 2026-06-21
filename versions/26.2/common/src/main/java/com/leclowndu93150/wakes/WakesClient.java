package com.leclowndu93150.wakes;

import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.debug.WakeDebugRenderer;
import com.leclowndu93150.wakes.debug.WakesDebugInfo;
import com.leclowndu93150.wakes.event.ChunkWakeCleanup;
import com.leclowndu93150.wakes.event.WakeClientLifecycle;
import com.leclowndu93150.wakes.event.WakeWorldTicker;
import com.leclowndu93150.wakes.particle.ModParticles;
import com.leclowndu93150.wakes.platform.Services;
import com.leclowndu93150.wakes.render.SplashPlaneRenderer;
import com.leclowndu93150.wakes.render.WakeRenderer;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WakesClient {

    public static final String MOD_ID = "wakes";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static boolean areShadersEnabled = false;

    private WakesClient() {}

    public static void init() {
        Services.PLATFORM.registerConfig(ModConfig.Type.CLIENT, WakesConfig.CLIENT_SPEC, MOD_ID + "-client.toml");
        ModParticles.register();
        SplashPlaneRenderer.init();
        Services.PLATFORM.registerClientLifecycle(SplashPlaneRenderer::setup);
        Services.PLATFORM.registerLevelRenderAfterTranslucent(WakeRenderer::render);
        Services.PLATFORM.registerLevelRenderAfterTranslucent(SplashPlaneRenderer::render);
        Services.PLATFORM.registerLevelRenderAfterTranslucent(WakeDebugRenderer::render);
        Services.PLATFORM.registerClientTickPre(WakeClientLifecycle::onClientTick);
        Services.PLATFORM.registerLevelUnload(WakeClientLifecycle::onLevelUnload);
        Services.PLATFORM.registerLevelTickPost(WakeWorldTicker::onLevelTick);
        Services.PLATFORM.registerPlayerChangedDimension(WakeWorldTicker::onPlayerChangedDimension);
        Services.PLATFORM.registerPlayerLoggedIn(WakeWorldTicker::onPlayerLoggedIn);
        Services.PLATFORM.registerChunkUnload(ChunkWakeCleanup::onChunkUnload);
        Services.PLATFORM.registerDebugScreenEntry(WakesDebugInfo.ID, new WakesDebugInfo());
    }
}
