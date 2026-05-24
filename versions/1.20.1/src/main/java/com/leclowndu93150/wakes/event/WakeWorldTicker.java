package com.leclowndu93150.wakes.event;

import com.leclowndu93150.wakes.WakesClient;
import com.leclowndu93150.wakes.simulation.WakeHandler;
import com.leclowndu93150.wakes.debug.WakesDebugInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WakeWorldTicker {

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if(event.phase != TickEvent.Phase.END) return;
        if (event.level instanceof ClientLevel clientLevel) {
            WakesClient.areShadersEnabled = WakesClient.areShadersEnabled();
            WakesDebugInfo.reset();
            
            WakeHandler.getInstance(clientLevel).ifPresent(WakeHandler::tick);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() != null && event.getEntity().level() instanceof ClientLevel) {
            WakeHandler.init(event.getEntity().level());

            ResourceKey<Level> fromDimension = event.getFrom();
            if (Minecraft.getInstance().level == null ||
                    !Minecraft.getInstance().level.dimension().equals(fromDimension)) {
                WakeHandler.killDimension(fromDimension);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() != null && event.getEntity().level() instanceof ClientLevel) {
            WakeHandler.init(event.getEntity().level());
        }
    }
}