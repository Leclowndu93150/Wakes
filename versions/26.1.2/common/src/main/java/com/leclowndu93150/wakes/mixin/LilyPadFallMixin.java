package com.leclowndu93150.wakes.mixin;

import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.config.enums.EffectSpawningRule;
import com.leclowndu93150.wakes.duck.ProducesWake;
import com.leclowndu93150.wakes.simulation.WakeNode;
import com.leclowndu93150.wakes.utils.WakesUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public class LilyPadFallMixin {

    @Inject(at = @At("TAIL"), method = "fallOn")
    public void onLandedUpon(Level world, BlockState state, BlockPos pos, Entity entity, double fallDistance, CallbackInfo ci) {
        if (!world.getBlockState(pos.above()).is(Blocks.LILY_PAD)) return;
        if (WakesConfig.GENERAL.disableMod.get()) return;
        EffectSpawningRule rule = WakesUtils.getEffectRuleFromSource(entity);
        ProducesWake wakeProducer = (ProducesWake) entity;
        if (rule.simulateWakes) {
            wakeProducer.wakes$setWakeHeight(pos.getY() + WakeNode.WATER_OFFSET);
            WakesUtils.placeFallSplash(entity);
        }
    }

}
