package com.leclowndu93150.wakes.mixin;

import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.debug.WakesDebugInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import net.minecraft.client.gui.components.DebugScreenOverlay;

@Mixin(DebugScreenOverlay.class)
public abstract class DebugHudMixin {

    @Inject(at = @At("RETURN"), method = "getGameInformation")
    protected void getLeftText(CallbackInfoReturnable<List<String>> info) {
        if (WakesConfig.DEBUG.showDebugInfo.get()) {
            if (WakesConfig.GENERAL.disableMod.get()) {
                info.getReturnValue().add("[Wakes] Mod disabled!");
            } else {
                WakesDebugInfo.show(info);
            }
        }
    }
}