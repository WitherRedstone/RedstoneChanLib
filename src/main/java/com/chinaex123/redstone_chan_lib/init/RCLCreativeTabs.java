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
            // 流体示例：液态红石桶、红石气体桶
            .section(CreativeSection.of(
                    Component.translatable("creative_tab." + RedstoneChanLIB.MODID + ".fluids"),
                    () -> new ItemStack(RCLDemoFluids.LIQUID_REDSTONE.bucketItem()),
                    output -> {
                        output.accept(RCLDemoFluids.LIQUID_REDSTONE.bucketItem());
                        output.accept(RCLDemoFluids.REDSTONE_GAS.bucketItem());
                        output.accept(RCLDemoFluids.REDSTONE_VAPOR.bucketItem());
                    }))
            .build();

    DeferredHolder<CreativeModeTab, CreativeModeTab> RCLIB_TAB =
            CREATIVE_MODE_TABS.register("rclib_tab", RCLIB::tab);

    static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
