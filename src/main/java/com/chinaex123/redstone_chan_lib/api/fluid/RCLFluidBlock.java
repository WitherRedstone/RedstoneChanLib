package com.chinaex123.redstone_chan_lib.api.fluid;

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * 模组流体方块。
 * <p>
 * 继承原版液体方块，作为本模组注册的各类流体方块的通用类型，
 * 仅封装构造参数，行为完全沿用原版液体方块。
 */
public class RCLFluidBlock extends LiquidBlock {

    /**
     * 构造模组流体方块。
     *
     * @param fluid      关联的流动流体
     * @param properties 方块属性
     */
    public RCLFluidBlock(FlowingFluid fluid, BlockBehaviour.Properties properties) {
        super(fluid, properties);
    }
}
