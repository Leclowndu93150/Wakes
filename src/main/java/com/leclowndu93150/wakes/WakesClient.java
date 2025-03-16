package com.leclowndu93150.wakes;

import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.particle.ModParticles;
import com.leclowndu93150.wakes.render.SplashPlaneRenderer;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Mod(WakesClient.MOD_ID)
public class WakesClient {

	public static ShaderInstance TRANSLUCENT_NO_LIGHT_DIRECTION_PROGRAM;
	public static ShaderInstance POSITION_TEXTURE_HSV;
	public static final String MOD_ID = "wakes";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static boolean areShadersEnabled = false;

	public WakesClient(IEventBus modEventBus, ModContainer modContainer) {
		modContainer.registerConfig(ModConfig.Type.CLIENT, WakesConfig.GENERAL_SPEC, MOD_ID + "-general.toml");
		modContainer.registerConfig(ModConfig.Type.CLIENT, WakesConfig.APPEARANCE_SPEC, MOD_ID + "-appearance.toml");
		modContainer.registerConfig(ModConfig.Type.CLIENT, WakesConfig.DEBUG_SPEC, MOD_ID + "-debug.toml");
		ModParticles.register(modEventBus);
		SplashPlaneRenderer.initSplashPlane();
		SplashPlaneRenderer.init();
		modEventBus.addListener(ModParticles::registerParticleFactories);
		modEventBus.addListener(this::onResourceReload);
	}

	private void onResourceReload(RegisterClientReloadListenersEvent event) {
		event.registerReloadListener((barrier, manager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor) -> CompletableFuture.supplyAsync(() -> {
            preparationsProfiler.startTick();
            preparationsProfiler.push("wakes_shader_preparation");
            // Do any preparation work here if needed
            preparationsProfiler.pop();
            preparationsProfiler.endTick();
            return null;
        }, backgroundExecutor).thenCompose(barrier::wait).thenAcceptAsync(unused -> {
            reloadProfiler.startTick();
            reloadProfiler.push("wakes_shader_loading");
            loadShaders(manager);
            reloadProfiler.pop();
            reloadProfiler.endTick();
        }, gameExecutor));
	}

	public static void loadShaders(ResourceProvider provider) {
		try {
			TRANSLUCENT_NO_LIGHT_DIRECTION_PROGRAM = new ShaderInstance(provider,
					ResourceLocation.fromNamespaceAndPath(MOD_ID, "translucent_no_light_direction"), DefaultVertexFormat.NEW_ENTITY);
			POSITION_TEXTURE_HSV = new ShaderInstance(provider,
					ResourceLocation.fromNamespaceAndPath(MOD_ID, "position_tex_hsv"), DefaultVertexFormat.POSITION_TEX_COLOR);
		} catch (IOException e) {
			LOGGER.error("Failed to load shaders", e);
		}
	}

	public static boolean areShadersEnabled() {
		if (FMLLoader.getLoadingModList().getModFileById("iris") != null) {
			return IrisApi.getInstance().getConfig().areShadersEnabled();
		}
		return false;
	}
}