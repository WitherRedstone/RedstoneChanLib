package com.chinaex123.redstone_chan_lib.client.creative;

import com.chinaex123.redstone_chan_lib.api.creative.CreativeSection;
import com.chinaex123.redstone_chan_lib.api.creative.CreativeSectionStyle;
import com.chinaex123.redstone_chan_lib.api.creative.CreativeSections;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * 创造模式物品栏左侧的分区按钮。
 * <p>
 * 根据分区样式的尺寸绘制底图，并按分区是否处于激活状态切换选中底图；
 * 样式允许时在其上绘制分区图标，悬停提示由样式根据分区名称生成。
 */
public class CreativeTabSectionButton extends Button {

    /** 所属页签。 */
    private final CreativeModeTab tab;
    /** 分区下标。 */
    private final int index;
    /** 分区。 */
    private final CreativeSection section;
    /** 生效的样式。 */
    private final CreativeSectionStyle style;

    /**
     * 构造一个分区按钮。
     *
     * @param x       屏幕 x 坐标
     * @param y       屏幕 y 坐标
     * @param tab     所属页签
     * @param index   分区下标
     * @param section 分区
     * @param style   生效的样式
     * @param onPress 点击回调
     */
    public CreativeTabSectionButton(int x, int y, CreativeModeTab tab, int index, CreativeSection section,
                                    CreativeSectionStyle style, OnPress onPress) {
        super(x, y, style.width(), style.height(), Component.empty(), onPress, Button.DEFAULT_NARRATION);
        this.tab = tab;
        this.index = index;
        this.section = section;
        this.style = style;
        Component tooltip = style.tooltipFor(section.name());
        if (!tooltip.getString().isEmpty()) {
            this.setTooltip(Tooltip.create(tooltip));
        }
    }

    /**
     * 分区当前是否处于选中状态。
     *
     * @return 是否选中
     */
    public boolean isSectionSelected() {
        return CreativeSections.isActive(this.tab, this.index);
    }

    /**
     * 按钮的样式。
     *
     * @return 样式
     */
    public CreativeSectionStyle style() {
        return this.style;
    }

    /**
     * 渲染按钮：先铺底图，再按需叠悬停高亮与物品。
     * <p>
     * 先按分区激活状态选择选中或普通底图并绘制；
     * 鼠标悬停时再把悬停底图叠画在同一位置（默认那张中间是透明的，只留一圈高亮边，
     * 所以不会盖住底图表达的选中状态）；样式允许时按设定的缩放与偏移绘制分区图标。
     *
     * @param graphics    绘制上下文
     * @param mouseX      鼠标 x 坐标
     * @param mouseY      鼠标 y 坐标
     * @param partialTick 插值刻
     */
    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean selected = this.isSectionSelected();
        this.blitTexture(graphics, selected ? this.style.selectedTexture() : this.style.texture());

        if (this.isHovered || this.isFocused()) {
            ResourceLocation hovered = this.style.hoveredTexture();
            if (hovered != null) {
                this.blitTexture(graphics, hovered);
            }
        }

        if (!this.style.renderIcon()) {
            return;
        }
        ItemStack icon = this.section.icon().get();
        if (icon.isEmpty()) {
            return;
        }
        float scale = this.style.iconScale();
        graphics.pose().pushPose();
        graphics.pose().translate(this.getX() + this.style.iconOffsetX(), this.getY() + this.style.iconOffsetY(), 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.renderItem(icon, 0, 0);
        graphics.pose().popPose();
    }

    /**
     * 按按钮尺寸铺一张底图。
     *
     * @param graphics 绘制上下文
     * @param texture  底图
     */
    private void blitTexture(GuiGraphics graphics, ResourceLocation texture) {
        graphics.blit(texture,
                this.getX(), this.getY(), this.style.width(), this.style.height(),
                0.0F, 0.0F, this.style.textureWidth(), this.style.textureHeight(),
                this.style.textureWidth(), this.style.textureHeight());
    }
}
