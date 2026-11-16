package com.chinaex123.redstone_chan_lib.util;

import com.chinaex123.redstone_chan_lib.block.CustomPortal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 跨维度定位 / 传送工具类。
 * <p>
 * 提供在目标维度查找或创建传送门，以及执行玩家跨维度传送的静态方法。
 */
public final class PortalEffectHelper {

    private PortalEffectHelper() {}

    /**
     * 播放传送门激活效果。
     *
     * @param level 所在维度
     * @param pos   传送门位置
     */
    public static void playActivationEffects(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.0f, 1.0f);
        spawnActivationParticles(level, pos);
    }

    /**
     * 生成传送门激活粒子。
     *
     * @param level 所在维度
     * @param pos   传送门位置
     */
    public static void spawnActivationParticles(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        BlockState portalState = level.getBlockState(pos);
        Direction.Axis portalAxis = portalState.hasProperty(CustomPortal.AXIS)
                ? portalState.getValue(CustomPortal.AXIS) : Direction.Axis.Z;

        double centerX = pos.getX() + 0.5;
        double centerY = pos.getY() + 0.5;
        double centerZ = pos.getZ() + 0.5;

        // 向内收缩粒子
        for (int i = 0; i < 50; i++) {
            double startX = centerX + (serverLevel.getRandom().nextDouble() - 0.5) * 6.0;
            double startY = centerY + (serverLevel.getRandom().nextDouble() - 0.5) * 6.0;
            double startZ = centerZ + (serverLevel.getRandom().nextDouble() - 0.5) * 6.0;

            double deltaX = centerX - startX;
            double deltaY = centerY - startY;
            double deltaZ = centerZ - startZ;

            double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
            if (distance > 0) {
                double speedFactor = 0.1 + serverLevel.getRandom().nextDouble() * 0.2;
                deltaX = (deltaX / distance) * speedFactor;
                deltaY = (deltaY / distance) * speedFactor;
                deltaZ = (deltaZ / distance) * speedFactor;
            }

            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    startX, startY, startZ, 1, deltaX, deltaY, deltaZ, 1.0);
        }

        // 漩涡粒子
        for (int i = 0; i < 30; i++) {
            double offsetX, offsetZ;
            if (portalAxis == Direction.Axis.X) {
                offsetX = (serverLevel.getRandom().nextDouble() - 0.5) * 0.8;
                offsetZ = (serverLevel.getRandom().nextDouble() - 0.5) * 2.0;
            } else {
                offsetX = (serverLevel.getRandom().nextDouble() - 0.5) * 2.0;
                offsetZ = (serverLevel.getRandom().nextDouble() - 0.5) * 0.8;
            }

            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    centerX + offsetX,
                    centerY + (serverLevel.getRandom().nextDouble() - 0.5) * 2.0,
                    centerZ + offsetZ,
                    1, 0.0, -0.1, 0.0, 0.5);
        }

        // 中心吸引点
        serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                centerX, centerY, centerZ, 20, 0.3, 0.3, 0.3, 0.0);
    }

    /**
     * 播放到达效果。
     *
     * @param player    到达的玩家
     * @param targetPos 到达位置
     */
    public static void playArrivalEffects(ServerPlayer player, BlockPos targetPos) {
        player.level().playSound(null, targetPos,
                SoundEvents.PORTAL_TRAVEL, SoundSource.BLOCKS, 1.0f, 1.2f);
    }
}
