package com.leclowndu93150.wakes.render;

import com.leclowndu93150.wakes.WakesClient;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class WakeTexture {
    public final int res;
    public final boolean isUsingBricks;
    private final int resolutionScaling;
    private final GpuTexture texture;
    private final GpuTextureView textureView;

    public WakeTexture(int res, boolean useBricks, int scaling) {
        this.res = res;
        this.isUsingBricks = useBricks;
        this.resolutionScaling = scaling;

        int dim = scaling * res;
        this.texture = RenderSystem.getDevice().createTexture(
                () -> WakesClient.MOD_ID + " wake texture",
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                TextureFormat.RGBA8, dim, dim, 1, 1);
        this.textureView = RenderSystem.getDevice().createTextureView(this.texture);
    }

    public void loadTexture(long imgPtr) {
        int dim = resolutionScaling * res;
        ByteBuffer pixels = MemoryUtil.memByteBuffer(imgPtr, 4 * dim * dim);
        RenderSystem.getDevice().createCommandEncoder().writeToTexture(this.texture, pixels, NativeImage.Format.RGBA, 0, 0, 0, 0, dim, dim);
    }

    public GpuTextureView getTextureView() {
        return this.textureView;
    }

    public void close() {
        this.textureView.close();
        this.texture.close();
    }
}
