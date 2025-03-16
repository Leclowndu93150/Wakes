package com.leclowndu93150.wakes.render.enums;

import com.leclowndu93150.wakes.WakesClient;
import com.leclowndu93150.wakes.config.WakesConfig;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.ShaderProgram;

public enum RenderType {
    AUTO(null),
    GENERAL(CoreShaders.POSITION_TEX_COLOR),
    ENTITY_TRANSLUCENT_CULL(CoreShaders.RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL),
    SOLID(CoreShaders.RENDERTYPE_SOLID);
    /*
    TRANSLUCENT(GameRenderer::getRendertypeTranslucentShader),
    CUTOUT(GameRenderer::getRendertypeCutoutShader),
    ENTITY_SOLID(GameRenderer::getRendertypeEntitySolidShader),
    ENTITY_TRANSLUCENT(GameRenderer::getRendertypeEntityTranslucentShader),
    ENTITY_CUTOUT(GameRenderer::getRendertypeEntityCutoutShader),
    ENTITY_CUTOUT_NO_CULL(GameRenderer::getRendertypeEntityCutoutNoCullShader),
    ENTITY_CUTOUT_NO_CULL_Z_OFFSET(GameRenderer::getRendertypeEntityCutoutNoCullZOffsetShader);
     */

    public final ShaderProgram program;

    RenderType(ShaderProgram program) {
        this.program = program;
    }

    public static ShaderProgram getProgram() {
        if (WakesConfig.DEBUG.renderType.get() == RenderType.AUTO) {
            if (WakesClient.areShadersEnabled) {
                return ENTITY_TRANSLUCENT_CULL.program;
            } else {
                return GENERAL.program;
            }
        }
        return WakesConfig.DEBUG.renderType.get().program;
    }
}