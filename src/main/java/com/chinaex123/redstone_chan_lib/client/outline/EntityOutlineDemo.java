package com.chinaex123.redstone_chan_lib.client.outline;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import com.chinaex123.redstone_chan_lib.api.outline.EntityOutline;
import com.chinaex123.redstone_chan_lib.network.OutlineCyclePayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * 实体描边演示。
 * <p>
 * 注册一个游戏内按键，按下时向服务端发送描边切换请求，
 * 用于演示与测试实体描边的各档效果。
 */
@EventBusSubscriber(modid = RedstoneChanLIB.MODID, value = Dist.CLIENT)
public final class EntityOutlineDemo {

    /** 切换到下一档描边 */
    private static final KeyMapping CYCLE_KEY = new KeyMapping(
            "key.redstone_chan_lib.outline_demo",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.redstone_chan_lib");

    /**
     * 注册按键映射。
     *
     * @param event 按键映射注册事件
     */
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(CYCLE_KEY);
    }

    /**
     * 客户端每刻更新。
     * <p>
     * 消费按键点击，若已连接到服务端则发送描边切换请求。
     *
     * @param event 客户端每刻更新事件
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (CYCLE_KEY.consumeClick()) {
            if (Minecraft.getInstance().getConnection() != null) {
                PacketDistributor.sendToServer(OutlineCyclePayload.INSTANCE);
            }
        }
    }
}
