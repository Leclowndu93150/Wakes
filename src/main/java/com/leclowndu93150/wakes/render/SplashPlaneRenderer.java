package com.leclowndu93150.wakes.render;


import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.config.enums.Resolution;
import com.leclowndu93150.wakes.duck.ProducesWake;
import com.leclowndu93150.wakes.particle.custom.SplashPlaneParticle;
import com.leclowndu93150.wakes.simulation.WakeHandler;
import com.leclowndu93150.wakes.utils.*;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.api.distmarker.Dist;
import org.joml.Matrix4f;

import java.util.*;

@EventBusSubscriber(value = Dist.CLIENT)
public class SplashPlaneRenderer {

    private static ArrayList<Vector2D> points;
    private static List<Triangle2D> triangles;
    private static ArrayList<Vec3> vertices;
    private static ArrayList<Vec3> normals;

    public static Map<Resolution, WakeTexture> wakeTextures = null;

    private static void initTextures() {
        wakeTextures = Map.of(
                Resolution.EIGHT, new WakeTexture(Resolution.EIGHT.res, false),
                Resolution.SIXTEEN, new WakeTexture(Resolution.SIXTEEN.res, false),
                Resolution.THIRTYTWO, new WakeTexture(Resolution.THIRTYTWO.res, false)
        );
    }

    private static final double SQRT_8 = Math.sqrt(8);

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onRenderLevelStage(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        if (WakeHandler.getInstance().isEmpty()) {
            return;
        }

        WakeHandler wakeHandler = WakeHandler.getInstance().get();
        for (SplashPlaneParticle particle : wakeHandler.getVisible(event.getFrustum(), SplashPlaneParticle.class)) {
            if (particle.isRenderReady) {
                SplashPlaneRenderer.render(particle.owner, particle, event, event.getPoseStack());
            }
        }
    }

    public static <T extends Entity> void render(T entity, SplashPlaneParticle splashPlane, RenderLevelStageEvent context, PoseStack matrices) {
        if (wakeTextures == null) initTextures();
        if (WakesConfig.GENERAL.disableMod.get() || !WakesUtils.getEffectRuleFromSource(entity).renderPlanes) {
            return;
        }

        matrices.pushPose();
        splashPlane.translateMatrix(context, matrices);
        matrices.mulPose(Axis.YP.rotationDegrees(splashPlane.lerpedYaw + 180f));
        float velocity = (float) Math.floor(((ProducesWake) entity).wakes$getHorizontalVelocity() * 20) / 20f;
        float progress = Math.min(1f, velocity / WakesConfig.APPEARANCE.maxSplashPlaneVelocity.get().floatValue());
        float scalar = (float) (WakesConfig.APPEARANCE.splashPlaneScale.get() * Math.sqrt(entity.getBbWidth() * Math.max(1f, progress) + 1) / 3f);
        matrices.scale(scalar, scalar, scalar);
        Matrix4f matrix = matrices.last().pose();

        wakeTextures.get(WakeHandler.resolution).loadTexture(splashPlane.imgPtr, GlConst.GL_RGBA);
        renderSurface(matrix);

        matrices.popPose();
    }

    private static void renderSurface(Matrix4f matrix) {
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.BLOCK);
        int light = LightTexture.FULL_BRIGHT;
        for (int s = -1; s < 2; s++) {
            if (s == 0) continue;
            for (int i = 0; i < vertices.size(); i++) {
                Vec3 vertex = vertices.get(i);
                Vec3 normal = normals.get(i);
                buffer.addVertex(matrix,
                                (float) (s * (vertex.x * WakesConfig.APPEARANCE.splashPlaneWidth.get() + WakesConfig.APPEARANCE.splashPlaneGap.get())),
                                (float) (vertex.z * WakesConfig.APPEARANCE.splashPlaneHeight.get()),
                                (float) (vertex.y * WakesConfig.APPEARANCE.splashPlaneDepth.get()))
                        .setUv((float) (vertex.x), (float) (vertex.y))
                        .setLight(LightTexture.FULL_BRIGHT)
                        .setColor(1f, 1f, 1f, 1f)
                        .setNormal((float) normal.x, (float) normal.y, (float) normal.z);
            }
        }

        MeshData built = buffer.buildOrThrow();

        GpuBuffer buffer2 = DefaultVertexFormat.BLOCK.uploadImmediateVertexBuffer(built.vertexBuffer());
        GpuBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.TRIANGLES).getBuffer(built.drawState().indexCount());
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Splash Plane", Minecraft.getInstance().getMainRenderTarget().getColorTextureView(), OptionalInt.empty(), Minecraft.getInstance().getMainRenderTarget().getDepthTextureView(), OptionalDouble.empty())) {
            pass.setPipeline(RenderPipelines.TRANSLUCENT_MOVING_BLOCK);
            pass.bindSampler("Sampler0", RenderSystem.getShaderTexture(0));
            pass.bindSampler("Sampler2", RenderSystem.getShaderTexture(2));
            RenderSystem.bindDefaultUniforms(pass);

            pass.setVertexBuffer(0, buffer2);
            pass.setIndexBuffer(indices, RenderSystem.getSequentialBuffer(VertexFormat.Mode.TRIANGLES).type());
            pass.drawIndexed(0, 0, built.drawState().indexCount(), 1);
        }
        built.close();
    }

    private static double upperBound(double x) {
        return - 2 * x * x + SQRT_8 * x;
    }

    private static double lowerBound(double x) {
        return (SQRT_8 - 2) * x * x;
    }

    private static double height(double x, double y) {
        return 4 * (x * (SQRT_8 - x) -y - x * x) / SQRT_8;
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
            for (Vector2D vec : new Vector2D[] {tri.a, tri.b, tri.c}) {
                double x = vec.x, y = vec.y;
                vertices.add(new Vec3(x, y, height(x, y)));
                normals.add(normal(x, y));
            }
        }
    }

    public static void initSplashPlane() {
        distributePoints();
        generateMesh();
    }
}