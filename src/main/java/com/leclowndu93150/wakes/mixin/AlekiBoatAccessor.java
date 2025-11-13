package com.leclowndu93150.wakes.mixin;

import com.alekiponi.alekiships.common.entity.vehicle.AbstractAlekiBoatEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AbstractAlekiBoatEntity.class, remap = false)
public interface AlekiBoatAccessor {
    @Accessor("paddlePositions")
    float[] wakes$getPaddlePositions();
}
