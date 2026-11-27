package com.chinaex123.redstone_chan_lib.client.outline;

import com.chinaex123.redstone_chan_lib.api.outline.EntityOutline;
import com.chinaex123.redstone_chan_lib.network.OutlineSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 客户端描边应用器。
 * <p>
 * 接收服务端下发的描边同步数据并应用到本地实体：
 * 若实体尚未加载，则暂存待处理，在客户端每刻尝试重新查找并应用；
 * 提供待处理列表的清理方法。
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class ClientOutlineApplier {

    /** 尚未找到对应实体、等待应用的描边数据，键为实体 UUID。 */
    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private ClientOutlineApplier() {
    }

    /**
     * 接收描边同步数据。
     * <p>
     * 若实体已加载则立即应用并移除待处理记录；
     * 描边为空时移除待处理记录，否则暂存等待后续应用。
     *
     * @param payload 描边同步数据
     */
    public static void accept(OutlineSyncPayload payload) {
        EntityOutline.Outline outline = payload.toOutline();
        Entity entity = findEntity(payload.entityId(), payload.uuid());
        if (entity != null) {
            EntityOutline.putLocal(entity, outline);
            PENDING.remove(payload.uuid());
            return;
        }
        if (outline == null) {
            PENDING.remove(payload.uuid());
        } else {
            PENDING.put(payload.uuid(), new Pending(payload.entityId(), outline));
        }
    }

    /**
     * 客户端每刻更新。
     * <p>
     * 遍历待处理记录，找到对应实体后应用描边并移除记录。
     *
     * @param event 客户端每刻更新事件
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (PENDING.isEmpty()) {
            return;
        }
        PENDING.entrySet().removeIf(entry -> {
            Entity entity = findEntity(entry.getValue().entityId(), entry.getKey());
            if (entity == null) {
                return false;
            }
            EntityOutline.putLocal(entity, entry.getValue().outline());
            return true;
        });
    }

    /**
     * 按实体 ID 与 UUID 查找客户端实体。
     * <p>
     * 客户端世界不可用或实体 ID 与 UUID 不匹配时返回 null。
     *
     * @param entityId 实体 ID
     * @param uuid     实体 UUID
     * @return 实体，未找到时返回 null
     */
    @Nullable
    private static Entity findEntity(int entityId, UUID uuid) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        Entity entity = level.getEntity(entityId);
        if (entity == null || !uuid.equals(entity.getUUID())) {
            return null;
        }
        return entity;
    }

    /**
     * 清空待处理描边数据。
     */
    public static void reset() {
        PENDING.clear();
    }

    /**
     * 待处理的描边数据。
     *
     * @param entityId 实体 ID
     * @param outline  描边配置
     */
    private record Pending(int entityId, EntityOutline.Outline outline) {
    }
}
