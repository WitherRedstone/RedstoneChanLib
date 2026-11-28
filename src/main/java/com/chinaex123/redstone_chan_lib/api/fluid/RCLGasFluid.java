package com.chinaex123.redstone_chan_lib.api.fluid;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.shorts.Short2BooleanMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * 模组气体流体基类。
 * <p>
 * 在机械动力的流动流体逻辑之上实现气体行为：优先向上扩散，
 * 遇到上方可通行空间即上升，扩散范围与衰减由流体描述配置；
 * 流动方向、高度与形状按气体特性调整，并提供源流体与流动流体两个子类。
 */
public abstract class RCLGasFluid extends FlowingFluid {

    /** 注册期间占位的绑定对象，源与流动两个子类都要用。 */
    protected final FluidBinding binding;

    /**
     * 构造气体流体。
     *
     * @param binding 流体绑定对象
     */
    protected RCLGasFluid(FluidBinding binding) {
        this.binding = binding;
    }

    /**
     * 判断该流体能否形成无限源。
     *
     * @param level 世界
     * @return 可以形成无限源返回 true
     */
    @Override
    protected boolean canConvertToSource(Level level) {
        return this.binding.definition().canConvertToSource();
    }

    /**
     * 破坏方块前的处理。
     * <p>
     * 气体流体不处理方块破坏，留空实现。
     *
     * @param level 世界访问器
     * @param pos   方块位置
     * @param state 方块状态
     */
    @Override
    protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
    }

    /**
     * 获取寻找斜坡的距离。
     *
     * @param level 方块读取器
     * @return 寻找斜坡的距离
     */
    @Override
    protected int getSlopeFindDistance(LevelReader level) {
        return this.binding.definition().slopeFindDistance();
    }

    /**
     * 获取每格流体的等级衰减。
     *
     * @param level 方块读取器
     * @return 每格等级衰减
     */
    @Override
    protected int getDropOff(LevelReader level) {
        return this.binding.definition().levelDecreasePerBlock();
    }

    /**
     * 获取流体状态的流体量。
     *
     * @param state 流体状态
     * @return 流体量，空流体返回 0
     */
    @Override
    public int getAmount(FluidState state) {
        if (state.isEmpty()) {
            return 0;
        }
        return state.getValue(LEVEL);
    }

    /**
     * 判断给定流体是否与本流体同类。
     * <p>
     * 本流体的源流体或流动流体均视为同类。
     *
     * @param fluid 流体
     * @return 同类返回 true
     */
    @Override
    public boolean isSame(Fluid fluid) {
        return fluid == this.binding.source() || fluid == this.binding.flowing();
    }

    /**
     * 获取对应桶物品。
     *
     * @return 桶物品
     */
    @Override
    public @NotNull Item getBucket() {
        return this.binding.bucket();
    }

    /**
     * 获取流体类型。
     *
     * @return 流体类型
     */
    @Override
    public @NotNull FluidType getFluidType() {
        return this.binding.type();
    }

    /**
     * 判断能否被指定流体替换。
     * <p>
     * 仅允许被从上方来的异种流体替换。
     *
     * @param state     当前流体状态
     * @param level     方块获取器
     * @param pos       方块位置
     * @param fluid     待替换的流体
     * @param direction 替换方向
     * @return 可替换返回 true
     */
    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid, Direction direction) {
        return direction == Direction.UP && !fluid.isSame(this);
    }

    /**
     * 获取流体更新刻率。
     *
     * @param level 方块读取器
     * @return 更新刻率
     */
    @Override
    public int getTickDelay(LevelReader level) {
        return this.binding.definition().tickRate();
    }

    /**
     * 获取流体爆炸抗性。
     *
     * @return 爆炸抗性
     */
    @Override
    public float getExplosionResistance() {
        return this.binding.definition().explosionResistance();
    }

    /**
     * 创建对应的传统方块状态。
     *
     * @param state 流体状态
     * @return 带流体等级的方块状态
     */
    @Override
    public @NotNull BlockState createLegacyBlock(FluidState state) {
        return this.binding.block().defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }

    /**
     * 扩散逻辑。
     * <p>
     * 优先向上扩散，若可向上流动则上升并在满足条件时向四周扩散；
     * 否则在源状态或非气体孔洞情况下向四周扩散。
     *
     * @param level 世界
     * @param pos   方块位置
     * @param state 流体状态
     */
    @Override
    protected void spread(Level level, BlockPos pos, FluidState state) {
        if (state.isEmpty()) {
            return;
        }
        BlockState blockState = level.getBlockState(pos);
        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        FluidState newFluid = this.getNewLiquid(level, abovePos, aboveState);
        if (this.canSpreadTo(level, pos, blockState, Direction.UP, abovePos, aboveState,
                level.getFluidState(abovePos), newFluid.getType())) {
            this.spreadTo(level, abovePos, aboveState, Direction.UP, newFluid);
            if (this.sourceNeighborCount(level, pos) >= 3) {
                this.spreadToSides(level, pos, state, blockState);
            }
        } else if (state.isSource() || !this.isGasHole(level, newFluid.getType(), pos, blockState, abovePos, aboveState)) {
            this.spreadToSides(level, pos, state, blockState);
        }
    }

    /**
     * 计算新的流体状态。
     * <p>
     * 水平方向存在两个以上同种源方块且下方为固体或同类源时生成源；
     * 下方为同类流体时生成向下流动；否则按最大邻接量衰减得到流动流体。
     *
     * @param level      世界
     * @param pos        方块位置
     * @param blockState 方块状态
     * @return 新的流体状态
     */
    @Override
    protected FluidState getNewLiquid(Level level, BlockPos pos, BlockState blockState) {
        int maxAmount = 0;
        int sourceCount = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);
            FluidState neighborFluid = neighborState.getFluidState();
            if (neighborFluid.getType().isSame(this)
                    && this.canPassThroughWall(direction, level, pos, blockState, neighborPos, neighborState)) {
                if (neighborFluid.isSource() && net.neoforged.neoforge.event.EventHooks.canCreateFluidSource(level, neighborPos, neighborState)) {
                    sourceCount++;
                }
                maxAmount = Math.max(maxAmount, neighborFluid.getAmount());
            }
        }
        if (sourceCount >= 2) {
            BlockState belowState = level.getBlockState(pos.below());
            FluidState belowFluid = belowState.getFluidState();
            if (belowState.isSolid() || this.isSourceBlockOfThisType(belowFluid)) {
                return this.getSource(false);
            }
        }
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        FluidState belowFluid = belowState.getFluidState();
        if (!belowFluid.isEmpty() && belowFluid.getType().isSame(this)
                && this.canPassThroughWall(Direction.DOWN, level, pos, blockState, belowPos, belowState)) {
            return this.getFlowing(8, true);
        }
        int amount = maxAmount - this.getDropOff(level);
        return amount <= 0 ? Fluids.EMPTY.defaultFluidState() : this.getFlowing(amount, false);
    }

    /**
     * 计算流体流动方向。
     * <p>
     * 依据水平邻接流体的高度差累加方向；处于下落状态且侧面为固体时
     * 额外加入向上分量，最后归一化返回。
     *
     * @param level     方块获取器
     * @param pos       方块位置
     * @param fluidState 流体状态
     * @return 流动方向向量
     */
    @Override
    public Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState fluidState) {
        double x = 0.0D;
        double z = 0.0D;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            mutable.setWithOffset(pos, direction);
            FluidState neighborFluid = level.getFluidState(mutable);
            if (this.affectsFlow(neighborFluid)) {
                float ownHeight = neighborFluid.getOwnHeight();
                float diff = 0.0F;
                if (ownHeight == 0.0F) {
                    if (!level.getBlockState(mutable).blocksMotion()) {
                        FluidState aboveFluid = level.getFluidState(mutable.above());
                        if (this.affectsFlow(aboveFluid)) {
                            ownHeight = aboveFluid.getOwnHeight();
                            if (ownHeight > 0.0F) {
                                diff = fluidState.getOwnHeight() - (ownHeight - 0.8888889F);
                            }
                        }
                    }
                } else if (ownHeight > 0.0F) {
                    diff = fluidState.getOwnHeight() - ownHeight;
                }
                if (diff != 0.0F) {
                    x += direction.getStepX() * diff;
                    z += direction.getStepZ() * diff;
                }
            }
        }
        Vec3 flow = new Vec3(x, 0.0D, z);
        if (fluidState.getValue(FALLING)) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                mutable.setWithOffset(pos, direction);
                if (this.isSolidFace(level, mutable, direction)
                        || this.isSolidFace(level, mutable.above(), direction)) {
                    flow = flow.normalize().add(0.0D, 6.0D, 0.0D);
                    break;
                }
            }
        }
        return flow.normalize();
    }

    /**
     * 获取流体高度。
     * <p>
     * 源流体或下方为同类气体时高度为 1，否则使用自身高度。
     *
     * @param state 流体状态
     * @param level 方块获取器
     * @param pos   方块位置
     * @return 流体高度
     */
    @Override
    public float getHeight(FluidState state, BlockGetter level, BlockPos pos) {
        return state.isSource() || this.isSameGasBelow(state, level, pos) ? 1.0F : state.getOwnHeight();
    }

    /**
     * 获取流体形状。
     * <p>
     * 满量且下方为同类气体时返回完整方块形状，否则返回从顶部向下计算高度的盒体。
     *
     * @param state 流体状态
     * @param level 方块获取器
     * @param pos   方块位置
     * @return 流体形状
     */
    @Override
    public @NotNull VoxelShape getShape(FluidState state, BlockGetter level, BlockPos pos) {
        if (state.getAmount() >= 8 && this.isSameGasBelow(state, level, pos)) {
            return Shapes.block();
        }
        float height = this.getHeight(state, level, pos);
        return Shapes.box(0.0D, 1.0D - height, 0.0D, 1.0D, 1.0D, 1.0D);
    }

    /**
     * 判断能否穿过指定位置扩散气体。
     * <p>
     * 目标非同类源、可穿过墙面且能容纳流体时返回 true。
     *
     * @param level      方块获取器
     * @param fluid      流体
     * @param pos        当前位置
     * @param state      当前方块状态
     * @param direction  扩散方向
     * @param spreadPos  扩散位置
     * @param spreadState 扩散方块状态
     * @param spreadFluid 扩散流体状态
     * @return 可穿过返回 true
     */
    private boolean canPassThroughGas(BlockGetter level, Fluid fluid, BlockPos pos, BlockState state, Direction direction,
                                      BlockPos spreadPos, BlockState spreadState, FluidState spreadFluid) {
        return !this.isSourceBlockOfThisType(spreadFluid)
                && this.canPassThroughWall(direction, level, pos, state, spreadPos, spreadState)
                && this.canHoldFluid(level, spreadPos, spreadState, fluid);
    }

    /**
     * 计算可扩散方向及对应流体状态。
     * <p>
     * 对每个水平方向判断能否扩散并计算到气体孔洞的坡度距离，
     * 保留距离最短的方向集合；借助缓存避免重复查询方块状态与孔洞判定。
     *
     * @param level 世界
     * @param pos   方块位置
     * @param state 方块状态
     * @return 方向到流体状态的映射
     */
    @Override
    protected Map<Direction, FluidState> getSpread(Level level, BlockPos pos, BlockState state) {
        int bestDistance = 1000;
        Map<Direction, FluidState> result = Maps.newEnumMap(Direction.class);
        Short2ObjectMap<Pair<BlockState, FluidState>> stateCache = new Short2ObjectOpenHashMap<>();
        Short2BooleanMap gasHoleCache = new Short2BooleanOpenHashMap();
        BlockState ownState = level.getBlockState(pos);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(direction);
            short key = getCacheKey(pos, neighborPos);
            Pair<BlockState, FluidState> pair = stateCache.computeIfAbsent(key, k -> {
                BlockState neighborState = level.getBlockState(neighborPos);
                return Pair.of(neighborState, neighborState.getFluidState());
            });
            BlockState neighborState = pair.getFirst();
            FluidState neighborFluid = pair.getSecond();
            FluidState newFluid = this.getNewLiquid(level, neighborPos, neighborState);
            if (!this.canPassThroughGas(level, newFluid.getType(), pos, ownState, direction, neighborPos, neighborState, neighborFluid)) {
                continue;
            }
            int distance = gasHoleCache.computeIfAbsent(key, k -> {
                BlockPos abovePos = neighborPos.above();
                return this.isGasHole(level, this.getFlowing(), neighborPos, neighborState, abovePos, level.getBlockState(abovePos));
            })
                    ? 0
                    : this.getGasSlopeDistance(level, this.getFlowing(), neighborPos, 1, direction.getOpposite(),
                    neighborState, pos, stateCache, gasHoleCache);
            if (distance < bestDistance) {
                result.clear();
            }
            if (distance <= bestDistance) {
                result.put(direction, newFluid);
                bestDistance = distance;
            }
        }
        return result;
    }

    /**
     * 递归计算到气体孔洞的坡度距离。
     * <p>
     * 沿水平方向逐层向外搜索可扩散位置，遇到气体孔洞即返回当前距离；
     * 在最大搜索距离内继续递归，返回最短距离。
     *
     * @param level        方块读取器
     * @param fluid        流体
     * @param spreadPos    扩散位置
     * @param distance     当前距离
     * @param fromDirection 来向（不回头搜索）
     * @param spreadState  扩散方块状态
     * @param sourcePos    源位置
     * @param stateCache   方块状态缓存
     * @param gasHoleCache 气体孔洞缓存
     * @return 到气体孔洞的最短坡度距离
     */
    private int getGasSlopeDistance(LevelReader level, Fluid fluid, BlockPos spreadPos, int distance, Direction fromDirection,
                                    BlockState spreadState, BlockPos sourcePos,
                                    Short2ObjectMap<Pair<BlockState, FluidState>> stateCache,
                                    Short2BooleanMap gasHoleCache) {
        int bestDistance = 1000;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (direction == fromDirection) {
                continue;
            }
            BlockPos neighborPos = spreadPos.relative(direction);
            short key = getCacheKey(sourcePos, neighborPos);
            Pair<BlockState, FluidState> pair = stateCache.computeIfAbsent(key, k -> {
                BlockState neighborState = level.getBlockState(neighborPos);
                return Pair.of(neighborState, neighborState.getFluidState());
            });
            BlockState neighborState = pair.getFirst();
            FluidState neighborFluid = pair.getSecond();
            if (!this.canPassThroughGas(level, fluid, spreadPos, spreadState, direction, neighborPos, neighborState, neighborFluid)) {
                continue;
            }
            boolean hole = gasHoleCache.computeIfAbsent(key, k -> {
                BlockPos abovePos = neighborPos.above();
                return this.isGasHole(level, fluid, neighborPos, neighborState, abovePos, level.getBlockState(abovePos));
            });
            if (hole) {
                return distance;
            }
            if (distance < this.getSlopeFindDistance(level)) {
                int next = this.getGasSlopeDistance(level, fluid, neighborPos, distance + 1, direction.getOpposite(),
                        neighborState, sourcePos, stateCache, gasHoleCache);
                if (next < bestDistance) {
                    bestDistance = next;
                }
            }
        }
        return bestDistance;
    }

    /**
     * 判断下方是否为同类气体。
     *
     * @param state 流体状态
     * @param level 方块获取器
     * @param pos   方块位置
     * @return 下方为同类气体返回 true
     */
    private boolean isSameGasBelow(FluidState state, BlockGetter level, BlockPos pos) {
        FluidState below = level.getFluidState(pos.below());
        return below.getType().isSame(state.getType());
    }

    /**
     * 生成位置对的缓存键。
     * <p>
     * 取水平方向上的相对偏移压缩为短整型键。
     *
     * @param from 起始位置
     * @param to   目标位置
     * @return 缓存键
     */
    private static short getCacheKey(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        return (short) (((dx + 128) & 0xFF) << 8 | ((dz + 128) & 0xFF));
    }

    /**
     * 判断指定位置是否为可上升的气体孔洞。
     * <p>
     * 要求可穿过上方墙面，且上方为同类气体或可容纳流体。
     *
     * @param level      方块获取器
     * @param fluid      流体
     * @param pos        当前位置
     * @param state      当前方块状态
     * @param spreadPos  上方位置
     * @param spreadState 上方方块状态
     * @return 是气体孔洞返回 true
     */
    private boolean isGasHole(BlockGetter level, Fluid fluid, BlockPos pos, BlockState state,
                              BlockPos spreadPos, BlockState spreadState) {
        if (!this.canPassThroughWall(Direction.UP, level, pos, state, spreadPos, spreadState)) {
            return false;
        }
        return spreadState.getFluidState().getType().isSame(this)
                || this.canHoldFluid(level, spreadPos, spreadState, fluid);
    }

    /**
     * 气体源流体。
     */
    public static final class Source extends RCLGasFluid {

        /**
         * 构造气体源流体。
         *
         * @param binding 流体绑定对象
         */
        public Source(FluidBinding binding) {
            super(binding);
        }

        /**
         * 获取对应的流动流体。
         *
         * @return 流动流体
         */
        @Override
        public Fluid getFlowing() {
            return this.binding.flowing();
        }

        /**
         * 获取对应的源流体。
         *
         * @return 源流体
         */
        @Override
        public Fluid getSource() {
            return this.binding.source();
        }

        /**
         * 判断流体状态是否为源。
         *
         * @param state 流体状态
         * @return 恒为 true
         */
        @Override
        public boolean isSource(FluidState state) {
            return true;
        }

        /**
         * 创建流体状态定义。
         * <p>
         * 在父类基础上追加流体等级属性。
         *
         * @param builder 状态定义构建器
         */
        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }
    }

    /**
     * 气体流动流体。
     */
    public static final class Flowing extends RCLGasFluid {

        /**
         * 构造气体流动流体。
         *
         * @param binding 流体绑定对象
         */
        public Flowing(FluidBinding binding) {
            super(binding);
        }

        /**
         * 获取对应的流动流体。
         *
         * @return 流动流体
         */
        @Override
        public Fluid getFlowing() {
            return this.binding.flowing();
        }

        /**
         * 获取对应的源流体。
         *
         * @return 源流体
         */
        @Override
        public Fluid getSource() {
            return this.binding.source();
        }

        /**
         * 判断流体状态是否为源。
         *
         * @param state 流体状态
         * @return 恒为 false
         */
        @Override
        public boolean isSource(FluidState state) {
            return false;
        }

        /**
         * 创建流体状态定义。
         * <p>
         * 在父类基础上追加流体等级属性。
         *
         * @param builder 状态定义构建器
         */
        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }
    }
}
