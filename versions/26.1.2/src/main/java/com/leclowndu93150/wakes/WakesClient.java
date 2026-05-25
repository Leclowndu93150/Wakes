package com.leclowndu93150.wakes;

import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.particle.ModParticles;
import com.leclowndu93150.wakes.render.SplashPlaneRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(value = WakesClient.MOD_ID, dist = Dist.CLIENT)
public class WakesClient {

	public static final String MOD_ID = "wakes";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static boolean areShadersEnabled = false;

	public WakesClient(IEventBus modEventBus, ModContainer modContainer) {
		modContainer.registerConfig(ModConfig.Type.CLIENT, WakesConfig.CLIENT_SPEC, MOD_ID + "-client.toml");
		ModParticles.register(modEventBus);
		SplashPlaneRenderer.init();
		modEventBus.addListener(ModParticles::registerParticleFactories);
		modEventBus.addListener(this::onClientSetup);
	}

	private void onClientSetup(FMLClientSetupEvent event) {
		SplashPlaneRenderer.setup();
	}
}
