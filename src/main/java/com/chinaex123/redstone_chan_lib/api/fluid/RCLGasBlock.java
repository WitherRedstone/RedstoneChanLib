package com.chinaex123.redstone_chan_lib.api.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

/**
 * 模组气体方块。
 * <p>
 * 继承原版液体方块，作为本模组气体流体的方块形态：
 * 仅在源高度允许实体站立于其上半格，其余情况无碰撞；
 * 放置后经过设定的消散刻数自动移除，上方被遮挡时延后处理。
 */
public class RCLGasBlock extends LiquidBlock {

    /** 实体能站立的上半格碰撞形状。 */
    public static final VoxelShape COLLISION_SHAPE = Block.box(0.0D, 8.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    /** 消散前存在的刻数。 */
    private final int dissipateTicks;

    /**
     * 创建气体方块。
     *
     * @param fluid          源流体
     * @param properties     方块参数
     * @param dissipateTicks 消散前存在的刻数
     */
    public RCLGasBlock(FlowingFluid fluid, BlockBehaviour.Properties properties, int dissipateTicks) {
        super(fluid, properties);
        this.dissipateTicks = dissipateTicks;
    }

    /**
     * 获取碰撞形状。
     * <p>
     * 实体碰撞上下文下，若实体位于方块上方、方块为源高度且可站在其下方流体之上，
     * 则返回上半格碰撞形状，否则无碰撞；非实体上下文一律无碰撞。
     *
     * @param state   方块状态
     * @param level   方块获取器
     * @param pos     方块位置
     * @param context 碰撞上下文
     * @return 碰撞形状
     */
    @Override
    protected @NotNull VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext) {
            return entityContext.isAbove(COLLISION_SHAPE, pos, false)
                    && state.getValue(LEVEL) == 0
                    && entityContext.canStandOnFluid(level.getFluidState(pos.below()), state.getFluidState())
                    ? COLLISION_SHAPE
                    : Shapes.empty();
        }
        return Shapes.empty();
    }

    /**
     * 放置方块时调度消散刻。
     * <p>
     * 服务端下按消散刻数调度方块刻。
     *
     * @param state          方块状态
     * @param level          世界
     * @param pos            方块位置
     * @param oldState       旧方块状态
     * @param movedByPiston  是否由活塞移动
     */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, this.dissipateTicks);
        }
    }

    /**
     * 方块刻更新。
     * <p>
     * 上方非空气时延后 20 刻再检查，否则移除本方块。
     *
     * @param state  方块状态
     * @param level  服务端世界
     * @param pos    方块位置
     * @param random 随机源
     */
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.getBlockState(pos.above()).isAir()) {
            level.scheduleTick(pos, this, 20);
            return;
        }
        level.removeBlock(pos, false);
    }
}
