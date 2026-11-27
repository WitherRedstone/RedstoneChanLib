package com.chinaex123.redstone_chan_lib.world;

import com.chinaex123.redstone_chan_lib.api.outline.BlockOutline;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * 方块描边存档数据。
 * <p>
 * 每个维度各存一份，记下这个维度里所有带描边的方块位置与配置，
 * 这样玩家退出重进、或者重新连上服务器时描边还在。
 */
public class BlockOutlineSavedData extends SavedData {

    /** 存档名称 */
    private static final String DATA_NAME = "redstone_chan_lib_block_outlines";

    /** 位置到描边配置的映射 */
    private final Map<BlockPos, BlockOutline.Outline> outlines = new HashMap<>();

    /**
     * 从 NBT 读取存档数据。
     *
     * @param tag        NBT 标签
     * @param registries 注册表访问器
     * @return 存档数据
     */
    public static BlockOutlineSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        BlockOutlineSavedData data = new BlockOutlineSavedData();
        ListTag list = tag.getList("outlines", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            BlockPos pos = new BlockPos(entry.getInt("x"), entry.getInt("y"), entry.getInt("z"));
            BlockOutline.Mode mode = BlockOutline.Mode.values()[Math.floorMod(entry.getInt("mode"), BlockOutline.Mode.values().length)];
            int rgb = entry.contains("rgb") ? entry.getInt("rgb") : BlockOutline.defaultColor(mode);
            double thickness = entry.contains("thickness") ? entry.getDouble("thickness") : BlockOutline.defaultThickness(mode);
            boolean throughWalls = !entry.contains("through_walls") || entry.getBoolean("through_walls");
            BlockOutline.Style style = entry.getBoolean("rainbow") ? BlockOutline.Style.RAINBOW : BlockOutline.Style.SOLID;
            boolean keepVisible = entry.getBoolean("keep_visible");
            double holdSeconds = entry.contains("hold_seconds") ? entry.getDouble("hold_seconds")
                    : BlockOutline.DEFAULT_HOLD_SECONDS;
            data.outlines.put(pos, new BlockOutline.Outline(mode, rgb, style, thickness, throughWalls, keepVisible,
                    holdSeconds));
        }
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
        ListTag list = new ListTag();
        for (Map.Entry<BlockPos, BlockOutline.Outline> entry : outlines.entrySet()) {
            CompoundTag item = new CompoundTag();
            BlockPos pos = entry.getKey();
            item.putInt("x", pos.getX());
            item.putInt("y", pos.getY());
            item.putInt("z", pos.getZ());
            BlockOutline.Outline outline = entry.getValue();
            item.putInt("mode", outline.mode().ordinal());
            item.putInt("rgb", outline.rgb());
            item.putDouble("thickness", outline.thickness());
            item.putBoolean("through_walls", outline.throughWalls());
            item.putBoolean("rainbow", outline.style() == BlockOutline.Style.RAINBOW);
            item.putBoolean("keep_visible", outline.keepVisible());
            item.putDouble("hold_seconds", outline.holdSeconds());
            list.add(item);
        }
        tag.put("outlines", list);
        return tag;
    }

    /**
     * 取某个维度的方块描边存档。
     *
     * @param level 维度
     * @return 存档数据
     */
    public static BlockOutlineSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(BlockOutlineSavedData::new, BlockOutlineSavedData::load),
                DATA_NAME);
    }

    /**
     * 写入或覆盖一条描边。
     *
     * @param pos     方块位置
     * @param outline 描边配置
     */
    public void put(BlockPos pos, BlockOutline.Outline outline) {
        outlines.put(pos.immutable(), outline);
        setDirty();
    }

    /**
     * 移除一条描边。
     *
     * @param pos 方块位置
     */
    public void remove(BlockPos pos) {
        if (outlines.remove(pos) != null) {
            setDirty();
        }
    }

    /**
     * 取一条描边。
     *
     * @param pos 方块位置
     * @return 描边配置，没有时返回 null
     */
    public BlockOutline.Outline get(BlockPos pos) {
        return outlines.get(pos);
    }

    /**
     * 取这个维度里所有描边。
     *
     * @return 位置到描边的映射
     */
    public Map<BlockPos, BlockOutline.Outline> all() {
        return outlines;
    }
}
