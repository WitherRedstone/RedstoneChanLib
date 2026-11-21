package com.chinaex123.redstone_chan_lib.api.outline;

import com.chinaex123.redstone_chan_lib.network.OutlineNetwork;
import com.chinaex123.redstone_chan_lib.util.PortalTexts;
import com.chinaex123.redstone_chan_lib.world.BlockScanSavedData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

/**
 * 周围方块扫描高亮 API。
 * <p>
 * 和 {@link BlockOutline} 的区别：{@code BlockOutline} 是“指名道姓地给某个坐标加描边”，
 * 这个类则是“给一个配方，扫描玩家周围符合条件的方块并全部高亮”，
 * 效果参考透视类模组的探矿（Prospector）显示。
 * <p>
 * 目标既可以是方块（{@link Block}），也可以是方块标签（{@link TagKey}），两者可以混用。
 * 扫描以玩家为中心，半径可以自己定。
 * <p>
 * 高亮方式沿用 {@link BlockOutline} 的模式，默认是 {@link BlockOutline.Mode#WIRE 线框}：
 * 每个方块只画十二条细线，而且默认穿透方块，这样隔着一层石头也能看到墙里的矿，
 * 而且一次标几十上百个方块也不会糊成一片。
 * <p>
 * 示例：
 * <pre>{@code
 * BlockScanOutline.Target ores = BlockScanOutline.target(BlockTags.COAL_ORES, BlockTags.IRON_ORES);
 * BlockScanOutline.start(level, ores, 16);
 * BlockScanOutline.stop(level);
 * }</pre>
 */
public final class BlockScanOutline {

    /** 默认扫描半径（格） */
    public static final int DEFAULT_RADIUS = 16;

    /** 扫描半径上限，太大既卡又没意义 */
    public static final int MAX_RADIUS = 48;

    /**
     * 扫描目标：一组方块与一组方块标签，命中任意一个就算目标。
     */
    public record Target(List<Block> blocks, List<TagKey<Block>> tags) {

        /**
         * 判断某个方块状态是否命中目标。
         *
         * @param state 方块状态
         * @return 命中返回 true
         */
        public boolean matches(BlockState state) {
            if (state == null || state.isAir()) {
                return false;
            }
            for (Block block : blocks) {
                if (state.is(block)) {
                    return true;
                }
            }
            for (TagKey<Block> tag : tags) {
                if (state.is(tag)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * 目标是否为空（空目标不会高亮任何方块）。
         *
         * @return 空返回 true
         */
        public boolean isEmpty() {
            return blocks.isEmpty() && tags.isEmpty();
        }
    }

    /**
     * 扫描高亮的配置。
     *
     * @param mode         高亮模式，沿用方块描边那几种
     * @param rgb          统一颜色，只取低 24 位；被 {@code colors} 或 {@code tagColors} 命中的方块不受它影响
     * @param style        颜色风格（纯色 / 彩色）
     * @param thickness    高亮粗细，单位是方块
     * @param radius       扫描半径（格）
     * @param throughWalls 是否穿透方块
     * @param colors       单独指定的方块颜色（方块 → ARGB），可以为空
     * @param tagColors    按标签指定的颜色（标签 → ARGB），可以为空；比 {@code colors} 优先级低
     */
    public record Scan(BlockOutline.Mode mode, int rgb, BlockOutline.Style style, double thickness,
                       int radius, boolean throughWalls, Map<Block, Integer> colors,
                       Map<TagKey<Block>, Integer> tagColors) {

        /**
         * 把半径与粗细夹到合法范围，并把两张颜色表各复制一份。
         *
         * @return 夹过的配置
         */
        public Scan clamped() {
            int safeRadius = Math.clamp(radius, 1, MAX_RADIUS);
            double safeThickness = Math.max(0.001D, thickness);
            return new Scan(mode, rgb, style, safeThickness, safeRadius, throughWalls,
                    Map.copyOf(colors), Map.copyOf(tagColors));
        }

        /**
         * 换一份单独颜色表。
         *
         * @param colors 方块 → 颜色
         * @return 新的扫描配置
         */
        public Scan withColors(Map<Block, Integer> colors) {
            return new Scan(mode, rgb, style, thickness, radius, throughWalls,
                    Map.copyOf(colors), tagColors);
        }

        /**
         * 给某个方块单独指定颜色。
         *
         * @param block 方块
         * @param color 颜色（只用低 24 位）
         * @return 新的扫描配置
         */
        public Scan withColor(Block block, int color) {
            Map<Block, Integer> merged = new LinkedHashMap<>(colors);
            merged.put(block, color);
            return withColors(merged);
        }

        /**
         * 换一份标签颜色表。
         *
         * @param tagColors 标签 → 颜色
         * @return 新的扫描配置
         */
        public Scan withTagColors(Map<TagKey<Block>, Integer> tagColors) {
            return new Scan(mode, rgb, style, thickness, radius, throughWalls,
                    colors, Map.copyOf(tagColors));
        }

        /**
         * 给某个标签指定颜色，标签里的方块都会用这个颜色。
         *
         * @param tag   方块标签
         * @param color 颜色（只用低 24 位）
         * @return 新的扫描配置
         */
        public Scan withTagColor(TagKey<Block> tag, int color) {
            Map<TagKey<Block>, Integer> merged = new LinkedHashMap<>(tagColors);
            merged.put(tag, color);
            return withTagColors(merged);
        }
    }

    /** 客户端当前的扫描配置 */
    private static volatile Scan clientScan;

    /** 客户端当前的扫描目标 */
    private static volatile Target clientTarget;

    private BlockScanOutline() {
    }

    /**
     * 构造一个扫描目标。
     *
     * @param blocks 方块
     * @param tags   方块标签
     * @return 扫描目标
     */
    public static Target target(List<Block> blocks, List<TagKey<Block>> tags) {
        return new Target(List.copyOf(blocks), List.copyOf(tags));
    }

    /**
     * 用方块构造扫描目标。
     *
     * @param blocks 方块
     * @return 扫描目标
     */
    @SafeVarargs
    public static Target target(Block... blocks) {
        return new Target(List.of(blocks), List.of());
    }

    /**
     * 用标签构造扫描目标。
     *
     * @param tags 方块标签
     * @return 扫描目标
     */
    @SafeVarargs
    public static Target target(TagKey<Block>... tags) {
        return new Target(List.of(), List.of(tags));
    }

    /**
     * 开始扫描高亮（发光模式、默认颜色）。
     *
     * @param level  所在维度
     * @param target 扫描目标
     * @param radius 扫描半径
     */
    public static void start(Level level, Target target, int radius) {
        start(level, target, BlockOutline.Mode.WIRE, BlockOutline.defaultColor(BlockOutline.Mode.WIRE), radius);
    }

    /**
     * 一条配色规则：要么按标签，要么按方块。
     * <p>
     * 用 {@link #of(TagKey, int)} 或 {@link #of(Block, int)} 构造，方便在调用处直接列出来。
     *
     * @param tag   方块标签，按方块配色时为 null
     * @param block 方块，按标签配色时为 null
     * @param rgb   颜色（只用低 24 位）
     */
    public record ColorRule(@Nullable TagKey<Block> tag, @Nullable Block block, int rgb) {

        /**
         * 按标签配色。
         *
         * @param tag 方块标签
         * @param rgb 颜色
         * @return 规则
         */
        public static ColorRule of(TagKey<Block> tag, int rgb) {
            return new ColorRule(Objects.requireNonNull(tag), null, rgb);
        }

        /**
         * 按方块配色。
         *
         * @param block 方块
         * @param rgb   颜色
         * @return 规则
         */
        public static ColorRule of(Block block, int rgb) {
            return new ColorRule(null, Objects.requireNonNull(block), rgb);
        }
    }

    /**
     * 开始拼一份扫描配置，调用处可以直接把颜色一条条列出来。
     * <p>
     * 例：{@code BlockScanOutline.scan(Mode.WIRE, 0xFF7CF9FF).color(BlockTags.IRON_ORES, 0xFFD8AF93)…build()}
     *
     * @param mode 高亮模式
     * @param rgb  统一颜色
     * @return 配置构造器
     */
    public static Builder scan(BlockOutline.Mode mode, int rgb) {
        return new Builder(mode, rgb);
    }

    /**
     * 开始拼一份扫描配置（指定颜色风格）。
     *
     * @param mode  高亮模式
     * @param rgb   统一颜色
     * @param style 颜色风格
     * @return 配置构造器
     */
    public static Builder scan(BlockOutline.Mode mode, int rgb, BlockOutline.Style style) {
        return new Builder(mode, rgb).style(style);
    }

    /**
     * 扫描配置的构造器，把一串参数和一条条配色写下来，最后 {@link #build()}。
     */
    public static final class Builder {

        private final BlockOutline.Mode mode;
        private final int rgb;
        private BlockOutline.Style style = BlockOutline.DEFAULT_STYLE;
        private double thickness;
        private int radius = DEFAULT_RADIUS;
        private boolean throughWalls = true;
        private final Map<Block, Integer> colors = new LinkedHashMap<>();
        private final Map<TagKey<Block>, Integer> tagColors = new LinkedHashMap<>();

        private Builder(BlockOutline.Mode mode, int rgb) {
            this.mode = Objects.requireNonNull(mode);
            this.rgb = rgb;
            this.thickness = BlockOutline.defaultThickness(mode);
        }

        /**
         * 设置颜色风格。
         *
         * @param style 纯色或彩色
         * @return 自己
         */
        public Builder style(BlockOutline.Style style) {
            if (style != null) {
                this.style = style;
            }
            return this;
        }

        /**
         * 设置粗细。
         *
         * @param thickness 粗细（格）
         * @return 自己
         */
        public Builder thickness(double thickness) {
            this.thickness = thickness;
            return this;
        }

        /**
         * 设置扫描半径。
         *
         * @param radius 半径（格）
         * @return 自己
         */
        public Builder radius(int radius) {
            this.radius = radius;
            return this;
        }

        /**
         * 是否穿透方块。
         *
         * @param throughWalls 是否穿透
         * @return 自己
         */
        public Builder throughWalls(boolean throughWalls) {
            this.throughWalls = throughWalls;
            return this;
        }

        /**
         * 给一个标签指定颜色，标签里的方块都用这个颜色。
         *
         * @param tag 方块标签
         * @param rgb 颜色
         * @return 自己
         */
        public Builder color(TagKey<Block> tag, int rgb) {
            tagColors.put(Objects.requireNonNull(tag), rgb);
            return this;
        }

        /**
         * 给一个方块指定颜色，优先级高于标签。
         *
         * @param block 方块
         * @param rgb   颜色
         * @return 自己
         */
        public Builder color(Block block, int rgb) {
            colors.put(Objects.requireNonNull(block), rgb);
            return this;
        }

        /**
         * 一次加一串规则，规则可以是标签也可以是方块。
         *
         * @param rules 配色规则列表
         * @return 自己
         */
        public Builder colors(List<ColorRule> rules) {
            for (ColorRule rule : rules) {
                if (rule == null) {
                    continue;
                }
                if (rule.tag() != null) {
                    tagColors.put(rule.tag(), rule.rgb());
                } else if (rule.block() != null) {
                    colors.put(rule.block(), rule.rgb());
                }
            }
            return this;
        }

        /**
         * 加上内置的矿石标签配色（{@link #oreTagColors()}），已有的条目保留不变。
         *
         * @return 自己
         */
        public Builder oreDefaults() {
            for (Map.Entry<TagKey<Block>, Integer> entry : oreTagColors().entrySet()) {
                tagColors.putIfAbsent(entry.getKey(), entry.getValue());
            }
            return this;
        }

        /**
         * 生成扫描配置。
         *
         * @return 扫描配置
         */
        public Scan build() {
            return new Scan(mode, rgb, style, thickness, radius, throughWalls,
                    Map.copyOf(colors), Map.copyOf(tagColors)).clamped();
        }
    }

    /**
     * 开始扫描高亮。
     *
     * @param level  所在维度
     * @param target 扫描目标
     * @param mode   高亮模式
     * @param rgb    颜色
     * @param radius 扫描半径
     */
    public static void start(Level level, Target target, BlockOutline.Mode mode, int rgb, int radius) {
        start(level, target, new Scan(mode, rgb, BlockOutline.DEFAULT_STYLE, BlockOutline.defaultThickness(mode),
                radius, true, Map.of(), Map.of()).clamped());
    }

    /**
     * 取某个方块在这次扫描里该用的颜色。
     *
     * @param scan  扫描配置
     * @param state 方块状态
     * @return ARGB 颜色
     */
    public static int colorOf(Scan scan, BlockState state) {
        Integer custom = scan.colors().get(state.getBlock());
        if (custom != null) {
            return custom;
        }
        for (Map.Entry<TagKey<Block>, Integer> entry : scan.tagColors().entrySet()) {
            if (state.is(entry.getKey())) {
                return entry.getValue();
            }
        }
        return scan.rgb();
    }

    /**
     * 一套常见矿石的标签配色
     *
     * @return 标签 → 颜色
     */
    public static Map<TagKey<Block>, Integer> oreTagColors() {
        Map<TagKey<Block>, Integer> colors = new LinkedHashMap<>();
        colors.put(Tags.Blocks.ORES_COAL, 0x2E2E2E);
        colors.put(Tags.Blocks.ORES_IRON, 0xD8AF93);
        colors.put(Tags.Blocks.ORES_COPPER, 0xE77C56);
        colors.put(Tags.Blocks.ORES_GOLD, 0xFCEE4B);
        colors.put(Tags.Blocks.ORES_REDSTONE, 0xFF2D2D);
        colors.put(Tags.Blocks.ORES_LAPIS, 0x4A80FF);
        colors.put(Tags.Blocks.ORES_DIAMOND, 0x4AEDD9);
        colors.put(Tags.Blocks.ORES_EMERALD, 0x17DD62);
        colors.put(Tags.Blocks.ORES_QUARTZ, 0xF0E6DC);
        colors.put(Tags.Blocks.ORES_NETHERITE_SCRAP, 0x6B4A35);
        return Map.copyOf(colors);
    }

    /**
     * 开始扫描高亮。
     *
     * @param level  所在维度
     * @param target 扫描目标
     * @param scan   扫描配置
     */
    public static void start(Level level, Target target, Scan scan) {
        if (level == null) {
            throw new IllegalArgumentException(PortalTexts.text("outline_null_level"));
        }
        if (target == null) {
            throw new IllegalArgumentException(PortalTexts.text("outline_null_target"));
        }
        if (scan == null || scan.mode() == null) {
            throw new IllegalArgumentException(PortalTexts.text("outline_null_mode"));
        }
        Scan safeScan = scan.clamped();
        if (level.isClientSide) {
            clientScan = safeScan;
            clientTarget = target;
            return;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockScanSavedData.get(serverLevel).set(target, safeScan);
        OutlineNetwork.broadcastScan(serverLevel, target, safeScan);
    }

    /**
     * 停止某个维度的扫描高亮。
     *
     * @param level 所在维度
     */
    public static void stop(Level level) {
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            clientScan = null;
            clientTarget = null;
            return;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockScanSavedData.get(serverLevel).clear();
        OutlineNetwork.broadcastScan(serverLevel, null, null);
    }

    /**
     * 取某个维度的扫描配置。
     *
     * @param level 所在维度
     * @return 扫描配置，没有时返回 null
     */
    @Nullable
    public static Scan get(Level level) {
        if (level == null) {
            return null;
        }
        if (level.isClientSide) {
            return clientScan;
        }
        return level instanceof ServerLevel serverLevel ? BlockScanSavedData.get(serverLevel).scan() : null;
    }

    /**
     * 取客户端当前的扫描目标。
     *
     * @return 扫描目标，没有时返回 null
     */
    @Nullable
    public static Target clientTarget() {
        return clientTarget;
    }

    /**
     * 直接写入客户端配置。
     *
     * @param target 扫描目标，传 null 表示停止
     * @param scan   扫描配置，传 null 表示停止
     */
    public static void putLocal(@Nullable Target target, @Nullable Scan scan) {
        clientTarget = target;
        clientScan = scan == null ? null : scan.clamped();
    }

    /**
     * 把客户端配置里的方块与标签导出成资源路径。
     *
     * @param target 扫描目标
     * @return 第一项是方块 id，第二项是标签 id
     */
    public static List<List<ResourceLocation>> export(Target target) {
        List<ResourceLocation> blocks = new ArrayList<>();
        for (Block block : target.blocks()) {
            blocks.add(BuiltInRegistries.BLOCK.getKey(block));
        }
        List<ResourceLocation> tags = new ArrayList<>();
        for (TagKey<Block> tag : target.tags()) {
            tags.add(tag.location());
        }
        return List.of(List.copyOf(blocks), List.copyOf(tags));
    }

    /**
     * 把资源路径还原成扫描目标（同步用）。
     *
     * @param blocks 方块 id
     * @param tags   标签 id
     * @return 扫描目标
     */
    public static Target importTarget(List<ResourceLocation> blocks, List<ResourceLocation> tags) {
        List<Block> blockList = new ArrayList<>();
        for (ResourceLocation id : blocks) {
            Block block = BuiltInRegistries.BLOCK.get(id);
            blockList.add(block);
        }
        List<TagKey<Block>> tagList = new ArrayList<>();
        for (ResourceLocation id : tags) {
            tagList.add(TagKey.create(Registries.BLOCK, id));
        }
        return target(blockList, tagList);
    }
}
