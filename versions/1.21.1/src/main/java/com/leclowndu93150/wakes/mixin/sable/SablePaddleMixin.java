package com.leclowndu93150.wakes.mixin.sable;

import com.leclowndu93150.wakes.compat.sable.SableCompat;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.ryanhcode.sable.network.client.ClientSubLevelPunchHelper", remap = false)
public abstract class SablePaddleMixin {
    @Inject(method = "clientTryPunch", at = @At("HEAD"), require = 0, remap = false)
    private static void wakes$spawnPaddleWake(BlockHitResult hit, Level level, boolean punching, CallbackInfo ci) {
        if (punching) return;
        SableCompat.spawnPaddleWake(hit, level);
    }
}
