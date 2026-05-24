package com.leclowndu93150.wakes.mixin.sable;

import com.leclowndu93150.wakes.compat.sable.SableCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.ryanhcode.sable.sublevel.plot.LevelPlot", remap = false)
public abstract class SableLevelPlotMixin {
    @Inject(method = "onBlockChange", at = @At("TAIL"), require = 0, remap = false)
    private void wakes$invalidateShape(BlockPos pos, BlockState state, CallbackInfo ci) {
        SableCompat.invalidateShapeFromPlot(this);
    }
}
