package com.chinaex123.redstone_chan_lib.client.outline;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import com.chinaex123.redstone_chan_lib.api.outline.BlockOutline;
import com.chinaex123.redstone_chan_lib.network.OutlineResyncPayload;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.*;

/**
 * 方块描边渲染。
 * <p>
 * 四种模式的差别在于混合方式与透明度：发光与高光用加法混合，黑色用普通混合，
 * 机械动力风格在原地淡入淡出——出现时一边变亮一边变粗，消失时一边变暗一边变细，
 * 方框本身不会向外扩大。
 */
@EventBusSubscriber(modid = RedstoneChanLIB.MODID, value = Dist.CLIENT)
public final class BlockOutlineRenderer {

    /** 机械动力风格的淡入时长（秒）。 */
    private static final float FADE_IN_SECONDS = 0.25F;
    /** 机械动力风格的淡出时长（秒）。 */
    private static final float FADE_OUT_SECONDS = 0.4F;
    /** 粗细换算：一格粗细对应多少屏幕像素。 */
    private static final double THICKNESS_PIXELS_PER_UNIT = 100.0D;
    /** 光晕的最小像素宽度，避免远处看不见。 */
    private static final double MIN_PIXEL_WIDTH = 1.5D;
    /** 光晕的最大像素宽度。 */
    private static final double MAX_PIXEL_WIDTH = 16.0D;
    /** 方框离方块表面的距离。 */
    private static final double BOX_INFLATE = 1.0D / 128.0D;
    /** 彩色描边色相流动一圈的时长（毫秒）。 */
    private static final long RAINBOW_PERIOD = 6000L;

    /** 每个位置的淡入进度：0 表示完全消失，1 表示完全显示。 */
    private static final Map<BlockPos, Float> FADE = new HashMap<>();
    /** 淡出时用来继续画的那个描边配置。 */
    private static final Map<BlockPos, BlockOutline.Outline> LAST_OUTLINE = new HashMap<>();
    /** 机械动力风格还剩下多少秒的“保持完全显示”。 */
    private static final Map<BlockPos, Double> HOLD_LEFT = new HashMap<>();
    /** 已经播完一次脉冲动画的位置：留在数据里，但不再绘制。 */
    private static final Set<BlockPos> DONE = new HashSet<>();
    /** 上一次看到的写入序号，用来判断是不是又被设置了一次。 */
    private static final Map<BlockPos, Long> LAST_STAMP = new HashMap<>();

    /** 上一次渲染所在的维度，用来在切换维度时清空本地记录。 */
    @Nullable
    private static Level lastLevel;

    /** 上一帧的时间戳，用来算淡入淡出的速度。 */
    private static long lastNanos;

    private BlockOutlineRenderer() {
    }

    /**
     * 关卡渲染回调。
     *
     * @param event 关卡渲染阶段事件
     */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            reset();
            return;
        }
        // 换维度（或者重进世界）时，本地记录的坐标已经不属于当前维度了
        if (lastLevel != level) {
            BlockOutline.clearAll();
            FADE.clear();
            LAST_OUTLINE.clear();
            HOLD_LEFT.clear();
            DONE.clear();
            LAST_STAMP.clear();
            lastLevel = level;
            lastNanos = 0L;
            if (Minecraft.getInstance().getConnection() != null) {
                PacketDistributor.sendToServer(OutlineResyncPayload.INSTANCE);
            }
        }

        float delta = deltaSeconds();
        updateFade(delta);

        if (FADE.isEmpty()) {
            return;
        }
        renderBoxes(event, snapshotFade(), FADE);
    }

    /**
     * 把一组“坐标 → 描边”画出来。
     *
     * @param event      关卡渲染阶段事件
     * @param targets    要画的坐标与描边
     * @param progresses 坐标对应的显示进度，缺省按 1 处理
     */
    static void renderBoxes(RenderLevelStageEvent event, Map<BlockPos, BlockOutline.Outline> targets,
                            Map<BlockPos, Float> progresses) {
        if (targets.isEmpty()) {
            return;
        }
        Matrix4f projection = new Matrix4f(event.getProjectionMatrix());
        Matrix4fStack stack = RenderSystem.getModelViewStack();
        Vec3 cameraPos = event.getCamera().getPosition();
        Matrix4f cameraMatrix = new Matrix4f(event.getModelViewMatrix());
        cameraMatrix.translate((float) -cameraPos.x, (float) -cameraPos.y, (float) -cameraPos.z);
        stack.pushMatrix();
        stack.identity();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        for (BlockOutline.Mode mode : BlockOutline.Mode.values()) {
            drawMode(mode, cameraMatrix, projection, targets, progresses);
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        stack.popMatrix();
        RenderSystem.setProjectionMatrix(projection, VertexSorting.DISTANCE_TO_ORIGIN);
        RenderSystem.applyModelViewMatrix();
    }

    /**
     * 画某一个模式的所有方块描边。
     *
     * @param mode         描边模式
     * @param cameraMatrix 相机矩阵
     * @param projection   投影矩阵
     * @param allTargets   全部要画的坐标与描边
     * @param progresses   坐标对应的显示进度
     */
    private static void drawMode(BlockOutline.Mode mode, Matrix4f cameraMatrix, Matrix4f projection,
                                 Map<BlockPos, BlockOutline.Outline> allTargets,
                                 Map<BlockPos, Float> progresses) {
        if (mode == BlockOutline.Mode.WIRE) {
            drawWire(cameraMatrix, allTargets, progresses);
            return;
        }
        boolean additive = mode != BlockOutline.Mode.BLACK && mode != BlockOutline.Mode.CREATE;
        boolean throughWalls = false;
        List<Map.Entry<BlockPos, BlockOutline.Outline>> targets = new ArrayList<>();
        for (Map.Entry<BlockPos, BlockOutline.Outline> entry : allTargets.entrySet()) {
            if (entry.getValue().mode() == mode) {
                targets.add(entry);
                throughWalls |= entry.getValue().throughWalls();
            }
        }
        if (targets.isEmpty()) {
            return;
        }
        if (throughWalls) {
            RenderSystem.disableDepthTest();
        } else {
            RenderSystem.enableDepthTest();
        }
        if (additive) {
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        } else {
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        }

        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        boolean any = false;
        for (Map.Entry<BlockPos, BlockOutline.Outline> entry : targets) {
            BlockPos pos = entry.getKey();
            BlockOutline.Outline outline = entry.getValue();
            float progress = progresses.getOrDefault(pos, 1.0F);
            if (progress <= 0.0F || mode == BlockOutline.Mode.CREATE && progress < 0.12F) {
                continue;
            }
            AABB box = new AABB(pos).inflate(BOX_INFLATE);
            double width = pixelWidth(outline, mode, progress, pos, cameraMatrix, projection);
            if (width <= 0.0D) {
                continue;
            }
            int color = colorFor(outline, mode, progress);
            boolean rainbow = outline.style() == BlockOutline.Style.RAINBOW && mode != BlockOutline.Mode.BLACK;
            if (appendBox(builder, cameraMatrix, box, width, color, rainbow, rainbowPhase())) {
                any = true;
            }
        }
        if (any) {
            MeshData mesh = builder.buildOrThrow();
            BufferUploader.drawWithShader(mesh);
        }
    }

    /**
     * 画线框模式：每个方块只画十二条棱，每条棱是一条细线。
     *
     * @param cameraMatrix 相机矩阵
     * @param allTargets   全部要画的坐标与描边
     * @param progresses   坐标对应的显示进度
     */
    private static void drawWire(Matrix4f cameraMatrix, Map<BlockPos, BlockOutline.Outline> allTargets,
                                 Map<BlockPos, Float> progresses) {
        List<AABB> boxes = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        boolean throughWalls = false;
        boolean rainbow = false;
        float lineWidth = 1.0F;
        for (Map.Entry<BlockPos, BlockOutline.Outline> entry : allTargets.entrySet()) {
            BlockOutline.Outline outline = entry.getValue();
            if (outline.mode() != BlockOutline.Mode.WIRE) {
                continue;
            }
            float progress = progresses.getOrDefault(entry.getKey(), 1.0F);
            if (progress <= 0.0F) {
                continue;
            }
            boxes.add(new AABB(entry.getKey()).inflate(BOX_INFLATE));
            colors.add(colorFor(outline, BlockOutline.Mode.WIRE, progress));
            throughWalls |= outline.throughWalls();
            rainbow |= outline.style() == BlockOutline.Style.RAINBOW;
            lineWidth = Math.max(lineWidth, (float) Mth.clamp(outline.thickness() * 100.0D, 1.0D, 4.0D));
        }
        if (boxes.isEmpty()) {
            return;
        }
        if (throughWalls) {
            RenderSystem.disableDepthTest();
        } else {
            RenderSystem.enableDepthTest();
        }
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.lineWidth(lineWidth);
        drawWireBoxes(cameraMatrix, boxes, colors, rainbow, rainbowPhase());
        RenderSystem.lineWidth(1.0F);
    }

    /**
     * 把一组包围盒画成线框。
     * <p>
     * 只写顶点、不碰 GL 状态，调用前要把矩阵、混合、深度测试都设置好。
     * 方块描边与实体扫描高亮都用这一份实现。
     *
     * @param cameraMatrix 相机矩阵
     * @param boxes        要画线框的包围盒
     * @param colors       每个包围盒的颜色，要与 {@code boxes} 一一对应
     * @param rainbow      是否按顶点位置铺开彩色
     * @param phase        彩色流动的相位
     */
    private static void drawWireBoxes(Matrix4f cameraMatrix, List<AABB> boxes, List<Integer> colors,
                                      boolean rainbow, float phase) {
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        boolean any = false;
        for (int index = 0; index < boxes.size(); index++) {
            int color = colors.get(index);
            if (appendWireEdges(builder, cameraMatrix, boxes.get(index), color, rainbow, phase)) {
                any = true;
            }
        }
        if (any) {
            MeshData mesh = builder.buildOrThrow();
            BufferUploader.drawWithShader(mesh);
        }
    }

    /**
     * 把一个包围盒的十二条棱写进线段缓冲区。
     *
     * @param builder      顶点缓冲
     * @param cameraMatrix 相机矩阵
     * @param box          包围盒
     * @param color        ARGB 颜色（彩色风格时只用来取透明度）
     * @param rainbow      是否按顶点位置铺开彩色
     * @param phase        彩色流动的相位
     * @return 是否写入了顶点
     */
    private static boolean appendWireEdges(BufferBuilder builder, Matrix4f cameraMatrix, AABB box,
                                           int color, boolean rainbow, float phase) {
        Vector3f[] corners = new Vector3f[8];
        Vector3f[] world = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            double x = (i & 1) == 0 ? box.minX : box.maxX;
            double y = (i & 2) == 0 ? box.minY : box.maxY;
            double z = (i & 4) == 0 ? box.minZ : box.maxZ;
            corners[i] = toVector3(cameraMatrix, x, y, z);
            world[i] = new Vector3f((float) x, (float) y, (float) z);
        }
        boolean any = false;
        for (int a = 0; a < 8; a++) {
            for (int bit = 1; bit <= 4; bit <<= 1) {
                int b = a | bit;
                if (b == a) {
                    continue;
                }
                int colorA = rainbow ? rainbowColor(world[a], color, phase) : color;
                int colorB = rainbow ? rainbowColor(world[b], color, phase) : color;
                builder.addVertex(corners[a].x(), corners[a].y(), corners[a].z()).setColor(colorA);
                builder.addVertex(corners[b].x(), corners[b].y(), corners[b].z()).setColor(colorB);
                any = true;
            }
        }
        return any;
    }

    /**
     * 画一组独立包围盒的线框（实体扫描高亮用）。
     * <p>
     * 自带矩阵与 GL 状态设置，调用方只要在关卡渲染阶段里调一次即可。
     *
     * @param event        关卡渲染阶段事件
     * @param boxes        要画线框的包围盒（世界坐标）
     * @param rgb          线框颜色
     * @param throughWalls 是否穿透方块
     * @param thickness    线宽（方块单位，一格约等于 100 像素）
     */
    static void renderWireBoxes(RenderLevelStageEvent event, List<AABB> boxes, int rgb, boolean throughWalls,
                                double thickness) {
        if (boxes.isEmpty()) {
            return;
        }
        Matrix4f projection = new Matrix4f(event.getProjectionMatrix());
        Matrix4fStack stack = RenderSystem.getModelViewStack();
        Vec3 cameraPos = event.getCamera().getPosition();
        Matrix4f cameraMatrix = new Matrix4f(event.getModelViewMatrix());
        cameraMatrix.translate((float) -cameraPos.x, (float) -cameraPos.y, (float) -cameraPos.z);
        stack.pushMatrix();
        stack.identity();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        if (throughWalls) {
            RenderSystem.disableDepthTest();
        } else {
            RenderSystem.enableDepthTest();
        }
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        float lineWidth = (float) Mth.clamp(thickness * 100.0D, 1.0D, 4.0D);
        RenderSystem.lineWidth(lineWidth);
        List<Integer> colors = new ArrayList<>(boxes.size());
        for (int i = 0; i < boxes.size(); i++) {
            colors.add(rgb);
        }
        drawWireBoxes(cameraMatrix, boxes, colors, false, 0.0F);
        RenderSystem.lineWidth(1.0F);

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        stack.popMatrix();
        RenderSystem.setProjectionMatrix(projection, VertexSorting.DISTANCE_TO_ORIGIN);
        RenderSystem.applyModelViewMatrix();
    }

    /**
     * 把方块包围盒的十二条棱拢成一个面朝镜头的方框写进缓冲区。
     *
     * @param builder      顶点缓冲
     * @param cameraMatrix 相机矩阵
     * @param box          方块包围盒
     * @param halfWidth    半宽（方块单位，镜头空间）
     * @param color        ARGB 颜色（彩色风格时只用来取透明度）
     * @param rainbow      是否按顶点位置铺开彩色
     * @param phase        彩色流动的相位
     * @return 是否写入了顶点
     */
    private static boolean appendBox(BufferBuilder builder, Matrix4f cameraMatrix, AABB box, double halfWidth,
                                     int color, boolean rainbow, float phase) {
        Vector3f[] corners = new Vector3f[8];
        Vector3f[] world = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            double x = (i & 1) == 0 ? box.minX : box.maxX;
            double y = (i & 2) == 0 ? box.minY : box.maxY;
            double z = (i & 4) == 0 ? box.minZ : box.maxZ;
            corners[i] = toVector3(cameraMatrix, x, y, z);
            world[i] = new Vector3f((float) x, (float) y, (float) z);
        }
        boolean any = false;
        for (int a = 0; a < 8; a++) {
            for (int bit = 1; bit <= 4; bit <<= 1) {
                int b = a | bit;
                if (b == a) {
                    continue;
                }
                int colorA = rainbow ? rainbowColor(world[a], color, phase) : color;
                int colorB = rainbow ? rainbowColor(world[b], color, phase) : color;
                if (appendEdge(builder, corners[a], corners[b], halfWidth, colorA, colorB)) {
                    any = true;
                }
            }
        }
        return any;
    }

    /**
     * 按顶点位置算彩色描边里这个顶点的颜色。
     *
     * @param pos   顶点世界坐标
     * @param color 基础颜色，只取它的透明度
     * @param phase 相位
     * @return ARGB 颜色
     */
    private static int rainbowColor(Vector3f pos, int color, float phase) {
        double wave = (double) pos.x() * 0.85D + (double) pos.y() * 0.45D + (double) pos.z() * 0.85D;
        float hue = (float) net.minecraft.util.Mth.frac(wave + (double) phase);
        int rgb = net.minecraft.util.Mth.hsvToRgb(hue, 0.9F, 1.0F);
        return color & 0xFF000000 | rgb & 0xFFFFFF;
    }

    /**
     * 彩色描边的流动相位。
     *
     * @return 0~1 之间的相位
     */
    private static float rainbowPhase() {
        return (net.minecraft.Util.getMillis() % RAINBOW_PERIOD) / (float) RAINBOW_PERIOD;
    }

    /**
     * 把一个世界坐标点变换到镜头空间。
     *
     * @param cameraMatrix 相机矩阵
     * @param x            世界坐标 X
     * @param y            世界坐标 Y
     * @param z            世界坐标 Z
     * @return 镜头空间坐标
     */
    private static Vector3f toVector3(Matrix4f cameraMatrix, double x, double y, double z) {
        Vector4f transformed = cameraMatrix.transform(new Vector4f((float) x, (float) y, (float) z, 1.0F));
        return new Vector3f(transformed.x(), transformed.y(), transformed.z());
    }

    /**
     * 写出一条棱对应的方棱柱。
     *
     * @param builder   顶点缓冲
     * @param a         起点（镜头空间）
     * @param b         终点（镜头空间）
     * @param halfWidth 半宽
     * @param colorA    起点颜色
     * @param colorB    终点颜色
     * @return 是否写入成功
     */
    private static boolean appendEdge(BufferBuilder builder, Vector3f a, Vector3f b, double halfWidth,
                                      int colorA, int colorB) {
        Vector3f dir = new Vector3f(b).sub(a);
        if (dir.lengthSquared() < 1.0E-6F) {
            return false;
        }
        dir.normalize();
        Vector3f reference = Math.abs(dir.y()) > 0.99F ? new Vector3f(1.0F, 0.0F, 0.0F) : new Vector3f(0.0F, 1.0F, 0.0F);
        Vector3f u = new Vector3f(dir).cross(reference);
        if (u.lengthSquared() < 1.0E-6F) {
            return false;
        }
        u.normalize().mul((float) halfWidth);
        Vector3f v = new Vector3f(dir).cross(u).normalize().mul((float) halfWidth);

        Vector3f extend = new Vector3f(dir).mul((float) halfWidth);
        Vector3f start = new Vector3f(a).sub(extend);
        Vector3f end = new Vector3f(b).add(extend);

        Vector3f u1 = new Vector3f(u);
        Vector3f v1 = new Vector3f(v);
        Vector3f[] startRing = new Vector3f[4];
        Vector3f[] endRing = new Vector3f[4];
        for (int i = 0; i < 4; i++) {
            float su = (i == 0 || i == 1) ? 1.0F : -1.0F;
            float sv = (i == 1 || i == 2) ? -1.0F : 1.0F;
            Vector3f offset = new Vector3f(u1).mul(su).add(new Vector3f(v1).mul(sv));
            startRing[i] = new Vector3f(start).add(offset);
            endRing[i] = new Vector3f(end).add(offset);
        }
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            addQuad(builder, startRing[i], endRing[i], endRing[j], startRing[j],
                    colorA, colorB, colorB, colorA);
        }
        addQuad(builder, startRing[0], startRing[1], startRing[2], startRing[3],
                colorA, colorA, colorA, colorA);
        addQuad(builder, endRing[3], endRing[2], endRing[1], endRing[0],
                colorB, colorB, colorB, colorB);
        return true;
    }

    /**
     * 写一个四边形，四个顶点各带自己的颜色。
     *
     * @param builder 顶点缓冲
     * @param p0      第一个顶点
     * @param p1      第二个顶点
     * @param p2      第三个顶点
     * @param p3      第四个顶点
     * @param c0      第一个顶点的颜色
     * @param c1      第二个顶点的颜色
     * @param c2      第三个顶点的颜色
     * @param c3      第四个顶点的颜色
     */
    private static void addQuad(BufferBuilder builder, Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3,
                                int c0, int c1, int c2, int c3) {
        builder.addVertex(p0.x(), p0.y(), p0.z()).setColor(c0);
        builder.addVertex(p1.x(), p1.y(), p1.z()).setColor(c1);
        builder.addVertex(p2.x(), p2.y(), p2.z()).setColor(c2);
        builder.addVertex(p3.x(), p3.y(), p3.z()).setColor(c3);
    }

    /**
     * 判断某个位置此刻应该画成什么颜色。
     *
     * @param outline  描边配置
     * @param mode     模式
     * @param progress 淡入进度
     * @return ARGB 颜色
     */
    private static int colorFor(BlockOutline.Outline outline, BlockOutline.Mode mode, float progress) {
        int rgb = outline.rgb() & 0xFFFFFF;
        float alpha = switch (mode) {
            case GLOW -> 0.85F;
            case HIGHLIGHT -> 0.7F;
            case BLACK -> 0.8F;
            case CREATE -> 0.95F;
            case WIRE -> 1.0F;
        };
        alpha *= progress;
        if (mode == BlockOutline.Mode.CREATE) {
            alpha *= progress * progress;
        }
        int alphaInt = net.minecraft.util.Mth.clamp((int) (alpha * 255.0F), 0, 255);
        return alphaInt << 24 | rgb;
    }

    /**
     * 计算这个描边在屏幕上的半宽（方块单位）。
     *
     * @param outline      描边配置
     * @param mode         模式
     * @param progress     淡入进度
     * @param pos          方块位置
     * @param cameraMatrix 相机矩阵
     * @param projection   投影矩阵
     * @return 半宽
     */
    private static double pixelWidth(BlockOutline.Outline outline, BlockOutline.Mode mode, float progress,
                                     BlockPos pos, Matrix4f cameraMatrix, Matrix4f projection) {
        float m11 = projection.m11();
        if (m11 <= 0.0F) {
            return 0.0D;
        }
        Vector3f center = toVector3(cameraMatrix, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        double depth = -center.z;
        if (depth < 0.05D) {
            return 0.0D;
        }
        double pixelsPerBlock = Minecraft.getInstance().getWindow().getHeight() * 0.5D * m11 / depth;
        if (pixelsPerBlock <= 0.0D) {
            return 0.0D;
        }
        double thickness = outline.thickness();
        if (mode == BlockOutline.Mode.CREATE) {
            thickness *= progress;
        }
        double pixels = thickness * THICKNESS_PIXELS_PER_UNIT;
        pixels = net.minecraft.util.Mth.clamp(pixels, MIN_PIXEL_WIDTH, MAX_PIXEL_WIDTH);
        return pixels / pixelsPerBlock * 0.5D;
    }

    /**
     * 更新所有位置的淡入进度与“保持显示”的计时。
     *
     * @param deltaSeconds 距上一帧的秒数
     */
    private static void updateFade(float deltaSeconds) {
        Map<BlockPos, BlockOutline.Outline> current = BlockOutline.clientOutlines();
        float stepIn = deltaSeconds / FADE_IN_SECONDS;
        float stepOut = deltaSeconds / FADE_OUT_SECONDS;

        Set<BlockPos> known = new HashSet<>(FADE.keySet());
        known.addAll(current.keySet());
        known.addAll(LAST_OUTLINE.keySet());

        List<BlockPos> gone = new ArrayList<>();

        for (BlockPos pos : known) {
            BlockOutline.Outline now = current.get(pos);
            if (now == null) {
                float progress = FADE.getOrDefault(pos, 1.0F) - stepOut;
                if (progress <= 0.0F) {
                    FADE.remove(pos);
                    LAST_OUTLINE.remove(pos);
                    HOLD_LEFT.remove(pos);
                    DONE.remove(pos);
                    LAST_STAMP.remove(pos);
                } else {
                    FADE.put(pos, progress);
                }
                continue;
            }

            LAST_OUTLINE.put(pos, now);
            if (isBlockGone(pos)) {
                gone.add(pos);
                continue;
            }
            long stamp = BlockOutline.stamp(pos);
            Long previousStamp = LAST_STAMP.put(pos, stamp);
            boolean restarted = previousStamp == null || previousStamp != stamp;
            if (restarted) {
                FADE.put(pos, 0.0F);
                HOLD_LEFT.put(pos, now.holdSeconds());
                DONE.remove(pos);
            }
            if (DONE.contains(pos)) {
                continue;
            }

            float progress = FADE.getOrDefault(pos, 0.0F);
            boolean pulse = now.mode() == BlockOutline.Mode.CREATE && !now.keepVisible();
            if (progress < 1.0F) {
                FADE.put(pos, Math.min(1.0F, progress + stepIn));
                continue;
            }
            if (!pulse) {
                continue;
            }
            double left = HOLD_LEFT.getOrDefault(pos, now.holdSeconds()) - deltaSeconds;
            if (left > 0.0D) {
                HOLD_LEFT.put(pos, left);
                continue;
            }
            progress -= stepOut;
            if (progress <= 0.0F) {
                FADE.remove(pos);
                HOLD_LEFT.remove(pos);
                DONE.add(pos);
            } else {
                FADE.put(pos, progress);
            }
        }

        for (BlockPos pos : gone) {
            BlockOutline.putLocal(pos, null);
        }
    }

    /**
     * 判断某个坐标上的方块是否已经不见了。
     *
     * @param pos 方块坐标
     * @return 方块确实已经变成空气时返回 {@code true}
     */
    private static boolean isBlockGone(BlockPos pos) {
        Level level = Minecraft.getInstance().level;
        if (level == null || !level.hasChunkAt(pos)) {
            return false;
        }
        return level.getBlockState(pos).isAir();
    }

    /**
     * 取当前要画的“位置 → 描边”快照。
     *
     * @return 位置到描边的映射
     */
    private static Map<BlockPos, BlockOutline.Outline> snapshotFade() {
        Map<BlockPos, BlockOutline.Outline> result = new LinkedHashMap<>();
        for (BlockPos pos : FADE.keySet()) {
            BlockOutline.Outline outline = LAST_OUTLINE.get(pos);
            if (outline != null) {
                result.put(pos, outline);
            }
        }
        return result;
    }

    /**
     * 计算距上一帧的秒数。
     *
     * @return 秒数
     */
    private static float deltaSeconds() {
        long now = System.nanoTime();
        if (lastNanos == 0L) {
            lastNanos = now;
            return 0.0F;
        }
        float delta = (now - lastNanos) / 1.0E9F;
        lastNanos = now;
        return Math.min(delta, 0.25F);
    }

    /**
     * 复位客户端缓存。
     */
    public static void reset() {
        FADE.clear();
        LAST_OUTLINE.clear();
        HOLD_LEFT.clear();
        DONE.clear();
        LAST_STAMP.clear();
        lastLevel = null;
        lastNanos = 0L;
    }
}
