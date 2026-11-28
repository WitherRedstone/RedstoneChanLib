package com.chinaex123.redstone_chan_lib.compat.jei.category;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import com.chinaex123.redstone_chan_lib.api.collision.CollisionOutput;
import com.chinaex123.redstone_chan_lib.api.collision.FluidCollisionRecipe;
import com.chinaex123.redstone_chan_lib.compat.jei.FluidCollisionJeiRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.List;

/**
 * 流体碰撞流体配方的 JEI 分类。
 * <p>
 * 左边两个槽是参与碰撞的流体，中间一个箭头，右边是碰撞后可能出现的方块或物品；
 * 底下写延迟刻数与是否消耗流体。
 */
public class FluidCollisionCategory implements IRecipeCategory<FluidCollisionJeiRecipe> {

    /**
     * JEI 配方类型。
     */
    public static final RecipeType<FluidCollisionJeiRecipe> TYPE =
            RecipeType.create(RedstoneChanLIB.MODID, "fluid_collision", FluidCollisionJeiRecipe.class);

    /** 分类宽度。 */
    private static final int WIDTH = 176;
    /** 分类高度。 */
    private static final int HEIGHT = 72;
    /** 每行最多放几个产出。 */
    private static final int OUTPUTS_PER_ROW = 3;
    /** 第一种流体的左边距。 */
    private static final int FIRST_X = 4;
    /** 中间空格槽的左边距。 */
    private static final int MIDDLE_X = 24;
    /** 第二种流体的左边距。 */
    private static final int SECOND_X = 44;
    /** 箭头起点。 */
    private static final int ARROW_X = 66;
    /** 产出槽的左边距。 */
    private static final int OUTPUT_X = 94;
    /** 产出槽的上边距。 */
    private static final int OUTPUT_Y = 4;
    /** 槽间距。 */
    private static final int SLOT_STEP = 20;
    /** 条件槽所在行。 */
    private static final int CONDITION_Y = 26;
    /** 说明文字所在行。 */
    private static final int TEXT_Y = 48;
    /** 概率图标的边长，和贴图本身一样大，不做缩放。 */
    private static final int CHANCE_ICON_SIZE = 18;

    private final IGuiHelper guiHelper;
    /** 带概率的产出用的槽位底图。 */
    private final IDrawable chanceSlotBackground;

    /**
     * 构造分类。
     *
     * @param guiHelper JEI 提供的绘制工具
     */
    public FluidCollisionCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;
        ResourceLocation chanceTexture = RedstoneChanLIB.id("textures/gui/jei/drop_chance.png");
        this.chanceSlotBackground = guiHelper.drawableBuilder(chanceTexture, 0, 0, CHANCE_ICON_SIZE, CHANCE_ICON_SIZE)
                .setTextureSize(CHANCE_ICON_SIZE, CHANCE_ICON_SIZE)
                .build();
    }

    /**
     * 获取分类对应的配方类型。
     *
     * @return 配方类型
     */
    @Override
    public RecipeType<FluidCollisionJeiRecipe> getRecipeType() {
        return TYPE;
    }

    /**
     * 分类标题。
     *
     * @return 标题
     */
    @Override
    public Component getTitle() {
        return Component.translatable("jei.redstone_chan_lib.fluid_collision");
    }

    /**
     * 分类图标。
     *
     * @return 图标
     */
    @Override
    public IDrawable getIcon() {
        return guiHelper.createDrawableItemStack(new ItemStack(Items.WATER_BUCKET));
    }

    /**
     * 分类宽度。
     *
     * @return 宽度
     */
    @Override
    public int getWidth() {
        return WIDTH;
    }

    /**
     * 分类高度。
     *
     * @return 高度
     */
    @Override
    public int getHeight() {
        return HEIGHT;
    }

    /**
     * 配方在 JEI 里的来源 id。
     *
     * @param recipe 配方
     * @return 配方 id
     */
    @Override
    public ResourceLocation getRegistryName(FluidCollisionJeiRecipe recipe) {
        return recipe.id();
    }

    /**
     * 把方块集合转成用于展示的物品堆。
     *
     * @param set 方块集合
     * @return 物品堆列表
     */
    private static List<ItemStack> blockStacks(HolderSet<Block> set) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Holder<Block> holder : set) {
            Block block = holder.value();
            if (block.asItem() != Items.AIR) {
                stacks.add(new ItemStack(block.asItem()));
            }
        }
        return stacks;
    }
    /**
     * 摆放槽位。
     *
     * @param builder 布局构建器
     * @param wrapper 配方
     * @param focuses 当前聚焦的原料
     */
    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, FluidCollisionJeiRecipe wrapper, IFocusGroup focuses) {
        FluidCollisionRecipe recipe = wrapper.recipe();
        IDrawable slot = guiHelper.getSlotDrawable();

        var below = recipe.below();
        int firstX = FIRST_X;
        int secondX = SECOND_X;
        int outputX = OUTPUT_X;

        var first = builder.addSlot(RecipeIngredientRole.INPUT, firstX, 4).setBackground(slot, -1, -1);
        for (Fluid fluid : recipe.fluidTypes()) {
            first.addFluidStack(fluid, FluidCollisionRecipe.TEST_AMOUNT);
        }
        first.setFluidRenderer(FluidCollisionRecipe.TEST_AMOUNT, true, 16, 16);

        var second = builder.addSlot(RecipeIngredientRole.INPUT, secondX, 4).setBackground(slot, -1, -1);
        for (Fluid fluid : recipe.otherFluidTypes()) {
            second.addFluidStack(fluid, FluidCollisionRecipe.TEST_AMOUNT);
        }
        second.setFluidRenderer(FluidCollisionRecipe.TEST_AMOUNT, true, 16, 16);

        List<ItemStack> fluidBelow = below.fluid() == null ? List.of() : blockStacks(below.fluid());
        if (!fluidBelow.isEmpty()) {
            // fluid 看第一种流体自己脚下
            IRecipeSlotBuilder slotFluid = builder.addSlot(RecipeIngredientRole.CATALYST, firstX, CONDITION_Y)
                    .setBackground(slot, -1, -1)
                    .addItemStacks(fluidBelow);
            slotFluid.addRichTooltipCallback((slotView, tooltip) -> tooltip.add(
                    Component.translatable("jei.redstone_chan_lib.below_fluid").withStyle(ChatFormatting.GRAY)));
        }
        List<ItemStack> downBelow = below.down() == null ? List.of() : blockStacks(below.down());
        if (!downBelow.isEmpty()) {
            // down 看另一侧（右侧）下面那一格
            IRecipeSlotBuilder slotDown = builder.addSlot(RecipeIngredientRole.CATALYST, secondX, CONDITION_Y)
                    .setBackground(slot, -1, -1)
                    .addItemStacks(downBelow);
            slotDown.addRichTooltipCallback((slotView, tooltip) -> tooltip.add(
                    Component.translatable("jei.redstone_chan_lib.below_down").withStyle(ChatFormatting.GRAY)));
        }
        List<ItemStack> centerBelow = below.center() == null ? List.of() : blockStacks(below.center());
        if (!centerBelow.isEmpty()) {
            // center 看中间空格下面那一格
            IRecipeSlotBuilder slotCenter = builder.addSlot(RecipeIngredientRole.CATALYST, MIDDLE_X, CONDITION_Y)
                    .setBackground(slot, -1, -1)
                    .addItemStacks(centerBelow);
            slotCenter.addRichTooltipCallback((slotView, tooltip) -> tooltip.add(
                    Component.translatable("jei.redstone_chan_lib.below_center").withStyle(ChatFormatting.GRAY)));
        }

        int index = 0;
        for (CollisionOutput output : recipe.results()) {
            int x = outputX + (index % OUTPUTS_PER_ROW) * SLOT_STEP;
            int y = OUTPUT_Y + (index / OUTPUTS_PER_ROW) * SLOT_STEP;
            IRecipeSlotBuilder result = builder.addSlot(RecipeIngredientRole.OUTPUT, x, y);
            if (output.chance() < 1.0F) {
                // 概率不足 100% 时直接用概率贴图当槽位底图，贴图按原大小铺，不缩放
                result.setBackground(chanceSlotBackground, -1, -1);
                int percent = Math.round(output.chance() * 100.0F);
                result.addRichTooltipCallback((slotView, tooltip) -> tooltip.add(
                        Component.translatable("jei.redstone_chan_lib.chance", percent)
                                .withStyle(ChatFormatting.GOLD)));
            } else {
                result.setBackground(slot, -1, -1);
            }
            if (output.isBlock()) {
                Block block = output.block();
                if (block != null && block.asItem() != Items.AIR) {
                    result.addItemStack(new ItemStack(block.asItem(), output.count()));
                }
            } else if (output.stack() != null) {
                result.addItemStack(output.stack().copyWithCount(output.count()));
            }
            index++;
        }
    }

    /**
     * 画箭头与说明文字。
     *
     * @param recipe    配方
     * @param slotsView 槽位视图
     * @param graphics  绘制上下文
     * @param mouseX    鼠标 x
     * @param mouseY    鼠标 y
     */
    @Override
    public void draw(FluidCollisionJeiRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics graphics,
                     double mouseX, double mouseY) {
        guiHelper.getRecipeArrow().draw(graphics, ARROW_X, 4);
        int textY = TEXT_Y;
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.translatable("jei.redstone_chan_lib.delay", recipe.recipe().delay()),
                4, textY, 0xFF555555, false);
        if (recipe.recipe().consumeFluid() || recipe.recipe().consumeOther()) {
            graphics.drawString(font, Component.translatable("jei.redstone_chan_lib.consume"), 4, textY + 10,
                    0xFF555555, false);
        }
    }
}
