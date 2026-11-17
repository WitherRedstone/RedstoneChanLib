package com.chinaex123.redstone_chan_lib.api.portal;

import com.chinaex123.redstone_chan_lib.util.PortalTexts;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 传送门仪式配置。
 * <p>
 * 描述仪式式激活所需的条件：空位内的流体（指定流体或标签）、
 * 空位尺寸、需要投入的物品媒介，以及是否要求框架包含全部框架类型。
 * 提供流体与物品的匹配判断，并通过构建器逐步组装配置。
 */
public final class PortalRitual {

    /** 默认空位宽度 */
    public static final int DEFAULT_WIDTH = 2;

    /** 默认空位深度 */
    public static final int DEFAULT_DEPTH = 2;

    /** 指定流体，未指定时为 null。 */
    @Nullable
    private final Fluid fluid;

    /** 流体标签，未指定时为 null。 */
    @Nullable
    private final TagKey<Fluid> fluidTag;

    /** 空位宽度。 */
    private final int width;

    /** 空位深度。 */
    private final int depth;

    /** 需要投入的物品媒介列表。 */
    private final List<PortalCatalyst> items;

    /** 是否要求框架包含全部框架类型。 */
    private final boolean requireAllFrameTypes;

    /**
     * 私有构造函数，从构建器创建仪式配置。
     *
     * @param builder 构建器
     */
    private PortalRitual(Builder builder) {
        this.fluid = builder.fluid;
        this.fluidTag = builder.fluidTag;
        this.width = builder.width;
        this.depth = builder.depth;
        this.items = List.copyOf(builder.items);
        this.requireAllFrameTypes = builder.requireAllFrameTypes;
    }

    /**
     * 创建构建器。
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取指定流体。
     *
     * @return 指定流体，未指定时返回 null
     */
    @Nullable
    public Fluid fluid() {
        return this.fluid;
    }

    /**
     * 获取流体标签。
     *
     * @return 流体标签，未指定时返回 null
     */
    @Nullable
    public TagKey<Fluid> fluidTag() {
        return this.fluidTag;
    }

    /**
     * 获取空位宽度。
     *
     * @return 空位宽度
     */
    public int width() {
        return this.width;
    }

    /**
     * 获取空位深度。
     *
     * @return 空位深度
     */
    public int depth() {
        return this.depth;
    }

    /**
     * 获取需要投入的物品媒介列表。
     *
     * @return 物品媒介列表
     */
    public List<PortalCatalyst> items() {
        return this.items;
    }

    /**
     * 判断是否要求框架包含全部框架类型。
     *
     * @return 是否要求全部框架类型
     */
    public boolean requireAllFrameTypes() {
        return this.requireAllFrameTypes;
    }

    /**
     * 判断流体状态是否匹配仪式所需流体。
     * <p>
     * 空流体不匹配；优先按流体标签匹配，其次按指定流体匹配。
     *
     * @param state 流体状态
     * @return 匹配返回 true
     */
    public boolean matchesFluid(FluidState state) {
        if (state.isEmpty()) {
            return false;
        }
        if (this.fluidTag != null && state.is(this.fluidTag)) {
            return true;
        }
        return this.fluid != null && state.is(this.fluid);
    }

    /**
     * 判断物品堆叠是否匹配仪式所需的投入物。
     * <p>
     * 未配置物品媒介时视为任意匹配；否则需匹配其中任一媒介。
     *
     * @param stack 物品堆叠
     * @return 匹配返回 true
     */
    public boolean matchesItem(ItemStack stack) {
        if (this.items.isEmpty()) {
            return true;
        }
        for (PortalCatalyst catalyst : this.items) {
            if (catalyst.matches(stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 仪式配置构建器。
     * <p>
     * 配置流体、空位尺寸、投入物媒介与框架类型要求，
     * 构建时校验至少指定了一种流体。
     */
    public static final class Builder {

        /** 指定流体。 */
        private Fluid fluid;

        /** 流体标签。 */
        private TagKey<Fluid> fluidTag;

        /** 空位宽度。 */
        private int width = DEFAULT_WIDTH;

        /** 空位深度。 */
        private int depth = DEFAULT_DEPTH;

        /** 物品媒介列表。 */
        private final List<PortalCatalyst> items = new ArrayList<>();

        /** 是否要求框架包含全部框架类型。 */
        private boolean requireAllFrameTypes;

        private Builder() {}

        /**
         * 设置指定流体。
         *
         * @param fluid 流体
         */
        public void fluid(Fluid fluid) {
            this.fluid = fluid;
        }

        /**
         * 设置流体标签。
         *
         * @param tag 流体标签
         */
        public void fluidTag(TagKey<Fluid> tag) {
            this.fluidTag = tag;
        }

        /**
         * 设置空位尺寸。
         *
         * @param width 宽度
         * @param depth 深度
         * @return 当前构建器
         */
        public Builder size(int width, int depth) {
            if (width < 1 || depth < 1) {
                throw new IllegalArgumentException(PortalTexts.text("ritual_size_too_small"));
            }
            this.width = width;
            this.depth = depth;
            return this;
        }

        /**
         * 设置单个物品作为投入物，替换之前的配置。
         *
         * @param item 物品，传 null 表示清空
         * @return 当前构建器
         */
        public Builder item(@Nullable Item item) {
            this.items.clear();
            if (item != null) {
                this.items.add(PortalCatalyst.item(item));
            }
            return this;
        }

        /**
         * 设置物品标签作为投入物，替换之前的配置。
         *
         * @param tag 物品标签
         * @return 当前构建器
         */
        public Builder itemTag(TagKey<Item> tag) {
            this.items.clear();
            this.items.add(PortalCatalyst.itemTag(tag));
            return this;
        }

        /**
         * 追加一个投入物媒介。
         *
         * @param catalyst 激活媒介
         * @return 当前构建器
         */
        public Builder item(PortalCatalyst catalyst) {
            if (catalyst != null) {
                this.items.add(catalyst);
            }
            return this;
        }

        /**
         * 替换全部投入物媒介。
         *
         * @param catalysts 激活媒介集合
         * @return 当前构建器
         */
        public Builder items(Collection<PortalCatalyst> catalysts) {
            this.items.clear();
            this.items.addAll(catalysts);
            return this;
        }

        /**
         * 设置是否要求框架包含全部框架类型。
         *
         * @param require 是否要求
         */
        public void requireAllFrameTypes(boolean require) {
            this.requireAllFrameTypes = require;
        }

        /**
         * 构建仪式配置。
         * <p>
         * 未指定任何流体时抛出异常。
         *
         * @return 仪式配置
         */
        public PortalRitual build() {
            if (this.fluid == null && this.fluidTag == null) {
                throw new IllegalStateException(PortalTexts.text("ritual_without_fluid"));
            }
            return new PortalRitual(this);
        }
    }
}
