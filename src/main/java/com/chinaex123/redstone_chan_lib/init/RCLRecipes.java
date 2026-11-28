package com.chinaex123.redstone_chan_lib.init;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import com.chinaex123.redstone_chan_lib.api.collision.BlockCollisionRecipe;
import com.chinaex123.redstone_chan_lib.api.collision.FluidCollisionRecipe;
import com.chinaex123.redstone_chan_lib.crafting.serializer.BlockCollisionRecipeSerializer;
import com.chinaex123.redstone_chan_lib.crafting.serializer.FluidCollisionRecipeSerializer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public interface RCLRecipes {
    /** 配方序列化器注册表。 */
    DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, RedstoneChanLIB.MODID);

    /** 流体碰撞配方的 id。 */
    ResourceLocation FLUID_COLLISION_ID = RedstoneChanLIB.id("fluid_collision");
    /** 方块碰撞配方的 id。 */
    ResourceLocation BLOCK_COLLISION_ID = RedstoneChanLIB.id("block_collision");

    /** 流体碰撞配方类型。 */
    RecipeType<FluidCollisionRecipe> FLUID_COLLISION_TYPE = RecipeType.simple(FLUID_COLLISION_ID);
    /** 方块碰撞配方类型。 */
    RecipeType<BlockCollisionRecipe> BLOCK_COLLISION_TYPE = RecipeType.simple(BLOCK_COLLISION_ID);

    /** 流体碰撞配方序列化器。 */
    DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FluidCollisionRecipe>> FLUID_COLLISION_SERIALIZER =
            RECIPE_SERIALIZERS.register("fluid_collision", FluidCollisionRecipeSerializer::new);
    /** 方块碰撞配方序列化器。 */
    DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BlockCollisionRecipe>> BLOCK_COLLISION_SERIALIZER =
            RECIPE_SERIALIZERS.register("block_collision", BlockCollisionRecipeSerializer::new);

    static void register(IEventBus bus) {
        RECIPE_SERIALIZERS.register(bus);
    }
}
