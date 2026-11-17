package com.chinaex123.redstone_chan_lib.api.portal;

import com.chinaex123.redstone_chan_lib.util.PortalTexts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 传送门定义。
 * <p>
 * 描述一种传送门的完整配置：外围框架方块与标签、激活方式、
 * 颜色、名称、目标维度、内部尺寸限制、传送等待时间、摆放类型与仪式配置。
 * 提供框架判定、媒介匹配、砌墙方块选择与尺寸校验等查询方法，
 * 并通过构建器逐步组装定义。
 */
public final class PortalDefinition {

    /** 默认传送门方块颜色。 */
    public static final int DEFAULT_COLOR = 0xFF730DFF;
    /** 默认内部最小宽度 */
    public static final int DEFAULT_MIN_WIDTH = 2;
    /** 默认内部最小高度 */
    public static final int DEFAULT_MIN_HEIGHT = 3;

    /** 默认内部最大宽度 */
    public static final int DEFAULT_MAX_WIDTH = 21;
    /** 默认内部最大高度 */
    public static final int DEFAULT_MAX_HEIGHT = 21;

    /** 默认传送等待时间（刻） */
    public static final int DEFAULT_WAIT_TICKS = 80;

    /** 定义唯一标识。 */
    private final ResourceLocation id;
    /** 外围框架方块集合。 */
    private final Set<Block> frameBlocks;
    /** 外围框架方块标签集合。 */
    private final Set<TagKey<Block>> frameTags;
    /** 激活方式列表。 */
    private final List<PortalCatalyst> catalysts;
    /** 传送门方块颜色。 */
    private final int color;
    /** 传送门方块名称。 */
    private final Component name;
    /** 目标维度。 */
    private final ResourceKey<Level> dimension;
    /** 内部最小宽度。 */
    private final int minWidth;
    /** 内部最小高度。 */
    private final int minHeight;
    /** 内部最大宽度。 */
    private final int maxWidth;
    /** 内部最大高度。 */
    private final int maxHeight;
    /** 传送等待时间（刻）。 */
    private final int waitTicks;
    /** 允许的摆放类型。 */
    private final PortalOrientation orientation;
    /** 仪式配置，无仪式激活时为 null。 */
    private final PortalRitual ritual;

    /**
     * 私有构造函数，从构建器创建定义。
     *
     * @param builder 构建器
     */
    private PortalDefinition(Builder builder) {
        this.id = builder.id;
        this.frameBlocks = Collections.unmodifiableSet(new LinkedHashSet<>(builder.frameBlocks));
        this.frameTags = Collections.unmodifiableSet(new LinkedHashSet<>(builder.frameTags));
        this.catalysts = List.copyOf(builder.catalysts);
        this.color = builder.color;
        this.name = builder.name;
        this.dimension = builder.dimension;
        this.minWidth = builder.minWidth;
        this.minHeight = builder.minHeight;
        this.maxWidth = builder.maxWidth;
        this.maxHeight = builder.maxHeight;
        this.waitTicks = builder.waitTicks;
        this.orientation = builder.orientation;
        this.ritual = builder.ritual;
    }

    /**
     * 创建构建器。
     *
     * @param id 定义唯一标识
     * @return 构建器
     */
    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    /**
     * 获取定义唯一标识。
     *
     * @return 定义唯一标识
     */
    public ResourceLocation id() {
        return this.id;
    }

    /**
     * 获取组成外围框架的方块集合。
     *
     * @return 外围框架方块集合（不可变）
     */
    public Set<Block> frameBlocks() {
        return this.frameBlocks;
    }

    /**
     * 获取组成外围框架的方块标签集合。
     *
     * @return 外围框架方块标签集合（不可变）
     */
    public Set<TagKey<Block>> frameTags() {
        return this.frameTags;
    }

    /**
     * 获取全部激活方式。
     *
     * @return 激活方式列表（不可变），空列表表示空手右键即可
     */
    public List<PortalCatalyst> catalysts() {
        return this.catalysts;
    }

    /**
     * 获取传送门方块颜色。
     * <p>
     * 染色是乘法，只取 R、G、B 三个通道，A 通道目前不参与渲染；
     * 贴图是灰度的，所以写什么颜色就得到什么颜色。
     *
     * @return 传送门方块颜色（ARGB）
     */
    public int color() {
        return this.color;
    }

    /**
     * 获取传送门方块名称。
     *
     * @return 传送门方块名称
     */
    public Component name() {
        return this.name;
    }

    /**
     * 获取目标维度。
     *
     * @return 目标维度
     */
    public ResourceKey<Level> dimension() {
        return this.dimension;
    }

    /**
     * 获取内部最小宽度。
     *
     * @return 内部最小宽度
     */
    public int minWidth() {
        return this.minWidth;
    }

    /**
     * 获取内部最小高度。
     *
     * @return 内部最小高度
     */
    public int minHeight() {
        return this.minHeight;
    }

    /**
     * 获取内部最大宽度。
     *
     * @return 内部最大宽度
     */
    public int maxWidth() {
        return this.maxWidth;
    }

    /**
     * 获取内部最大高度。
     *
     * @return 内部最大高度
     */
    public int maxHeight() {
        return this.maxHeight;
    }

    /**
     * 获取传送等待时间。
     *
     * @return 传送等待时间（刻）
     */
    public int waitTicks() {
        return this.waitTicks;
    }

    /**
     * 获取允许的摆放类型。
     *
     * @return 允许的摆放类型
     */
    public PortalOrientation orientation() {
        return this.orientation;
    }

    /**
     * 获取仪式配置。
     *
     * @return 仪式配置；没有配置仪式式激活时返回 null
     */
    @Nullable
    public PortalRitual ritual() {
        return this.ritual;
    }

    /**
     * 判断本定义是否接受指定法线轴的传送门平面。
     *
     * @param axis 平面法线轴
     * @return 接受时返回 true
     */
    public boolean allowsAxis(Direction.Axis axis) {
        return this.orientation.allows(axis);
    }

    /**
     * 判断某个方块状态是否属于本传送门的外围框架。
     *
     * @param state 待判断的方块状态
     * @return 属于框架则返回 true
     */
    public boolean isFrame(BlockState state) {
        for (Block block : this.frameBlocks) {
            if (state.is(block)) return true;
        }
        for (TagKey<Block> tag : this.frameTags) {
            if (state.is(tag)) return true;
        }
        return false;
    }

    /**
     * 判断手上的物品能否点亮本传送门。
     *
     * @param stack 手上的物品
     * @return 可以点亮返回 true
     */
    public boolean isCatalyst(ItemStack stack) {
        if (this.catalysts.isEmpty()) {
            return stack.isEmpty();
        }
        return matchingCatalyst(stack) != null;
    }

    /**
     * 找出与手上物品匹配的激活方式。
     *
     * @param stack 手上的物品
     * @return 匹配的激活方式；不匹配返回 null
     */
    @Nullable
    public PortalCatalyst matchingCatalyst(ItemStack stack) {
        if (this.catalysts.isEmpty()) {
            return stack.isEmpty() ? PortalCatalyst.empty() : null;
        }
        for (PortalCatalyst catalyst : this.catalysts) {
            if (catalyst.matches(stack)) {
                return catalyst;
            }
        }
        return null;
    }

    /**
     * 获取建造新传送门时用来搭框架的方块。
     * <p>
     * 在目标维度自动建门时，框架是要砌成墙的，所以优先挑一个碰撞形状是完整方块的：
     * 仪式式传送门常常用花之类的植物当框架，直接拿花去砌墙会很奇怪，这种情况会退回标签里第一个完整方块，
     * 实在没有完整方块时才用标签里的第一个方块，最后才兜底成石头。
     *
     * @return 用来搭框架的方块
     */
    public Block getBuildFrameBlock() {
        for (Block block : this.frameBlocks) {
            if (isWallBlock(block.defaultBlockState())) {
                return block;
            }
        }
        if (!this.frameBlocks.isEmpty()) {
            return this.frameBlocks.iterator().next();
        }
        for (TagKey<Block> tag : this.frameTags) {
            Optional<HolderSet.Named<Block>> holders = BuiltInRegistries.BLOCK.getTag(tag);
            if (holders.isEmpty() || holders.get().size() <= 0) {
                continue;
            }
            for (var holder : holders.get()) {
                Block block = holder.value();
                if (isWallBlock(block.defaultBlockState())) {
                    return block;
                }
            }
        }
        return Blocks.STONE;
    }

    /**
     * 判断一个方块状态够不够格当“砌墙用”的框架方块。
     * <p>
     * 这里用 BlockState#isCollisionShapeFullBlock 而不是旧的 isSolid()：
     * 后者在 1.21.1 已经标记为废弃，前者才是现在的官方写法（原版自己也是这么改的）。
     * 只有碰撞形状是完整方块的才会被选中，所以花、树苗、火把、栅栏这类会被跳过。
     * <p>
     * 这个方法只关心方块本身的形状，不需要真实世界，传空世界与原点即可。
     */
    private static boolean isWallBlock(BlockState state) {
        return state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    /**
     * 判断一组内部尺寸是否符合本定义的限制。
     *
     * @param width  内部宽度
     * @param height 内部高度
     * @return 在允许范围内返回 true
     */
    public boolean isSizeAllowed(int width, int height) {
        return width >= this.minWidth && width <= this.maxWidth
                && height >= this.minHeight && height <= this.maxHeight;
    }

    /**
     * 传送门定义构建器。
     * <p>
     * 逐步配置框架方块与标签、激活方式、颜色、名称、维度、
     * 尺寸限制、等待时间、摆放类型与仪式，最终构建不可变定义。
     */
    public static final class Builder {

        /** 定义唯一标识。 */
        private final ResourceLocation id;

        /** 外围框架方块列表。 */
        private final List<Block> frameBlocks = new ArrayList<>();

        /** 外围框架方块标签列表。 */
        private final List<TagKey<Block>> frameTags = new ArrayList<>();

        /** 激活方式列表。 */
        private final List<PortalCatalyst> catalysts = new ArrayList<>();

        /** 传送门方块颜色。 */
        private int color = DEFAULT_COLOR;

        /** 传送门方块名称。 */
        private Component name;

        /** 目标维度。 */
        private ResourceKey<Level> dimension = Level.NETHER;
        /** 内部最小宽度。 */
        private int minWidth = DEFAULT_MIN_WIDTH;
        /** 内部最小高度。 */
        private int minHeight = DEFAULT_MIN_HEIGHT;
        /** 内部最大宽度。 */
        private int maxWidth = DEFAULT_MAX_WIDTH;
        /** 内部最大高度。 */
        private int maxHeight = DEFAULT_MAX_HEIGHT;
        /** 传送等待时间（刻）。 */
        private int waitTicks = DEFAULT_WAIT_TICKS;
        /** 允许的摆放类型。 */
        private PortalOrientation orientation = PortalOrientation.ANY;
        /** 仪式配置。 */
        private PortalRitual ritual;

        /**
         * 私有构造函数。
         * <p>
         * 默认名称根据定义 ID 生成翻译键。
         *
         * @param id 定义唯一标识
         */
        private Builder(ResourceLocation id) {
            this.id = id;
            this.name = Component.translatable("block." + id.getNamespace() + "." + id.getPath().replace('/', '.'));
        }

        /**
         * 添加一种外围框架方块。
         *
         * @param block 方块
         * @return 当前构建器
         */
        public Builder frameBlock(Block block) {
            if (block != null) {
                this.frameBlocks.add(block);
            }
            return this;
        }

        /**
         * 添加一个外围框架方块标签。
         *
         * @param tag 方块标签
         */
        public void frameTag(TagKey<Block> tag) {
            if (tag != null) {
                this.frameTags.add(tag);
            }
        }

        /**
         * 用单个物品作为激活方式，会替换掉之前配置的激活方式。
         *
         * @param item 物品，传 null 表示空手
         * @return 当前构建器
         */
        public Builder catalyst(@Nullable Item item) {
            this.catalysts.clear();
            if (item != null) {
                this.catalysts.add(PortalCatalyst.item(item));
            }
            return this;
        }

        /**
         * 追加一个激活方式。
         *
         * @param catalyst 激活方式
         */
        public void catalyst(@Nullable PortalCatalyst catalyst) {
            if (catalyst != null) {
                this.catalysts.add(catalyst);
            }
        }

        /**
         * 替换全部激活方式。
         *
         * @param catalysts 激活方式集合
         * @return 当前构建器
         */
        public Builder catalysts(java.util.Collection<PortalCatalyst> catalysts) {
            this.catalysts.clear();
            this.catalysts.addAll(catalysts);
            return this;
        }

        /**
         * 追加一个物品标签激活方式。
         *
         * @param tag 物品标签
         * @return 当前构建器
         */
        public Builder itemTagCatalyst(TagKey<Item> tag) {
            this.catalysts.add(PortalCatalyst.itemTag(tag));
            return this;
        }

        /**
         * 追加一个流体激活方式（手持装有该流体的容器）。
         *
         * @param fluid 流体
         * @return 当前构建器
         */
        public Builder fluidCatalyst(Fluid fluid) {
            this.catalysts.add(PortalCatalyst.fluid(fluid));
            return this;
        }

        /**
         * 追加一个流体标签激活方式。
         *
         * @param tag 流体标签
         * @return 当前构建器
         */
        public Builder fluidTagCatalyst(TagKey<Fluid> tag) {
            this.catalysts.add(PortalCatalyst.fluidTag(tag));
            return this;
        }

        /**
         * 设置传送门方块颜色。
         * <p>
         * 染色是乘法、贴图是灰度的，所以给什么颜色就是什么颜色。
         *
         * @param argb ARGB 颜色
         */
        public void color(int argb) {
            this.color = argb;
        }

        /**
         * 设置传送门方块名称。
         *
         * @param name 名称
         * @return 当前构建器
         */
        public Builder name(Component name) {
            if (name != null) {
                this.name = name;
            }
            return this;
        }

        /**
         * 设置目标维度。
         *
         * @param dimension 目标维度键
         * @return 当前构建器
         */
        public Builder dimension(ResourceKey<Level> dimension) {
            if (dimension != null) {
                this.dimension = dimension;
            }
            return this;
        }

        /**
         * 设置内部尺寸限制。
         *
         * @param minWidth  内部最小宽度
         * @param minHeight 内部最小高度
         * @param maxWidth  内部最大宽度
         * @param maxHeight 内部最大高度
         * @return 当前构建器
         */
        public Builder size(int minWidth, int minHeight, int maxWidth, int maxHeight) {
            if (minWidth > 0 && minHeight > 0 && maxWidth >= minWidth && maxHeight >= minHeight) {
                this.minWidth = minWidth;
                this.minHeight = minHeight;
                this.maxWidth = maxWidth;
                this.maxHeight = maxHeight;
            }
            return this;
        }

        /**
         * 设置进入传送门后的等待时间。
         *
         * @param ticks 等待刻数
         */
        public void waitTicks(int ticks) {
            if (ticks > 0) {
                this.waitTicks = ticks;
            }
        }

        /**
         * 设置摆放类型。
         *
         * @param orientation 摆放类型
         */
        public void orientation(PortalOrientation orientation) {
            if (orientation != null) {
                this.orientation = orientation;
            }
        }

        /**
         * 设置仪式式激活配置（往流体里丢物品、雷劈激活）。
         *
         * @param ritual 仪式配置
         */
        public void ritual(@Nullable PortalRitual ritual) {
            this.ritual = ritual;
        }

        /**
         * 构建定义。
         *
         * @return 传送门定义
         */
        public PortalDefinition build() {
            if (this.frameBlocks.isEmpty() && this.frameTags.isEmpty()) {
                throw new IllegalStateException(PortalTexts.text("definition_without_frame", this.id));
            }
            return new PortalDefinition(this);
        }
    }
}
