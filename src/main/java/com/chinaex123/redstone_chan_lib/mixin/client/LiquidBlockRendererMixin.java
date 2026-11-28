package com.chinaex123.redstone_chan_lib.mixin.client;

import com.chinaex123.redstone_chan_lib.api.fluid.RCLGasFluid;
import com.chinaex123.redstone_chan_lib.client.fluid.GasFluidRenderer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 液体方块渲染器混入类。
 * <p>
 * 在液体方块网格生成前拦截：若流体为本模组的气体流体，
 * 则改用气体流体渲染器绘制并取消原版渲染逻辑。
 */
@Mixin(LiquidBlockRenderer.class)
public class LiquidBlockRendererMixin {

    /**
     * 拦截液体方块的网格生成。
     * <p>
     * 流体为气体流体时交由气体流体渲染器处理，并取消原逻辑。
     *
     * @param level     方块与着色获取器
     * @param pos       方块位置
     * @param buffer    顶点消费者
     * @param blockState 方块状态
     * @param fluidState 流体状态
     * @param callback  回调信息
     */
    @Inject(method = "tesselate", at = @At("HEAD"), cancellable = true)
    private void redstoneChanLib$renderGas(BlockAndTintGetter level, BlockPos pos, VertexConsumer buffer,
                                           BlockState blockState, FluidState fluidState, CallbackInfo callback) {
        if (fluidState.getType() instanceof RCLGasFluid) {
            GasFluidRenderer.render(level, pos, buffer, blockState, fluidState);
            callback.cancel();
        }
    }
}
