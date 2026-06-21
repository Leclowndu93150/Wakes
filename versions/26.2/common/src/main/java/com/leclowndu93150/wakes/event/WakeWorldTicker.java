package com.leclowndu93150.wakes.event;

import com.leclowndu93150.wakes.debug.WakesDebugInfo;
import com.leclowndu93150.wakes.simulation.WakeHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class WakeWorldTicker {

    public static void onLevelTick(ClientLevel level) {
        WakesDebugInfo.reset();
        WakeHandler.getInstance(level).ifPresent(WakeHandler::tick);
    }

    public static void onPlayerChangedDimension(Entity entity, ResourceKey<Level> fromDimension) {
        if (entity.level() instanceof ClientLevel) {
            WakeHandler.init(entity.level());

            if (Minecraft.getInstance().level == null ||
                    !Minecraft.getInstance().level.dimension().equals(fromDimension)) {
                WakeHandler.killDimension(fromDimension);
            }
        }
    }

    public static void onPlayerLoggedIn(Entity entity) {
        if (entity.level() instanceof ClientLevel) {
            WakeHandler.init(entity.level());
        }
    }
}
