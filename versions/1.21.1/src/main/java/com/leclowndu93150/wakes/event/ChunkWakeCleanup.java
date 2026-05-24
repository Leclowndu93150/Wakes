package com.leclowndu93150.wakes.event;

import com.leclowndu93150.wakes.simulation.WakeHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class ChunkWakeCleanup {

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ClientLevel) {
            ChunkPos pos = event.getChunk().getPos();
            WakeHandler.getInstance().ifPresent(handler ->
                    handler.cleanupChunk(pos.getMinBlockX(), pos.getMinBlockZ(),
                            pos.getMaxBlockX(), pos.getMaxBlockZ()));
        }
    }
}