package com.chinaex123.redstone_chan_lib.api.creative;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 创造模式物品栏里的一个分区。
 * <p>
 * 分区 = 按钮上显示的图标 + 一组物品 + 可选的独立样式。创造栏会把所有“当前选中”的分区的物品
 * 依次填进去，未选中的分区不会出现在列表里，但仍然可以被搜索到。
 *
 * @param name    分区名称（显示在提示文本里）
 * @param icon    按钮上的图标
 * @param content 往物品栏里填物品的逻辑
 * @param style   这个分区专用的样式，null 表示用页签的默认样式
 */
public record CreativeSection(
        Component name,
        Supplier<ItemStack> icon,
        Consumer<CreativeModeTab.Output> content,
        @Nullable CreativeSectionStyle style) {

    /**
     * 紧凑构造器：校验必填项。
     *
     * @param name    分区名称
     * @param icon    图标
     * @param content 填物品逻辑
     * @param style   专用样式
     */
    public CreativeSection {
        Objects.requireNonNull(name, "分区名称不能为空");
        Objects.requireNonNull(icon, "分区图标不能为空");
        Objects.requireNonNull(content, "分区的物品填充逻辑不能为空");
    }

    /**
     * 创建一个使用默认样式的分区。
     *
     * @param name    分区名称
     * @param icon    图标
     * @param content 填物品逻辑
     * @return 分区
     */
    public static CreativeSection of(Component name, Supplier<ItemStack> icon,
                                     Consumer<CreativeModeTab.Output> content) {
        return new CreativeSection(name, icon, content, null);
    }

    /**
     * 创建一个带独立样式的分区。
     *
     * @param name    分区名称
     * @param icon    图标
     * @param content 填物品逻辑
     * @param style   专用样式
     * @return 分区
     */
    public static CreativeSection of(Component name, Supplier<ItemStack> icon,
                                     Consumer<CreativeModeTab.Output> content, CreativeSectionStyle style) {
        return new CreativeSection(name, icon, content, style);
    }
}
