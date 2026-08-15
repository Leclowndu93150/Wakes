package com.leclowndu93150.wakes.platform;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface WakesPlatform {
    boolean isModLoaded(String modId);

    void registerConfig(ModConfig.Type type, ModConfigSpec spec, String filename);

    void registerPipeline(RenderPipeline pipeline);

    void registerClientLifecycle(Runnable setup);

    void registerClientTickPre(Runnable r);

    void registerLevelTickPost(Consumer<ClientLevel> r);

    void registerLevelUnload(Consumer<ClientLevel> r);

    void registerChunkUnload(BiConsumer<ClientLevel, ChunkPos> r);

    void registerPlayerChangedDimension(BiConsumer<Entity, ResourceKey<Level>> r);

    void registerPlayerLoggedIn(Consumer<Entity> r);

    void registerLevelRenderAfterTranslucent(LevelRenderHook hook);

    <T extends ParticleType<?>> Supplier<T> registerParticleType(String name, Supplier<T> factory);

    <T extends ParticleOptions> void registerParticleFactory(Supplier<? extends ParticleType<T>> type, Function<SpriteSet, ParticleProvider<T>> provider);

    void registerDebugScreenEntry(Identifier id, DebugScreenEntry entry);
}
