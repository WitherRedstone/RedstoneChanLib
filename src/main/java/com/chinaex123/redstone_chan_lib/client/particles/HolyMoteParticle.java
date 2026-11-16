package com.chinaex123.redstone_chan_lib.client.particles;

import com.chinaex123.redstone_chan_lib.init.RCLParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class HolyMoteParticle extends TextureSheetParticle {

    private final SpriteSet sprites;

    protected HolyMoteParticle(ClientLevel level, double x, double y, double z,
                               double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = sprites;

        // 上升
        this.xd = xSpeed;
        this.yd = Math.max(0.012D, ySpeed);
        this.zd = zSpeed;
        this.gravity = -0.0009F;
        this.friction = 0.995F;
        this.hasPhysics = false;

        this.lifetime = 50 + this.random.nextInt(45);
        this.quadSize = 0.035F + this.random.nextFloat() * 0.035F;

        float tint = 0.93F + this.random.nextFloat() * 0.07F;
        this.rCol = tint;
        this.gCol = tint;
        this.bCol = 1.0F;
        this.alpha = 1.0F;

        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        super.tick();
        this.setSpriteFromAge(this.sprites);

        // 左右摇摆，让光点不是笔直上升
        float sway = this.age * 0.25F;
        this.xd += Mth.sin(sway) * 0.00035D;
        this.zd += Mth.cos(sway * 0.8F) * 0.00035D;

        // 前 15% 淡入、后 30% 淡出
        float progress = (float) this.age / (float) this.lifetime;
        if (progress < 0.15F) {
            this.alpha = progress / 0.15F;
        } else if (progress > 0.7F) {
            this.alpha = 1.0F - (progress - 0.7F) / 0.3F;
        } else {
            this.alpha = 1.0F;
        }
        this.alpha = Mth.clamp(this.alpha, 0.0F, 1.0F);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new HolyMoteParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }

    public static void spawn(ClientLevel level, double x, double y, double z,
                             double xSpeed, double ySpeed, double zSpeed) {
        level.addParticle(RCLParticles.HOLY_MOTE.get(), x, y, z, xSpeed, ySpeed, zSpeed);
    }
}
