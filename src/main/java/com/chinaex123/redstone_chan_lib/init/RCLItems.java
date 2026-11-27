package com.chinaex123.redstone_chan_lib.init;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public interface RCLItems {
    DeferredRegister.Items ITEMS = DeferredRegister.createItems(RedstoneChanLIB.MODID);

    static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
