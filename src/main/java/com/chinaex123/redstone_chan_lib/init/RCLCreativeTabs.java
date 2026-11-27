package com.chinaex123.redstone_chan_lib.init;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import com.chinaex123.redstone_chan_lib.api.creative.CreativeSection;
import com.chinaex123.redstone_chan_lib.api.creative.CreativeSectionStyle;
import com.chinaex123.redstone_chan_lib.api.creative.SectionedCreativeTab;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 本模组的创造模式页签。
 * <p>这里同时也是 {@link SectionedCreativeTab} 的用法示例：用原版物品摆出三个分区，
 * 方便直接在游戏里看分区按钮的切换与 Shift 多选效果。</p>
 */
public interface RCLCreativeTabs {
    DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RedstoneChanLIB.MODID);

    SectionedCreativeTab RCLIB = SectionedCreativeTab.builder(RedstoneChanLIB.id("rclib_tab"))
            .title(Component.translatable("itemGroup." + RedstoneChanLIB.MODID))
            .icon(() -> new ItemStack(Items.REDSTONE))
            .style(CreativeSectionStyle.defaults())
            // 材料：铁锭、金锭
            .section(CreativeSection.of(
                    Component.translatable("creative_tab." + RedstoneChanLIB.MODID + ".materials"),
                    () -> new ItemStack(Items.IRON_INGOT),
                    output -> {
                        output.accept(Items.IRON_INGOT);
                        output.accept(Items.GOLD_INGOT);
                    }))
            // 矿物：钻石、绿宝石
            .section(CreativeSection.of(
                    Component.translatable("creative_tab." + RedstoneChanLIB.MODID + ".ores"),
                    () -> new ItemStack(Items.DIAMOND),
                    output -> {
                        output.accept(Items.DIAMOND);
                        output.accept(Items.EMERALD);
                    }))
            // 方块：石头、红砖块
            .section(CreativeSection.of(
                    Component.translatable("creative_tab." + RedstoneChanLIB.MODID + ".blocks"),
                    () -> new ItemStack(Blocks.STONE),
                    output -> {
                        output.accept(Blocks.STONE);
                        output.accept(Blocks.BRICKS);
                    }))
            .build();

    DeferredHolder<CreativeModeTab, CreativeModeTab> RCLIB_TAB =
            CREATIVE_MODE_TABS.register("rclib_tab", RCLIB::tab);

    static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
