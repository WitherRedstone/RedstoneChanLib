package com.chinaex123.redstone_chan_lib.data.portal;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import com.chinaex123.redstone_chan_lib.api.portal.PortalCatalyst;
import com.chinaex123.redstone_chan_lib.api.portal.PortalDefinition;
import com.chinaex123.redstone_chan_lib.api.portal.PortalDefinitions;
import com.chinaex123.redstone_chan_lib.api.portal.PortalOrientation;
import com.chinaex123.redstone_chan_lib.api.portal.PortalRitual;
import com.chinaex123.redstone_chan_lib.util.PortalTexts;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import java.util.HashMap;
import java.util.Map;

/**
 * 传送门定义数据包加载器。
 * <p>
 * 从数据包目录读取 JSON 定义并解析为传送门定义：
 * 支持框架方块与标签、激活方式（单个或数组）、目标维度、
 * 摆放类型、颜色、名称、尺寸、等待时间与仪式式激活配置。
 * 解析失败的条目会记录错误并跳过，全部解析完成后写入定义注册表。
 */
public class PortalDefinitionLoader extends SimpleJsonResourceReloadListener {

    /** 数据包中的定义目录。 */
    public static final String DIRECTORY = RedstoneChanLIB.MODID + "/portal";

    /** JSON 解析器。 */
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /**
     * 构造定义加载器。
     */
    public PortalDefinitionLoader() {
        super(GSON, DIRECTORY);
    }

    /**
     * 应用读取到的 JSON 数据。
     * <p>
     * 逐个解析定义，失败时记录错误；全部处理完后写入定义注册表。
     *
     * @param jsons           定义 ID 到 JSON 的映射
     * @param resourceManager 资源管理器
     * @param profiler        性能分析器
     */
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, PortalDefinition> parsed = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : jsons.entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                JsonObject json = GsonHelper.convertToJsonObject(entry.getValue(), "portal definition");
                parsed.put(id, this.parse(id, json));
            } catch (Exception exception) {
                RedstoneChanLIB.LOGGER.error(PortalTexts.text("definition_parse_failed", id), exception);
            }
        }
        PortalDefinitions.applyDatapack(parsed);
    }

    /**
     * 解析单个传送门定义。
     *
     * @param id   定义 ID
     * @param json 定义 JSON
     * @return 传送门定义
     */
    private PortalDefinition parse(ResourceLocation id, JsonObject json) {
        PortalDefinition.Builder builder = PortalDefinition.builder(id);

        // 外围方块：必须至少有一个
        JsonArray frames = GsonHelper.getAsJsonArray(json, "frame_blocks");
        for (JsonElement element : frames) {
            String value = element.getAsString();
            if (value.startsWith("#")) {
                builder.frameTag(TagKey.create(Registries.BLOCK, ResourceLocation.parse(value.substring(1))));
            } else {
                Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(value));
                builder.frameBlock(block);
            }
        }

        // 激活方式，可选。支持单个字符串，也支持字符串数组（任意一种匹配即可点亮）
        if (json.has("catalyst")) {
            JsonElement catalystElement = json.get("catalyst");
            if (catalystElement.isJsonArray()) {
                for (JsonElement element : catalystElement.getAsJsonArray()) {
                    builder.catalyst(PortalCatalyst.parse(element.getAsString()));
                }
            } else {
                builder.catalyst(PortalCatalyst.parse(catalystElement.getAsString()));
            }
        }

        // 目标维度，可选
        if (json.has("dimension")) {
            ResourceLocation dimension = ResourceLocation.parse(GsonHelper.getAsString(json, "dimension"));
            builder.dimension(ResourceKey.create(Registries.DIMENSION, dimension));
        } else {
            builder.dimension(Level.NETHER);
        }

        // 摆放类型，可选：any（竖横都行，默认）、vertical（只有竖直的门）、horizontal（只有平铺的门）
        if (json.has("orientation")) {
            builder.orientation(PortalOrientation.parse(GsonHelper.getAsString(json, "orientation")));
        }

        // 颜色，可选
        if (json.has("color")) {
            builder.color(parseColor(GsonHelper.getAsString(json, "color")));
        }

        // 名称，可选，翻译键优先
        if (json.has("translate")) {
            builder.name(Component.translatable(GsonHelper.getAsString(json, "translate")));
        } else if (json.has("name")) {
            builder.name(Component.literal(GsonHelper.getAsString(json, "name")));
        }

        // 尺寸与等待时间，可选
        int minWidth = GsonHelper.getAsInt(json, "min_width", PortalDefinition.DEFAULT_MIN_WIDTH);
        int minHeight = GsonHelper.getAsInt(json, "min_height", PortalDefinition.DEFAULT_MIN_HEIGHT);
        int maxWidth = GsonHelper.getAsInt(json, "max_width", PortalDefinition.DEFAULT_MAX_WIDTH);
        int maxHeight = GsonHelper.getAsInt(json, "max_height", PortalDefinition.DEFAULT_MAX_HEIGHT);
        builder.size(minWidth, minHeight, maxWidth, maxHeight);

        int waitTicks = GsonHelper.getAsInt(json, "wait_ticks", PortalDefinition.DEFAULT_WAIT_TICKS);
        builder.waitTicks(waitTicks);

        // 仪式式激活配置，可选
        if (json.has("ritual")) {
            builder.ritual(this.parseRitual(GsonHelper.getAsJsonObject(json, "ritual")));
        }

        return builder.build();
    }

    /**
     * 解析仪式式激活配置。
     * <p>
     * 支持指定流体或流体标签、空位尺寸、丢入物品（单个或数组），
     * 以及是否要求全部框架类型。
     *
     * @param json 仪式 JSON
     * @return 仪式配置
     */
    private PortalRitual parseRitual(JsonObject json) {
        PortalRitual.Builder builder = PortalRitual.builder();

        if (json.has("fluid")) {
            ResourceLocation fluidId = ResourceLocation.parse(GsonHelper.getAsString(json, "fluid"));
            Fluid fluid = BuiltInRegistries.FLUID.getOptional(fluidId)
                    .orElseThrow(() -> new IllegalArgumentException(PortalTexts.text("unknown_fluid", fluidId)));
            builder.fluid(fluid);
        }
        if (json.has("fluid_tag")) {
            String tag = GsonHelper.getAsString(json, "fluid_tag");
            String path = tag.startsWith("#") ? tag.substring(1) : tag;
            builder.fluidTag(TagKey.create(Registries.FLUID, ResourceLocation.parse(path)));
        }

        // 空位大小，可选，默认 2×2
        int width = GsonHelper.getAsInt(json, "width", PortalRitual.DEFAULT_WIDTH);
        int depth = GsonHelper.getAsInt(json, "depth", PortalRitual.DEFAULT_DEPTH);
        builder.size(width, depth);

        // 丢入的物品，可选，支持字符串或字符串数组（任意一种匹配即可）
        if (json.has("item")) {
            JsonElement itemElement = json.get("item");
            if (itemElement.isJsonArray()) {
                for (JsonElement element : itemElement.getAsJsonArray()) {
                    builder.item(PortalCatalyst.parse(element.getAsString()));
                }
            } else {
                builder.item(PortalCatalyst.parse(itemElement.getAsString()));
            }
        }

        builder.requireAllFrameTypes(GsonHelper.getAsBoolean(json, "require_all_frame_types", false));
        return builder.build();
    }

    /**
     * 解析颜色字符串。
     * <p>
     * 支持 6 位 RGB（自动补全不透明）与 8 位 ARGB 十六进制，可带「#」前缀。
     *
     * @param value 颜色字符串
     * @return ARGB 颜色值
     */
    private static int parseColor(String value) {
        String hex = value.startsWith("#") ? value.substring(1) : value;
        if (hex.length() == 6) {
            return 0xFF000000 | (int) Long.parseLong(hex, 16);
        }
        if (hex.length() == 8) {
            return (int) Long.parseLong(hex, 16);
        }
        throw new IllegalArgumentException(PortalTexts.text("invalid_color", value));
    }
}
