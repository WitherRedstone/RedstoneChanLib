package com.chinaex123.redstone_chan_lib.api.creative;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 带分区的创造模式物品栏。
 * <p>
 * 用法：先用 {@link #builder(ResourceLocation)} 描述这个页签（标题、图标、样式、一个个分区），
 * 再 {@link Builder#build()}；把 {@link #tab()} 交给自己的 {@code DeferredRegister} 注册即可。
 * <pre>{@code
 * SectionedCreativeTab example = SectionedCreativeTab.builder(MyMod.id("example_tab"))
 *         .title(Component.translatable("itemGroup.my_mod"))
 *         .icon(() -> new ItemStack(Items.DIAMOND))
 *         .section(CreativeSection.of(Component.literal("材料"), () -> new ItemStack(Items.IRON_INGOT),
 *                 output -> output.accept(Items.IRON_INGOT)))
 *         .build();
 * }</pre>
 * 页签进游戏后左侧会出现每个分区一个按钮，点击只显示该分区，按住 Shift 点击可以同时选多个。
 */
public final class SectionedCreativeTab {

    private final ResourceLocation id;
    private final CreativeModeTab tab;
    private final List<CreativeSection> sections;
    private final CreativeSectionStyle style;

    private SectionedCreativeTab(ResourceLocation id, CreativeModeTab tab, List<CreativeSection> sections,
                                 CreativeSectionStyle style) {
        this.id = id;
        this.tab = tab;
        this.sections = List.copyOf(sections);
        this.style = style;
    }

    /**
     * 开始描述一个带分区的创造栏。
     *
     * @param id 页签 id，只用来在注册表里定位这个页签
     * @return 构造器
     */
    public static Builder builder(ResourceLocation id) {
        return new Builder(Objects.requireNonNull(id, "页签 id 不能为空"));
    }

    /**
     * 页签 id。
     *
     * @return 页签 id
     */
    public ResourceLocation id() {
        return this.id;
    }

    /**
     * 生成好的页签，交给自己的 {@code DeferredRegister} 注册。
     *
     * @return 创造模式页签
     */
    public CreativeModeTab tab() {
        return this.tab;
    }

    /**
     * 分区列表。
     *
     * @return 分区列表
     */
    public List<CreativeSection> sections() {
        return this.sections;
    }

    /**
     * 默认样式。
     *
     * @return 默认样式
     */
    public CreativeSectionStyle style() {
        return this.style;
    }

    /**
     * 页签构造器。
     */
    public static final class Builder {

        private final ResourceLocation id;
        private final List<CreativeSection> sections = new ArrayList<>();
        private Component title;
        private Supplier<ItemStack> icon = () -> ItemStack.EMPTY;
        private CreativeSectionStyle style = CreativeSectionStyle.defaults();

        private Builder(ResourceLocation id) {
            this.id = id;
        }

        /**
         * 设置页签标题（显示在创造栏顶部与提示里）。
         *
         * @param title 标题
         * @return 自己
         */
        public Builder title(Component title) {
            this.title = Objects.requireNonNull(title, "页签标题不能为空");
            return this;
        }

        /**
         * 设置页签图标（创造栏那一排标签上的图标）。
         *
         * @param icon 图标
         * @return 自己
         */
        public Builder icon(Supplier<ItemStack> icon) {
            this.icon = Objects.requireNonNull(icon, "页签图标不能为空");
            return this;
        }

        /**
         * 设置默认样式，所有没有单独样式的分区都用它。
         *
         * @param style 样式
         * @return 自己
         */
        public Builder style(CreativeSectionStyle style) {
            this.style = Objects.requireNonNull(style, "默认样式不能为空");
            return this;
        }

        /**
         * 加一个分区（用默认样式）。
         *
         * @param name    分区名称
         * @param icon    分区图标
         * @param content 填物品逻辑
         * @return 自己
         */
        public Builder section(Component name, Supplier<ItemStack> icon, Consumer<CreativeModeTab.Output> content) {
            this.sections.add(CreativeSection.of(name, icon, content));
            return this;
        }

        /**
         * 加一个分区（带独立样式）。
         *
         * @param section 分区
         * @return 自己
         */
        public Builder section(CreativeSection section) {
            this.sections.add(Objects.requireNonNull(section, "分区不能为空"));
            return this;
        }

        /**
         * 生成页签并登记到 {@link CreativeSections}。
         *
         * @return 带分区的创造栏
         */
        public SectionedCreativeTab build() {
            if (this.title == null) {
                throw new IllegalStateException("创造栏 " + this.id + " 还没有设置标题");
            }
            if (this.sections.isEmpty()) {
                throw new IllegalStateException("创造栏 " + this.id + " 至少需要一个分区");
            }
            List<CreativeSection> copy = List.copyOf(this.sections);
            CreativeModeTab tab = CreativeModeTab.builder()
                    .title(this.title)
                    .icon(this.icon)
                    .displayItems((parameters, output) -> {
                        CreativeSections.Entry entry = CreativeSections.byId(this.id);
                        if (entry != null) {
                            CreativeSections.addItems(entry.tab(), output);
                        }
                    })
                    .build();
            CreativeSections.register(this.id, tab, copy, this.style);
            return new SectionedCreativeTab(this.id, tab, copy, this.style);
        }
    }
}
