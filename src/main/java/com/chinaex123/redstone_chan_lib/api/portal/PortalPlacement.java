package com.chinaex123.redstone_chan_lib.api.portal;

import com.chinaex123.redstone_chan_lib.block.PortalBlockEntity;
import com.chinaex123.redstone_chan_lib.init.RCLBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 传送门定位与建造工具类。
 * <p>
 * 负责在目标维度为传送旅行寻找或创建传送门：
 * 先按维度比例换算落点，再在附近搜索同定义的已有传送门；
 * 找不到时在落点上下与周边逐圈搜索可建造位置并搭建框架，
 * 实在无处可建时在落点高度强制建门，保证玩家不会卡住。
 */
public final class PortalPlacement {

    /** 寻找已有传送门的水平搜索半径 */
    private static final int SEARCH_RADIUS = 24;

    /** 寻找已有传送门的竖直搜索半径 */
    private static final int SEARCH_HEIGHT = 16;

    /** 落点附近建不了门时，向外扩大搜索的水平半径 */
    private static final int HORIZONTAL_SEARCH_RADIUS = 8;

    private PortalPlacement() {}

    /**
     * 寻找或创建传送门（不指定朝向）。
     *
     * @param target    目标维度
     * @param source    来源维度
     * @param def       传送门定义
     * @param sourcePos 来源位置
     * @return 传送门到达位置
     */
    public static BlockPos findOrCreatePortal(ServerLevel target, ServerLevel source, PortalDefinition def, BlockPos sourcePos) {
        return findOrCreatePortal(target, source, def, sourcePos, null);
    }

    /**
     * 寻找或创建传送门。
     * <p>
     * 先按维度比例换算落点，再查找附近同定义的已有传送门；
     * 找不到时在落点附近创建新的传送门。
     *
     * @param target    目标维度
     * @param source    来源维度
     * @param def       传送门定义
     * @param sourcePos 来源位置
     * @param axis      期望的平面法线轴，可为 null
     * @return 传送门到达位置
     */
    public static BlockPos findOrCreatePortal(ServerLevel target, ServerLevel source, PortalDefinition def,
                                              BlockPos sourcePos, @Nullable Direction.Axis axis) {
        BlockPos base = scalePosition(source, target, sourcePos);

        BlockPos existing = findExistingPortal(target, base, def);
        if (existing != null) {
            return existing;
        }
        return createPortal(target, base, def, axis);
    }

    /**
     * 按维度比例换算落点坐标。
     * <p>
     * 进入下界时水平坐标除以 8，离开下界时乘以 8；
     * 高度限制在目标维度的可建造范围内。
     *
     * @param source 来源维度
     * @param target 目标维度
     * @param pos    来源位置
     * @return 换算后的落点
     */
    public static BlockPos scalePosition(ServerLevel source, ServerLevel target, BlockPos pos) {
        double x = pos.getX();
        double z = pos.getZ();
        if (target.dimension() == Level.NETHER && source.dimension() != Level.NETHER) {
            x /= 8.0D;
            z /= 8.0D;
        } else if (source.dimension() == Level.NETHER && target.dimension() != Level.NETHER) {
            x *= 8.0D;
            z *= 8.0D;
        }
        int y = Math.clamp(pos.getY(), target.getMinBuildHeight() + 2, target.getMaxBuildHeight() - 6);
        return BlockPos.containing(x, y, z);
    }

    /**
     * 在指定范围内查找同定义的已有传送门。
     * <p>
     * 遍历搜索范围内的所有位置，找到属于指定定义的传送门方块后，
     * 返回其内部中心位置。
     *
     * @param level 目标维度
     * @param base  基准位置
     * @param def   传送门定义
     * @return 内部中心位置，未找到时返回 null
     */
    @Nullable
    public static BlockPos findExistingPortal(ServerLevel level, BlockPos base, PortalDefinition def) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                for (int dy = -SEARCH_HEIGHT; dy <= SEARCH_HEIGHT; dy++) {
                    cursor.set(base.getX() + dx, base.getY() + dy, base.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    if (!state.is(RCLBlocks.CUSTOM_PORTAL.get())) continue;
                    if (!(level.getBlockEntity(cursor) instanceof PortalBlockEntity blockEntity)) continue;
                    PortalDefinition owner = blockEntity.getDefinition();
                    if (owner == null || !owner.id().equals(def.id())) continue;
                    return interiorCenter(level, cursor.immutable());
                }
            }
        }
        return null;
    }

    /**
     * 计算传送门内部中心位置。
     * <p>
     * 收集连通的传送门方块，取其水平范围的中点与最低高度。
     *
     * @param level 目标维度
     * @param start 起始传送门方块位置
     * @return 内部中心位置
     */
    public static BlockPos interiorCenter(ServerLevel level, BlockPos start) {
        List<BlockPos> blocks = PortalShape.connectedPortalBlocks(level, start, RCLBlocks.CUSTOM_PORTAL.get());
        if (blocks.isEmpty()) {
            return start;
        }
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : blocks) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        return new BlockPos((minX + maxX) / 2, minY, (minZ + maxZ) / 2);
    }

    /**
     * 在指定落点创建传送门（不指定朝向）。
     *
     * @param level 目标维度
     * @param base  落点
     * @param def   传送门定义
     * @return 传送门到达位置
     */
    public static BlockPos createPortal(ServerLevel level, BlockPos base, PortalDefinition def) {
        return createPortal(level, base, def, null);
    }

    /**
     * 在指定落点创建传送门。
     * <p>
     * 先按落点竖直方向上下寻找可建造位置，再逐圈向外扩大范围；
     * 均找不到时在落点高度强制建门，宁可悬空也不把玩家卡住。
     *
     * @param level 目标维度
     * @param base  落点
     * @param def   传送门定义
     * @param axis  期望的平面法线轴，可为 null
     * @return 传送门到达位置
     */
    public static BlockPos createPortal(ServerLevel level, BlockPos base, PortalDefinition def, @Nullable Direction.Axis axis) {
        int width = Math.max(2, def.minWidth());
        int height = Math.max(3, def.minHeight());
        Direction.Axis preferred = chooseAxis(def, axis);
        int verticalExtent = preferred == Direction.Axis.Y ? 1 : height;

        int minY = level.getMinBuildHeight() + 2;
        int maxY = level.getMaxBuildHeight() - verticalExtent - 3;

        // 在落点所在竖直方向上向下找地面，找不到再向上找
        BlockPos spot = searchVertically(level, base.getX(), base.getZ(), base.getY(), width, height, def, preferred);
        if (spot != null) {
            return spot;
        }

        // 落点可能正好在山体 / 岩浆里，这时按圈向外扩大范围继续找，
        for (int radius = 1; radius <= HORIZONTAL_SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    // 只遍历当前这一圈的边缘，内部早已在上一圈找过
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    spot = searchVertically(level, base.getX() + dx, base.getZ() + dz, base.getY(), width, height, def, preferred);
                    if (spot != null) {
                        return spot;
                    }
                }
            }
        }

        // 实在找不到就把门硬建在落点高度上，宁可悬空也不把玩家卡住
        int y = Math.clamp(minY, base.getY(), maxY);
        BlockPos frameMin;
        BlockPos arrival;
        if (preferred == Direction.Axis.Y) {
            // 横门以落点为中心向四周展开
            int shiftU = (width - 1) / 2;
            int shiftV = (height - 1) / 2;
            frameMin = new BlockPos(base.getX() - 1 - shiftU, y, base.getZ() - 1 - shiftV);
            arrival = new BlockPos(base.getX(), y, base.getZ());
        } else {
            frameMin = new BlockPos(base.getX(), y, base.getZ());
            // 框架沿 X 轴展开，内部左边界为 frameMin.x + 1，中心再偏移半个宽度
            arrival = new BlockPos(frameMin.getX() + 1 + (width - 1) / 2, frameMin.getY() + 1, frameMin.getZ());
        }
        PortalStructureHelper.buildPortal(level, frameMin, preferred, width, height, def);
        return arrival;
    }

    /**
     * 选择传送门的平面法线轴。
     * <p>
     * 优先使用允许的期望轴；定义只接受横门时使用竖直轴，否则使用 Z 轴。
     *
     * @param def  传送门定义
     * @param axis 期望的平面法线轴
     * @return 选定的平面法线轴
     */
    private static Direction.Axis chooseAxis(PortalDefinition def, @Nullable Direction.Axis axis) {
        if (axis != null && def.allowsAxis(axis)) {
            return axis;
        }
        // 定义只接受横门时，新建的门也必须是横门
        if (def.orientation() == PortalOrientation.HORIZONTAL) {
            return Direction.Axis.Y;
        }
        return Direction.Axis.Z;
    }

    /**
     * 沿竖直方向上下搜索可建造位置。
     * <p>
     * 从起始高度开始，依次尝试向下与向上各 24 格，
     * 返回第一个可成功建造的位置。
     *
     * @param level   目标维度
     * @param x       X 坐标
     * @param z       Z 坐标
     * @param startY  起始高度
     * @param width   内部宽度
     * @param height  内部高度
     * @param def     传送门定义
     * @param axis    平面法线轴
     * @return 可建造位置，未找到时返回 null
     */
    @Nullable
    private static BlockPos searchVertically(ServerLevel level, int x, int z, int startY, int width, int height,
                                             PortalDefinition def, Direction.Axis axis) {
        int verticalExtent = axis == Direction.Axis.Y ? 1 : height;
        int minY = level.getMinBuildHeight() + 2;
        int maxY = level.getMaxBuildHeight() - verticalExtent - 3;
        for (int step = 0; step <= 24; step++) {
            for (int sign = 0; sign <= 1; sign++) {
                if (step == 0 && sign == 1) {
                    continue;
                }
                int y = startY + (sign == 0 ? -step : step);
                if (y < minY || y > maxY) {
                    continue;
                }
                BlockPos spot = tryBuildAt(level, x, y, z, width, height, def, axis);
                if (spot != null) {
                    return spot;
                }
            }
        }
        return null;
    }

    /**
     * 尝试在指定位置建造传送门。
     * <p>
     * 检查地面支撑与空间是否合适，满足条件则搭建框架并返回到达位置。
     *
     * @param level  目标维度
     * @param x      X 坐标
     * @param y      Y 坐标
     * @param z      Z 坐标
     * @param width  内部宽度
     * @param height 内部高度
     * @param def    传送门定义
     * @param axis   平面法线轴
     * @return 到达位置，无法建造时返回 null
     */
    @Nullable
    private static BlockPos tryBuildAt(ServerLevel level, int x, int y, int z, int width, int height,
                                       PortalDefinition def, Direction.Axis axis) {
        BlockPos base = new BlockPos(x, y, z);
        // 让玩家落点尽量靠近传送门中间，宽度为 2 时不做偏移
        int shift = (width - 1) / 2;

        if (axis == Direction.Axis.Y) {
            // 横门：以落点为中心向四周摊开，玩家直接站在门中间
            int shiftV = (height - 1) / 2;
            BlockPos frameMin = base.offset(-1 - shift, 0, -1 - shiftV);
            if (!hasGroundFlat(level, frameMin, width, height)) {
                return null;
            }
            if (!PortalStructureHelper.isAreaSuitable(level, frameMin, axis, width, height)) {
                return null;
            }
            PortalStructureHelper.buildPortal(level, frameMin, axis, width, height, def);
            return base;
        }

        BlockPos frameMin = axis == Direction.Axis.Z
                ? base.offset(-1 - shift, 0, 0)
                : base.offset(0, 0, -1 - shift);
        if (!hasGround(level, frameMin, axis, width, height)) {
            return null;
        }
        if (!PortalStructureHelper.isAreaSuitable(level, frameMin, axis, width, height)) {
            return null;
        }
        PortalStructureHelper.buildPortal(level, frameMin, axis, width, height, def);
        // 回传内部底部的中心格，宽度为偶数时取偏左（偏负方向）的一格
        return new BlockPos(base.getX(), base.getY() + 1, base.getZ());
    }

    /**
     * 检查竖门底部是否有连续地面支撑。
     *
     * @param level  目标维度
     * @param min    框架最小角
     * @param axis   平面法线轴
     * @param width  内部宽度
     * @param height 内部高度
     * @return 地面完整返回 true
     */
    private static boolean hasGround(ServerLevel level, BlockPos min, Direction.Axis axis, int width, int height) {
        int outerWidth = width + 2;
        BlockPos below = min.below();
        for (int du = 0; du < outerWidth; du++) {
            BlockPos pos = axis == Direction.Axis.X ? below.offset(0, 0, du) : below.offset(du, 0, 0);
            BlockState state = level.getBlockState(pos);
            if (!state.isFaceSturdy(level, pos, Direction.UP)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查横门底部是否有完整的平面地面支撑。
     *
     * @param level  目标维度
     * @param min    框架最小角
     * @param width  内部宽度
     * @param height 内部高度
     * @return 地面完整返回 true
     */
    private static boolean hasGroundFlat(ServerLevel level, BlockPos min, int width, int height) {
        int outerWidth = width + 2;
        int outerHeight = height + 2;
        BlockPos below = min.below();
        for (int dv = 0; dv < outerHeight; dv++) {
            for (int du = 0; du < outerWidth; du++) {
                BlockPos pos = below.offset(du, 0, dv);
                BlockState state = level.getBlockState(pos);
                if (!state.isFaceSturdy(level, pos, Direction.UP)) {
                    return false;
                }
            }
        }
        return true;
    }
}
