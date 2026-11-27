package com.chinaex123.redstone_chan_lib.world;

import com.chinaex123.redstone_chan_lib.api.outline.BlockOutline;
import com.chinaex123.redstone_chan_lib.api.outline.EntityScanOutline;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 实体扫描高亮的存档数据。
 * <p>
 * 每个维度各存一份，记下这个维度当前生效的实体扫描配置，
 * 这样玩家退出重进、或者换维度回来时扫描还在。
 */
public class EntityScanSavedData extends SavedData {

    /** 存档名称 */
    private static final String DATA_NAME = "redstone_chan_lib_entity_scans";

    /** 当前扫描目标，没有时为 null */
    @Nullable
    private EntityScanOutline.Target target;

    /** 当前扫描配置，没有时为 null */
    @Nullable
    private EntityScanOutline.Scan scan;

    /**
     * 从 NBT 读取存档数据。
     *
     * @param tag        NBT 标签
     * @param registries 注册表访问器
     * @return 存档数据
     */
    public static EntityScanSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        EntityScanSavedData data = new EntityScanSavedData();
        if (!tag.contains("scan", Tag.TAG_COMPOUND)) {
            return data;
        }
        CompoundTag scanTag = tag.getCompound("scan");
        BlockOutline.Style style = scanTag.getBoolean("rainbow")
                ? BlockOutline.Style.RAINBOW : BlockOutline.Style.SOLID;
        EntityScanOutline.Scan scan = new EntityScanOutline.Scan(
                scanTag.getInt("rgb"),
                style,
                scanTag.getDouble("thickness"),
                scanTag.getInt("radius"),
                scanTag.getBoolean("through_walls"));
        List<EntityType<?>> types = new ArrayList<>();
        ListTag typeList = scanTag.getList("types", Tag.TAG_STRING);
        for (int i = 0; i < typeList.size(); i++) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(typeList.getString(i)));
            types.add(type);
        }
        List<TagKey<EntityType<?>>> tags = new ArrayList<>();
        ListTag tagList = scanTag.getList("tags", Tag.TAG_STRING);
        for (int i = 0; i < tagList.size(); i++) {
            tags.add(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(tagList.getString(i))));
        }
        data.target = EntityScanOutline.target(types, tags);
        data.scan = scan;
        return data;
    }

    /**
     * 保存存档数据。
     *
     * @param tag        NBT 标签
     * @param registries 注册表访问器
     */
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (target == null || scan == null) {
            return tag;
        }
        CompoundTag scanTag = new CompoundTag();
        scanTag.putInt("rgb", scan.rgb());
        scanTag.putBoolean("rainbow", scan.style() == BlockOutline.Style.RAINBOW);
        scanTag.putDouble("thickness", scan.thickness());
        scanTag.putInt("radius", scan.radius());
        scanTag.putBoolean("through_walls", scan.throughWalls());

        ListTag typeList = new ListTag();
        for (EntityType<?> type : target.types()) {
            typeList.add(StringTag.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString()));
        }
        scanTag.put("types", typeList);
        ListTag tagList = new ListTag();
        for (TagKey<EntityType<?>> entityTag : target.tags()) {
            tagList.add(StringTag.valueOf(entityTag.location().toString()));
        }
        scanTag.put("tags", tagList);
        tag.put("scan", scanTag);
        return tag;
    }

    /**
     * 取某个维度的实体扫描存档。
     *
     * @param level 维度
     * @return 存档数据
     */
    public static EntityScanSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(EntityScanSavedData::new, EntityScanSavedData::load),
                DATA_NAME);
    }

    /**
     * 设置扫描配置。
     *
     * @param target 扫描目标
     * @param scan   扫描配置
     */
    public void set(EntityScanOutline.Target target, EntityScanOutline.Scan scan) {
        this.target = target;
        this.scan = scan;
        setDirty();
    }

    /**
     * 清掉扫描配置。
     */
    public void clear() {
        this.target = null;
        this.scan = null;
        setDirty();
    }

    /**
     * 取当前扫描配置。
     *
     * @return 扫描配置，没有时返回 null
     */
    @Nullable
    public EntityScanOutline.Scan scan() {
        return scan;
    }

    /**
     * 取当前扫描目标。
     *
     * @return 扫描目标，没有时返回 null
     */
    @Nullable
    public EntityScanOutline.Target target() {
        return target;
    }
}
