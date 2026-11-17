package com.chinaex123.redstone_chan_lib.init;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public interface RCLCreativeTabs {
    DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RedstoneChanLIB.MODID);

    DeferredHolder<CreativeModeTab, CreativeModeTab> RCLIB_TAB = CREATIVE_MODE_TABS.register("rclib_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(RCLItems.CESHI.get()))
                    .title(Component.translatable("itemGroup." + RedstoneChanLIB.MODID))
                    .displayItems((parameters, output) -> {

                        output.accept(RCLItems.CESHI.get());

                    })
                    .build());

    static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
