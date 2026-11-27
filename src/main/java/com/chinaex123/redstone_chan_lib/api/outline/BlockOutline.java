package com.chinaex123.redstone_chan_lib.api.outline;

import com.chinaex123.redstone_chan_lib.network.OutlineNetwork;
import com.chinaex123.redstone_chan_lib.util.PortalTexts;
import com.chinaex123.redstone_chan_lib.world.BlockOutlineSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 方块描边 API。
 * <p>
 * 为某个方块位置设置描边：客户端直接记录到本地映射，
 * 服务端则写入该维度的存档数据并广播给附近的客户端。
 * <p>
 * 共有五种模式：发光描边、黑色描边、高光、机械动力风格的方框描边，以及纯线框。
 * 机械动力风格自带“淡入淡出”动画：出现时一边变亮一边变粗，消失时一边变暗一边变细。
 * 线框模式每条棱只画一条细线，适合一次标很多方块（比如探矿扫描）。
 * <p>
 * 示例：
 * <pre>{@code
 * BlockOutline.apply(level, pos, BlockOutline.Mode.GLOW, 0xFFFFAA00);
 * BlockOutline.apply(level, pos, BlockOutline.Mode.CREATE);
 * BlockOutline.clear(level, pos);
 * }</pre>
 */
public final class BlockOutline {

    /** 描边模式 */
    public enum Mode {
        /** 发光描边 */
        GLOW,
        /** 黑色描边 */
        BLACK,
        /** 高光描边 */
        HIGHLIGHT,
        /** 机械动力风格 */
        CREATE,
        /** 线框 */
        WIRE
    }

    /** 颜色风格 */
    public enum Style {
        /** 纯色 */
        SOLID,
        /** 彩色 */
        RAINBOW
    }

    /**
     * 方块描边配置。
     *
     * @param mode         描边模式
     * @param rgb          颜色，只取低 24 位
     * @param style        颜色风格
     * @param thickness    描边粗细，单位是方块，越大越粗；机械动力风格完全显示时就是这个粗细
     * @param throughWalls 是否穿透方块（关掉后会被地形挡住）
     * @param keepVisible  是否一直显示；只有机械动力风格有意义，关掉时“显示一段时间后自动消失”
     * @param holdSeconds  机械动力风格保持完全显示的时长（秒）
     */
    public record Outline(Mode mode, int rgb, Style style, double thickness, boolean throughWalls,
                          boolean keepVisible, double holdSeconds) {
    }

    /** 默认描边粗细 */
    public static final double DEFAULT_THICKNESS = 0.045D;
    /** 机械动力风格的默认描边粗细，比其它模式粗一些 */
    public static final double CREATE_THICKNESS = 0.09D;
    /** 线框模式的默认粗细：0.01 格换算成 1 像素，就是一条细线 */
    public static final double WIRE_THICKNESS = 0.01D;
    /** 默认颜色风格 */
    public static final Style DEFAULT_STYLE = Style.SOLID;
    /** 默认是否穿透方块 */
    public static final boolean DEFAULT_THROUGH_WALLS = true;
    /** 机械动力风格默认不是一直显示，而是“显示一下就消失” */
    public static final boolean DEFAULT_KEEP_VISIBLE = false;
    /** 机械动力风格默认保持显示的时长（秒） */
    public static final double DEFAULT_HOLD_SECONDS = 1.5D;
    /** 机械动力风格的默认颜色。 */
    private static final int DEFAULT_CREATE_COLOR = 0xFF65CAFC;

    /** 客户端本地方块描边记录。 */
    private static final Map<BlockPos, Outline> CLIENT_OUTLINES = Collections.synchronizedMap(new ConcurrentHashMap<>());
    /** 客户端每个位置的写入序号，用来让渲染器判断“又被设置了一次”。 */
    private static final Map<BlockPos, Long> CLIENT_STAMPS = new ConcurrentHashMap<>();

    /** 写入序号的来源。 */
    private static final java.util.concurrent.atomic.AtomicLong STAMP = new java.util.concurrent.atomic.AtomicLong();

    private BlockOutline() {
    }

    /**
     * 以默认颜色与默认粗细设置方块描边。
     *
     * @param level 所在维度
     * @param pos   方块位置
     * @param mode  描边模式
     */
    public static void apply(Level level, BlockPos pos, Mode mode) {
        apply(level, pos, mode, defaultColor(mode), DEFAULT_STYLE, defaultThickness(mode), DEFAULT_THROUGH_WALLS,
                DEFAULT_KEEP_VISIBLE, DEFAULT_HOLD_SECONDS);
    }

    /**
     * 以默认粗细设置方块描边。
     *
     * @param level 所在维度
     * @param pos   方块位置
     * @param mode  描边模式
     * @param rgb   颜色
     */
    public static void apply(Level level, BlockPos pos, Mode mode, int rgb) {
        apply(level, pos, mode, rgb, DEFAULT_STYLE, defaultThickness(mode), DEFAULT_THROUGH_WALLS,
                DEFAULT_KEEP_VISIBLE, DEFAULT_HOLD_SECONDS);
    }

    /**
     * 指定颜色风格的方块描边，粗细取该模式的默认值。
     *
     * @param level 所在维度
     * @param pos   方块位置
     * @param mode  描边模式
     * @param rgb   颜色
     * @param style 颜色风格
     */
    public static void apply(Level level, BlockPos pos, Mode mode, int rgb, Style style) {
        apply(level, pos, mode, rgb, style, defaultThickness(mode), DEFAULT_THROUGH_WALLS,
                DEFAULT_KEEP_VISIBLE, DEFAULT_HOLD_SECONDS);
    }

    /**
     * 设置方块描边，颜色风格取纯色。
     *
     * @param level        所在维度
     * @param pos          方块位置
     * @param mode         描边模式
     * @param rgb          颜色
     * @param thickness    描边粗细
     * @param throughWalls 是否穿透方块
     */
    public static void apply(Level level, BlockPos pos, Mode mode, int rgb, double thickness, boolean throughWalls) {
        apply(level, pos, mode, rgb, DEFAULT_STYLE, thickness, throughWalls,
                DEFAULT_KEEP_VISIBLE, DEFAULT_HOLD_SECONDS);
    }

    /**
     * 设置方块描边，机械动力风格采用“显示一下就消失”。
     *
     * @param level        所在维度
     * @param pos          方块位置
     * @param mode         描边模式
     * @param rgb          颜色
     * @param style        颜色风格
     * @param thickness    描边粗细
     * @param throughWalls 是否穿透方块
     */
    public static void apply(Level level, BlockPos pos, Mode mode, int rgb, Style style, double thickness, boolean throughWalls) {
        apply(level, pos, mode, rgb, style, thickness, throughWalls, DEFAULT_KEEP_VISIBLE, DEFAULT_HOLD_SECONDS);
    }

    /**
     * 设置一个一直显示的机械动力风格描边。
     *
     * @param level        所在维度
     * @param pos          方块位置
     * @param mode         描边模式
     * @param rgb          颜色
     * @param keepVisible  是否一直显示
     * @param holdSeconds  保持完全显示的时长（秒），一直显示时没有意义
     */
    public static void apply(Level level, BlockPos pos, Mode mode, int rgb, boolean keepVisible, double holdSeconds) {
        apply(level, pos, mode, rgb, DEFAULT_STYLE, defaultThickness(mode), DEFAULT_THROUGH_WALLS,
                keepVisible, holdSeconds);
    }

    /**
     * 设置方块描边。
     *
     * @param level        所在维度
     * @param pos          方块位置
     * @param mode         描边模式
     * @param rgb          颜色
     * @param style        颜色风格
     * @param thickness    描边粗细
     * @param throughWalls 是否穿透方块
     * @param keepVisible  是否一直显示（只对机械动力风格有意义）
     * @param holdSeconds  保持完全显示的时长（秒）
     */
    public static void apply(Level level, BlockPos pos, Mode mode, int rgb, Style style, double thickness,
                             boolean throughWalls, boolean keepVisible, double holdSeconds) {
        if (level == null) {
            throw new IllegalArgumentException(PortalTexts.text("outline_null_level"));
        }
        if (pos == null) {
            throw new IllegalArgumentException(PortalTexts.text("outline_null_pos"));
        }
        if (mode == null) {
            throw new IllegalArgumentException(PortalTexts.text("outline_null_mode"));
        }
        if (!(thickness > 0.0D)) {
            throw new IllegalArgumentException(PortalTexts.text("outline_invalid_thickness", thickness));
        }
        Outline outline = new Outline(mode, rgb, style == null ? DEFAULT_STYLE : style, thickness, throughWalls,
                keepVisible, Math.max(0.0D, holdSeconds));
        if (level.isClientSide) {
            CLIENT_OUTLINES.put(pos.immutable(), outline);
            markStamped(pos);
            return;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockOutlineSavedData.get(serverLevel).put(pos, outline);
        OutlineNetwork.broadcastBlock(serverLevel, pos, outline);
    }

    /**
     * 取某个模式的默认粗细：机械动力风格粗一点，其它模式细一点。
     *
     * @param mode 描边模式
     * @return 默认粗细
     */
    public static double defaultThickness(Mode mode) {
        return switch (mode) {
            case CREATE -> CREATE_THICKNESS;
            case WIRE -> WIRE_THICKNESS;
            default -> DEFAULT_THICKNESS;
        };
    }

    /**
     * 清除方块描边。
     *
     * @param level 所在维度
     * @param pos   方块位置
     */
    public static void clear(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return;
        }
        if (level.isClientSide) {
            CLIENT_OUTLINES.remove(pos);
            synchronized (CLIENT_STAMPS) {
                CLIENT_STAMPS.remove(pos);
            }
            return;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockOutlineSavedData.get(serverLevel).remove(pos);
        OutlineNetwork.broadcastBlock(serverLevel, pos, null);
    }

    /**
     * 清除客户端所有方块描边记录。
     */
    public static void clearAll() {
        CLIENT_OUTLINES.clear();
        synchronized (CLIENT_STAMPS) {
            CLIENT_STAMPS.clear();
        }
    }

    /**
     * 判断方块位置是否有描边。
     *
     * @param level 所在维度
     * @param pos   方块位置
     * @return 有描边返回 true
     */
    public static boolean isActive(Level level, BlockPos pos) {
        return get(level, pos) != null;
    }

    /**
     * 获取方块描边配置。
     *
     * @param level 所在维度
     * @param pos   方块位置
     * @return 描边配置，没有时返回 null
     */
    @Nullable
    public static Outline get(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return null;
        }
        if (level.isClientSide) {
            return CLIENT_OUTLINES.get(pos);
        }
        return level instanceof ServerLevel serverLevel ? BlockOutlineSavedData.get(serverLevel).get(pos) : null;
    }

    /**
     * 获取客户端所有带描边的方块位置（渲染用）。
     *
     * @return 位置列表
     */
    public static List<BlockPos> activePositions() {
        synchronized (CLIENT_OUTLINES) {
            return new ArrayList<>(CLIENT_OUTLINES.keySet());
        }
    }

    /**
     * 获取客户端描边表。
     *
     * @return 位置到描边的映射
     */
    public static Map<BlockPos, Outline> clientOutlines() {
        return Collections.unmodifiableMap(CLIENT_OUTLINES);
    }

    /**
     * 直接写入或移除客户端记录。
     *
     * @param pos     方块位置
     * @param outline 描边配置，传 null 表示移除
     */
    public static void putLocal(BlockPos pos, @Nullable Outline outline) {
        if (pos == null) {
            return;
        }
        if (outline == null) {
            CLIENT_OUTLINES.remove(pos);
            synchronized (CLIENT_STAMPS) {
                CLIENT_STAMPS.remove(pos);
            }
        } else {
            CLIENT_OUTLINES.put(pos.immutable(), outline);
            markStamped(pos);
        }
    }

    /**
     * 取某个位置的写入序号（渲染用）。
     *
     * @param pos 方块位置
     * @return 序号，没有记录时是 0
     */
    public static long stamp(BlockPos pos) {
        synchronized (CLIENT_STAMPS) {
            return CLIENT_STAMPS.getOrDefault(pos, 0L);
        }
    }

    /**
     * 更新某个位置的写入序号。
     *
     * @param pos 方块位置
     */
    private static void markStamped(BlockPos pos) {
        synchronized (CLIENT_STAMPS) {
            CLIENT_STAMPS.put(pos.immutable(), STAMP.incrementAndGet());
        }
    }

    /**
     * 获取指定模式的默认颜色。
     *
     * @param mode 描边模式
     * @return 默认颜色
     */
    public static int defaultColor(Mode mode) {
        return switch (mode) {
            case BLACK -> 0xFF000000;
            case HIGHLIGHT, GLOW, WIRE -> 0xFFFFFFFF;
            case CREATE -> DEFAULT_CREATE_COLOR;
        };
    }
}
