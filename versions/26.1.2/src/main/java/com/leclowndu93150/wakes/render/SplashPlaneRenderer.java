package com.leclowndu93150.wakes.render;

import com.leclowndu93150.wakes.WakesClient;
import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.config.enums.Resolution;
import com.leclowndu93150.wakes.duck.ProducesWake;
import com.leclowndu93150.wakes.particle.custom.SplashPlaneParticle;
import com.leclowndu93150.wakes.render.enums.RenderType;
import com.leclowndu93150.wakes.simulation.WakeHandler;
import com.leclowndu93150.baguettelib.math.delaunay.DelaunayTriangulator;
import com.leclowndu93150.baguettelib.math.delaunay.NotEnoughPointsException;
import com.leclowndu93150.baguettelib.math.delaunay.Triangle2D;
import com.leclowndu93150.baguettelib.math.delaunay.Vector2D;
import com.leclowndu93150.wakes.utils.WakesUtils;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class SplashPlaneRenderer {

    private static ArrayList<Vector2D> points;
    private static List<Triangle2D> triangles;
    private static ArrayList<Vec3> vertices;
    private static ArrayList<Vec3> normals;

    public static Map<Resolution, WakeTexture> wakeTextures = null;

    private static void initTextures() {
        wakeTextures = Map.of(
                Resolution.EIGHT, new WakeTexture(Resolution.EIGHT.res, false, 1),
                Resolution.SIXTEEN, new WakeTexture(Resolution.SIXTEEN.res, false, 1),
                Resolution.THIRTYTWO, new WakeTexture(Resolution.THIRTYTWO.res, false, 1)
        );
    }

    private static final double SQRT_8 = Math.sqrt(8);

    public static void init() {
        NeoForge.EVENT_BUS.register(SplashPlaneRenderer.class);
    }

    public static void setup() {
        distributePoints();
        generateMesh();
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onRenderLevel(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        if (WakeHandler.getInstance().isEmpty()) {
            return;
        }

        WakeHandler wakeHandler = WakeHandler.getInstance().get();
        CameraRenderState camera = event.getLevelRenderState().cameraRenderState;
        for (SplashPlaneParticle particle : wakeHandler.getVisible(camera.cullFrustum, SplashPlaneParticle.class)) {
            if (particle.isRenderReady) {
                SplashPlaneRenderer.render(particle.owner, particle, camera, event.getPoseStack());
            }
        }
    }

    public static <T extends Entity> void render(T entity, SplashPlaneParticle splashPlane, CameraRenderState camera, PoseStack matrices) {
        if (wakeTextures == null) initTextures();
        if (WakesConfig.GENERAL.disableMod.get() || !WakesUtils.getEffectRuleFromSource(entity).renderPlanes) {
            return;
        }

        matrices.pushPose();
        splashPlane.translateMatrix(camera, matrices);
        matrices.mulPose(Axis.YP.rotationDegrees(splashPlane.lerpedYaw + 180f));
        float velocity = (float) Math.floor(((ProducesWake) entity).wakes$getHorizontalVelocity() * 20) / 20f;
        float progress = Math.min(1f, velocity / WakesConfig.APPEARANCE.maxSplashPlaneVelocity.get().floatValue());
        float scalar = (float) (WakesConfig.APPEARANCE.splashPlaneScale.get() * Math.sqrt(entity.getBbWidth() * Math.max(1f, progress) + 1) / 3f);
        matrices.scale(scalar, scalar, scalar);
        Matrix4f matrix = matrices.last().pose();
        matrices.popPose();

        WakeTexture texture = wakeTextures.get(WakeHandler.resolution);
        texture.loadTexture(splashPlane.imgPtr);
        renderSurface(matrix, texture);
    }

    private static void renderSurface(Matrix4f matrix, WakeTexture texture) {
        RenderPipeline pipeline = RenderType.getPipeline();
        BufferBuilder bb = Tesselator.getInstance().begin(pipeline.getVertexFormatMode(), pipeline.getVertexFormat());

        for (int s = -1; s < 2; s++) {
            if (s == 0) continue;
            for (int i = 0; i < vertices.size(); i += 3) {
                Vec3 v0 = vertices.get(i);
                Vec3 n0 = normals.get(i);
                Vec3 v1 = vertices.get(i + 1);
                Vec3 n1 = normals.get(i + 1);
                Vec3 v2 = vertices.get(i + 2);
                Vec3 n2 = normals.get(i + 2);
                addDegenerateQuad(bb, matrix, s, v0, n0, v1, n1, v2, n2);
                addDegenerateQuad(bb, matrix, s, v0, n0, v2, n2, v1, n1);
            }
        }

        MeshData built = bb.buildOrThrow();
        MeshData.DrawState drawState = built.drawState();
        VertexFormat format = drawState.format();
        GpuBuffer vertexBuffer = format.uploadImmediateVertexBuffer(built.vertexBuffer());

        GpuBuffer indexBuffer;
        VertexFormat.IndexType indexType;
        if (pipeline.getVertexFormatMode() == VertexFormat.Mode.QUADS) {
            RenderSystem.AutoStorageIndexBuffer seqBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
            indexBuffer = seqBuffer.getBuffer(drawState.indexCount());
            indexType = seqBuffer.type();
        } else {
            indexBuffer = format.uploadImmediateIndexBuffer(built.indexBuffer());
            indexType = drawState.indexType();
        }

        GpuSampler sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);
        Minecraft client = Minecraft.getInstance();

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(RenderSystem.getModelViewMatrix(), new Vector4f(1f, 1f, 1f, 1f), new Vector3f(), new Matrix4f());

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> WakesClient.MOD_ID + " splash plane",
                client.getMainRenderTarget().getColorTextureView(),
                OptionalInt.empty(),
                client.getMainRenderTarget().getDepthTextureView(),
                OptionalDouble.empty())) {

            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", dynamicTransforms);
            pass.bindTexture("Sampler0", texture.getTextureView(), sampler);
            pass.setVertexBuffer(0, vertexBuffer);
            pass.setIndexBuffer(indexBuffer, indexType);
            pass.drawIndexed(0, 0, drawState.indexCount(), 1);
        }
        built.close();
    }

    private static void addVertex(BufferBuilder bb, Matrix4f matrix, int side, Vec3 vertex, Vec3 normal) {
        bb.addVertex(matrix,
                        (float) (side * (vertex.x * WakesConfig.APPEARANCE.splashPlaneWidth.get() + WakesConfig.APPEARANCE.splashPlaneGap.get())),
                        (float) (vertex.z * WakesConfig.APPEARANCE.splashPlaneHeight.get()),
                        (float) (vertex.y * WakesConfig.APPEARANCE.splashPlaneDepth.get()))
                .setUv((float) vertex.x, (float) vertex.y)
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setColor(1f, 1f, 1f, 1f)
                .setNormal((float) normal.x, (float) normal.y, (float) normal.z);
    }

    private static void addDegenerateQuad(BufferBuilder bb, Matrix4f matrix, int side, Vec3 a, Vec3 an, Vec3 b, Vec3 bn, Vec3 c, Vec3 cn) {
        addVertex(bb, matrix, side, a, an);
        addVertex(bb, matrix, side, b, bn);
        addVertex(bb, matrix, side, c, cn);
        addVertex(bb, matrix, side, c, cn);
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
