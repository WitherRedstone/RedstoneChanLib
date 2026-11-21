package com.chinaex123.redstone_chan_lib.event.outline;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import com.chinaex123.redstone_chan_lib.api.outline.BlockOutline;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/**
 * 方块描边的自动清理。
 * <p>
 * 在方块被破坏或被爆炸摧毁时清除对应坐标上的描边，
 * 避免描边残留在已经空掉的位置上。
 */
@EventBusSubscriber(modid = RedstoneChanLIB.MODID)
public final class BlockOutlineEvents {

    private BlockOutlineEvents() {
    }

    /**
     * 玩家或别的模组破坏方块时，清掉这个坐标上的描边。
     *
     * @param event 方块破坏事件
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide) {
            return;
        }
        BlockPos pos = event.getPos();
        if (BlockOutline.isActive(level, pos)) {
            BlockOutline.clear(level, pos);
        }
    }

    /**
     * 爆炸炸掉方块时，清掉这些坐标上的描边。
     *
     * @param event 爆炸确定影响范围的事件
     */
    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        for (BlockPos pos : event.getAffectedBlocks()) {
            if (BlockOutline.isActive(level, pos)) {
                BlockOutline.clear(level, pos);
            }
        }
    }
}
