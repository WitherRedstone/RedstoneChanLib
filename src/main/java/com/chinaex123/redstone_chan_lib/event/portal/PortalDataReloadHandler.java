package com.chinaex123.redstone_chan_lib.event.portal;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import com.chinaex123.redstone_chan_lib.data.portal.PortalDefinitionLoader;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/**
 * 传送门数据重载事件处理器。
 * <p>
 * 在资源重载时注册传送门定义加载器，
 * 使数据包中的传送门定义能够随 /reload 重新加载。
 */
@EventBusSubscriber(modid = RedstoneChanLIB.MODID)
public class PortalDataReloadHandler {

    /**
     * 注册数据重载监听器。
     * <p>
     * 向重载事件添加传送门定义加载器。
     *
     * @param event 数据重载监听器注册事件
     */
    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new PortalDefinitionLoader());
    }
}