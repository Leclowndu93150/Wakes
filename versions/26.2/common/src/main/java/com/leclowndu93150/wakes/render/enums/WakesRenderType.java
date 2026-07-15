package com.leclowndu93150.wakes.render.enums;

import com.leclowndu93150.wakes.WakesClient;
import com.leclowndu93150.wakes.config.WakesConfig;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public enum WakesRenderType {
    AUTO(null),
    TRANSLUCENT_BLOCK(RenderPipelines.TRANSLUCENT_BLOCK),
    BEACON_BEAM_TRANSLUCENT(RenderPipelines.BEACON_BEAM_TRANSLUCENT);

    public static final RenderPipeline WAKE_TRANSLUCENT_LIT = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("wakes", "pipeline/wake_translucent_lit"))
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.FOG)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withVertexShader("core/block")
            .withFragmentShader("core/block")
            .withVertexBinding(0, DefaultVertexFormat.BLOCK)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withShaderDefine("ALPHA_CUTOUT", 0.01F)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .build();

    public final RenderPipeline pipeline;

    WakesRenderType(RenderPipeline pipeline) {
        this.pipeline = pipeline;
    }

    public static RenderPipeline getPipeline() {
        WakesRenderType configured = WakesConfig.DEBUG.renderType.get();
        if (configured == WakesRenderType.AUTO) {
            if (WakesClient.areShadersEnabled) {
                return TRANSLUCENT_BLOCK.pipeline;
            } else {
                return WAKE_TRANSLUCENT_LIT;
            }
        }
        if (configured == WakesRenderType.BEACON_BEAM_TRANSLUCENT) {
            return WAKE_TRANSLUCENT_LIT;
        }
        return configured.pipeline;
    }
}
