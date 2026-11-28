package com.chinaex123.redstone_chan_lib.client.fluid;

import com.chinaex123.redstone_chan_lib.api.fluid.FluidDefinition;
import com.chinaex123.redstone_chan_lib.api.fluid.FluidEntry;
import com.chinaex123.redstone_chan_lib.api.fluid.FluidRegistrar;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;

/**
 * 流体客户端初始化。
 * <p>
 * 将流体相关的客户端监听挂到调用方的模组总线上：
 * 注册流体类型的客户端扩展（贴图、着色与雾气）、
 * 桶物品的动态流体容器着色，以及按半透明配置设置流体方块的渲染层。
 */
public final class FluidClientSetup {

    private FluidClientSetup() {
    }

    /**
     * 把客户端监听挂到调用方的模组总线上。
     *
     * @param modEventBus 调用方的模组总线
     * @param registrar   流体注册器
     */
    public static void attach(IEventBus modEventBus, FluidRegistrar registrar) {
        modEventBus.addListener(RegisterClientExtensionsEvent.class, event -> registerExtensions(event, registrar));
        modEventBus.addListener(RegisterColorHandlersEvent.Item.class, event -> registerItemColors(event, registrar));
        modEventBus.addListener(FMLClientSetupEvent.class,
                event -> event.enqueueWork(() -> setRenderLayers(registrar)));
    }

    /**
     * 注册流体类型的客户端扩展。
     * <p>
     * 按流体描述为每个流体类型注册静动贴图、着色、雾气颜色与距离，
     * 以及雾气是否禁用。
     *
     * @param event     客户端扩展注册事件
     * @param registrar 流体注册器
     */
    private static void registerExtensions(RegisterClientExtensionsEvent event, FluidRegistrar registrar) {
        for (FluidEntry entry : registrar.entries()) {
            FluidDefinition definition = entry.definition();
            event.registerFluidType(new SimpleFluidExtension(
                    definition.stillTexture(),
                    definition.flowingTexture(),
                    definition.tintColor(),
                    definition.fogColor(),
                    definition.fogDistance(),
                    !definition.fogEnabled()), entry.type());
        }
    }

    /**
     * 注册桶物品的着色处理器。
     * <p>
     * 为每个注册了桶的流体条目使用动态流体容器模型着色。
     *
     * @param event     物品着色注册事件
     * @param registrar 流体注册器
     */
    private static void registerItemColors(RegisterColorHandlersEvent.Item event, FluidRegistrar registrar) {
        ItemColor color = new DynamicFluidContainerModel.Colors();
        for (FluidEntry entry : registrar.entries()) {
            if (entry.bucket() != null) {
                event.register(color, entry.bucket());
            }
        }
    }

    /**
     * 设置流体方块的渲染层。
     * <p>
     * 按流体描述的半透明配置，为源流体与流动流体选择半透明或实心渲染层。
     *
     * @param registrar 流体注册器
     */
    private static void setRenderLayers(FluidRegistrar registrar) {
        for (FluidEntry entry : registrar.entries()) {
            RenderType layer = entry.definition().translucent() ? RenderType.translucent() : RenderType.solid();
            ItemBlockRenderTypes.setRenderLayer(entry.sourceFluid(), layer);
            ItemBlockRenderTypes.setRenderLayer(entry.flowingFluid(), layer);
        }
    }
}
