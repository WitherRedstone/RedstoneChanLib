# 用法与示例

这份文档是 [README.md](README.md) 的补充，主题是“怎么写出一座自己的传送门”：数据包 JSON 的字段与示例、Java API 的注册方式与可调用的方法，以及一些实现上的说明。

## 数据包自定义

在 `data/<命名空间>/redstone_chan_lib/portal/` 下新建 JSON 文件，文件名去掉 `.json` 后就是定义 id。例如 `data/example/redstone_chan_lib/portal/moon.json` 对应 `example:moon`。

修改数据包后执行 `/reload` 就会生效，不需要重启游戏。数据包定义会覆盖同 id 的 Java 内置定义，所以想改测试门的行为，直接改对应的 JSON 即可。

### 字段说明

- `frame_blocks`：必填，外围方块数组。可以写方块 id（如 `minecraft:diamond_block`），也可以写方块标签（以 `#` 开头，如 `#minecraft:stone_bricks`）
- `catalyst`：可选，激活方式。可以写一个字符串，也可以写字符串数组（任意一种匹配即可点亮）。字符串写法：
    - `item:minecraft:diamond`：手持指定物品
    - `item:#minecraft:coals` 或直接 `#minecraft:coals`：手持标签内任意物品
    - `fluid:minecraft:water`：手持装有指定流体的容器（水桶、岩浆桶、任意带流体能力的储罐都行）
    - `fluid:#minecraft:water`：手持装有标签内任意流体的容器
    - 省略 `item:` 前缀时按物品处理
    - 写成空字符串表示空手右键即可点亮；整个字段省略也等同于空手
- `dimension`：可选，目标维度 id，默认 `minecraft:the_nether`
- `orientation`：可选，摆放类型。`any`（默认，竖直与平铺都能激活）、`vertical`（只认竖直的门）、`horizontal`（只认平铺在地面的门）
- `color`：可选，颜色。支持 `#RRGGBB` 与 `#AARRGGBB`，省略为模组默认的紫色 `#730DFF`。染色是乘法、贴图本身是灰度的，所以写什么颜色就得到什么颜色（只用到 R、G、B，A 通道不参与渲染）
- `name`：可选，传送门方块的名称（纯文本）
- `translate`：可选，传送门方块名称的翻译键，优先级高于 `name`
- `min_width`、`min_height`：可选，内部最小宽度与高度，默认 2 和 3
- `max_width`、`max_height`：可选，内部最大宽度与高度，默认 21
- `wait_ticks`：可选，进入传送门后等待多少刻才传送，默认 80
- `ritual`：可选，仪式式激活配置。写成一个对象，包含：
    - `fluid`：空位里必须装满的流体，如 `minecraft:water`
    - `fluid_tag`：用流体标签指定空位流体，如 `#minecraft:water`
    - `width`、`depth`：空位大小，默认 2 和 2
    - `item`：丢进流体才会触发的物品，写法与 `catalyst` 一致（`item:` 前缀、`#` 标签、字符串数组都可以），省略表示任意物品
    - `require_all_frame_types`：为 `true` 时，定义里的每一种框架方块（或标签）都必须在门框上出现至少一次，默认 `false`

### 示例：钻石块 ＋ 钻石（模组自带的 `test.json`）

```json
{
  "frame_blocks": [
    "minecraft:diamond_block"
  ],
  "catalyst": [
    "item:minecraft:diamond",
    "fluid:minecraft:water"
  ],
  "dimension": "minecraft:the_nether",
  "name": "测试",
  "min_width": 2,
  "min_height": 3,
  "max_width": 21,
  "max_height": 21,
  "wait_ticks": 80
}
```

### 示例：带颜色的自定义维度门

```json
{
  "frame_blocks": [
    "#minecraft:stone_bricks"
  ],
  "catalyst": [
    "item:minecraft:amethyst_shard",
    "#minecraft:coals"
  ],
  "dimension": "example:moon",
  "color": "#FF8844FF",
  "name": "月之门",
  "min_width": 2,
  "min_height": 3,
  "max_width": 9,
  "max_height": 9,
  "wait_ticks": 60
}
```

### 示例：平铺在地面的横门（模组自带的 `test_flat.json`）

```json
{
  "frame_blocks": [
    "minecraft:gold_block"
  ],
  "catalyst": [
    "item:minecraft:diamond"
  ],
  "dimension": "minecraft:the_nether",
  "orientation": "horizontal",
  "name": "测试横门",
  "min_width": 2,
  "min_height": 2,
  "max_width": 21,
  "max_height": 21,
  "wait_ticks": 80
}
```

### 示例：仪式式传送门（模组自带的 `test_ritual.json`）

```json
{
  "frame_blocks": [
    "#minecraft:flowers"
  ],
  "dimension": "minecraft:the_nether",
  "orientation": "horizontal",
  "name": "测试仪式门",
  "min_width": 2,
  "min_height": 2,
  "max_width": 21,
  "max_height": 21,
  "wait_ticks": 80,
  "ritual": {
    "fluid": "minecraft:water",
    "width": 2,
    "depth": 2,
    "item": "item:minecraft:diamond",
    "require_all_frame_types": false
  }
}
```

摆法：挖一个 2×2、深度一格的坑灌满水，四周一圈方块的顶面插上花，往水里丢一颗钻石；钻石沉下去之后会落雷，水面变成横门。

## Java API

- 对外公开的 API 全部放在 `com.chinaex123.redstone_chan_lib.api.portal` 包里，一共十三个类：`PortalDefinition`（定义）、`PortalCatalyst`（激活方式）、`PortalRitual`（仪式配置）、`PortalDefinitions`（注册表）、`PortalShape`（形状判定）、`PortalOrientation`（摆放类型）、`PortalStructureHelper`（摆放与拆除）、`PortalPlacement`（跨维度定位）、`PortalTeleport`（传送执行）、`PortalActivation`（激活流程）、`PortalRitualActivation`（仪式激活流程）、`PortalEffects`（特效与音效）、`PortalTexts`（提示文案）。包的说明见同目录的 `package-info.java`。
- 这不是另写的一份“给外部用”的实现：模组自己的右键点亮、破坏拆门、传送倒计时、遮挡遮罩，调用的就是这些类里的同一批方法。所以外部模组调 `PortalActivation.tryActivate(...)` 得到的效果，和玩家自己右键点亮的完全一致。
- 附属模组或整合包脚本可以在模组构造阶段注册内置定义。下面示例用到的 import：

```java
import com.chinaex123.redstone_chan_lib.api.portal.PortalDefinition;
import com.chinaex123.redstone_chan_lib.api.portal.PortalDefinitions;
import com.chinaex123.redstone_chan_lib.api.portal.PortalOrientation;
import com.chinaex123.redstone_chan_lib.api.portal.PortalRitual;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
```

注册一个完整的定义：

```java
PortalDefinitions.register(
        PortalDefinition.builder(ResourceLocation.fromNamespaceAndPath("example", "moon"))
                .frameBlock(Blocks.DEEPSLATE_BRICKS)
                .catalyst(Items.AMETHYST_SHARD)
                .color(0xFF8844FF)
                .name(Component.translatable("portal.redstone_chan_lib.test"))
                .dimension(ResourceKey.create(Registries.DIMENSION,
                        ResourceLocation.fromNamespaceAndPath("example", "moon")))
                .size(2, 3, 9, 9)
                .waitTicks(60)
                .build()
);
```

框架也可以按标签配置：

```java
PortalDefinition.builder(ResourceLocation.fromNamespaceAndPath("example", "bricks"))
        .frameTag(BlockTags.STONE_BRICKS)
        .catalyst(Items.FLINT_AND_STEEL)
        .build();
```

激活方式可以叠加，例如“钻石或水都行”：

```java
PortalDefinition.builder(ResourceLocation.fromNamespaceAndPath("example", "diamond"))
        .frameBlock(Blocks.DIAMOND_BLOCK)
        .catalyst(Items.DIAMOND)    // 物品
        .itemTagCatalyst(ItemTags.COALS)    // 物品标签
        .fluidCatalyst(Fluids.WATER)    // 流体
        .fluidTagCatalyst(FluidTags.LAVA)   // 流体标签
        .build();
```

不调用任何 `catalyst(...)` 方法时就是空手右键点亮。

想限制摆放类型，例如只接受平铺在地面上的门（像末地传送门）：

```java
PortalDefinition.builder(ResourceLocation.fromNamespaceAndPath("example", "flat"))
        .frameBlock(Blocks.GOLD_BLOCK)
        .catalyst(Items.DIAMOND)
        .orientation(PortalOrientation.HORIZONTAL)
        .build();
```

`PortalOrientation` 的三个取值：`ANY`（默认，竖直与平铺都行）、`VERTICAL`（只认竖直的门）、`HORIZONTAL`（只认平铺的门）。

想要仪式式激活（暮色森林式），给定义挂一个 `PortalRitual`：

```java
PortalDefinitions.register(
        PortalDefinition.builder(ResourceLocation.fromNamespaceAndPath("example", "ritual"))
                .frameTag(BlockTags.FLOWERS)    // 水池边上一圈方块顶面插的花
                .dimension(Level.NETHER)
                .orientation(PortalOrientation.HORIZONTAL)
                .ritual(PortalRitual.builder()
                        .fluid(Fluids.WATER)    // 空位里的流体
                        .size(2, 2) // 空位大小
                        .item(Items.DIAMOND)    // 丢进去的物品
                        .build())
                .build()
);
```

仪式门的框架方块摆在水池四周方块的上方，所以示例里用的是花标签：2×2 的水池，四周一圈方块顶上插花，丢钻石进去就会落雷点亮。

`PortalRitual` 还可以用 `fluidTag(FluidTags.WATER)` 指定流体标签、用 `itemTag(ItemTags.COALS)` 指定物品标签、用 `requireAllFrameTypes(true)` 要求门框用满定义里列出的每种方块。不调用 `item(...)` 时任何物品都能触发仪式。

注册过的定义可以用 `PortalDefinitions.get(id)` 查询，`PortalDefinitions.all()` 列出全部当前有效定义。

### 可以直接调用的方法

下面按用途列出公开方法的签名，参数里的 `def` 都是 `PortalDefinition`，`level` 是 `Level`（服务端请传 `ServerLevel`）。

注册表 `PortalDefinitions`：

- `register(PortalDefinition def)`：注册或替换一个内置定义。
- `get(ResourceLocation id)`：按 id 取定义，没有则返回 `null`。
- `all()`：当前所有有效定义（数据包定义覆盖同 id 内置定义）。
- `matchingFrames(BlockState state)`：这个方块能当哪些定义的外围框架。
- `isAnyFrame(BlockState state)`：这个方块是否是任意定义的外围框架。
- `hasRituals()`：当前是否有定义配置了仪式式激活，掉落物检测用它做快速跳过。
- `defaultDefinition()`：默认定义，方块名显示用的就是它。

形状判定 `PortalShape`：

- `find(Level level, BlockPos framePos, PortalDefinition def)`：从任意一个框架方块出发，把整座门圈出来；形状不合法返回 `null`。四个角可以留空。
- `compose(BlockPos min, Direction.Axis axis, int width, int height)`：按已知的框架范围和内部尺寸直接构造形状，不做洪水填充（仪式门点亮时用的就是它）。
- `connectedPortalBlocks(BlockGetter level, BlockPos start, Block block)`：把相连的传送门方块收集成一个列表。
- `isFramePosition(BlockGetter level, BlockPos pos, BlockPos portalPos, Direction.Axis axis, Block block)`：判断某个坐标是不是这座门真正的外圈框架（只看四条边，四个角不算）。
- `min()`、`max()`、`axis()`、`width()`、`height()`、`isHorizontal()`、`interiorCenter()`、`interiorMin()`、`interiorPositions()`、`isInside(BlockPos pos)`：拿到形状后的读数。`axis()` 为 `X`、`Z` 是竖直门，为 `Y` 是平铺的横门。

摆放与拆除 `PortalStructureHelper`：

- `fillPortal(Level level, PortalShape shape, PortalDefinition def)`：把一个已经合法的框架内部填成传送门方块。
- `buildPortal(Level level, BlockPos min, Direction.Axis axis, int width, int height, PortalDefinition def)`：按定义盖一座新门，`min` 是框架最小角。
- `destroyConnectedPortal(Level level, BlockPos start)`：拆掉与 `start` 相连的整座门，保留外围框架，每一格都有破坏音效。
- `isAreaSuitable(Level level, BlockPos min, Direction.Axis axis, int width, int height)`：判断这块地方能不能盖门。

跨维度定位 `PortalPlacement`：

- `findOrCreatePortal(ServerLevel target, ServerLevel source, PortalDefinition def, BlockPos sourcePos)`：在目标维度找同款门，找不到就地盖一座，返回玩家应站立的内部坐标。
- `findOrCreatePortal(ServerLevel target, ServerLevel source, PortalDefinition def, BlockPos sourcePos, Direction.Axis axis)`：同上，并指定新建的门用哪种朝向（`Y` 为横门，`X`/`Z` 为竖直门，`null` 按定义自动选择）。
- `createPortal(ServerLevel level, BlockPos base, PortalDefinition def)` 与带 `Direction.Axis axis` 的重载：直接在指定位置盖一座门。

传送执行 `PortalTeleport`：

- `resolve(ServerPlayer player, PortalDefinition def)`：算出目的地（`Destination` 记录维度与坐标），算不出来返回 `null`。
- `resolve(ServerPlayer player, PortalDefinition def, Direction.Axis axis)`：同上，并让新建的目标门沿用给定的朝向。
- `move(ServerPlayer player, Destination destination)`：把玩家移过去，并带动到达特效。
- `teleport(ServerPlayer player, PortalDefinition def)`：等于 `resolve` + `move`，成功返回 `true`。

激活与特效 `PortalActivation`、`PortalRitualActivation`、`PortalEffects`：

- `PortalActivation.tryActivate(Player player, BlockPos framePos)`：用主手物品尝试点亮，返回是否成功。
- `PortalActivation.tryActivate(Level level, BlockPos framePos, Player player, InteractionHand hand, ItemStack stack)`：指定手与物品的版本，返回点亮成功的定义或 `null`。
- `PortalRitualActivation.tryActivate(ServerLevel level, ItemEntity itemEntity)`：判断这件掉落物是否落进了某个仪式门的流体池，命中就点亮并返回 `true`。模组的掉落物检测每刻调用它。
- `PortalEffects.playActivationEffects(Level level, BlockPos pos)`：播放点亮时的音效与粒子。
- `PortalEffects.playArrivalEffects(ServerPlayer player, BlockPos targetPos)`：播放到达时的音效。

提示文案 `PortalTexts`：

- `PortalTexts.text(String key, Object... args)`：取一条按语言本地化后的普通字符串，用于异常信息与日志；键名不带前缀，例如 `PortalTexts.text("unknown_fluid", value)`。
- `PortalTexts.component(String key, Object... args)`：取文本组件，方便塞进界面或提示里。
- 键统一以 `message.redstone_chan_lib.` 开头（`PortalTexts.PREFIX`），写在模组自己的语言文件里，其它模组或整合包也可以覆盖同名键来改写提示。

## 实现说明

- 传送门方块本体只有朝向属性，颜色、名称与所属定义都存在方块实体 `PortalBlockEntity` 里，因此同一种方块可以同时属于多个传送门而不冲突
- 朝向属性用的是 `axis`，取值 `x`、`y`、`z`：`x`/`z` 是竖直的门，`y` 是平铺在地面的横门。横门的贴图与形状都是悬在方块中间的一层薄片（7/16 到 9/16），正好落在框架的腰线上；判定传送靠的是方块格本身，与形状无关
- 形状判定把所有朝向统一成“平面内的横向 u 与纵向 v”，所以竖门与横门走的是同一套洪水填充逻辑，只是 u、v 映射到的世界坐标轴不同
- 颜色通过方块染色回调实现，模型面写了 `tintindex`。染色是乘法叠在贴图上，所以传送门贴图本身是灰度图（亮度取自原来的紫色贴图），颜色完全由 `color` 决定，想变成红色就是红色，不会在紫色上再叠一层；默认的紫色 `#730DFF` 就是原版下界门那种紫，也是这么染出来的。染色只用到 R、G、B 三个通道，A 通道不参与渲染，透明度始终来自贴图本身。整座门（包括破坏粒子）只用这一张灰度贴图，所以粒子也会跟着染色走
- 流体激活读的是物品的流体能力，不看物品 id，所以原版桶和附属模组的储罐都能被同一条定义识别；点亮后会从容器里抽走一桶，水桶因此变成空桶
- 形状判定分两步：先从被点击的框架方块旁边找一个内部空格，在传送门所在的平面里做四向洪水填充，把内部整块圈出来；再检查这块区域是完整矩形，且它外面那一圈（四条边）都是框架方块。四个角允许留空，和原版下界门一致，判定的只是四条边是否把内部封住
- 因为判定看的是“内部空格有没有被封住”，框架和同材质的墙、地板连成一片也不会误判；但内部不能有缺口，否则空气会漏到外面，门点不着
- 传送门方块在生存模式下不可破坏，创造模式破坏它会拆掉整座门；拆掉真正的外圈框架方块也会让整座门消失，拆除时每一格都会播放破坏音效。拆分时会先用相邻的传送门方块反推出整座门的范围，确认被拆的方块确实落在外圈四条边上才动手，所以门旁边随手放的同材质方块拆掉不会影响门
- 自动化或其它模组也可以直接调 `PortalShape.isFramePosition(...)` 做同样的判断：给定一个坐标、相邻的传送门方块、门的法线轴和门方块类型，返回它是不是这座门的外圈框架
- 客户端遮罩改为在客户端刻里扫描玩家碰撞箱覆盖的方块，不依赖服务端发包
- 仪式门与右键门共用同一套定义与方块，区别只在“怎么点亮”：仪式门从物品所在的流体格开始，在同一个 Y 层做四向洪水填充圈出整个流体池，再检查外圈方块的上方；点亮后直接用洪水填充算出的矩形构造横门，省去一次重复判定
- 仪式门要求掉落的物品整个沉到液面以下才判定，避免物品还在半空中就点亮
- 掉落物检测挂的是实体刻事件的 `Post` 阶段，只在服务端处理掉落物，并且先用 `PortalDefinitions.hasRituals()` 判断有没有仪式门，没用仪式门的整合包里几乎零开销
- 传送门方块没有碰撞箱，但注册时加了 `forceSolidOn()`、并重写了拒绝流体替换的 `canBeReplaced`，所以水流不进来、流体桶也放不进去，不会出现“水把门冲出一道口子”
- 仪式门的落雷是普通闪电实体，会点燃方块、会劈伤附近的实体；闪电落在流体池正上方一格
- 在目标维度自动建门时，框架优先挑“碰撞形状是完整方块”的方块来砌墙；如果定义里的框架都是花、树苗这类不能当墙的方块，会退回石头
- 报错与日志不再把中文写死在代码里，全部走语言文件里的 `message.redstone_chan_lib.*`：中文客户端看到中文，英文客户端与专用服务端看到英文。整合包或附属模组想换措辞，在自己的语言文件里写同名键覆盖即可
- 内置测试门的名字用的是翻译键 `portal.redstone_chan_lib.test`，所以它也会跟着语言变（中文“测试”、英文“Test”）；数据包里的定义想做到同样效果，可以写 `translate` 而不是 `name`
