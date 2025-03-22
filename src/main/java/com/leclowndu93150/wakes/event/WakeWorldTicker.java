package com.leclowndu93150.wakes.event;

import com.leclowndu93150.wakes.WakesClient;
import com.leclowndu93150.wakes.simulation.WakeHandler;
import com.leclowndu93150.wakes.debug.WakesDebugInfo;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
            WakeHandler.getInstance().ifPresent(WakeHandler::tick);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel destination = player.serverLevel();
            WakeHandler.init(destination);
        }
    }
}