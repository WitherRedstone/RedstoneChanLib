package com.chinaex123.redstone_chan_lib.init;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import com.chinaex123.redstone_chan_lib.item.CeshiItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public interface RCLItems {
    DeferredRegister.Items ITEMS = DeferredRegister.createItems(RedstoneChanLIB.MODID);

    DeferredItem<Item> CESHI = ITEMS.register("ceshi", () -> new CeshiItem(new Item.Properties()));

    static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
