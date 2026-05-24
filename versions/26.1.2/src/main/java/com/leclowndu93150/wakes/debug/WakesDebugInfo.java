package com.leclowndu93150.wakes.debug;

import com.leclowndu93150.wakes.WakesClient;
import com.leclowndu93150.wakes.config.WakesConfig;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDebugEntriesEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

@EventBusSubscriber
public class WakesDebugInfo implements DebugScreenEntry {
    public static double nodeLogicTime = 0;
    public static double texturingTime = 0;
    public static ArrayList<Long> renderingTime = new ArrayList<>();
    public static int quadsRendered = 0;
    public static int nodeCount = 0;

    public static final Identifier ID = Identifier.fromNamespaceAndPath(WakesClient.MOD_ID, "debug_info");

    public static void reset() {
        nodeCount = 0;
        nodeLogicTime = 0;
        texturingTime = 0;
        renderingTime = new ArrayList<>();
    }

    @SubscribeEvent
    public static void onRegisterDebugEntries(RegisterDebugEntriesEvent event) {
        event.register(ID, new WakesDebugInfo());
    }

    @Override
    public void display(DebugScreenDisplayer displayer, @Nullable Level level, @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk) {
        if (!WakesConfig.DEBUG.showDebugInfo.get()) return;

        if (WakesConfig.GENERAL.disableMod.get()) {
            displayer.addLine("[Wakes] Mod disabled!");
        } else {
            displayer.addLine(String.format("[Wakes] Rendering %d quads for %d wake nodes", quadsRendered, nodeCount));
            displayer.addLine(String.format("[Wakes] Node logic: %.2fms/t", 10e-6 * nodeLogicTime));
            displayer.addLine(String.format("[Wakes] Texturing: %.2fms/t", 10e-6 * texturingTime));
            long avgRendering = renderingTime.isEmpty() ? 0 : renderingTime.stream().reduce(0L, Long::sum) / renderingTime.size();
            displayer.addLine(String.format("[Wakes] Rendering: %.3fms/f", 10e-6 * avgRendering));
        }
    }
}
