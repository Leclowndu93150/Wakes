package com.leclowndu93150.wakes.render;

import com.leclowndu93150.wakes.WakesClient;
import com.leclowndu93150.wakes.config.WakesConfig;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class WakeColor {
    private static final ConcurrentHashMap<String, Integer> HEX_PARSE_CACHE = new ConcurrentHashMap<>();
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
        // Minecraft seems to work with argb but OpenGL uses abgr
        this(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >> 24 & 0xFF);
    }

    public WakeColor(int red, int green, int blue, int alpha) {
        this.argb = alpha << 24 | red << 16 | green << 8 | blue;
        this.abgr = alpha << 24 | blue << 16 | green << 8 | red;
        this.a = alpha;
        this.r = red;
        this.g = green;
        this.b = blue;
        var hsv = Color.RGBtoHSB(red, green, blue, null);
        this.h = hsv[0];
        this.s = hsv[1];
        this.v = hsv[2];
    }

    public WakeColor(float hue, float saturation, float value, float opacity) {
        this(((int)((1f - opacity) * 255)) << 24 ^ Color.HSBtoRGB(hue, saturation, value));
    }

    public WakeColor(String argbHex) {
        this(HEX_PARSE_CACHE.computeIfAbsent(argbHex, k -> Integer.parseUnsignedInt(k.replace("#", ""), 16)));
    }

    public String toHex() {
        return "#" + Integer.toHexString(a << 24 | r << 16 | g << 8 | b);
    }

    private static double invertedLogisticCurve(float x) {
        float k = WakesConfig.APPEARANCE.shaderLightPassthrough.get().floatValue();
        return WakesClient.areShadersEnabled ? k * (4 * Math.pow(x - 0.5f, 3) + 0.5f) : x;
    }

    private static double cachedBlendStrength = -1;
    private static double cachedBlendExponent = -1;
    private static double[] cachedSrcA;
    private static double[] cachedInvSrcA;
    private static List<?> cachedIntervals = null;
    private static double[] cachedIntervalsArray;
    private static int cachedIntervalsSize;

    private static void updateBlendCache() {
        double val = WakesConfig.APPEARANCE.blendStrength.getAsDouble();
        if (val != cachedBlendStrength) {
            cachedBlendStrength = val;
            cachedBlendExponent = val * 10;
            cachedSrcA = new double[256];
            cachedInvSrcA = new double[256];
            for (int i = 0; i < 256; i++) {
                cachedSrcA[i] = Math.pow(i / 255.0, cachedBlendExponent);
                cachedInvSrcA[i] = 1.0 - cachedSrcA[i];
            }
        }
    }

    private static void updateIntervalsCache() {
        var intervals = WakesConfig.APPEARANCE.wakeColorIntervals.get();
        if (intervals != cachedIntervals) {
            cachedIntervals = intervals;
            cachedIntervalsSize = intervals.size();
            cachedIntervalsArray = new double[cachedIntervalsSize];
            for (int i = 0; i < cachedIntervalsSize; i++) {
                cachedIntervalsArray[i] = (Double) intervals.get(i);
            }
        }
    }

    private static double fastSigmoid(double x) {
        double ax = 0.1 * x;
        if (ax > 6) return 1.0;
        if (ax < -6) return 0.0;
        return 1.0 / (1.0 + Math.exp(-ax));
    }

    public static int sampleColor(float waveEqAvg, int fluidCol, int lightColor, float opacity) {
        updateBlendCache();
        updateIntervalsCache();

        int tintR = fluidCol >> 16 & 0xFF;
        int tintG = fluidCol >> 8 & 0xFF;
        int tintB = fluidCol & 0xFF;

        double clampedRange = fastSigmoid(waveEqAvg);
        int returnIndex = cachedIntervalsSize;
        for (int i = 0; i < cachedIntervalsSize; i++) {
            if (clampedRange < cachedIntervalsArray[i]) {
                returnIndex = i;
                break;
            }
        }
        WakeColor color = WakesConfig.getWakeColor(returnIndex);
        return blendFast(color, tintR, tintG, tintB, lightColor, opacity);
    }

    private static int blendFast(WakeColor color, int tintR, int tintG, int tintB, int lightColor, float opacity) {
        double srcA = cachedSrcA[color.a];
        double invSrcA = cachedInvSrcA[color.a];

        int r = (int) (color.r * srcA + tintR * invSrcA);
        int g = (int) (color.g * srcA + tintG * invSrcA);
        int b = (int) (color.b * srcA + tintB * invSrcA);

        r = (int) (r * invertedLogisticCurve((lightColor & 0xFF) / 255f));
        g = (int) (g * invertedLogisticCurve((lightColor >> 8 & 0xFF) / 255f));
        b = (int) (b * invertedLogisticCurve((lightColor >> 16 & 0xFF) / 255f));

        int a = (int) (color.a * opacity);
        return a << 24 | b << 16 | g << 8 | r;
    }

}
