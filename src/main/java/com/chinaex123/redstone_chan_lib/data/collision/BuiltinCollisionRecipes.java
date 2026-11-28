package com.chinaex123.redstone_chan_lib.data.collision;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import com.chinaex123.redstone_chan_lib.api.collision.BelowCondition;
import com.chinaex123.redstone_chan_lib.api.collision.BlockCollisionRecipe;
import com.chinaex123.redstone_chan_lib.api.collision.CollisionOutput;
import com.chinaex123.redstone_chan_lib.api.collision.CollisionRecipes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

import java.util.List;
import java.util.Optional;

/**
 * 内置的碰撞配方示例。
 * <p>
 * 这些配方用 Java 注册，效果与放在数据包里的 JSON 完全一样，
 * 用来演示“怎么在代码里写配方”，也给不想写 JSON 的整合包作者做参考。
 * 不需要的话可以在构造阶段不调用 {@link #register()}。
 */
public final class BuiltinCollisionRecipes {

    private BuiltinCollisionRecipes() {
    }

    /**
     * 注册全部内置示例配方。
     */
    public static void register() {
        // 熔岩碰到钻石矿石：百分百掉一颗钻石
        CollisionRecipes.register(RedstoneChanLIB.id("lava_diamond_ore"),
                new BlockCollisionRecipe(
                        FluidIngredient.of(Fluids.LAVA),
                        BlockCollisionRecipe.blocks(Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE),
                        List.of(CollisionOutput.item(Items.DIAMOND, 1, 1.0F)),
                        1, false, true,
                        Optional.empty()
                ));

        // 熔岩碰到钻石块：再放一个钻石块
        CollisionRecipes.register(RedstoneChanLIB.id("lava_diamond_block"),
                new BlockCollisionRecipe(
                        FluidIngredient.of(Fluids.LAVA),
                        BlockCollisionRecipe.blocks(Blocks.DIAMOND_BLOCK),
                        List.of(CollisionOutput.block(Blocks.DIAMOND_BLOCK, 1, 1.0F)),
                        1, false, false,
                        Optional.empty()
                ));

        // 熔岩碰到远古残骸：一半概率还是远古残骸，四分之一下界石英矿石，四分之一下界岩
        CollisionRecipes.register(RedstoneChanLIB.id("lava_ancient_debris"),
                new BlockCollisionRecipe(
                        FluidIngredient.of(Fluids.LAVA),
                        BlockCollisionRecipe.blocks(Blocks.ANCIENT_DEBRIS),
                        List.of(
                                CollisionOutput.block(Blocks.ANCIENT_DEBRIS, 1, 0.5F),
                                CollisionOutput.block(Blocks.NETHER_QUARTZ_ORE, 1, 0.25F),
                                CollisionOutput.block(Blocks.NETHERRACK, 1, 0.25F)
                        ),
                        1, false, false,
                        SoundEvents.LAVA_EXTINGUISH
                ));

        // 熔岩碰到金块，且中间那格空的下方也是金块时，才把流体那一格变成金矿石
        CollisionRecipes.register(RedstoneChanLIB.id("lava_gold_block"),
                new BlockCollisionRecipe(
                        FluidIngredient.of(Fluids.LAVA),
                        BlockCollisionRecipe.blocks(Blocks.GOLD_BLOCK),
                        List.of(CollisionOutput.block(Blocks.GOLD_ORE, 1, 1.0F)),
                        1, false, false, false, null,
                        BelowCondition.center(Blocks.GOLD_BLOCK)
                ));

        // 熔岩碰到石头，且熔岩自己脚下是岩浆块时，才把流体那一格变成岩浆块
        CollisionRecipes.register(RedstoneChanLIB.id("lava_stone_on_magma"),
                new BlockCollisionRecipe(
                        FluidIngredient.of(Fluids.LAVA),
                        BlockCollisionRecipe.blocks(Blocks.STONE),
                        List.of(CollisionOutput.block(Blocks.MAGMA_BLOCK, 1, 1.0F)),
                        1, false, false, false, null,
                        BelowCondition.down(Blocks.MAGMA_BLOCK)
                ));

    }
}
