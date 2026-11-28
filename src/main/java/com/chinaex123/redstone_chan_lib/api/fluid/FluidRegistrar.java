package com.chinaex123.redstone_chan_lib.api.fluid;

import com.chinaex123.redstone_chan_lib.client.fluid.FluidClientSetup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 流体与气体注册器。
 * <p>
 * 别的模组想加流体，只需要在构造里写：
 * <pre>{@code
 * FluidRegistrar registrar = FluidRegistrar.create(MODID, modEventBus);
 * FluidEntry mana = registrar.register(FluidDefinition.builder("liquid_mana")
 *         .tintColor(0xFF3080FF)
 *         .fogColor(0x3080FF)
 *         .fogDistance(24.0F)
 *         .build());
 * }</pre>
 * 一次调用会把流体类型、源流体、流动流体、流体方块、桶物品全部注册好，
 * 顺手把客户端的贴图、着色和雾气也接上，不用再单独写客户端代码。
 * <p>
 * 资源文件仍然要自己准备：流体的静止与流动贴图、桶物品的模型。
 * 方块不需要 blockstate 文件，原版是靠流体渲染器画的。
 */
public final class FluidRegistrar {

    private final String namespace;
    private final DeferredRegister<FluidType> types;
    private final DeferredRegister<Fluid> fluids;
    private final DeferredRegister.Blocks blocks;
    private final DeferredRegister.Items items;
    private final List<FluidEntry> entries = new ArrayList<>();

    private FluidRegistrar(String namespace) {
        this.namespace = namespace;
        this.types = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, namespace);
        this.fluids = DeferredRegister.create(BuiltInRegistries.FLUID, namespace);
        this.blocks = DeferredRegister.createBlocks(namespace);
        this.items = DeferredRegister.createItems(namespace);
    }

    /**
     * 为某个模组创建一个注册器。
     * <p>
     * 传进来的事件总线要是调用方自己的模组总线，
     * 这样注册出来的东西才算在调用方名下。
     *
     * @param namespace   命名空间，一般就是模组 id
     * @param modEventBus 调用方的模组事件总线
     * @return 注册器
     */
    public static FluidRegistrar create(String namespace, IEventBus modEventBus) {
        FluidRegistrar registrar = new FluidRegistrar(namespace);
        registrar.attach(modEventBus);
        return registrar;
    }

    private void attach(IEventBus modEventBus) {
        this.types.register(modEventBus);
        this.fluids.register(modEventBus);
        this.blocks.register(modEventBus);
        this.items.register(modEventBus);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            FluidClientSetup.attach(modEventBus, this);
        }
    }

    /**
     * 注册一份流体。
     *
     * @param definition 流体描述
     * @return 注册结果
     */
    public FluidEntry register(FluidDefinition definition) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(this.namespace, definition.name());
        FluidBinding binding = new FluidBinding();
        binding.definition(definition);

        DeferredHolder<FluidType, FluidType> type = this.types.register(definition.name(),
                () -> new FluidType(definition.createTypeProperties(this.namespace)));
        binding.type(type);

        boolean gas = definition.kind() == FluidKind.GAS;

        // 方块和桶都要反过来引用流体，所以先建占位，等流体的 supplier 真正执行时引用已经齐了
        DeferredBlock<LiquidBlock> block = this.blocks.register(definition.name(), () -> {
            FlowingFluid fluid = (FlowingFluid) binding.source();
            return gas
                    ? new RCLGasBlock(fluid, definition.createBlockProperties(), definition.gasDissipateTicks())
                    : new RCLFluidBlock(fluid, definition.createBlockProperties());
        });
        binding.block(block);

        DeferredItem<Item> bucket = null;
        if (definition.createBucket()) {
            bucket = this.items.register(definition.name() + "_bucket", () -> {
                Item.Properties properties = new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET);
                return gas
                        ? new RCLGasBucketItem(binding.source(), properties, definition.gasBurst())
                        : new BucketItem(binding.source(), properties);
            });
            binding.bucket(bucket);
        }

        if (gas) {
            binding.flowing(this.fluids.<Fluid>register("flowing_" + definition.name(),
                    () -> new RCLGasFluid.Flowing(binding)));
            binding.source(this.fluids.<Fluid>register(definition.name(),
                    () -> new RCLGasFluid.Source(binding)));
        } else {
            binding.flowing(this.fluids.<Fluid>register("flowing_" + definition.name(),
                    () -> new BaseFlowingFluid.Flowing(definition.createFluidProperties(binding))));
            binding.source(this.fluids.<Fluid>register(definition.name(),
                    () -> new BaseFlowingFluid.Source(definition.createFluidProperties(binding))));
        }

        FluidEntry entry = new FluidEntry(id, definition, type, binding.sourceHolder(), binding.flowingHolder(),
                block, bucket);
        this.entries.add(entry);
        return entry;
    }

    /**
     * 注册多份流体。
     *
     * @param definitions 流体描述
     * @return 注册结果
     */
    public List<FluidEntry> register(FluidDefinition... definitions) {
        List<FluidEntry> result = new ArrayList<>(definitions.length);
        for (FluidDefinition definition : definitions) {
            result.add(this.register(definition));
        }
        return result;
    }

    /**
     * 取注册名。
     *
     * @param entry 注册结果
     * @return 完整注册名
     */
    public ResourceLocation idOf(FluidEntry entry) {
        return entry.id();
    }

    /** @return 命名空间 */
    public String namespace() {
        return this.namespace;
    }

    /** @return 已经注册的流体，按注册顺序 */
    public List<FluidEntry> entries() {
        return Collections.unmodifiableList(this.entries);
    }
}
