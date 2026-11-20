package com.chinaex123.redstone_chan_lib.world;

import com.chinaex123.redstone_chan_lib.api.outline.EntityOutline;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * 把描边设置写进存档。
 * <p>
 * 键是生物的 UUID，所以玩家退出重进、区块卸载又重新加载之后，只要生物本身还在，描边就能被重新应用。
 * 数据挂在主世界的存档数据里（和地图数据同一套机制），换维度不影响。
 */
public class OutlineSavedData extends SavedData {

    /** 存档里的数据名，同一个名字会读写同一个文件片段 */
    private static final String DATA_NAME = "redstone_chan_lib_outlines";

    private static final String TAG_OUTLINES = "outlines";
    private static final String TAG_ID = "id";
    private static final String TAG_MODE = "mode";
    private static final String TAG_RGB = "rgb";
    private static final String TAG_STYLE = "style";
    private static final String TAG_THICKNESS = "thickness";
    private static final String TAG_THROUGH_WALLS = "through_walls";

    /** 实体 UUID 到描边配置的映射。 */
    private final Map<UUID, EntityOutline.Outline> outlines = new HashMap<>();

    /**
     * 获取主世界的描边存档数据。
     * <p>
     * 不存在时创建新的数据实例并注册到主世界数据存储。
     *
     * @param server 服务器
     * @return 描边存档数据
     */
    public static OutlineSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(OutlineSavedData::new, OutlineSavedData::load), DATA_NAME);
    }

    /**
     * 从 NBT 加载描边数据。
     * <p>
     * 逐条读取描边记录；模式与风格序号按取模安全还原，
     * 缺少穿透字段时默认为穿透。
     *
     * @param tag        数据标签
     * @param registries 注册表提供者
     * @return 描边存档数据
     */
    public static OutlineSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        OutlineSavedData data = new OutlineSavedData();
        ListTag list = tag.getList(TAG_OUTLINES, Tag.TAG_COMPOUND);
        EntityOutline.Mode[] modes = EntityOutline.Mode.values();
        EntityOutline.Style[] styles = EntityOutline.Style.values();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.hasUUID(TAG_ID)) {
                continue;
            }
            EntityOutline.Mode mode = modes[Math.floorMod(entry.getInt(TAG_MODE), modes.length)];
            EntityOutline.Style style = styles[Math.floorMod(entry.getInt(TAG_STYLE), styles.length)];
            data.outlines.put(entry.getUUID(TAG_ID),
                    new EntityOutline.Outline(mode, entry.getInt(TAG_RGB), style, entry.getDouble(TAG_THICKNESS),
                            !entry.contains(TAG_THROUGH_WALLS) || entry.getBoolean(TAG_THROUGH_WALLS)));
        }
        return data;
    }

    /**
     * 保存描边数据到 NBT。
     * <p>
     * 将所有描边记录写入列表标签，包含 ID、模式、颜色、风格、粗细与穿透标记。
     *
     * @param tag        数据标签
     * @param registries 注册表提供者
     * @return 数据标签
     */
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        this.outlines.forEach((uuid, outline) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID(TAG_ID, uuid);
            entry.putInt(TAG_MODE, outline.mode().ordinal());
            entry.putInt(TAG_RGB, outline.rgb());
            entry.putInt(TAG_STYLE, outline.style().ordinal());
            entry.putDouble(TAG_THICKNESS, outline.thickness());
            entry.putBoolean(TAG_THROUGH_WALLS, outline.throughWalls());
            list.add(entry);
        });
        tag.put(TAG_OUTLINES, list);
        return tag;
    }

    /**
     * 写入或更新指定实体的描边，并标记数据已修改。
     *
     * @param entityId 实体 UUID
     * @param outline  描边配置
     */
    public void put(UUID entityId, EntityOutline.Outline outline) {
        this.outlines.put(entityId, outline);
        setDirty();
    }

    /**
     * 移除指定实体的描边，存在记录时标记数据已修改。
     *
     * @param entityId 实体 UUID
     */
    public void remove(UUID entityId) {
        if (this.outlines.remove(entityId) != null) {
            setDirty();
        }
    }

    /**
     * 获取指定实体的描边。
     *
     * @param entityId 实体 UUID
     * @return 描边配置，无记录时返回 null
     */
    @Nullable
    public EntityOutline.Outline get(UUID entityId) {
        return this.outlines.get(entityId);
    }

    /**
     * 清空全部描边记录，存在记录时标记数据已修改。
     */
    public void clearAll() {
        if (!this.outlines.isEmpty()) {
            this.outlines.clear();
            setDirty();
        }
    }
}