package com.chinaex123.redstone_chan_lib.api.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

/**
 * 气体桶物品。
 * <p>
 * 继承原版桶物品，按配置支持两种使用方式：普通倒出气体方块，
 * 或「直接散掉」——不放置方块，在视线落点生成云雾粒子、播放熄灭音效，
 * 并把桶换成空桶。
 */
public class RCLGasBucketItem extends BucketItem {

    /** 右键时是否直接散成粒子、不放出气体方块。 */
    private final boolean burst;

    /**
     * 创建气体桶。
     *
     * @param fluid      内容流体
     * @param properties 物品参数
     * @param burst      右键时是否直接散成粒子、不放出气体方块
     */
    public RCLGasBucketItem(Fluid fluid, Properties properties, boolean burst) {
        super(fluid, properties);
        this.burst = burst;
    }

    /**
     * 使用物品。
     * <p>
     * 未启用直接散掉时走原版桶的放置逻辑，否则执行散掉逻辑。
     *
     * @param level  世界
     * @param player 玩家
     * @param hand   交互手
     * @return 使用结果
     */
    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!this.burst) {
            return super.use(level, player, hand);
        }
        return this.burstOut(level, player, hand);
    }

    /**
     * 直接散掉：不放方块，只放粒子，并把桶换成空桶。
     * <p>
     * 服务端摆动手臂、播放熄灭音效，并在视线命中的方块面或视线前方生成云雾粒子；
     * 非创造模式将手持物品替换为空桶。
     *
     * @param level  世界
     * @param player 玩家
     * @param hand   手
     * @return 使用结果
     */
    private InteractionResultHolder<ItemStack> burstOut(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.swing(hand, true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 1.0F);

            HitResult hit = player.pick(5.0D, 0.0F, false);
            double x;
            double y;
            double z;
            if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = blockHit.getBlockPos().relative(blockHit.getDirection());
                x = pos.getX() + 0.5D;
                y = pos.getY() + 0.25D;
                z = pos.getZ() + 0.5D;
            } else {
                var look = player.getLookAngle();
                x = player.getX() + look.x * 1.5D;
                y = player.getEyeY() + look.y * 1.5D;
                z = player.getZ() + look.z * 1.5D;
            }

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CLOUD, x, y, z, 24, 0.35D, 0.35D, 0.35D, 0.02D);
            }

            if (!player.getAbilities().instabuild) {
                player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
