package com.chinaex123.redstone_chan_lib.api.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 传送门形状。
 * <p>
 * 表示传送门在某一平面上的矩形结构，包含框架最小/最大角、平面法线轴、
 * 内部宽度与高度。提供从框架方块寻找并校验形状、按尺寸直接构造、
 * 收集连通的传送门方块、框架位置判定以及内部格与中心查询等方法。
 */
public final class PortalShape {

    /** 单次洪水填充允许收集的最大内部格数，防止异常结构导致卡顿 */
    private static final int MAX_INTERIOR_CELLS = 4096;
    /** 单次搜索允许收集的最大传送门方块数量 */
    private static final int MAX_PORTAL_BLOCKS = 1024;

    /** 依次尝试的三种传送门平面：先竖门，最后才是平铺的横门 */
    private static final Direction.Axis[] AXES = {Direction.Axis.X, Direction.Axis.Z, Direction.Axis.Y};

    /** 框架最小角。 */
    private final BlockPos min;
    /** 框架最大角。 */
    private final BlockPos max;
    /** 平面法线轴。 */
    private final Direction.Axis axis;
    /** 内部宽度。 */
    private final int width;
    /** 内部高度。 */
    private final int height;

    /**
     * 私有构造函数。
     *
     * @param min    框架最小角
     * @param max    框架最大角
     * @param axis   平面法线轴
     * @param width  内部宽度
     * @param height 内部高度
     */
    private PortalShape(BlockPos min, BlockPos max, Direction.Axis axis, int width, int height) {
        this.min = min;
        this.max = max;
        this.axis = axis;
        this.width = width;
        this.height = height;
    }

    /**
     * 按内部尺寸直接构造形状。
     * <p>
     * 以给定点为框架最小角，向外扩展内部宽度与高度各加一的偏移得到最大角。
     *
     * @param min    框架最小角
     * @param axis   平面法线轴
     * @param width  内部宽度
     * @param height 内部高度
     * @return 传送门形状
     */
    public static PortalShape compose(BlockPos min, Direction.Axis axis, int width, int height) {
        BlockPos max = inPlane(axis, min, uOf(axis, min) + width + 1, vOf(axis, min) + height + 1);
        return new PortalShape(min, max, axis, width, height);
    }

    /**
     * 从框架方块寻找并校验传送门形状。
     * <p>
     * 依次尝试三种平面朝向，跳过定义不允许的朝向，返回首个命中的形状。
     *
     * @param level    世界
     * @param framePos 框架方块位置
     * @param def      传送门定义
     * @return 传送门形状，未找到时返回 null
     */
    @Nullable
    public static PortalShape find(Level level, BlockPos framePos, PortalDefinition def) {
        if (!def.isFrame(level.getBlockState(framePos))) {
            return null;
        }
        // 平面朝向未知，三种都试一次，先命中的生效
        for (Direction.Axis axis : AXES) {
            // 定义可以限制只允许竖门或只允许横门
            if (!def.allowsAxis(axis)) {
                continue;
            }
            PortalShape shape = tryFind(level, framePos, def, axis);
            if (shape != null) {
                return shape;
            }
        }
        return null;
    }

    /**
     * 在指定平面朝向上尝试寻找形状。
     * <p>
     * 以框架方块平面内周围 8 格为候选起点，对空气区域做泛洪并校验，
     * 同一空气区域只处理一次。
     *
     * @param level    世界
     * @param framePos 框架方块位置
     * @param def      传送门定义
     * @param axis     平面法线轴
     * @return 传送门形状，未找到时返回 null
     */
    @Nullable
    private static PortalShape tryFind(Level level, BlockPos framePos, PortalDefinition def, Direction.Axis axis) {
        // 把平面内周围 8 格都当成候选起点
        Set<BlockPos> tried = new HashSet<>();
        for (int du = -1; du <= 1; du++) {
            for (int dv = -1; dv <= 1; dv++) {
                if (du == 0 && dv == 0) {
                    continue;
                }
                BlockPos start = stepV(axis, stepU(axis, framePos, du), dv);
                if (tried.contains(start)) {
                    continue;
                }
                if (!isInteriorSpace(level.getBlockState(start))) {
                    continue;
                }

                Set<BlockPos> region = floodInterior(level, start, axis, def);
                if (region == null || region.isEmpty()) {
                    continue;
                }
                // 同一个空气区域只处理一次
                tried.addAll(region);

                PortalShape shape = validate(level, framePos, def, axis, region);
                if (shape != null) {
                    return shape;
                }
            }
        }
        return null;
    }

    /**
     * 在平面内泛洪收集连通的空气区域。
     * <p>
     * 仅沿平面内的两个方向扩散，超过定义允许的最大面积时视为框架敞开并返回 null。
     *
     * @param level 世界
     * @param start 起始位置
     * @param axis  平面法线轴
     * @param def   传送门定义
     * @return 空气区域集合，超出限制时返回 null
     */
    @Nullable
    private static Set<BlockPos> floodInterior(Level level, BlockPos start, Direction.Axis axis, PortalDefinition def) {
        // 任何合法矩形的面积都不会超过定义允许的最大尺寸，因此可以提前收敛
        int limit = Math.clamp((long) def.maxWidth() * def.maxHeight(), 1, MAX_INTERIOR_CELLS);

        Set<BlockPos> region = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        BlockPos origin = start.immutable();
        region.add(origin);
        queue.add(origin);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (int sign = -1; sign <= 1; sign += 2) {
                for (int along = 0; along <= 1; along++) {
                    BlockPos next = along == 0
                            ? stepU(axis, current, sign)
                            : stepV(axis, current, sign);
                    if (region.contains(next)) {
                        continue;
                    }
                    if (!isInteriorSpace(level.getBlockState(next))) {
                        continue;
                    }
                    region.add(next.immutable());
                    if (region.size() > limit) {
                        // 说明框架是敞开的，空气漏到了外面
                        return null;
                    }
                    queue.add(next.immutable());
                }
            }
        }
        return region;
    }

    /**
     * 校验空气区域能否构成合法传送门。
     * <p>
     * 要求区域为完整矩形、尺寸在定义允许范围内，且外圈四条边均为框架方块；
     * 四个角允许留空。
     *
     * @param level    世界
     * @param framePos 框架方块位置
     * @param def      传送门定义
     * @param axis     平面法线轴
     * @param region   空气区域
     * @return 传送门形状，校验失败时返回 null
     */
    @Nullable
    private static PortalShape validate(Level level, BlockPos framePos, PortalDefinition def,
                                        Direction.Axis axis, Set<BlockPos> region) {
        int uMin = Integer.MAX_VALUE, uMax = Integer.MIN_VALUE;
        int vMin = Integer.MAX_VALUE, vMax = Integer.MIN_VALUE;
        for (BlockPos pos : region) {
            int u = uOf(axis, pos);
            int v = vOf(axis, pos);
            uMin = Math.min(uMin, u);
            uMax = Math.max(uMax, u);
            vMin = Math.min(vMin, v);
            vMax = Math.max(vMax, v);
        }

        int width = uMax - uMin + 1;
        int height = vMax - vMin + 1;
        // 区域格数必须正好等于矩形面积，否则内部有缺口或形状不规则
        if (region.size() != width * height) {
            return null;
        }
        if (!def.isSizeAllowed(width, height)) {
            return null;
        }

        // 外圈检查：四条边必须是框架方块，四个角允许留空
        for (int v = vMin - 1; v <= vMax + 1; v++) {
            for (int u = uMin - 1; u <= uMax + 1; u++) {
                boolean edgeU = u == uMin - 1 || u == uMax + 1;
                boolean edgeV = v == vMin - 1 || v == vMax + 1;
                if (!edgeU && !edgeV) {
                    continue;
                }
                if (edgeU && edgeV) {
                    continue;
                }
                BlockPos pos = inPlane(axis, framePos, u, v);
                if (!def.isFrame(level.getBlockState(pos))) {
                    return null;
                }
            }
        }

        BlockPos min = inPlane(axis, framePos, uMin - 1, vMin - 1);
        BlockPos max = inPlane(axis, framePos, uMax + 1, vMax + 1);
        return new PortalShape(min, max, axis, width, height);
    }

    /**
     * 根据平面法线轴将平面坐标转换为世界坐标。
     * <p>
     * X 轴平面用 (v, u) 对应 (Y, Z)，Z 轴平面用 (u, v) 对应 (X, Y)，
     * Y 轴平面用 (u, v) 对应 (X, Z)。
     *
     * @param axis   平面法线轴
     * @param anchor 锚点
     * @param u      平面内第一坐标
     * @param v      平面内第二坐标
     * @return 世界坐标
     */
    private static BlockPos inPlane(Direction.Axis axis, BlockPos anchor, int u, int v) {
        return switch (axis) {
            case X -> new BlockPos(anchor.getX(), v, u);
            case Z -> new BlockPos(u, v, anchor.getZ());
            case Y -> new BlockPos(u, anchor.getY(), v);
        };
    }

    /**
     * 获取位置在平面内第一坐标上的值。
     *
     * @param axis 平面法线轴
     * @param pos  位置
     * @return 第一坐标值
     */
    private static int uOf(Direction.Axis axis, BlockPos pos) {
        return axis == Direction.Axis.X ? pos.getZ() : pos.getX();
    }

    /**
     * 获取位置在平面内第二坐标上的值。
     *
     * @param axis 平面法线轴
     * @param pos  位置
     * @return 第二坐标值
     */
    private static int vOf(Direction.Axis axis, BlockPos pos) {
        return axis == Direction.Axis.Y ? pos.getZ() : pos.getY();
    }

    /**
     * 沿平面内第一坐标方向移动。
     *
     * @param axis   平面法线轴
     * @param pos    位置
     * @param amount 移动量
     * @return 移动后的位置
     */
    private static BlockPos stepU(Direction.Axis axis, BlockPos pos, int amount) {
        return switch (axis) {
            case X -> pos.offset(0, 0, amount);
            case Z, Y -> pos.offset(amount, 0, 0);
        };
    }

    /**
     * 沿平面内第二坐标方向移动。
     *
     * @param axis   平面法线轴
     * @param pos    位置
     * @param amount 移动量
     * @return 移动后的位置
     */
    private static BlockPos stepV(Direction.Axis axis, BlockPos pos, int amount) {
        return switch (axis) {
            case X, Z -> pos.offset(0, amount, 0);
            case Y -> pos.offset(0, 0, amount);
        };
    }

    /**
     * 判断方块状态是否可作为传送门内部空间。
     *
     * @param state 方块状态
     * @return 为空气或可被替换时返回 true
     */
    private static boolean isInteriorSpace(BlockState state) {
        return state.isAir() || state.canBeReplaced();
    }

    /**
     * 收集与起始位置连通的指定传送门方块。
     * <p>
     * 沿六个方向广度优先搜索，最多收集限定数量。
     *
     * @param level 方块获取器
     * @param start 起始位置
     * @param block 传送门方块
     * @return 连通的传送门方块列表
     */
    public static List<BlockPos> connectedPortalBlocks(BlockGetter level, BlockPos start, net.minecraft.world.level.block.Block block) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        BlockPos origin = start.immutable();
        queue.add(origin);
        visited.add(origin);

        while (!queue.isEmpty() && result.size() <= MAX_PORTAL_BLOCKS) {
            BlockPos current = queue.poll();
            if (!level.getBlockState(current).is(block)) continue;
            result.add(current);
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (visited.contains(next)) continue;
                visited.add(next.immutable());
                queue.add(next.immutable());
            }
        }
        return result;
    }

    /**
     * 判断某位置是否为传送门的框架位置。
     * <p>
     * 收集连通的传送门方块得到范围，要求位置与门方块处于同一平面层，
     * 且落在矩形四条边（不含四角）上。
     *
     * @param level      方块获取器
     * @param pos        待判断位置
     * @param portalPos  传送门方块位置
     * @param axis       平面法线轴
     * @param portalBlock 传送门方块
     * @return 是框架位置返回 true
     */
    public static boolean isFramePosition(BlockGetter level, BlockPos pos, BlockPos portalPos,
                                          Direction.Axis axis, net.minecraft.world.level.block.Block portalBlock) {
        List<BlockPos> blocks = connectedPortalBlocks(level, portalPos, portalBlock);
        if (blocks.isEmpty()) {
            return false;
        }

        int uMin = Integer.MAX_VALUE, uMax = Integer.MIN_VALUE;
        int vMin = Integer.MAX_VALUE, vMax = Integer.MIN_VALUE;
        for (BlockPos block : blocks) {
            int u = uOf(axis, block);
            int v = vOf(axis, block);
            uMin = Math.min(uMin, u);
            uMax = Math.max(uMax, u);
            vMin = Math.min(vMin, v);
            vMax = Math.max(vMax, v);
        }

        // 框架必须和门方块处在同一层平面上
        if (fixedOf(axis, pos) != fixedOf(axis, portalPos)) {
            return false;
        }

        int u = uOf(axis, pos);
        int v = vOf(axis, pos);
        boolean edgeU = u == uMin - 1 || u == uMax + 1;
        boolean edgeV = v == vMin - 1 || v == vMax + 1;
        // 落在四条边上才算框架；四个角可以留空，所以角上的方块不参与判定
        return (edgeU || edgeV) && !(edgeU && edgeV);
    }

    /**
     * 获取位置在平面法线轴上的固定坐标。
     *
     * @param axis 平面法线轴
     * @param pos  位置
     * @return 固定坐标值
     */
    private static int fixedOf(Direction.Axis axis, BlockPos pos) {
        return switch (axis) {
            case X -> pos.getX();
            case Z -> pos.getZ();
            case Y -> pos.getY();
        };
    }

    /**
     * 获取框架最小角。
     *
     * @return 框架最小角
     */
    public BlockPos min() {
        return this.min;
    }

    /**
     * 获取框架最大角。
     *
     * @return 框架最大角
     */
    public BlockPos max() {
        return this.max;
    }

    /**
     * 获取平面法线轴。
     *
     * @return 平面法线轴
     */
    public Direction.Axis axis() {
        return this.axis;
    }

    /**
     * 判断是否为横门（平铺在地面）。
     *
     * @return 横门返回 true
     */
    public boolean isHorizontal() {
        return this.axis == Direction.Axis.Y;
    }

    /**
     * 获取内部宽度。
     *
     * @return 内部宽度
     */
    public int width() {
        return this.width;
    }

    /**
     * 获取内部高度。
     *
     * @return 内部高度
     */
    public int height() {
        return this.height;
    }

    /**
     * 获取内部区域在第一坐标方向上的最小值。
     *
     * @return 内部第一坐标最小值
     */
    private int interiorUMin() {
        return uOf(this.axis, this.min) + 1;
    }

    /**
     * 获取内部区域在第一坐标方向上的最大值。
     *
     * @return 内部第一坐标最大值
     */
    private int interiorUMax() {
        return uOf(this.axis, this.max) - 1;
    }

    /**
     * 获取内部区域在第二坐标方向上的最小值。
     *
     * @return 内部第二坐标最小值
     */
    private int interiorVMin() {
        return vOf(this.axis, this.min) + 1;
    }

    /**
     * 获取内部区域在第二坐标方向上的最大值。
     *
     * @return 内部第二坐标最大值
     */
    private int interiorVMax() {
        return vOf(this.axis, this.max) - 1;
    }

    /**
     * 获取内部中心位置。
     * <p>
     * 横门取内部高度方向的中点，竖门取内部底部。
     *
     * @return 内部中心位置
     */
    public BlockPos interiorCenter() {
        int u = (interiorUMin() + interiorUMax()) / 2;
        int v = isHorizontal() ? (interiorVMin() + interiorVMax()) / 2 : interiorVMin();
        return inPlane(this.axis, this.min, u, v);
    }

    /**
     * 获取内部区域的最小角位置。
     *
     * @return 内部最小角位置
     */
    public BlockPos interiorMin() {
        return inPlane(this.axis, this.min, interiorUMin(), interiorVMin());
    }

    /**
     * 获取内部区域的全部位置。
     *
     * @return 内部位置列表
     */
    public List<BlockPos> interiorPositions() {
        List<BlockPos> positions = new ArrayList<>();
        for (int v = interiorVMin(); v <= interiorVMax(); v++) {
            for (int u = interiorUMin(); u <= interiorUMax(); u++) {
                positions.add(inPlane(this.axis, this.min, u, v));
            }
        }
        return positions;
    }

    /**
     * 判断位置是否位于内部区域。
     * <p>
     * 要求位置在框架所在平面层，且平面内坐标严格位于框架内外边界之间。
     *
     * @param pos 位置
     * @return 位于内部返回 true
     */
    public boolean isInside(BlockPos pos) {
        // 先确认方块就在这层平面上
        switch (this.axis) {
            case X -> {
                if (pos.getX() != this.min.getX()) {
                    return false;
                }
            }
            case Z -> {
                if (pos.getZ() != this.min.getZ()) {
                    return false;
                }
            }
            case Y -> {
                if (pos.getY() != this.min.getY()) {
                    return false;
                }
            }
        }
        return uOf(this.axis, pos) > uOf(this.axis, this.min) && uOf(this.axis, pos) < uOf(this.axis, this.max)
                && vOf(this.axis, pos) > vOf(this.axis, this.min) && vOf(this.axis, pos) < vOf(this.axis, this.max);
    }
}
