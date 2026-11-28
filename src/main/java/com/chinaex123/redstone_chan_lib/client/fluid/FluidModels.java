package com.chinaex123.redstone_chan_lib.client.fluid;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.loaders.DynamicFluidContainerModelBuilder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * 流体桶物品模型工具类。
 * <p>
 * 基于 NeoForge 的动态流体容器模型生成本模组桶物品的模型：
 * 提供普通桶、滴落桶与「气体上浮」桶三种变体，
 * 分别对应不同的父模型与气体翻转配置。
 */
public final class FluidModels {

    private FluidModels() {
    }

    /**
     * 生成普通流体桶模型。
     * <p>
     * 使用 NeoForge 的桶父模型，流体内容取自桶物品，气体不翻转。
     *
     * @param provider 物品模型提供器
     * @param item     桶物品
     */
    public static void bucket(ItemModelProvider provider, DeferredItem<? extends BucketItem> item) {
        provider.withExistingParent(item.getId().getPath(), ResourceLocation.parse("neoforge:item/bucket"))
                .customLoader((builder, helper) -> DynamicFluidContainerModelBuilder.begin(builder, helper)
                        .fluid(item.get().content)
                        .flipGas(false));
    }

    /**
     * 生成滴落流体桶模型。
     * <p>
     * 使用 NeoForge 的滴落桶父模型，流体内容取自桶物品，气体不翻转。
     *
     * @param provider 物品模型提供器
     * @param item     桶物品
     */
    public static void dripBucket(ItemModelProvider provider, DeferredItem<? extends BucketItem> item) {
        provider.withExistingParent(item.getId().getPath(), ResourceLocation.parse("neoforge:item/bucket_drip"))
                .customLoader((builder, helper) -> DynamicFluidContainerModelBuilder.begin(builder, helper)
                        .fluid(item.get().content)
                        .flipGas(false));
    }

    /**
     * 生成气体上浮的流体桶模型。
     * <p>
     * 使用 NeoForge 的桶父模型，流体内容取自桶物品，并翻转气体使液面朝上。
     *
     * @param provider 物品模型提供器
     * @param item     桶物品
     */
    public static void floatingBucket(ItemModelProvider provider, DeferredItem<? extends BucketItem> item) {
        provider.withExistingParent(item.getId().getPath(), ResourceLocation.parse("neoforge:item/bucket"))
                .customLoader((builder, helper) -> DynamicFluidContainerModelBuilder.begin(builder, helper)
                        .fluid(item.get().content)
                        .flipGas(true));
    }
}
