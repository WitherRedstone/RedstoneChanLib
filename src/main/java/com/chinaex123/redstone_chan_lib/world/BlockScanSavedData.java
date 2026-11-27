package com.chinaex123.redstone_chan_lib.world;

import com.chinaex123.redstone_chan_lib.api.outline.BlockOutline;
import com.chinaex123.redstone_chan_lib.api.outline.BlockScanOutline;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 扫描高亮的存档数据。
 * <p>
 * 每个维度各存一份，记下这个维度当前生效的扫描配置，
 * 这样玩家退出重进、或者换维度回来时扫描还在。
 */
public class BlockScanSavedData extends SavedData {

    /** 存档名称 */
    private static final String DATA_NAME = "redstone_chan_lib_block_scans";

    /** 当前扫描目标，没有时为 null */
    @Nullable
    private BlockScanOutline.Target target;

    /** 当前扫描配置，没有时为 null */
    @Nullable
    private BlockScanOutline.Scan scan;

    /**
     * 从 NBT 读取存档数据。
     *
     * @param tag        NBT 标签
     * @param registries 注册表访问器
     * @return 存档数据
     */
    public static BlockScanSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        BlockScanSavedData data = new BlockScanSavedData();
        if (!tag.contains("scan", Tag.TAG_COMPOUND)) {
            return data;
        }
        CompoundTag scanTag = tag.getCompound("scan");
        BlockOutline.Mode mode = BlockOutline.Mode.values()[
                Math.floorMod(scanTag.getInt("mode"), BlockOutline.Mode.values().length)];
        BlockOutline.Style style = scanTag.getBoolean("rainbow")
                ? BlockOutline.Style.RAINBOW : BlockOutline.Style.SOLID;
        Map<Block, Integer> colors = new LinkedHashMap<>();
        ListTag colorIds = scanTag.getList("color_ids", Tag.TAG_STRING);
        ListTag colorValues = scanTag.getList("color_values", Tag.TAG_INT);
        for (int i = 0; i < colorIds.size() && i < colorValues.size(); i++) {
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(colorIds.getString(i)));
            colors.put(block, colorValues.getInt(i));
        }
        Map<TagKey<Block>, Integer> tagColors = new LinkedHashMap<>();
        ListTag colorTagIds = scanTag.getList("color_tag_ids", Tag.TAG_STRING);
        ListTag colorTagValues = scanTag.getList("color_tag_values", Tag.TAG_INT);
        for (int i = 0; i < colorTagIds.size() && i < colorTagValues.size(); i++) {
            tagColors.put(TagKey.create(Registries.BLOCK, ResourceLocation.parse(colorTagIds.getString(i))),
                    colorTagValues.getInt(i));
        }
        BlockScanOutline.Scan scan = new BlockScanOutline.Scan(
                mode,
                scanTag.getInt("rgb"),
                style,
                scanTag.getDouble("thickness"),
                scanTag.getInt("radius"),
                scanTag.getBoolean("through_walls"),
                colors,
                tagColors);
        List<Block> blocks = new ArrayList<>();
        ListTag blockList = scanTag.getList("blocks", Tag.TAG_STRING);
        for (int i = 0; i < blockList.size(); i++) {
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockList.getString(i)));
            blocks.add(block);
        }
        List<TagKey<Block>> tags = new ArrayList<>();
        ListTag tagList = scanTag.getList("tags", Tag.TAG_STRING);
        for (int i = 0; i < tagList.size(); i++) {
            tags.add(TagKey.create(Registries.BLOCK, ResourceLocation.parse(tagList.getString(i))));
        }
        data.target = BlockScanOutline.target(blocks, tags);
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
        scanTag.putInt("mode", scan.mode().ordinal());
        scanTag.putInt("rgb", scan.rgb());
        scanTag.putBoolean("rainbow", scan.style() == BlockOutline.Style.RAINBOW);
        scanTag.putDouble("thickness", scan.thickness());
        scanTag.putInt("radius", scan.radius());
        scanTag.putBoolean("through_walls", scan.throughWalls());

        ListTag blockList = new ListTag();
        for (Block block : target.blocks()) {
            blockList.add(StringTag.valueOf(BuiltInRegistries.BLOCK.getKey(block).toString()));
        }
        scanTag.put("blocks", blockList);
        ListTag tagList = new ListTag();
        for (TagKey<Block> blockTag : target.tags()) {
            tagList.add(StringTag.valueOf(blockTag.location().toString()));
        }
        scanTag.put("tags", tagList);
        ListTag colorIds = new ListTag();
        ListTag colorValues = new ListTag();
        for (Map.Entry<Block, Integer> entry : scan.colors().entrySet()) {
            colorIds.add(StringTag.valueOf(BuiltInRegistries.BLOCK.getKey(entry.getKey()).toString()));
            colorValues.add(IntTag.valueOf(entry.getValue()));
        }
        scanTag.put("color_ids", colorIds);
        scanTag.put("color_values", colorValues);
        ListTag colorTagIds = new ListTag();
        ListTag colorTagValues = new ListTag();
        for (Map.Entry<TagKey<Block>, Integer> entry : scan.tagColors().entrySet()) {
            colorTagIds.add(StringTag.valueOf(entry.getKey().location().toString()));
            colorTagValues.add(IntTag.valueOf(entry.getValue()));
        }
        scanTag.put("color_tag_ids", colorTagIds);
        scanTag.put("color_tag_values", colorTagValues);
        tag.put("scan", scanTag);
        return tag;
    }

    /**
     * 取某个维度的扫描存档。
     *
     * @param level 维度
     * @return 存档数据
     */
    public static BlockScanSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(BlockScanSavedData::new, BlockScanSavedData::load),
                DATA_NAME);
    }

    /**
     * 设置扫描配置。
     *
     * @param target 扫描目标
     * @param scan   扫描配置
     */
    public void set(BlockScanOutline.Target target, BlockScanOutline.Scan scan) {
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
    public BlockScanOutline.Scan scan() {
        return scan;
    }

    /**
     * 取当前扫描目标。
     *
     * @return 扫描目标，没有时返回 null
     */
    @Nullable
    public BlockScanOutline.Target target() {
        return target;
    }
}
