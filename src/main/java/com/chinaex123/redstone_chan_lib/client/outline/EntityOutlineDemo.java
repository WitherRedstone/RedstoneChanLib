package com.chinaex123.redstone_chan_lib.client.outline;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import com.chinaex123.redstone_chan_lib.network.OutlineCyclePayload;
import com.chinaex123.redstone_chan_lib.network.ScanTogglePayload;
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
 * 描边演示。
 * <p>
 * 注册三个游戏内按键：K 键向服务端发送描边切换请求，用来演示实体与单方块描边；
 * J 键开关周围矿石的扫描高亮；L 键开关“准星指着的那个实体”的同类扫描。
 * <p>
 * 三个键都只是“发请求”，真正的写存档、广播同步都在服务端做，
 * 所以退出重进世界之后描边与扫描都还在。
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

    /** 开关周围矿石的扫描高亮 */
    private static final KeyMapping SCAN_KEY = new KeyMapping(
            "key.redstone_chan_lib.scan_demo",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "key.categories.redstone_chan_lib");

    /** 开关实体扫描高亮 */
    private static final KeyMapping ENTITY_SCAN_KEY = new KeyMapping(
            "key.redstone_chan_lib.entity_scan_demo",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_L,
            "key.categories.redstone_chan_lib");

    private EntityOutlineDemo() {
    }

    /**
     * 注册按键映射。
     *
     * @param event 按键映射注册事件
     */
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(CYCLE_KEY);
        event.register(SCAN_KEY);
        event.register(ENTITY_SCAN_KEY);
    }

    /**
     * 客户端每刻更新。
     * <p>
     * 消费按键点击并向服务端发请求：K 切描边，J 开关矿石扫描，L 开关实体扫描。
     *
     * @param event 客户端每刻更新事件
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().getConnection() == null) {
            return;
        }
        while (CYCLE_KEY.consumeClick()) {
            PacketDistributor.sendToServer(OutlineCyclePayload.INSTANCE);
        }
        while (SCAN_KEY.consumeClick()) {
            PacketDistributor.sendToServer(new ScanTogglePayload(ScanTogglePayload.KIND_BLOCK));
        }
        while (ENTITY_SCAN_KEY.consumeClick()) {
            PacketDistributor.sendToServer(new ScanTogglePayload(ScanTogglePayload.KIND_ENTITY));
        }
    }
}
