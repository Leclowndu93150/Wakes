package com.leclowndu93150.wakes.render.enums;

import com.leclowndu93150.wakes.mixin.RenderTypeInvoker;
import com.leclowndu93150.wakes.render.WakeTexture;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

public enum WakesRenderType {
    AUTO(null),
    TRANSLUCENT_BLOCK(RenderPipelines.TRANSLUCENT_BLOCK),
    BEACON_BEAM_TRANSLUCENT(RenderPipelines.BEACON_BEAM_TRANSLUCENT);

    public static final RenderPipeline WAKE_TRANSLUCENT_LIT = RenderPipelines.TRANSLUCENT_BLOCK;

    public final RenderPipeline pipeline;

    WakesRenderType(RenderPipeline pipeline) {
        this.pipeline = pipeline;
    }

    public static RenderPipeline getPipeline() {
        return WAKE_TRANSLUCENT_LIT;
    }

    private static final Map<Identifier, RenderType> WAKE_TYPE_CACHE = new HashMap<>();

    public static RenderType wakeRenderTypeFor(WakeTexture tex) {
        return WAKE_TYPE_CACHE.computeIfAbsent(tex.identifier(), id -> {
            RenderSetup setup = RenderSetup.builder(getPipeline())
                    .withTexture("Sampler0", id)
                    .useLightmap()
                    .createRenderSetup();
            return RenderTypeInvoker.wakes$create("wakes:wake_" + id.getPath(), setup);
        });
    }
}
