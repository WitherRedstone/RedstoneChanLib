package com.chinaex123.redstone_chan_lib.compat.jei;

import com.chinaex123.redstone_chan_lib.api.collision.BlockCollisionRecipe;
import net.minecraft.resources.ResourceLocation;

/**
 * JEI 里展示用的一条方块碰撞配方。
 *
 * @param id     配方 id
 * @param recipe 配方内容
 */
public record BlockCollisionJeiRecipe(ResourceLocation id, BlockCollisionRecipe recipe) {
}
