package com.chinaex123.redstone_chan_lib.api.outline;

import com.chinaex123.redstone_chan_lib.network.OutlineNetwork;
import com.chinaex123.redstone_chan_lib.util.PortalTexts;
import com.chinaex123.redstone_chan_lib.world.EntityScanSavedData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 周围实体扫描高亮 API。
 * <p>
 * 和 {@link BlockScanOutline} 是一对：那个扫方块，这个扫实体。
 * 扫到的每个实体会被一圈细线框住，用的是和方块线框同一套画法
 * （十二棱 {@code DEBUG_LINES}，一个像素宽），可以穿透方块看。
 * <p>
 * 目标既可以是实体类型（{@link EntityType}），也可以是实体类型标签（{@link TagKey}），两者可以混用。
 * 扫描以玩家为中心，半径可以自己定。
 * <p>
 * 示例：
 * <pre>{@code
 * EntityScanOutline.Target zombies = EntityScanOutline.target(EntityType.ZOMBIE, EntityType.HUSK);
 * EntityScanOutline.start(level, zombies, 24);
 * EntityScanOutline.stop(level);
 * }</pre>
 */
public final class EntityScanOutline {

    /** 默认扫描半径（格） */
    public static final int DEFAULT_RADIUS = 16;
    /** 扫描半径上限，太大既卡又没意义 */
    public static final int MAX_RADIUS = 64;

    /**
     * 扫描目标：一组实体类型与一组实体类型标签，命中任意一个就算目标。
     */
    public record Target(List<EntityType<?>> types, List<TagKey<EntityType<?>>> tags) {

        /**
         * 判断某个实体是否命中目标。
         *
         * @param entity 实体
         * @return 命中返回 true
         */
        public boolean matches(Entity entity) {
            if (entity == null || !entity.isAlive()) {
                return false;
            }
            for (EntityType<?> type : types) {
                if (entity.getType() == type) {
                    return true;
                }
            }
            for (TagKey<EntityType<?>> tag : tags) {
                if (entity.getType().is(tag)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * 目标是否为空（空目标不会高亮任何实体）。
         *
         * @return 空返回 true
         */
        public boolean isEmpty() {
            return types.isEmpty() && tags.isEmpty();
        }
    }

    /**
     * 扫描高亮的配置。
     *
     * @param rgb          线框颜色，只取低 24 位
     * @param style        颜色风格（纯色 / 彩色）
     * @param thickness    线宽，单位是方块（一格约等于 100 像素，会夹在 1~4 像素）
     * @param radius       扫描半径（格）
     * @param throughWalls 是否穿透方块
     */
    public record Scan(int rgb, BlockOutline.Style style, double thickness, int radius, boolean throughWalls) {

        /**
         * 把半径与线宽夹到合法范围。
         *
         * @return 夹过的配置
         */
        public Scan clamped() {
            int safeRadius = Math.clamp(radius, 1, MAX_RADIUS);
            double safeThickness = Math.max(0.001D, thickness);
            return new Scan(rgb, style, safeThickness, safeRadius, throughWalls);
        }
    }

    /** 客户端当前的扫描配置 */
    private static volatile Scan clientScan;

    /** 客户端当前的扫描目标 */
    private static volatile Target clientTarget;

    private EntityScanOutline() {
    }

    /**
     * 构造一个扫描目标。
     *
     * @param types 实体类型
     * @param tags  实体类型标签
     * @return 扫描目标
     */
    public static Target target(List<EntityType<?>> types, List<TagKey<EntityType<?>>> tags) {
        return new Target(List.copyOf(types), List.copyOf(tags));
    }

    /**
     * 用实体类型构造扫描目标。
     *
     * @param types 实体类型
     * @return 扫描目标
     */
    @SafeVarargs
    public static Target target(EntityType<?>... types) {
        return new Target(List.of(types), List.of());
    }

    /**
     * 用标签构造扫描目标。
     *
     * @param tags 实体类型标签
     * @return 扫描目标
     */
    @SafeVarargs
    public static Target target(TagKey<EntityType<?>>... tags) {
        return new Target(List.of(), List.of(tags));
    }

    /**
     * 开始扫描高亮。
     *
     * @param level  所在维度
     * @param target 扫描目标
     * @param radius 扫描半径
     */
    public static void start(Level level, Target target, int radius) {
        start(level, target, new Scan(0xFFFFFFFF, BlockOutline.DEFAULT_STYLE, BlockOutline.WIRE_THICKNESS,
                radius, true).clamped());
    }

    /**
     * 开始扫描高亮。
     *
     * @param level  所在维度
     * @param target 扫描目标
     * @param rgb    线框颜色
     * @param radius 扫描半径
     */
    public static void start(Level level, Target target, int rgb, int radius) {
        start(level, target, new Scan(rgb, BlockOutline.DEFAULT_STYLE, BlockOutline.WIRE_THICKNESS,
                radius, true).clamped());
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
        if (scan == null) {
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
        EntityScanSavedData.get(serverLevel).set(target, safeScan);
        OutlineNetwork.broadcastEntityScan(serverLevel, target, safeScan);
    }

    /**
     * 停止某个维度的实体扫描高亮。
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
        EntityScanSavedData.get(serverLevel).clear();
        OutlineNetwork.broadcastEntityScan(serverLevel, null, null);
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
        return level instanceof ServerLevel serverLevel ? EntityScanSavedData.get(serverLevel).scan() : null;
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
     * 把客户端配置里的实体类型与标签导出成资源路径。
     *
     * @param target 扫描目标
     * @return 第一项是实体类型 id，第二项是标签 id
     */
    public static List<List<ResourceLocation>> export(Target target) {
        List<ResourceLocation> types = new ArrayList<>();
        for (EntityType<?> type : target.types()) {
            types.add(BuiltInRegistries.ENTITY_TYPE.getKey(type));
        }
        List<ResourceLocation> tags = new ArrayList<>();
        for (TagKey<EntityType<?>> tag : target.tags()) {
            tags.add(tag.location());
        }
        return List.of(List.copyOf(types), List.copyOf(tags));
    }

    /**
     * 把资源路径还原成扫描目标。
     *
     * @param types 实体类型 id
     * @param tags  标签 id
     * @return 扫描目标
     */
    public static Target importTarget(List<ResourceLocation> types, List<ResourceLocation> tags) {
        List<EntityType<?>> typeList = new ArrayList<>();
        for (ResourceLocation id : types) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
            typeList.add(type);
        }
        List<TagKey<EntityType<?>>> tagList = new ArrayList<>();
        for (ResourceLocation id : tags) {
            tagList.add(TagKey.create(Registries.ENTITY_TYPE, id));
        }
        return target(typeList, tagList);
    }
}
