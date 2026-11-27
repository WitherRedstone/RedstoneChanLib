package com.chinaex123.redstone_chan_lib.mixin.client;

import com.chinaex123.redstone_chan_lib.api.creative.CreativeSection;
import com.chinaex123.redstone_chan_lib.api.creative.CreativeSectionStyle;
import com.chinaex123.redstone_chan_lib.api.creative.CreativeSections;
import com.chinaex123.redstone_chan_lib.client.creative.CreativeTabSectionButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 给带分区的创造模式物品栏加左侧按钮。
 * <p>
 * 切到带分区的页签时，在界面左侧按分区顺序生成一排按钮；点击按钮只显示该分区，
 * 按住 Shift 点击可以同时选中多个分区。点击后重建页签内容并刷新列表。
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin
        extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {

    /** 当前选中的创造模式页签。 */
    @Shadow
    private static CreativeModeTab selectedTab;

    /** 是否展示管理员物品（重建物品列表时要按同样的条件传参）。 */
    @Shadow
    @Final
    private boolean displayOperatorCreativeTab;

    /** 本界面创建的分区按钮。 */
    @Unique
    private final List<CreativeTabSectionButton> rcl$sectionButtons = new ArrayList<>();

    /**
     * 私有构造函数。
     * <p>
     * 仅用于满足父类构造要求，Mixin 不会实际调用。
     */
    private CreativeModeInventoryScreenMixin() {
        super(null, null, null);
    }

    /**
     * 切换页签时重建按钮。
     *
     * @param tab 切到的页签
     * @param ci  回调
     */
    @Inject(method = "selectTab", at = @At("HEAD"))
    private void rcl$rebuildSectionButtons(CreativeModeTab tab, CallbackInfo ci) {
        for (CreativeTabSectionButton button : this.rcl$sectionButtons) {
            this.removeWidget(button);
        }
        this.rcl$sectionButtons.clear();

        CreativeSections.Entry entry = CreativeSections.byTab(tab);
        if (entry == null) {
            return;
        }
        List<CreativeSection> sections = entry.sections();
        for (int i = 0; i < sections.size(); i++) {
            int index = i;
            CreativeSectionStyle style = CreativeSections.styleOf(entry, index);
            // 竖着往下排，一列放不下就换到左边一列
            int step = style.height() + 4;
            int x = this.leftPos - style.width() - 3 - (i / 6) * (style.width() + 3);
            int y = this.topPos + 10 + (i % 6) * step;
            CreativeTabSectionButton button = new CreativeTabSectionButton(x, y, tab, index, sections.get(i), style,
                    pressed -> this.rcl$onSectionPressed(index));
            this.rcl$sectionButtons.add(button);
            this.addRenderableWidget(button);
        }
    }

    /**
     * 点击分区按钮：Shift 多选，否则只选这一个。
     *
     * @param index 分区下标
     */
    @Unique
    private void rcl$onSectionPressed(int index) {
        CreativeModeTab tab = selectedTab;
        if (!CreativeSections.has(tab)) {
            return;
        }
        if (Screen.hasShiftDown()) {
            CreativeSections.toggleSection(tab, index);
        } else {
            CreativeSections.selectSection(tab, index);
        }
        Minecraft minecraft = this.minecraft;
        LocalPlayer player = minecraft == null ? null : minecraft.player;
        if (player == null) {
            return;
        }
        // 用新选中的分区重建页签内容，再让界面刷新列表
        CreativeModeTab.ItemDisplayParameters parameters = new CreativeModeTab.ItemDisplayParameters(
                player.connection.enabledFeatures(),
                player.canUseGameMasterBlocks() && this.displayOperatorCreativeTab,
                player.level().registryAccess());
        tab.buildContents(parameters);
        ((CreativeModeInventoryScreenInvoker) this).rcl$refreshCurrentTabContents(tab.getDisplayItems());
    }
}
