package com.chinaex123.redstone_chan_lib.client.outline;

import com.chinaex123.redstone_chan_lib.api.outline.EntityOutline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.util.Set;

/**
 * 光晕描边渲染目标。
 * <p>
 * 通过离屏遮罩实现实体描边：先把实体模型渲染为纯色剪影写入遮罩贴图，
 * 再以屏幕空间的多圈偏移采样把剪影扩散成外淡内实的光晕叠回主画面。
 * 光晕半径按描边粗细直接换算为固定像素，远近观感一致；
 * 遮罩尺寸随窗口变化自动调整，贴图环绕方式固定为夹到边缘。
 */
public final class GlowOutlineTarget {

    /** 每一圈取多少个方向上的偏移样本。 */
    private static final int RING_SAMPLES = 8;
    /** 三层由外到内的圆环，半径依次收缩，透明度依次提高，叠出“外淡内实”的光晕。 */
    private static final double[] RING_SCALES = {1.0D, 0.68D, 0.36D};
    /** 各圆环的基础透明度。 */
    private static final float[] RING_ALPHAS = {0.22F, 0.30F, 0.42F};
    /** 黑色描边要压暗周围。 */
    private static final float BLACK_ALPHA_FACTOR = 0.55F;
    /** 粗细换算系数：一格粗细对应多少屏幕像素，固定值，不随距离、视角变化。 */
    private static final double PIXELS_PER_THICKNESS_UNIT = 100.0D;
    /** 光晕最细的像素。 */
    private static final double MIN_RADIUS_PIXELS = 3.0D;
    /** 光晕最粗的像素，防止把 thickness 调得太大时糊满屏幕。 */
    private static final double MAX_RADIUS_PIXELS = 32.0D;

    /** 剪影渲染用的共享顶点缓冲。 */
    private static final ByteBufferBuilder BUFFER = new ByteBufferBuilder(786432);

    /** 遮罩渲染目标。 */
    private static RenderTarget mask;
    /** 按模型贴图缓存的遮罩渲染类型：不同生物用的是不同贴图。 */
    private static final java.util.Map<ResourceLocation, RenderType> MASK_TYPES = new java.util.HashMap<>();

    /** 遮罩宽度，-1 表示未初始化。 */
    private static int width = -1;
    /** 遮罩高度，-1 表示未初始化。 */
    private static int height = -1;

    /**
     * 开始一帧描边绘制。
     * <p>
     * 确保遮罩目标存在并清空，然后把绘制目标切回主画面。
     */
    public static void begin() {
        ensureTarget();
        if (mask == null) {
            return;
        }
        mask.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        mask.clear(false);
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
    }

    /**
     * 创建剪影遮罩的缓冲区来源。
     * <p>
     * 按模型贴图选择遮罩渲染类型，并限定允许进入遮罩的渲染类型，
     * 从而滤掉透明层、发光层与眼睛层等非实体主体部分。
     *
     * @param texture      模型贴图
     * @param allowedTypes 允许进入遮罩的渲染类型
     * @return 遮罩缓冲区来源
     */
    public static MaskSource newMaskSource(ResourceLocation texture, Set<RenderType> allowedTypes) {
        ensureTarget();
        return new MaskSource(BUFFER, maskType(texture), allowedTypes);
    }

    /**
     * 将剪影合成为光晕叠回主画面。
     * <p>
     * 计算光晕半径后切换为正交投影，按三层多方向的偏移采样以加法或普通混合
     * 绘制遮罩贴图，彩色风格按屏幕坐标与时间生成彩虹色，最后恢复渲染状态。
     *
     * @param entity  描边实体
     * @param outline 描边配置
     */
    public static void composite(net.minecraft.world.entity.LivingEntity entity, EntityOutline.Outline outline) {
        if (mask == null || width <= 0 || height <= 0) {
            return;
        }

        // 光晕半径按粗细直接换算成屏幕像素，固定不变
        double radiusPixels = radiusPixels(outline);

        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget main = minecraft.getMainRenderTarget();
        main.bindWrite(true);

        Matrix4f oldProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        Matrix4fStack stack = RenderSystem.getModelViewStack();
        stack.pushMatrix();
        stack.identity();
        stack.translate(0.0F, 0.0F, -2000.0F);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(
                new Matrix4f().setOrtho(0.0F, (float) width, (float) height, 0.0F, 1000.0F, 3000.0F),
                VertexSorting.DISTANCE_TO_ORIGIN);

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, mask.getColorTextureId());
        RenderSystem.enableBlend();
        if (outline.mode() == EntityOutline.Mode.BLACK) {
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        } else {
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        }
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        double thicknessPixels = radiusPixels;
        float phase = (Mth.frac((float) (System.currentTimeMillis() % 4000L) / 4000.0F));

        for (int ring = 0; ring < RING_SCALES.length; ring++) {
            double radius = thicknessPixels * RING_SCALES[ring];
            float alpha = RING_ALPHAS[ring];
            if (outline.mode() == EntityOutline.Mode.BLACK) {
                alpha *= BLACK_ALPHA_FACTOR;
            }
            for (int sample = 0; sample < RING_SAMPLES; sample++) {
                double angle = Math.PI * 2.0D * sample / RING_SAMPLES;
                drawOffsetQuad(Math.cos(angle) * radius, Math.sin(angle) * radius, outline, alpha, phase);
            }
        }

        stack.popMatrix();
        RenderSystem.setProjectionMatrix(oldProjection, VertexSorting.DISTANCE_TO_ORIGIN);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    /**
     * 计算光晕半径（像素）。
     * <p>
     * 由描边粗细换算成像素，并限制在最小与最大像素之间。
     *
     * @param outline 描边配置
     * @return 光晕半径（像素）
     */
    private static double radiusPixels(EntityOutline.Outline outline) {
        double pixels = outline.thickness() * PIXELS_PER_THICKNESS_UNIT;
        return Mth.clamp(pixels, MIN_RADIUS_PIXELS, MAX_RADIUS_PIXELS);
    }

    /**
     * 释放描边渲染目标与缓存。
     */
    public static void close() {
        if (mask != null) {
            mask.destroyBuffers();
            mask = null;
        }
        MASK_TYPES.clear();
        width = -1;
        height = -1;
    }

    /**
     * 确保遮罩渲染目标存在且尺寸与窗口一致。
     * <p>
     * 首次创建或尺寸变化时重建/调整目标、清空渲染类型缓存并重新设置贴图环绕方式。
     */
    private static void ensureTarget() {
        Minecraft minecraft = Minecraft.getInstance();
        int windowWidth = Math.max(1, minecraft.getWindow().getWidth());
        int windowHeight = Math.max(1, minecraft.getWindow().getHeight());
        if (mask == null) {
            mask = new TextureTarget(windowWidth, windowHeight, false, false);
            mask.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            width = windowWidth;
            height = windowHeight;
            MASK_TYPES.clear();
            clampMaskTexture();
        } else if (width != windowWidth || height != windowHeight) {
            mask.resize(windowWidth, windowHeight, false);
            width = windowWidth;
            height = windowHeight;
            MASK_TYPES.clear();
            clampMaskTexture();
        }
    }

    /**
     * 把遮罩贴图的环绕方式改成“夹到边缘”。
     * <p>
     * 合成时是按“偏移若干像素”去采样遮罩的，贴到屏幕边缘的那部分会采到贴图外面；
     * 默认的 REPEAT 会把对边的内容卷回来，画面上就会出现一圈莫名其妙的光环。
     */
    private static void clampMaskTexture() {
        GlStateManager._bindTexture(mask.getColorTextureId());
        GlStateManager._texParameter(3553, 10242, 33071);
        GlStateManager._texParameter(3553, 10245, 33071);
    }

    /**
     * 获取或创建指定贴图的遮罩渲染类型。
     * <p>
     * 使用轮廓着色器与模型贴图，关闭透明、深度测试与剔除，仅写入颜色，
     * 并在绘制前后切换渲染目标；结果按贴图缓存。
     *
     * @param texture 模型贴图
     * @return 遮罩渲染类型
     */
    private static RenderType maskType(ResourceLocation texture) {
        RenderType cached = MASK_TYPES.get(texture);
        if (cached != null) {
            return cached;
        }
        RenderStateShard.OutputStateShard output = new RenderStateShard.OutputStateShard(
                "redstone_chan_lib_outline_mask",
                () -> {
                    if (mask != null) {
                        mask.bindWrite(true);
                    }
                },
                () -> Minecraft.getInstance().getMainRenderTarget().bindWrite(true));
        RenderType type = RenderType.create(
                "redstone_chan_lib_outline_mask",
                DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 1536, false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.RENDERTYPE_OUTLINE_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                        .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .setOutputState(output)
                        .createCompositeState(false));
        MASK_TYPES.put(texture, type);
        return type;
    }

    /**
     * 绘制一个带偏移的遮罩四边形。
     * <p>
     * 以屏幕尺寸为基准，按偏移量采样遮罩贴图并绘制铺满屏幕的四边形，
     * 四角颜色依据描边配置计算。
     *
     * @param offsetX 水平偏移（像素）
     * @param offsetY 垂直偏移（像素）
     * @param outline 描边配置
     * @param alpha   透明度
     * @param phase   彩虹色相位
     */
    private static void drawOffsetQuad(double offsetX, double offsetY, EntityOutline.Outline outline,
                                       float alpha, float phase) {
        float u0 = (float) (offsetX / width);
        float v0 = (float) (offsetY / height);
        float u1 = 1.0F + u0;
        float v1 = 1.0F + v0;
        int alphaInt = Mth.clamp((int) (alpha * 255.0F), 0, 255);

        int colorTopLeft = cornerColor(outline, 0.0F, 0.0F, alphaInt, phase);
        int colorTopRight = cornerColor(outline, (float) width, 0.0F, alphaInt, phase);
        int colorBottomRight = cornerColor(outline, (float) width, (float) height, alphaInt, phase);
        int colorBottomLeft = cornerColor(outline, 0.0F, (float) height, alphaInt, phase);

        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        builder.addVertex(0.0F, 0.0F, 0.0F).setUv(u0, v1).setColor(colorTopLeft);
        builder.addVertex((float) width, 0.0F, 0.0F).setUv(u1, v1).setColor(colorTopRight);
        builder.addVertex((float) width, (float) height, 0.0F).setUv(u1, v0).setColor(colorBottomRight);
        builder.addVertex(0.0F, (float) height, 0.0F).setUv(u0, v0).setColor(colorBottomLeft);
        MeshData mesh = builder.buildOrThrow();
        BufferUploader.drawWithShader(mesh);
    }

    /**
     * 计算四边形某个角的颜色。
     * <p>
     * 黑色描边为纯黑；彩色风格按屏幕坐标与相位计算彩虹色；其余使用描边自身颜色。
     *
     * @param outline 描边配置
     * @param screenX 屏幕 X 坐标
     * @param screenY 屏幕 Y 坐标
     * @param alpha   透明度
     * @param phase   彩虹色相位
     * @return ARGB 颜色
     */
    private static int cornerColor(EntityOutline.Outline outline, float screenX, float screenY, int alpha, float phase) {
        int rgb;
        if (outline.mode() == EntityOutline.Mode.BLACK) {
            rgb = 0x000000;
        } else if (outline.style() == EntityOutline.Style.RAINBOW) {
            float hue = Mth.frac(screenX / (float) Math.max(1, width) * 0.75F
                    + screenY / (float) Math.max(1, height) * 0.45F + phase);
            rgb = Mth.hsvToRgb(hue, 0.9F, 1.0F) & 0xFFFFFF;
        } else {
            rgb = outline.rgb() & 0xFFFFFF;
        }
        return alpha << 24 | rgb;
    }

    /**
     * 剪影遮罩缓冲区来源。
     * <p>
     * 仅对实体顶点格式且属于允许类型的渲染提供遮罩顶点消费者，其余返回空消费者；
     * 提供一次性刷新方法。
     */
    public static final class MaskSource implements MultiBufferSource {

        /** 实际的即时缓冲区来源。 */
        private final MultiBufferSource.BufferSource delegate;
        /** 遮罩渲染类型。 */
        private final RenderType type;
        /** 允许进入遮罩的渲染类型，用来滤掉透明层、发光层与眼睛层。 */
        private final java.util.Set<RenderType> allowedTypes;

        /**
         * 构造遮罩缓冲区来源。
         *
         * @param buffer       顶点缓冲
         * @param type         遮罩渲染类型
         * @param allowedTypes 允许进入遮罩的渲染类型
         */
        private MaskSource(ByteBufferBuilder buffer, RenderType type, java.util.Set<RenderType> allowedTypes) {
            this.delegate = MultiBufferSource.immediate(buffer);
            this.type = type;
            this.allowedTypes = allowedTypes;
        }

        /**
         * 获取指定渲染类型的顶点消费者。
         * <p>
         * 非实体顶点格式或不在允许列表中的渲染类型返回空消费者。
         *
         * @param renderType 渲染类型
         * @return 顶点消费者
         */
        @Override
        public @NotNull VertexConsumer getBuffer(RenderType renderType) {
            if (!DefaultVertexFormat.NEW_ENTITY.equals(renderType.format()) || !this.allowedTypes.contains(renderType)) {
                return NoopVertexConsumer.INSTANCE;
            }
            return new MaskVertexConsumer(this.delegate.getBuffer(this.type));
        }

        /**
         * 刷新缓冲区，提交已写入的剪影顶点。
         */
        public void flush() {
            this.delegate.endLastBatch();
        }
    }

    /**
     * 剪影顶点消费者。
     * <p>
     * 保留顶点位置与 UV，颜色一律写成白色：UV 用来让着色器按模型贴图的 alpha 丢弃完全透明的像素，
     * 这样才能只留下外形轮廓；颜色交给合成阶段决定，所以这里固定白色。
     */
    private static final class MaskVertexConsumer implements VertexConsumer {

        /** 实际写入顶点数据的消费者。 */
        private final VertexConsumer delegate;

        /** 当前顶点 X 坐标。 */
        private float x;
        /** 当前顶点 Y 坐标。 */
        private float y;
        /** 当前顶点 Z 坐标。 */
        private float z;
        /** 当前顶点 U 坐标。 */
        private float u;
        /** 当前顶点 V 坐标。 */
        private float v;

        /**
         * 构造剪影顶点消费者。
         *
         * @param delegate 实际消费者
         */
        private MaskVertexConsumer(VertexConsumer delegate) {
            this.delegate = delegate;
        }

        /**
         * 记录当前顶点的位置。
         *
         * @param x X 坐标
         * @param y Y 坐标
         * @param z Z 坐标
         * @return 当前消费者
         */
        @Override
        public @NotNull VertexConsumer addVertex(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        /**
         * 忽略颜色：剪影颜色在合成阶段统一决定，顶点一律写白色。
         *
         * @param red   红色分量
         * @param green 绿色分量
         * @param blue  蓝色分量
         * @param alpha 透明度
         * @return 当前消费者
         */
        @Override
        public @NotNull VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return this;
        }

        /**
         * 记录当前顶点的 UV 坐标，供着色器按贴图 alpha 丢弃透明像素。
         *
         * @param u U 坐标
         * @param v V 坐标
         * @return 当前消费者
         */
        @Override
        public @NotNull VertexConsumer setUv(float u, float v) {
            this.u = u;
            this.v = v;
            return this;
        }

        /**
         * 忽略光照贴图坐标。
         *
         * @param u 光照 U
         * @param v 光照 V
         * @return 当前消费者
         */
        @Override
        public @NotNull VertexConsumer setUv1(int u, int v) {
            return this;
        }

        /**
         * 忽略覆盖层坐标。
         *
         * @param u 覆盖 U
         * @param v 覆盖 V
         * @return 当前消费者
         */
        @Override
        public @NotNull VertexConsumer setUv2(int u, int v) {
            return this;
        }

        /**
         * 设置法线，同时写入一个白色顶点。
         *
         * @param normalX 法线 X
         * @param normalY 法线 Y
         * @param normalZ 法线 Z
         * @return 当前消费者
         */
        @Override
        public @NotNull VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
            this.delegate.addVertex(this.x, this.y, this.z).setUv(this.u, this.v).setColor(0xFFFFFFFF);
            return this;
        }
    }

    /**
     * 空顶点消费者，丢弃所有写入。
     */
    public static final class NoopVertexConsumer implements VertexConsumer {

        /** 单例实例。 */
        public static final NoopVertexConsumer INSTANCE = new NoopVertexConsumer();

        /**
         * 丢弃顶点位置。
         *
         * @param x X 坐标
         * @param y Y 坐标
         * @param z Z 坐标
         * @return 当前消费者
         */
        @Override
        public @NotNull VertexConsumer addVertex(float x, float y, float z) {
            return this;
        }

        /**
         * 丢弃顶点颜色。
         *
         * @param red   红色分量
         * @param green 绿色分量
         * @param blue  蓝色分量
         * @param alpha 透明度
         * @return 当前消费者
         */
        @Override
        public @NotNull VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return this;
        }

        /**
         * 丢弃 UV 坐标。
         *
         * @param u U 坐标
         * @param v V 坐标
         * @return 当前消费者
         */
        @Override
        public @NotNull VertexConsumer setUv(float u, float v) {
            return this;
        }

        /**
         * 丢弃光照贴图坐标。
         *
         * @param u 光照 U
         * @param v 光照 V
         * @return 当前消费者
         */
        @Override
        public @NotNull VertexConsumer setUv1(int u, int v) {
            return this;
        }

        /**
         * 丢弃覆盖层坐标。
         *
         * @param u 覆盖 U
         * @param v 覆盖 V
         * @return 当前消费者
         */
        @Override
        public @NotNull VertexConsumer setUv2(int u, int v) {
            return this;
        }

        /**
         * 丢弃法线。
         *
         * @param normalX 法线 X
         * @param normalY 法线 Y
         * @param normalZ 法线 Z
         * @return 当前消费者
         */
        @Override
        public @NotNull VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
            return this;
        }
    }
}