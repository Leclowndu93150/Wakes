package com.leclowndu93150.wakes.render;

import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.duck.ProducesWake;
import com.leclowndu93150.wakes.particle.custom.SplashPlaneParticle;
import com.leclowndu93150.wakes.simulation.WakeHandler;
import com.leclowndu93150.baguettelib.math.delaunay.DelaunayTriangulator;
import com.leclowndu93150.baguettelib.math.delaunay.NotEnoughPointsException;
import com.leclowndu93150.baguettelib.math.delaunay.Triangle2D;
import com.leclowndu93150.baguettelib.math.delaunay.Vector2D;
import com.leclowndu93150.wakes.utils.WakesUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class SplashPlaneRenderer {

    private static ArrayList<Vector2D> points;
    private static List<Triangle2D> triangles;
    private static ArrayList<Vec3> vertices;
    private static ArrayList<Vec3> normals;

    private static final double SQRT_8 = Math.sqrt(8);

    public static void init() {
    }

    public static void setup() {
        distributePoints();
        generateMesh();
    }

    public static void render(SubmitNodeCollector collector, PoseStack poseStack, Frustum frustum, Vec3 cameraPos) {
        if (WakeHandler.getInstance().isEmpty()) {
            return;
        }

        WakeHandler wakeHandler = WakeHandler.getInstance().get();
        for (SplashPlaneParticle particle : wakeHandler.getVisible(frustum, SplashPlaneParticle.class)) {
            if (particle.isRenderReady) {
                renderParticle(collector, poseStack, particle.owner, particle, cameraPos);
            }
        }
    }

    private static <T extends Entity> void renderParticle(SubmitNodeCollector collector, PoseStack poseStack, T entity, SplashPlaneParticle splashPlane, Vec3 cameraPos) {
        if (WakesConfig.GENERAL.disableMod.get() || !WakesUtils.getEffectRuleFromSource(entity).renderPlanes) {
            return;
        }

        poseStack.pushPose();
        splashPlane.translateMatrix(cameraPos, poseStack);
        poseStack.mulPose(Axis.YP.rotationDegrees(splashPlane.lerpedYaw + 180f));
        float velocity = (float) Math.floor(((ProducesWake) entity).wakes$getHorizontalVelocity() * 20) / 20f;
        float progress = Math.min(1f, velocity / WakesConfig.APPEARANCE.maxSplashPlaneVelocity.get().floatValue());
        float scalar = (float) (WakesConfig.APPEARANCE.splashPlaneScale.get() * Math.sqrt(entity.getBbWidth() * Math.max(1f, progress) + 1) / 3f);
        poseStack.scale(scalar, scalar, scalar);
        final Matrix4f matrix = new Matrix4f(poseStack.last().pose());
        poseStack.popPose();

        if (splashPlane.imgPtr == -1) return;
        if (splashPlane.wakeTexture == null) {
            splashPlane.wakeTexture = new WakeTexture(WakeHandler.resolution.res, false, 1);
        }
        if (splashPlane.pixelsDirty) {
            splashPlane.wakeTexture.loadTexture(splashPlane.imgPtr);
            splashPlane.pixelsDirty = false;
        }

        final int packedLight = computeEntityLight(entity);
        RenderType type = splashPlane.wakeTexture.renderType();

        WakeRenderer.submitAfterTerrain(collector, poseStack, type, (pose, vc) -> {
            currentLight = packedLight;
            for (int s = -1; s < 2; s++) {
                if (s == 0) continue;
                for (int i = 0; i < vertices.size(); i += 3) {
                    Vec3 v0 = vertices.get(i);
                    Vec3 n0 = normals.get(i);
                    Vec3 v1 = vertices.get(i + 1);
                    Vec3 n1 = normals.get(i + 1);
                    Vec3 v2 = vertices.get(i + 2);
                    Vec3 n2 = normals.get(i + 2);
                    addDegenerateQuad(vc, matrix, s, v0, n0, v1, n1, v2, n2);
                    addDegenerateQuad(vc, matrix, s, v0, n0, v2, n2, v1, n1);
                }
            }
        });
    }

    private static int computeEntityLight(Entity entity) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return LightCoordsUtil.FULL_BRIGHT;
        BlockPos pos = entity.blockPosition();
        if (!level.hasChunkAt(pos)) return LightCoordsUtil.FULL_BRIGHT;
        int block = level.getBrightness(LightLayer.BLOCK, pos);
        int sky = level.getBrightness(LightLayer.SKY, pos);
        return LightCoordsUtil.pack(block, sky);
    }

    private static int currentLight = LightCoordsUtil.FULL_BRIGHT;

    private static void addVertex(VertexConsumer vc, Matrix4f matrix, int side, Vec3 vertex, Vec3 normal) {
        vc.addVertex(matrix,
                        (float) (side * (vertex.x * WakesConfig.APPEARANCE.splashPlaneWidth.get() + WakesConfig.APPEARANCE.splashPlaneGap.get())),
                        (float) (vertex.z * WakesConfig.APPEARANCE.splashPlaneHeight.get()),
                        (float) (vertex.y * WakesConfig.APPEARANCE.splashPlaneDepth.get()))
                .setUv((float) vertex.x, (float) vertex.y)
                .setLight(currentLight)
                .setColor(1f, 1f, 1f, 1f)
                .setNormal((float) normal.x, (float) normal.y, (float) normal.z);
    }

    private static void addDegenerateQuad(VertexConsumer vc, Matrix4f matrix, int side, Vec3 a, Vec3 an, Vec3 b, Vec3 bn, Vec3 c, Vec3 cn) {
        addVertex(vc, matrix, side, a, an);
        addVertex(vc, matrix, side, b, bn);
        addVertex(vc, matrix, side, c, cn);
        addVertex(vc, matrix, side, c, cn);
    }

    private static double upperBound(double x) {
        return -2 * x * x + SQRT_8 * x;
    }

    private static double lowerBound(double x) {
        return (SQRT_8 - 2) * x * x;
    }

    private static double height(double x, double y) {
        return 4 * (x * (SQRT_8 - x) - y - x * x) / SQRT_8;
    }

    private static Vec3 normal(double x, double y) {
        double nx = SQRT_8 / (4 * (4 * x + y - SQRT_8));
        double ny = SQRT_8 / (4 * (2 * x * x - SQRT_8 + 1));
        return Vec3.directionFromRotation((float) Math.tan(nx), (float) Math.tan(ny));
    }

    private static void distributePoints() {
        int res = WakesConfig.APPEARANCE.splashPlaneResolution.getAsInt();
        points = new ArrayList<>();

        for (float i = 0; i < res; i++) {
            double x = i / (res - 1);
            double h = upperBound(x) - lowerBound(x);
            int n_points = (int) Math.max(1, Math.floor(h * res));
            for (float j = 0; j < n_points + 1; j++) {
                float y = (float) ((j / n_points) * h + lowerBound(x));
                points.add(new Vector2D(x, y));
            }
        }
    }

    private static void generateMesh() {
        vertices = new ArrayList<>();
        normals = new ArrayList<>();
        try {
            DelaunayTriangulator delaunay = new DelaunayTriangulator(points);
            delaunay.triangulate();
            triangles = delaunay.getTriangles();
        } catch (NotEnoughPointsException e) {
            e.printStackTrace();
        }
        for (Triangle2D tri : triangles) {
            for (Vector2D vec : new Vector2D[]{tri.a, tri.b, tri.c}) {
                double x = vec.x, y = vec.y;
                vertices.add(new Vec3(x, y, height(x, y)));
                normals.add(normal(x, y));
            }
        }
    }
}
