package com.chinaex123.redstone_chan_lib.api.fluid;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * 一次流体注册的产物。
 * <p>
 * 注册时拿到它，需要什么东西就从这里取。
 *
 * @param id         流体的注册名
 * @param definition 当初传入的流体描述
 * @param type       流体类型
 * @param source     源流体
 * @param flowing    流动流体
 * @param block      流体方块
 * @param bucket     桶物品，没注册桶时为 null
 */
public record FluidEntry(
        ResourceLocation id,
        FluidDefinition definition,
        DeferredHolder<FluidType, FluidType> type,
        DeferredHolder<Fluid, Fluid> source,
        DeferredHolder<Fluid, Fluid> flowing,
        DeferredBlock<LiquidBlock> block,
        DeferredItem<Item> bucket
) {

    /** @return 流体类型 */
    public FluidType fluidType() {
        return this.type.get();
    }

    /** @return 源流体 */
    public Fluid sourceFluid() {
        return this.source.get();
    }

    /** @return 流动流体 */
    public Fluid flowingFluid() {
        return this.flowing.get();
    }

    /** @return 流体方块 */
    public LiquidBlock fluidBlock() {
        return this.block.get();
    }

    /** @return 桶物品，没注册桶时为 null */
    public Item bucketItem() {
        return this.bucket == null ? null : this.bucket.get();
    }
}
