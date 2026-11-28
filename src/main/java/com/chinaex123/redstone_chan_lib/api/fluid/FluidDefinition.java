package com.chinaex123.redstone_chan_lib.api.fluid;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * 流体描述。
 * <p>
 * 集中描述一种流体的全部配置：名称、种类、贴图、着色与雾气、
 * 物理参数（密度、粘稠度、温度、发光等）、各类交互开关以及流体方块属性。
 * 提供转换为流体类型、方块与流体属性参数的便捷方法，
 * 并通过构建器逐步配置全部选项。
 */
public final class FluidDefinition {

    /** 默认静止贴图。 */
    public static final ResourceLocation DEFAULT_STILL_TEXTURE = ResourceLocation.withDefaultNamespace("block/water_still");

    /** 默认流动贴图。 */
    public static final ResourceLocation DEFAULT_FLOWING_TEXTURE = ResourceLocation.withDefaultNamespace("block/water_flow");

    /** 注册名。 */
    private final String name;

    /** 流体种类。 */
    private final FluidKind kind;

    /** 静止贴图。 */
    private final ResourceLocation stillTexture;

    /** 流动贴图。 */
    private final ResourceLocation flowingTexture;

    /** 着色颜色（ARGB）。 */
    private final int tintColor;

    /** 雾气颜色（RGB）。 */
    private final int fogColor;

    /** 雾气最远距离。 */
    private final float fogDistance;

    /** 是否启用雾气。 */
    private final boolean fogEnabled;

    /** 是否使用半透明渲染层。 */
    private final boolean translucent;

    /** 密度。 */
    private final int density;

    /** 粘稠度。 */
    private final int viscosity;

    /** 温度。 */
    private final int temperature;

    /** 流体类型发光等级。 */
    private final int lightLevel;

    /** 摔落距离修正。 */
    private final float fallDistanceModifier;

    /** 实体运动速度缩放。 */
    private final double motionScale;

    /** 是否能湿润耕地。 */
    private final boolean canHydrate;

    /** 是否能形成无限源。 */
    private final boolean canConvertToSource;

    /** 是否能撑船。 */
    private final boolean supportsBoating;

    /** 是否能灭火。 */
    private final boolean canExtinguish;

    /** 是否能推动实体。 */
    private final boolean canPushEntity;

    /** 是否会溺水。 */
    private final boolean canDrown;

    /** 是否能游泳。 */
    private final boolean canSwim;

    /** 爆炸抗性。 */
    private final float explosionResistance;

    /** 寻找斜坡的距离。 */
    private final int slopeFindDistance;

    /** 每格流体的等级衰减。 */
    private final int levelDecreasePerBlock;

    /** 流体更新刻率。 */
    private final int tickRate;

    /** 气体方块消散前存在的刻数。 */
    private final int gasDissipateTicks;

    /** 是否同时注册桶物品。 */
    private final boolean createBucket;

    /** 气体桶右键时是否直接散成粒子。 */
    private final boolean gasBurst;

    /** 翻译键，可能为 null。 */
    private final String translationKey;

    /** 流体方块的发光等级。 */
    private final int blockLightLevel;

    /**
     * 私有构造函数，从构建器创建流体描述。
     *
     * @param builder 构建器
     */
    private FluidDefinition(Builder builder) {
        this.name = builder.name;
        this.kind = builder.kind;
        this.stillTexture = builder.stillTexture;
        this.flowingTexture = builder.flowingTexture;
        this.tintColor = builder.tintColor;
        this.fogColor = builder.fogColor;
        this.fogDistance = builder.fogDistance;
        this.fogEnabled = builder.fogEnabled;
        this.translucent = builder.translucent;
        this.density = builder.density;
        this.viscosity = builder.viscosity;
        this.temperature = builder.temperature;
        this.lightLevel = builder.lightLevel;
        this.fallDistanceModifier = builder.fallDistanceModifier;
        this.motionScale = builder.motionScale;
        this.canHydrate = builder.canHydrate;
        this.canConvertToSource = builder.canConvertToSource;
        this.supportsBoating = builder.supportsBoating;
        this.canExtinguish = builder.canExtinguish;
        this.canPushEntity = builder.canPushEntity;
        this.canDrown = builder.canDrown;
        this.canSwim = builder.canSwim;
        this.explosionResistance = builder.explosionResistance;
        this.slopeFindDistance = builder.slopeFindDistance;
        this.levelDecreasePerBlock = builder.levelDecreasePerBlock;
        this.tickRate = builder.tickRate;
        this.gasDissipateTicks = builder.gasDissipateTicks;
        this.createBucket = builder.createBucket;
        this.gasBurst = builder.gasBurst;
        this.translationKey = builder.translationKey;
        this.blockLightLevel = builder.blockLightLevel;
    }

    /**
     * 开一个构建器。
     *
     * @param name 注册名，只能是路径部分，例如 {@code liquid_mana}
     * @return 构建器
     */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    /**
     * 转成流体类型的参数。
     *
     * @param namespace 命名空间，用来拼默认的翻译键
     * @return 流体类型参数
     */
    public FluidType.Properties createTypeProperties(String namespace) {
        return FluidType.Properties.create()
                .descriptionId(this.translationKey == null ? "block." + namespace + "." + this.name : this.translationKey)
                .fallDistanceModifier(this.fallDistanceModifier)
                .lightLevel(this.lightLevel)
                .density(this.density)
                .temperature(this.temperature)
                .viscosity(this.viscosity)
                .motionScale(this.motionScale)
                .canHydrate(this.canHydrate)
                .canConvertToSource(this.canConvertToSource)
                .canSwim(this.canSwim)
                .canDrown(this.canDrown)
                .canPushEntity(this.canPushEntity)
                .canExtinguish(this.canExtinguish)
                .supportsBoating(this.supportsBoating)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY);
    }

    /**
     * 转成流体方块的参数。
     * <p>
     * 默认使用水的地图颜色、可替换、无碰撞、极高硬度、无掉落表、
     * 液体推力与空音效；方块发光等级大于 0 时附加发光。
     *
     * @return 方块参数
     */
    public BlockBehaviour.Properties createBlockProperties() {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                .mapColor(MapColor.WATER)
                .replaceable()
                .noCollission()
                .strength(100.0F)
                .noLootTable()
                .liquid()
                .pushReaction(PushReaction.DESTROY)
                .sound(SoundType.EMPTY);
        if (this.blockLightLevel > 0) {
            properties = properties.lightLevel(state -> this.blockLightLevel);
        }
        return properties;
    }

    /**
     * 转成液体流体的参数。
     *
     * @param binding 持有各注册结果的绑定对象
     * @return 液体流体参数
     */
    public BaseFlowingFluid.Properties createFluidProperties(FluidBinding binding) {
        return new BaseFlowingFluid.Properties(binding::type, binding::source, binding::flowing)
                .bucket(binding::bucket)
                .block(binding::block)
                .explosionResistance(this.explosionResistance)
                .slopeFindDistance(this.slopeFindDistance)
                .levelDecreasePerBlock(this.levelDecreasePerBlock)
                .tickRate(this.tickRate);
    }

    /** @return 注册名 */
    public String name() {
        return this.name;
    }

    /** @return 流体种类 */
    public FluidKind kind() {
        return this.kind;
    }

    /** @return 静止贴图 */
    public ResourceLocation stillTexture() {
        return this.stillTexture;
    }

    /** @return 流动贴图 */
    public ResourceLocation flowingTexture() {
        return this.flowingTexture;
    }

    /** @return 着色颜色，ARGB */
    public int tintColor() {
        return this.tintColor;
    }

    /** @return 雾气颜色，RGB */
    public int fogColor() {
        return this.fogColor;
    }

    /** @return 雾气最远距离 */
    public float fogDistance() {
        return this.fogDistance;
    }

    /** @return 是否启用雾气 */
    public boolean fogEnabled() {
        return this.fogEnabled;
    }

    /** @return 是否使用半透明渲染层 */
    public boolean translucent() {
        return this.translucent;
    }

    /** @return 密度 */
    public int density() {
        return this.density;
    }

    /** @return 粘稠度 */
    public int viscosity() {
        return this.viscosity;
    }

    /** @return 温度 */
    public int temperature() {
        return this.temperature;
    }

    /** @return 发光等级 */
    public int lightLevel() {
        return this.lightLevel;
    }

    /** @return 摔落距离修正 */
    public float fallDistanceModifier() {
        return this.fallDistanceModifier;
    }

    /** @return 实体在其中运动速度的缩放 */
    public double motionScale() {
        return this.motionScale;
    }

    /** @return 是否能湿润耕地 */
    public boolean canHydrate() {
        return this.canHydrate;
    }

    /** @return 是否能形成无限源 */
    public boolean canConvertToSource() {
        return this.canConvertToSource;
    }

    /** @return 是否能撑船 */
    public boolean supportsBoating() {
        return this.supportsBoating;
    }

    /** @return 是否能灭火 */
    public boolean canExtinguish() {
        return this.canExtinguish;
    }

    /** @return 是否能推动实体 */
    public boolean canPushEntity() {
        return this.canPushEntity;
    }

    /** @return 是否会溺水 */
    public boolean canDrown() {
        return this.canDrown;
    }

    /** @return 是否能游泳 */
    public boolean canSwim() {
        return this.canSwim;
    }

    /** @return 爆炸抗性 */
    public float explosionResistance() {
        return this.explosionResistance;
    }

    /** @return 寻找斜坡的距离 */
    public int slopeFindDistance() {
        return this.slopeFindDistance;
    }

    /** @return 每格流体的等级衰减 */
    public int levelDecreasePerBlock() {
        return this.levelDecreasePerBlock;
    }

    /** @return 流体更新刻率 */
    public int tickRate() {
        return this.tickRate;
    }

    /** @return 气体方块消散前存在的刻数 */
    public int gasDissipateTicks() {
        return this.gasDissipateTicks;
    }

    /** @return 是否同时注册桶物品 */
    public boolean createBucket() {
        return this.createBucket;
    }

    /** @return 气体桶右键时是否直接散成粒子、不放出气体方块 */
    public boolean gasBurst() {
        return this.gasBurst;
    }

    /** @return 翻译键，可能为 null */
    public String translationKey() {
        return this.translationKey;
    }

    /** @return 流体方块的发光等级 */
    public int blockLightLevel() {
        return this.blockLightLevel;
    }

    /**
     * 流体描述的构建器。
     * <p>
     * 提供各类流体属性的链式设置，默认值按普通液体给出，构建时生成不可变描述。
     */
    public static final class Builder {

        /** 注册名。 */
        private final String name;
        /** 流体种类。 */
        private FluidKind kind = FluidKind.LIQUID;
        /** 静止贴图。 */
        private ResourceLocation stillTexture = DEFAULT_STILL_TEXTURE;
        /** 流动贴图。 */
        private ResourceLocation flowingTexture = DEFAULT_FLOWING_TEXTURE;
        /** 着色颜色。 */
        private int tintColor = 0xFFFFFFFF;
        /** 雾气颜色。 */
        private int fogColor = 0xFFFFFF;
        /** 雾气最远距离。 */
        private float fogDistance = 96.0F;
        /** 是否启用雾气。 */
        private boolean fogEnabled = true;
        /** 是否使用半透明渲染层。 */
        private boolean translucent = true;
        /** 密度。 */
        private int density = 1000;
        /** 粘稠度。 */
        private int viscosity = 1000;
        /** 温度。 */
        private int temperature = 300;
        /** 流体类型发光等级。 */
        private int lightLevel = 0;
        /** 摔落距离修正。 */
        private float fallDistanceModifier = 1.0F;
        /** 实体运动速度缩放。 */
        private double motionScale = 0.014D;
        /** 是否能湿润耕地。 */
        private boolean canHydrate = false;
        /** 是否能形成无限源。 */
        private boolean canConvertToSource = false;
        /** 是否能撑船。 */
        private boolean supportsBoating = true;
        /** 是否能灭火。 */
        private boolean canExtinguish = false;
        /** 是否能推动实体。 */
        private boolean canPushEntity = true;
        /** 是否会溺水。 */
        private boolean canDrown = true;
        /** 是否能游泳。 */
        private boolean canSwim = true;
        /** 爆炸抗性。 */
        private float explosionResistance = 100.0F;
        /** 寻找斜坡的距离。 */
        private int slopeFindDistance = 4;
        /** 每格流体的等级衰减。 */
        private int levelDecreasePerBlock = 1;
        /** 流体更新刻率。 */
        private int tickRate = 5;
        /** 气体方块消散前存在的刻数。 */
        private int gasDissipateTicks = 200;
        /** 是否同时注册桶物品。 */
        private boolean createBucket = true;
        /** 气体桶右键时是否直接散成粒子。 */
        private boolean gasBurst = false;
        /** 翻译键。 */
        private String translationKey = null;
        /** 流体方块的发光等级。 */
        private int blockLightLevel = 0;

        /**
         * 私有构造函数。
         *
         * @param name 注册名
         */
        private Builder(String name) {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("流体注册名不能为空");
            }
            this.name = name;
        }

        /**
         * 设置流体种类。
         *
         * @param kind 液体或气体
         * @return 构建器
         */
        public Builder kind(FluidKind kind) {
            this.kind = kind;
            return this;
        }

        /**
         * 设置静止与流动贴图。
         *
         * @param still   静止贴图
         * @param flowing 流动贴图
         * @return 构建器
         */
        public Builder textures(ResourceLocation still, ResourceLocation flowing) {
            this.stillTexture = still;
            this.flowingTexture = flowing;
            return this;
        }

        /**
         * 设置静止贴图。
         *
         * @param still 静止贴图
         * @return 构建器
         */
        public Builder stillTexture(ResourceLocation still) {
            this.stillTexture = still;
            return this;
        }

        /**
         * 设置流动贴图。
         *
         * @param flowing 流动贴图
         * @return 构建器
         */
        public Builder flowingTexture(ResourceLocation flowing) {
            this.flowingTexture = flowing;
            return this;
        }

        /**
         * 设置流体着色。
         *
         * @param argb 颜色，带透明度，例如 {@code 0xFF3080FF}
         * @return 构建器
         */
        public Builder tintColor(int argb) {
            this.tintColor = argb;
            return this;
        }

        /**
         * 设置雾气颜色。
         *
         * @param rgb 颜色，不带透明度，例如 {@code 0x3080FF}
         * @return 构建器
         */
        public Builder fogColor(int rgb) {
            this.fogColor = rgb;
            return this;
        }

        /**
         * 设置雾气最远距离。
         *
         * @param distance 距离，单位是格，越小越糊
         * @return 构建器
         */
        public Builder fogDistance(float distance) {
            this.fogDistance = distance;
            return this;
        }

        /**
         * 设置是否启用雾气。
         *
         * @param enabled 为 false 时进入流体不会有雾
         * @return 构建器
         */
        public Builder fogEnabled(boolean enabled) {
            this.fogEnabled = enabled;
            return this;
        }

        /**
         * 设置是否使用半透明渲染层。
         *
         * @param translucent 为 true 时半透明，为 false 时用实心渲染层
         * @return 构建器
         */
        public Builder translucent(boolean translucent) {
            this.translucent = translucent;
            return this;
        }

        /**
         * 设置密度，负数会往上浮。
         *
         * @param density 密度
         * @return 构建器
         */
        public Builder density(int density) {
            this.density = density;
            return this;
        }

        /**
         * 设置粘稠度，越大流得越慢。
         *
         * @param viscosity 粘稠度
         * @return 构建器
         */
        public Builder viscosity(int viscosity) {
            this.viscosity = viscosity;
            return this;
        }

        /**
         * 设置温度。
         *
         * @param temperature 温度
         * @return 构建器
         */
        public Builder temperature(int temperature) {
            this.temperature = temperature;
            return this;
        }

        /**
         * 设置流体类型的发光等级。
         *
         * @param lightLevel 发光等级，0 到 15
         * @return 构建器
         */
        public Builder lightLevel(int lightLevel) {
            this.lightLevel = lightLevel;
            return this;
        }

        /**
         * 设置流体方块的发光等级。
         *
         * @param lightLevel 发光等级，0 到 15
         * @return 构建器
         */
        public Builder blockLightLevel(int lightLevel) {
            this.blockLightLevel = lightLevel;
            return this;
        }

        /**
         * 设置摔落距离修正。
         *
         * @param modifier 修正系数
         * @return 构建器
         */
        public Builder fallDistanceModifier(float modifier) {
            this.fallDistanceModifier = modifier;
            return this;
        }

        /**
         * 设置实体在流体中的运动速度缩放。
         *
         * @param scale 缩放值，越小越慢
         * @return 构建器
         */
        public Builder motionScale(double scale) {
            this.motionScale = scale;
            return this;
        }

        /**
         * 设置是否能湿润耕地。
         *
         * @param value 是否
         * @return 构建器
         */
        public Builder canHydrate(boolean value) {
            this.canHydrate = value;
            return this;
        }

        /**
         * 设置是否能形成无限源。
         *
         * @param value 是否
         * @return 构建器
         */
        public Builder canConvertToSource(boolean value) {
            this.canConvertToSource = value;
            return this;
        }

        /**
         * 设置是否能撑船。
         *
         * @param value 是否
         * @return 构建器
         */
        public Builder supportsBoating(boolean value) {
            this.supportsBoating = value;
            return this;
        }

        /**
         * 设置是否能灭火。
         *
         * @param value 是否
         * @return 构建器
         */
        public Builder canExtinguish(boolean value) {
            this.canExtinguish = value;
            return this;
        }

        /**
         * 设置是否能推动实体。
         *
         * @param value 是否
         * @return 构建器
         */
        public Builder canPushEntity(boolean value) {
            this.canPushEntity = value;
            return this;
        }

        /**
         * 设置是否会溺水。
         *
         * @param value 是否
         * @return 构建器
         */
        public Builder canDrown(boolean value) {
            this.canDrown = value;
            return this;
        }

        /**
         * 设置是否能游泳。
         *
         * @param value 是否
         * @return 构建器
         */
        public Builder canSwim(boolean value) {
            this.canSwim = value;
            return this;
        }

        /**
         * 设置爆炸抗性。
         *
         * @param value 抗性
         * @return 构建器
         */
        public Builder explosionResistance(float value) {
            this.explosionResistance = value;
            return this;
        }

        /**
         * 设置寻找斜坡的距离。
         *
         * @param value 距离
         * @return 构建器
         */
        public Builder slopeFindDistance(int value) {
            this.slopeFindDistance = value;
            return this;
        }

        /**
         * 设置每格流体的等级衰减。
         *
         * @param value 衰减
         * @return 构建器
         */
        public Builder levelDecreasePerBlock(int value) {
            this.levelDecreasePerBlock = value;
            return this;
        }

        /**
         * 设置流体更新刻率。
         *
         * @param value 刻数，越小流得越快
         * @return 构建器
         */
        public Builder tickRate(int value) {
            this.tickRate = value;
            return this;
        }

        /**
         * 设置气体方块消散前存在的刻数。
         *
         * @param ticks 刻数，20 刻是一秒
         * @return 构建器
         */
        public Builder gasDissipateTicks(int ticks) {
            this.gasDissipateTicks = ticks;
            return this;
        }

        /**
         * 设置是否同时注册桶物品。
         *
         * @param value 是否
         * @return 构建器
         */
        public Builder createBucket(boolean value) {
            this.createBucket = value;
            return this;
        }

        /**
         * 设置气体桶右键时的表现。
         * <p>
         * 开了之后右键不会真的放下气体方块，而是在视线落点放一团粒子、播一声熄灭的音效，
         * 看起来像是气体当场散掉了；关掉就是普通气体桶，右键照常倒出气体方块。
         * 只有 {@code kind(FluidKind.GAS)} 才用得上这个开关。
         *
         * @param value 是否直接散掉
         * @return 构建器
         */
        public Builder gasBurst(boolean value) {
            this.gasBurst = value;
            return this;
        }

        /**
         * 设置翻译键。
         *
         * @param key 例如 {@code block.mymod.my_fluid}
         * @return 构建器
         */
        public Builder translationKey(String key) {
            this.translationKey = key;
            return this;
        }

        /**
         * 构建。
         *
         * @return 流体描述
         */
        public FluidDefinition build() {
            return new FluidDefinition(this);
        }
    }
}
