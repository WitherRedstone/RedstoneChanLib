package com.chinaex123.redstone_chan_lib.item;

import com.chinaex123.redstone_chan_lib.init.RCLParticles;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CeshiItem extends Item {

    public CeshiItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        if (level.isClientSide) {
            SimpleParticleType type = RCLParticles.HOLY_MOTE.get();

            double px = player.getX();
            double py = player.getY() + 0.5;
            double pz = player.getZ();

            for (int i = 0; i < 12; i++) {
                double angle = (i / 12.0) * Math.PI * 2;
                double offsetX = Math.cos(angle) * 0.6;
                double offsetZ = Math.sin(angle) * 0.6;

                level.addParticle(
                        type,
                        px + offsetX, py, pz + offsetZ,
                        0.0, 0.04, 0.0
                );
            }
        }

        return InteractionResultHolder.success(stack);
    }
}