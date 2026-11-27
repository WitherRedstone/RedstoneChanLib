package com.chinaex123.redstone_chan_lib.network;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import com.chinaex123.redstone_chan_lib.api.outline.BlockOutline;
import com.chinaex123.redstone_chan_lib.api.outline.BlockScanOutline;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 扫描高亮同步包。
 * <p>
 * 把某个维度当前的扫描配置（模式、颜色、粗细、半径、是否穿透、逐方块与逐标签配色）
 * 以及扫描目标（方块列表与标签列表）一起发给客户端。
 * 传 {@code active = false} 表示停止扫描。
 *
 * @param active        是否启用扫描
 * @param mode          高亮模式序号
 * @param rgb           统一颜色
 * @param style         颜色风格序号（0 纯色、1 彩色）
 * @param thickness     粗细
 * @param radius        扫描半径
 * @param flags         标志位：第 0 位表示是否穿透方块
 * @param blocks        方块 id 列表
 * @param tags          方块标签 id 列表
 * @param colors        单独配色的方块 id 列表（与 {@code colorValues} 一一对应）
 * @param colorValues   单独配色的颜色列表
 * @param colorTags     按标签配色的标签 id 列表（与 {@code colorTagValues} 一一对应）
 * @param colorTagValues 按标签配色的颜色列表
 */
public record BlockScanSyncPayload(boolean active, int mode, int rgb, int style, double thickness, int radius,
                                   int flags, List<ResourceLocation> blocks, List<ResourceLocation> tags,
                                   List<ResourceLocation> colors, List<Integer> colorValues,
                                   List<ResourceLocation> colorTags, List<Integer> colorTagValues)
        implements CustomPacketPayload {

    /** 数据包类型 */
    public static final CustomPacketPayload.Type<BlockScanSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(RedstoneChanLIB.id("block_scan_sync"));

    /** 穿透方块标志位 */
    private static final int THROUGH_WALLS_BIT = 0x1;

    /** 流编解码器。 */
    public static final StreamCodec<RegistryFriendlyByteBuf, BlockScanSyncPayload> STREAM_CODEC =
            StreamCodec.of(BlockScanSyncPayload::write, BlockScanSyncPayload::read);

    /**
     * 构造停止扫描的数据包。
     *
     * @return 数据包
     */
    public static BlockScanSyncPayload cleared() {
        return new BlockScanSyncPayload(false, 0, 0, 0, 0.0D, 0, 0,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    /**
     * 构造同步数据包。
     *
     * @param target 扫描目标，null 表示停止
     * @param scan   扫描配置，null 表示停止
     * @return 数据包
     */
    public static BlockScanSyncPayload of(@Nullable BlockScanOutline.Target target,
                                          @Nullable BlockScanOutline.Scan scan) {
        if (target == null || scan == null) {
            return cleared();
        }
        List<List<ResourceLocation>> exported = BlockScanOutline.export(target);
        int flags = 0;
        if (scan.throughWalls()) {
            flags |= THROUGH_WALLS_BIT;
        }
        List<ResourceLocation> colorIds = new ArrayList<>();
        List<Integer> colorValues = new ArrayList<>();
        for (Map.Entry<Block, Integer> entry : scan.colors().entrySet()) {
            colorIds.add(BuiltInRegistries.BLOCK.getKey(entry.getKey()));
            colorValues.add(entry.getValue());
        }
        List<ResourceLocation> colorTagIds = new ArrayList<>();
        List<Integer> colorTagValues = new ArrayList<>();
        for (Map.Entry<TagKey<Block>, Integer> entry : scan.tagColors().entrySet()) {
            colorTagIds.add(entry.getKey().location());
            colorTagValues.add(entry.getValue());
        }

        return new BlockScanSyncPayload(true, scan.mode().ordinal(), scan.rgb(),
                scan.style() == BlockOutline.Style.RAINBOW ? 1 : 0, scan.thickness(), scan.radius(), flags,
                exported.get(0), exported.get(1), List.copyOf(colorIds), List.copyOf(colorValues),
                List.copyOf(colorTagIds), List.copyOf(colorTagValues));
    }

    /**
     * 写入缓冲区。
     *
     * @param buf     缓冲区
     * @param payload 数据包
     */
    private static void write(RegistryFriendlyByteBuf buf, BlockScanSyncPayload payload) {
        buf.writeBoolean(payload.active);
        buf.writeVarInt(payload.mode);
        buf.writeVarInt(payload.rgb);
        buf.writeVarInt(payload.style);
        buf.writeDouble(payload.thickness);
        buf.writeVarInt(payload.radius);
        buf.writeVarInt(payload.flags);
        buf.writeVarInt(payload.blocks.size());
        for (ResourceLocation id : payload.blocks) {
            buf.writeResourceLocation(id);
        }
        buf.writeVarInt(payload.tags.size());
        for (ResourceLocation id : payload.tags) {
            buf.writeResourceLocation(id);
        }
        buf.writeVarInt(payload.colors.size());
        for (int i = 0; i < payload.colors.size(); i++) {
            buf.writeResourceLocation(payload.colors.get(i));
            buf.writeVarInt(payload.colorValues.get(i));
        }
        buf.writeVarInt(payload.colorTags.size());
        for (int i = 0; i < payload.colorTags.size(); i++) {
            buf.writeResourceLocation(payload.colorTags.get(i));
            buf.writeVarInt(payload.colorTagValues.get(i));
        }
    }

    /**
     * 从缓冲区读取。
     *
     * @param buf 缓冲区
     * @return 数据包
     */
    private static BlockScanSyncPayload read(RegistryFriendlyByteBuf buf) {
        boolean active = buf.readBoolean();
        int mode = buf.readVarInt();
        int rgb = buf.readVarInt();
        int style = buf.readVarInt();
        double thickness = buf.readDouble();
        int radius = buf.readVarInt();
        int flags = buf.readVarInt();
        int blockCount = buf.readVarInt();
        List<ResourceLocation> blocks = new ArrayList<>(blockCount);
        for (int i = 0; i < blockCount; i++) {
            blocks.add(buf.readResourceLocation());
        }
        int tagCount = buf.readVarInt();
        List<ResourceLocation> tags = new ArrayList<>(tagCount);
        for (int i = 0; i < tagCount; i++) {
            tags.add(buf.readResourceLocation());
        }
        int colorCount = buf.readVarInt();
        List<ResourceLocation> colors = new ArrayList<>(colorCount);
        List<Integer> colorValues = new ArrayList<>(colorCount);
        for (int i = 0; i < colorCount; i++) {
            colors.add(buf.readResourceLocation());
            colorValues.add(buf.readVarInt());
        }
        int colorTagCount = buf.readVarInt();
        List<ResourceLocation> colorTags = new ArrayList<>(colorTagCount);
        List<Integer> colorTagValues = new ArrayList<>(colorTagCount);
        for (int i = 0; i < colorTagCount; i++) {
            colorTags.add(buf.readResourceLocation());
            colorTagValues.add(buf.readVarInt());
        }
        return new BlockScanSyncPayload(active, mode, rgb, style, thickness, radius, flags, blocks, tags,
                colors, colorValues, colorTags, colorTagValues);
    }

    /**
     * 还原成扫描配置。
     *
     * @return 扫描配置，停止扫描时返回 null
     */
    @Nullable
    public BlockScanOutline.Scan toScan() {
        if (!active) {
            return null;
        }
        BlockOutline.Mode[] modes = BlockOutline.Mode.values();
        Map<Block, Integer> customColors = new LinkedHashMap<>();
        for (int i = 0; i < colors.size() && i < colorValues.size(); i++) {
            Block block = BuiltInRegistries.BLOCK.get(colors.get(i));
            if (block != null) {
                customColors.put(block, colorValues.get(i));
            }
        }
        Map<TagKey<Block>, Integer> tagColors = new LinkedHashMap<>();
        for (int i = 0; i < colorTags.size() && i < colorTagValues.size(); i++) {
            tagColors.put(TagKey.create(Registries.BLOCK, colorTags.get(i)), colorTagValues.get(i));
        }
        return new BlockScanOutline.Scan(
                modes[Math.floorMod(mode, modes.length)],
                rgb,
                style == 1 ? BlockOutline.Style.RAINBOW : BlockOutline.Style.SOLID,
                thickness,
                radius,
                (flags & THROUGH_WALLS_BIT) != 0,
                customColors,
                tagColors);
    }

    /**
     * 还原成扫描目标。
     *
     * @return 扫描目标，停止扫描时返回 null
     */
    @Nullable
    public BlockScanOutline.Target toTarget() {
        if (!active) {
            return null;
        }
        return BlockScanOutline.importTarget(blocks, tags);
    }

    /**
     * 获取数据包类型。
     *
     * @return 数据包类型
     */
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
