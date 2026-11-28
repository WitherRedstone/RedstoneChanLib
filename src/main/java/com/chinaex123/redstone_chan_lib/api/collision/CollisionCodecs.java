package com.chinaex123.redstone_chan_lib.api.collision;

import com.chinaex123.redstone_chan_lib.util.PortalTexts;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

import java.util.ArrayList;
import java.util.List;

/**
 * 碰撞配方编解码工具类。
 * <p>
 * 为方块碰撞配方的字段提供宽松的编解码器：
 * 流体原料与物品堆叠既接受简写字符串也接受完整编解码格式，
 * 方块集合既接受单个方块、方块列表的字符串写法，也接受完整的同质列表格式；
 * 解析失败时返回带本地化提示的错误结果。
 */
public final class CollisionCodecs {
    private CollisionCodecs() {
    }

    /**
     * 流体原料编解码器。
     * <p>
     * 左侧字符串写作支持「#标签」或具体流体 ID，右侧为完整的流体原料编解码格式。
     */
    public static final Codec<FluidIngredient> FLUID_INGREDIENT = Codec.either(Codec.STRING, FluidIngredient.CODEC)
            .comapFlatMap(
                    either -> either.map(CollisionCodecs::parseFluid, DataResult::success),
                    Either::right);

    /**
     * 方块集合编解码器。
     * <p>
     * 左侧字符串写作支持单个方块或方块列表（可含「#标签」），
     * 右侧为方块注册表的同质列表编解码格式。
     */
    public static final Codec<HolderSet<Block>> BLOCK_SET = Codec.either(
                    Codec.either(Codec.STRING, Codec.STRING.listOf()),
                    net.minecraft.core.RegistryCodecs.homogeneousList(Registries.BLOCK))
            .comapFlatMap(
                    either -> either.map(
                            loose -> loose.map(
                                    single -> parseBlockSet(List.of(single)),
                                    CollisionCodecs::parseBlockSet),
                            DataResult::success),
                    Either::right);

    /**
     * 物品堆叠编解码器。
     * <p>
     * 左侧字符串写作支持具体物品 ID，右侧为完整的物品堆叠编解码格式。
     */
    public static final Codec<ItemStack> ITEM_STACK = Codec.either(Codec.STRING, ItemStack.CODEC)
            .comapFlatMap(
                    either -> either.map(CollisionCodecs::parseItem, DataResult::success),
                    Either::right);

    /**
     * 解析流体原料字符串。
     * <p>
     * 「#」开头按流体标签解析，否则要求为已注册的流体。
     *
     * @param value 流体字符串
     * @return 流体原料，解析失败时返回错误结果
     */
    private static DataResult<FluidIngredient> parseFluid(String value) {
        if (value.startsWith("#")) {
            return ResourceLocation.read(value.substring(1))
                    .map(id -> FluidIngredient.tag(TagKey.create(Registries.FLUID, id)));
        }
        return ResourceLocation.read(value).flatMap(id -> {
            if (!BuiltInRegistries.FLUID.containsKey(id)) {
                return DataResult.error(() -> PortalTexts.text("collision_unknown_fluid", value));
            }
            return DataResult.success(FluidIngredient.of(BuiltInRegistries.FLUID.get(id)));
        });
    }

    /**
     * 解析方块集合字符串列表。
     * <p>
     * 「#」开头按方块标签展开为具体方块，否则要求为已注册的方块；
     * 结果为空、标签不存在或方块未注册时返回错误。
     *
     * @param values 方块字符串列表
     * @return 方块集合，解析失败时返回错误结果
     */
    private static DataResult<HolderSet<Block>> parseBlockSet(List<String> values) {
        List<Holder<Block>> holders = new ArrayList<>();
        for (String value : values) {
            if (value.startsWith("#")) {
                TagKey<Block> tag = TagKey.create(Registries.BLOCK, ResourceLocation.parse(value.substring(1)));
                HolderSet.Named<Block> named = BuiltInRegistries.BLOCK.getTag(tag).orElse(null);
                if (named == null) {
                    return DataResult.error(() -> PortalTexts.text("collision_unknown_block_tag", value));
                }
                named.forEach(holders::add);
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(value);
            if (id == null) {
                return DataResult.error(() -> PortalTexts.text("collision_unknown_block", value));
            }
            Holder<Block> holder = BuiltInRegistries.BLOCK.getHolder(id).orElse(null);
            if (holder == null) {
                return DataResult.error(() -> PortalTexts.text("collision_unknown_block", value));
            }
            holders.add(holder);
        }
        if (holders.isEmpty()) {
            return DataResult.error(() -> PortalTexts.text("collision_blocks_empty"));
        }
        return DataResult.success(HolderSet.direct(holders));
    }

    /**
     * 解析物品堆叠字符串。
     * <p>
     * 要求为已注册的物品，返回数量为 1 的物品堆叠。
     *
     * @param value 物品字符串
     * @return 物品堆叠，解析失败时返回错误结果
     */
    private static DataResult<ItemStack> parseItem(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) {
            return DataResult.error(() -> PortalTexts.text("collision_unknown_item", value));
        }
        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            return DataResult.error(() -> PortalTexts.text("collision_unknown_item", value));
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        return DataResult.success(new ItemStack(item));
    }
}
