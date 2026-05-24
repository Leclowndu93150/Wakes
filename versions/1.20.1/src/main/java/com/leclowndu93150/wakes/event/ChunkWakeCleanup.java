package com.leclowndu93150.wakes.event;

import com.leclowndu93150.wakes.simulation.WakeHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
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