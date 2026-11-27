package com.chinaex123.redstone_chan_lib.api.creative;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * 分区按钮的外观样式。
 * <p>
 * 一个样式描述：底图（未选中、选中、鼠标悬停各一张）、按钮尺寸、是否在按钮上画物品、
 * 物品渲染的缩放与偏移、以及鼠标悬停时显示的提示文本。除提示文本外都可以自由设置，
 * 未设置的部分用默认值，默认值是“24×24 的按钮，底图取自本模组自带的贴图，物品 16×16 画在按钮中心偏上一点”。
 */
public record CreativeSectionStyle(
        ResourceLocation texture,
        ResourceLocation selectedTexture,
        ResourceLocation hoveredTexture,
        int width,
        int height,
        int textureWidth,
        int textureHeight,
        boolean renderIcon,
        float iconScale,
        int iconOffsetX,
        int iconOffsetY,
        @Nullable Component tooltip) {

    /** 默认按钮边长。 */
    public static final int DEFAULT_SIZE = 24;

    /** 默认底图（未选中）。 */
    public static final ResourceLocation DEFAULT_TEXTURE =
            RedstoneChanLIB.id("textures/gui/creative_inventory/section_button.png");

    /** 默认底图（选中）。 */
    public static final ResourceLocation DEFAULT_SELECTED_TEXTURE =
            RedstoneChanLIB.id("textures/gui/creative_inventory/section_button_selected.png");

    /** 默认底图（鼠标悬停，叠在底图上面画）。 */
    public static final ResourceLocation DEFAULT_HOVERED_TEXTURE =
            RedstoneChanLIB.id("textures/gui/creative_inventory/section_button_hovered.png");

    /** 默认物品缩放。 */
    public static final float DEFAULT_ICON_SCALE = 1.0F;

    /** 默认物品偏移（16×16 的物品画在 24×24 按钮里正好留 4 像素边）。 */
    public static final int DEFAULT_ICON_OFFSET = 4;

    /**
     * 取得默认样式。
     *
     * @return 默认样式
     */
    public static CreativeSectionStyle defaults() {
        return new CreativeSectionStyle(
                DEFAULT_TEXTURE,
                DEFAULT_SELECTED_TEXTURE,
                DEFAULT_HOVERED_TEXTURE,
                DEFAULT_SIZE,
                DEFAULT_SIZE,
                DEFAULT_SIZE,
                DEFAULT_SIZE,
                true,
                DEFAULT_ICON_SCALE,
                DEFAULT_ICON_OFFSET,
                DEFAULT_ICON_OFFSET,
                null);
    }

    /**
     * 换掉底图（未选中、选中、悬停一起换）。
     *
     * @param texture         未选中底图
     * @param selectedTexture 选中底图
     * @param hoveredTexture  悬停底图
     * @return 新样式
     */
    public CreativeSectionStyle withTexture(ResourceLocation texture, ResourceLocation selectedTexture,
                                            ResourceLocation hoveredTexture) {
        return new CreativeSectionStyle(texture, selectedTexture, hoveredTexture, width, height, textureWidth,
                textureHeight, renderIcon, iconScale, iconOffsetX, iconOffsetY, tooltip);
    }

    /**
     * 换掉未选中与选中底图，悬停底图沿用默认的。
     *
     * @param texture         未选中底图
     * @param selectedTexture 选中底图
     * @return 新样式
     */
    public CreativeSectionStyle withTexture(ResourceLocation texture, ResourceLocation selectedTexture) {
        return withTexture(texture, selectedTexture, hoveredTexture);
    }

    /**
     * 三张底图都用同一张。
     *
     * @param texture 底图
     * @return 新样式
     */
    public CreativeSectionStyle withTexture(ResourceLocation texture) {
        return withTexture(texture, texture, texture);
    }

    /**
     * 只换鼠标悬停底图。
     *
     * @param hoveredTexture 悬停底图，null 表示悬停时不做额外高亮
     * @return 新样式
     */
    public CreativeSectionStyle withHoveredTexture(@Nullable ResourceLocation hoveredTexture) {
        return new CreativeSectionStyle(texture, selectedTexture, hoveredTexture, width, height, textureWidth,
                textureHeight, renderIcon, iconScale, iconOffsetX, iconOffsetY, tooltip);
    }

    /**
     * 设置按钮尺寸。
     *
     * @param width  宽
     * @param height 高
     * @return 新样式
     */
    public CreativeSectionStyle withSize(int width, int height) {
        return new CreativeSectionStyle(texture, selectedTexture, hoveredTexture, width, height,
                width, height, renderIcon, iconScale, iconOffsetX, iconOffsetY, tooltip);
    }

    /**
     * 设置底图的实际像素尺寸（按钮被拉伸或缩小时用来对齐采样区域）。
     *
     * @param textureWidth  底图宽
     * @param textureHeight 底图高
     * @return 新样式
     */
    public CreativeSectionStyle withTextureSize(int textureWidth, int textureHeight) {
        return new CreativeSectionStyle(texture, selectedTexture, hoveredTexture, width, height,
                textureWidth, textureHeight, renderIcon, iconScale, iconOffsetX, iconOffsetY, tooltip);
    }

    /**
     * 是否在按钮上画物品。
     *
     * @param renderIcon 是否画
     * @return 新样式
     */
    public CreativeSectionStyle withIcon(boolean renderIcon) {
        return new CreativeSectionStyle(texture, selectedTexture, hoveredTexture, width, height, textureWidth,
                textureHeight, renderIcon, iconScale, iconOffsetX, iconOffsetY, tooltip);
    }

    /**
     * 设置物品渲染的缩放（1.0 是原尺寸 16×16，0.5 是 8×8）。
     *
     * @param iconScale 缩放
     * @return 新样式
     */
    public CreativeSectionStyle withIconScale(float iconScale) {
        return new CreativeSectionStyle(texture, selectedTexture, hoveredTexture, width, height, textureWidth,
                textureHeight, renderIcon, iconScale, iconOffsetX, iconOffsetY, tooltip);
    }

    /**
     * 设置物品渲染的偏移（相对按钮左上角）。
     *
     * @param x 横向偏移
     * @param y 纵向偏移
     * @return 新样式
     */
    public CreativeSectionStyle withIconOffset(int x, int y) {
        return new CreativeSectionStyle(texture, selectedTexture, hoveredTexture, width, height, textureWidth,
                textureHeight, renderIcon, iconScale, x, y, tooltip);
    }

    /**
     * 设置鼠标悬停时的提示文本。
     * <p>
     * 传 null 表示用默认的“分区名 + 换行 + 按住 Shift 可多选”。
     *
     * @param tooltip 提示文本
     * @return 新样式
     */
    public CreativeSectionStyle withTooltip(@Nullable Component tooltip) {
        return new CreativeSectionStyle(texture, selectedTexture, hoveredTexture, width, height, textureWidth,
                textureHeight, renderIcon, iconScale, iconOffsetX, iconOffsetY, tooltip);
    }

    /**
     * 算出这个分区实际要显示的提示文本。
     *
     * @param sectionName 分区名称
     * @return 提示文本
     */
    public Component tooltipFor(Component sectionName) {
        if (this.tooltip != null) {
            return this.tooltip;
        }
        return Component.empty()
                .append(sectionName)
                .append("\n")
                .append(Component.translatable("message." + RedstoneChanLIB.MODID + ".creative_tab.shift_hint"));
    }
}
