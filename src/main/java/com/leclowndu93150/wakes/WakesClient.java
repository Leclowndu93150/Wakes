package com.leclowndu93150.wakes;

import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.particle.ModParticles;
import com.leclowndu93150.wakes.render.SplashPlaneRenderer;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import eu.midnightdust.lib.config.MidnightConfig;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import org.ladysnake.satin.api.managed.ManagedCoreShader;
import org.ladysnake.satin.api.managed.ShaderEffectManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(WakesClient.MOD_ID)
public class WakesClient {

	public static final String MOD_ID = "wakes";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final ManagedCoreShader TRANSLUCENT_NO_LIGHT_DIRECTION_PROGRAM = ShaderEffectManager.getInstance().manageCoreShader(
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "translucent_no_light_direction"), DefaultVertexFormat.NEW_ENTITY);
	public static final ManagedCoreShader POSITION_TEXTURE_HSV = ShaderEffectManager.getInstance().manageCoreShader(
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "position_tex_hsv"), DefaultVertexFormat.POSITION_TEX_COLOR);
	public static boolean areShadersEnabled = false;

	public WakesClient(IEventBus modEventBus) {
		MidnightConfig.init(WakesClient.MOD_ID, WakesConfig.class);
		ModParticles.register(modEventBus);
		SplashPlaneRenderer.initSplashPlane();
		modEventBus.addListener(ModParticles::registerParticleFactories);
	}

	public static boolean areShadersEnabled() {
		if (FMLLoader.getLoadingModList().getModFileById("iris") != null) {
			return IrisApi.getInstance().getConfig().areShadersEnabled();
		}
		return false;
	}
}