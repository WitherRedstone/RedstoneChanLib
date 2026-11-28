package com.chinaex123.redstone_chan_lib.world;

import com.chinaex123.redstone_chan_lib.api.collision.CollisionOutput;
import com.chinaex123.redstone_chan_lib.api.collision.CollisionRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 碰撞产出的延迟调度。
 * <p>
 * 一组产出在配方里声明了延迟（默认 1 刻），这段时间里先记下来，
 * 每刻推进一次，到点再真正执行，方便做“流过去一会儿才凝固”的效果。
 */
public final class CollisionScheduler {

    /**
     * 一条待执行任务。
     *
     * @param level     所属服务端世界
     * @param pos       产出位置
     * @param outputs   产出列表
     * @param ticksLeft 还剩几刻
     * @param sound     触发音效，可为空
     * @param consumeAt 需要清空的位置列表，可为空列表
     * @param replace   是否直接替换产出位置上的方块（方块碰撞用，原位把被撞的方块换掉）
     */
    public record Task(ServerLevel level, BlockPos pos, List<CollisionOutput> outputs, int ticksLeft,
                       @Nullable SoundEvent sound, List<BlockPos> consumeAt, boolean replace) {

        /**
         * 复制一份并改掉剩余刻数。
         *
         * @param ticksLeft 新的剩余刻数
         * @return 新任务
         */
        public Task withTicksLeft(int ticksLeft) {
            return new Task(level, pos, outputs, ticksLeft, sound, consumeAt, replace);
        }
    }

    /**
     * 工具类，不允许实例化。
     */
    private CollisionScheduler() {
    }

    /**
     * 执行一条任务：先按需清空目标，再按概率投放产出。
     *
     * @param task 任务
     */
    public static void execute(Task task) {
        ServerLevel level = task.level();
        if (!level.isLoaded(task.pos())) {
            return;
        }
        for (BlockPos consumeAt : task.consumeAt()) {
            CollisionRecipes.clearAt(level, consumeAt);
        }
        CollisionRecipes.dropOutputs(level, task.pos(), task.outputs(), task.sound(), task.replace());
    }
}
