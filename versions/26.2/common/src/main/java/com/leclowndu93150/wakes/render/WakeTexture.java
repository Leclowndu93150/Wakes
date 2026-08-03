package com.leclowndu93150.wakes.render;

import com.leclowndu93150.wakes.WakesClient;
import com.leclowndu93150.wakes.mixin.RenderTypeInvoker;
import com.leclowndu93150.wakes.render.enums.WakesRenderType;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class WakeTexture extends AbstractTexture {
    private static final AtomicInteger SEQ = new AtomicInteger();

    public final int res;
    public final boolean isUsingBricks;
    private final int resolutionScaling;
    private final Identifier identifier;
    private RenderType renderType;
    private RenderPipeline renderTypePipeline;

    public WakeTexture(int res, boolean useBricks, int scaling) {
        this.res = res;
        this.isUsingBricks = useBricks;
        this.resolutionScaling = scaling;

        int dim = scaling * res;
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
        if (this.texture == null) return;
        int dim = resolutionScaling * res;
        ByteBuffer pixels = MemoryUtil.memByteBuffer(imgPtr, 4 * dim * dim);
        RenderSystem.getDevice().createCommandEncoder().writeToTexture(this.texture, pixels, 0, 0, 0, 0, dim, dim);
    }

    public void destroy() {
        Minecraft.getInstance().getTextureManager().release(this.identifier);
    }
}
