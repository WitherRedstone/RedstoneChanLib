package com.chinaex123.redstone_chan_lib.client.outline;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import com.chinaex123.redstone_chan_lib.api.outline.BlockOutline;
import com.chinaex123.redstone_chan_lib.api.outline.BlockScanOutline;
import com.chinaex123.redstone_chan_lib.api.outline.EntityScanOutline;
import com.chinaex123.redstone_chan_lib.network.OutlineCyclePayload;
import com.chinaex123.redstone_chan_lib.util.PortalTexts;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
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
 */
@EventBusSubscriber(modid = RedstoneChanLIB.MODID, value = Dist.CLIENT)
public final class EntityOutlineDemo {

    /** 扫描测试用的半径 */
    private static final int SCAN_RADIUS = 16;

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
     * 消费按键点击：K 键在连上服务端时发送描边切换请求；
     * J、L 键直接在客户端开关扫描（扫描只是“看”的效果，客户端生效最快）。
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
        while (SCAN_KEY.consumeClick()) {
            toggleScan();
        }
        while (ENTITY_SCAN_KEY.consumeClick()) {
            toggleEntityScan();
        }
    }

    /**
     * 开关矿石扫描。
     * <p>
     * 开启时用线框高亮周围所有矿石，每种矿用它自己的颜色（和参考模组的配矿色一致），
     * 再按一次关掉。
     */
    private static void toggleScan() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        if (BlockScanOutline.get(minecraft.level) != null) {
            BlockScanOutline.stop(minecraft.level);
            notify("scan_demo_off");
            return;
        }
        BlockScanOutline.Target ores = BlockScanOutline.target(
                BlockTags.COAL_ORES, BlockTags.IRON_ORES, BlockTags.COPPER_ORES, BlockTags.GOLD_ORES,
                BlockTags.REDSTONE_ORES, BlockTags.LAPIS_ORES, BlockTags.DIAMOND_ORES, BlockTags.EMERALD_ORES);
        BlockScanOutline.Scan scan = BlockScanOutline.scan(BlockOutline.Mode.WIRE, 0xFF7CF9FF)
                .radius(SCAN_RADIUS)
                .throughWalls(true)
                .oreDefaults()
                .color(Blocks.DEEPSLATE_DIAMOND_ORE, 0xFF9CFFF2)
                .build();
        BlockScanOutline.start(minecraft.level, ores, scan);
        notify("scan_demo_on");
    }

    /**
     * 开关“准星所指实体的同类”扫描。
     * <p>
     * 开启时把准星指着的实体的类型当作扫描目标，再按一次关掉。
     */
    private static void toggleEntityScan() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        if (EntityScanOutline.get(minecraft.level) != null) {
            EntityScanOutline.stop(minecraft.level);
            notify("entity_scan_off");
            return;
        }
        Entity picked = minecraft.crosshairPickEntity;
        if (picked == null) {
            notify("entity_scan_no_target");
            return;
        }
        EntityScanOutline.Target target = EntityScanOutline.target(picked.getType());
        EntityScanOutline.Scan scan = new EntityScanOutline.Scan(0xFFFFC966, BlockOutline.Style.SOLID,
                BlockOutline.defaultThickness(BlockOutline.Mode.WIRE), SCAN_RADIUS, true);
        EntityScanOutline.start(minecraft.level, target, scan);
        Component name = picked.getType().getDescription();
        minecraft.player.displayClientMessage(
                PortalTexts.component("entity_scan_on", name.getString()), true);
        RedstoneChanLIB.LOGGER.info(PortalTexts.text("entity_scan_on", name.getString()));
    }

    /**
     * 显示提示并在日志里留一行。
     *
     * @param key 文案键
     */
    private static void notify(String key) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(PortalTexts.component(key), true);
        }
        RedstoneChanLIB.LOGGER.info(PortalTexts.text(key));
    }
}
