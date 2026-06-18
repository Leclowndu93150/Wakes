package com.leclowndu93150.wakes.event;

import com.leclowndu93150.wakes.simulation.WakeHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;

public class ChunkWakeCleanup {

    public static void onChunkUnload(ClientLevel level, ChunkPos pos) {
        WakeHandler.getInstance().ifPresent(handler ->
                handler.cleanupChunk(pos.getMinBlockX(), pos.getMinBlockZ(),
                        pos.getMaxBlockX(), pos.getMaxBlockZ()));
    }
}
