package com.chinaex123.redstone_chan_lib.compat.jei;

import com.chinaex123.redstone_chan_lib.api.collision.FluidCollisionRecipe;
import net.minecraft.resources.ResourceLocation;

/**
 * JEI 里展示用的一条流体碰撞配方。
 * <p>
 * 数据包配方与代码注册的配方在 JEI 里用同一种结构表示，id 用于左下方的配方来源提示。
 *
 * @param id     配方 id
 * @param recipe 配方内容
 */
public record FluidCollisionJeiRecipe(ResourceLocation id, FluidCollisionRecipe recipe) {
}
