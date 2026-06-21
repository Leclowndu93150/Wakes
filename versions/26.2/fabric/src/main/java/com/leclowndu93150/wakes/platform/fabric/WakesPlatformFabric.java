package com.leclowndu93150.wakes.platform.fabric;

import com.leclowndu93150.wakes.WakesClient;
import com.leclowndu93150.wakes.platform.LevelRenderHook;
import com.leclowndu93150.wakes.platform.WakesPlatform;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import fuzs.forgeconfigapiport.fabric.api.v5.client.ConfigScreenFactoryRegistry;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import com.leclowndu93150.wakes.mixin.DebugScreenEntriesInvoker;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class WakesPlatformFabric implements WakesPlatform {
    private ResourceKey<Level> lastDimension;

    private boolean screenRegistered = false;

    @Override
    public void registerConfig(ModConfig.Type type, ModConfigSpec spec, String filename) {
        ConfigRegistry.INSTANCE.register(WakesClient.MOD_ID, type, spec, filename);
        if (!screenRegistered) {
            screenRegistered = true;
            ConfigScreenFactoryRegistry.INSTANCE.register(WakesClient.MOD_ID, ConfigurationScreen::new);
        }
    }

    @Override
    public void registerPipeline(RenderPipeline pipeline) {
    }

    @Override
    public void registerClientLifecycle(Runnable setup) {
        ClientLifecycleEvents.CLIENT_STARTED.register(c -> setup.run());
    }

    @Override
    public void registerClientTickPre(Runnable r) {
        ClientTickEvents.START_CLIENT_TICK.register(c -> r.run());
    }

    @Override
    public void registerLevelTickPost(Consumer<ClientLevel> r) {
        ClientTickEvents.END_LEVEL_TICK.register(r::accept);
    }

    @Override
    public void registerLevelUnload(Consumer<ClientLevel> r) {
        ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> {
            if (client.level != null) r.accept(client.level);
        });
    }

    @Override
    public void registerChunkUnload(BiConsumer<ClientLevel, ChunkPos> r) {
        ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> r.accept(level, chunk.getPos()));
    }

    @Override
    public void registerPlayerChangedDimension(BiConsumer<Entity, ResourceKey<Level>> r) {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Player player = client.player;
            if (player == null) {
                lastDimension = null;
                return;
            }
            ResourceKey<Level> current = player.level().dimension();
            if (lastDimension != null && !lastDimension.equals(current)) {
                r.accept(player, lastDimension);
            }
            lastDimension = current;
        });
    }

    @Override
    public void registerPlayerLoggedIn(Consumer<Entity> r) {
        ClientPlayConnectionEvents.JOIN.register((listener, sender, client) -> {
            if (client.player != null) r.accept(client.player);
        });
    }

    @Override
    public void registerLevelRenderAfterTranslucent(LevelRenderHook hook) {
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(context -> hook.run(
                context.submitNodeCollector(),
                context.poseStack(),
                context.levelState().cameraRenderState.cullFrustum,
                context.levelState().cameraRenderState.pos));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T extends ParticleType<?>> Supplier<T> registerParticleType(String name, Supplier<T> factory) {
        T particleType = factory.get();
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath(WakesClient.MOD_ID, name), (ParticleType) particleType);
        return () -> particleType;
    }

    @Override
    public <T extends ParticleOptions> void registerParticleFactory(Supplier<? extends ParticleType<T>> type, Function<SpriteSet, ParticleProvider<T>> provider) {
        ParticleProviderRegistry.getInstance().register(type.get(), provider::apply);
    }

    @Override
    public void registerDebugScreenEntry(Identifier id, DebugScreenEntry entry) {
        DebugScreenEntriesInvoker.wakes$register(id, entry);
    }
}
