package com.leclowndu93150.wakes.mixin;

import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractBoat.class)
public interface AbstractBoatAccessor {
    @Accessor("paddlePositions")
    float[] wakes$paddlePositions();

    @Accessor("PADDLE_SPEED")
    static float wakes$paddleSpeed() {
        throw new AssertionError();
    }
}
