package com.chinaex123.redstone_chan_lib.network;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * 描边重新同步请求数据包。
 * <p>
 * 客户端在进入世界、切换维度时本地缓存会被清空，此时发这个包请求服务端
 * 把当前维度里存档的方块描边与扫描配置重新下发一遍，避免“退出重进后描边消失”。
 * 该包不携带任何数据，使用单位流编解码器。
 */
public record OutlineResyncPayload() implements CustomPacketPayload {

    /** 单例实例。 */
    public static final OutlineResyncPayload INSTANCE = new OutlineResyncPayload();

    /** 数据包类型。 */
    public static final CustomPacketPayload.Type<OutlineResyncPayload> TYPE =
            new CustomPacketPayload.Type<>(RedstoneChanLIB.id("outline_resync"));

    /** 流编解码器：无数据负载，直接复用单例。 */
    public static final StreamCodec<FriendlyByteBuf, OutlineResyncPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

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
