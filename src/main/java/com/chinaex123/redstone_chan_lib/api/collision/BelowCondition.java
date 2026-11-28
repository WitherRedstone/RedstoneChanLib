package com.chinaex123.redstone_chan_lib.api.collision;

import com.chinaex123.redstone_chan_lib.util.PortalTexts;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * “碰撞位置下方需要什么方块”的附加条件。
 *
 * <p>三个位置都是可选的，都可以写方块 id 或者 {@code #} 开头的标签：</p>
 * <ul>
 *     <li>{@code fluid_below}：流入的那一格流体正下方的方块（流体自己脚下）；</li>
 *     <li>{@code down}：参与碰撞的对方（方块那一边）正下方的方块；</li>
 *     <li>{@code center}：流体与对方中间那一格空格正下方的方块。</li>
 * </ul>
 *
 * <p>写几个就要求几个同时满足；都不写表示不限制。流体撞流体时 likewise 生效，
 * 此时 {@code down} 看的是另一种流体那格脚下。</p>
 *
 * <p>Java 里可以用 {@link #fluid(Block...)}、{@link #down(Block...)}、
 * {@link #center(Block...)}、{@link #all(HolderSet, HolderSet, HolderSet)} 这些工厂直接写方块，
 * 去数据包那边则写成 {@code fluid_below} / {@code down} / {@code center} 字段（不能叫 {@code fluid}，会和配方本身的流体原料重名）。</p>
 *
 * @param fluid  流体自己脚下的方块集合
 * @param down   对方脚下的方块集合
 * @param center 中间空格下方的方块集合
 */
public record BelowCondition(@Nullable HolderSet<Block> fluid, @Nullable HolderSet<Block> down,
                             @Nullable HolderSet<Block> center) {

    /** 不做任何限制。 */
    public static final BelowCondition NONE = new BelowCondition(null, null, null);

    /** 数据包编解码（三个字段都是可选的）。 */
    public static final MapCodec<BelowCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            CollisionCodecs.BLOCK_SET.optionalFieldOf("fluid_below").forGetter(condition -> Optional.ofNullable(condition.fluid())),
            CollisionCodecs.BLOCK_SET.optionalFieldOf("down").forGetter(condition -> Optional.ofNullable(condition.down())),
            CollisionCodecs.BLOCK_SET.optionalFieldOf("center").forGetter(condition -> Optional.ofNullable(condition.center()))
    ).apply(instance, (fluid, down, center) -> new BelowCondition(fluid.orElse(null), down.orElse(null),
            center.orElse(null))));

    /** 数据包编解码的 Codec 形式。 */
    public static final Codec<BelowCondition> CODEC = MAP_CODEC.codec();

    /** 网络流编解码。 */
    public static final StreamCodec<RegistryFriendlyByteBuf, BelowCondition> STREAM_CODEC = new StreamCodec<>() {

        /**
         * 写出条件。
         *
         * @param buffer    目标缓冲区
         * @param condition 条件
         */
        @Override
        public void encode(RegistryFriendlyByteBuf buffer, BelowCondition condition) {
            writeBlockSet(buffer, condition.fluid());
            writeBlockSet(buffer, condition.down());
            writeBlockSet(buffer, condition.center());
        }

        /**
         * 读回条件。
         *
         * @param buffer 来源缓冲区
         * @return 条件
         */
        @Override
        public BelowCondition decode(RegistryFriendlyByteBuf buffer) {
            return new BelowCondition(readBlockSet(buffer), readBlockSet(buffer), readBlockSet(buffer));
        }
    };

    /**
     * 把一个可空的方块集合写进缓冲区。
     *
     * @param buffer 目标缓冲区
     * @param set    方块集合，可为空
     */
    private static void writeBlockSet(RegistryFriendlyByteBuf buffer, @Nullable HolderSet<Block> set) {
        buffer.writeBoolean(set != null);
        if (set == null) {
            return;
        }
        List<Block> blocks = set.stream().map(Holder::value).toList();
        buffer.writeVarInt(blocks.size());
        for (Block block : blocks) {
            buffer.writeResourceLocation(BuiltInRegistries.BLOCK.getKey(block));
        }
    }

    /**
     * 读回一个可空的方块集合。
     *
     * @param buffer 来源缓冲区
     * @return 方块集合，可能为空
     */
    @Nullable
    private static HolderSet<Block> readBlockSet(RegistryFriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) {
            return null;
        }
        int size = buffer.readVarInt();
        List<Holder<Block>> holders = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            holders.add(BuiltInRegistries.BLOCK.getHolder(buffer.readResourceLocation())
                    .orElseThrow(() -> new IllegalArgumentException(PortalTexts.text("collision_unknown_block"))));
        }
        return HolderSet.direct(holders);
    }

    /**
     * 造一个只限制“流体自己脚下”的条件。
     *
     * @param fluid 流体下方的方块集合
     * @return 条件
     */
    public static BelowCondition fluid(HolderSet<Block> fluid) {
        return new BelowCondition(fluid, null, null);
    }

    /**
     * 造一个只限制“流体自己脚下”的条件。
     *
     * @param blocks 允许出现在流体下方的方块，至少一个
     * @return 条件
     */
    public static BelowCondition fluid(Block... blocks) {
        return fluid(setOf(blocks));
    }

    /**
     * 造一个只限制“流体自己脚下”的条件。
     *
     * @param tag 允许出现在流体下方的方块标签
     * @return 条件
     */
    public static BelowCondition fluid(TagKey<Block> tag) {
        return fluid(setOf(tag));
    }

    /**
     * 造一个只限制“对方脚下”的条件。
     *
     * @param down 对方下方的方块集合
     * @return 条件
     */
    public static BelowCondition down(HolderSet<Block> down) {
        return new BelowCondition(null, down, null);
    }

    /**
     * 造一个只限制“对方脚下”的条件。
     *
     * @param blocks 允许出现在对方下方的方块，至少一个
     * @return 条件
     */
    public static BelowCondition down(Block... blocks) {
        return down(setOf(blocks));
    }

    /**
     * 造一个只限制“对方脚下”的条件。
     *
     * @param tag 允许出现在对方下方的方块标签
     * @return 条件
     */
    public static BelowCondition down(TagKey<Block> tag) {
        return down(setOf(tag));
    }

    /**
     * 造一个只限制“中间空格下方”的条件。
     *
     * @param center 中间空格下方的方块集合
     * @return 条件
     */
    public static BelowCondition center(HolderSet<Block> center) {
        return new BelowCondition(null, null, center);
    }

    /**
     * 造一个只限制“中间空格下方”的条件。
     *
     * @param blocks 允许出现在中间空格下方的方块，至少一个
     * @return 条件
     */
    public static BelowCondition center(Block... blocks) {
        return center(setOf(blocks));
    }

    /**
     * 造一个只限制“中间空格下方”的条件。
     *
     * @param tag 允许出现在中间空格下方的方块标签
     * @return 条件
     */
    public static BelowCondition center(TagKey<Block> tag) {
        return center(setOf(tag));
    }

    /**
     * 造一个同时限制两处下方的条件（流体脚下 + 对方脚下）。
     *
     * @param fluid 流体下方的方块集合，可为空
     * @param down  对方下方的方块集合，可为空
     * @return 条件
     */
    public static BelowCondition both(@Nullable HolderSet<Block> fluid, @Nullable HolderSet<Block> down) {
        return new BelowCondition(fluid, down, null);
    }

    /**
     * 造一个三处都限制的条件。
     *
     * @param fluid  流体下方的方块集合，可为空
     * @param down   对方下方的方块集合，可为空
     * @param center 中间空格下方的方块集合，可为空
     * @return 条件
     */
    public static BelowCondition all(@Nullable HolderSet<Block> fluid, @Nullable HolderSet<Block> down,
                                     @Nullable HolderSet<Block> center) {
        return new BelowCondition(fluid, down, center);
    }

    /**
     * 把若干个方块收成一个集合。
     *
     * @param blocks 方块，至少一个
     * @return 方块集合
     */
    private static HolderSet<Block> setOf(Block... blocks) {
        if (blocks == null || blocks.length == 0) {
            throw new IllegalArgumentException(PortalTexts.text("collision_blocks_empty"));
        }
        List<Holder<Block>> holders = new ArrayList<>(blocks.length);
        for (Block block : blocks) {
            holders.add(block.builtInRegistryHolder());
        }
        return HolderSet.direct(holders);
    }

    /**
     * 把一个方块标签收成集合。
     *
     * @param tag 方块标签
     * @return 方块集合
     */
    private static HolderSet<Block> setOf(TagKey<Block> tag) {
        return BuiltInRegistries.BLOCK.getTag(tag)
                .orElseThrow(() -> new IllegalArgumentException(PortalTexts.text("collision_unknown_block_tag", tag.location())));
    }

    /**
     * 是否不做限制。
     *
     * @return 三个字段都为空时返回 true
     */
    public boolean isEmpty() {
        return this.fluid == null && this.down == null && this.center == null;
    }

    /**
     * 判断“流体自己脚下”是否满足。
     *
     * @param state 下方的方块状态
     * @return 没限制或命中返回 true
     */
    public boolean testFluid(BlockState state) {
        return this.fluid == null || this.fluid.contains(state.getBlock().builtInRegistryHolder());
    }

    /**
     * 判断“对方脚下”是否满足。
     *
     * @param state 下方的方块状态
     * @return 没限制或命中返回 true
     */
    public boolean testDown(BlockState state) {
        return this.down == null || this.down.contains(state.getBlock().builtInRegistryHolder());
    }

    /**
     * 判断“中间空格下方”是否满足。
     *
     * @param state 下方的方块状态
     * @return 没限制或命中返回 true
     */
    public boolean testCenter(BlockState state) {
        return this.center == null || this.center.contains(state.getBlock().builtInRegistryHolder());
    }
}
