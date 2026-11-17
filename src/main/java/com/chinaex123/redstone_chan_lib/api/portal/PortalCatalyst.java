package com.chinaex123.redstone_chan_lib.api.portal;

import com.chinaex123.redstone_chan_lib.util.PortalTexts;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.Nullable;

/**
 * 传送门激活媒介。
 * <p>
 * 描述点亮传送门所需的激活方式：空手、指定物品、物品标签、
 * 指定流体或流体标签。提供匹配判断、激活后的消耗逻辑，
 * 以及从字符串解析媒介定义的能力。
 */
public final class PortalCatalyst {

    /** 激活方式种类 */
    public enum Kind {
        /** 空手 */
        EMPTY,
        /** 指定物品 */
        ITEM,
        /** 物品标签 */
        ITEM_TAG,
        /** 指定流体 */
        FLUID,
        /** 流体标签 */
        FLUID_TAG
    }

    /** 激活方式种类。 */
    private final Kind kind;

    /** 指定物品，非物品类型时为 null。 */
    @Nullable
    private final Item item;

    /** 物品标签，非物品标签类型时为 null。 */
    @Nullable
    private final TagKey<Item> itemTag;

    /** 指定流体，非流体类型时为 null。 */
    @Nullable
    private final Fluid fluid;

    /** 流体标签，非流体标签类型时为 null。 */
    @Nullable
    private final TagKey<Fluid> fluidTag;

    /**
     * 私有构造函数，创建激活媒介。
     *
     * @param kind     激活方式种类
     * @param item     指定物品
     * @param itemTag  物品标签
     * @param fluid    指定流体
     * @param fluidTag 流体标签
     */
    private PortalCatalyst(Kind kind, @Nullable Item item, @Nullable TagKey<Item> itemTag,
                           @Nullable Fluid fluid, @Nullable TagKey<Fluid> fluidTag) {
        this.kind = kind;
        this.item = item;
        this.itemTag = itemTag;
        this.fluid = fluid;
        this.fluidTag = fluidTag;
    }

    /**
     * 创建空手激活媒介。
     *
     * @return 空手媒介
     */
    public static PortalCatalyst empty() {
        return new PortalCatalyst(Kind.EMPTY, null, null, null, null);
    }

    /**
     * 创建指定物品激活媒介。
     *
     * @param item 物品
     * @return 物品媒介
     */
    public static PortalCatalyst item(Item item) {
        return new PortalCatalyst(Kind.ITEM, item, null, null, null);
    }

    /**
     * 创建物品标签激活媒介。
     *
     * @param tag 物品标签
     * @return 物品标签媒介
     */
    public static PortalCatalyst itemTag(TagKey<Item> tag) {
        return new PortalCatalyst(Kind.ITEM_TAG, null, tag, null, null);
    }

    /**
     * 创建指定流体激活媒介。
     *
     * @param fluid 流体
     * @return 流体媒介
     */
    public static PortalCatalyst fluid(Fluid fluid) {
        return new PortalCatalyst(Kind.FLUID, null, null, fluid, null);
    }

    /**
     * 创建流体标签激活媒介。
     *
     * @param tag 流体标签
     * @return 流体标签媒介
     */
    public static PortalCatalyst fluidTag(TagKey<Fluid> tag) {
        return new PortalCatalyst(Kind.FLUID_TAG, null, null, null, tag);
    }

    /**
     * 获取激活方式种类。
     *
     * @return 激活方式种类
     */
    public Kind kind() {
        return this.kind;
    }

    /**
     * 判断是否为流体类媒介。
     *
     * @return 是否为流体类媒介
     */
    public boolean isFluid() {
        return this.kind == Kind.FLUID || this.kind == Kind.FLUID_TAG;
    }

    /**
     * 判断物品堆叠是否匹配本媒介。
     * <p>
     * 空手要求堆叠为空；物品与物品标签按堆叠判断；
     * 流体与流体标签则读取堆叠内的流体内容进行判断。
     *
     * @param stack 物品堆叠
     * @return 是否匹配
     */
    public boolean matches(ItemStack stack) {
        return switch (this.kind) {
            case EMPTY -> stack.isEmpty();
            case ITEM -> !stack.isEmpty() && stack.is(this.item);
            case ITEM_TAG -> !stack.isEmpty() && stack.is(this.itemTag);
            case FLUID -> {
                FluidStack content = fluidOf(stack);
                yield !content.isEmpty() && content.is(this.fluid);
            }
            case FLUID_TAG -> {
                FluidStack content = fluidOf(stack);
                yield !content.isEmpty() && content.is(this.fluidTag);
            }
        };
    }

    /**
     * 获取物品堆叠中的流体内容。
     * <p>
     * 空堆叠或没有流体能力时返回空流体。
     *
     * @param stack 物品堆叠
     * @return 流体内容
     */
    public static FluidStack fluidOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return FluidStack.EMPTY;
        }
        IFluidHandlerItem handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null) {
            return FluidStack.EMPTY;
        }
        return handler.getFluidInTank(0);
    }

    /**
     * 激活成功后消耗媒介。
     * <p>
     * 空手或创造模式不消耗；流体类媒介抽取一桶流体并处理容器返还；
     * 有耐久的物品只扣 1 点耐久，无耐久物品直接消耗一个。
     *
     * @param player 玩家
     * @param hand   交互手
     * @param stack  物品堆叠
     */
    public void consume(Player player, InteractionHand hand, ItemStack stack) {
        if (this.kind == Kind.EMPTY || player.isCreative()) {
            return;
        }
        if (this.isFluid()) {
            this.consumeFluid(player, hand, stack);
            return;
        }
        if (stack.isDamageableItem()) {
            stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        } else {
            stack.shrink(1);
        }
    }

    /**
     * 消耗流体类媒介。
     * <p>
     * 复制单个物品读取其流体能力并抽取一桶，
     * 再将容器返还：手持仅一个时直接替换，多个时消耗一个并添加容器。
     *
     * @param player 玩家
     * @param hand   交互手
     * @param stack  物品堆叠
     */
    private void consumeFluid(Player player, InteractionHand hand, ItemStack stack) {
        ItemStack single = stack.copyWithCount(1);
        IFluidHandlerItem handler = single.getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null) {
            return;
        }
        FluidStack drained = handler.drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return;
        }
        ItemStack container = handler.getContainer();
        if (stack.getCount() <= 1) {
            player.setItemInHand(hand, container);
        } else {
            stack.shrink(1);
            if (!container.isEmpty()) {
                player.addItem(container);
            }
        }
    }

    /**
     * 获取媒介的字符串表示。
     *
     * @return 字符串表示
     */
    @Override
    public String toString() {
        return switch (this.kind) {
            case EMPTY -> "PortalCatalyst[empty]";
            case ITEM -> "PortalCatalyst[item:" + BuiltInRegistries.ITEM.getKey(this.item) + "]";
            case ITEM_TAG -> "PortalCatalyst[item:#" + this.itemTag.location() + "]";
            case FLUID -> "PortalCatalyst[fluid:" + BuiltInRegistries.FLUID.getKey(this.fluid) + "]";
            case FLUID_TAG -> "PortalCatalyst[fluid:#" + this.fluidTag.location() + "]";
        };
    }

    /**
     * 从字符串解析激活媒介。
     * <p>
     * 空字符串表示空手；支持可选的「#」标签前缀与「item:」「fluid:」类型前缀，
     * 物品与流体需已注册，否则抛出异常。
     *
     * @param value 字符串定义
     * @return 激活媒介
     */
    public static PortalCatalyst parse(String value) {
        String text = value.trim();
        if (text.isEmpty()) {
            return empty();
        }

        boolean tag = false;
        if (text.startsWith("#")) {
            tag = true;
            text = text.substring(1);
        }

        String type = "item";
        int colon = text.indexOf(':');
        if (colon > 0) {
            String prefix = text.substring(0, colon);
            if (prefix.equals("item") || prefix.equals("fluid")) {
                type = prefix;
                text = text.substring(colon + 1);
            }
        }
        if (text.startsWith("#")) {
            tag = true;
            text = text.substring(1);
        }
        if (text.isEmpty()) {
            throw new IllegalArgumentException(PortalTexts.text("missing_catalyst_id", value));
        }

        ResourceLocation id = ResourceLocation.parse(text);
        if (type.equals("fluid")) {
            if (tag) {
                return fluidTag(TagKey.create(Registries.FLUID, id));
            }
            Fluid fluid = BuiltInRegistries.FLUID.getOptional(id)
                    .orElseThrow(() -> new IllegalArgumentException(PortalTexts.text("unknown_fluid", value)));
            return fluid(fluid);
        }
        if (tag) {
            return itemTag(TagKey.create(Registries.ITEM, id));
        }
        Item item = BuiltInRegistries.ITEM.getOptional(id)
                .orElseThrow(() -> new IllegalArgumentException(PortalTexts.text("unknown_item", value)));
        return item(item);
    }
}
