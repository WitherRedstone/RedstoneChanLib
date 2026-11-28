package com.chinaex123.redstone_chan_lib.mixin;

import com.chinaex123.redstone_chan_lib.api.collision.CollisionRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 流体方块混入类。
 * <p>
 * 在流体方块放置或更新时通知碰撞配方管理器，
 * 以便检测流体与周围流体、方块之间是否触发碰撞配方。
 */
@Mixin(LiquidBlock.class)
public abstract class LiquidBlockMixin {

    /**
     * 流体方块被放置或更新时触发。
     *
     * @param state     新的方块状态
     * @param level     世界
     * @param pos       位置
     * @param oldState  被替换掉的旧方块状态
     * @param isMoving  是否是流动造成的变化
     * @param callbackInfo 注入回调信息
     */
    @Inject(method = "onPlace", at = @At("HEAD"))
    private void redstoneChanLib$onFluidPlaced(BlockState state, Level level, BlockPos pos, BlockState oldState,
                                               boolean isMoving, CallbackInfo callbackInfo) {
        CollisionRecipes.onFluidPlaced(level, pos, state, oldState);
    }
}
