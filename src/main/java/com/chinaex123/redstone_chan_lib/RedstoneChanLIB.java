package com.chinaex123.redstone_chan_lib;

import com.chinaex123.redstone_chan_lib.init.*;
import com.chinaex123.redstone_chan_lib.data.portal.BuiltinPortalDefinitions;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import org.slf4j.Logger;

@Mod(RedstoneChanLIB.MODID)
public class RedstoneChanLIB {
    public static final String MODID = "redstone_chan_lib";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RedstoneChanLIB(IEventBus modEventBus, ModContainer modContainer) {
        RCLBlocks.register(modEventBus);
        RCLItems.register(modEventBus);
        RCLParticles.register(modEventBus);
        RCLBlockEntities.register(modEventBus);
        RCLCreativeTabs.register(modEventBus);

        BuiltinPortalDefinitions.register();
    }

    public static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(MODID, name);
    }
}