package com.chinaex123.redstone_chan_lib.mixin.client;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Collection;

/**
 * 创造模式物品栏界面的调用器。
 * <p>
 * 供分区按钮点击后“用新的物品集合刷新当前页签”。
 */
@Mixin(CreativeModeInventoryScreen.class)
public interface CreativeModeInventoryScreenInvoker {

    /**
     * 用给定物品刷新当前页签的列表。
     *
     * @param items 物品集合
     */
    @Invoker("refreshCurrentTabContents")
    void rcl$refreshCurrentTabContents(Collection<ItemStack> items);
}
