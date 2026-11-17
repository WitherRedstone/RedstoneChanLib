package com.chinaex123.redstone_chan_lib.client;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import com.chinaex123.redstone_chan_lib.block.PortalBlockEntity;
import com.chinaex123.redstone_chan_lib.client.particles.HolyMoteParticle;
import com.chinaex123.redstone_chan_lib.client.portal.PortalClientEvents;
import com.chinaex123.redstone_chan_lib.client.portal.PortalOverlay;
import com.chinaex123.redstone_chan_lib.init.RCLBlocks;
import com.chinaex123.redstone_chan_lib.init.RCLParticles;
import com.chinaex123.redstone_chan_lib.api.portal.PortalDefinition;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/**
 * 客户端初始化。
 * <p>
 * 注册粒子提供器、传送门方块颜色处理器与 GUI 叠加层，
 * 并在客户端每刻推进传送门状态、在退出世界时重置状态。
 */
@EventBusSubscriber(modid = RedstoneChanLIB.MODID, value = Dist.CLIENT)
public class ClientSetup {

    /**
     * 注册粒子提供器。
     * <p>
     * 为圣光微粒粒子注册基于精灵表的提供器。
     *
     * @param event 粒子提供器注册事件
     */
    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(RCLParticles.HOLY_MOTE.get(), HolyMoteParticle.Provider::new);
    }

    /**
     * 注册方块颜色处理器。
     * <p>
     * 传送门方块按方块实体中保存的颜色着色，无法读取时使用默认颜色。
     *
     * @param event 方块颜色注册事件
     */
    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (level == null || pos == null) {
                return PortalDefinition.DEFAULT_COLOR;
            }
            if (level.getBlockEntity(pos) instanceof PortalBlockEntity blockEntity) {
                return blockEntity.getColor();
            }
            return PortalDefinition.DEFAULT_COLOR;
        }, RCLBlocks.CUSTOM_PORTAL.get());
    }

    /**
     * 客户端每刻更新。
     * <p>
     * 推进传送门客户端状态。
     *
     * @param event 客户端每刻更新事件
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        PortalClientEvents.tick();
    }

    /**
     * 退出世界时重置。
     * <p>
     * 清理客户端传送门状态，避免带入下一个世界。
     *
     * @param event 退出世界事件
     */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        PortalClientEvents.reset();
    }

    /**
     * 注册 GUI 叠加层。
     * <p>
     * 注册传送门遮罩叠加层。
     *
     * @param event GUI 叠加层注册事件
     */
    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        PortalOverlay.register(event);
    }
}
