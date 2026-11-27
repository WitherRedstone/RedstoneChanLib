package com.chinaex123.redstone_chan_lib.client.outline;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import com.chinaex123.redstone_chan_lib.api.outline.BlockOutline;
import com.chinaex123.redstone_chan_lib.api.outline.BlockScanOutline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 扫描高亮的客户端渲染。
 * <p>
 * 按扫描配置定期扫描玩家附近的方块，记录匹配目标方块的描边，
 * 并在半透明方块渲染之后统一绘制高亮：
 * 配置变化、玩家移动超过阈值或到达重扫间隔时触发重新扫描。
 */
@EventBusSubscriber(modid = RedstoneChanLIB.MODID, value = Dist.CLIENT)
public final class BlockScanRenderer {

    /** 多少 tick 重扫一次 */
    private static final int RESCAN_TICKS = 10;

    /** 玩家移动超过这个距离（的平方）就重扫 */
    private static final double RESCAN_DISTANCE_SQR = 16.0D;

    /** 当前扫出来的方块 */
    private static Map<BlockPos, BlockOutline.Outline> boxes = Map.of();

    /** 上次扫描的中心 */
    @Nullable
    private static BlockPos lastCenter;

    /** 距离上次扫描过了多少 tick */
    private static int tickCounter;

    /** 上次扫描用的配置，配置变了要立刻重扫 */
    @Nullable
    private static BlockScanOutline.Scan lastScan;

    /** 上次扫描用的目标 */
    @Nullable
    private static BlockScanOutline.Target lastTarget;

    private BlockScanRenderer() {
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
        BlockScanOutline.Scan scan = BlockScanOutline.get(player.level());
        BlockScanOutline.Target target = BlockScanOutline.clientTarget();
        if (scan == null || target == null || target.isEmpty()) {
            if (!boxes.isEmpty()) {
                boxes = Map.of();
            }
            lastScan = null;
            lastTarget = null;
            lastCenter = null;
            return;
        }
        BlockPos center = player.blockPosition();
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
     * 扫描指定范围内的方块。
     * <p>
     * 遍历以中心为基准的立方体范围，跳过未加载区块，
     * 将匹配目标的方块连同描边配置记入映射。
     *
     * @param level  维度
     * @param center 中心
     * @param target 扫描目标
     * @param scan   扫描配置
     * @return 坐标到描边的映射
     */
    private static Map<BlockPos, BlockOutline.Outline> scan(Level level, BlockPos center,
                                                            BlockScanOutline.Target target,
                                                            BlockScanOutline.Scan scan) {
        int radius = scan.radius();
        Map<BlockPos, BlockOutline.Outline> found = new HashMap<>();
        BlockPos min = center.offset(-radius, -radius, -radius);
        BlockPos max = center.offset(radius, radius, radius);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (target.matches(state)) {
                BlockOutline.Outline outline = new BlockOutline.Outline(scan.mode(),
                        BlockScanOutline.colorOf(scan, state), scan.style(), scan.thickness(),
                        scan.throughWalls(), false, 0.0D);
                found.put(pos.immutable(), outline);
            }
        }
        return Map.copyOf(found);
    }

    /**
     * 关卡渲染阶段画扫描高亮。
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
        BlockScanOutline.Scan scan = lastScan;
        if (boxes.isEmpty() || scan == null) {
            return;
        }
        BlockOutlineRenderer.renderBoxes(event, boxes, Map.of());
    }

    /**
     * 清空扫描缓存（退出世界时用）。
     */
    private static void reset() {
        boxes = Map.of();
        lastCenter = null;
        lastScan = null;
        lastTarget = null;
        tickCounter = 0;
    }
}