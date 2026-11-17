package com.chinaex123.redstone_chan_lib.init;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import com.chinaex123.redstone_chan_lib.block.PortalBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public interface RCLBlockEntities {

    DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, RedstoneChanLIB.MODID);

    /** 传送门方块实体 */
    DeferredHolder<BlockEntityType<?>, BlockEntityType<PortalBlockEntity>> PORTAL = BLOCK_ENTITIES.register("portal",
            () -> BlockEntityType.Builder.of(PortalBlockEntity::new, RCLBlocks.CUSTOM_PORTAL.get()).build(null));

    static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
