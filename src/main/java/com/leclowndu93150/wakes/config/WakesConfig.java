package com.leclowndu93150.wakes.config;

import com.leclowndu93150.wakes.config.enums.EffectSpawningRule;
import com.leclowndu93150.wakes.config.enums.Resolution;
import com.leclowndu93150.wakes.render.WakeColor;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.File;

public class WakesConfig {

    private static Configuration config;

    public static boolean disableMod = false;
    public static boolean pickBoat = true;

    public static EffectSpawningRule boatSpawning = EffectSpawningRule.SIMULATION_AND_PLANES;
    public static EffectSpawningRule playerSpawning = EffectSpawningRule.ONLY_SIMULATION;
    public static EffectSpawningRule otherPlayersSpawning = EffectSpawningRule.ONLY_SIMULATION;
    public static EffectSpawningRule mobSpawning = EffectSpawningRule.ONLY_SIMULATION;
    public static EffectSpawningRule itemSpawning = EffectSpawningRule.ONLY_SIMULATION;

    public static double wavePropagationFactor = 0.95;
    public static double waveDecayFactor = 0.5;
    public static int initialStrength = 20;
    public static int paddleStrength = 100;
    public static int splashStrength = 100;

    public static Resolution wakeResolution = Resolution.SIXTEEN;
    public static double wakeOpacity = 1.0;
    public static double blendStrength = 0.5;
    public static boolean firstPersonSplashPlane = false;
    public static boolean spawnParticles = true;
    public static double shaderLightPassthrough = 0.5;

    public static double splashPlaneWidth = 2.0;
    public static double splashPlaneHeight = 1.5;
    public static double splashPlaneDepth = 3.0;
    public static double splashPlaneOffset = 0.0;
    public static double splashPlaneGap = 1.0;
    public static int splashPlaneResolution = 5;
    public static double maxSplashPlaneVelocity = 0.5;
    public static double splashPlaneScale = 0.8;

    public static double[] wakeColorIntervals = {0.05, 0.15, 0.2, 0.35, 0.52, 0.6, 0.7, 0.9};
    public static String[] wakeColors = {
            "#00000000", "#289399a6", "#649ea5b0", "#b4c4cad1",
            "#00000000", "#b4c4cad1", "#ffffffff", "#b4c4cad1", "#649ea5b0"
    };

    public static boolean debugColors = false;
    public static boolean drawDebugBoxes = false;
    public static boolean showDebugInfo = false;
    public static int floodFillDistance = 2;
    public static int floodFillTickDelay = 2;

    public static void init(File configFile) {
        config = new Configuration(configFile);
        syncConfig();
    }

    public static void syncConfig() {
        String cat;

        cat = "general";
        disableMod = config.getBoolean("disableMod", cat, false, "Disable the mod functionality");
        pickBoat = config.getBoolean("pickBoat", cat, true, "");

        boatSpawning = getEnum(config, "boatSpawning", cat, EffectSpawningRule.SIMULATION_AND_PLANES);
        playerSpawning = getEnum(config, "playerSpawning", cat, EffectSpawningRule.ONLY_SIMULATION);
        otherPlayersSpawning = getEnum(config, "otherPlayersSpawning", cat, EffectSpawningRule.ONLY_SIMULATION);
        mobSpawning = getEnum(config, "mobSpawning", cat, EffectSpawningRule.ONLY_SIMULATION);
        itemSpawning = getEnum(config, "itemSpawning", cat, EffectSpawningRule.ONLY_SIMULATION);

        wavePropagationFactor = config.getFloat("wavePropagationFactor", cat, 0.95f, 0.0f, 1.0f, "");
        waveDecayFactor = config.getFloat("waveDecayFactor", cat, 0.5f, 0.0f, 1.0f, "");
        initialStrength = config.getInt("initialStrength", cat, 20, 0, Integer.MAX_VALUE, "");
        paddleStrength = config.getInt("paddleStrength", cat, 100, 0, Integer.MAX_VALUE, "");
        splashStrength = config.getInt("splashStrength", cat, 100, 0, Integer.MAX_VALUE, "");

        cat = "appearance";
        wakeResolution = getEnum(config, "wakeResolution", cat, Resolution.SIXTEEN);
        wakeOpacity = config.getFloat("wakeOpacity", cat, 1.0f, 0.0f, 1.0f, "");
        blendStrength = config.getFloat("blendStrength", cat, 0.5f, 0.0f, 1.0f, "");
        firstPersonSplashPlane = config.getBoolean("firstPersonSplashPlane", cat, false, "");
        spawnParticles = config.getBoolean("spawnParticles", cat, true, "");
        shaderLightPassthrough = config.getFloat("shaderLightPassthrough", cat, 0.5f, 0.0f, 1.0f, "");

        splashPlaneWidth = config.getFloat("splashPlaneWidth", cat, 2.0f, -5.0f, 5.0f, "");
        splashPlaneHeight = config.getFloat("splashPlaneHeight", cat, 1.5f, -5.0f, 5.0f, "");
        splashPlaneDepth = config.getFloat("splashPlaneDepth", cat, 3.0f, -5.0f, 5.0f, "");
        splashPlaneOffset = config.getFloat("splashPlaneOffset", cat, 0.0f, -5.0f, 5.0f, "");
        splashPlaneGap = config.getFloat("splashPlaneGap", cat, 1.0f, -5.0f, 5.0f, "");
        splashPlaneResolution = config.getInt("splashPlaneResolution", cat, 5, 0, 10, "");
        maxSplashPlaneVelocity = config.getFloat("maxSplashPlaneVelocity", cat, 0.5f, -5.0f, 5.0f, "");
        splashPlaneScale = config.getFloat("splashPlaneScale", cat, 0.8f, -5.0f, 5.0f, "");

        wakeColorIntervals = getDoubleArray(config, "wakeColorIntervals", cat,
                new double[]{0.05, 0.15, 0.2, 0.35, 0.52, 0.6, 0.7, 0.9});
        wakeColors = config.getStringList("wakeColors", cat,
                new String[]{"#00000000", "#289399a6", "#649ea5b0", "#b4c4cad1",
                        "#00000000", "#b4c4cad1", "#ffffffff", "#b4c4cad1", "#649ea5b0"},
                "Wake color gradient in ARGB hex");

        cat = "debug";
        debugColors = config.getBoolean("debugColors", cat, false, "");
        drawDebugBoxes = config.getBoolean("drawDebugBoxes", cat, false, "");
        showDebugInfo = config.getBoolean("showDebugInfo", cat, false, "");
        floodFillDistance = config.getInt("floodFillDistance", cat, 2, 1, 6, "");
        floodFillTickDelay = config.getInt("floodFillTickDelay", cat, 2, 1, 20, "");

        if (config.hasChanged()) {
            config.save();
        }
    }

    private static <T extends Enum<T>> T getEnum(Configuration cfg, String name, String category, T defaultValue) {
        String val = cfg.getString(name, category, defaultValue.name(), "Valid values: " + java.util.Arrays.toString(defaultValue.getDeclaringClass().getEnumConstants()));
        try {
            return Enum.valueOf(defaultValue.getDeclaringClass(), val);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    private static double[] getDoubleArray(Configuration cfg, String name, String category, double[] defaultValue) {
        String[] defaults = new String[defaultValue.length];
        for (int i = 0; i < defaultValue.length; i++) {
            defaults[i] = String.valueOf(defaultValue[i]);
        }
        String[] vals = cfg.getStringList(name, category, defaults, "");
        double[] result = new double[vals.length];
        for (int i = 0; i < vals.length; i++) {
            try {
                result[i] = Double.parseDouble(vals[i]);
            } catch (NumberFormatException e) {
                result[i] = i < defaultValue.length ? defaultValue[i] : 0;
            }
        }
        return result;
    }

    private static WakeColor[] wakeColorCache = null;
    private static String[] wakeColorCacheKeys = null;

    public static WakeColor getWakeColor(int i) {
        if (i < 0 || i >= wakeColors.length) {
            return new WakeColor(0);
        }
        if (wakeColorCache == null || wakeColorCache.length != wakeColors.length || !java.util.Arrays.equals(wakeColorCacheKeys, wakeColors)) {
            wakeColorCacheKeys = wakeColors.clone();
            wakeColorCache = new WakeColor[wakeColors.length];
            for (int j = 0; j < wakeColors.length; j++) {
                wakeColorCache[j] = new WakeColor(wakeColors[j]);
            }
        }
        return wakeColorCache[i];
    }
}
