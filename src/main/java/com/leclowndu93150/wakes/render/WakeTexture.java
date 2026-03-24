package com.leclowndu93150.wakes.render;

import com.leclowndu93150.wakes.WakesClient;
import com.leclowndu93150.wakes.simulation.WakeHandler;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import org.lwjgl.system.MemoryUtil;

public class WakeTexture {
    public int res;
    public final boolean isUsingBricks;
    private final int resolutionScaling;
    private final GpuTexture texture;
    private final GpuTextureView textureView;
    private final NativeImage image;

    public WakeTexture(int res, boolean useBricks, int scaling) {
        this.res = res;
        this.isUsingBricks = useBricks;
        this.resolutionScaling = scaling;

        int dim = scaling * res;
        this.image = new NativeImage(dim, dim, false);
        this.texture = RenderSystem.getDevice().createTexture(
                () -> WakesClient.MOD_ID + " wake texture",
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                TextureFormat.RGBA8, dim, dim, 1, 1);
        this.textureView = RenderSystem.getDevice().createTextureView(this.texture);
    }

    public void loadTexture(long imgPtr) {
        int dim = resolutionScaling * WakeHandler.resolution.res;
        for (int y = 0; y < dim; y++) {
            for (int x = 0; x < dim; x++) {
                long offset = 4L * ((long) y * dim + x);
                int pixel = MemoryUtil.memGetInt(imgPtr + offset);
                image.setPixelABGR(x, y, pixel);
            }
        }
        RenderSystem.getDevice().createCommandEncoder().writeToTexture(this.texture, this.image);
    }

    public GpuTextureView getTextureView() {
        return this.textureView;
    }

    public void close() {
        this.image.close();
        this.textureView.close();
        this.texture.close();
    }
}
