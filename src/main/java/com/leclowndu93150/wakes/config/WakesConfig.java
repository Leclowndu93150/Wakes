package com.leclowndu93150.wakes.config;

import com.leclowndu93150.wakes.config.enums.EffectSpawningRule;
import com.leclowndu93150.wakes.config.enums.Resolution;
import com.leclowndu93150.wakes.render.enums.RenderType;
import com.leclowndu93150.wakes.render.WakeColor;
import com.google.common.collect.Lists;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.entity.EntityType;

public class WakesConfig {
    public static final ModConfigSpec CLIENT_SPEC;

    public static final General GENERAL;
    public static final Appearance APPEARANCE;
    public static final Debug DEBUG;

    private static Set<Fluid> fluidCache = null;
    private static Set<EntityType<?>> mobBlacklistCache = null;

    static {
        ModConfigSpec.Builder clientBuilder = new ModConfigSpec.Builder();

        GENERAL = new General(clientBuilder);
        APPEARANCE = new Appearance(clientBuilder);
        DEBUG = new Debug(clientBuilder);

        CLIENT_SPEC = clientBuilder.build();
    }

    public static class General {
        public final ModConfigSpec.BooleanValue disableMod;
        public final ModConfigSpec.BooleanValue pickBoat;

        public final ModConfigSpec.EnumValue<EffectSpawningRule> boatSpawning;
        public final ModConfigSpec.EnumValue<EffectSpawningRule> playerSpawning;
        public final ModConfigSpec.EnumValue<EffectSpawningRule> otherPlayersSpawning;
        public final ModConfigSpec.EnumValue<EffectSpawningRule> mobSpawning;
        public final ModConfigSpec.EnumValue<EffectSpawningRule> itemSpawning;

        public final ModConfigSpec.DoubleValue wavePropagationFactor;
        public final ModConfigSpec.DoubleValue waveDecayFactor;
        public final ModConfigSpec.IntValue initialStrength;
        public final ModConfigSpec.IntValue paddleStrength;
        public final ModConfigSpec.IntValue splashStrength;

        public final ModConfigSpec.ConfigValue<List<? extends String>> fluidWhitelist;
        public final ModConfigSpec.ConfigValue<List<? extends String>> mobBlacklist;

        public General(ModConfigSpec.Builder builder) {
            disableMod = builder
                    .comment("Disable the mod functionality")
                    .define("disableMod", false);

            pickBoat = builder
                    .define("pickBoat", true);

            builder.comment("Spawning Rules").push("spawningRules");

            boatSpawning = builder
                    .defineEnum("boatSpawning", EffectSpawningRule.SIMULATION_AND_PLANES);

            playerSpawning = builder
                    .defineEnum("playerSpawning", EffectSpawningRule.ONLY_SIMULATION);

            otherPlayersSpawning = builder
                    .defineEnum("otherPlayersSpawning", EffectSpawningRule.ONLY_SIMULATION);

            mobSpawning = builder
                    .defineEnum("mobSpawning", EffectSpawningRule.ONLY_SIMULATION);

            itemSpawning = builder
                    .defineEnum("itemSpawning", EffectSpawningRule.ONLY_SIMULATION);

            builder.pop();

            builder.comment("Wake Behavior").push("wakeBehavior");

            wavePropagationFactor = builder
                    .defineInRange("wavePropagationFactor", 0.95d, 0.0d, 1.0d);

            waveDecayFactor = builder
                    .defineInRange("waveDecayFactor", 0.5d, 0.0d, 1.0d);

            initialStrength = builder
                    .defineInRange("initialStrength", 20, 0, Integer.MAX_VALUE);

            paddleStrength = builder
                    .defineInRange("paddleStrength", 100, 0, Integer.MAX_VALUE);

            splashStrength = builder
                    .defineInRange("splashStrength", 100, 0, Integer.MAX_VALUE);

            builder.pop();

            List<String> defaultFluids = Lists.newArrayList(
                    "minecraft:water",
                    "minecraft:flowing_water"
            );

            fluidWhitelist = builder
                    .comment("List of fluid IDs that can produce wakes")
                    .defineList("fluidWhitelist", defaultFluids, obj -> obj instanceof String);

            List<String> defaultMobBlacklist = Lists.newArrayList();

            mobBlacklist = builder
                    .comment("List of entity type IDs that should not produce wakes")
                    .defineList("mobBlacklist", defaultMobBlacklist, obj -> obj instanceof String);
        }
    }

    public static class Appearance {
        public final ModConfigSpec.EnumValue<Resolution> wakeResolution;
        public final ModConfigSpec.DoubleValue wakeOpacity;
        public final ModConfigSpec.DoubleValue blendStrength;
        public final ModConfigSpec.BooleanValue firstPersonSplashPlane;
        public final ModConfigSpec.BooleanValue spawnParticles;
        public final ModConfigSpec.DoubleValue shaderLightPassthrough;

        public final ModConfigSpec.DoubleValue splashPlaneWidth;
        public final ModConfigSpec.DoubleValue splashPlaneHeight;
        public final ModConfigSpec.DoubleValue splashPlaneDepth;
        public final ModConfigSpec.DoubleValue splashPlaneOffset;
        public final ModConfigSpec.DoubleValue splashPlaneGap;
        public final ModConfigSpec.IntValue splashPlaneResolution;
        public final ModConfigSpec.DoubleValue maxSplashPlaneVelocity;
        public final ModConfigSpec.DoubleValue splashPlaneScale;

        public final ModConfigSpec.ConfigValue<List<? extends Double>> wakeColorIntervals;
        public final ModConfigSpec.ConfigValue<List<? extends String>> wakeColors;

        public Appearance(ModConfigSpec.Builder builder) {
            wakeResolution = builder
                    .defineEnum("wakeResolution", Resolution.SIXTEEN);

            wakeOpacity = builder
                    .defineInRange("wakeOpacity", 1.0d, 0.0d, 1.0d);

            blendStrength = builder
                    .defineInRange("blendStrength", 0.5d, 0.0d, 1.0d);

            firstPersonSplashPlane = builder
                    .define("firstPersonSplashPlane", false);

            spawnParticles = builder
                    .define("spawnParticles", true);

            shaderLightPassthrough = builder
                    .defineInRange("shaderLightPassthrough", 0.5d, 0.0d, 1.0d);

            builder.comment("Splash Plane Settings").push("splashPlane");

            splashPlaneWidth = builder
                    .defineInRange("splashPlaneWidth", 2.0d, -5.0d, 5.0d);

            splashPlaneHeight = builder
                    .defineInRange("splashPlaneHeight", 1.5d, -5.0d, 5.0d);

            splashPlaneDepth = builder
                    .defineInRange("splashPlaneDepth", 3.0d, -5.0d, 5.0d);

            splashPlaneOffset = builder
                    .defineInRange("splashPlaneOffset", 0.0d, -5.0d, 5.0d);

            splashPlaneGap = builder
                    .defineInRange("splashPlaneGap", 1.0d, -5.0d, 5.0d);

            splashPlaneResolution = builder
                    .defineInRange("splashPlaneResolution", 5, 0, 10);

            maxSplashPlaneVelocity = builder
                    .defineInRange("maxSplashPlaneVelocity", 0.5d, -5.0d, 5.0d);

            splashPlaneScale = builder
                    .defineInRange("splashPlaneScale", 0.8d, -5.0d, 5.0d);

            builder.pop();

            List<Double> defaultWakeColorIntervals = Lists.newArrayList(0.05, 0.15, 0.2, 0.35, 0.52, 0.6, 0.7, 0.9);
            List<String> defaultWakeColors = Lists.newArrayList(
                    "#00000000", "#289399a6", "#649ea5b0", "#b4c4cad1",
                    "#00000000", "#b4c4cad1", "#ffffffff", "#b4c4cad1", "#649ea5b0"
            );

            wakeColorIntervals = builder
                    .defineList("wakeColorIntervals", defaultWakeColorIntervals, obj -> obj instanceof Double);

            wakeColors = builder
                    .defineList("wakeColors", defaultWakeColors, obj -> obj instanceof String);
        }
    }

    public static class Debug {
        public final ModConfigSpec.BooleanValue debugColors;
        public final ModConfigSpec.BooleanValue drawDebugBoxes;
        public final ModConfigSpec.BooleanValue showDebugInfo;
        public final ModConfigSpec.IntValue floodFillDistance;
        public final ModConfigSpec.IntValue floodFillTickDelay;
        public final ModConfigSpec.EnumValue<RenderType> renderType;

        public Debug(ModConfigSpec.Builder builder) {
            debugColors = builder
                    .define("debugColors", false);

            drawDebugBoxes = builder
                    .define("drawDebugBoxes", false);

            showDebugInfo = builder
                    .define("showDebugInfo", false);

            floodFillDistance = builder
                    .defineInRange("floodFillDistance", 2, 1, 6);

            floodFillTickDelay = builder
                    .defineInRange("floodFillTickDelay", 2, 1, 20);

            renderType = builder
                    .defineEnum("renderType", RenderType.AUTO);
        }
    }

    public static WakeColor getWakeColor(int i) {
        return new WakeColor(APPEARANCE.wakeColors.get().get(i));
    }

    public static Set<Fluid> getFluidWhitelist() {
        if (fluidCache == null) {
            fluidCache = GENERAL.fluidWhitelist.get().stream()
                    .map(ResourceLocation::parse)
                    .map(BuiltInRegistries.FLUID::get)
                    .collect(Collectors.toSet());
        }
        return fluidCache;
    }

    public static Set<EntityType<?>> getMobBlacklist() {
        if (mobBlacklistCache == null) {
            mobBlacklistCache = GENERAL.mobBlacklist.get().stream()
                    .map(ResourceLocation::parse)
                    .map(BuiltInRegistries.ENTITY_TYPE::get)
                    .collect(Collectors.toSet());
        }
        return mobBlacklistCache;
    }

    public static void clearCache() {
        fluidCache = null;
        mobBlacklistCache = null;
    }
}