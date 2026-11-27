package com.chinaex123.redstone_chan_lib.block;

import com.chinaex123.redstone_chan_lib.api.portal.PortalDefinition;
import com.chinaex123.redstone_chan_lib.api.portal.PortalDefinitions;
import com.chinaex123.redstone_chan_lib.util.PortalTeleportManager;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 自定义传送门方块。
 * <p>
 * 传送门的实体方块，具有平面法线轴属性与对应的薄片碰撞箱。
 * 玩家进入时在服务端启动传送倒计时，方块实体保存所属传送门定义；
 * 方块名称取自默认传送门定义。
 */
public class CustomPortal extends Block implements EntityBlock {

    /** 平面法线轴属性。 */
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    /** X 轴平面的碰撞箱。 */
    private static final VoxelShape X_AXIS_AABB = Block.box(6.0D, 0.0D, 0.0D, 10.0D, 16.0D, 16.0D);
    /** Z 轴平面的碰撞箱。 */
    private static final VoxelShape Z_AXIS_AABB = Block.box(0.0D, 0.0D, 6.0D, 16.0D, 16.0D, 10.0D);
    /** Y 轴平面（横门）的碰撞箱。 */
    private static final VoxelShape Y_AXIS_AABB = Block.box(0.0D, 7.0D, 0.0D, 16.0D, 9.0D, 16.0D);

    /**
     * 构造自定义传送门方块。
     *
     * @param properties 方块属性
     */
    public CustomPortal(Properties properties) {
        super(properties.noOcclusion());
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    /**
     * 创建方块状态定义。
     *
     * @param builder 状态定义构建器
     */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    /**
     * 判断能否被流体替换。
     *
     * @param state 方块状态
     * @param fluid 流体
     * @return 恒为 false
     */
    @Override
    protected boolean canBeReplaced(BlockState state, net.minecraft.world.level.material.Fluid fluid) {
        return false;
    }

    /**
     * 获取方块碰撞箱。
     * <p>
     * 根据平面法线轴返回对应的薄片形状。
     *
     * @param state   方块状态
     * @param level   方块获取器
     * @param pos     方块位置
     * @param context 碰撞上下文
     * @return 碰撞箱
     */
    @Override
    public @NotNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(AXIS)) {
            case X -> X_AXIS_AABB;
            case Z -> Z_AXIS_AABB;
            case Y -> Y_AXIS_AABB;
        };
    }

    /**
     * 获取放置时的方块状态。
     * <p>
     * 放置时让门面朝向玩家视角的水平方向。
     *
     * @param context 放置上下文
     * @return 放置后的方块状态
     */
    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 放置时让门面朝向玩家视角的水平方向
        return this.defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getClockWise().getAxis());
    }

    /**
     * 实体进入方块时调用。
     * <p>
     * 仅在服务端为玩家启动传送倒计时，客户端表现由客户端遮罩自行处理。
     *
     * @param state  方块状态
     * @param level  世界
     * @param pos    方块位置
     * @param entity 进入的实体
     */
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // 只在服务端为玩家启动倒计时，客户端表现由客户端遮罩自行处理
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            PortalTeleportManager.beginPortalTeleport(player, pos);
        }
    }

    /**
     * 创建方块实体。
     *
     * @param pos   方块位置
     * @param state 方块状态
     * @return 传送门方块实体
     */
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PortalBlockEntity(pos, state);
    }

    /**
     * 获取方块编解码器。
     *
     * @return 方块编解码器
     */
    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(CustomPortal::new);
    }

    /**
     * 获取方块名称。
     * <p>
     * 优先使用默认传送门定义的名称，无定义时回退到默认名称。
     *
     * @return 方块名称
     */
    @Override
    public MutableComponent getName() {
        PortalDefinition definition = PortalDefinitions.defaultDefinition();
        return definition != null ? definition.name().copy() : super.getName();
    }
}
