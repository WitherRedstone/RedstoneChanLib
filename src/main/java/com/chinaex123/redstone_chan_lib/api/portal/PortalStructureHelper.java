package com.chinaex123.redstone_chan_lib.api.portal;

import com.chinaex123.redstone_chan_lib.block.PortalBlockEntity;
import com.chinaex123.redstone_chan_lib.block.CustomPortal;
import com.chinaex123.redstone_chan_lib.init.RCLBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 传送门结构操作工具类。
 * <p>
 * 提供传送门结构的填充、搭建、拆除与可建造性检查：
 * 将内部空间填充为传送门方块并写入定义，
 * 按尺寸搭建完整框架与内部，拆除连通的传送门，
 * 以及判断目标区域是否为空旷可建造。
 */
public final class PortalStructureHelper {

    private PortalStructureHelper() {}

    /**
     * 将传送门内部空间填充为传送门方块。
     * <p>
     * 按形状的平面法线轴设置方块状态，并为每个传送门方块写入定义。
     *
     * @param level 世界
     * @param shape 传送门形状
     * @param def   传送门定义
     */
    public static void fillPortal(Level level, PortalShape shape, PortalDefinition def) {
        BlockState portalState = RCLBlocks.CUSTOM_PORTAL.get().defaultBlockState()
                .setValue(CustomPortal.AXIS, shape.axis());
        for (BlockPos pos : shape.interiorPositions()) {
            level.setBlock(pos, portalState, Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof PortalBlockEntity blockEntity) {
                blockEntity.applyDefinition(def);
            }
        }
    }

    /**
     * 搭建完整传送门。
     * <p>
     * 以给定位置为最小角，按内部尺寸外扩一圈绘制框架，
     * 内部填充传送门方块并写入定义。
     *
     * @param level  世界
     * @param min    框架最小角
     * @param axis   平面法线轴
     * @param width  内部宽度
     * @param height 内部高度
     * @param def    传送门定义
     */
    public static void buildPortal(Level level, BlockPos min, Direction.Axis axis, int width, int height, PortalDefinition def) {
        BlockState frameState = def.getBuildFrameBlock().defaultBlockState();
        BlockState portalState = RCLBlocks.CUSTOM_PORTAL.get().defaultBlockState()
                .setValue(CustomPortal.AXIS, axis);

        // 边框：外侧尺寸比内部多 2
        int outerWidth = width + 2;
        int outerHeight = height + 2;
        for (int dv = 0; dv < outerHeight; dv++) {
            for (int du = 0; du < outerWidth; du++) {
                boolean border = dv == 0 || dv == outerHeight - 1 || du == 0 || du == outerWidth - 1;
                BlockPos pos = planePos(min, axis, du, dv);
                if (border) {
                    level.setBlock(pos, frameState, Block.UPDATE_ALL);
                } else {
                    level.setBlock(pos, portalState, Block.UPDATE_ALL);
                    if (level.getBlockEntity(pos) instanceof PortalBlockEntity blockEntity) {
                        blockEntity.applyDefinition(def);
                    }
                }
            }
        }
    }

    /**
     * 拆除与起始位置连通的传送门方块。
     *
     * @param level 世界
     * @param start 起始位置
     */
    public static void destroyConnectedPortal(Level level, BlockPos start) {
        List<BlockPos> blocks = PortalShape.connectedPortalBlocks(level, start, RCLBlocks.CUSTOM_PORTAL.get());
        for (BlockPos pos : blocks) {
            level.destroyBlock(pos, false, null, 512);
        }
    }

    /**
     * 判断目标区域是否适合建造传送门。
     * <p>
     * 检查框架与内部覆盖的全部位置是否均为空气或可替换方块。
     *
     * @param level  世界
     * @param min    框架最小角
     * @param axis   平面法线轴
     * @param width  内部宽度
     * @param height 内部高度
     * @return 适合建造返回 true
     */
    public static boolean isAreaSuitable(Level level, BlockPos min, Direction.Axis axis, int width, int height) {
        int outerWidth = width + 2;
        int outerHeight = height + 2;
        for (int dv = 0; dv < outerHeight; dv++) {
            for (int du = 0; du < outerWidth; du++) {
                BlockPos pos = planePos(min, axis, du, dv);
                BlockState state = level.getBlockState(pos);
                if (!state.isAir() && !state.canBeReplaced()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 根据平面法线轴将平面坐标转换为世界坐标。
     * <p>
     * X 轴平面用 (u, v) 对应 (Z, Y)，Z 轴平面用 (u, v) 对应 (X, Y)，
     * Y 轴平面用 (u, v) 对应 (X, Z)。
     *
     * @param min  框架最小角
     * @param axis 平面法线轴
     * @param u    平面内第一坐标
     * @param v    平面内第二坐标
     * @return 世界坐标
     */
    public static BlockPos planePos(BlockPos min, Direction.Axis axis, int u, int v) {
        return switch (axis) {
            case X -> min.offset(0, v, u);
            case Z -> min.offset(u, v, 0);
            case Y -> min.offset(u, 0, v);
        };
    }
}
