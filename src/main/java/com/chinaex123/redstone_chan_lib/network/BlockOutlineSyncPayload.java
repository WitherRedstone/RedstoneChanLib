package com.chinaex123.redstone_chan_lib.network;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import com.chinaex123.redstone_chan_lib.api.outline.BlockOutline;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * 方块描边同步数据包。
 * <p>
 * 把一个方块位置的描边配置下发到客户端：位置、模式、颜色、粗细与是否穿透方块。
 * 模式为 -1 表示清除这个位置的描边。
 */
public record BlockOutlineSyncPayload(BlockPos pos, int mode, int rgb, double thickness, int flags,
                                      double holdSeconds) implements CustomPacketPayload {

    /** 数据包类型。 */
    public static final CustomPacketPayload.Type<BlockOutlineSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(RedstoneChanLIB.id("block_outline_sync"));

    /** 「是否穿透方块」占用 flags 的最低一位 */
    private static final int THROUGH_WALLS_BIT = 0x1;
    /** 「是否彩色」占用 flags 的第二位 */
    private static final int RAINBOW_BIT = 0x2;
    /** 「是否一直显示」占用 flags 的第三位 */
    private static final int KEEP_VISIBLE_BIT = 0x4;

    /** 流编解码器。 */
    public static final StreamCodec<ByteBuf, BlockOutlineSyncPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, BlockOutlineSyncPayload::pos,
            ByteBufCodecs.VAR_INT, BlockOutlineSyncPayload::mode,
            ByteBufCodecs.VAR_INT, BlockOutlineSyncPayload::rgb,
            ByteBufCodecs.DOUBLE, BlockOutlineSyncPayload::thickness,
            ByteBufCodecs.VAR_INT, BlockOutlineSyncPayload::flags,
            ByteBufCodecs.DOUBLE, BlockOutlineSyncPayload::holdSeconds,
            BlockOutlineSyncPayload::new);

    /**
     * 由描边配置创建同步数据包。
     *
     * @param pos     方块位置
     * @param outline 描边配置，null 表示清除
     * @return 同步数据包
     */
    public static BlockOutlineSyncPayload of(BlockPos pos, BlockOutline.Outline outline) {
        if (outline == null) {
            return new BlockOutlineSyncPayload(pos, -1, 0, 0.0D, 0, 0.0D);
        }
        int flags = 0;
        if (outline.throughWalls()) {
            flags |= THROUGH_WALLS_BIT;
        }
        if (outline.style() == BlockOutline.Style.RAINBOW) {
            flags |= RAINBOW_BIT;
        }
        if (outline.keepVisible()) {
            flags |= KEEP_VISIBLE_BIT;
        }
        return new BlockOutlineSyncPayload(pos, outline.mode().ordinal(), outline.rgb(), outline.thickness(), flags,
                outline.holdSeconds());
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
     *
     * @return 描边配置，清除时为 null
     */
    public BlockOutline.Outline toOutline() {
        if (isClear()) {
            return null;
        }
        BlockOutline.Mode[] modes = BlockOutline.Mode.values();
        BlockOutline.Mode mode = modes[Math.floorMod(this.mode, modes.length)];
        boolean throughWalls = (this.flags & THROUGH_WALLS_BIT) != 0;
        BlockOutline.Style style = (this.flags & RAINBOW_BIT) != 0 ? BlockOutline.Style.RAINBOW : BlockOutline.Style.SOLID;
        boolean keepVisible = (this.flags & KEEP_VISIBLE_BIT) != 0;
        return new BlockOutline.Outline(mode, this.rgb, style, this.thickness, throughWalls, keepVisible,
                this.holdSeconds);
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
