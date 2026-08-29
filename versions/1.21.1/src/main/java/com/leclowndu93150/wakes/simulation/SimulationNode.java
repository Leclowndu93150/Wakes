package com.leclowndu93150.wakes.simulation;

import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.render.WakeColor;
import com.leclowndu93150.wakes.utils.WakesUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public abstract class SimulationNode {
    public float[][][] u;
    public float[][] initialValues;
    public final int res;

    public SimulationNode() {
        this.res = WakeHandler.getResolutionSize();
        this.u = new float[3][res+2][res+2];
        this.initialValues = new float[res+2][res+2];
    }

    public void setInitialValue(long pos, int val) {
        float resFactor = res / 16f;
        int[] xz = WakesUtils.longAsPos(pos);
        if (xz[0] < 0) xz[0] += res;
        if (xz[1] < 0) xz[1] += res;
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                this.initialValues[xz[1]+i+1][xz[0]+j+1] = val * resFactor;
            }
        }
    }

    public int getPixelColor(int x, int z, int fluidCol, int lightCol, float opacity) {
        float waveEqAvg = (this.u[0][z + 1][x + 1] + this.u[1][z + 1][x + 1] + this.u[2][z + 1][x + 1]) / 3;
        if (WakesConfig.DEBUG.debugColors.get()) {
            int clampedRange = (int) (255 * (2 / (1 + Math.exp(-0.1 * waveEqAvg)) - 1));
            return new WakeColor(Math.max(-clampedRange, 0), Math.max(clampedRange, 0), 0, 255).abgr;
        }
        return WakeColor.sampleColor(waveEqAvg, fluidCol, lightCol, opacity);
    }

    public abstract void tick(@Nullable Float velocity, @Nullable SimulationNode NORTH, @Nullable SimulationNode SOUTH, @Nullable SimulationNode EAST, @Nullable SimulationNode WEST);

    public void advance() {
    }

    public boolean isQuiet(float epsilon) {
        for (int z = 1; z < res + 1; z++) {
            float[] current = u[0][z];
            float[] previous = u[1][z];
            for (int x = 1; x < res + 1; x++) {
                if (Math.abs(current[x]) > epsilon || Math.abs(previous[x]) > epsilon) return false;
            }
        }
        return true;
    }

    public void solve(@Nullable Float velocity, @Nullable SimulationNode NORTH, @Nullable SimulationNode SOUTH, @Nullable SimulationNode EAST, @Nullable SimulationNode WEST) {
        tick(velocity, NORTH, SOUTH, EAST, WEST);
    }

    public static class WakeSimulation extends SimulationNode {
        private static float cachedAlpha;
        private static float cachedBeta;
        private static double lastPropagation = -1;
        private static double lastDecay = -1;

        private static void updateCachedParams() {
            double prop = WakesConfig.GENERAL.wavePropagationFactor.get();
            double decay = WakesConfig.GENERAL.waveDecayFactor.get();
            if (prop != lastPropagation || decay != lastDecay) {
                lastPropagation = prop;
                lastDecay = decay;
                float time = 20f;
                cachedAlpha = (float) Math.pow(prop * 16f / time, 2);
                cachedBeta = (float) (Math.log(10 * decay + 10) / Math.log(20));
            }
        }

        @Override
        public void tick(@Nullable Float velocity, @Nullable SimulationNode NORTH, @Nullable SimulationNode SOUTH, @Nullable SimulationNode EAST, @Nullable SimulationNode WEST) {
            advance();
            solve(velocity, NORTH, SOUTH, EAST, WEST);
        }

        @Override
        public void advance() {
            float[][] u0 = u[0];
            float[][] u1 = u[1];
            float[][] u2 = u[2];

            for (int z = 1; z < res+1; z++) {
                for (int x = 1; x < res+1; x++) {
                    u0[z][x] += this.initialValues[z][x];
                    this.initialValues[z][x] = 0;

                    u2[z][x] = u1[z][x];
                    u1[z][x] = u0[z][x];
                }
            }
        }

        @Override
        public void solve(@Nullable Float velocity, @Nullable SimulationNode NORTH, @Nullable SimulationNode SOUTH, @Nullable SimulationNode EAST, @Nullable SimulationNode WEST) {
            updateCachedParams();
            float alpha = cachedAlpha;
            float beta = cachedBeta;

            for (int i = 2; i >= 1; i--) {
                float[][] layer = this.u[i];
                if (NORTH != null) System.arraycopy(NORTH.u[i][res], 0, layer[0], 0, res+2);
                else Arrays.fill(layer[0], 0f);
                if (SOUTH != null) System.arraycopy(SOUTH.u[i][1], 0, layer[res+1], 0, res+2);
                else Arrays.fill(layer[res+1], 0f);
                for (int z = 0; z < res+2; z++) {
                    layer[z][res+1] = EAST != null ? EAST.u[i][z][1] : 0f;
                    layer[z][0] = WEST != null ? WEST.u[i][z][res] : 0f;
                }
            }

            float[][] u0 = u[0];
            float[][] u1 = u[1];
            float[][] u2 = u[2];

            for (int z = 1; z < res+1; z++) {
                float[] rowAbove = u1[z - 1];
                float[] row = u1[z];
                float[] rowBelow = u1[z + 1];

                for (int x = 1; x < res+1; x++) {
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
        private static double[][] angles;
        private static double[][] dists;
        private static int tableRes = -1;

        private static void updateTables(int res) {
            if (tableRes == res) return;
            tableRes = res;
            angles = new double[res + 2][res + 2];
            dists = new double[res + 2][res + 2];
            for (int z = 1; z < res + 1; z++) {
                for (int x = 1; x < res + 1; x++) {
                    angles[z][x] = 10 * Math.atan((z - 16f) / x);
                    dists[z][x] = Math.sqrt(Math.pow(z - 16, 2) + Math.pow(x, 2));
                }
            }
        }

        @Override
        public void tick(@Nullable Float velocity, @Nullable SimulationNode NORTH, @Nullable SimulationNode SOUTH, @Nullable SimulationNode EAST, @Nullable SimulationNode WEST) {
            double t = System.currentTimeMillis() / (double) 1000;
            if (velocity == null) return;
            updateTables(res);
            int p = (int) (14 * Math.min(1f, 2 * velocity / WakesConfig.APPEARANCE.maxSplashPlaneVelocity.get()));
            double phase = 2 * Math.PI * t;
            for (int z = 1; z < res+1; z++) {
                double[] angleRow = angles[z];
                double[] distRow = dists[z];
                float[] uRow = this.u[0][z];
                for (int x = 1; x < res+1; x++) {
                    double d = distRow[x] + 0.5 * Math.sin(angleRow[x] - phase);
                    if (d < p) {
                        double dp = d - p;
                        uRow[x] = (float) (200 * dp * dp / (d*d));
                    } else {
                        uRow[x] = 0;
                    }
                }
            }
        }
    }
}
