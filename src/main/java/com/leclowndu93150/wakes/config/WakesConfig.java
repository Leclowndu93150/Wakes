package com.leclowndu93150.wakes.config;

import com.leclowndu93150.wakes.config.enums.EffectSpawningRule;
import com.leclowndu93150.wakes.config.enums.Resolution;
import com.leclowndu93150.wakes.render.enums.RenderType;
import com.leclowndu93150.wakes.render.WakeColor;
import com.google.common.collect.Lists;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.entity.EntityType;

public class WakesConfig {
    public static final ForgeConfigSpec CLIENT_SPEC;

    public static final General GENERAL;
    public static final Appearance APPEARANCE;
    public static final Debug DEBUG;

    private static Set<Fluid> fluidCache = null;
    private static Set<EntityType<?>> mobBlacklistCache = null;

    static {
        ForgeConfigSpec.Builder clientBuilder = new ForgeConfigSpec.Builder();

        GENERAL = new General(clientBuilder);
        APPEARANCE = new Appearance(clientBuilder);
        DEBUG = new Debug(clientBuilder);

        CLIENT_SPEC = clientBuilder.build();
    }

    public static class General {
        public final ForgeConfigSpec.BooleanValue disableMod;
        public final ForgeConfigSpec.BooleanValue pickBoat;

        public final ForgeConfigSpec.EnumValue<EffectSpawningRule> boatSpawning;
        public final ForgeConfigSpec.EnumValue<EffectSpawningRule> playerSpawning;
        public final ForgeConfigSpec.EnumValue<EffectSpawningRule> otherPlayersSpawning;
        public final ForgeConfigSpec.EnumValue<EffectSpawningRule> mobSpawning;
        public final ForgeConfigSpec.EnumValue<EffectSpawningRule> itemSpawning;

        public final ForgeConfigSpec.ConfigValue<List<? extends String>> blacklistedMobs;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> fluidWhitelist;

        public final ForgeConfigSpec.DoubleValue wavePropagationFactor;
        public final ForgeConfigSpec.DoubleValue waveDecayFactor;
        public final ForgeConfigSpec.IntValue initialStrength;
        public final ForgeConfigSpec.IntValue paddleStrength;
        public final ForgeConfigSpec.IntValue splashStrength;

        public General(ForgeConfigSpec.Builder builder) {
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

            blacklistedMobs = builder
                    .comment("List of entity type IDs that should not produce wakes (e.g., \"minecraft:dolphin\", \"minecraft:squid\")")
                    .defineList("blacklistedMobs", Lists.newArrayList(), obj -> obj instanceof String);

            List<String> defaultFluids = Lists.newArrayList(
                    "minecraft:water",
                    "minecraft:flowing_water",
                    "tfc:salt_water",
                    "tfc:spring_water",
                    "tfc:river_water"
            );

            fluidWhitelist = builder
                    .comment("List of fluid IDs that can produce wakes")
                    .defineList("fluidWhitelist", defaultFluids, obj -> obj instanceof String);

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
        }
    }

    public static class Appearance {
        public final ForgeConfigSpec.EnumValue<Resolution> wakeResolution;
        public final ForgeConfigSpec.DoubleValue wakeOpacity;
        public final ForgeConfigSpec.DoubleValue blendStrength;
        public final ForgeConfigSpec.BooleanValue firstPersonSplashPlane;
        public final ForgeConfigSpec.BooleanValue spawnParticles;
        public final ForgeConfigSpec.DoubleValue shaderLightPassthrough;

        public final ForgeConfigSpec.DoubleValue splashPlaneWidth;
        public final ForgeConfigSpec.DoubleValue splashPlaneHeight;
        public final ForgeConfigSpec.DoubleValue splashPlaneDepth;
        public final ForgeConfigSpec.DoubleValue splashPlaneOffset;
        public final ForgeConfigSpec.DoubleValue splashPlaneGap;
        public final ForgeConfigSpec.IntValue splashPlaneResolution;
        public final ForgeConfigSpec.DoubleValue maxSplashPlaneVelocity;
        public final ForgeConfigSpec.DoubleValue splashPlaneScale;

        public final ForgeConfigSpec.ConfigValue<List<? extends Double>> wakeColorIntervals;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> wakeColors;

        public Appearance(ForgeConfigSpec.Builder builder) {
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
        public final ForgeConfigSpec.BooleanValue debugColors;
        public final ForgeConfigSpec.BooleanValue drawDebugBoxes;
        public final ForgeConfigSpec.BooleanValue showDebugInfo;
        public final ForgeConfigSpec.IntValue floodFillDistance;
        public final ForgeConfigSpec.IntValue floodFillTickDelay;
        public final ForgeConfigSpec.EnumValue<RenderType> renderType;

        public Debug(ForgeConfigSpec.Builder builder) {
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
                    .map(ResourceLocation::new)
                    .map(BuiltInRegistries.FLUID::get)
                    .collect(Collectors.toSet());
        }
        return fluidCache;
    }

    public static Set<EntityType<?>> getMobBlacklist() {
        if (mobBlacklistCache == null) {
            mobBlacklistCache = GENERAL.blacklistedMobs.get().stream()
                    .map(ResourceLocation::new)
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