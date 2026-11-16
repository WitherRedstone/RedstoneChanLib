package com.chinaex123.redstone_chan_lib;

import com.chinaex123.redstone_chan_lib.init.RCLItems;
import com.chinaex123.redstone_chan_lib.init.RCLParticles;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;

@Mod(RedstonechanLIB.MODID)
public class RedstonechanLIB {
    public static final String MODID = "redstone_chan_lib";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RedstonechanLIB(IEventBus modEventBus, ModContainer modContainer) {
        RCLItems.register(modEventBus);
        RCLParticles.register(modEventBus);
    }

    public static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(MODID, name);
    }
}