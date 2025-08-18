package com.leclowndu93150.wakes.render;

import com.leclowndu93150.wakes.simulation.QuadTree;
import com.leclowndu93150.wakes.simulation.WakeHandler;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.neoforged.neoforge.client.blaze3d.validation.ValidationGpuTexture;

public class WakeTexture {
    public int res;
    public final boolean isUsingBricks;
    private final int resolutionScaling;
    private final GpuTextureView textureView;
    public GpuTexture texture;

    public WakeTexture(int res, boolean useBricks) {
        this.res = res;
        this.isUsingBricks = useBricks;
        this.resolutionScaling = useBricks ? QuadTree.BRICK_WIDTH : 1;

        this.texture = RenderSystem.getDevice().createTexture(() -> "Wake Texture", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                TextureFormat.RGBA8, resolutionScaling * res, resolutionScaling * res, 1, 1);
        texture.setTextureFilter(FilterMode.NEAREST, false);
        this.textureView = RenderSystem.getDevice().createTextureView(texture);
    }

    public void loadTexture(long imgPtr, int glFormat) {
        GlStateManager._pixelStore(GlConst.GL_UNPACK_ROW_LENGTH, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_PIXELS, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_ROWS, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_ALIGNMENT, 4);

        int dim = resolutionScaling * WakeHandler.resolution.res;

        GpuTexture actualTexture = texture;
        if (texture instanceof ValidationGpuTexture) {
            actualTexture = ((ValidationGpuTexture) texture).getRealTexture();
        }

        if (actualTexture instanceof GlTexture glTexture) {
            GlStateManager._bindTexture(glTexture.glId());
            GlStateManager._texSubImage2D(GlConst.GL_TEXTURE_2D, 0, 0, 0, dim, dim, glFormat, GlConst.GL_UNSIGNED_BYTE, imgPtr);
        }

        //RenderSystem.setShaderTexture(0, texture);
        //RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        //RenderSystem.enableDepthTest(); // Is it THIS simple? https://github.com/Goby56/wakes/issues/46
        //RenderSystem.disableCull();
        RenderSystem.setShaderTexture(0, textureView);
    }

    public GpuTexture getTexture() {
        return texture;
    }

    public GpuTextureView getTextureView() {
        return textureView;
    }
}
