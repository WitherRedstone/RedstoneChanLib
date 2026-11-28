package com.chinaex123.redstone_chan_lib.api.collision;

import com.chinaex123.redstone_chan_lib.init.RCLRecipes;
import com.chinaex123.redstone_chan_lib.util.PortalTexts;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 流体碰撞流体配方。
 *
 * @param fluid        流入的流体
 * @param otherFluid   被碰撞的流体
 * @param results      产出列表
 * @param delay        产出延迟（刻），默认 1
 * @param consumeFluid 触发后是否移除流入的那格流体
 * @param consumeOther  触发后是否移除被碰撞的那格流体
 * @param consumeSource 触发后是否连流体源头一起移除
 * @param sound         触发时播放的音效，省略时默认用“水遇岩浆熄灭”
 */
public record FluidCollisionRecipe(FluidIngredient fluid, FluidIngredient otherFluid, List<CollisionOutput> results,
                                   int delay, boolean consumeFluid, boolean consumeOther,
                                   boolean consumeSource, @Nullable SoundEvent sound, BelowCondition below)
        implements Recipe<SingleRecipeInput> {

    /** 默认延迟（刻）。 */
    public static final int DEFAULT_DELAY = 1;
    /** 判定流体时使用的标准量。 */
    public static final int TEST_AMOUNT = 1000;
    /** 没写音效时使用的默认音效：水遇岩浆熄灭。 */
    public static final SoundEvent DEFAULT_SOUND = SoundEvents.LAVA_EXTINGUISH;

    /**
     * 流体碰撞配方的编解码器。
     * <p>
     * 依次编解码流入流体原料、另一方流体原料、产物列表、延迟刻数、
     * 是否消耗流入流体、是否消耗另一方流体、是否消耗源方块、音效与下方条件；
     * 音效为可选字段，缺省时解析为 null。
     */
    public static final MapCodec<FluidCollisionRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    CollisionCodecs.FLUID_INGREDIENT.fieldOf("fluid").forGetter(FluidCollisionRecipe::fluid),
                    CollisionCodecs.FLUID_INGREDIENT.fieldOf("other_fluid").forGetter(FluidCollisionRecipe::otherFluid),
                    CollisionOutput.CODEC.listOf().fieldOf("results").forGetter(FluidCollisionRecipe::results),
                    Codec.INT.optionalFieldOf("delay", DEFAULT_DELAY).forGetter(FluidCollisionRecipe::delay),
                    Codec.BOOL.optionalFieldOf("consume_fluid", false).forGetter(FluidCollisionRecipe::consumeFluid),
                    Codec.BOOL.optionalFieldOf("consume_other", false).forGetter(FluidCollisionRecipe::consumeOther),
                    Codec.BOOL.optionalFieldOf("consume_source", false).forGetter(FluidCollisionRecipe::consumeSource),
                    BuiltInRegistries.SOUND_EVENT.byNameCodec().optionalFieldOf("sound").forGetter(recipe -> Optional.ofNullable(recipe.sound())),
                    BelowCondition.MAP_CODEC.forGetter(FluidCollisionRecipe::below)
            ).apply(instance, (fluid, otherFluid, results, delay, consumeFluid, consumeOther, consumeSource, sound, below) ->
                    new FluidCollisionRecipe(fluid, otherFluid, results, delay, consumeFluid, consumeOther,
                            consumeSource, sound.orElse(null), below)
            )
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidCollisionRecipe> STREAM_CODEC = new StreamCodec<>() {

        /**
         * 写出配方。
         *
         * @param buffer 目标缓冲区
         * @param recipe 配方
         */
        @Override
        public void encode(RegistryFriendlyByteBuf buffer, FluidCollisionRecipe recipe) {
            FluidIngredient.STREAM_CODEC.encode(buffer, recipe.fluid());
            FluidIngredient.STREAM_CODEC.encode(buffer, recipe.otherFluid());
            buffer.writeVarInt(recipe.results().size());
            for (CollisionOutput output : recipe.results()) {
                CollisionOutput.STREAM_CODEC.encode(buffer, output);
            }
            buffer.writeVarInt(recipe.delay());
            buffer.writeBoolean(recipe.consumeFluid());
            buffer.writeBoolean(recipe.consumeOther());
            buffer.writeBoolean(recipe.consumeSource());
            buffer.writeBoolean(recipe.sound() != null);
            if (recipe.sound() != null) {
                buffer.writeResourceLocation(Objects.requireNonNull(BuiltInRegistries.SOUND_EVENT.getKey(recipe.sound())));
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
        public FluidCollisionRecipe decode(RegistryFriendlyByteBuf buffer) {
            FluidIngredient fluid = FluidIngredient.STREAM_CODEC.decode(buffer);
            FluidIngredient otherFluid = FluidIngredient.STREAM_CODEC.decode(buffer);
            int size = buffer.readVarInt();
            List<CollisionOutput> results = new java.util.ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                results.add(CollisionOutput.STREAM_CODEC.decode(buffer));
            }
            int delay = buffer.readVarInt();
            boolean consumeFluid = buffer.readBoolean();
            boolean consumeOther = buffer.readBoolean();
            boolean consumeSource = buffer.readBoolean();
            SoundEvent sound = buffer.readBoolean() ? BuiltInRegistries.SOUND_EVENT.get(buffer.readResourceLocation()) : null;
            BelowCondition below = BelowCondition.STREAM_CODEC.decode(buffer);
            return new FluidCollisionRecipe(fluid, otherFluid, results, delay, consumeFluid, consumeOther, consumeSource, sound, below);
        }
    };

    /**
     * 数据包读取时的构造器。
     *
     * @param fluid        流入的流体
     * @param otherFluid   被碰撞的流体
     * @param results      产出
     * @param delay        延迟
     * @param consumeFluid 是否消耗流入流体
     * @param consumeOther  是否消耗被碰撞流体
     * @param sound         音效（可选）
     * @param consumeSource 是否消耗流体源头
     */
    public FluidCollisionRecipe(FluidIngredient fluid, FluidIngredient otherFluid, List<CollisionOutput> results,
                                int delay, boolean consumeFluid, boolean consumeOther, boolean consumeSource,
                                Optional<SoundEvent> sound) {
        this(fluid, otherFluid, results, delay, consumeFluid, consumeOther, consumeSource, sound.orElse(null), BelowCondition.NONE);
    }


    /**
     * 带上“是否消耗源头”、音效直接写 SoundEvent 的便捷构造器。
     *
     * @param fluid         流入的流体
     * @param otherFluid    被碰撞的流体
     * @param results       产出
     * @param delay         延迟
     * @param consumeFluid  是否消耗流入流体
     * @param consumeOther  是否消耗被碰撞流体
     * @param consumeSource 是否消耗流体源头
     * @param sound         音效（可选）
     */
    public FluidCollisionRecipe(FluidIngredient fluid, FluidIngredient otherFluid, List<CollisionOutput> results,
                                int delay, boolean consumeFluid, boolean consumeOther, boolean consumeSource,
                                @Nullable SoundEvent sound) {
        this(fluid, otherFluid, results, delay, consumeFluid, consumeOther, consumeSource, sound, BelowCondition.NONE);
    }

    /**
     * 不带“消耗源头”的便捷构造器。
     *
     * @param fluid        流入的流体
     * @param otherFluid   被碰撞的流体
     * @param results      产出
     * @param delay        延迟
     * @param consumeFluid 是否消耗流入流体
     * @param consumeOther 是否消耗被碰撞流体
     * @param sound        音效（可选）
     */
    public FluidCollisionRecipe(FluidIngredient fluid, FluidIngredient otherFluid, List<CollisionOutput> results,
                                int delay, boolean consumeFluid, boolean consumeOther, @Nullable SoundEvent sound) {
        this(fluid, otherFluid, results, delay, consumeFluid, consumeOther, false, sound, BelowCondition.NONE);
    }

    /**
     * 只关心“是否消耗源头”的便捷构造器，音效用默认值。
     *
     * @param fluid         流入的流体
     * @param otherFluid    被碰撞的流体
     * @param results       产出
     * @param delay         延迟
     * @param consumeFluid  是否消耗流入流体
     * @param consumeOther  是否消耗被碰撞流体
     * @param consumeSource 是否消耗流体源头
     */
    public FluidCollisionRecipe(FluidIngredient fluid, FluidIngredient otherFluid, List<CollisionOutput> results,
                                int delay, boolean consumeFluid, boolean consumeOther, boolean consumeSource) {
        this(fluid, otherFluid, results, delay, consumeFluid, consumeOther, consumeSource, DEFAULT_SOUND, BelowCondition.NONE);
    }

    /**
     * 不带“消耗源头”的便捷构造器（音效用 Optional 表示）。
     *
     * @param fluid        流入的流体
     * @param otherFluid   被碰撞的流体
     * @param results      产出
     * @param delay        延迟
     * @param consumeFluid 是否消耗流入流体
     * @param consumeOther 是否消耗被碰撞流体
     * @param sound        音效（可选）
     */
    public FluidCollisionRecipe(FluidIngredient fluid, FluidIngredient otherFluid, List<CollisionOutput> results,
                                int delay, boolean consumeFluid, boolean consumeOther, Optional<SoundEvent> sound) {
        this(fluid, otherFluid, results, delay, consumeFluid, consumeOther, false, sound.orElse(null), BelowCondition.NONE);
    }

    /**
     * 校验配方内容。
     */
    public FluidCollisionRecipe {
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
     * 判断某个流体状态是否满足“被碰撞的流体”条件。
     *
     * @param state 流体状态
     * @return 满足返回 true
     */
    public boolean testOtherFluid(FluidState state) {
        return FluidStates.matches(otherFluid, state);
    }

    /**
     * 取“流入的流体”里包含的代表流体，用于 JEI 展示。
     *
     * @return 流体数组
     */
    public Fluid[] fluidTypes() {
        return toTypes(fluid);
    }

    /**
     * 取“被碰撞的流体”里包含的代表流体，用于 JEI 展示。
     *
     * @return 流体数组
     */
    public Fluid[] otherFluidTypes() {
        return toTypes(otherFluid);
    }

    /**
     * 把流体原料展开成流体类型数组。
     *
     * @param ingredient 流体原料
     * @return 流体类型数组
     */
    private static Fluid[] toTypes(FluidIngredient ingredient) {
        return java.util.Arrays.stream(ingredient.getStacks())
                .map(FluidStack::getFluid)
                .distinct()
                .toArray(Fluid[]::new);
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
     * @return 流体碰撞配方序列化器
     */
    @Override
    public RecipeSerializer<?> getSerializer() {
        return RCLRecipes.FLUID_COLLISION_SERIALIZER.get();
    }

    /**
     * 获取配方类型。
     *
     * @return 流体碰撞配方类型
     */
    @Override
    public RecipeType<?> getType() {
        return RCLRecipes.FLUID_COLLISION_TYPE;
    }
}
