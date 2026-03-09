package com.leclowndu93150.wakes.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeColorHelper;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.IFluidBlock;

public final class WaterTintUtils {
    private static final int DEFAULT_WATER_TINT = 0x3F76E4;
    private static final Map<Fluid, Integer> FLUID_COLOR_CACHE = new ConcurrentHashMap<>();

    private WaterTintUtils() {
    }

    public static int normalizeBiomeWaterColor(int biomeWaterColor) {
        return biomeWaterColor == 0xFFFFFF ? DEFAULT_WATER_TINT : biomeWaterColor;
    }

    public static int getFluidColor(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        if (block instanceof IFluidBlock) {
            Fluid fluid = ((IFluidBlock) block).getFluid();
            if (fluid != null && fluid != FluidRegistry.WATER) {
                return resolveFluidColor(fluid);
            }
        }

        return normalizeBiomeWaterColor(BiomeColorHelper.getWaterColorAtPos(world, pos));
    }

    private static int resolveFluidColor(Fluid fluid) {
        return FLUID_COLOR_CACHE.computeIfAbsent(fluid, f -> {
            int color = f.getColor();
            int rgb = color & 0x00FFFFFF;
            if (rgb != 0xFFFFFF && rgb != 0) {
                return rgb;
            }
            return sampleTextureColor(f);
        });
    }

    private static int sampleTextureColor(Fluid fluid) {
        try {
            ResourceLocation still = fluid.getStill();
            if (still == null) return DEFAULT_WATER_TINT;

            TextureMap map = Minecraft.getMinecraft().getTextureMapBlocks();
            TextureAtlasSprite sprite = map.getAtlasSprite(still.toString());
            if (sprite == null || sprite.getFrameCount() == 0) return DEFAULT_WATER_TINT;

            int width = sprite.getIconWidth();
            int height = sprite.getIconHeight();
            int[] pixels = sprite.getFrameTextureData(0)[0];
            if (pixels == null || pixels.length == 0) return DEFAULT_WATER_TINT;

            long r = 0, g = 0, b = 0;
            int count = 0;
            for (int i = 0; i < Math.min(pixels.length, width * height); i++) {
                int pixel = pixels[i];
                int a = (pixel >> 24) & 0xFF;
                if (a < 128) continue;
                r += (pixel >> 16) & 0xFF;
                g += (pixel >> 8) & 0xFF;
                b += pixel & 0xFF;
                count++;
            }

            if (count == 0) return DEFAULT_WATER_TINT;
            return (int) (r / count) << 16 | (int) (g / count) << 8 | (int) (b / count);
        } catch (Exception e) {
            return DEFAULT_WATER_TINT;
        }
    }

    public static void clearCache() {
        FLUID_COLOR_CACHE.clear();
    }
}
