package com.leclowndu93150.wakes;

import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.particle.ModParticles;
import com.leclowndu93150.wakes.render.SplashPlaneRenderer;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(WakesClient.MOD_ID)
public class WakesClient {

	public static final String MOD_ID = "wakes";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static boolean areShadersEnabled = false;

	public WakesClient() {
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		if(FMLLoader.getDist().isClient()){
			ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, WakesConfig.CLIENT_SPEC, MOD_ID + "-client.toml");
			SplashPlaneRenderer.init();
		}
		ModParticles.register(modEventBus);
	}

	public static boolean areShadersEnabled() {
		if (FMLLoader.getLoadingModList().getModFileById("iris") != null) {
			return IrisApi.getInstance().getConfig().areShadersEnabled();
		}
		return false;
	}
}