package com.leclowndu93150.wakes.render;

import com.leclowndu93150.wakes.WakesMod;
import com.leclowndu93150.wakes.config.WakesConfig;
import java.awt.*;

public class WakeColor {
    public final int argb;
    public final int abgr;
    public final int r;
    public final int g;
    public final int b;
    public final int a;
    public final float h;
    public final float s;
    public final float v;

    public WakeColor(int argb) {
        this(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >> 24 & 0xFF);
    }

    public WakeColor(int red, int green, int blue, int alpha) {
        this.argb = alpha << 24 | red << 16 | green << 8 | blue;
        this.abgr = alpha << 24 | blue << 16 | green << 8 | red;
        this.a = alpha;
        this.r = red;
        this.g = green;
        this.b = blue;
        float[] hsv = Color.RGBtoHSB(red, green, blue, null);
        this.h = hsv[0];
        this.s = hsv[1];
        this.v = hsv[2];
    }

    public WakeColor(float hue, float saturation, float value, float opacity) {
        this(((int) ((1f - opacity) * 255)) << 24 ^ Color.HSBtoRGB(hue, saturation, value));
    }

    public WakeColor(String argbHex) {
        this((int) Long.parseLong(argbHex.replace("#", ""), 16));
    }

    public String toHex() {
        return "#" + Integer.toHexString(a << 24 | r << 16 | g << 8 | b);
    }

    private static double invertedLogisticCurve(float x) {
        float k = (float) WakesConfig.shaderLightPassthrough;
        return WakesMod.areShadersEnabled ? k * (4 * Math.pow(x - 0.5f, 3) + 0.5f) : x;
    }

    public static int sampleColor(float waveEqAvg, int fluidCol, int lightColor, float opacity) {
        int tintR = fluidCol >> 16 & 0xFF;
        int tintG = fluidCol >> 8 & 0xFF;
        int tintB = fluidCol & 0xFF;
        double clampedRange = 1 / (1 + Math.exp(-0.1 * waveEqAvg));
        double[] ranges = WakesConfig.wakeColorIntervals;
        int returnIndex = ranges.length;
        for (int i = 0; i < ranges.length; i++) {
            if (clampedRange < ranges[i]) {
                returnIndex = i;
                break;
            }
        }
        WakeColor color = WakesConfig.getWakeColor(returnIndex);
        return blendFast(color, tintR, tintG, tintB, lightColor, opacity);
    }

    private static int blendFast(WakeColor color, int tintR, int tintG, int tintB, int lightColor, float opacity) {
        double srcA = Math.pow(color.a / 255f, WakesConfig.blendStrength * 10);
        double invSrcA = 1 - srcA;

        int r = (int) (color.r * srcA + tintR * invSrcA);
        int g = (int) (color.g * srcA + tintG * invSrcA);
        int b = (int) (color.b * srcA + tintB * invSrcA);

        r = (int) (r * invertedLogisticCurve((lightColor & 0xFF) / 255f));
        g = (int) (g * invertedLogisticCurve((lightColor >> 8 & 0xFF) / 255f));
        b = (int) (b * invertedLogisticCurve((lightColor >> 16 & 0xFF) / 255f));

        int a = (int) (color.a * opacity);
        return a << 24 | b << 16 | g << 8 | r;
    }

    public WakeColor blend(WakeColor tint, int lightColor, float opacity) {
        double srcA = Math.pow(this.a / 255f, WakesConfig.blendStrength * 10);

        int r = (int) ((this.r) * (srcA) + (tint.r) * (1 - srcA));
        int g = (int) ((this.g) * (srcA) + (tint.g) * (1 - srcA));
        int b = (int) ((this.b) * (srcA) + (tint.b) * (1 - srcA));

        r = (int) ((r * invertedLogisticCurve((lightColor & 0xFF) / 255f)));
        g = (int) ((g * invertedLogisticCurve((lightColor >> 8 & 0xFF) / 255f)));
        b = (int) ((b * invertedLogisticCurve((lightColor >> 16 & 0xFF) / 255f)));

        return new WakeColor(r, g, b, (int) (this.a * opacity));
    }
}
