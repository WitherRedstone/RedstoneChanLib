package com.chinaex123.redstone_chan_lib.client.outline;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import com.chinaex123.redstone_chan_lib.api.outline.EntityScanOutline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;

/**
 * 实体扫描高亮的客户端渲染。
 * <p>
 * 按扫描配置定期扫描玩家附近匹配目标的实体，记录其包围盒，
 * 并在半透明方块渲染之后统一绘制线框高亮：
 * 配置变化、玩家移动超过阈值或到达重扫间隔时触发重新扫描。
 */
@EventBusSubscriber(modid = RedstoneChanLIB.MODID, value = Dist.CLIENT)
public final class EntityScanRenderer {

    /** 多少 tick 重扫一次 */
    private static final int RESCAN_TICKS = 10;

    /** 玩家移动超过这个距离（的平方）就重扫 */
    private static final double RESCAN_DISTANCE_SQR = 16.0D;

    /** 线框离实体表面稍微外扩一点，避免和实体本身打架 */
    private static final double BOX_INFLATE = 0.002D;

    /** 当前扫出来的实体包围盒 */
    private static List<AABB> boxes = List.of();

    /** 上次扫描的中心 */
    @Nullable
    private static net.minecraft.core.BlockPos lastCenter;

    /** 距离上次扫描过了多少 tick */
    private static int tickCounter;

    /** 上次扫描用的配置，配置变了要立刻重扫 */
    @Nullable
    private static EntityScanOutline.Scan lastScan;

    /** 上次扫描用的目标 */
    @Nullable
    private static EntityScanOutline.Target lastTarget;

    private EntityScanRenderer() {
    }

    /**
     * 客户端每 tick 检查一次是否需要重扫。
     * <p>
     * 玩家或世界不可用时重置；无扫描配置或目标时清空缓存；
     * 配置变化、玩家移动超过阈值或到达重扫间隔时重新扫描。
     *
     * @param event 客户端 tick 事件
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            reset();
            return;
        }
        EntityScanOutline.Scan scan = EntityScanOutline.get(player.level());
        EntityScanOutline.Target target = EntityScanOutline.clientTarget();
        if (scan == null || target == null || target.isEmpty()) {
            if (!boxes.isEmpty()) {
                boxes = List.of();
            }
            lastScan = null;
            lastTarget = null;
            lastCenter = null;
            return;
        }
        net.minecraft.core.BlockPos center = player.blockPosition();
        tickCounter++;
        boolean configChanged = !scan.equals(lastScan) || !target.equals(lastTarget);
        boolean moved = lastCenter == null || lastCenter.distSqr(center) > RESCAN_DISTANCE_SQR;
        if (!configChanged && !moved && tickCounter < RESCAN_TICKS) {
            return;
        }
        tickCounter = 0;
        lastScan = scan;
        lastTarget = target;
        lastCenter = center;
        boxes = scan(player.level(), center, target, scan);
    }

    /**
     * 扫描指定范围内的实体。
     * <p>
     * 在以中心为基准的立方体范围内查找匹配目标的实体，
     * 记录其略微外扩的包围盒。
     *
     * @param level  维度
     * @param center 中心
     * @param target 扫描目标
     * @param scan   扫描配置
     * @return 命中实体的包围盒
     */
    private static List<AABB> scan(Level level, net.minecraft.core.BlockPos center,
                                   EntityScanOutline.Target target, EntityScanOutline.Scan scan) {
        int radius = scan.radius();
        AABB area = new AABB(center).inflate(radius);
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, area, entity -> target.matches(entity));
        List<AABB> found = new ArrayList<>(entities.size());
        for (Entity entity : entities) {
            found.add(entity.getBoundingBox().inflate(BOX_INFLATE));
        }
        return List.copyOf(found);
    }

    /**
     * 关卡渲染阶段画实体扫描线框。
     * <p>
     * 仅在半透明方块渲染之后绘制，缓存为空或无扫描配置时跳过。
     *
     * @param event 关卡渲染事件
     */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        EntityScanOutline.Scan scan = lastScan;
        if (boxes.isEmpty() || scan == null) {
            return;
        }
        BlockOutlineRenderer.renderWireBoxes(event, boxes, scan.rgb(), scan.throughWalls(), scan.thickness());
    }

    /**
     * 清空扫描缓存（退出世界时用）。
     */
    private static void reset() {
        boxes = List.of();
        lastCenter = null;
        lastScan = null;
        lastTarget = null;
        tickCounter = 0;
    }
}