package com.chinaex123.redstone_chan_lib.init;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import com.chinaex123.redstone_chan_lib.api.fluid.FluidDefinition;
import com.chinaex123.redstone_chan_lib.api.fluid.FluidEntry;
import com.chinaex123.redstone_chan_lib.api.fluid.FluidKind;
import com.chinaex123.redstone_chan_lib.api.fluid.FluidRegistrar;
import net.neoforged.bus.api.IEventBus;

/**
 * 本模组自带的流体与气体示例。
 * <p>
 * 这里同时也是 {@link FluidRegistrar} 的用法示例：一份液体加一份气体，
 * 参数都写在链式调用里，客户端表现由注册器自动接好。
 * 想看效果的话，创造模式物品栏里的“流体示例”分区能拿到两只桶。
 */
public final class RCLDemoFluids {

    /**
     * 液态红石：偏稠、会发光、在水里游不动。
     */
    public static FluidEntry LIQUID_REDSTONE;

    /**
     * 红石气体：密度为负，会往天上飘，飘一会儿自己散掉。
     */
    public static FluidEntry REDSTONE_GAS;

    /**
     * 红石蒸汽：参数和气体差不多，但桶右键是直接散成粒子，不会留下方块。
     */
    public static FluidEntry REDSTONE_VAPOR;

    private static FluidRegistrar registrar;

    private RCLDemoFluids() {
    }

    /**
     * 注册示例流体。
     *
     * @param modEventBus 本模组的模组总线
     */
    public static void register(IEventBus modEventBus) {
        if (registrar != null) {
            return;
        }
        registrar = FluidRegistrar.create(RedstoneChanLIB.MODID, modEventBus);

        // 流体
        LIQUID_REDSTONE = registrar.register(FluidDefinition.builder("liquid_redstone")
                .tintColor(0xFFFF3A3A)
                .fogColor(0x8C0E0E)
                .fogDistance(8.0F)
                .density(1600)
                .viscosity(1800)
                .temperature(320)
                .tickRate(12)
                .slopeFindDistance(2)
                .levelDecreasePerBlock(2)
                .lightLevel(10)
                .blockLightLevel(10)
                .canSwim(false)
                .motionScale(0.006)
                .build());

        // 气体，普通气体桶，不放出方块
        REDSTONE_VAPOR = registrar.register(FluidDefinition.builder("redstone_vapor")
                .kind(FluidKind.GAS)
                .textures(
                        RedstoneChanLIB.id("block/redstone_gas_still"),
                        RedstoneChanLIB.id("block/redstone_gas_flow")
                )
                .tintColor(0x80FFD0D0)
                .fogColor(0xD08080)
                .fogDistance(4.0F)
                .density(-400)
                .viscosity(100)
                .temperature(300)
                .tickRate(8)
                .gasDissipateTicks(100)
                .canDrown(false)
                .canSwim(false)
                .supportsBoating(false)
                .canExtinguish(false)
                .gasBurst(true)
                .build());

        // 气体，普通气体桶，右键会倒出气体方块
        REDSTONE_GAS = registrar.register(FluidDefinition.builder("redstone_gas")
                .kind(FluidKind.GAS)
                .tintColor(0xB0FFB4B4)
                .fogColor(0xC06060)
                .fogDistance(4.0F)
                .density(-800)
                .viscosity(200)
                .temperature(280)
                .tickRate(6)
                .slopeFindDistance(6)
                .levelDecreasePerBlock(1)
                .gasDissipateTicks(160)
                .canDrown(false)
                .canSwim(false)
                .supportsBoating(false)
                .canExtinguish(false)
                .translucent(true)
                .build());
    }

    /** @return 示例流体注册器 */
    public static FluidRegistrar registrar() {
        return registrar;
    }
}
