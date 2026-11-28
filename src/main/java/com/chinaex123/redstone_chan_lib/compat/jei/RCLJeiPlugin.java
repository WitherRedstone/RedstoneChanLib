package com.chinaex123.redstone_chan_lib.compat.jei;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import com.chinaex123.redstone_chan_lib.api.collision.BlockCollisionRecipe;
import com.chinaex123.redstone_chan_lib.api.collision.CollisionRecipes;
import com.chinaex123.redstone_chan_lib.api.collision.FluidCollisionRecipe;
import com.chinaex123.redstone_chan_lib.compat.jei.category.BlockCollisionCategory;
import com.chinaex123.redstone_chan_lib.compat.jei.category.FluidCollisionCategory;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class RCLJeiPlugin implements IModPlugin {

    /**
     * JEI 插件的唯一 id。
     */
    public static final ResourceLocation UID = RedstoneChanLIB.id("jei_plugin");

    /**
     * 获取插件 id。
     *
     * @return 插件 id
     */
    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    /**
     * 注册两个配方分类。
     *
     * @param registration 分类注册器
     */
    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new FluidCollisionCategory(registration.getJeiHelpers().getGuiHelper()),
                new BlockCollisionCategory(registration.getJeiHelpers().getGuiHelper())
        );
    }

    /**
     * 注册两个分类下的配方。
     *
     * @param registration 配方注册器
     */
    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(FluidCollisionCategory.TYPE, collectFluidRecipes());
        registration.addRecipes(BlockCollisionCategory.TYPE, collectBlockRecipes());
    }

    /**
     * 收集全部流体碰撞配方：数据包 + Java。
     *
     * @return 配方列表
     */
    private static List<FluidCollisionJeiRecipe> collectFluidRecipes() {
        List<FluidCollisionJeiRecipe> result = new ArrayList<>();
        RecipeManager manager = recipeManager();
        if (manager != null) {
            for (RecipeHolder<FluidCollisionRecipe> holder : manager.getAllRecipesFor(
                    com.chinaex123.redstone_chan_lib.init.RCLRecipes.FLUID_COLLISION_TYPE)) {
                result.add(new FluidCollisionJeiRecipe(holder.id(), holder.value()));
            }
        }
        CollisionRecipes.javaFluidRecipes().forEach((id, recipe) -> result.add(new FluidCollisionJeiRecipe(id, recipe)));
        return result;
    }

    /**
     * 收集全部方块碰撞配方：数据包 + Java。
     *
     * @return 配方列表
     */
    private static List<BlockCollisionJeiRecipe> collectBlockRecipes() {
        List<BlockCollisionJeiRecipe> result = new ArrayList<>();
        RecipeManager manager = recipeManager();
        if (manager != null) {
            for (RecipeHolder<BlockCollisionRecipe> holder : manager.getAllRecipesFor(
                    com.chinaex123.redstone_chan_lib.init.RCLRecipes.BLOCK_COLLISION_TYPE)) {
                result.add(new BlockCollisionJeiRecipe(holder.id(), holder.value()));
            }
        }
        CollisionRecipes.javaBlockRecipes().forEach((id, recipe) -> result.add(new BlockCollisionJeiRecipe(id, recipe)));
        return result;
    }

    /**
     * 取当前世界的配方管理器，界面上没有世界时返回空。
     *
     * @return 配方管理器，可为空
     */
    private static RecipeManager recipeManager() {
        var level = Minecraft.getInstance().level;
        return level == null ? null : level.getRecipeManager();
    }
}
