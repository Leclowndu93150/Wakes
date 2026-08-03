package com.leclowndu93150.wakes.simulation;

import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.debug.WakesDebugInfo;
import com.leclowndu93150.wakes.render.WakeColor;
import com.leclowndu93150.wakes.render.WakeTexture;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Brick {
    private final WakeNode[][] nodes;
    public final int capacity;
    public final int dim;

    public int occupied = 0;

    public final Vec3 pos;

    public Brick NORTH;
    public Brick EAST;
    public Brick SOUTH;
    public Brick WEST;

    public long imgPtr = -1;
    public int texRes;
    public WakeTexture wakeTexture = null;
    public boolean pixelsStale = false;
    public boolean pixelsDirty = false;
    private boolean refreshColors = false;
    private int[] palette = null;

    private int unusedTicks = 0;

    public Brick(int x, float y, int z, int width) {
        this.dim = width;
        this.capacity = dim * dim;
        this.nodes = new WakeNode[dim][dim];
        this.pos = new Vec3(x, y, z);

        initTexture(WakeHandler.resolution.res);
    }

    public void initTexture(int res) {
        long size = 4L * dim * dim * res * res;
        if (imgPtr == -1) {
            this.imgPtr = MemoryUtil.nmemAlloc(size);
        } else {
            this.imgPtr = MemoryUtil.nmemRealloc(imgPtr, size);
        }
        MemoryUtil.memSet(imgPtr, 0, size);
        this.texRes = res;
        this.pixelsStale = true;
    }

    public void deallocTexture() {
        if (imgPtr != -1) {
            MemoryUtil.nmemFree(imgPtr);
            imgPtr = -1;
        }
        if (wakeTexture != null) {
            wakeTexture.close();
            wakeTexture = null;
        }
        pixelsStale = false;
        pixelsDirty = false;
    }


    public boolean tick(WakeHandler wakeHandler) {
        if (occupied == 0) {
            unusedTicks++;
            if (unusedTicks > 100 && imgPtr != -1) { // Deallocate after 5 seconds of no use
                deallocTexture();
            }
            return false;
        }

        unusedTicks = 0;

        long tNode = System.nanoTime();
        for (int z = 0; z < dim; z++) {
            WakeNode[] row = nodes[z];
            for (int x = 0; x < dim; x++) {
                WakeNode node = row[x];
                if (node == null) continue;

                if (!node.tick(wakeHandler)) {
                    this.clear(x, z);
                }
            }
        }
        WakesDebugInfo.nodeLogicTime += (System.nanoTime() - tNode);
        WakesDebugInfo.nodeCount += occupied;
        pixelsStale = true;
        return occupied != 0;
    }

    public void query(Frustum frustum, ArrayList<WakeNode> output) {
        for (int z = 0; z < dim; z++) {
            for (int x = 0; x < dim; x++) {
                var node = this.get(x, z);
                if (node == null) continue;
                AABB b = node.toBox();
                if (frustum.isVisible(b)) output.add(node);
            }
        }
    }

    public WakeNode get(int x, int z) {
        if (x >= 0 && x < dim) {
            if (z < 0 && NORTH != null) {
                return NORTH.nodes[Math.floorMod(z, dim)][x];
            } else if (z >= dim && SOUTH != null) {
                return SOUTH.nodes[Math.floorMod(z, dim)][x];
            } else if (z >= 0 && z < dim){
                return nodes[z][x];
            }
        }
        if (z >= 0 && z < dim) {
            if (x < 0 && WEST != null) {
                return WEST.nodes[z][Math.floorMod(x, dim)];
            } else if (x >= dim && EAST != null) {
                return EAST.nodes[z][Math.floorMod(x, dim)];
            }
        }
        return null;
    }

    public void insert(WakeNode node) {
        int x = Math.floorMod(node.x, dim), z = Math.floorMod(node.z, dim);
        if (nodes[z][x] != null) {
            nodes[z][x].revive(node);
            return;
        }
        this.set(x, z, node);
        for (WakeNode neighbor : getAdjacentNodes(x, z)) {
            neighbor.updateAdjacency(node);
        }
    }

    protected void set(int x, int z, WakeNode node) {
        boolean wasNull = nodes[z][x] == null;
        nodes[z][x] = node;
        if (node == null) {
            if (!wasNull) this.occupied--;
        } else {
            if (wasNull) this.occupied++;
        }
    }

    public void clear(int x, int z) {
        this.set(x, z, null);
        if (imgPtr != -1) {
            zeroCell(x, z);
        }
    }

    private void zeroCell(int x, int z) {
        int stride = dim * texRes;
        long cellPtr = imgPtr + texRes * 4L * (((long) z * stride) + x);
        long rowBytes = 4L * texRes;
        for (int r = 0; r < texRes; r++) {
            MemoryUtil.memSet(cellPtr + 4L * ((long) r * stride), 0, rowBytes);
        }
        pixelsDirty = true;
    }

    public void markForRecolor() {
        refreshColors = true;
        pixelsStale = true;
    }

    private List<WakeNode> getAdjacentNodes(int x, int z) {
        return Stream.of(
                this.get(x, z + 1),
                this.get(x + 1, z),
                this.get(x, z - 1),
                this.get(x - 1, z)).filter(Objects::nonNull).toList();
    }

    public void updateAdjacency(Brick brick) {
        if (brick.pos.x == this.pos.x && brick.pos.z == this.pos.z - dim) {
            this.NORTH = brick;
            brick.SOUTH = this;
            return;
        }
        if (brick.pos.x == this.pos.x + dim && brick.pos.z == this.pos.z) {
            this.EAST = brick;
            brick.WEST = this;
            return;
        }
        if (brick.pos.x == this.pos.x && brick.pos.z == this.pos.z + dim) {
            this.SOUTH = brick;
            brick.NORTH = this;
            return;
        }
        if (brick.pos.x == this.pos.x - dim && brick.pos.z == this.pos.z) {
            this.WEST = brick;
            brick.EAST = this;
        }
    }

    public void populatePixels() {
        if (imgPtr == -1) {
            initTexture(WakeHandler.resolution.res);
        }

        long tTexturing = System.nanoTime();
        Level world = Minecraft.getInstance().level;
        boolean debug = WakesConfig.DEBUG.debugColors.get();
        float wakeOpacity = WakesConfig.APPEARANCE.wakeOpacity.get().floatValue();
        WakeColor.updateCaches();
        if (palette == null || palette.length != WakeColor.paletteSize()) {
            palette = new int[WakeColor.paletteSize()];
        }

        int stride = dim * texRes;
        for (int z = 0; z < dim; z++) {
            for (int x = 0; x < dim; x++) {
                WakeNode node = nodes[z][x];
                if (node == null) continue;

                if (refreshColors || !node.hasCachedFluidColor) {
                    node.cachedFluidColor = BiomeColors.getAverageWaterColor(world, node.blockPos());
                    node.hasCachedFluidColor = true;
                }
                int lightCoordinate = LevelRenderer.getLightColor(world, node.blockPos());
                int lightCol = Minecraft.getInstance().gameRenderer.lightTexture().lightPixels.getPixelRGBA(
                        LightTexture.block(lightCoordinate),
                        LightTexture.sky(lightCoordinate)
                );
                float opacity = (float) ((-Math.pow(node.t, 2) + 1) * wakeOpacity);
                long cellPtr = imgPtr + texRes * 4L * (((long) z * stride) + x);

                if (debug) {
                    for (int r = 0; r < texRes; r++) {
                        long rowPtr = cellPtr + 4L * ((long) r * stride);
                        for (int c = 0; c < texRes; c++) {
                            MemoryUtil.memPutInt(rowPtr + 4L * c, node.simulationNode.getPixelColor(c, r, node.cachedFluidColor, lightCol, opacity));
                        }
                    }
                } else {
                    WakeColor.computePalette(palette, node.cachedFluidColor, lightCol, opacity);
                    float[][][] u = node.simulationNode.u;
                    for (int r = 0; r < texRes; r++) {
                        float[] u0 = u[0][r + 1];
                        float[] u1 = u[1][r + 1];
                        float[] u2 = u[2][r + 1];
                        long rowPtr = cellPtr + 4L * ((long) r * stride);
                        for (int c = 0; c < texRes; c++) {
                            float waveEqAvg = (u0[c + 1] + u1[c + 1] + u2[c + 1]) / 3;
                            MemoryUtil.memPutInt(rowPtr + 4L * c, palette[WakeColor.paletteIndex(waveEqAvg)]);
                        }
                    }
                }
            }
        }
        refreshColors = false;
        pixelsStale = false;
        pixelsDirty = true;
        WakesDebugInfo.texturingTime += (System.nanoTime() - tTexturing);
    }
}
