package com.chinaex123.redstone_chan_lib.api.collision;

import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

/**
 * 流体状态匹配工具类。
 * <p>
 * 用于将流体状态与流体原料进行匹配：以假想流体量构造流体堆叠进行测试，
 * 若当前为流动流体则额外用其对应的源流体再测试一次，
 * 使流体原料既能匹配源方块也能匹配流动形态。
 */
public final class FluidStates {

    /** 匹配时使用的假想流体量。 */
    private static final int TEST_AMOUNT = 1000;

    private FluidStates() {
    }

    /**
     * 判断流体状态是否匹配流体原料。
     * <p>
     * 空流体不匹配；先以当前流体类型测试，若是流动流体
     * 且其源流体与自身不同，再用源流体测试一次。
     *
     * @param ingredient 流体原料
     * @param state      流体状态
     * @return 匹配返回 true
     */
    public static boolean matches(FluidIngredient ingredient, FluidState state) {
        if (state.isEmpty()) {
            return false;
        }
        Fluid type = state.getType();
        if (ingredient.test(new FluidStack(type, TEST_AMOUNT))) {
            return true;
        }
        if (type instanceof FlowingFluid flowing) {
            Fluid source = flowing.getSource();
            return source != type && ingredient.test(new FluidStack(source, TEST_AMOUNT));
        }
        return false;
    }
}
