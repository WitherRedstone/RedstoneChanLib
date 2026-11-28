package com.chinaex123.redstone_chan_lib.api.collision;

import com.chinaex123.redstone_chan_lib.util.PortalTexts;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * 碰撞配方的一条产出。
 * <p>
 * 一条产出要么是方块、要么是物品，两者不能同时存在；概率决定这条产出是否真的生效，
 * 数量对方块表示“最多尝试放置几格”，对物品表示物品堆的数量。
 *
 * @param block  产出的方块，可为空
 * @param stack  产出的物品，可为空
 * @param count  数量
 * @param chance 生效概率，取值 0~1
 */
public record CollisionOutput(@Nullable Block block, @Nullable ItemStack stack, int count, float chance) {

    /** 默认数量。 */
    public static final int DEFAULT_COUNT = 1;
    /** 默认概率。 */
    public static final float DEFAULT_CHANCE = 1.0F;

    /**
     * 编码器：block 与 item 都是可选字段，写回时只会出现其中一个。
     * <p>
     * 两个字段都允许省略，但构造器会检查“至少要有一个”，所以字段名写错时能立刻报错，
     * 而不是静默解析成一条空产出。
     */
    public static final Codec<CollisionOutput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("block")
                    .forGetter(output -> Optional.ofNullable(output.block())),
            CollisionCodecs.ITEM_STACK.optionalFieldOf("item")
                    .forGetter(output -> Optional.ofNullable(output.stack())),
            Codec.intRange(1, 64).optionalFieldOf("count", DEFAULT_COUNT).forGetter(CollisionOutput::count),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("chance", DEFAULT_CHANCE).forGetter(CollisionOutput::chance)
    ).apply(instance, CollisionOutput::fromOptionals));

    /**
     * 由两个可选字段构造一条产出。
     *
     * @param block  方块（可为空）
     * @param stack  物品（可为空）
     * @param count  数量
     * @param chance 概率
     * @return 产出
     */
    private static CollisionOutput fromOptionals(Optional<Block> block, Optional<ItemStack> stack, int count, float chance) {
        return new CollisionOutput(block.orElse(null), stack.orElse(null), count, chance);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, CollisionOutput> STREAM_CODEC = new StreamCodec<>() {

        /**
         * 写出产出。
         *
         * @param buffer 目标缓冲区
         * @param output 产出
         */
        @Override
        public void encode(RegistryFriendlyByteBuf buffer, CollisionOutput output) {
            buffer.writeBoolean(output.block() != null);
            if (output.block() != null) {
                buffer.writeResourceLocation(BuiltInRegistries.BLOCK.getKey(output.block()));
            } else {
                ItemStack.STREAM_CODEC.encode(buffer, output.stack() == null ? ItemStack.EMPTY : output.stack());
            }
            buffer.writeVarInt(output.count());
            buffer.writeFloat(output.chance());
        }

        /**
         * 读回产出。
         *
         * @param buffer 来源缓冲区
         * @return 产出
         */
        @Override
        public CollisionOutput decode(RegistryFriendlyByteBuf buffer) {
            Block block = null;
            ItemStack stack = null;
            if (buffer.readBoolean()) {
                block = BuiltInRegistries.BLOCK.get(buffer.readResourceLocation());
            } else {
                stack = ItemStack.STREAM_CODEC.decode(buffer);
            }
            int count = buffer.readVarInt();
            float chance = buffer.readFloat();
            return new CollisionOutput(block, stack, count, chance);
        }
    };

    /**
     * 数据包读取时的构造器。
     *
     * @param block  方块（可选）
     * @param stack  物品（可选）
     * @param count  数量
     * @param chance 概率
     */
    public CollisionOutput(Optional<Block> block, Optional<ItemStack> stack, int count, float chance) {
        this(block.orElse(null), stack.orElse(null), count, chance);
    }

    /**
     * 校验产出内容。
     *
     * @param block  方块
     * @param stack  物品
     * @param count  数量
     * @param chance 概率
     */
    public CollisionOutput {
        if (block != null && stack != null) {
            throw new IllegalArgumentException(PortalTexts.text("collision_output_both", count));
        }
        if (block == null && stack == null) {
            throw new IllegalArgumentException(PortalTexts.text("collision_output_empty"));
        }
        if (count < 1) {
            throw new IllegalArgumentException(PortalTexts.text("collision_output_count", count));
        }
        if (chance < 0.0F || chance > 1.0F) {
            throw new IllegalArgumentException(PortalTexts.text("collision_output_chance", chance));
        }
    }

    /**
     * 创建一个方块产出。
     *
     * @param block 方块
     * @return 产出
     */
    public static CollisionOutput block(Block block) {
        return new CollisionOutput(block, null, DEFAULT_COUNT, DEFAULT_CHANCE);
    }

    /**
     * 创建一个方块产出。
     *
     * @param block  方块
     * @param chance 概率
     * @return 产出
     */
    public static CollisionOutput block(Block block, float chance) {
        return new CollisionOutput(block, null, DEFAULT_COUNT, chance);
    }

    /**
     * 创建一个方块产出。
     *
     * @param block  方块
     * @param count  数量
     * @param chance 概率
     * @return 产出
     */
    public static CollisionOutput block(Block block, int count, float chance) {
        return new CollisionOutput(block, null, count, chance);
    }

    /**
     * 创建一个物品产出。
     *
     * @param item 物品
     * @return 产出
     */
    public static CollisionOutput item(Item item) {
        return new CollisionOutput(null, new ItemStack(item), DEFAULT_COUNT, DEFAULT_CHANCE);
    }

    /**
     * 创建一个物品产出。
     *
     * @param item   物品
     * @param count  数量
     * @param chance 概率
     * @return 产出
     */
    public static CollisionOutput item(Item item, int count, float chance) {
        return new CollisionOutput(null, new ItemStack(item), count, chance);
    }

    /**
     * 创建一个物品产出。
     *
     * @param stack  物品堆
     * @param chance 概率
     * @return 产出
     */
    public static CollisionOutput item(ItemStack stack, float chance) {
        return new CollisionOutput(null, stack.copy(), Math.max(1, stack.getCount()), chance);
    }

    /**
     * 判断这条产出是不是方块。
     *
     * @return 方块产出返回 true
     */
    public boolean isBlock() {
        return block != null;
    }
}
