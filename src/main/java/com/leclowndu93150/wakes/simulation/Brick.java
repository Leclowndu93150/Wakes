package com.leclowndu93150.wakes.simulation;

import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.debug.WakesDebugInfo;
import com.leclowndu93150.wakes.utils.WaterTintUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class Brick {
    private final WakeNode[][] nodes;
    public final int capacity;
    public final int dim;

    public int occupied = 0;

    public final Vec3d pos;

    public Brick NORTH;
    public Brick EAST;
    public Brick SOUTH;
    public Brick WEST;

    public ByteBuffer imgBuffer = null;
    public int texRes;
    public boolean hasPopulatedPixels = false;

    private boolean shouldDeallocate = false;
    private int unusedTicks = 0;

    public Brick(int x, float y, int z, int width) {
        this.dim = width;
        this.capacity = dim * dim;
        this.nodes = new WakeNode[dim][dim];
        this.pos = new Vec3d(x, y, z);

        initTexture(WakeHandler.resolution.res);
    }

    public void initTexture(int res) {
        int size = 4 * dim * dim * res * res;
        this.imgBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
        this.texRes = res;
        this.hasPopulatedPixels = false;
    }

    public void deallocTexture() {
        imgBuffer = null;
    }

    public boolean tick(WakeHandler wakeHandler) {
        if (occupied == 0) {
            unusedTicks++;
            if (unusedTicks > 100 && imgBuffer != null) {
                deallocTexture();
                hasPopulatedPixels = false;
            }
            return false;
        }

        unusedTicks = 0;

        long tNode = System.nanoTime();
        for (int z = 0; z < dim; z++) {
            for (int x = 0; x < dim; x++) {
                if (this.get(x, z) == null) continue;

                if (!this.get(x, z).tick(wakeHandler)) {
                    this.clear(x, z);
                }
            }
        }
        WakesDebugInfo.nodeLogicTime += (System.nanoTime() - tNode);
        long tTexturing = System.nanoTime();
        populatePixels();
        WakesDebugInfo.texturingTime += (System.nanoTime() - tTexturing);
        WakesDebugInfo.nodeCount += occupied;
        return occupied != 0;
    }

    public void query(Frustum frustum, ArrayList<WakeNode> output) {
        for (int z = 0; z < dim; z++) {
            for (int x = 0; x < dim; x++) {
                WakeNode node = this.get(x, z);
                if (node == null) continue;
                AxisAlignedBB b = node.toBox();
                if (frustum.isBoundingBoxInFrustum(b)) output.add(node);
            }
        }
    }

    public WakeNode get(int x, int z) {
        if (x >= 0 && x < dim) {
            if (z < 0 && NORTH != null) {
                return NORTH.nodes[Math.floorMod(z, dim)][x];
            } else if (z >= dim && SOUTH != null) {
                return SOUTH.nodes[Math.floorMod(z, dim)][x];
            } else if (z >= 0 && z < dim) {
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
    }

    private List<WakeNode> getAdjacentNodes(int x, int z) {
        ArrayList<WakeNode> result = new ArrayList<>();
        WakeNode n;
        n = this.get(x, z + 1); if (n != null) result.add(n);
        n = this.get(x + 1, z); if (n != null) result.add(n);
        n = this.get(x, z - 1); if (n != null) result.add(n);
        n = this.get(x - 1, z); if (n != null) result.add(n);
        return result;
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
        if (imgBuffer == null) {
            initTexture(WakeHandler.resolution.res);
        }

        World world = Minecraft.getMinecraft().world;
        float nightVisionFactor = getNightVisionFactor();
        float sunBrightness = world.getSunBrightness(1.0f);
        double wakeOpacity = WakesConfig.wakeOpacity;
        int rowStride = dim * texRes;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int z = 0; z < dim; z++) {
            for (int x = 0; x < dim; x++) {
                WakeNode node = nodes[z][x];
                int nodeOffset = texRes * 4 * (z * rowStride + x);

                if (node == null) {
                    for (int r = 0; r < texRes; r++) {
                        int pixelOffset = 4 * (r * rowStride);
                        for (int c = 0; c < texRes; c++) {
                            imgBuffer.putInt(nodeOffset + pixelOffset + c * 4, 0);
                        }
                    }
                    continue;
                }

                mutablePos.setPos(node.x, node.y, node.z);
                int fluidColor = WaterTintUtils.getFluidColor(world, mutablePos);
                int light = world.getCombinedLight(mutablePos, 0);
                int skyLight = (light >> 20) & 0xF;
                int blockLight = (light >> 4) & 0xF;
                float brightness = Math.max(skyLight * sunBrightness, blockLight) / 15f;
                if (nightVisionFactor > 0) {
                    brightness = Math.max(brightness, nightVisionFactor);
                }
                int b = (int) (brightness * 255);
                int lightCol = 0xFF000000 | (b << 16) | (b << 8) | b;
                float opacity = (float) ((1 - node.t * node.t) * wakeOpacity);

                SimulationNode sim = node.simulationNode;
                for (int r = 0; r < texRes; r++) {
                    int pixelOffset = 4 * (r * rowStride);
                    for (int c = 0; c < texRes; c++) {
                        imgBuffer.putInt(nodeOffset + pixelOffset + c * 4, sim.getPixelColor(c, r, fluidColor, lightCol, opacity));
                    }
                }
            }
        }
        hasPopulatedPixels = true;
    }

    private static float getNightVisionFactor() {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null || !player.isPotionActive(MobEffects.NIGHT_VISION)) {
            return 0;
        }
        PotionEffect effect = player.getActivePotionEffect(MobEffects.NIGHT_VISION);
        if (effect == null) return 0;
        int duration = effect.getDuration();
        return duration > 200 ? 1.0f : 0.7f + MathHelper.sin(((float) duration) * (float) Math.PI * 0.2f) * 0.3f;
    }
}
