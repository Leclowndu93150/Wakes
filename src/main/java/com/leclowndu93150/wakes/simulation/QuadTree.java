package com.leclowndu93150.wakes.simulation;

import java.util.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.math.AxisAlignedBB;

public class QuadTree {
    public static final int BRICK_WIDTH = 4;
    private static final int MAX_DEPTH = (int) (26 - Math.log(BRICK_WIDTH) / Math.log(2));
    private static final int ROOT_X = (int) -Math.pow(2, 25);
    private static final int ROOT_Z = (int) -Math.pow(2, 25);
    private static final int ROOT_WIDTH = (int) Math.pow(2, 26);

    private final QuadTree ROOT;
    private QuadTree[] children;

    private final int bx, bz, bwidth;
    private final float by;
    private final int depth;
    private Brick brick;
    public final float yLevel;

    public QuadTree(float y) {
        this(ROOT_X, y, ROOT_Z, ROOT_WIDTH, 0, null);
    }

    private QuadTree(int x, float y, int z, int width, int depth, QuadTree root) {
        this.bx = x;
        this.by = y;
        this.bz = z;
        this.bwidth = width;
        this.depth = depth;
        this.ROOT = root == null ? this : root;
        this.yLevel = y;
    }

    private boolean hasLeaf() {
        return depth == MAX_DEPTH && brick != null;
    }

    private void initLeaf() {
        if (depth >= MAX_DEPTH) {
            this.brick = new Brick(bx, this.yLevel, bz, bwidth);
            this.ROOT.updateAdjacency(this);
        }
    }

    protected void updateAdjacency(QuadTree leaf) {
        if (this == leaf) return;
        if (!neighbors(leaf) && !intersects(leaf)) {
            return;
        }
        if (brick != null) {
            brick.updateAdjacency(leaf.brick);
            return;
        }
        if (children != null) {
            for (QuadTree tree : children) {
                tree.updateAdjacency(leaf);
            }
        }
    }

    private boolean contains(int x, int z) {
        return this.bx <= x && x < this.bx + this.bwidth &&
                this.bz <= z && z < this.bz + this.bwidth;
    }

    private boolean intersects(QuadTree other) {
        return !(this.bx > other.bx + other.bwidth ||
                this.bx + this.bwidth < other.bx ||
                this.bz > other.bz + other.bwidth ||
                this.bz + this.bwidth < other.bz);
    }

    private boolean intersectsArea(int minX, int minZ, int maxX, int maxZ) {
        return !(this.bx > maxX || this.bx + this.bwidth < minX ||
                this.bz > maxZ || this.bz + this.bwidth < minZ);
    }

    private boolean neighbors(QuadTree other) {
        return !(this.bx == other.bx + other.bwidth ||
                this.bx + this.bwidth == other.bx ||
                this.bz == other.bz + other.bwidth ||
                this.bz + this.bwidth == other.bz);
    }

    private AxisAlignedBB toBox() {
        return new AxisAlignedBB(this.bx, yLevel - 0.5, this.bz,
                this.bx + this.bwidth, yLevel + 0.5, this.bz + this.bwidth);
    }

    public void cleanupArea(int minX, int minZ, int maxX, int maxZ) {
        if (!this.intersectsArea(minX, minZ, maxX, maxZ)) {
            return;
        }

        if (hasLeaf() && brick != null) {
            if (bx >= minX && bx + bwidth <= maxX &&
                    bz >= minZ && bz + bwidth <= maxZ) {
                brick.deallocTexture();
                brick = null;
            }
            return;
        }

        if (children != null) {
            for (QuadTree tree : children) {
                tree.cleanupArea(minX, minZ, maxX, maxZ);
            }
        }
    }

    public boolean tick(WakeHandler wakeHandler) {
        if (hasLeaf()) {
            return brick.tick(wakeHandler);
        }
        if (children == null) return false;
        int aliveChildren = 0;
        for (QuadTree tree : children) {
            if (tree.tick(wakeHandler)) aliveChildren++;
        }
        if (aliveChildren == 0) this.prune();
        return aliveChildren > 0;
    }

    public boolean insert(WakeNode node) {
        if (!this.contains(node.x, node.z)) {
            return false;
        }

        if (depth == MAX_DEPTH) {
            if (brick == null) {
                initLeaf();
            }
            brick.insert(node);
            return true;
        }

        if (children == null) this.subdivide();
        for (QuadTree tree : children) {
            if (tree.insert(node)) return true;
        }
        return false;
    }

    public void recolorWakes() {
        if (hasLeaf()) {
            brick.populatePixels();
        }
        if (children == null) return;
        for (QuadTree tree : children) {
            tree.recolorWakes();
        }
    }

    public <T> void query(Frustum frustum, ArrayList<T> output, Class<T> type) {
        if (!frustum.isBoundingBoxInFrustum(this.toBox())) {
            return;
        }
        if (hasLeaf() && brick.occupied > 0) {
            if (type.equals(Brick.class)) {
                output.add(type.cast(brick));
            }
            if (type.equals(WakeNode.class)) {
                ArrayList<WakeNode> nodes = new ArrayList<>();
                brick.query(frustum, nodes);
                for (WakeNode node : nodes) {
                    output.add(type.cast(node));
                }
            }
            return;
        }
        if (children == null) return;
        for (QuadTree tree : children) {
            tree.query(frustum, output, type);
        }
    }

    private void subdivide() {
        if (depth == MAX_DEPTH) return;
        int x = this.bx;
        int z = this.bz;
        int w = this.bwidth >> 1;
        children = new QuadTree[]{
            new QuadTree(x, yLevel, z, w, depth + 1, this.ROOT),
            new QuadTree(x + w, yLevel, z, w, depth + 1, this.ROOT),
            new QuadTree(x, yLevel, z + w, w, depth + 1, this.ROOT),
            new QuadTree(x + w, yLevel, z + w, w, depth + 1, this.ROOT)
        };
    }

    public void prune() {
        if (children != null) {
            for (QuadTree tree : children) {
                tree.prune();
                if (tree.hasLeaf()) tree.brick.deallocTexture();
            }
        }
        children = null;
    }
}
