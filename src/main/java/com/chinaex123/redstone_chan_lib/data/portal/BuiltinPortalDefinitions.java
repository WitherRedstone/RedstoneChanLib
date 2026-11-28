package com.chinaex123.redstone_chan_lib.data.portal;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import com.chinaex123.redstone_chan_lib.api.portal.PortalDefinition;
import com.chinaex123.redstone_chan_lib.api.portal.PortalDefinitions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

/**
 * 内置传送门定义。
 * <p>
 * 这些定义用 Java 直接注册，不需要任何数据包即可使用；
 * 服务器如果提供了同 id 的数据包定义，会覆盖这里的同名定义。
 */
public final class BuiltinPortalDefinitions {

    public static void register() {

//        // 竖向门：钻石块外框 + 钻石或水桶激活 + 默认颜色 + 名称“测试” + 前往下界
//        PortalDefinitions.register(PortalDefinition.builder(RedstoneChanLIB.id("test"))
//                .frameBlock(Blocks.DIAMOND_BLOCK)
//                .catalyst(Items.DIAMOND)
//                .fluidCatalyst(Fluids.WATER)
//                .name(Component.translatable("portal.redstone_chan_lib.test"))
//                .dimension(Level.NETHER)
//                .build());

    }
}
