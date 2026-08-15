package com.leclowndu93150.wakes.platform.neoforge;

import com.leclowndu93150.wakes.WakesClient;
import com.leclowndu93150.wakes.platform.LevelRenderHook;
import com.leclowndu93150.wakes.platform.WakesPlatform;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterDebugEntriesEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class WakesPlatformNeoForge implements WakesPlatform {
    private static IEventBus modEventBus;
    private static ModContainer modContainer;
    private static DeferredRegister<ParticleType<?>> particleTypes;
    private static final List<RenderPipeline> pendingPipelines = new ArrayList<>();
    private static final List<Consumer<RegisterParticleProvidersEvent>> pendingParticleFactories = new ArrayList<>();
    private static final List<DebugEntry> pendingDebugEntries = new ArrayList<>();

    private record DebugEntry(Identifier id, DebugScreenEntry entry) {}

    public static void bootstrap(IEventBus bus, ModContainer container) {
        modEventBus = bus;
        modContainer = container;
        particleTypes = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, WakesClient.MOD_ID);
        particleTypes.register(modEventBus);

        modEventBus.addListener((RegisterRenderPipelinesEvent event) -> {
            for (RenderPipeline pipeline : pendingPipelines) event.registerPipeline(pipeline);
        });
        modEventBus.addListener((RegisterParticleProvidersEvent event) -> {
            for (Consumer<RegisterParticleProvidersEvent> c : pendingParticleFactories) c.accept(event);
        });
        modEventBus.addListener((RegisterDebugEntriesEvent event) -> {
            for (DebugEntry e : pendingDebugEntries) event.register(e.id(), e.entry());
        });
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public void registerConfig(ModConfig.Type type, ModConfigSpec spec, String filename) {
        modContainer.registerConfig(type, spec, filename);
    }

    @Override
    public void registerPipeline(RenderPipeline pipeline) {
        pendingPipelines.add(pipeline);
    }

    @Override
    public void registerClientLifecycle(Runnable setup) {
        modEventBus.addListener((FMLClientSetupEvent e) -> setup.run());
    }

    @Override
    public void registerClientTickPre(Runnable r) {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Pre e) -> r.run());
    }

    @Override
    public void registerLevelTickPost(Consumer<ClientLevel> r) {
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post e) -> {
            if (e.getLevel() instanceof ClientLevel cl) r.accept(cl);
        });
    }

    @Override
    public void registerLevelUnload(Consumer<ClientLevel> r) {
        NeoForge.EVENT_BUS.addListener((LevelEvent.Unload e) -> {
            if (e.getLevel() instanceof ClientLevel cl) r.accept(cl);
        });
    }

    @Override
    public void registerChunkUnload(BiConsumer<ClientLevel, ChunkPos> r) {
        NeoForge.EVENT_BUS.addListener((ChunkEvent.Unload e) -> {
            if (e.getLevel() instanceof ClientLevel cl) r.accept(cl, e.getChunk().getPos());
        });
    }

    @Override
    public void registerPlayerChangedDimension(BiConsumer<Entity, ResourceKey<Level>> r) {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> r.accept(e.getEntity(), e.getFrom()));
    }

    @Override
    public void registerPlayerLoggedIn(Consumer<Entity> r) {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> r.accept(e.getEntity()));
    }

    @Override
    public void registerLevelRenderAfterTranslucent(LevelRenderHook hook) {
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent.AfterTranslucentBlocks e) ->
                hook.run(e.getPoseStack(),
                        e.getLevelRenderState().cameraRenderState.cullFrustum,
                        e.getLevelRenderState().cameraRenderState.pos));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T extends ParticleType<?>> Supplier<T> registerParticleType(String name, Supplier<T> factory) {
        var holder = particleTypes.register(name, (Supplier) factory);
        return () -> (T) holder.get();
    }

    @Override
    public <T extends ParticleOptions> void registerParticleFactory(Supplier<? extends ParticleType<T>> type, Function<SpriteSet, ParticleProvider<T>> provider) {
        pendingParticleFactories.add(event -> event.registerSpriteSet(type.get(), provider::apply));
    }

    @Override
    public void registerDebugScreenEntry(Identifier id, DebugScreenEntry entry) {
        pendingDebugEntries.add(new DebugEntry(id, entry));
    }
}
