package com.leclowndu93150.wakes.utils;

import com.leclowndu93150.wakes.render.WakeTexture;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public class LightmapWrapper {
    public static long imgPtr = -1;
    public static WakeTexture texture;

    public static void initTexture() {
        imgPtr = MemoryUtil.nmemAlloc(16 * 16 * 3);
        texture = new WakeTexture(16, false);
    }

    public static void updateTexture(LightTexture lightmapTextureManager) {
        if (imgPtr == -1) {
            initTexture();
        }
        RenderSystem.bindTexture(lightmapTextureManager.target.getColorTextureId());
        GlStateManager._getTexImage(GlConst.GL_TEXTURE_2D, 0, GlConst.GL_BGR, GlConst.GL_UNSIGNED_BYTE, imgPtr);
    }

    public static int readPixel(int block, int sky) {
        if (imgPtr == -1) {
            return 0;
        }
        int index = (block + sky * 16) * 3;

        return MemoryUtil.memGetInt(imgPtr + index);
    }

    public static void render(Matrix4f matrix) {
        texture.loadTexture(imgPtr, GlConst.GL_BGR);

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        int middleX = Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2;
        buffer.addVertex(matrix, middleX - 50, 0, 0)
                .setUv(0, 0)
                .setColor(1f, 1f, 1f, 1f);
        buffer.addVertex(matrix,  middleX + 50, 0, 0)
                .setUv(0, 1)
                .setColor(1f, 1f, 1f, 1f);
        buffer.addVertex(matrix, middleX + 50, 100, 0)
                .setUv(1, 1)
                .setColor(1f, 1f, 1f, 1f);
        buffer.addVertex(matrix, middleX - 50, 100, 0)
                .setUv(1, 0)
                .setColor(1f, 1f, 1f, 1f);

        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }
}
