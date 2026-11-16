package com.chinaex123.redstone_chan_lib.event.portal;

import com.chinaex123.redstone_chan_lib.RedstonechanLIB;
import com.chinaex123.redstone_chan_lib.block.CustomPortal;
import com.chinaex123.redstone_chan_lib.init.RCLBlocks;
import com.chinaex123.redstone_chan_lib.util.PortalFrameHelper;
import com.chinaex123.redstone_chan_lib.util.PortalStructureHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * 虚空传送门破坏事件处理类。
 * <p>
 * 负责处理传送门相关方块被破坏时的联动逻辑：
 * 破坏关键框架方块时销毁相关传送门，破坏传送门方块时销毁整个传送门结构。
 */
@EventBusSubscriber(modid = RedstonechanLIB.MODID)
public class PortalBreakEvent {

    /**
     * 处理方块破坏事件。
     * <p>
     * 当被破坏方块为虚空石且属于关键框架方块时，销毁与之相关的传送门；
     * 当被破坏方块为传送门方块时，根据其轴向销毁整个传送门结构。
     *
     * @param event 方块破坏事件
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Level level = event.getPlayer().level();
        if (level.isClientSide()) return;

        BlockPos brokenPos = event.getPos();
        BlockState brokenState = event.getState();

        if (brokenState.is(RCLBlocks.NAUGHT_STONE.get())) {
            if (PortalFrameHelper.isCriticalFrameBlock(level, brokenPos)) {
                PortalStructureHelper.destroyRelatedPortals(level, brokenPos);
            }
        } else if (brokenState.is(RCLBlocks.CUSTOM_PORTAL.get())) {
            try {
                Direction.Axis portalAxis = brokenState.getValue(CustomPortal.AXIS);
                Direction.Axis frameAxis = PortalFrameHelper.frameAxisOf(portalAxis);
                PortalStructureHelper.destroyPortalStructure(level, brokenPos, frameAxis);
            } catch (Exception e) {
                // 忽略
            }
        }
    }
}
