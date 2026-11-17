package com.chinaex123.redstone_chan_lib.client.particles;

import com.chinaex123.redstone_chan_lib.init.RCLParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

/**
 * 圣土光尘粒子。
 * <p>
 * 一种缓慢上升、带左右摇摆的光点粒子：使用精灵表逐帧动画，
 * 初始轻微上浮并受极小负重力影响，前 15% 生命淡入、后 30% 淡出，
 * 渲染类型为半透明粒子表。
 */
@OnlyIn(Dist.CLIENT)
public class HolyMoteParticle extends TextureSheetParticle {

    /** 粒子精灵表。 */
    private final SpriteSet sprites;

    /**
     * 构造圣土光尘粒子。
     * <p>
     * 初始化运动参数、生命周期、尺寸与颜色，并按年龄设置初始精灵。
     *
     * @param level    客户端世界
     * @param x        X 坐标
     * @param y        Y 坐标
     * @param z        Z 坐标
     * @param xSpeed   X 速度
     * @param ySpeed   Y 速度
     * @param zSpeed   Z 速度
     * @param sprites  精灵表
     */
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

    /**
     * 每刻更新粒子。
     * <p>
     * 记录上一位置、更新精灵表动画，并施加左右摇摆与淡入淡出效果。
     */
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

    /**
     * 获取粒子渲染类型。
     *
     * @return 半透明粒子表渲染类型
     */
    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /**
     * 圣土光尘粒子提供器。
     * <p>
     * 根据简单粒子类型创建圣土光尘粒子。
     */
    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {

        /** 粒子精灵表。 */
        private final SpriteSet sprites;

        /**
         * 构造粒子提供器。
         *
         * @param sprites 精灵表
         */
        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        /**
         * 创建粒子。
         *
         * @param type     粒子类型
         * @param level    客户端世界
         * @param x        X 坐标
         * @param y        Y 坐标
         * @param z        Z 坐标
         * @param xSpeed   X 速度
         * @param ySpeed   Y 速度
         * @param zSpeed   Z 速度
         * @return 新创建的粒子
         */
        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new HolyMoteParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }

    /**
     * 在指定位置生成一个圣土光尘粒子。
     *
     * @param level  客户端世界
     * @param x      X 坐标
     * @param y      Y 坐标
     * @param z      Z 坐标
     * @param xSpeed X 速度
     * @param ySpeed Y 速度
     * @param zSpeed Z 速度
     */
    public static void spawn(ClientLevel level, double x, double y, double z,
                             double xSpeed, double ySpeed, double zSpeed) {
        level.addParticle(RCLParticles.HOLY_MOTE.get(), x, y, z, xSpeed, ySpeed, zSpeed);
    }
}
