package com.leclowndu93150.wakes.render;

import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.config.enums.Resolution;
import com.leclowndu93150.wakes.duck.ProducesWake;
import com.leclowndu93150.wakes.particle.custom.SplashPlaneParticle;
import com.leclowndu93150.wakes.simulation.WakeHandler;
import com.leclowndu93150.wakes.utils.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SplashPlaneRenderer {

    private static ArrayList<Vector2D> points;
    private static List<Triangle2D> triangles;
    private static ArrayList<Vec3d> vertices;
    private static ArrayList<Vec3d> normals;

    public static Map<Resolution, WakeTexture> wakeTextures = null;

    private static void initTextures() {
        wakeTextures = new HashMap<>();
        wakeTextures.put(Resolution.EIGHT, new WakeTexture(Resolution.EIGHT.res, false));
        wakeTextures.put(Resolution.SIXTEEN, new WakeTexture(Resolution.SIXTEEN.res, false));
        wakeTextures.put(Resolution.THIRTYTWO, new WakeTexture(Resolution.THIRTYTWO.res, false));
    }

    private static final double SQRT_8 = Math.sqrt(8);

    public static void init() {
        distributePoints();
        generateMesh();
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!WakeHandler.getInstance().isPresent()) {
            return;
        }

        Entity viewer = Minecraft.getMinecraft().getRenderViewEntity();
        if (viewer == null) return;

        double cx = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * event.getPartialTicks();
        double cy = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * event.getPartialTicks();
        double cz = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * event.getPartialTicks();

        Frustum frustum = new Frustum();
        frustum.setPosition(cx, cy, cz);

        WakeHandler wakeHandler = WakeHandler.getInstance().get();
        for (SplashPlaneParticle particle : wakeHandler.getVisible(frustum, SplashPlaneParticle.class)) {
            if (particle.isRenderReady) {
                render(particle.owner, particle, event.getPartialTicks(), cx, cy, cz);
            }
        }
    }

    public static <T extends Entity> void render(T entity, SplashPlaneParticle splashPlane, float partialTicks, double cx, double cy, double cz) {
        if (wakeTextures == null) initTextures();
        if (WakesConfig.disableMod || !WakesUtils.getEffectRuleFromSource(entity).renderPlanes) {
            return;
        }

        GlStateManager.pushMatrix();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.enableTexture2D();
        GlStateManager.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.disableLighting();
        GlStateManager.disableAlpha();
        GlStateManager.depthMask(false);
        GlStateManager.enableFog();

        float x = (float) (MathHelper.clampedLerp(splashPlane.getPrevX(), splashPlane.getX(), partialTicks) - cx);
        float y = (float) (MathHelper.clampedLerp(splashPlane.getPrevY(), splashPlane.getY(), partialTicks) - cy);
        float z = (float) (MathHelper.clampedLerp(splashPlane.getPrevZ(), splashPlane.getZ(), partialTicks) - cz);

        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(splashPlane.lerpedYaw + 180f, 0, 1, 0);

        float velocity = (float) Math.floor(((ProducesWake) entity).wakes$getHorizontalVelocity() * 20) / 20f;
        float progress = (float) Math.min(1f, velocity / WakesConfig.maxSplashPlaneVelocity);
        float scalar = (float) (WakesConfig.splashPlaneScale * Math.sqrt(entity.width * Math.max(1f, progress) + 1) / 3f);
        GlStateManager.scale(scalar, scalar, scalar);

        wakeTextures.get(WakeHandler.resolution).loadTexture(splashPlane.imgBuffer);
        renderSurface();

        GlStateManager.disableFog();
        GlStateManager.depthMask(true);
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static void renderSurface() {
        GlStateManager.disableCull();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_TEX);

        for (int s = -1; s < 2; s++) {
            if (s == 0) continue;
            for (int i = 0; i < vertices.size(); i++) {
                Vec3d vertex = vertices.get(i);
                buffer.pos(
                                s * (vertex.x * WakesConfig.splashPlaneWidth + WakesConfig.splashPlaneGap),
                                vertex.z * WakesConfig.splashPlaneHeight,
                                vertex.y * WakesConfig.splashPlaneDepth)
                        .tex((float) vertex.x, (float) vertex.y).endVertex();
            }
        }

        tessellator.draw();
        GlStateManager.enableCull();
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

    private static Vec3d normal(double x, double y) {
        double nx = SQRT_8 / (4 * (4 * x + y - SQRT_8));
        double ny = SQRT_8 / (4 * (2 * x * x - SQRT_8 + 1));
        double yawRad = Math.toRadians(Math.toDegrees(Math.atan(ny)));
        double pitchRad = Math.toRadians(Math.toDegrees(Math.atan(nx)));
        double cosP = Math.cos(pitchRad);
        return new Vec3d(-Math.sin(yawRad) * cosP, Math.sin(pitchRad), Math.cos(yawRad) * cosP);
    }

    private static void distributePoints() {
        int res = WakesConfig.splashPlaneResolution;
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
                vertices.add(new Vec3d(x, y, height(x, y)));
                normals.add(normal(x, y));
            }
        }
    }
}
