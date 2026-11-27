package com.chinaex123.redstone_chan_lib.event.portal;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import com.chinaex123.redstone_chan_lib.api.portal.*;
import com.chinaex123.redstone_chan_lib.block.CustomPortal;
import com.chinaex123.redstone_chan_lib.init.RCLBlocks;
import com.chinaex123.redstone_chan_lib.util.PortalTeleportManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 传送门事件处理器。
 * <p>
 * 集中处理传送门的各项交互与生命周期：
 * 右键框架尝试点亮、掉落物触发仪式式激活、
 * 破坏传送门或框架时拆除整座门、服务端每刻推进传送计时，
 * 以及玩家退出时取消其传送。
 */
@EventBusSubscriber(modid = RedstoneChanLIB.MODID)
public final class PortalEvents {

    private PortalEvents() {}

    /**
     * 处理右键框架方块事件。
     * <p>
     * 仅处理主手；若成功点亮传送门则取消原交互并返回成功。
     *
     * @param event 右键方块事件
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Level level = event.getLevel();
        PortalDefinition activated = PortalActivation.tryActivate(level, event.getPos(), event.getEntity(),
                event.getHand(), event.getItemStack());
        if (activated != null) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    /**
     * 处理掉落物每刻更新事件。
     * <p>
     * 服务端下尝试用掉落物触发仪式式激活。
     *
     * @param event 实体每刻更新事件
     */
    @SubscribeEvent
    public static void onItemTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity itemEntity)) {
            return;
        }
        if (!(itemEntity.level() instanceof ServerLevel level)) {
            return;
        }
        PortalRitualActivation.tryActivate(level, itemEntity);
    }

    /**
     * 处理方块破坏事件。
     * <p>
     * 破坏传送门方块时拆除整座连通的传送门；
     * 破坏框架方块时，检查紧邻六向的传送门方块，
     * 仅当该方块正好落在某座门的外圈上时才拆除该门。
     *
     * @param event 方块破坏事件
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        LevelAccessor accessor = event.getLevel();
        if (!(accessor instanceof Level level)) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState state = event.getState();

        if (state.is(RCLBlocks.CUSTOM_PORTAL.get())) {
            PortalStructureHelper.destroyConnectedPortal(level, pos);
            return;
        }

        if (!PortalDefinitions.isAnyFrame(state)) {
            return;
        }

        // 框架被拆：检查紧邻的六个方向，只有当这块方块正好落在某座门的外圈上时才拆除
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighbor);
            if (!neighborState.is(RCLBlocks.CUSTOM_PORTAL.get())) {
                continue;
            }
            Direction.Axis axis = neighborState.getValue(CustomPortal.AXIS);
            if (PortalShape.isFramePosition(level, pos, neighbor, axis, RCLBlocks.CUSTOM_PORTAL.get())) {
                PortalStructureHelper.destroyConnectedPortal(level, neighbor);
            }
        }
    }

    /**
     * 处理服务端每刻更新事件。
     * <p>
     * 推进传送倒计时管理。
     *
     * @param event 服务端每刻更新事件
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        PortalTeleportManager.tick();
    }

    /**
     * 处理玩家退出事件。
     * <p>
     * 取消该玩家尚未完成的传送。
     *
     * @param event 玩家退出事件
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PortalTeleportManager.cancel(player);
        }
    }
}
