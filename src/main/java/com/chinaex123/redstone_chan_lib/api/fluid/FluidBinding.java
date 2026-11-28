package com.chinaex123.redstone_chan_lib.api.fluid;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * 流体绑定。
 * <p>
 * 保存一种流体的完整注册结果：流体描述、流体类型、源流体与流动流体、
 * 流体方块及可选桶物品。各字段由流体注册流程填充，
 * 对外提供解析后的实例与部分延迟引用。
 */
public final class FluidBinding {

    /** 流体描述。 */
    private FluidDefinition definition;

    /** 流体类型。 */
    private DeferredHolder<FluidType, FluidType> type;

    /** 源流体。 */
    private DeferredHolder<Fluid, Fluid> source;

    /** 流动流体。 */
    private DeferredHolder<Fluid, Fluid> flowing;

    /** 流体方块。 */
    private DeferredBlock<LiquidBlock> block;

    /** 桶物品，可为 null。 */
    private DeferredItem<Item> bucket;

    FluidBinding() {
    }

    /**
     * 设置流体描述。
     *
     * @param definition 流体描述
     */
    void definition(FluidDefinition definition) {
        this.definition = definition;
    }

    /**
     * 设置流体类型。
     *
     * @param type 流体类型
     */
    void type(DeferredHolder<FluidType, FluidType> type) {
        this.type = type;
    }

    /**
     * 设置源流体。
     *
     * @param source 源流体
     */
    void source(DeferredHolder<Fluid, Fluid> source) {
        this.source = source;
    }

    /**
     * 设置流动流体。
     *
     * @param flowing 流动流体
     */
    void flowing(DeferredHolder<Fluid, Fluid> flowing) {
        this.flowing = flowing;
    }

    /**
     * 设置流体方块。
     *
     * @param block 流体方块
     */
    void block(DeferredBlock<LiquidBlock> block) {
        this.block = block;
    }

    /**
     * 设置桶物品。
     *
     * @param bucket 桶物品
     */
    void bucket(DeferredItem<Item> bucket) {
        this.bucket = bucket;
    }

    /** @return 流体描述 */
    public FluidDefinition definition() {
        return this.definition;
    }

    /** @return 流体类型 */
    public FluidType type() {
        return this.type.get();
    }

    /** @return 源流体 */
    public Fluid source() {
        return this.source.get();
    }

    /** @return 流动流体 */
    public Fluid flowing() {
        return this.flowing.get();
    }

    /** @return 流体方块 */
    public LiquidBlock block() {
        return this.block.get();
    }

    /** @return 桶物品，没注册桶时返回空气 */
    public Item bucket() {
        return this.bucket == null ? Items.AIR : this.bucket.get();
    }

    /** @return 源流体的延迟引用 */
    public DeferredHolder<Fluid, Fluid> sourceHolder() {
        return this.source;
    }

    /** @return 流动流体的延迟引用 */
    public DeferredHolder<Fluid, Fluid> flowingHolder() {
        return this.flowing;
    }
}
