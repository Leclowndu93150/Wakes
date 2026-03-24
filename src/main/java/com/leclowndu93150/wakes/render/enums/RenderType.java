package com.leclowndu93150.wakes.render.enums;

import com.leclowndu93150.wakes.WakesClient;
import com.leclowndu93150.wakes.config.WakesConfig;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;

public enum RenderType {
    AUTO(null),
    TRANSLUCENT_BLOCK(RenderPipelines.TRANSLUCENT_BLOCK),
    BEACON_BEAM_TRANSLUCENT(RenderPipelines.BEACON_BEAM_TRANSLUCENT);

    public final RenderPipeline pipeline;

    RenderType(RenderPipeline pipeline) {
        this.pipeline = pipeline;
    }

    public static RenderPipeline getPipeline() {
        if (WakesConfig.DEBUG.renderType.get() == RenderType.AUTO) {
            if (WakesClient.areShadersEnabled) {
                return TRANSLUCENT_BLOCK.pipeline;
            } else {
                return BEACON_BEAM_TRANSLUCENT.pipeline;
            }
        }
        return WakesConfig.DEBUG.renderType.get().pipeline;
    }
}
