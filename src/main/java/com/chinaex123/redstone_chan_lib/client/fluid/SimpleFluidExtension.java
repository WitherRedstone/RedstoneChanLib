package com.chinaex123.redstone_chan_lib.client.fluid;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

/**
 * 简易流体类型客户端扩展。
 * <p>
 * 为流体提供静止与流动贴图、着色与雾气表现：
 * 可配置雾气颜色与最远距离，或在不需要雾气时保持原样；
 * 同时提供若干基于默认值的便捷构造方式。
 */
public class SimpleFluidExtension implements IClientFluidTypeExtensions {

    /** 静止贴图。 */
    private final ResourceLocation stillTexture;

    /** 流动贴图。 */
    private final ResourceLocation flowingTexture;

    /** 着色颜色（ARGB）。 */
    private final int tintColor;

    /** 雾气颜色（RGB）。 */
    private final int fogColor;

    /** 雾气最远距离。 */
    private final float fogDistance;

    /** 是否禁用雾气。 */
    private final boolean noFog;

    /**
     * 构造流体客户端扩展。
     *
     * @param stillTexture   静止贴图
     * @param flowingTexture 流动贴图
     * @param tintColor      着色颜色（ARGB）
     * @param fogColor       雾气颜色（RGB）
     * @param fogDistance    雾气最远距离
     * @param noFog          是否禁用雾气
     */
    public SimpleFluidExtension(ResourceLocation stillTexture, ResourceLocation flowingTexture,
                                int tintColor, int fogColor, float fogDistance, boolean noFog) {
        this.stillTexture = stillTexture;
        this.flowingTexture = flowingTexture;
        this.tintColor = tintColor;
        this.fogColor = fogColor;
        this.fogDistance = fogDistance;
        this.noFog = noFog;
    }

    /**
     * 以雾气颜色推导着色颜色的构造方式。
     * <p>
     * 着色颜色取不透明的雾气颜色，并启用雾气。
     *
     * @param stillTexture   静止贴图
     * @param flowingTexture 流动贴图
     * @param fogColor       雾气颜色（RGB）
     * @param fogDistance    雾气最远距离
     */
    public SimpleFluidExtension(ResourceLocation stillTexture, ResourceLocation flowingTexture,
                                int fogColor, float fogDistance) {
        this(stillTexture, flowingTexture, 0xFF000000 | (fogColor & 0xFFFFFF), fogColor, fogDistance, false);
    }

    /**
     * 使用默认着色与雾气的构造方式。
     * <p>
     * 不着色（白色），并禁用雾气。
     *
     * @param stillTexture   静止贴图
     * @param flowingTexture 流动贴图
     */
    public SimpleFluidExtension(ResourceLocation stillTexture, ResourceLocation flowingTexture) {
        this(stillTexture, flowingTexture, 0xFFFFFFFF, 0xFFFFFF, 96.0F, true);
    }

    /**
     * 获取静止贴图。
     *
     * @return 静止贴图
     */
    @Override
    public ResourceLocation getStillTexture() {
        return this.stillTexture;
    }

    /**
     * 获取流动贴图。
     *
     * @return 流动贴图
     */
    @Override
    public ResourceLocation getFlowingTexture() {
        return this.flowingTexture;
    }

    /**
     * 获取着色颜色。
     *
     * @return 着色颜色（ARGB）
     */
    @Override
    public int getTintColor() {
        return this.tintColor;
    }

    /**
     * 修改雾气颜色。
     * <p>
     * 禁用雾气时保持原值；否则按配置的雾气颜色返回。
     *
     * @param camera            相机
     * @param partialTick       插值刻
     * @param level             客户端世界
     * @param renderDistance    渲染距离
     * @param darkenWorldAmount 世界变暗量
     * @param fluidFogColor     原流体雾气颜色
     * @return 修改后的雾气颜色
     */
    @Override
    public @NotNull Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level,
                                            int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
        if (this.noFog) {
            return fluidFogColor;
        }
        float red = ((this.fogColor >> 16) & 0xFF) / 255.0F;
        float green = ((this.fogColor >> 8) & 0xFF) / 255.0F;
        float blue = (this.fogColor & 0xFF) / 255.0F;
        return new Vector3f(red, green, blue);
    }

    /**
     * 修改雾气渲染参数。
     * <p>
     * 禁用雾气或处于旁观者模式时不处理；否则设置雾气的起止距离与颜色。
     *
     * @param camera        相机
     * @param mode          雾气模式
     * @param renderDistance 渲染距离
     * @param partialTick   插值刻
     * @param nearDistance  原起始距离
     * @param farDistance   原结束距离
     * @param shape         雾气形状
     */
    @Override
    public void modifyFogRender(Camera camera, FogRenderer.FogMode mode, float renderDistance,
                                float partialTick, float nearDistance, float farDistance, FogShape shape) {
        if (this.noFog) {
            return;
        }
        if (camera.getEntity().isSpectator()) {
            return;
        }
        RenderSystem.setShaderFogStart(0.0F);
        RenderSystem.setShaderFogEnd(this.fogDistance);
        float red = ((this.fogColor >> 16) & 0xFF) / 255.0F;
        float green = ((this.fogColor >> 8) & 0xFF) / 255.0F;
        float blue = (this.fogColor & 0xFF) / 255.0F;
        RenderSystem.setShaderFogColor(red, green, blue);
    }
}
