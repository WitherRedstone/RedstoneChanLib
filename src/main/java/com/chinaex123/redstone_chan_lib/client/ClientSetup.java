package com.chinaex123.redstone_chan_lib.client;

import com.chinaex123.redstone_chan_lib.RedstonechanLIB;
import com.chinaex123.redstone_chan_lib.client.particles.HolyMoteParticle;
import com.chinaex123.redstone_chan_lib.client.portal.PortalClientEvents;
import com.chinaex123.redstone_chan_lib.client.portal.PortalOverlay;
import com.chinaex123.redstone_chan_lib.init.RCLParticles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = RedstonechanLIB.MODID, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(RCLParticles.HOLY_MOTE.get(), HolyMoteParticle.Provider::new);
    }

    /**
     * 处理客户端刻事件。
     * <p>
     * 委托给虚空传送门客户端事件处理，推进其计时逻辑。
     *
     * @param event 客户端刻事件
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        PortalClientEvents.tick();
    }

    /**
     * 处理玩家退出世界事件。
     * <p>
     * 委托给虚空传送门客户端事件处理，重置其状态。
     *
     * @param event 玩家退出事件
     */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        PortalClientEvents.reset();
    }

    /**
     * 处理 GUI 图层注册事件。
     * <p>
     * 注册虚空传送门遮罩层。
     *
     * @param event GUI 图层注册事件
     */
    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        PortalOverlay.register(event);
    }
}
