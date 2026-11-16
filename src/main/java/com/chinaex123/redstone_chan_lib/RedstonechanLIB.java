package com.chinaex123.redstone_chan_lib;

import com.chinaex123.redstone_chan_lib.init.*;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import org.slf4j.Logger;

@Mod(RedstonechanLIB.MODID)
public class RedstonechanLIB {
    public static final String MODID = "redstone_chan_lib";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RedstonechanLIB(IEventBus modEventBus, ModContainer modContainer) {
        RCLBlocks.register(modEventBus);
        RCLItems.register(modEventBus);
        RCLParticles.register(modEventBus);
    }

    public static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(MODID, name);
    }
}