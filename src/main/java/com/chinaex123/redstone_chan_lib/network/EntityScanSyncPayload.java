package com.chinaex123.redstone_chan_lib.network;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import com.chinaex123.redstone_chan_lib.api.outline.BlockOutline;
import com.chinaex123.redstone_chan_lib.api.outline.EntityScanOutline;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 实体扫描高亮同步包。
 * <p>
 * 把某个维度当前的实体扫描配置（颜色、风格、线宽、半径、是否穿透）
 * 以及扫描目标（实体类型列表与标签列表）一起发给客户端。
 * 传 {@code active = false} 表示停止扫描。
 *
 * @param active       是否启用扫描
 * @param rgb          线框颜色
 * @param style        颜色风格序号（0 纯色、1 彩色）
 * @param thickness    线宽
 * @param radius       扫描半径
 * @param flags        标志位：第 0 位表示是否穿透方块
 * @param types        实体类型 id 列表
 * @param tags         实体类型标签 id 列表
 */
public record EntityScanSyncPayload(boolean active, int rgb, int style, double thickness, int radius, int flags,
                                    List<ResourceLocation> types, List<ResourceLocation> tags)
        implements CustomPacketPayload {

    /** 数据包类型 */
    public static final CustomPacketPayload.Type<EntityScanSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(RedstoneChanLIB.id("entity_scan_sync"));

    /** 穿透方块标志位 */
    private static final int THROUGH_WALLS_BIT = 0x1;

    /** 数据包编解码器。 */
    public static final StreamCodec<RegistryFriendlyByteBuf, EntityScanSyncPayload> STREAM_CODEC =
            StreamCodec.of(EntityScanSyncPayload::write, EntityScanSyncPayload::read);

    /**
     * 构造停止扫描的数据包。
     *
     * @return 数据包
     */
    public static EntityScanSyncPayload cleared() {
        return new EntityScanSyncPayload(false, 0, 0, 0.0D, 0, 0, List.of(), List.of());
    }

    /**
     * 构造同步数据包。
     *
     * @param target 扫描目标，null 表示停止
     * @param scan   扫描配置，null 表示停止
     * @return 数据包
     */
    public static EntityScanSyncPayload of(@Nullable EntityScanOutline.Target target,
                                           @Nullable EntityScanOutline.Scan scan) {
        if (target == null || scan == null) {
            return cleared();
        }
        List<List<ResourceLocation>> exported = EntityScanOutline.export(target);
        int flags = scan.throughWalls() ? THROUGH_WALLS_BIT : 0;
        return new EntityScanSyncPayload(true, scan.rgb(),
                scan.style() == BlockOutline.Style.RAINBOW ? 1 : 0, scan.thickness(), scan.radius(), flags,
                exported.get(0), exported.get(1));
    }

    /**
     * 写入缓冲区。
     *
     * @param buf     缓冲区
     * @param payload 数据包
     */
    private static void write(RegistryFriendlyByteBuf buf, EntityScanSyncPayload payload) {
        buf.writeBoolean(payload.active);
        buf.writeVarInt(payload.rgb);
        buf.writeVarInt(payload.style);
        buf.writeDouble(payload.thickness);
        buf.writeVarInt(payload.radius);
        buf.writeVarInt(payload.flags);
        buf.writeVarInt(payload.types.size());
        for (ResourceLocation id : payload.types) {
            buf.writeResourceLocation(id);
        }
        buf.writeVarInt(payload.tags.size());
        for (ResourceLocation id : payload.tags) {
            buf.writeResourceLocation(id);
        }
    }

    /**
     * 从缓冲区读取。
     *
     * @param buf 缓冲区
     * @return 数据包
     */
    private static EntityScanSyncPayload read(RegistryFriendlyByteBuf buf) {
        boolean active = buf.readBoolean();
        int rgb = buf.readVarInt();
        int style = buf.readVarInt();
        double thickness = buf.readDouble();
        int radius = buf.readVarInt();
        int flags = buf.readVarInt();
        int typeCount = buf.readVarInt();
        List<ResourceLocation> types = new ArrayList<>(typeCount);
        for (int i = 0; i < typeCount; i++) {
            types.add(buf.readResourceLocation());
        }
        int tagCount = buf.readVarInt();
        List<ResourceLocation> tags = new ArrayList<>(tagCount);
        for (int i = 0; i < tagCount; i++) {
            tags.add(buf.readResourceLocation());
        }
        return new EntityScanSyncPayload(active, rgb, style, thickness, radius, flags, types, tags);
    }

    /**
     * 还原成扫描配置。
     *
     * @return 扫描配置，停止扫描时返回 null
     */
    @Nullable
    public EntityScanOutline.Scan toScan() {
        if (!active) {
            return null;
        }
        return new EntityScanOutline.Scan(
                rgb,
                style == 1 ? BlockOutline.Style.RAINBOW : BlockOutline.Style.SOLID,
                thickness,
                radius,
                (flags & THROUGH_WALLS_BIT) != 0);
    }

    /**
     * 还原成扫描目标。
     *
     * @return 扫描目标，停止扫描时返回 null
     */
    @Nullable
    public EntityScanOutline.Target toTarget() {
        if (!active) {
            return null;
        }
        return EntityScanOutline.importTarget(types, tags);
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
