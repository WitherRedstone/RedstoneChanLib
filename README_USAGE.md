# 用法与示例

这份文档是 [README.md](README.md) 的补充，讲的是“具体怎么写”：传送门的数据包 JSON 字段与示例、传送门的 Java 注册与可调用方法，以及生物描边、方块描边的调用方式。

传送门有两种写法，效果完全一样，可以混着用：写在数据包里（适合整合包和玩家自己改），或者写在 Java 里（适合附属模组）。数据包定义会覆盖同 id 的 Java 定义。

## 传送门（数据包）

### 文件放在哪里

在 `data/<命名空间>/redstone_chan_lib/portal/` 下新建 JSON 文件，文件名去掉 `.json` 后就是定义 id。例如 `data/example/redstone_chan_lib/portal/moon.json` 对应 `example:moon`。

改完执行 `/reload` 就生效，不用重启游戏。同 id 的数据包定义会覆盖 Java 内置定义，所以想改测试门的行为，直接改对应的 JSON 就行。

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

`min_width`、`min_height` 说的是**内部**尺寸（不算框架那一圈），所以默认 2 和 3 就是原版下界门的大小；`max_width`、`max_height` 同样是内部尺寸。

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

### 几点说明

- 门框必须在**同一个平面**上：要么全是同一层的方块（平铺的门），要么全是同一个竖直平面（竖着的门），斜着拐弯的形状不认
- 四个角可以留空，就像原版下界门；判定看的是四条边有没有把内部封住，内部必须全是空气（或可替换方块，比如草）
- 门框不要和同材质的墙、地板连成一片，否则贴着墙点亮时会被当成框架的一部分，导致“怎么点都不亮”或者门比预想的大
- `dimension` 只能指向**已经存在**的维度。数据包不能凭空新建维度，要加新维度得用别的模组（或者自己写维度 JSON）
- 仪式门的框架写的是“水池四周方块**上方**那一格”，所以示例里用花：水池边一圈方块，顶上插花
- 数据包里的颜色是乘法染色，写 `#FF0000` 得到的是纯红，写 `#FFFFFF` 得到接近原贴图的灰白；想恢复默认紫色就删掉 `color`，或者写 `#730DFF`

## 传送门（Java 调用）

- 对外公开的 API 分两个包。传送门相关的在 `com.chinaex123.redstone_chan_lib.api.portal`，一共十二个类：`PortalDefinition`（定义）、`PortalCatalyst`（激活方式）、`PortalRitual`（仪式配置）、`PortalDefinitions`（注册表）、`PortalShape`（形状判定）、`PortalOrientation`（摆放类型）、`PortalStructureHelper`（摆放与拆除）、`PortalPlacement`（跨维度定位）、`PortalTeleport`（传送执行）、`PortalActivation`（激活流程）、`PortalRitualActivation`（仪式激活流程）、`PortalEffects`（特效与音效）；提示文案工具 `com.chinaex123.redstone_chan_lib.util.PortalTexts` 也在公开范围内。生物描边在 `com.chinaex123.redstone_chan_lib.api.outline`，有两个类：`EntityOutline`（生物描边）与 `BlockOutline`（方块描边）。包的说明见各自目录下的 `package-info.java`。
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

定义 `PortalDefinition`：

- `builder(ResourceLocation id)`：新建一个定义构造器。
- `Builder` 上可用的方法：`frameBlock(Block)`、`frameTag(TagKey<Block>)`、`catalyst(Item)`、`catalyst(PortalCatalyst)`、`catalysts(Collection<PortalCatalyst>)`、`itemTagCatalyst(TagKey<Item>)`、`fluidCatalyst(Fluid)`、`fluidTagCatalyst(TagKey<Fluid>)`、`color(int)`、`name(Component)`、`dimension(ResourceKey<Level>)`、`size(int minWidth, int minHeight, int maxWidth, int maxHeight)`、`waitTicks(int)`、`orientation(PortalOrientation)`、`ritual(PortalRitual)`、`build()`。
- 读定义：`id()`、`frameBlocks()`、`frameTags()`、`catalysts()`、`color()`、`name()`、`dimension()`、`minWidth()`、`minHeight()`、`maxWidth()`、`maxHeight()`、`waitTicks()`、`orientation()`、`ritual()`。
- 判断：`isFrame(BlockState state)`、`isCatalyst(ItemStack stack)`、`matchingCatalyst(ItemStack stack)`（返回匹配到的激活方式，不匹配返回 `null`）、`allowsAxis(Direction.Axis axis)`、`isSizeAllowed(int width, int height)`、`getBuildFrameBlock()`（自动盖门时用来铺框架的方块）。

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

### 几点说明

- 注册要在模组构造阶段（`@Mod` 的构造器里，或者 `FMLCommonSetupEvent` 之前）做完，错过这个时间点注册表已经建好，数据包也不会再补发
- Java 内置定义和数据包定义共用同一个 id 空间：同一个 id 同时存在时以数据包为准，`/reload` 之后又变回数据包那份；把数据包删掉，Java 那份就会重新生效
- 传 `dimension(...)` 时那个维度必须已经存在，否则玩家进门会被卡在 `resolve` 返回 `null` 的那一步（传送直接取消）
- `PortalDefinition` 是不可变的，想改就重新 `build()` 一个新的再 `register` 覆盖
- 自定义名称只对“单个方块”有效的那一份存在方块实体里，`Block#getName()` 没有坐标参数，所以物品栏、Jade 这类地方看到的是默认定义的名字

## 生物描边（Java 调用）

描边本身只影响画面，不改实体属性；但“谁身上有描边”这件事是记在服务端存档里的，所以**推荐在服务端调用**：
服务端调用会写进存档、并把设置同步给所有能看到这只生物的玩家，玩家退出重进、区块重新加载之后描边还在。
在客户端调用只对当前这一次游戏有效，重进就没了，适合纯本地的视觉效果。

下面示例用到的 import：

```java
import com.chinaex123.redstone_chan_lib.api.outline.EntityOutline;
import net.minecraft.world.entity.Entity;
```

给一只生物加上描边：

```java
// 发光描边，默认白色
EntityOutline.apply(entity, EntityOutline.Mode.GLOW);
// 发光描边，自定义颜色（纯色）
EntityOutline.apply(entity, EntityOutline.Mode.GLOW, 0xFF7CF9FF);
// 发光描边，彩色（颜色沿着轮廓铺开，同一时刻就能看到好几种颜色）
EntityOutline.apply(entity, EntityOutline.Mode.GLOW, 0xFFFFFFFF, EntityOutline.Style.RAINBOW);
// 发光描边，并指定粗细（单位是方块，0.08 是默认值，写大一点就更粗）
EntityOutline.apply(entity, EntityOutline.Mode.GLOW, 0xFF7CF9FF, EntityOutline.Style.SOLID, 0.3);
// 发光描边，粗细 0.3，并且被方块挡住时不画（最后一个参数是“是否穿透方块”）
EntityOutline.apply(entity, EntityOutline.Mode.GLOW, 0xFF7CF9FF, EntityOutline.Style.SOLID, 0.3, false);
// 黑色描边
EntityOutline.apply(entity, EntityOutline.Mode.BLACK);
// 高光效果
EntityOutline.apply(entity, EntityOutline.Mode.HIGHLIGHT);
// 取消
EntityOutline.clear(entity);
```

### 可以直接调用的方法

- `apply(Entity entity, Mode mode)`：按模式加描边，发光与高光用默认白色，黑色模式用黑色。
- `apply(Entity entity, Mode mode, int rgb)`：同上并指定颜色，只取低 24 位。
- `apply(Entity entity, Mode mode, int rgb, Style style)`：同上并指定纯色 `Style.SOLID` 或彩色 `Style.RAINBOW`，粗细用默认值 `EntityOutline.DEFAULT_THICKNESS`（0.08 格）。
- `apply(Entity entity, Mode mode, int rgb, Style style, double thickness)`：再加上粗细，单位是方块，意思是轮廓最多向外鼓出多少格；越大约粗，黑色模式会自动收窄到八成，看起来更像描边而不是外发光。
- `apply(Entity entity, Mode mode, int rgb, Style style, double thickness, boolean throughWalls)`：再加一个“是否穿透方块”。`true` 时生物躲在墙后面也能看见描边（默认值 `EntityOutline.DEFAULT_THROUGH_WALLS`），`false` 时从玩家眼睛到生物的连线被方块挡住就不画这只生物的描边。
- `clear(Entity entity)`：取消某只生物的描边。
- `clearAll()`：取消所有描边。
- `isActive(Entity entity)`：这只生物当前是否有描边。
- `get(Entity entity)`：取当前的描边设置（`Outline` 记录，含模式、颜色、纯色/彩色、粗细、是否穿透方块），没有则返回 `null`。
- `activeEntities()`：当前所有带描边的生物。
- `defaultColor(Mode mode)`：某种模式的默认颜色。
- 模式是 `EntityOutline.Mode` 的三个值 `GLOW`（发光）、`BLACK`（黑色）、`HIGHLIGHT`（高光）；纯色/彩色是 `EntityOutline.Style` 的 `SOLID`、`RAINBOW`；彩色不是整只生物一起换色，而是按屏幕位置铺开色相，所以同一瞬间这圈光晕上就有红、黄、绿、蓝，而且会随时间缓慢流动。

### 几点说明

- 同一个实体重复调用会覆盖之前的设置，所以换颜色、换模式都直接再调一次 `apply` 即可
- 在服务端调用时，设置会写进世界存档（`redstone_chan_lib_outlines`），并在生物进入玩家视野时自动补发一次，所以退出重进、换维度回来都还在；在客户端调用只存在内存里，重进就没了
- 描边是按实体记录的，实体被移除后对应的记录会自动失效；`EntityOutline.clearAll()` 只清掉当前客户端画面上的描边，不动存档
- 只有活体（`LivingEntity`）会被渲染出来，其它实体加了也没有效果
- 颜色只取低 24 位（R、G、B），透明度由描边的渲染类型固定
- 粗细默认 0.08 格，换算成屏幕像素是“一格 = 100 像素”，所以默认约 8 像素；这个粗细是**固定的**，不随生物远近、视角高低变化，最多夹在 3~32 像素之间。想要更粗就调大 `thickness`
- 模组自带一个测试入口：对着生物按 `K`，依次切换“发光（青色）→ 发光（彩色）→ 黑色 → 高光 → 发光（青色，被挡住就不画）→ 取消”，动作栏会提示当前模式。按 `K` 的切换是发给服务端做的，所以设完之后退出重进也还在，可以顺便验证持久化

## 方块描边（Java 调用）

给方块加描边和给生物加描边是同一套用法，区别只有目标从实体换成了“维度 + 坐标”。同样是**推荐在服务端调用**：会写进该维度的存档，并同步给附近的玩家。

下面示例用到的 import：

```java
import com.chinaex123.redstone_chan_lib.api.outline.BlockOutline;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
```

给一个方块加上描边：

```java
// 发光描边，默认白色
BlockOutline.apply(level, pos, BlockOutline.Mode.GLOW);
// 发光描边，自定义颜色（纯色）
BlockOutline.apply(level, pos, BlockOutline.Mode.GLOW, 0xFF7CF9FF);
// 黑色描边
BlockOutline.apply(level, pos, BlockOutline.Mode.BLACK);
// 高光效果
BlockOutline.apply(level, pos, BlockOutline.Mode.HIGHLIGHT);
// 机械动力风格：细着出现、变到最粗、保持一会儿、再变细消失
BlockOutline.apply(level, pos, BlockOutline.Mode.CREATE, 0xFF65CAFC);
// 机械动力风格，一直显示（不会自动消失）
BlockOutline.apply(level, pos, BlockOutline.Mode.CREATE, 0xFF65CAFC, true, 1.5);
// 机械动力风格，显示 3 秒后自动变细消失
BlockOutline.apply(level, pos, BlockOutline.Mode.CREATE, 0xFF65CAFC, false, 3.0);
// 彩色发光描边
BlockOutline.apply(level, pos, BlockOutline.Mode.GLOW, 0xFFFFFFFF, BlockOutline.Style.RAINBOW);
// 指定粗细与“被挡住就不画”
BlockOutline.apply(level, pos, BlockOutline.Mode.GLOW, 0xFF7CF9FF, 0.1, false);
// 线框：最细的一条线，适合一次标很多方块
BlockOutline.apply(level, pos, BlockOutline.Mode.WIRE, 0xFF7CF9FF);
// 取消
BlockOutline.clear(level, pos);
```

### 可以直接调用的方法

- `apply(Level level, BlockPos pos, Mode mode)`：按模式给方块加描边，发光与高光用默认白色，黑色模式用黑色，机械动力风格用机械动力的蓝色 `0xFF65CAFC`，线框用白色。
- `apply(Level level, BlockPos pos, Mode mode, int rgb)`：同上并指定颜色，只取低 24 位。
- `apply(Level level, BlockPos pos, Mode mode, int rgb, double thickness, boolean throughWalls)`：再加上粗细与“是否穿透方块”。粗细单位是格，换算成屏幕像素是“一格 = 100 像素”：`GLOW`/`BLACK`/`HIGHLIGHT` 默认 0.045 格（约 4.5 像素，夹在 1.5~16 像素），`WIRE` 默认 0.01 格（1 像素，夹在 1~4 像素）。
- `apply(Level level, BlockPos pos, Mode mode, int rgb, Style style)`：指定颜色风格，`SOLID` 是纯色，`RAINBOW` 是彩色。彩色和生物那边一样按位置铺开色相，所以同一瞬间一整圈方框上就有好几种颜色，并且会缓慢流动。
- `apply(Level level, BlockPos pos, Mode mode, int rgb, boolean keepVisible, double holdSeconds)`：机械动力风格用，控制“是否一直显示”和“保持显示的秒数”。
- `apply(Level level, BlockPos pos, Mode mode, int rgb, Style style, double thickness, boolean throughWalls, boolean keepVisible, double holdSeconds)`：全部参数都自己指定。
- `clear(Level level, BlockPos pos)`：取消某个方块的描边。
- `isActive(Level level, BlockPos pos)`：这个方块当前是否有描边。
- `get(Level level, BlockPos pos)`：取当前的描边设置（`Outline` 记录，含模式、颜色、风格、粗细、是否穿透方块、是否一直显示、保持秒数），没有则返回 `null`。
- `activePositions()`：当前客户端上所有带描边的方块坐标。
- `defaultColor(Mode mode)`：某种模式的默认颜色。
- `defaultThickness(Mode mode)`：某种模式的默认粗细，机械动力风格是 `CREATE_THICKNESS`（0.09 格），线框是 `WIRE_THICKNESS`（0.01 格），其它模式是 0.045 格。
- 模式是 `BlockOutline.Mode` 的五个值 `GLOW`（发光）、`BLACK`（黑色）、`HIGHLIGHT`（高光）、`CREATE`（机械动力风格）、`WIRE`（线框）。

### 几点说明

- 同一个方块重复调用会覆盖之前的设置，换颜色、换模式直接再调一次 `apply` 即可；重设一次会重新播一遍出现动画
- 描边画的是方块的包围盒棱线，所以形状奇怪的方块（楼梯、台阶）也是按整格画一圈
- `CREATE` 模式完全显示时的粗细就是你设定的粗细（默认 `CREATE_THICKNESS`，0.09 格，约 9 像素），出现时由细变粗、消失时由粗变细，方框始终贴在方块上，不会向外扩也不会收
- `WIRE` 是唯一不画实心方条的模式：十二条棱各画一条线段（`DEBUG_LINES`），所以看起来就是一圈细线，一次标上百个方块也不会糊；线段线宽很多驱动会钳到 1 像素，所以默认就是最细
- `CREATE` 默认不是一直显示：亮起来、保持 `holdSeconds` 秒（默认 1.5 秒）、再原地变细消失；传 `keepVisible = true` 就一直显示
- 测试入口：对着方块按 `K`，没有对着生物时会依次切换“发光（青色）→ 发光（彩色）→ 黑色 → 高光 → 机械动力风格 → 机械动力风格（一直显示）→ 线框 → 取消”，动作栏会提示当前模式
- 描边按坐标记录，不区分方块种类；方块被挖掉、被爆炸炸掉时会自动清掉这个坐标上的描边（淡出之后消失），所以不会出现“空气上飘着一个方框”。用 `/setblock` 或别的模组把方块换成空气的情况，客户端发现方块没了也会自己收掉
- 方块被换成**别的方块**时描边会保留，因为记录里只存了坐标：想跟着方块一起消失，就在替换方块的那段代码里顺手 `clear` 一下

## 方块扫描高亮（Java 调用）

`BlockOutline` 是“指名道姓地给某个坐标加描边”，`BlockScanOutline` 则是“给一个配方，扫描玩家周围符合条件的方块并全部高亮”，也就是探矿类模组那种效果：拿到之后周围的矿自动亮起来。

扫描以玩家为中心，半径自己定；高亮方式沿用上面那些模式，**默认是 `WIRE` 线框并且穿透方块**——探矿就是要看见墙里的矿。

下面示例用到的 import：

```java
import com.chinaex123.redstone_chan_lib.api.outline.BlockOutline;
import com.chinaex123.redstone_chan_lib.api.outline.BlockScanOutline;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
```

用方块标签当目标，高亮周围所有矿石：

```java
BlockScanOutline.Target ores = BlockScanOutline.target(
        BlockTags.COAL_ORES, BlockTags.IRON_ORES, BlockTags.GOLD_ORES, BlockTags.DIAMOND_ORES);
BlockScanOutline.start(level, ores, BlockOutline.Mode.GLOW, 0xFF7CF9FF, 16);
```

用方块当目标，只标出两种方块：

```java
BlockScanOutline.Target machines = BlockScanOutline.target(Blocks.FURNACE, Blocks.CRAFTING_TABLE);
BlockScanOutline.start(level, machines, 24);
```

完整配置：指定模式、颜色风格、粗细、半径、是否穿透方块、逐方块配色、逐标签配色：

```java
BlockScanOutline.Scan scan = new BlockScanOutline.Scan(
        BlockOutline.Mode.WIRE,       // 高亮模式，五种都行
        0xFF7CF9FF,                   // 统一颜色（两张配色表都没命中的方块用它）
        BlockOutline.Style.RAINBOW,   // 纯色还是彩色
        0.05D,                        // 粗细（格）
        32,                           // 扫描半径
        true,                         // 是否穿透方块（探矿一般要开）
        Map.of(),                     // 逐方块配色
        Map.of());                    // 逐标签配色
BlockScanOutline.start(level, ores, scan);
```

配色优先用标签，标签会跟着矿词自动收进各模组的同类矿石：

```java
// 内置的一份：c:ores/coal、c:ores/iron……c:ores/netherite_scrap
BlockScanOutline.Scan colorful = scan.withTagColors(BlockScanOutline.oreTagColors());

// 再给某个标签单独指定颜色（这里拿“红石矿”举例）
BlockScanOutline.Scan tuned = colorful.withTagColor(BlockTags.REDSTONE_ORES, 0xFFFF4040);
BlockScanOutline.start(level, ores, tuned);
```

如果某个模组的矿石没有进任何矿词标签，也可以用方块 id 兜底，方块配色的优先级高于标签：

```java
BlockScanOutline.Scan exact = tuned.withColor(myModOre, 0xFF00FFE0);
```

两张表都没命中的方块会退回 `rgb`，所以“统一颜色”“按标签配色”“按方块配色”可以同时用。

不过更省事的是用构造器：调用的时候直接把颜色一条条列出来，不用自己拼 `Map`：

```java
BlockScanOutline.Scan scan = BlockScanOutline.scan(BlockOutline.Mode.WIRE, 0xFF7CF9FF)
        .radius(32)                                  // 扫描半径
        .thickness(0.04D)                            // 粗细（格）
        .throughWalls(true)                          // 穿透方块，探矿要开
        .style(BlockOutline.Style.SOLID)             // 纯色还是彩色
        .oreDefaults()                               // 先把内置的矿词配色铺上
        .color(BlockTags.REDSTONE_ORES, 0xFFFF4040)  // 按标签指定颜色，可以写很多条
        .color(Blocks.DEEPSLATE_DIAMOND_ORE, 0xFF9CFFF2) // 按方块指定颜色，优先级更高
        .build();
BlockScanOutline.start(level, ores, scan);
```

如果颜色已经放在一个列表里（比如从配置读出来），也可以一次性传进来：

```java
List<BlockScanOutline.ColorRule> rules = List.of(
        BlockScanOutline.ColorRule.of(BlockTags.IRON_ORES, 0xFFD8AF93),
        BlockScanOutline.ColorRule.of(BlockTags.GOLD_ORES, 0xFFFCEE4B),
        BlockScanOutline.ColorRule.of(myModOre, 0xFF00FFE0));

BlockScanOutline.Scan scan = BlockScanOutline.scan(BlockOutline.Mode.WIRE, 0xFF7CF9FF)
        .colors(rules)
        .build();
```

后加的规则会覆盖先加的规则；`oreDefaults()` 只补“还没有颜色”的标签，所以自己指定的颜色不会被它冲掉。

停止扫描：

```java
BlockScanOutline.stop(level);
```

### 可以直接调用的方法

- `target(List<Block> blocks, List<TagKey<Block>> tags)`：扫描目标，方块与标签可以混用。
- `target(Block... blocks)`：只用方块。
- `target(TagKey<Block>... tags)`：只用标签。
- `start(Level level, Target target, int radius)`：线框模式、默认颜色、穿透方块，只给半径。
- `start(Level level, Target target, Mode mode, int rgb, int radius)`：指定模式与颜色。
- `start(Level level, Target target, Scan scan)`：全部参数自己指定。
- `stop(Level level)`：停止这个维度的扫描。
- `get(Level level)`：取当前扫描配置（`Scan` 记录），没有则返回 `null`。
- `Target.matches(BlockState state)`：判断某个方块状态是否命中目标。
- `Scan.clamped()`：把半径与粗细夹到合法范围（半径 1~48 格）。
- `Scan.withColors(Map<Block, Integer> colors)`：换一整份逐方块配色。
- `Scan.withColor(Block block, int rgb)`：给某一种方块单独指定颜色（在原有表上追加）。
- `Scan.withTagColors(Map<TagKey<Block>, Integer> tagColors)`：换一整份逐标签配色。
- `Scan.withTagColor(TagKey<Block> tag, int rgb)`：给某个标签指定颜色，标签里的方块都会用这个颜色。
- `colorOf(Scan scan, BlockState state)`：按“逐方块配色 → 逐标签配色 → 统一颜色”的优先级算出实际颜色。
- `oreTagColors()`：内置的一份矿石标签配色，用的是 NeoForge 的 `c:ores/xxx` 标签（煤、铁、铜、金、红石、青金石、钻石、绿宝石、石英、远古残骸）。
- `scan(Mode mode, int rgb)`：开始拼一份扫描配置，返回 `Builder`，用来在调用处直接列颜色。
- `Builder.radius(int)`／`thickness(double)`／`throughWalls(boolean)`／`style(Style)`：设置各项参数。
- `Builder.color(TagKey<Block> tag, int rgb)`：按标签指定颜色，可以连着写很多条。
- `Builder.color(Block block, int rgb)`：按方块指定颜色，优先级高于标签。
- `Builder.colors(List<ColorRule> rules)`：一次传入一整串规则。
- `Builder.oreDefaults()`：铺上 `oreTagColors()`，已有的配色保留。
- `Builder.build()`：生成 `Scan`。
- `ColorRule.of(TagKey<Block> tag, int rgb)`／`ColorRule.of(Block block, int rgb)`：列表写法里的一条规则。
- 常量：`DEFAULT_RADIUS`（16 格）、`MAX_RADIUS`（48 格）。

### 想让别的模组的矿也有颜色怎么办

三种做法，从省事到精确：

- 直接用 `oreTagColors()`：只要那个模组把自己的矿石加进了 `c:ores/xxx`（NeoForge 矿词，绝大多数模组都会加），自动就有对应的颜色，不用改代码
- 它用了自己的标签（比如 `mymod:ores/tin`）：`scan.withTagColor(TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("mymod", "ores/tin")), 0xFFC0C0C0)`
- 它连标签都没加：`scan.withColor(BuiltInRegistries.BLOCK.get(...), 0xFFC0C0C0)`，直接按方块 id 指定

这些表都跟着扫描配置一起存档、一起同步给客户端，所以服务端调用也能让所有人看到同样的配色。

### 几点说明

- 客户端调用立刻生效（扫描只是“看”的效果）；服务端调用会写进该维度存档并同步给这个维度里的所有玩家，所以玩家退出重进、换维度回来扫描还在。示例里用哪种都可以，看你需不需要让所有人看到
- 扫描每 10 tick 重扫一次，玩家走出 4 格之外也会立刻重扫，所以挖掉矿之后高亮会自己消失
- 默认模式是 `WIRE`：每个方块十二条 `DEBUG_LINES` 细线，穿透方块，和探矿类模组看到的一样；想换成发光那种光晕就自己传 `BlockOutline.Mode.GLOW`
- 半径越大扫得越多，48 格是上限；一次扫描 32×32×32 大约几万个方块，对帧率影响可以忽略，因为不画的时候完全不扫
- 测试入口：游戏里按 `J`，开关周围所有矿石的扫描（线框、穿透方块、半径 16 格，每种矿用自己的颜色）

## 实体扫描高亮（Java 调用）

`EntityScanOutline` 和方块扫描是一对：方块扫描找方块，它找实体——扫描玩家周围符合条件（实体类型或实体标签）的实体，把每个实体的碰撞箱画成一圈线框。同样是“拿到之后周围目标自动亮起来”，同样只在有目标时才扫描。

用法和方块扫描几乎一样，只是目标换成 `EntityType` 与 `EntityType` 的标签：

```java
import com.chinaex123.redstone_chan_lib.api.outline.BlockOutline;
import com.chinaex123.redstone_chan_lib.api.outline.EntityScanOutline;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
```

用实体类型当目标：

```java
EntityScanOutline.Target zombies = EntityScanOutline.target(EntityType.ZOMBIE, EntityType.SKELETON);
EntityScanOutline.start(level, zombies, 24);
```

用实体标签当目标（这里用“节肢动物”做例子）：

```java
EntityScanOutline.Target arthropods = EntityScanOutline.target(EntityTypeTags.ARTHROPOD);
EntityScanOutline.start(level, arthropods, 0xFFFFC966, 16);
```

完整配置：

```java
EntityScanOutline.Scan scan = new EntityScanOutline.Scan(
        0xFFFFC966,                   // 线框颜色
        BlockOutline.Style.SOLID,     // 纯色还是彩色
        0.01D,                        // 线宽（格，换算成 1~4 像素）
        32,                           // 扫描半径
        true);                        // 是否穿透方块
EntityScanOutline.start(level, zombies, scan);
```

停止扫描：

```java
EntityScanOutline.stop(level);
```

### 可以直接调用的方法

- `target(List<EntityType<?>> types, List<TagKey<EntityType<?>>> tags)`：扫描目标，类型与标签可以混用。
- `target(EntityType<?>... types)`：只用实体类型。
- `target(TagKey<EntityType<?>>... tags)`：只用实体标签。
- `start(Level level, Target target, int radius)`：白色线框、穿透方块，只给半径。
- `start(Level level, Target target, int rgb, int radius)`：指定颜色与半径。
- `start(Level level, Target target, Scan scan)`：全部参数自己指定。
- `stop(Level level)`：停止这个维度的实体扫描。
- `get(Level level)`：取当前扫描配置，没有则返回 `null`。
- `Target.matches(Entity entity)`：判断某个实体是否命中目标（自动跳过已死亡的实体）。
- `Scan.clamped()`：把半径与线宽夹到合法范围（半径 1~64 格）。
- 常量：`DEFAULT_RADIUS`（16 格）、`MAX_RADIUS`（64 格）。

### 几点说明

- 和方块扫描一样，客户端调用立刻生效；服务端调用会写进该维度存档并同步给这个维度里的所有玩家
- 扫描每 10 tick 重扫一次，玩家走出 4 格也会立刻重扫，所以实体走开了高亮会自己消失
- 线框画的是实体的碰撞箱，不是模型轮廓；大型实体（比如末影龙）框会比较大
- 测试入口：游戏里按 `L`，准星对着某个实体时开关“扫描这个实体的同类”（橙色线框）

## 创造模式物品栏分区（Java 调用）

`SectionedCreativeTab` 用来把创造栏拆成分区：进游戏后界面左侧按分区顺序出现一排按钮，点击只显示这个分区，按住 Shift 点击可以同时选多个。没被选中的分区不会出现在列表里，但搜索仍能搜到它们。

下面示例用到的 import：

```java
import com.chinaex123.redstone_chan_lib.api.creative.CreativeSection;
import com.chinaex123.redstone_chan_lib.api.creative.CreativeSectionStyle;
import com.chinaex123.redstone_chan_lib.api.creative.SectionedCreativeTab;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
```

最小用法：三个分区，外观全用默认值。

```java
public static final SectionedCreativeTab TAB = SectionedCreativeTab.builder(
                ResourceLocation.fromNamespaceAndPath("mymod", "main_tab"))
        .title(Component.translatable("itemGroup.mymod"))
        .icon(() -> new ItemStack(Items.DIAMOND))
        .section(Component.literal("材料"), () -> new ItemStack(Items.IRON_INGOT),
                output -> output.accept(Items.IRON_INGOT))
        .section(Component.literal("工具"), () -> new ItemStack(Items.IRON_PICKAXE),
                output -> {
                    output.accept(Items.IRON_PICKAXE);
                    output.accept(Items.IRON_AXE);
                })
        .section(Component.literal("方块"), () -> new ItemStack(Items.STONE),
                output -> output.accept(Items.STONE))
        .build();

// 注册（生成好的页签直接交给自己的 DeferredRegister）
public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "mymod");
public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN =
        TABS.register("main_tab", () -> TAB.tab());
```

自定义外观：底图（未选中、选中、悬停）、按钮大小、物品渲染开关与大小、提示文本。

```java
CreativeSectionStyle style = CreativeSectionStyle.defaults()
        .withTexture(                       // 未选中 / 选中的底图
                ResourceLocation.fromNamespaceAndPath("mymod", "textures/gui/tab_button.png"),
                ResourceLocation.fromNamespaceAndPath("mymod", "textures/gui/tab_button_selected.png"))
        .withHoveredTexture(                // 鼠标悬停时叠在底图上的那张，null 表示悬停不高亮
                ResourceLocation.fromNamespaceAndPath("mymod", "textures/gui/tab_button_hovered.png"))
        .withSize(30, 30)                   // 按钮大小
        .withTextureSize(30, 30)            // 底图实际像素尺寸（和按钮一致就不用写）
        .withIcon(true)                     // 画不画物品
        .withIconScale(0.75F)               // 物品渲染大小，1.0 是 16×16
        .withIconOffset(9, 9)               // 物品相对按钮左上角的偏移
        .withTooltip(Component.literal("材料都在这里"));  // 提示文本，null 表示用默认的“名字 + 按住 Shift 可多选”
```

三张底图也可以一次性换成自己的：`withTexture(未选中, 选中, 悬停)`。

样式可以给整个页签设一份默认值，也可以给单个分区单独设：

```java
SectionedCreativeTab.builder(id)
        .title(Component.literal("我的创造栏"))
        .icon(() -> new ItemStack(Items.DIAMOND))
        .style(CreativeSectionStyle.defaults())
        .section(CreativeSection.of(
                Component.literal("特殊分区"),
                () -> new ItemStack(Items.NETHER_STAR),
                output -> output.accept(Items.NETHER_STAR),
                CreativeSectionStyle.defaults().withIcon(false)))  // 这个按钮只显示底图
        .build();
```

### 可以直接调用的方法

- `SectionedCreativeTab.builder(ResourceLocation id)`：开始描述页签，返回构造器。
- `Builder.title(Component)`：页签标题（必填）。
- `Builder.icon(Supplier<ItemStack>)`：页签图标。
- `Builder.style(CreativeSectionStyle)`：所有分区的默认按钮样式。
- `Builder.section(Component name, Supplier<ItemStack> icon, Consumer<CreativeModeTab.Output> content)`：加一个用默认样式的分区。
- `Builder.section(CreativeSection section)`：加一个分区，可带独立样式。
- `Builder.build()`：生成并登记，返回 `SectionedCreativeTab`。
- `SectionedCreativeTab.tab()`：生成好的 `CreativeModeTab`，交给自己的 `DeferredRegister`。
- `SectionedCreativeTab.id()`／`sections()`／`style()`：取回 id、分区列表与默认样式。
- `CreativeSection.of(name, icon, content)`／`of(name, icon, content, style)`：构造分区，也可以直接 `new CreativeSection(...)`。
- `CreativeSectionStyle.defaults()`：默认样式（24×24、自带底图、物品原尺寸居中偏上、默认提示）。
- `CreativeSectionStyle.withTexture(未选中, 选中, 悬停)`／`withTexture(未选中, 选中)`／`withTexture(同一张)`：换底图。
- `withHoveredTexture(ResourceLocation)`：单独换鼠标悬停底图，`null` 表示悬停时不做额外高亮。
- `withSize(int width, int height)`／`withTextureSize(int w, int h)`：按钮大小与底图像素尺寸。
- `withIcon(boolean)`／`withIconScale(float)`／`withIconOffset(int x, int y)`：物品渲染的开关、大小与偏移。
- `withTooltip(Component)`：悬停提示，`null` 表示用默认的“分区名 + 按住 Shift 可多选”。
- `CreativeSections.byTab(CreativeModeTab)`／`byId(ResourceLocation)`：查登记信息（`Entry` 里有 id、页签、分区、默认样式）。
- `CreativeSections.isActive(tab, index)`／`selectSection(tab, index)`／`toggleSection(tab, index)`：读/改选中状态（代码里也能主动切换）。
- `CreativeSections.sections(tab)`／`styleOf(entry, index)`／`addItems(tab, output)`／`indices(tab)`：取分区、取实际样式、填物品、取下标。

### 几点说明

- 页签必须在模组构造阶段（`DeferredRegister` 注册期间）构建，`build()` 会同时把页签登记到 `CreativeSections`，界面靠这份登记来画按钮
- 分区顺序就是按钮顺序，也是物品在创造栏里的排列顺序；按钮一列放 6 个，超过就换到左边一列
- 未选中的分区以“只进搜索结果”注册，所以它们在创造栏里看不到，但搜索栏里能搜到，不会丢物品
- 自带三张 24×24 的默认底图（未选中、选中、悬停），想换成自己的图就把 PNG 放进 `assets/<你的命名空间>/textures/gui/...`，路径要写全（含 `textures/` 前缀和 `.png`）
- 悬停图是**叠在**底图之上画的，默认那张中间是透明的只留一圈高亮边，所以选中状态仍然看得出来；如果你换成一张不透明的悬停图，它会把选中底图整个盖住
- 物品是原版物品渲染，缩放通过 `PoseStack` 做，所以 `withIconScale(0.5F)` 得到 8×8、`0.75F` 得到 12×12
- 按钮位置是“界面左边距 - 按钮宽度 - 3”往左排，界面宽度变化时会跟着 `leftPos` 走

## 实现说明

传送门：

- 形状判定分两步：先从被点击的框架方块旁边找一个内部空格，在传送门所在的平面里做四向洪水填充，把内部整块圈出来；再检查这块区域是完整矩形，且它外面那一圈（四条边）都是框架方块。四个角允许留空，和原版下界门一致，判定的只是四条边是否把内部封住
- 一座门的颜色、名称、定义 id 存在每个传送门方块的方块实体里，靠方块更新包同步，所以同一套方块可以有不同颜色、不同名称、去不同维度；方块被拆掉时按连通关系整片清除
- 传送分两步：进门时记录目标维度和目标坐标并开始倒计时，倒计时结束时再真正 `teleportTo`；倒计时期间玩家离开这整片门方块的范围就取消，避免站门口反复传送
- 到达之后会记一份“刚到达的门方块”，短时间内不会再触发回传，避免两头来回弹
- 目标维度的坐标会按下界比例换算（主世界与下界之间 ×/÷8），没有合适的落点就在附近找空地按定义盖一座同款门
- 传送门方块没有碰撞箱，但注册时加了 `forceSolidOn()`、并重写了拒绝流体替换的 `canBeReplaced`，所以水流不进来、流体桶也放不进去，不会出现“水把门冲出一道口子”
- 报错与日志走 `message.redstone_chan_lib.*` 翻译键，随语言变化，整合包可以直接覆盖这些键改写提示；内置测试门的名称也用的是翻译键

生物描边：

- 描边是屏幕空间的：先把模型画进一张离屏遮罩（只写剪影，不采样模型贴图，用原版描边着色器按贴图 alpha 丢弃透明像素），再把遮罩按 8 个方向的像素偏移叠回主画面，所以是贴着模型的一圈，不会出现“两层壳打架”
- 合成时把 `RenderSystem` 的模型视图矩阵与投影矩阵都换成 2D 正交，结束再恢复；换完必须显式 `applyModelViewMatrix()`，否则着色器还是用旧矩阵，四边形会被变换到画面外
- 遮罩贴图的环绕方式被设成“夹到边缘”：合成时是按偏移若干像素去采样遮罩的，屏幕边缘那部分会采到贴图外面，若保持默认的重复环绕就会把对边的剪影卷回来，画面上出现一圈莫名其妙的光环
- `throughWalls` 为 `false` 时，画之前先做一次可见性检查（先查方块，再查其它生物的碰撞箱），被地形或别的生物挡住就整只不画；为 `true` 时不管挡不挡都画，和原版发光一致

方块描边：

- 方块描边走的是几何路线：在关卡渲染的“半透明方块之后”那一步，把方块包围盒的十二条棱各展开成一根实心的方棱柱（和机械动力的画法一致），所以粗细直接就是屏幕像素，不需要离屏缓冲
- 方棱柱的横截面是正方形：先取棱的方向，再叉乘出一个参考轴得到横截面的两条边，两端各沿棱多伸半个线宽，所以从任何角度看都一样粗，拐角处三条棱还会叠成一个实心小方块
- 机械动力风格的动画由两个量组成：线宽随进度从零涨到设定的最粗、透明度按立方的进度渐隐，方框始终贴在方块上原地不动；进度存在 `BlockOutlineRenderer` 的 `FADE` 表里，出现用 0.25 秒、消失用 0.4 秒（机械动力是 8 tick），中间那段保持显示的时长由 `holdSeconds` 决定
- 方块描边同样是“设置一次就从 0 开始播”，所以重复调用 `apply` 会重新播一遍；每次写入都会记一个序号（`BlockOutline.stamp`），渲染器靠它区分“新设置了一次”和“一直没变”
- 机械动力风格默认是“脉冲”：播完淡入、保持、淡出之后停在“已播完”状态，数据还在但不再绘制，直到下次重新设置；要一直显示就传 `keepVisible = true`
- 方块的彩色风格和生物一样按顶点位置铺开色相：同一条棱的两端各算一次颜色，中间自动插值，所以一整圈方框上就能同时看到好几种颜色
- 方块描边同样支持“是否穿透方块”：为 `false` 时开深度测试，被挡住的部分自然不画；为 `true` 时关深度测试，隔着墙也能看见轮廓，和机械动力那种“透视高亮”一致
- 方块没了之后描边会自己收掉：服务端监听方块破坏与爆炸事件，把同坐标的记录删掉并同步给附近玩家；客户端渲染时还会检查一次方块是不是已经变成空气（区块没加载就先不动），所以漏网的情况也能兜住
- 退出重进之后描边还在：描边记在维度存档里，登录、换维度时服务端会重发一遍；客户端在进入世界时会把上一份缓存清掉，清完立刻发一个 `outline_resync` 请求，让服务端再发一次，所以不会出现“刚进去有、下一帧被自己清掉”的时序问题

方块扫描高亮：

- 扫描在客户端做：每 10 tick（或玩家移动超过 4 格）以玩家为中心遍历一遍立方体区域，用 `state.is(block)` / `state.is(tag)` 匹配目标，把命中的坐标缓存成一张和 `BlockOutline` 一样的“坐标 → 描边”表
- 那张表直接喂给 `BlockOutlineRenderer.renderBoxes(...)`，所以五种模式、颜色、彩色、粗细、穿透开关全部复用同一套画法，不维护第二份渲染代码
- 线框模式用的是另一种图元：`VertexFormat.Mode.DEBUG_LINES` 手写十二条棱，坐标同样先变换到镜头空间、着色器的 ModelViewMat 清成单位矩阵，和方块描边用的是同一套坐标约定；线段天生 1 像素宽、普通 alpha 混合，所以又细又干净
- 扫描配置和方块描边一样有服务端存档与同步：服务端调用会写进维度存档并广播给同维度玩家，玩家登录、换维度时补发；客户端调用则只影响自己
- 游戏里按 `J`、`L` 开的扫描也是走服务端的（客户端只发一个 `scan_toggle` 请求），所以退出重进之后扫描还在；如果只在客户端直接调 `BlockScanOutline.start(...)`，那就是临时的，重进就没了
- 逐方块、逐标签配色是在生成描边表时定的：每个命中的方块都单独算一次 `colorOf(scan, state)`，优先级是“方块表 → 标签表 → 统一颜色”，所以同一屏里煤是深灰、铁是米色、钻石是青色
- 内置配色刻意按 NeoForge 的 `c:ores/xxx` 标签写，而不是写死方块 id：别的模组的铜矿、锡矿只要进了矿词标签就自动有颜色
- 两张颜色表跟着扫描配置一起存档与同步（NBT 里存 id 与颜色两列，方块与标签各一组），客户端拿到的就是算好的成品表

实体扫描高亮：

- 扫描同样在客户端做：每 10 tick（或玩家移动超过 4 格）用 `level.getEntitiesOfClass(Entity.class, 区域, 目标判定)` 取一批实体，把它们的碰撞箱缓存下来
- 画的时候复用 `BlockOutlineRenderer.renderWireBoxes(...)`，所以和方块线框是同一套图元、同一套坐标约定、同一套穿透开关，不额外维护渲染代码
- 实体目标是“类型列表 + 标签列表”，判定时先跳过已死亡的实体；服务端调用会写进该维度存档并在登录、换维度时补发

创造模式物品栏分区：

- 分区信息在构建页签时登记到 `CreativeSections`（页签实例与 id 两套索引），`displayItems` 回调里按页签取回分区，所以页签本身不需要知道有哪些分区
- 界面侧靠两个客户端 Mixin：一个在 `CreativeModeInventoryScreen#selectTab` 的 HEAD 位置拆掉旧按钮、按新页签重建，一个用 `@Invoker` 暴露私有的 `refreshCurrentTabContents` 供点击后刷新列表
- 点击分区后先改选中集合，再用页签当前的显示条件重建内容（`buildContents`），最后用新列表刷新界面，所以不需要重新开界面
- 未选中的分区通过一个包装过的 `CreativeModeTab.Output` 把物品降级成 `SEARCH_TAB_ONLY`，既不出现在列表里，也不会在搜索里丢
