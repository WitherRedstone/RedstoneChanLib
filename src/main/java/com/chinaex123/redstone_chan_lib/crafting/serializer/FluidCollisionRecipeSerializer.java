package com.chinaex123.redstone_chan_lib.crafting.serializer;

import com.chinaex123.redstone_chan_lib.api.collision.FluidCollisionRecipe;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * 流体碰撞配方序列化器。
 * <p>
 * 数据编解码与网络流编解码都委托给配方自身定义的静态编解码器。
 */
public class FluidCollisionRecipeSerializer implements RecipeSerializer<FluidCollisionRecipe> {

    /**
     * 获取数据编解码器。
     *
     * @return 配方数据编解码器
     */
    @Override
    public MapCodec<FluidCollisionRecipe> codec() {
        return FluidCollisionRecipe.CODEC;
    }

    /**
     * 获取网络流编解码器。
     *
     * @return 配方网络流编解码器
     */
    @Override
    public StreamCodec<RegistryFriendlyByteBuf, FluidCollisionRecipe> streamCodec() {
        return FluidCollisionRecipe.STREAM_CODEC;
    }
}
