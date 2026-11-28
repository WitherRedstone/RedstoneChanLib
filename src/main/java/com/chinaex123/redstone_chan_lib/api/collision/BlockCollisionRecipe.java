package com.chinaex123.redstone_chan_lib.api.collision;

import com.chinaex123.redstone_chan_lib.init.RCLRecipes;
import com.chinaex123.redstone_chan_lib.util.PortalTexts;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 流体碰撞方块配方。
 * <p>
 * 当一种流体流进某个方块所在的位置（或紧贴着某个方块）时触发：
 * 声明“流入的流体”与“被碰撞的方块（或方块标签）”，以及若干条产出。
 *
 * @param fluid        流入的流体
 * @param blocks       被碰撞的方块集合，支持单个方块与方块标签
 * @param results      产出列表
 * @param delay        产出延迟（刻），默认 1
 * @param consumeFluid 触发后是否移除流入的那格流体
 * @param consumeBlock 触发后是否移除被碰撞的那格方块
 * @param sound        触发时播放的音效，可为空
 */
public record BlockCollisionRecipe(FluidIngredient fluid, HolderSet<Block> blocks, List<CollisionOutput> results,
                                   int delay, boolean consumeFluid, boolean consumeBlock,
                                   boolean consumeSource, @Nullable SoundEvent sound, BelowCondition below)
        implements Recipe<SingleRecipeInput> {

    /** 默认延迟（刻）。 */
    public static final int DEFAULT_DELAY = 1;
    /** 判定流体时使用的标准量。 */
    public static final int TEST_AMOUNT = 1000;
    /** 没写音效时使用的默认音效：水遇岩浆熄灭。 */
    public static final SoundEvent DEFAULT_SOUND = SoundEvents.LAVA_EXTINGUISH;

    public static final Codec<HolderSet<Block>> BLOCK_SET_CODEC = RegistryCodecs.homogeneousList(Registries.BLOCK);

    public static final StreamCodec<RegistryFriendlyByteBuf, HolderSet<Block>> BLOCK_SET_STREAM_CODEC = new StreamCodec<>() {

        /**
         * 写出方块集合。
         *
         * @param buffer 目标缓冲区
         * @param set    方块集合
         */
        @Override
        public void encode(RegistryFriendlyByteBuf buffer, HolderSet<Block> set) {
            List<Block> blocks = set.stream().map(Holder::value).toList();
            buffer.writeVarInt(blocks.size());
            for (Block block : blocks) {
                buffer.writeResourceLocation(BuiltInRegistries.BLOCK.getKey(block));
            }
        }

        /**
         * 读回方块集合。
         *
         * @param buffer 来源缓冲区
         * @return 方块集合
         */
        @Override
        public HolderSet<Block> decode(RegistryFriendlyByteBuf buffer) {
            int size = buffer.readVarInt();
            List<Holder<Block>> holders = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                holders.add(BuiltInRegistries.BLOCK.getHolder(buffer.readResourceLocation())
                        .orElseThrow(() -> new IllegalArgumentException(PortalTexts.text("collision_unknown_block"))));
            }
            return HolderSet.direct(holders);
        }
    };

    /**
     * 方块碰撞配方的编解码器。
     * <p>
     * 依次编解码流体原料、目标方块集合、产物列表、延迟刻数、
     * 是否消耗流体、是否消耗方块、是否消耗源方块、音效与下方条件；
     * 音效为可选字段，缺省时解析为 null。
     */
    public static final MapCodec<BlockCollisionRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            CollisionCodecs.FLUID_INGREDIENT.fieldOf("fluid").forGetter(BlockCollisionRecipe::fluid),
            CollisionCodecs.BLOCK_SET.fieldOf("block").forGetter(BlockCollisionRecipe::blocks),
            CollisionOutput.CODEC.listOf().fieldOf("results").forGetter(BlockCollisionRecipe::results),
            Codec.INT.optionalFieldOf("delay", DEFAULT_DELAY).forGetter(BlockCollisionRecipe::delay),
            Codec.BOOL.optionalFieldOf("consume_fluid", false).forGetter(BlockCollisionRecipe::consumeFluid),
            Codec.BOOL.optionalFieldOf("consume_block", false).forGetter(BlockCollisionRecipe::consumeBlock),
            Codec.BOOL.optionalFieldOf("consume_source", false).forGetter(BlockCollisionRecipe::consumeSource),
            BuiltInRegistries.SOUND_EVENT.byNameCodec().optionalFieldOf("sound").forGetter(recipe -> Optional.ofNullable(recipe.sound())),
            BelowCondition.MAP_CODEC.forGetter(BlockCollisionRecipe::below)
    ).apply(instance, (fluid, blocks, results, delay, consumeFluid, consumeBlock, consumeSource, sound, below) ->
            new BlockCollisionRecipe(fluid, blocks, results, delay, consumeFluid, consumeBlock,
                    consumeSource, sound.orElse(null), below)
            )
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, BlockCollisionRecipe> STREAM_CODEC = new StreamCodec<>() {

        /**
         * 写出配方。
         *
         * @param buffer 目标缓冲区
         * @param recipe 配方
         */
        @Override
        public void encode(RegistryFriendlyByteBuf buffer, BlockCollisionRecipe recipe) {
            FluidIngredient.STREAM_CODEC.encode(buffer, recipe.fluid());
            BLOCK_SET_STREAM_CODEC.encode(buffer, recipe.blocks());
            buffer.writeVarInt(recipe.results().size());
            for (CollisionOutput output : recipe.results()) {
                CollisionOutput.STREAM_CODEC.encode(buffer, output);
            }
            buffer.writeVarInt(recipe.delay());
            buffer.writeBoolean(recipe.consumeFluid());
            buffer.writeBoolean(recipe.consumeBlock());
            buffer.writeBoolean(recipe.consumeSource());
            buffer.writeBoolean(recipe.sound() != null);
            if (recipe.sound() != null) {
                buffer.writeResourceLocation(BuiltInRegistries.SOUND_EVENT.getKey(recipe.sound()));
            }
            BelowCondition.STREAM_CODEC.encode(buffer, recipe.below());
        }

        /**
         * 读回配方。
         *
         * @param buffer 来源缓冲区
         * @return 配方
         */
        @Override
        public BlockCollisionRecipe decode(RegistryFriendlyByteBuf buffer) {
            FluidIngredient fluid = FluidIngredient.STREAM_CODEC.decode(buffer);
            HolderSet<Block> blocks = BLOCK_SET_STREAM_CODEC.decode(buffer);
            int size = buffer.readVarInt();
            List<CollisionOutput> results = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                results.add(CollisionOutput.STREAM_CODEC.decode(buffer));
            }
            int delay = buffer.readVarInt();
            boolean consumeFluid = buffer.readBoolean();
            boolean consumeBlock = buffer.readBoolean();
            boolean consumeSource = buffer.readBoolean();
            SoundEvent sound = buffer.readBoolean() ? BuiltInRegistries.SOUND_EVENT.get(buffer.readResourceLocation()) : null;
            BelowCondition below = BelowCondition.STREAM_CODEC.decode(buffer);
            return new BlockCollisionRecipe(fluid, blocks, results, delay, consumeFluid, consumeBlock, consumeSource, sound, below);
        }
    };

    /**
     * 数据包读取时的构造器。
     *
     * @param fluid        流入的流体
     * @param blocks       被碰撞的方块
     * @param results      产出
     * @param delay        延迟
     * @param consumeFluid 是否消耗流入流体
     * @param consumeBlock 是否消耗被碰撞方块
     * @param sound        音效（可选）
     */
    public BlockCollisionRecipe(FluidIngredient fluid, HolderSet<Block> blocks, List<CollisionOutput> results,
                                int delay, boolean consumeFluid, boolean consumeBlock, boolean consumeSource,
                                Optional<SoundEvent> sound) {
        this(fluid, blocks, results, delay, consumeFluid, consumeBlock, consumeSource, sound.orElse(null), BelowCondition.NONE);
    }

    /**
     * 数据包读取时的便捷构造器（不消耗源头）。
     *
     * @param fluid        流入的流体
     * @param blocks       被碰撞的方块
     * @param results      产出
     * @param delay        延迟
     * @param consumeFluid 是否消耗流入流体
     * @param consumeBlock 是否消耗被碰撞方块
     * @param sound        音效（可选）
     */
    public BlockCollisionRecipe(FluidIngredient fluid, HolderSet<Block> blocks, List<CollisionOutput> results,
                                int delay, boolean consumeFluid, boolean consumeBlock, @Nullable SoundEvent sound) {
        this(fluid, blocks, results, delay, consumeFluid, consumeBlock, false, sound, BelowCondition.NONE);
    }

    /**
     * 只关心“是否消耗源头”的便捷构造器，音效用默认值。
     *
     * @param fluid         流入的流体
     * @param blocks        被碰撞的方块
     * @param results       产出
     * @param delay         延迟
     * @param consumeFluid  是否消耗流入流体
     * @param consumeBlock  是否消耗被碰撞方块
     * @param consumeSource 是否消耗流体源头
     */
    public BlockCollisionRecipe(FluidIngredient fluid, HolderSet<Block> blocks, List<CollisionOutput> results,
                                int delay, boolean consumeFluid, boolean consumeBlock, boolean consumeSource) {
        this(fluid, blocks, results, delay, consumeFluid, consumeBlock, consumeSource, DEFAULT_SOUND, BelowCondition.NONE);
    }

    /**
     * 数据包读取时的便捷构造器（不消耗源头，音效用 Optional 表示）。
     *
     * @param fluid        流入的流体
     * @param blocks       被碰撞的方块
     * @param results      产出
     * @param delay        延迟
     * @param consumeFluid 是否消耗流入流体
     * @param consumeBlock 是否消耗被碰撞方块
     * @param sound        音效（可选）
     */
    public BlockCollisionRecipe(FluidIngredient fluid, HolderSet<Block> blocks, List<CollisionOutput> results,
                                int delay, boolean consumeFluid, boolean consumeBlock, Optional<SoundEvent> sound) {
        this(fluid, blocks, results, delay, consumeFluid, consumeBlock, false, sound.orElse(null), BelowCondition.NONE);
    }

    public BlockCollisionRecipe {
        if (blocks.size() == 0) {
            throw new IllegalArgumentException(PortalTexts.text("collision_blocks_empty"));
        }
        if (results.isEmpty()) {
            throw new IllegalArgumentException(PortalTexts.text("collision_results_empty"));
        }
        if (delay < 0) {
            throw new IllegalArgumentException(PortalTexts.text("collision_delay", delay));
        }
        results = List.copyOf(results);
        if (sound == null) {
            sound = DEFAULT_SOUND;
        }
    }

    /**
     * 判断某个流体状态是否满足“流入的流体”条件。
     *
     * @param state 流体状态
     * @return 满足返回 true
     */
    public boolean testFluid(FluidState state) {
        return FluidStates.matches(fluid, state);
    }

    /**
     * 判断某个方块状态是否在“被碰撞的方块”集合里。
     *
     * @param state 方块状态
     * @return 在集合里返回 true
     */
    public boolean testBlock(BlockState state) {
        return blocks.contains(state.getBlock().builtInRegistryHolder());
    }

    /**
     * 取“流入的流体”里包含的代表流体，用于 JEI 展示。
     *
     * @return 流体数组
     */
    public Fluid[] fluidTypes() {
        return java.util.Arrays.stream(fluid.getStacks())
                .map(FluidStack::getFluid)
                .distinct()
                .toArray(Fluid[]::new);
    }

    /**
     * 把一个方块标签变成配方需要的方块集合。
     * <p>
     * 静态注册表上的标签在数据包加载之后才可用，所以这里在找不到时直接抛异常，
     * 而不是悄悄返回空集合。
     *
     * @param tag 方块标签
     * @return 方块集合
     */
    public static HolderSet<Block> tag(TagKey<Block> tag) {
        return BuiltInRegistries.BLOCK.getTag(tag)
                .orElseThrow(() -> new IllegalArgumentException(PortalTexts.text("collision_unknown_block_tag", tag.location())));
    }

    /**
     * 把若干方块包成一个方块集合，方便在 Java 里直接写配方。
     *
     * @param blocks 方块
     * @return 方块集合
     */
    public static HolderSet<Block> blocks(Block... blocks) {
        List<Holder<Block>> holders = new ArrayList<>(blocks.length);
        for (Block block : blocks) {
            holders.add(block.builtInRegistryHolder());
        }
        return HolderSet.direct(holders);
    }

    /**
     * 该配方不靠物品栏匹配。
     *
     * @param input 输入
     * @param level 世界
     * @return 恒为 false
     */
    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return false;
    }

    /**
     * 该配方没有物品形态的产出。
     *
     * @param input      输入
     * @param registries 注册表
     * @return 空物品堆
     */
    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    /**
     * 不受合成网格尺寸限制。
     *
     * @param width  宽度
     * @param height 高度
     * @return 恒为 true
     */
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    /**
     * 该配方没有物品形态的产出。
     *
     * @param registries 注册表
     * @return 空物品堆
     */
    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    /**
     * 获取序列化器。
     *
     * @return 方块碰撞配方序列化器
     */
    @Override
    public RecipeSerializer<?> getSerializer() {
        return RCLRecipes.BLOCK_COLLISION_SERIALIZER.get();
    }

    /**
     * 获取配方类型。
     *
     * @return 方块碰撞配方类型
     */
    @Override
    public RecipeType<?> getType() {
        return RCLRecipes.BLOCK_COLLISION_TYPE;
    }
}