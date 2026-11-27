package com.chinaex123.redstone_chan_lib.util;

import com.chinaex123.redstone_chan_lib.api.portal.PortalDefinition;
import com.chinaex123.redstone_chan_lib.api.portal.PortalShape;
import com.chinaex123.redstone_chan_lib.api.portal.PortalTeleport;
import com.chinaex123.redstone_chan_lib.block.CustomPortal;
import com.chinaex123.redstone_chan_lib.block.PortalBlockEntity;
import com.chinaex123.redstone_chan_lib.init.RCLBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 传送门传送管理器。
 * <p>
 * 管理玩家经过传送门的倒计时传送：玩家进入传送门时登记等待请求，
 * 服务端每刻推进倒计时，期间玩家只要仍在门内就继续计时，
 * 离开门、换维度或断线则取消；倒计时结束后执行传送，
 * 并记录目标门方块以防玩家落地后立刻被弹回。
 */
public final class PortalTeleportManager {

    /** 默认等待刻数，客户端遮罩动画也按这个值计算进度 */
    public static final int PORTAL_WAIT_TICKS = 80;

    /** 等待中的传送请求，键为玩家 UUID */
    private static final Map<UUID, PendingTeleport> PENDING = new HashMap<>();

    /** 玩家刚抵达的目标门记录，键为玩家 UUID */
    private static final Map<UUID, Arrival> ARRIVALS = new HashMap<>();

    /**
     * 抵达记录。
     *
     * @param player 玩家
     * @param blocks 目标门方块集合
     */
    private record Arrival(ServerPlayer player, java.util.Set<BlockPos> blocks) {}

    private PortalTeleportManager() {}

    /**
     * 等待中的传送请求。
     */
    private static final class PendingTeleport {

        /** 玩家。 */
        private final ServerPlayer player;

        /** 来源维度。 */
        private final ServerLevel sourceLevel;

        /** 进门方块位置。 */
        private final BlockPos portalPos;

        /** 来源门的方块集合。 */
        private final java.util.Set<BlockPos> portalBlocks;

        /** 传送目的地。 */
        private final PortalTeleport.Destination destination;

        /** 剩余等待刻数。 */
        private int ticksLeft;

        /**
         * 构造等待中的传送请求。
         *
         * @param player       玩家
         * @param sourceLevel  来源维度
         * @param portalPos    进门方块位置
         * @param portalBlocks 来源门方块集合
         * @param destination  传送目的地
         * @param ticksLeft    剩余等待刻数
         */
        private PendingTeleport(ServerPlayer player, ServerLevel sourceLevel, BlockPos portalPos,
                                java.util.Set<BlockPos> portalBlocks, PortalTeleport.Destination destination,
                                int ticksLeft) {
            this.player = player;
            this.sourceLevel = sourceLevel;
            this.portalPos = portalPos;
            this.portalBlocks = portalBlocks;
            this.destination = destination;
            this.ticksLeft = ticksLeft;
        }
    }

    /**
     * 开始一次传送倒计时。
     * <p>
     * 已有等待请求或玩家刚抵达目标门时忽略；
     * 读取进门方块的传送门定义并解析目的地，失败则放弃；
     * 成功后记录来源门方块并播放触发音效。
     *
     * @param player 玩家
     * @param pos    进门方块位置
     */
    public static void beginPortalTeleport(ServerPlayer player, BlockPos pos) {
        if (PENDING.containsKey(player.getUUID())) {
            return;
        }
        // 刚抵达目标门时忽略触发，避免来回弹跳
        Arrival arrival = ARRIVALS.get(player.getUUID());
        if (arrival != null && arrival.blocks().contains(pos)) {
            return;
        }
        if (!(player.level().getBlockEntity(pos) instanceof PortalBlockEntity blockEntity)) {
            return;
        }
        PortalDefinition definition = blockEntity.getDefinition();
        if (definition == null) {
            return;
        }

        ServerLevel sourceLevel = player.serverLevel();
        // 取进门方块的法线轴，目标维度新建的门会沿用同样的朝向
        Direction.Axis axis = player.level().getBlockState(pos).getOptionalValue(CustomPortal.AXIS).orElse(null);
        // 目的地由 API 统一计算：已有同定义的门就复用，没有就建一座
        PortalTeleport.Destination destination = PortalTeleport.resolve(player, definition, axis);
        if (destination == null) {
            // 目标维度不存在，直接放弃这次传送
            return;
        }

        int waitTicks = definition.waitTicks() > 0 ? definition.waitTicks() : PORTAL_WAIT_TICKS;
        // 记录整座门的方块，倒计时期间玩家在门内横向移动也不会被打断
        java.util.Set<BlockPos> portalBlocks = new java.util.HashSet<>(
                PortalShape.connectedPortalBlocks(sourceLevel, pos, RCLBlocks.CUSTOM_PORTAL.get()));
        portalBlocks.add(pos.immutable());
        PENDING.put(player.getUUID(), new PendingTeleport(player, sourceLevel, pos.immutable(), portalBlocks,
                destination, waitTicks));

        sourceLevel.playSound(null, pos, SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 0.6F, 1.0F);
    }

    /**
     * 每刻更新。
     * <p>
     * 先清理玩家已离开目标门的抵达记录，使其再次进入时能正常触发；
     * 再推进各等待请求的倒计时，玩家失效、换维度或离开门时取消，
     * 倒计时结束则执行传送。
     */
    public static void tick() {
        // 先清理已经离开目标门的抵达记录，玩家再次进入时才能正常触发
        ARRIVALS.entrySet().removeIf(entry -> {
            ServerPlayer player = entry.getValue().player();
            if (player.isRemoved() || player.hasDisconnected()) {
                return true;
            }
            BlockPos current = findPortalBlock(player);
            return current == null || !entry.getValue().blocks().contains(current);
        });

        Iterator<Map.Entry<UUID, PendingTeleport>> iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            PendingTeleport pending = iterator.next().getValue();
            ServerPlayer player = pending.player;

            if (player.isRemoved() || player.hasDisconnected()) {
                iterator.remove();
                continue;
            }
            if (player.serverLevel() != pending.sourceLevel || !overlapsAny(player, pending.portalBlocks)) {
                iterator.remove();
                continue;
            }

            pending.ticksLeft--;
            if (pending.ticksLeft <= 0) {
                iterator.remove();
                executeDelayedTeleport(pending);
            }
        }
    }

    /**
     * 执行延迟传送。
     * <p>
     * 移动玩家到目的地，并记录目标门的全部方块，
     * 防止玩家刚落地就被再次传送回去。
     *
     * @param pending 等待中的传送请求
     */
    private static void executeDelayedTeleport(PendingTeleport pending) {
        ServerPlayer player = pending.player;
        PortalTeleport.Destination destination = pending.destination;
        PortalTeleport.move(player, destination);

        // 记录目标门的全部方块，防止玩家刚落地就被再次传送回去
        ServerLevel targetLevel = destination.level();
        BlockPos target = destination.pos();
        java.util.Set<BlockPos> blocks = new java.util.HashSet<>(
                PortalShape.connectedPortalBlocks(targetLevel, target, RCLBlocks.CUSTOM_PORTAL.get()));
        if (!blocks.isEmpty()) {
            ARRIVALS.put(player.getUUID(), new Arrival(player, blocks));
        }
    }

    /**
     * 查找玩家碰撞箱覆盖范围内的传送门方块。
     *
     * @param player 玩家
     * @return 传送门方块位置，未找到时返回 null
     */
    @Nullable
    private static BlockPos findPortalBlock(ServerPlayer player) {
        AABB box = player.getBoundingBox();
        BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
        BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (player.level().getBlockState(pos).is(RCLBlocks.CUSTOM_PORTAL.get())) {
                return pos.immutable();
            }
        }
        return null;
    }

    /**
     * 判断玩家碰撞箱是否与给定门方块集合中的任意一个重叠。
     *
     * @param player 玩家
     * @param blocks 门方块集合
     * @return 存在重叠返回 true
     */
    private static boolean overlapsAny(ServerPlayer player, java.util.Set<BlockPos> blocks) {
        ServerLevel level = player.serverLevel();
        AABB box = player.getBoundingBox();
        for (BlockPos pos : blocks) {
            if (!level.getBlockState(pos).is(RCLBlocks.CUSTOM_PORTAL.get())) {
                continue;
            }
            if (new AABB(pos).deflate(0.1D).intersects(box)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断玩家是否正在等待传送。
     *
     * @param player 玩家
     * @return 正在等待传送返回 true
     */
    public static boolean isPending(ServerPlayer player) {
        return PENDING.containsKey(player.getUUID());
    }

    /**
     * 取消玩家的等待传送与抵达记录。
     *
     * @param player 玩家
     */
    public static void cancel(ServerPlayer player) {
        PENDING.remove(player.getUUID());
        ARRIVALS.remove(player.getUUID());
    }

    /**
     * 清空所有传送状态。
     */
    public static void clearAll() {
        PENDING.clear();
        ARRIVALS.clear();
    }
}
