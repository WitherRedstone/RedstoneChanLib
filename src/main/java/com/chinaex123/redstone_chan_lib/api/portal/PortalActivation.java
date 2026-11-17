package com.chinaex123.redstone_chan_lib.api.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 传送门激活逻辑工具类。
 * <p>
 * 处理玩家对传送门框架的激活尝试：在服务端按框架方块查找匹配的传送门定义，
 * 依次校验激活媒介与门体形状，成功后填充门体、播放激活特效并消耗媒介。
 * 媒介匹配支持物品、物品标签、流体、流体标签与空手等多种方式。
 */
public final class PortalActivation {

    private PortalActivation() {}

    /**
     * 尝试用主手物品激活传送门框架。
     *
     * @param player  玩家
     * @param framePos 框架方块位置
     * @return 激活成功返回 true
     */
    public static boolean tryActivate(Player player, BlockPos framePos) {
        return tryActivate(player.level(), framePos, player, InteractionHand.MAIN_HAND,
                player.getMainHandItem()) != null;
    }

    /**
     * 尝试激活传送门框架。
     * <p>
     * 客户端直接返回 null；服务端下遍历匹配当前框架方块的传送门定义，
     * 校验激活媒介与门体形状，成功时填充门体、播放激活特效，
     * 并在点亮成功后再消耗媒介，避免判定失败也扣物品。
     *
     * @param level    世界
     * @param framePos 框架方块位置
     * @param player   玩家
     * @param hand     交互手
     * @param stack    手持物品堆叠
     * @return 激活成功的传送门定义，失败时返回 null
     */
    @Nullable
    public static PortalDefinition tryActivate(Level level, BlockPos framePos, Player player,
                                               InteractionHand hand, ItemStack stack) {
        if (level.isClientSide) {
            return null;
        }

        BlockState state = level.getBlockState(framePos);
        List<PortalDefinition> candidates = PortalDefinitions.matchingFrames(state);
        for (PortalDefinition definition : candidates) {
            // 激活方式可能是物品、物品标签、流体、流体标签或空手，任意一种匹配即可
            PortalCatalyst catalyst = definition.matchingCatalyst(stack);
            if (catalyst == null) {
                continue;
            }
            PortalShape shape = PortalShape.find(level, framePos, definition);
            if (shape == null) {
                continue;
            }

            PortalStructureHelper.fillPortal(level, shape, definition);
            PortalEffects.playActivationEffects(level, shape.interiorCenter());
            // 点亮成功后再消耗激活媒介，避免判定失败也扣东西
            catalyst.consume(player, hand, stack);
            return definition;
        }
        return null;
    }
}
