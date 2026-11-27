package com.chinaex123.redstone_chan_lib.network;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * 扫描高亮开关请求数据包。
 * <p>
 * 模组自带的测试键（J、L）用它请求服务端开关扫描。开关必须由服务端执行，
 * 原因和方块描边一样：扫描配置写在服务端存档里，客户端只负责拿着同步下来的配置去扫，
 * 这样退出重进之后扫描还在。
 */
public record ScanTogglePayload(int kind) implements CustomPacketPayload {

    /** 开关周围的矿石扫描。 */
    public static final int KIND_BLOCK = 0;

    /** 开关“准星所指实体的同类”扫描。 */
    public static final int KIND_ENTITY = 1;

    /** 数据包类型。 */
    public static final CustomPacketPayload.Type<ScanTogglePayload> TYPE =
            new CustomPacketPayload.Type<>(RedstoneChanLIB.id("scan_toggle"));

    /** 流编解码器：只有一个整型字段，手写读写。 */
    public static final StreamCodec<FriendlyByteBuf, ScanTogglePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> buffer.writeVarInt(payload.kind()),
                    buffer -> new ScanTogglePayload(buffer.readVarInt()));

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
