package com.leclowndu93150.wakes.event;

import com.leclowndu93150.wakes.WakesClient;
import com.leclowndu93150.wakes.simulation.WakeHandler;
import com.leclowndu93150.wakes.debug.WakesDebugInfo;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class WakeWorldTicker {

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ClientLevel clientLevel) {
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