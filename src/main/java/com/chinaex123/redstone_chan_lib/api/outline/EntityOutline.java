package com.chinaex123.redstone_chan_lib.api.outline;

import com.chinaex123.redstone_chan_lib.network.OutlineNetwork;
import com.chinaex123.redstone_chan_lib.util.PortalTexts;
import com.chinaex123.redstone_chan_lib.world.OutlineSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 实体描边 API。
 * <p>
 * 为实体设置描边效果：客户端直接记录到本地弱引用映射，
 * 服务端则写入存档数据并广播给客户端。
 * 提供描边模式、颜色风格、粗细与是否穿透方块的配置，
 * 以及描边的查询、清除与默认颜色获取。
 */
public final class EntityOutline {

    /** 描边模式 */
    public enum Mode {
        /** 发光描边 */
        GLOW,
        /** 黑色描边 */
        BLACK,
        /** 高光描边 */
        HIGHLIGHT
    }

    /** 颜色风格 */
    public enum Style {
        /** 纯色 */
        SOLID,
        /** 彩色 */
        RAINBOW
    }

    /**
     * 描边配置。
     *
     * @param mode        描边模式
     * @param rgb         颜色
     * @param style       颜色风格
     * @param thickness   描边粗细
     * @param throughWalls 是否穿透方块
     */
    public record Outline(Mode mode, int rgb, Style style, double thickness, boolean throughWalls) {
    }

    /** 默认描边粗细 */
    public static final double DEFAULT_THICKNESS = 0.08D;

    /** 默认是否穿透方块 */
    public static final boolean DEFAULT_THROUGH_WALLS = true;

    /** 内置默认颜色：发光与高光是白色，黑色描边是纯黑 */
    private static final int DEFAULT_GLOW_COLOR = 0xFFFFFFFF;
    private static final int DEFAULT_BLACK_COLOR = 0xFF000000;
    private static final int DEFAULT_HIGHLIGHT_COLOR = 0xFFFFFFFF;

    /** 客户端本地描边记录，弱引用避免泄漏。 */
    private static final Map<Entity, Outline> CLIENT_OUTLINES = Collections.synchronizedMap(new WeakHashMap<>());

    private EntityOutline() {
    }

    /**
     * 以默认颜色、纯色风格与默认粗细设置描边。
     *
     * @param entity 目标实体
     * @param mode   描边模式
     */
    public static void apply(Entity entity, Mode mode) {
        apply(entity, mode, defaultColor(mode), Style.SOLID, DEFAULT_THICKNESS);
    }

    /**
     * 以纯色风格与默认粗细设置描边。
     *
     * @param entity 目标实体
     * @param mode   描边模式
     * @param rgb    颜色
     */
    public static void apply(Entity entity, Mode mode, int rgb) {
        apply(entity, mode, rgb, Style.SOLID, DEFAULT_THICKNESS);
    }

    /**
     * 以默认粗细设置描边。
     *
     * @param entity 目标实体
     * @param mode   描边模式
     * @param rgb    颜色
     * @param style  颜色风格
     */
    public static void apply(Entity entity, Mode mode, int rgb, Style style) {
        apply(entity, mode, rgb, style, DEFAULT_THICKNESS);
    }

    /**
     * 以默认穿透设置设置描边。
     *
     * @param entity    目标实体
     * @param mode      描边模式
     * @param rgb       颜色
     * @param style     颜色风格
     * @param thickness 描边粗细
     */
    public static void apply(Entity entity, Mode mode, int rgb, Style style, double thickness) {
        apply(entity, mode, rgb, style, thickness, DEFAULT_THROUGH_WALLS);
    }

    /**
     * 设置实体描边。
     * <p>
     * 校验参数后构造描边配置：客户端写入本地映射；
     * 服务端写入存档数据并广播到客户端。
     *
     * @param entity       目标实体
     * @param mode         描边模式
     * @param rgb          颜色
     * @param style        颜色风格
     * @param thickness    描边粗细
     * @param throughWalls 是否穿透方块
     */
    public static void apply(Entity entity, Mode mode, int rgb, Style style, double thickness, boolean throughWalls) {
        if (entity == null) {
            throw new IllegalArgumentException(PortalTexts.text("outline_null_target"));
        }
        if (mode == null) {
            throw new IllegalArgumentException(PortalTexts.text("outline_null_mode"));
        }
        if (!(thickness > 0.0D)) {
            throw new IllegalArgumentException(PortalTexts.text("outline_invalid_thickness", thickness));
        }
        Outline outline = new Outline(mode, rgb, style == null ? Style.SOLID : style, thickness, throughWalls);
        if (entity.level().isClientSide) {
            CLIENT_OUTLINES.put(entity, outline);
            return;
        }
        MinecraftServer server = entity.getServer();
        if (server == null) {
            return;
        }
        OutlineSavedData.get(server).put(entity.getUUID(), outline);
        OutlineNetwork.broadcast(entity, outline);
    }

    /**
     * 清除实体描边。
     * <p>
     * 客户端移除本地记录；服务端移除存档数据并广播清除。
     *
     * @param entity 目标实体
     */
    public static void clear(Entity entity) {
        if (entity == null) {
            return;
        }
        if (entity.level().isClientSide) {
            CLIENT_OUTLINES.remove(entity);
            return;
        }
        MinecraftServer server = entity.getServer();
        if (server == null) {
            return;
        }
        OutlineSavedData.get(server).remove(entity.getUUID());
        OutlineNetwork.broadcast(entity, null);
    }

    /**
     * 清除所有本地描边记录。
     */
    public static void clearAll() {
        CLIENT_OUTLINES.clear();
    }

    /**
     * 判断实体是否具有描边。
     *
     * @param entity 目标实体
     * @return 具有描边返回 true
     */
    public static boolean isActive(Entity entity) {
        return get(entity) != null;
    }

    /**
     * 获取实体描边配置。
     *
     * @param entity 目标实体
     * @return 描边配置，无描边时返回 null
     */
    @Nullable
    public static Outline get(Entity entity) {
        if (entity == null) {
            return null;
        }
        if (entity.level().isClientSide) {
            return CLIENT_OUTLINES.get(entity);
        }
        MinecraftServer server = entity.getServer();
        return server == null ? null : OutlineSavedData.get(server).get(entity.getUUID());
    }

    /**
     * 获取所有具有本地描边的实体。
     *
     * @return 实体列表
     */
    public static List<Entity> activeEntities() {
        synchronized (CLIENT_OUTLINES) {
            return new ArrayList<>(CLIENT_OUTLINES.keySet());
        }
    }

    /**
     * 直接写入或移除本地描边记录（仅客户端渲染使用）。
     *
     * @param entity  目标实体
     * @param outline 描边配置，传 null 表示移除
     */
    public static void putLocal(Entity entity, @Nullable Outline outline) {
        if (outline == null) {
            CLIENT_OUTLINES.remove(entity);
        } else {
            CLIENT_OUTLINES.put(entity, outline);
        }
    }

    /**
     * 获取指定描边模式的默认颜色。
     *
     * @param mode 描边模式
     * @return 默认颜色
     */
    public static int defaultColor(Mode mode) {
        return switch (mode) {
            case BLACK -> DEFAULT_BLACK_COLOR;
            case HIGHLIGHT -> DEFAULT_HIGHLIGHT_COLOR;
            case GLOW -> DEFAULT_GLOW_COLOR;
        };
    }
}
