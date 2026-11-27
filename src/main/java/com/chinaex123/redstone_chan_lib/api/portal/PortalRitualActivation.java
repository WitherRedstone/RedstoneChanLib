package com.chinaex123.redstone_chan_lib.api.portal;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import com.chinaex123.redstone_chan_lib.util.PortalTexts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.*;

/**
 * 传送门仪式式激活。
 * <p>
 * 处理「往流体空位里丢物品」触发的仪式式点亮：
 * 校验物品媒介、静态源流体、空位矩形尺寸与四周框架环，
 * 成功时打雷预警、将空位填充为传送门并播放特效，最后消耗一个物品。
 */
public final class PortalRitualActivation {

    /** 泛洪搜索的流体格子数量上限。 */
    private static final int MAX_POOL_CELLS = 1024;

    /** 传送门定义。 */
    private final PortalDefinition definition;

    /** 仪式配置。 */
    private final PortalRitual ritual;

    /** 传送门形状。 */
    private final PortalShape shape;

    /**
     * 私有构造函数。
     *
     * @param definition 传送门定义
     * @param ritual     仪式配置
     * @param shape      传送门形状
     */
    private PortalRitualActivation(PortalDefinition definition, PortalRitual ritual, PortalShape shape) {
        this.definition = definition;
        this.ritual = ritual;
        this.shape = shape;
    }

    /**
     * 尝试用掉落的物品触发仪式式激活。
     * <p>
     * 无仪式式定义时直接返回；否则遍历所有定义，
     * 对物品媒介匹配的仪式尝试在其所在流体位置激活。
     *
     * @param level      服务端世界
     * @param itemEntity 掉落物实体
     */
    public static void tryActivate(ServerLevel level, ItemEntity itemEntity) {
        // 没有仪式式定义时直接返回，省掉后面的遍历
        if (!PortalDefinitions.hasRituals()) {
            return;
        }
        ItemStack stack = itemEntity.getItem();
        if (stack.isEmpty()) {
            return;
        }

        // 物品可能浮在流体表面，也可能已经沉进去了
        BlockPos itemPos = itemEntity.blockPosition();
        List<BlockPos> candidates = new ArrayList<>(1);
        candidates.add(itemPos);

        for (PortalDefinition definition : PortalDefinitions.all()) {
            PortalRitual ritual = definition.ritual();
            if (ritual == null || !ritual.matchesItem(stack)) {
                continue;
            }
            for (BlockPos candidate : candidates) {
                if (tryActivateAt(level, itemEntity, definition, ritual, candidate)) {
                    return;
                }
            }
        }
    }

    /**
     * 尝试在指定流体位置触发仪式式激活。
     * <p>
     * 要求流体为匹配的静态源流体、物品完全沉入流体、空位恰好为配置尺寸且无空洞、
     * 四周框架环满足要求；成功时打雷、填充传送门、播放特效并消耗一个物品。
     *
     * @param level      服务端世界
     * @param itemEntity 掉落物实体
     * @param definition 传送门定义
     * @param ritual     仪式配置
     * @param fluidPos   流体位置
     * @return 激活成功返回 true
     */
    private static boolean tryActivateAt(ServerLevel level, ItemEntity itemEntity,
                                         PortalDefinition definition, PortalRitual ritual, BlockPos fluidPos) {
        FluidState fluidState = level.getFluidState(fluidPos);
        // 只认静态的源方块，流动的液体不算
        if (!ritual.matchesFluid(fluidState) || !fluidState.isSource()) {
            return false;
        }
        // 物品要整个沉进流体里才算数
        double surface = fluidPos.getY() + fluidState.getHeight(level, fluidPos);
        if (itemEntity.getBoundingBox().maxY > surface - 1.0E-4D) {
            return false;
        }

        Set<BlockPos> pool = flood(level, fluidPos, ritual);
        if (pool == null || pool.isEmpty()) {
            return false;
        }

        // 空位必须正好是配置的矩形大小，多一格少一格都不行
        int uMin = Integer.MAX_VALUE;
        int uMax = Integer.MIN_VALUE;
        int vMin = Integer.MAX_VALUE;
        int vMax = Integer.MIN_VALUE;
        for (BlockPos pos : pool) {
            uMin = Math.min(uMin, pos.getX());
            uMax = Math.max(uMax, pos.getX());
            vMin = Math.min(vMin, pos.getZ());
            vMax = Math.max(vMax, pos.getZ());
        }
        int width = uMax - uMin + 1;
        int depth = vMax - vMin + 1;
        if (width != ritual.width() || depth != ritual.depth()) {
            return false;
        }
        // 防止池子里有空洞
        if (pool.size() != width * depth) {
            return false;
        }

        int y = fluidPos.getY();
        if (!checkFrameRing(level, definition, ritual, y, uMin, uMax, vMin, vMax)) {
            return false;
        }

        // 空位左上角的框架坐标
        BlockPos frameMin = new BlockPos(uMin - 1, y, vMin - 1);
        PortalShape shape = PortalShape.compose(frameMin, Direction.Axis.Y, width, depth);
        BlockPos center = shape.interiorCenter();

        // 先打雷，把玩家的注意力引过来
        strikeLightning(level, center);
        // 再把空位里的流体换成传送门方块
        PortalStructureHelper.fillPortal(level, shape, definition);
        PortalEffects.playActivationEffects(level, center);

        // 消耗一个物品，物品实体空了就移除
        ItemStack stack = itemEntity.getItem();
        stack.shrink(1);
        if (stack.isEmpty()) {
            itemEntity.discard();
        }

        RedstoneChanLIB.LOGGER.debug(PortalTexts.text("ritual_activated", definition.id(), center));
        return true;
    }

    /**
     * 从起始位置水平泛洪收集连通的源流体格子。
     * <p>
     * 仅沿水平方向扩散，超过数量上限时返回 null。
     *
     * @param level  服务端世界
     * @param start  起始位置
     * @param ritual 仪式配置
     * @return 流体格子集合，超出上限时返回 null
     */
    private static Set<BlockPos> flood(ServerLevel level, BlockPos start, PortalRitual ritual) {
        int limit = Math.min(MAX_POOL_CELLS, Math.max(1, ritual.width() * ritual.depth()) + 1);

        Set<BlockPos> pool = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        BlockPos origin = start.immutable();
        pool.add(origin);
        queue.add(origin);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos next = current.relative(direction);
                if (pool.contains(next)) {
                    continue;
                }
                FluidState state = level.getFluidState(next);
                if (!ritual.matchesFluid(state) || !state.isSource()) {
                    continue;
                }
                pool.add(next.immutable());
                if (pool.size() > limit) {
                    return null;
                }
                queue.add(next.immutable());
            }
        }
        return pool;
    }

    /**
     * 检查空位四周的框架环。
     * <p>
     * 环上每个位置要求底部方块可支撑、上方方块属于传送门框架；
     * 若仪式要求全部框架类型，则每种框架方块与标签都必须出现在环上。
     *
     * @param level      服务端世界
     * @param definition 传送门定义
     * @param ritual     仪式配置
     * @param y          高度
     * @param uMin       X 方向最小坐标
     * @param uMax       X 方向最大坐标
     * @param vMin       Z 方向最小坐标
     * @param vMax       Z 方向最大坐标
     * @return 框架环满足要求返回 true
     */
    private static boolean checkFrameRing(ServerLevel level, PortalDefinition definition, PortalRitual ritual,
                                          int y, int uMin, int uMax, int vMin, int vMax) {
        List<BlockState> ringStates = new ArrayList<>();
        for (int x = uMin - 1; x <= uMax + 1; x++) {
            for (int z = vMin - 1; z <= vMax + 1; z++) {
                boolean edgeX = x == uMin - 1 || x == uMax + 1;
                boolean edgeZ = z == vMin - 1 || z == vMax + 1;
                if (!edgeX && !edgeZ) {
                    continue;
                }
                if (edgeX && edgeZ) {
                    continue;
                }
                BlockPos ringPos = new BlockPos(x, y, z);
                BlockState base = level.getBlockState(ringPos);
                if (!base.isFaceSturdy(level, ringPos, Direction.UP)) {
                    return false;
                }
                BlockState above = level.getBlockState(ringPos.above());
                if (!definition.isFrame(above)) {
                    return false;
                }
                ringStates.add(above);
            }
        }

        if (!ritual.requireAllFrameTypes()) {
            return true;
        }

        for (Block block : definition.frameBlocks()) {
            boolean found = false;
            for (BlockState state : ringStates) {
                if (state.is(block)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        for (TagKey<Block> tag : definition.frameTags()) {
            boolean found = false;
            for (BlockState state : ringStates) {
                if (state.is(tag)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    /**
     * 在指定中心上方生成闪电。
     *
     * @param level  服务端世界
     * @param center 中心位置
     */
    private static void strikeLightning(ServerLevel level, BlockPos center) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) {
            return;
        }
        bolt.moveTo(center.getX() + 0.5D, center.getY() + 1.0D, center.getZ() + 0.5D);
        level.addFreshEntity(bolt);
    }

    /**
     * 获取传送门定义。
     *
     * @return 传送门定义
     */
    public PortalDefinition definition() {
        return this.definition;
    }

    /**
     * 获取仪式配置。
     *
     * @return 仪式配置
     */
    public PortalRitual ritual() {
        return this.ritual;
    }

    /**
     * 获取传送门形状。
     *
     * @return 传送门形状
     */
    public PortalShape shape() {
        return this.shape;
    }
}
