package com.chinaex123.redstone_chan_lib.api.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 传送门传送逻辑工具类。
 * <p>
 * 负责解析玩家经传送门抵达的目标维度与位置，并执行实际传送：
 * 目标维度与当前维度相同时自动切换到主世界或下界，
 * 再由传送门定位逻辑寻找或创建落点；传送前清除动量与坠落距离，
 * 传送后播放到达特效。
 */
public final class PortalTeleport {

    private PortalTeleport() {
    }

    /**
     * 传送目的地。
     *
     * @param level 目标维度
     * @param pos   目标位置
     */
    public record Destination(ServerLevel level, BlockPos pos) {
    }

    /**
     * 解析传送目的地（不指定朝向）。
     *
     * @param player     玩家
     * @param definition 传送门定义
     * @return 传送目的地，无法解析时返回 null
     */
    @Nullable
    public static Destination resolve(ServerPlayer player, PortalDefinition definition) {
        return resolve(player, definition, null);
    }

    /**
     * 解析传送目的地。
     * <p>
     * 若目标维度与玩家当前维度相同，则自动切换到主世界或下界；
     * 目标维度不可用时返回 null，否则寻找或创建传送门作为落点。
     *
     * @param player     玩家
     * @param definition 传送门定义
     * @param axis       期望的平面法线轴，可为 null
     * @return 传送目的地，无法解析时返回 null
     */
    @Nullable
    public static Destination resolve(ServerPlayer player, PortalDefinition definition,
                                      @Nullable Direction.Axis axis) {
        ServerLevel sourceLevel = player.serverLevel();
        ResourceKey<Level> dimension = definition.dimension();
        if (dimension.equals(sourceLevel.dimension())) {
            dimension = sourceLevel.dimension().equals(Level.OVERWORLD) ? Level.NETHER : Level.OVERWORLD;
        }
        ServerLevel targetLevel = player.server.getLevel(dimension);
        if (targetLevel == null) {
            return null;
        }
        BlockPos target = PortalPlacement.findOrCreatePortal(targetLevel, sourceLevel, definition,
                player.blockPosition(), axis);
        return new Destination(targetLevel, target);
    }

    /**
     * 将玩家传送到目的地。
     * <p>
     * 传送前清掉速度与坠落距离，避免玩家带着冲进门时的动量被弹飞；
     * 落点站在传送门内部底部方块上方，稍微抬高一点避免卡进框架。
     *
     * @param player      玩家
     * @param destination 传送目的地
     */
    public static void move(ServerPlayer player, Destination destination) {
        // 传送前清掉速度与坠落距离，避免玩家带着冲进门时的动量被弹飞
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        // 站在传送门内部底部的方块上，稍微抬高一点避免卡进框架
        BlockPos target = destination.pos();
        player.teleportTo(destination.level(), target.getX() + 0.5D, target.getY() + 0.05D, target.getZ() + 0.5D,
                player.getYRot(), player.getXRot());
        PortalEffects.playArrivalEffects(player, target);
    }

    /**
     * 执行传送。
     * <p>
     * 解析目的地，成功后传送玩家并返回 true。
     *
     * @param player     玩家
     * @param definition 传送门定义
     * @return 传送成功返回 true
     */
    public static boolean teleport(ServerPlayer player, PortalDefinition definition) {
        Destination destination = resolve(player, definition);
        if (destination == null) {
            return false;
        }
        move(player, destination);
        return true;
    }
}
