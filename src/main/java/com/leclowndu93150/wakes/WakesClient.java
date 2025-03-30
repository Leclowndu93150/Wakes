package com.leclowndu93150.wakes;

import com.leclowndu93150.wakes.particle.ModParticles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(WakesClient.MOD_ID)
public class WakesClient {

	public static final String MOD_ID = "wakes";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static boolean areShadersEnabled = false;

	public WakesClient() {
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		ModParticles.register(modEventBus);
		DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientSetup::init);
	}

	public static boolean areShadersEnabled() {
		return areShadersEnabled;
	}

	public static class ClientSetup {
		public static void init() {
			ClientRegistry.init();
		}
	}
}