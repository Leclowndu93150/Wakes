package com.leclowndu93150.wakes.render;

import com.leclowndu93150.wakes.WakesClient;
import com.leclowndu93150.wakes.mixin.RenderTypeInvoker;
import com.leclowndu93150.wakes.render.enums.WakesRenderType;
import com.leclowndu93150.wakes.simulation.WakeHandler;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryUtil;

import java.util.concurrent.atomic.AtomicInteger;

public class WakeTexture extends AbstractTexture {
    private static final AtomicInteger SEQ = new AtomicInteger();

    public int res;
    public final boolean isUsingBricks;
    private final int resolutionScaling;
    private final NativeImage image;
    private final Identifier identifier;
    private RenderType renderType;
    private RenderPipeline renderTypePipeline;

    public WakeTexture(int res, boolean useBricks, int scaling) {
        this.res = res;
        this.isUsingBricks = useBricks;
        this.resolutionScaling = scaling;

        int dim = scaling * res;
        this.image = new NativeImage(dim, dim, false);
        GpuTexture tex = RenderSystem.getDevice().createTexture(
                () -> WakesClient.MOD_ID + " wake texture",
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                GpuFormat.RGBA8_UNORM, dim, dim, 1, 1);
        this.texture = tex;
        this.textureView = RenderSystem.getDevice().createTextureView(tex);
        this.sampler = RenderSystem.getSamplerCache().getSampler(
                AddressMode.REPEAT, AddressMode.REPEAT,
                FilterMode.NEAREST, FilterMode.NEAREST, false);

        this.identifier = Identifier.fromNamespaceAndPath(
                WakesClient.MOD_ID, "wake_tex_" + res + "_" + SEQ.getAndIncrement());
        Minecraft.getInstance().getTextureManager().register(this.identifier, this);
    }

    public Identifier identifier() {
        return this.identifier;
    }

    public RenderType renderType() {
        RenderPipeline pipeline = WakesRenderType.getPipeline();
        if (renderType == null || renderTypePipeline != pipeline) {
            renderTypePipeline = pipeline;
            RenderSetup setup = RenderSetup.builder(pipeline)
                    .withTexture("Sampler0", identifier)
                    .useLightmap()
                    .createRenderSetup();
            renderType = RenderTypeInvoker.wakes$create("wakes:wake_" + identifier.getPath(), setup);
        }
        return renderType;
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

    @Override
    public void close() {
        this.image.close();
        super.close();
    }
}
