package com.chinaex123.redstone_chan_lib.api.portal;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import com.chinaex123.redstone_chan_lib.util.PortalTexts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 传送门定义注册表。
 * <p>
 * 分别维护内置定义与数据包定义两张表，查询时数据包定义优先；
 * 提供按 ID 查找、全量列举、按框架方块匹配、框架判定与默认定义查询，
 * 并缓存是否存在仪式式激活定义，供快速判断。
 */
public final class PortalDefinitions {

    /** 内置定义表。 */
    private static final Map<ResourceLocation, PortalDefinition> BUILTIN = new ConcurrentHashMap<>();

    /** 数据包定义表。 */
    private static final Map<ResourceLocation, PortalDefinition> DATAPACK = new ConcurrentHashMap<>();

    /** 默认定义（最近注册或加载的定义）。 */
    private static volatile PortalDefinition fallback;

    /** 是否存在仪式式激活定义。 */
    private static volatile boolean hasRituals;

    private PortalDefinitions() {}

    /**
     * 注册内置定义。
     * <p>
     * 同时将其设为默认定义，并刷新仪式标志。
     *
     * @param definition 传送门定义
     */
    public static void register(PortalDefinition definition) {
        BUILTIN.put(definition.id(), definition);
        fallback = definition;
        refreshRitualFlag();
    }

    /**
     * 应用数据包定义。
     * <p>
     * 替换整个数据包定义表并记录数量；若当前默认定义在数据包中有同名定义则更新为数据包版本，
     * 否则在没有默认定义时取任一数据包定义作为默认，最后刷新仪式标志。
     *
     * @param definitions 数据包定义表
     */
    public static void applyDatapack(Map<ResourceLocation, PortalDefinition> definitions) {
        DATAPACK.clear();
        DATAPACK.putAll(definitions);
        RedstoneChanLIB.LOGGER.info(PortalTexts.text("definitions_loaded", definitions.size()));
        PortalDefinition current = fallback;
        if (current != null) {
            PortalDefinition updated = definitions.get(current.id());
            if (updated != null) {
                fallback = updated;
            }
        } else if (!definitions.isEmpty()) {
            fallback = definitions.values().iterator().next();
        }
        refreshRitualFlag();
    }

    /**
     * 刷新是否存在仪式式激活定义的标志。
     */
    private static void refreshRitualFlag() {
        boolean found = false;
        for (PortalDefinition definition : all()) {
            if (definition.ritual() != null) {
                found = true;
                break;
            }
        }
        hasRituals = found;
    }

    /**
     * 判断是否存在仪式式激活定义。
     *
     * @return 是否存在仪式式激活定义
     */
    public static boolean hasRituals() {
        return hasRituals;
    }

    /**
     * 按 ID 查找定义。
     * <p>
     * 数据包定义优先，其次为内置定义。
     *
     * @param id 定义 ID
     * @return 定义，未找到时返回 null
     */
    @Nullable
    public static PortalDefinition get(ResourceLocation id) {
        if (id == null) return null;
        PortalDefinition definition = DATAPACK.get(id);
        return definition != null ? definition : BUILTIN.get(id);
    }

    /**
     * 按字符串 ID 查找定义。
     *
     * @param id 定义 ID 字符串
     * @return 定义，无法解析或未找到时返回 null
     */
    @Nullable
    public static PortalDefinition byId(@Nullable String id) {
        if (id == null || id.isEmpty()) return null;
        return get(ResourceLocation.tryParse(id));
    }

    /**
     * 列举全部定义。
     * <p>
     * 以内置定义为基础，数据包定义按 ID 覆盖同名内置定义。
     *
     * @return 全部定义列表
     */
    public static List<PortalDefinition> all() {
        List<PortalDefinition> result = new ArrayList<>(BUILTIN.values());
        for (PortalDefinition definition : DATAPACK.values()) {
            result.removeIf(existing -> existing.id().equals(definition.id()));
            result.add(definition);
        }
        return result;
    }

    /**
     * 找出所有以指定方块状态作为框架的定义。
     *
     * @param state 方块状态
     * @return 匹配的定义列表
     */
    public static List<PortalDefinition> matchingFrames(BlockState state) {
        List<PortalDefinition> result = new ArrayList<>();
        for (PortalDefinition definition : all()) {
            if (definition.isFrame(state)) {
                result.add(definition);
            }
        }
        return result;
    }

    /**
     * 判断指定方块状态是否属于任一传送门框架。
     *
     * @param state 方块状态
     * @return 属于任一框架返回 true
     */
    public static boolean isAnyFrame(BlockState state) {
        for (PortalDefinition definition : all()) {
            if (definition.isFrame(state)) return true;
        }
        return false;
    }

    /**
     * 获取默认定义。
     * <p>
     * 优先返回缓存的默认定义，否则取全部定义中的第一个。
     *
     * @return 默认定义，无任何定义时返回 null
     */
    @Nullable
    public static PortalDefinition defaultDefinition() {
        PortalDefinition definition = fallback;
        if (definition != null) return definition;
        List<PortalDefinition> all = all();
        return all.isEmpty() ? null : all.getFirst();
    }
}
