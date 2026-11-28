package com.chinaex123.redstone_chan_lib.event.collision;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import com.chinaex123.redstone_chan_lib.api.collision.CollisionRecipes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 碰撞配方的每刻推进。
 * <p>
 * 产出的延迟执行靠这里驱动，服务器关闭时顺手把队列清掉。
 */
@EventBusSubscriber(modid = RedstoneChanLIB.MODID)
public final class CollisionEvents {

    /**
     * 工具类，不允许实例化。
     */
    private CollisionEvents() {
    }

    /**
     * 每个服务端刻推进一次延迟队列。
     *
     * @param event 服务端刻事件
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        CollisionRecipes.tick();
    }

    /**
     * 服务器关闭时清空队列，避免残留引用。
     *
     * @param event 服务器关闭事件
     */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        CollisionRecipes.clearTasks();
    }
}
