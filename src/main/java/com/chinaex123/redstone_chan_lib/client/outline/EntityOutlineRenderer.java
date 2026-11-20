package com.chinaex123.redstone_chan_lib.client.outline;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import com.chinaex123.redstone_chan_lib.api.outline.EntityOutline;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

/**
 * 实体描边渲染器。
 * <p>
 * 在活体实体渲染前为其绘制描边：先将实体模型渲染为纯色剪影写入遮罩，
 * 再把剪影扩散成光晕叠回画面，本体随后照常渲染覆盖内侧。
 * 不允许穿透方块的描边会先做可见性判断，地形或其他实体遮挡时整只不画。
 */
@EventBusSubscriber(modid = RedstoneChanLIB.MODID, value = Dist.CLIENT)
public final class EntityOutlineRenderer {

    /** 是否正在绘制描边，防止剪影渲染时递归触发本事件。 */
    private static boolean rendering;

    /**
     * 处理活体实体渲染前事件。
     * <p>
     * 无描边或实体不可见时跳过；不允许穿透时先做可见性判断；
     * 随后在防护标记下绘制描边，出现异常时记录日志。
     *
     * @param event 活体实体渲染前事件
     */
    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        if (rendering) {
            return;
        }

        LivingEntity entity = event.getEntity();
        EntityOutline.Outline outline = EntityOutline.get(entity);
        if (outline == null || entity.isInvisible()) {
            return;
        }
        if (!outline.throughWalls() && !isVisible(entity)) {
            return;
        }

        rendering = true;
        try {
            drawOutline(event, outline);
        } catch (RuntimeException exception) {
            RedstoneChanLIB.LOGGER.error("绘制生物描边时出错", exception);
        } finally {
            rendering = false;
        }
    }

    /**
     * 绘制描边。
     * <p>
     * 清空遮罩后把实体模型渲染为纯色剪影，再把剪影扩散成光晕合成回主画面。
     *
     * @param event   活体实体渲染前事件
     * @param outline 描边配置
     */
    private static void drawOutline(RenderLivingEvent.Pre<?, ?> event, EntityOutline.Outline outline) {
        LivingEntity entity = event.getEntity();
        ResourceLocation texture = textureOf(event, entity);

        // 清空遮罩，把这只生物的模型画成白色剪影
        GlowOutlineTarget.begin();
        float[] oldColor = RenderSystem.getShaderColor().clone();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        GlowOutlineTarget.MaskSource source = GlowOutlineTarget.newMaskSource(texture, baseRenderTypes(texture));
        renderLiving(event, entity, event.getPartialTick(), source);
        source.flush();
        RenderSystem.setShaderColor(oldColor[0], oldColor[1], oldColor[2], oldColor[3]);

        GlowOutlineTarget.composite(entity, outline);
    }

    /**
     * 取这只生物自己的模型贴图。遮罩用它来丢弃完全透明的像素。
     *
     * @param event  渲染事件
     * @param entity 目标生物
     * @return 模型贴图位置
     */
    @SuppressWarnings("rawtypes")
    private static ResourceLocation textureOf(RenderLivingEvent.Pre<?, ?> event, LivingEntity entity) {
        return ((EntityRenderer) event.getRenderer()).getTextureLocation(entity);
    }

    /**
     * 列出这只生物“本体皮肤”可能用到的渲染类型。
     * <p>
     * 同一个生物模型除了本体那一层，还会叠透明层、附加发光层与眼睛层，
     * 这里只放行用本体贴图的几种基础类型，其余一律丢弃，等于“只描外形”。
     *
     * @param texture 模型贴图
     * @return 允许进入遮罩的渲染类型集合
     */
    private static Set<RenderType> baseRenderTypes(ResourceLocation texture) {
        return Set.of(
                RenderType.entitySolid(texture),
                RenderType.entityCutout(texture),
                RenderType.entityCutoutNoCull(texture),
                RenderType.entityCutoutNoCullZOffset(texture),
                RenderType.entityTranslucent(texture),
                RenderType.entityTranslucentCull(texture));
    }

    /**
     * 将实体模型渲染到指定缓冲区。
     *
     * @param event      活体实体渲染前事件
     * @param entity     实体
     * @param partialTick 插值刻
     * @param source     目标缓冲区
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void renderLiving(RenderLivingEvent.Pre<?, ?> event, LivingEntity entity, float partialTick,
                                     MultiBufferSource source) {
        LivingEntityRenderer renderer = event.getRenderer();
        float yaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        renderer.render(entity, yaw, partialTick, event.getPoseStack(), source, event.getPackedLight());
    }

    /**
     * 判断实体对本地玩家是否可见。
     * <p>
     * 先检测地形遮挡，再检测其他可拾取实体的碰撞箱遮挡；
     * 客户端玩家或世界不可用时视为可见。
     *
     * @param entity 目标实体
     * @return 可见返回 true
     */
    private static boolean isVisible(LivingEntity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return true;
        }
        Vec3 eye = minecraft.player.getEyePosition();
        AABB box = entity.getBoundingBox();
        Vec3 target = box.getCenter();

        // 先查方块
        BlockHitResult blockHit = minecraft.level.clip(
                new ClipContext(eye, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraft.player));
        if (blockHit.getType() != HitResult.Type.MISS) {
            return false;
        }

        // 再查其它生物
        double distance = eye.distanceTo(target);
        for (Entity other : minecraft.level.getEntities(entity, box.inflate(96.0D))) {
            if (other == minecraft.player || other == entity || other.isSpectator()) {
                continue;
            }
            if (!other.isPickable()) {
                continue;
            }
            Optional<Vec3> hit = other.getBoundingBox().inflate(0.1D).clip(eye, target);
            if (hit.isPresent() && eye.distanceTo(hit.get()) < distance - 0.2D) {
                return false;
            }
        }
        return true;
    }
}
