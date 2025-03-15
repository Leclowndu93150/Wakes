package com.leclowndu93150.wakes.simulation;

import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.config.enums.Resolution;
import com.leclowndu93150.wakes.particle.custom.SplashPlaneParticle;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Queue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.Level;

public class WakeHandler {
    private static WakeHandler INSTANCE;
    public Level world;

    private QuadTree[] trees;
    private QueueSet<WakeNode>[] toBeInserted;
    private final int minY;
    private final int maxY;
    private ArrayList<SplashPlaneParticle> splashPlanes;

    public static Resolution resolution = WakesConfig.wakeResolution;

    public static boolean resolutionResetScheduled = false;

    private WakeHandler(Level world) {
        this.world = world;
        this.minY = world.getMinBuildHeight();
        this.maxY = world.getMaxBuildHeight();
        int worldHeight = this.maxY - this.minY;
        this.trees = new QuadTree[worldHeight];
        this.toBeInserted = new QueueSet[worldHeight];
        for (int i = 0; i < worldHeight; i++) {
            toBeInserted[i] = new QueueSet<>();
        }
        this.splashPlanes = new ArrayList<>();
    }

    public static Optional<WakeHandler> getInstance() {
        if (INSTANCE == null) {
            if (Minecraft.getInstance().level == null) {
                return Optional.empty();
            }
            INSTANCE = new WakeHandler(Minecraft.getInstance().level);
        }
        return Optional.of(INSTANCE);
    }

    public static void init(Level world) {
        INSTANCE = new WakeHandler(world);
    }

    public static void kill() {
        INSTANCE = null;
    }

    public void tick() {
        if (WakesConfig.wakeResolution.res != WakeHandler.resolution.res) {
            scheduleResolutionChange(WakesConfig.wakeResolution);
        }
        for (int i = 0; i < this.maxY - this.minY; i++) {
            Queue<WakeNode> pendingNodes = this.toBeInserted[i];
            if (resolutionResetScheduled) {
                if (pendingNodes != null) pendingNodes.clear();
                continue;
            }
            QuadTree tree = this.trees[i];
            if (tree != null) {
                tree.tick(this);
                while (pendingNodes.peek() != null) {
                    tree.insert(pendingNodes.poll());
                }
            }
        }
        for (int i = this.splashPlanes.size() - 1; i >= 0; i--) {
            if (!this.splashPlanes.get(i).isAlive()) {
                this.splashPlanes.remove(i);
            }
        }
        if (resolutionResetScheduled) {
            this.changeResolution();
        }
    }

    public void recolorWakes() {
        for (int i = 0; i < this.maxY - this.minY; i++) {
            QuadTree tree = this.trees[i];
            if (tree != null) {
                tree.recolorWakes();
            }
        }
        for (var splashPlane : this.splashPlanes) {
            if (splashPlane != null) {
                splashPlane.populatePixels();
            }
        }
    }

    public void registerSplashPlane(SplashPlaneParticle splashPlane) {
        this.splashPlanes.add(splashPlane);
    }

    public void insert(WakeNode node) {
        if (resolutionResetScheduled) return;
        int i = this.getArrayIndex(node.y);
        if (i < 0) return;

        if (this.trees[i] == null) {
            this.trees[i] = new QuadTree(node.y);
        }

        if (node.validPos(world)) {
            this.toBeInserted[i].add(node);
        }
    }

    public <T> ArrayList<T> getVisible(Frustum frustum, Class<T> type) {
        ArrayList<T> visibleObjects = new ArrayList<>();
        if (type.equals(SplashPlaneParticle.class)) {
            for (SplashPlaneParticle particle : splashPlanes) {
                if (frustum.isVisible(particle.getBoundingBox())) {
                    visibleObjects.add(type.cast(particle));
                }
            }
        } else {
            for (int i = 0; i < this.maxY - this.minY; i++) {
                if (this.trees[i] != null) {
                    this.trees[i].query(frustum, visibleObjects, type);
                }
            }
        }
        return visibleObjects;
    }

    private int getArrayIndex(int y) {
        if (y < this.minY || y >= this.maxY) {
            return -1;
        }
        return y - this.minY;
    }

    public static void scheduleResolutionChange(Resolution newRes) {
        resolutionResetScheduled = true;
    }

    private void changeResolution() {
        this.reset();
        WakeHandler.resolution = WakesConfig.wakeResolution;
        resolutionResetScheduled = false;
    }

    private void reset() {
        for (int i = 0; i < this.maxY - this.minY; i++) {
            QuadTree tree = this.trees[i];
            if (tree != null) {
                tree.prune();
            }
            toBeInserted[i].clear();
        }
    }
}
