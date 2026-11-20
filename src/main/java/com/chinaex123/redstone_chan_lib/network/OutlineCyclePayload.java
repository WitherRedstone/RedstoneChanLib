package com.chinaex123.redstone_chan_lib.network;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * 描边切换请求数据包。
 * <p>
 * 客户端请求将当前注视生物的描边切换为下一种，模组自带的测试入口使用它。
 * 真正的切换在服务端处理，因为描边以服务端数据为准写入存档，
 * 这样退出重进后描边不会丢失。该包不携带任何数据，使用单位流编解码器。
 */
public record OutlineCyclePayload() implements CustomPacketPayload {

    /** 单例实例。 */
    public static final OutlineCyclePayload INSTANCE = new OutlineCyclePayload();

    /** 数据包类型。 */
    public static final CustomPacketPayload.Type<OutlineCyclePayload> TYPE =
            new CustomPacketPayload.Type<>(RedstoneChanLIB.id("outline_cycle"));

    /** 流编解码器：无数据负载，直接复用单例。 */
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, OutlineCyclePayload> STREAM_CODEC =
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