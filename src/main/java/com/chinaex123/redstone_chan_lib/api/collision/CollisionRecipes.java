package com.chinaex123.redstone_chan_lib.api.collision;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import com.chinaex123.redstone_chan_lib.world.CollisionScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 碰撞配方管理器。
 * <p>
 * 维护流体碰撞与方块碰撞配方（数据包配方与 Java 侧注册配方），
 * 在流体放置时检测其与周围流体、方块的碰撞，匹配配方后按配置
 * 调度延迟产出、消耗指定格子并施加冷却，同时管理待执行任务队列。
 */
public final class CollisionRecipes {

    /** Java 侧注册的流体碰撞配方。 */
    private static final Map<ResourceLocation, FluidCollisionRecipe> JAVA_FLUID = new LinkedHashMap<>();
    /** Java 侧注册的方块碰撞配方。 */
    private static final Map<ResourceLocation, BlockCollisionRecipe> JAVA_BLOCK = new LinkedHashMap<>();
    /** 触发冷却记录，键是维度与坐标组合成的字符串。 */
    private static final Map<String, Long> COOLDOWN = new LinkedHashMap<>();

    /** 冷却时长（刻）。 */
    private static final long COOLDOWN_TICKS = 20L;

    /** 待执行的产出任务。 */
    private static final List<CollisionScheduler.Task> TASKS = new CopyOnWriteArrayList<>();

    private CollisionRecipes() {
    }

    /**
     * 以完整标识注册流体碰撞配方。
     *
     * @param id     配方标识
     * @param recipe 流体碰撞配方
     */
    public static void register(ResourceLocation id, FluidCollisionRecipe recipe) {
        JAVA_FLUID.put(id, recipe);
    }

    /**
     * 以路径注册流体碰撞配方，标识使用本模组命名空间。
     *
     * @param path   标识路径
     * @param recipe 流体碰撞配方
     */
    public static void register(String path, FluidCollisionRecipe recipe) {
        register(RedstoneChanLIB.id(path), recipe);
    }

    /**
     * 以完整标识注册方块碰撞配方。
     *
     * @param id     配方标识
     * @param recipe 方块碰撞配方
     */
    public static void register(ResourceLocation id, BlockCollisionRecipe recipe) {
        JAVA_BLOCK.put(id, recipe);
    }

    /**
     * 以路径注册方块碰撞配方，标识使用本模组命名空间。
     *
     * @param path   标识路径
     * @param recipe 方块碰撞配方
     */
    public static void register(String path, BlockCollisionRecipe recipe) {
        register(RedstoneChanLIB.id(path), recipe);
    }

    /**
     * 获取 Java 侧流体碰撞配方的只读副本。
     *
     * @return 流体碰撞配方映射
     */
    public static Map<ResourceLocation, FluidCollisionRecipe> javaFluidRecipes() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(JAVA_FLUID));
    }

    /**
     * 获取 Java 侧方块碰撞配方的只读副本。
     *
     * @return 方块碰撞配方映射
     */
    public static Map<ResourceLocation, BlockCollisionRecipe> javaBlockRecipes() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(JAVA_BLOCK));
    }

    /**
     * 清除全部 Java 侧配方。
     */
    public static void clearJavaRecipes() {
        JAVA_FLUID.clear();
        JAVA_BLOCK.clear();
    }

    /**
     * 处理流体放置事件，检测碰撞。
     * <p>
     * 仅服务端且新状态含流体时处理；同一流体仅液面高度变化不算碰撞。
     * 对六个方向依次检查相邻与隔一格的位置：遇到异种流体尝试流体碰撞，
     * 遇到方块尝试方块碰撞。
     *
     * @param level    世界
     * @param pos      流体位置
     * @param newState 新方块状态
     * @param oldState 旧方块状态
     */
    public static void onFluidPlaced(Level level, BlockPos pos, BlockState newState, BlockState oldState) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        FluidState fluidState = newState.getFluidState();
        if (fluidState.isEmpty()) {
            return;
        }
        FluidState oldFluid = oldState.getFluidState();
        // 同一种流体只是液面高度变了，不算碰撞
        if (!oldFluid.isEmpty() && oldFluid.getType().isSame(fluidState.getType())) {
            return;
        }

        for (Direction direction : Direction.values()) {
            BlockPos nearPos = pos.relative(direction);
            if (!level.isLoaded(nearPos)) {
                continue;
            }
            BlockState nearState = level.getBlockState(nearPos);
            FluidState nearFluid = nearState.getFluidState();
            if (!nearFluid.isEmpty()) {
                if (!nearFluid.getType().isSame(fluidState.getType())) {
                    triggerFluid(serverLevel, pos, fluidState, nearFluid, nearPos, pos);
                }
                continue;
            }
            if (!nearState.isAir()) {
                triggerBlock(serverLevel, pos, fluidState, nearState, nearPos, pos);
                continue;
            }
            BlockPos farPos = nearPos.relative(direction);
            if (!level.isLoaded(farPos)) {
                continue;
            }
            BlockState farState = level.getBlockState(farPos);
            FluidState farFluid = farState.getFluidState();
            if (!farFluid.isEmpty()) {
                if (!farFluid.getType().isSame(fluidState.getType())) {
                    triggerFluid(serverLevel, pos, fluidState, farFluid, farPos, nearPos);
                }
            } else if (!farState.isAir()) {
                triggerBlock(serverLevel, pos, fluidState, farState, farPos, nearPos);
            }
        }
    }

    /**
     * 判断“下方方块”这个附加条件是否满足。
     *
     * @param level     服务端世界
     * @param condition 条件
     * @param pos       流入流体的位置
     * @param otherPos  碰撞另一方的位置
     * @param middlePos 中间空格的位置（相邻碰撞时为 null）
     * @return 满足（或没有限制）返回 true
     */
    private static boolean belowMatches(ServerLevel level, BelowCondition condition, BlockPos pos,
                                        BlockPos otherPos, BlockPos middlePos) {
        if (condition.isEmpty()) {
            return true;
        }
        // fluid 看流体自己脚下
        if (!condition.testFluid(level.getBlockState(pos.below()))) {
            return false;
        }
        // down 看对方（右侧那格方块/流体）的正下方
        if (!condition.testDown(level.getBlockState(otherPos.below()))) {
            return false;
        }
        // center 看碰撞中间那个空格的下方
        return condition.testCenter(level.getBlockState(middlePos.below()));
    }

    /**
     * 尝试触发流体碰撞。
     * <p>
     * 冷却未结束则跳过；依次匹配数据包配方与 Java 侧配方，
     * 首个满足流体匹配与下方条件的配方被调度并返回。
     *
     * @param level    服务端世界
     * @param pos      流入流体位置
     * @param incoming 流入流体状态
     * @param other    另一方流体状态
     * @param otherPos 另一方流体位置
     * @param middlePos 中间空格位置
     */
    private static void triggerFluid(ServerLevel level, BlockPos pos, FluidState incoming, FluidState other,
                                     BlockPos otherPos, BlockPos middlePos) {
        if (!isReady(level, middlePos)) {
            return;
        }
        for (RecipeHolder<FluidCollisionRecipe> holder : level.getRecipeManager().getAllRecipesFor(
                com.chinaex123.redstone_chan_lib.init.RCLRecipes.FLUID_COLLISION_TYPE)) {
            FluidCollisionRecipe recipe = holder.value();
            if (recipe.testFluid(incoming) && recipe.testOtherFluid(other)
                    && belowMatches(level, recipe.below(), pos, otherPos, middlePos)) {
                scheduleFluid(level, middlePos, pos, recipe, otherPos);
                return;
            }
        }
        for (FluidCollisionRecipe recipe : JAVA_FLUID.values()) {
            if (recipe.testFluid(incoming) && recipe.testOtherFluid(other)
                    && belowMatches(level, recipe.below(), pos, otherPos, middlePos)) {
                scheduleFluid(level, middlePos, pos, recipe, otherPos);
                return;
            }
        }
    }

    /**
     * 安排一条流体碰撞的产出，并按配置决定要清掉哪些格子。
     *
     * @param level    服务端世界
     * @param middlePos 中间空格的位置（流体碰撞的产物就落在这里）
     * @param pos      触发位置
     * @param recipe   命中的配方
     * @param otherPos 另一方流体的位置
     */
    private static void scheduleFluid(ServerLevel level, BlockPos middlePos, BlockPos pos, FluidCollisionRecipe recipe,
                                      BlockPos otherPos) {
        List<BlockPos> consume = new ArrayList<>(2);
        if (recipe.consumeFluid()) {
            consume.add(pos.immutable());
        }
        if (recipe.consumeOther() && !otherPos.equals(pos)) {
            consume.add(otherPos.immutable());
        }
        if (recipe.consumeSource()) {
            BlockPos source = findFluidSource(level, pos, level.getFluidState(pos));
            if (source != null && !consume.contains(source)) {
                consume.add(source);
            }
        }
        // 流体碰撞的产物落在中间那一格里（两种流体相遇的位置）
        schedule(level, middlePos, recipe.results(), recipe.delay(), recipe.sound(), consume, false);
        markCoolDown(level, middlePos);
    }

    /**
     * 尝试触发方块碰撞。
     * <p>
     * 冷却未结束则跳过；依次匹配数据包配方与 Java 侧配方，
     * 首个满足流体匹配、方块匹配与下方条件的配方被调度并返回。
     *
     * @param level     服务端世界
     * @param pos       流入流体位置
     * @param incoming  流入流体状态
     * @param blockState 碰撞方块状态
     * @param blockPos  碰撞方块位置
     * @param middlePos 中间空格位置
     */
    private static void triggerBlock(ServerLevel level, BlockPos pos, FluidState incoming, BlockState blockState,
                                     BlockPos blockPos, BlockPos middlePos) {
        if (!isReady(level, middlePos)) {
            return;
        }
        for (RecipeHolder<BlockCollisionRecipe> holder : level.getRecipeManager().getAllRecipesFor(
                com.chinaex123.redstone_chan_lib.init.RCLRecipes.BLOCK_COLLISION_TYPE)) {
            BlockCollisionRecipe recipe = holder.value();
            if (recipe.testFluid(incoming) && recipe.testBlock(blockState)
                    && belowMatches(level, recipe.below(), pos, blockPos, middlePos)) {
                scheduleBlock(level, middlePos, pos, recipe, blockPos);
                return;
            }
        }
        for (BlockCollisionRecipe recipe : JAVA_BLOCK.values()) {
            if (recipe.testFluid(incoming) && recipe.testBlock(blockState)
                    && belowMatches(level, recipe.below(), pos, blockPos, middlePos)) {
                scheduleBlock(level, middlePos, pos, recipe, blockPos);
                return;
            }
        }
    }

    /**
     * 从起始位置出发寻找同种流体的源方块。
     * <p>
     * 沿六个方向广度优先搜索，限制访问数量；找到第一个源方块即返回。
     *
     * @param level     服务端世界
     * @param start     起始位置
     * @param fluidState 流体状态
     * @return 源方块位置，未找到时返回 null
     */
    @Nullable
    private static BlockPos findFluidSource(ServerLevel level, BlockPos start, FluidState fluidState) {
        if (fluidState.isEmpty()) {
            return null;
        }
        Fluid type = fluidState.getType();
        Deque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        queue.add(start);
        seen.add(start);
        int budget = 256;
        while (!queue.isEmpty() && budget-- > 0) {
            BlockPos current = queue.poll();
            FluidState state = level.getFluidState(current);
            if (state.isEmpty() || !state.getType().isSame(type)) {
                continue;
            }
            if (state.isSource()) {
                return current.immutable();
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        return null;
    }

    /**
     * 安排一条方块碰撞的产出，并按配置决定要清掉哪些格子。
     *
     * @param level     服务端世界
     * @param middlePos 中间空格的位置
     * @param pos       触发位置
     * @param recipe    命中的配方
     * @param blockPos  碰撞方块位置
     */
    private static void scheduleBlock(ServerLevel level, BlockPos middlePos, BlockPos pos, BlockCollisionRecipe recipe,
                                      BlockPos blockPos) {
        List<BlockPos> consume = new ArrayList<>(2);
        if (recipe.consumeFluid()) {
            consume.add(pos.immutable());
        }
        if (recipe.consumeBlock() && !blockPos.equals(pos)) {
            consume.add(blockPos.immutable());
        }
        if (recipe.consumeSource()) {
            BlockPos source = findFluidSource(level, pos, level.getFluidState(pos));
            if (source != null && !consume.contains(source)) {
                consume.add(source);
            }
        }
        schedule(level, middlePos, recipe.results(), recipe.delay(), recipe.sound(), consume, true);
        markCoolDown(level, middlePos);
    }

    /**
     * 向任务队列添加一条延迟产出任务。
     *
     * @param level    服务端世界
     * @param pos      产出位置
     * @param outputs  产出列表
     * @param delay    延迟刻数
     * @param sound    播放音效，可为 null
     * @param consumeAt 需要清除的格子
     * @param replace  是否直接替换产出位置
     */
    private static void schedule(ServerLevel level, BlockPos pos, List<CollisionOutput> outputs, int delay,
                                 @Nullable SoundEvent sound, List<BlockPos> consumeAt, boolean replace) {
        TASKS.add(new CollisionScheduler.Task(level, pos.immutable(), List.copyOf(outputs),
                Math.max(0, delay), sound, List.copyOf(consumeAt), replace));
    }

    /**
     * 推进任务队列。
     * <p>
     * 逐个递减任务的剩余刻数，到期的任务移出队列并交给调度器执行。
     */
    public static void tick() {
        if (TASKS.isEmpty()) {
            return;
        }
        List<CollisionScheduler.Task> done = new ArrayList<>();
        for (int index = 0; index < TASKS.size(); index++) {
            CollisionScheduler.Task task = TASKS.get(index);
            if (task.ticksLeft() > 0) {
                TASKS.set(index, task.withTicksLeft(task.ticksLeft() - 1));
            } else {
                TASKS.remove(index);
                index--;
                done.add(task);
            }
        }
        for (CollisionScheduler.Task task : done) {
            CollisionScheduler.execute(task);
        }
    }

    /**
     * 清空任务队列与冷却记录。
     */
    public static void clearTasks() {
        TASKS.clear();
        COOLDOWN.clear();
    }

    /**
     * 判断指定位置是否已过冷却。
     *
     * @param level 世界
     * @param pos   位置
     * @return 可以触发返回 true
     */
    private static boolean isReady(Level level, BlockPos pos) {
        String key = level.dimension().location() + "@" + pos.asLong();
        Long until = COOLDOWN.get(key);
        return until == null || level.getGameTime() >= until;
    }

    /**
     * 标记指定位置的冷却。
     *
     * @param level 世界
     * @param pos   位置
     */
    private static void markCoolDown(Level level, BlockPos pos) {
        COOLDOWN.put(level.dimension().location() + "@" + pos.asLong(), level.getGameTime() + COOLDOWN_TICKS);
    }

    /**
     * 执行产出：随机掷出一条产出并生成物品或方块，并播放音效。
     *
     * @param level   服务端世界
     * @param pos     产出位置
     * @param outputs 产出列表
     * @param sound   播放音效，可为 null
     * @param replace 是否直接替换产出位置
     */
    public static void dropOutputs(ServerLevel level, BlockPos pos, List<CollisionOutput> outputs,
                                   @Nullable SoundEvent sound, boolean replace) {
        CollisionOutput output = rollOne(outputs, level.random);
        if (output == null) {
            return;
        }
        if (output.isBlock()) {
            placeBlock(level, pos, output, replace);
        } else {
            ItemStack stack = output.stack() == null ? ItemStack.EMPTY : output.stack().copyWithCount(output.count());
            if (!stack.isEmpty()) {
                Block.popResource(level, pos, stack);
            }
        }
        if (sound != null) {
            level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    /**
     * 掷一次骰子，挑出这次要执行的那一条产出。
     *
     * @param outputs 产出列表
     * @param random  随机源
     * @return 命中的产出；概率总和不足 100% 时可能为 null
     */
    private static @Nullable CollisionOutput rollOne(List<CollisionOutput> outputs, RandomSource random) {
        float roll = random.nextFloat();
        float accumulated = 0.0F;
        for (CollisionOutput output : outputs) {
            accumulated += output.chance();
            if (roll < accumulated) {
                return output;
            }
        }
        return null;
    }

    /**
     * 放置产出方块。
     * <p>
     * 允许替换时先直接替换产出位置，再按需向产出位置上方与四周的可替换位置
     * 继续放置，直到达到产出数量或无处可放。
     *
     * @param level   服务端世界
     * @param pos     产出位置
     * @param output  产出配置
     * @param replace 是否直接替换产出位置
     */
    private static void placeBlock(ServerLevel level, BlockPos pos, CollisionOutput output, boolean replace) {
        Block block = output.block();
        if (block == null) {
            return;
        }
        BlockState state = block.defaultBlockState();
        int placed = 0;
        if (replace) {
            level.setBlockAndUpdate(pos, state);
            placed = 1;
        }
        List<BlockPos> candidates = new ArrayList<>();
        candidates.add(pos);
        candidates.add(pos.above());
        for (Direction direction : Direction.values()) {
            candidates.add(pos.relative(direction));
        }
        for (BlockPos candidate : candidates) {
            if (placed >= output.count()) {
                return;
            }
            if (!level.isLoaded(candidate)) {
                continue;
            }
            BlockState existing = level.getBlockState(candidate);
            if (existing.isAir() || existing.canBeReplaced()) {
                level.setBlockAndUpdate(candidate, state);
                placed++;
            }
        }
    }

    /**
     * 清除指定位置的方块，置为空气。
     *
     * @param level 服务端世界
     * @param pos   位置
     */
    public static void clearAt(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return;
        }
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }
}
