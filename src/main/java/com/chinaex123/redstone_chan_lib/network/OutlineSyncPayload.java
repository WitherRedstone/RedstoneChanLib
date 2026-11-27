package com.chinaex123.redstone_chan_lib.network;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import com.chinaex123.redstone_chan_lib.api.outline.EntityOutline;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/**
 * 描边同步数据包。
 * <p>
 * 将实体的描边配置下发到客户端：包含实体 UUID 与 ID、描边模式、颜色、
 * 颜色风格与粗细。为压进流编解码器的字段数量，「是否穿透方块」被并入
 * 风格序号的最高位；模式为 -1 表示清除描边。
 */
public record OutlineSyncPayload(UUID uuid, int entityId, int mode, int rgb, int style, double thickness) implements CustomPacketPayload {

    /** 数据包类型。 */
    public static final CustomPacketPayload.Type<OutlineSyncPayload> TYPE = new CustomPacketPayload.Type<>(RedstoneChanLIB.id("outline_sync"));

    /** 颜色风格序号只占低 8 位，第 9 位拿来记“是否穿透方块” */
    private static final int THROUGH_WALLS_BIT = 0x100;

    /** 流编解码器。 */
    public static final StreamCodec<ByteBuf, OutlineSyncPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, OutlineSyncPayload::uuid,
            ByteBufCodecs.VAR_INT, OutlineSyncPayload::entityId,
            ByteBufCodecs.VAR_INT, OutlineSyncPayload::mode,
            ByteBufCodecs.VAR_INT, OutlineSyncPayload::rgb,
            ByteBufCodecs.VAR_INT, OutlineSyncPayload::style,
            ByteBufCodecs.DOUBLE, OutlineSyncPayload::thickness,
            OutlineSyncPayload::new);

    /**
     * 由描边配置创建同步数据包。
     * <p>
     * 描边为 null 时生成清除包；否则把「是否穿透方块」并入风格序号。
     *
     * @param uuid     实体 UUID
     * @param entityId 实体 ID
     * @param outline  描边配置，null 表示清除
     * @return 同步数据包
     */
    public static OutlineSyncPayload of(UUID uuid, int entityId, EntityOutline.Outline outline) {
        if (outline == null) {
            return new OutlineSyncPayload(uuid, entityId, -1, 0, 0, 0.0D);
        }
        int style = outline.style().ordinal() | (outline.throughWalls() ? THROUGH_WALLS_BIT : 0);
        return new OutlineSyncPayload(uuid, entityId, outline.mode().ordinal(), outline.rgb(),
                style, outline.thickness());
    }

    /**
     * 判断描边是否穿透方块。
     *
     * @return 穿透方块返回 true
     */
    public boolean throughWalls() {
        return (this.style & THROUGH_WALLS_BIT) != 0;
    }

    /**
     * 判断是否为清除描边。
     *
     * @return 清除描边返回 true
     */
    public boolean isClear() {
        return this.mode < 0;
    }

    /**
     * 还原描边配置。
     * <p>
     * 清除包返回 null；否则按取模安全地还原模式与颜色风格，
     * 并从风格位中提取穿透标记。
     *
     * @return 描边配置，清除时为 null
     */
    public EntityOutline.Outline toOutline() {
        if (isClear()) {
            return null;
        }
        EntityOutline.Mode[] modes = EntityOutline.Mode.values();
        EntityOutline.Style[] styles = EntityOutline.Style.values();
        EntityOutline.Mode mode = modes[Math.floorMod(this.mode, modes.length)];
        EntityOutline.Style style = styles[Math.floorMod(this.style & 0xFF, styles.length)];
        return new EntityOutline.Outline(mode, this.rgb, style, this.thickness, throughWalls());
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