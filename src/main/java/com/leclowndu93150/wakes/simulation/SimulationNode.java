package com.leclowndu93150.wakes.simulation;

import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.render.WakeColor;
import com.leclowndu93150.wakes.utils.WakesUtils;

import javax.annotation.Nullable;

public abstract class SimulationNode {
    public float[][][] u;
    public float[][] initialValues;
    public final int res;

    public SimulationNode() {
        this.res = WakeHandler.resolution.res;
        this.u = new float[3][res + 2][res + 2];
        this.initialValues = new float[res + 2][res + 2];
    }

    public void setInitialValue(long pos, int val) {
        float resFactor = res / 16f;
        int x = (int) (pos >> 32);
        int z = (int) pos;
        if (x < 0) x += res;
        if (z < 0) z += res;
        float v = val * resFactor;
        for (int i = -1; i < 2; i++) {
            float[] row = this.initialValues[z + i + 1];
            for (int j = -1; j < 2; j++) {
                row[x + j + 1] = v;
            }
        }
    }

    public int getPixelColor(int x, int z, int fluidCol, int lightCol, float opacity) {
        int zz = z + 1, xx = x + 1;
        float waveEqAvg = (this.u[0][zz][xx] + this.u[1][zz][xx] + this.u[2][zz][xx]) * 0.33333334f;
        if (WakesConfig.debugColors) {
            int clampedRange = (int) (255 * (2 / (1 + Math.exp(-0.1 * waveEqAvg)) - 1));
            int rr = Math.max(-clampedRange, 0);
            int gg = Math.max(clampedRange, 0);
            return 0xFF000000 | rr | (gg << 8);
        }
        return WakeColor.sampleColor(waveEqAvg, fluidCol, lightCol, opacity);
    }

    public abstract void tick(@Nullable Float velocity, @Nullable SimulationNode NORTH, @Nullable SimulationNode SOUTH, @Nullable SimulationNode EAST, @Nullable SimulationNode WEST);

    public static class WakeSimulation extends SimulationNode {

        private static float cachedAlpha;
        private static float cachedBeta;
        private static double lastPropagation = -1;
        private static double lastDecay = -1;

        private static void updateCachedCoefficients() {
            double prop = WakesConfig.wavePropagationFactor;
            double decay = WakesConfig.waveDecayFactor;
            if (prop != lastPropagation || decay != lastDecay) {
                lastPropagation = prop;
                lastDecay = decay;
                float factor = (float) (prop * 16f / 20f);
                cachedAlpha = factor * factor;
                cachedBeta = (float) (Math.log(10 * decay + 10) / Math.log(20));
            }
        }

        @Override
        public void tick(@Nullable Float velocity, @Nullable SimulationNode NORTH, @Nullable SimulationNode SOUTH, @Nullable SimulationNode EAST, @Nullable SimulationNode WEST) {
            updateCachedCoefficients();
            float alpha = cachedAlpha;
            float beta = cachedBeta;

            float[][] u0 = this.u[0];
            float[][] u1 = this.u[1];
            float[][] u2 = this.u[2];
            int max = res + 1;

            for (int i = 2; i >= 1; i--) {
                if (NORTH != null) this.u[i][0] = NORTH.u[i][res];
                if (SOUTH != null) this.u[i][res + 1] = SOUTH.u[i][1];
                if (EAST != null || WEST != null) {
                    for (int z = 0; z < res + 2; z++) {
                        if (EAST != null) this.u[i][z][res + 1] = EAST.u[i][z][1];
                        if (WEST != null) this.u[i][z][0] = WEST.u[i][z][res];
                    }
                }
            }

            for (int z = 1; z < max; z++) {
                float[] row0 = u0[z];
                float[] row1 = u1[z];
                float[] row2 = u2[z];
                float[] initRow = this.initialValues[z];
                for (int x = 1; x < max; x++) {
                    row0[x] += initRow[x];
                    initRow[x] = 0;
                    row2[x] = row1[x];
                    row1[x] = row0[x];
                }
            }

            for (int z = 1; z < max; z++) {
                float[] rowAbove = u1[z - 1];
                float[] row = u1[z];
                float[] rowBelow = u1[z + 1];

                for (int x = 1; x < max; x++) {
                    float center = row[x];
                    float val =
                        0.5f * rowAbove[x] +
                        0.25f * rowAbove[x + 1] +
                        0.5f * row[x + 1] +
                        0.25f * rowBelow[x + 1] +
                        0.5f * rowBelow[x] +
                        0.25f * rowBelow[x - 1] +
                        0.5f * row[x - 1] +
                        0.25f * rowAbove[x - 1] -
                        3f * center;

                    u0[z][x] = (alpha * val + 2f * center - u2[z][x]) * beta;
                }
            }
        }
    }

    public static class SplashPlaneSimulation extends SimulationNode {

        @Override
        public void tick(@Nullable Float velocity, @Nullable SimulationNode NORTH, @Nullable SimulationNode SOUTH, @Nullable SimulationNode EAST, @Nullable SimulationNode WEST) {
            double t = System.currentTimeMillis() / (double) 1000;
            if (velocity == null) return;
            int p = (int) (14 * Math.min(1f, 2 * velocity / WakesConfig.maxSplashPlaneVelocity));
            for (int z = 1; z < res + 1; z++) {
                for (int x = 1; x < res + 1; x++) {
                    this.u[0][z][x] = 0;
                    double v = Math.atan((z - 16f) / x);
                    double d = Math.sqrt(Math.pow(z - 16, 2) + Math.pow(x, 2)) + 0.5 * Math.sin(10 * v - 2 * Math.PI * t);

                    if (d < p) {
                        this.u[0][z][x] = (float) (200 * Math.pow(d - p, 2) / (d * d));
                    }
                }
            }
        }
    }
}
