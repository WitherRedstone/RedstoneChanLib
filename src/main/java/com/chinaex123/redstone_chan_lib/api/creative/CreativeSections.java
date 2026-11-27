package com.chinaex123.redstone_chan_lib.api.creative;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 分区的注册表与选中状态。
 * <p>
 * 每个“带分区的创造栏”在这里登记一次：页签本身、分区列表、默认样式。
 * 客户端界面按页签实例查回来画按钮，服务端与客户端都按页签实例查回来决定往列表里填哪些物品。
 * 选中状态按页签分开保存，默认全选。
 */
public final class CreativeSections {

    /** 每个页签的登记信息。 */
    private static final Map<CreativeModeTab, Entry> BY_TAB = new ConcurrentHashMap<>();

    /** 按页签 id 的登记信息，方便外部按 id 查询。 */
    private static final Map<ResourceLocation, Entry> BY_ID = new ConcurrentHashMap<>();

    /** 每个页签当前选中的分区下标。 */
    private static final Map<CreativeModeTab, Set<Integer>> ACTIVE = new ConcurrentHashMap<>();

    private CreativeSections() {
    }

    /**
     * 登记一个带分区的创造栏。
     *
     * @param id       页签 id
     * @param tab      页签实例
     * @param sections 分区列表
     * @param style    默认样式
     */
    static void register(ResourceLocation id, CreativeModeTab tab, List<CreativeSection> sections,
                         CreativeSectionStyle style) {
        Entry entry = new Entry(id, tab, List.copyOf(sections), style);
        BY_TAB.put(tab, entry);
        BY_ID.put(id, entry);
        Set<Integer> active = Collections.synchronizedSet(new LinkedHashSet<>());
        for (int i = 0; i < sections.size(); i++) {
            active.add(i);
        }
        ACTIVE.put(tab, active);
    }

    /**
     * 按页签实例取登记信息。
     *
     * @param tab 页签
     * @return 登记信息，没有则返回 null
     */
    @Nullable
    public static Entry byTab(CreativeModeTab tab) {
        return tab == null ? null : BY_TAB.get(tab);
    }

    /**
     * 按页签 id 取登记信息。
     *
     * @param id 页签 id
     * @return 登记信息，没有则返回 null
     */
    @Nullable
    public static Entry byId(ResourceLocation id) {
        return id == null ? null : BY_ID.get(id);
    }

    /**
     * 判断某个页签是不是带分区的。
     *
     * @param tab 页签
     * @return 是否带分区
     */
    public static boolean has(CreativeModeTab tab) {
        return byTab(tab) != null;
    }

    /**
     * 判断某个分区当前是否选中。
     *
     * @param tab   页签
     * @param index 分区下标
     * @return 是否选中
     */
    public static boolean isActive(CreativeModeTab tab, int index) {
        Set<Integer> active = ACTIVE.get(tab);
        return active != null && active.contains(index);
    }

    /**
     * 只选中一个分区。
     *
     * @param tab   页签
     * @param index 分区下标
     */
    public static void selectSection(CreativeModeTab tab, int index) {
        Set<Integer> active = ACTIVE.get(tab);
        if (active == null) {
            return;
        }
        synchronized (active) {
            active.clear();
            active.add(index);
        }
    }

    /**
     * 切换一个分区的选中状态，至少保留一个分区。
     *
     * @param tab   页签
     * @param index 分区下标
     */
    public static void toggleSection(CreativeModeTab tab, int index) {
        Set<Integer> active = ACTIVE.get(tab);
        if (active == null) {
            return;
        }
        synchronized (active) {
            if (active.contains(index)) {
                // 至少留一个分区，否则创造栏会空掉
                if (active.size() > 1) {
                    active.remove(index);
                }
            } else {
                active.add(index);
            }
        }
    }

    /**
     * 把当前选中的分区的物品填进创造栏。
     * <p>
     * 未选中的分区以“只进搜索结果”的方式注册，这样它们在创造栏里看不到，但搜索仍然找得到。
     *
     * @param tab    页签
     * @param output 物品栏输出
     */
    public static void addItems(CreativeModeTab tab, CreativeModeTab.Output output) {
        Entry entry = byTab(tab);
        if (entry == null) {
            return;
        }
        List<CreativeSection> sections = entry.sections();
        for (int i = 0; i < sections.size(); i++) {
            CreativeSection section = sections.get(i);
            if (isActive(tab, i)) {
                section.content().accept(output);
            } else {
                section.content().accept(new SearchOnlyOutput(output));
            }
        }
    }

    /**
     * 按页面实例取分区列表。
     *
     * @param tab 页签
     * @return 分区列表，没有则返回空表
     */
    public static List<CreativeSection> sections(CreativeModeTab tab) {
        Entry entry = byTab(tab);
        return entry == null ? List.of() : entry.sections();
    }

    /**
     * 取某个分区实际生效的样式（分区自己的优先，否则用页签默认样式）。
     *
     * @param entry 登记信息
     * @param index 分区下标
     * @return 样式
     */
    public static CreativeSectionStyle styleOf(Entry entry, int index) {
        List<CreativeSection> sections = entry.sections();
        if (index < 0 || index >= sections.size()) {
            return entry.style();
        }
        CreativeSectionStyle style = sections.get(index).style();
        return style == null ? entry.style() : style;
    }

    /**
     * 一个带分区的创造栏的登记信息。
     *
     * @param id       页签 id
     * @param tab      页签实例
     * @param sections 分区列表
     * @param style    默认样式
     */
    public record Entry(ResourceLocation id, CreativeModeTab tab, List<CreativeSection> sections,
                        CreativeSectionStyle style) {
    }

    /**
     * 只把物品放进搜索结果的输出包装。
     *
     * @param delegate 真实输出
     */
    private record SearchOnlyOutput(CreativeModeTab.Output delegate) implements CreativeModeTab.Output {

        @Override
        public void accept(ItemStack stack, CreativeModeTab.TabVisibility visibility) {
            this.delegate.accept(stack, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        }
    }

    /**
     * 取某个页签所有分区的下标（给界面按顺序建按钮用）。
     *
     * @param tab 页签
     * @return 下标列表
     */
    public static List<Integer> indices(CreativeModeTab tab) {
        Entry entry = byTab(tab);
        if (entry == null) {
            return List.of();
        }
        List<Integer> indices = new ArrayList<>(entry.sections().size());
        for (int i = 0; i < entry.sections().size(); i++) {
            indices.add(i);
        }
        return indices;
    }
}
