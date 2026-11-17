package com.chinaex123.redstone_chan_lib.client.portal;

import com.chinaex123.redstone_chan_lib.init.RCLBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 传送门客户端事件。
 * <p>
 * 每刻检测本地玩家是否处于传送门方块内，若是则标记客户端传送状态；
 * 同时推进客户端状态计时，并提供重置方法。
 */
public final class PortalClientEvents {

    private PortalClientEvents() {}

    /**
     * 每刻更新。
     * <p>
     * 若本地玩家处于传送门方块内则标记状态，随后推进客户端状态计时。
     */
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        Level level = minecraft.level;

        if (player != null && level != null && isInsidePortal(player, level)) {
            PortalClientState.markInsidePortal();
        }
        PortalClientState.tick();
    }

    /**
     * 判断玩家是否处于传送门方块内。
     * <p>
     * 将玩家碰撞箱稍微外扩，遍历覆盖范围内的方块，
     * 若为传送门方块且其形状与碰撞箱相交则判定为处于门内。
     *
     * @param player 本地玩家
     * @param level  客户端世界
     * @return 处于传送门内返回 true
     */
    private static boolean isInsidePortal(LocalPlayer player, Level level) {
        // 稍微外扩一点，避免玩家贴着门框边缘时判定丢失
        AABB box = player.getBoundingBox().inflate(0.15D);
        BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
        BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(RCLBlocks.CUSTOM_PORTAL.get())) {
                continue;
            }
            VoxelShape shape = state.getShape(level, pos);
            if (!shape.isEmpty() && shape.bounds().move(pos).intersects(box)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 重置客户端传送状态。
     */
    public static void reset() {
        PortalClientState.reset();
    }
}
