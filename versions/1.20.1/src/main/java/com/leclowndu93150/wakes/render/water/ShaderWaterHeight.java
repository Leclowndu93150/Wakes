package com.leclowndu93150.wakes.render.water;

import com.leclowndu93150.wakes.WakesClient;
import com.leclowndu93150.wakes.compat.iris.IrisAccess;
import com.leclowndu93150.wakes.config.WakesConfig;

import java.util.Locale;
import java.util.Objects;

public final class ShaderWaterHeight {

    private static final float DEPTH_MARGIN = 0.001f;

    private static String packName;
    private static int optionsRevision = -1;
    private static float detectedRise;
    private static boolean recognized;

    private ShaderWaterHeight() {}

    public static float offset() {
        if (!WakesClient.areShadersEnabled) return 0f;

        String name = IrisAccess.packName();
        int revision = IrisAccess.optionsRevision();
        if (!Objects.equals(name, packName) || revision != optionsRevision) {
            packName = name;
            optionsRevision = revision;
            Float rise = maxRise(name.toLowerCase());
            recognized = rise != null;
            detectedRise = rise == null ? 0f : Math.max(rise, 0f);
            WakesClient.LOGGER.info("Shader water: pack '{}' peak rise {} -> offset {}",
                    name, recognized ? String.format(Locale.ROOT, "%+.4f", rise) : "unknown",
                    String.format(Locale.ROOT, "%.4f", offsetFor(detectedRise)));
        }

        return offsetFor(detectedRise);
    }

    public static boolean isRecognized() {
        return recognized;
    }

    private static float offsetFor(float rise) {
        return rise + DEPTH_MARGIN + WakesConfig.APPEARANCE.shaderWaterHeightOffset.get().floatValue();
    }

    private static Float maxRise(String name) {
        if (name.contains("complementary") || name.contains("reimagined") || name.contains("unbound")
                || name.contains("spooklementary") || name.contains("voxlementary") || name.contains("rethinking")) {
            return IrisAccess.flag("WAVING_WATER_VERTEX", true) ? -0.025f : 0f;
        }
        if (name.contains("photon") || name.contains("hysteria")) {
            return IrisAccess.flag("WATER_DISPLACEMENT", true) ? 0.025f : 0f;
        }
        if (name.contains("sildur")) {
            return IrisAccess.flag("Waving_Water", true) ? 0.1f * IrisAccess.number("waves_amplitude", 0.65f) : 0f;
        }
        if (name.contains("kappa")) {
            return IrisAccess.flag("waterVertexWavesEnabled", true) ? 0.15f : 0f;
        }
        if (name.contains("astralex")) {
            return IrisAccess.flag("WAVING_WATER", true) ? 0.025f : 0f;
        }
        if (name.contains("insanity") || name.contains("pastel")) {
            return IrisAccess.flag("WAVING_LIQUID", true) ? 0.025f : 0f;
        }
        if (name.contains("bsl")) {
            return IrisAccess.flag("WAVING_WATER", true) ? 0.025f * IrisAccess.number("ANIMATION_STRENGTH", 1f) : 0f;
        }

        if (IrisAccess.hasOption("WAVING_WATER_VERTEX")) {
            return IrisAccess.flag("WAVING_WATER_VERTEX", true) ? -0.025f : 0f;
        }
        if (IrisAccess.hasOption("WATER_WAVE_FREQUENCY")) {
            return IrisAccess.flag("WATER_DISPLACEMENT", true) ? 0.025f : 0f;
        }
        if (IrisAccess.hasOption("waves_amplitude")) {
            return IrisAccess.flag("Waving_Water", true) ? 0.1f * IrisAccess.number("waves_amplitude", 0.65f) : 0f;
        }
        if (IrisAccess.hasOption("waterVertexWavesEnabled")) {
            return IrisAccess.flag("waterVertexWavesEnabled", true) ? 0.15f : 0f;
        }
        if (IrisAccess.hasOption("WAVING_LIQUID")) {
            return IrisAccess.flag("WAVING_LIQUID", true) ? 0.025f : 0f;
        }
        if (IrisAccess.hasOption("ANIMATION_STRENGTH") && IrisAccess.hasOption("WAVING_WATER")) {
            return IrisAccess.flag("WAVING_WATER", true) ? 0.025f * IrisAccess.number("ANIMATION_STRENGTH", 1f) : 0f;
        }

        return null;
    }
}
