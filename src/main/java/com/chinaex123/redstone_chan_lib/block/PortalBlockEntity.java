package com.chinaex123.redstone_chan_lib.block;

import com.chinaex123.redstone_chan_lib.api.portal.PortalDefinition;
import com.chinaex123.redstone_chan_lib.api.portal.PortalDefinitions;
import com.chinaex123.redstone_chan_lib.init.RCLBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 传送门方块实体。
 * <p>
 * 保存传送门所属定义 ID、颜色与自定义名称，并提供名称查询与同步：
 * 应用定义时写入 ID、颜色与名称，并通知客户端刷新；
 * 定义查找失败时回退到默认定义。
 */
public class PortalBlockEntity extends BlockEntity implements Nameable {

    /** 定义 id 的存档键 */
    private static final String TAG_DEFINITION = "Definition";
    /** 颜色（ARGB）的存档键 */
    private static final String TAG_COLOR = "Color";
    /** 自定义名称的存档键 */
    private static final String TAG_NAME = "CustomName";

    /** 传送门颜色。 */
    private int color = PortalDefinition.DEFAULT_COLOR;

    /** 所属传送门定义 ID。 */
    @Nullable
    private ResourceLocation definitionId;

    /** 自定义名称。 */
    @Nullable
    private Component customName;

    /**
     * 构造传送门方块实体。
     *
     * @param pos   方块位置
     * @param state 方块状态
     */
    public PortalBlockEntity(BlockPos pos, BlockState state) {
        super(RCLBlockEntities.PORTAL.get(), pos, state);
    }

    /**
     * 应用传送门定义。
     * <p>
     * 写入定义 ID、颜色与名称，并在服务端通知客户端刷新方块实体数据，
     * 使颜色与名称立即生效。
     *
     * @param definition 传送门定义
     */
    public void applyDefinition(PortalDefinition definition) {
        this.definitionId = definition.id();
        this.color = definition.color();
        this.customName = definition.name();
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            // 通知客户端刷新方块实体数据，颜色与名称才能立即生效
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    /**
     * 获取所属传送门定义。
     * <p>
     * 按 ID 查找定义，找不到时回退到默认定义。
     *
     * @return 传送门定义，无可用定义时返回 null
     */
    @Nullable
    public PortalDefinition getDefinition() {
        PortalDefinition definition = PortalDefinitions.get(this.definitionId);
        return definition != null ? definition : PortalDefinitions.defaultDefinition();
    }

    /**
     * 获取所属传送门定义 ID。
     *
     * @return 定义 ID，可能为 null
     */
    @Nullable
    public ResourceLocation getDefinitionId() {
        return this.definitionId;
    }

    /**
     * 获取传送门颜色。
     *
     * @return 传送门颜色（ARGB）
     */
    public int getColor() {
        return this.color;
    }

    /**
     * 设置传送门颜色。
     *
     * @param argb 颜色（ARGB）
     */
    public void setColor(int argb) {
        this.color = argb;
        this.setChanged();
    }

    /**
     * 获取方块实体名称。
     * <p>
     * 优先使用自定义名称，其次为所属定义名称，最后回退到默认翻译名。
     *
     * @return 名称
     */
    @Override
    public Component getName() {
        if (this.customName != null) {
            return this.customName;
        }
        PortalDefinition definition = this.getDefinition();
        if (definition != null) {
            return definition.name();
        }
        return Component.translatable("block.redstone_chan_lib.custom_portal");
    }

    /**
     * 获取自定义名称。
     *
     * @return 自定义名称，未设置时返回 null
     */
    @Override
    @Nullable
    public Component getCustomName() {
        return this.customName;
    }

    /**
     * 设置自定义名称。
     *
     * @param name 名称，可为 null 表示清除
     */
    public void setCustomName(@Nullable Component name) {
        this.customName = name;
        this.setChanged();
    }

    /**
     * 读取方块实体数据。
     * <p>
     * 还原定义 ID、颜色与自定义名称。
     *
     * @param tag        数据标签
     * @param registries 注册表提供者
     */
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_DEFINITION)) {
            this.definitionId = ResourceLocation.tryParse(tag.getString(TAG_DEFINITION));
        }
        if (tag.contains(TAG_COLOR)) {
            this.color = tag.getInt(TAG_COLOR);
        }
        if (tag.contains(TAG_NAME)) {
            this.customName = Component.Serializer.fromJson(tag.getString(TAG_NAME), registries);
        }
    }

    /**
     * 保存方块实体数据。
     * <p>
     * 写入定义 ID、颜色与自定义名称。
     *
     * @param tag        数据标签
     * @param registries 注册表提供者
     */
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.definitionId != null) {
            tag.putString(TAG_DEFINITION, this.definitionId.toString());
        }
        tag.putInt(TAG_COLOR, this.color);
        if (this.customName != null) {
            tag.putString(TAG_NAME, Component.Serializer.toJson(this.customName, registries));
        }
    }

    /**
     * 获取更新标签。
     * <p>
     * 返回不含元数据的保存数据，用于同步到客户端。
     *
     * @param registries 注册表提供者
     * @return 更新标签
     */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    /**
     * 获取更新数据包。
     *
     * @return 方块实体数据包
     */
    @Override
    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}