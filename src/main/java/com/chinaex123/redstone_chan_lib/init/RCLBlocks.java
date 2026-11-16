package com.chinaex123.redstone_chan_lib.init;

import com.chinaex123.redstone_chan_lib.RedstonechanLIB;
import com.chinaex123.redstone_chan_lib.block.CustomPortal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public interface RCLBlocks {
    DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RedstonechanLIB.MODID);

    /* 虚空传送门 */
    DeferredBlock<Block> CUSTOM_PORTAL =
            BLOCKS.register("custom_portal", () -> new CustomPortal(BlockBehaviour.Properties.of()
                    .noCollission()
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> 15)
                    .strength(-1.0F, 3600000.0F)));

    static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
