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

## 流体碰撞配方（数据包）

### 文件放在哪里

放进数据包的 `data/<命名空间>/recipe/<文件名>.json`，`type` 写 `redstone_chan_lib:fluid_collision`（流体撞流体）或 `redstone_chan_lib:block_collision`（流体撞方块），改动后 `/reload` 即可生效。

### 字段说明

- `type`：`redstone_chan_lib:fluid_collision` 或 `redstone_chan_lib:block_collision`。
- `fluid`：流入的流体，写 `"minecraft:lava"` 或 `"#minecraft:water"` 这样带 `#` 的标签；也可以写原版的 `{"tag": "..."}` 或流体列表。
- `other_fluid`：被撞上的另一种流体（只有 `fluid_collision` 有），写法同上。
- `block`：被撞上的方块（只有 `block_collision` 有），写 `"minecraft:ice"`、`"#minecraft:logs"`、一个方块 id 的列表，或原版的 `{"tag": "..."}`。
- `results`：产出列表，至少一条。每条写 `block` 或 `item` 二选一，可带 `count`（默认 1，1~64）与 `chance`（默认 1.0，0~1）；`item` 直接写物品 id 即可，也可以写 `{"id": "...", "count": ...}`。
  概率是**一次碰撞只出一个**：所有条目按顺序排开，掷一次 0~1 的骰子，谁的累计概率区间盖住它就只出谁，所以 50% + 25% + 25% 意味着三选一，不会同时冒出来；总和不足 100% 时剩下的概率就是“什么都不出”。
- `delay`：触发后延迟几刻才生成，默认 1。
- `consume_fluid`：触发后是否把流入的流体拿掉，默认 `false`。
- `consume_other`：`fluid_collision` 专用，触发后是否把另一种流体也拿掉，默认 `false`。
- `consume_block`：`block_collision` 专用，触发后是否把被撞的方块拿掉，默认 `false`。
- `consume_source`：触发后是否连着把这一股流体的**源头**一起抽掉，默认 `false`；适合“岩浆倒进水池会一直冒石头”这种场景，关掉就只处理碰撞的那一格，源头还在。
- `sound`：触发时播放的音效 id，可省略；省略时默认用“水遇岩浆熄灭”（`minecraft:block.lava.extinguish`）。
- `fluid_below`：可选，要求流入流体**自己脚下**是某种方块，写 `"minecraft:magma_block"` 或标签都行；字段名不能叫 `fluid`，因为 `fluid` 已经被用来写碰撞的流体原料了。JEI 里这个条件槽画在左边流体那一列的下方。
- `down`：可选，要求参与碰撞的**对方**（方块那一边）正下方是某种方块，写 `"minecraft:gold_block"` 或 `"#minecraft:base_stone_nether"` 这类标签都行。JEI 里这个条件槽画在右边方块那一列的下方。
- `center`：可选，要求“空出来的那一格”正下方是某种方块。两种摆法都算碰撞：流体直接流到金块旁边（甚至流到金块正上方），这时空出来的那一格就是流体自己那格，`center` 看流体脚下；或者摆成 `[金块][空][岩浆]` 三格，`center` 看中间空格脚下。JEI 里这个条件槽画在中间那一列的下方。
  三个条件写几个就要求几个同时满足，都不写就是不做下方限制；流体撞流体时同样生效（`down` 这时看另一种流体那格脚下）。

### 示例：熔岩撞水变石头

```json
{
  "type": "redstone_chan_lib:fluid_collision",
  "fluid": "minecraft:lava",
  "other_fluid": "minecraft:water",
  "results": [
    { "block": "minecraft:obsidian", "count": 1, "chance": 0.25 },
    { "block": "minecraft:stone", "count": 1, "chance": 0.75 }
  ],
  "delay": 1,
  "consume_fluid": true,
  "consume_other": true,
  "consume_source": true,
  "sound": "minecraft:block.lava.extinguish"
}
```

构造器的参数顺序是“流体、对方原料、产出、延迟、是否消耗流入流体、是否消耗对方、是否消耗流体源头、音效”；后两个可以省略，省略时音效默认是水遇岩浆熄灭。

### 示例：岩浆撞金块，中间空格下方也得是金块

```json
{
  "type": "redstone_chan_lib:block_collision",
  "fluid": "minecraft:lava",
  "block": "minecraft:gold_block",
  "results": [
    { "block": "minecraft:gold_ore", "chance": 1.0 }
  ],
  "center": "minecraft:gold_block",
  "delay": 1
}
```

摆成同一高度的三格：`金块 — 空气 — 岩浆`，并且中间那格空气的正下方也是金块；也可以直接把岩浆倒在金块上方或旁边，只要那一格脚下是金块。
这时岩浆那一格里的流体会和左边的金块隔着空格撞上，产出金矿石；把中间的空格填掉、或者把空格下面换成石头，都不会触发。

### 示例：熔岩撞冰

```json
{
  "type": "redstone_chan_lib:block_collision",
  "fluid": "minecraft:lava",
  "block": "minecraft:ice",
  "results": [
    { "block": "minecraft:obsidian", "count": 1, "chance": 0.5 },
    { "item": "minecraft:charcoal", "count": 2, "chance": 0.3 }
  ],
  "delay": 5,
  "consume_fluid": true,
  "consume_block": true,
  "sound": "minecraft:block.glass.break"
}
```

## 流体碰撞配方（Java 调用）

配方类本身就是公开的 record，直接用构造器或者静态工厂拼一份，再注册进 `CollisionRecipes` 即可；注册的配方会与数据包配方一起参与匹配，也会出现在 JEI 里。

下面示例用到的 import：

```java
import com.chinaex123.redstone_chan_lib.api.collision.BlockCollisionRecipe;
import com.chinaex123.redstone_chan_lib.api.collision.CollisionOutput;
import com.chinaex123.redstone_chan_lib.api.collision.CollisionRecipes;
import com.chinaex123.redstone_chan_lib.api.collision.FluidCollisionRecipe;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import java.util.List;
import java.util.Optional;
```

### 示例：熔岩撞水

```java
CollisionRecipes.register("lava_water", new FluidCollisionRecipe(
        FluidIngredient.of(Fluids.LAVA),
        FluidIngredient.of(Fluids.WATER),
        List.of(
                CollisionOutput.block(Blocks.STONE),
                CollisionOutput.block(Blocks.OBSIDIAN, 1, 0.25F)
        ),
        1, true, true, false, SoundEvents.LAVA_EXTINGUISH));
```

构造器的参数顺序是“流体、对方原料、产出、延迟、是否消耗流入流体、是否消耗对方、是否消耗流体源头、音效”；后两个可以省略，省略时音效默认是水遇岩浆熄灭。

### 示例：岩浆撞金块，中间空格下方也得是金块

```java
CollisionRecipes.register("lava_gold_block", new BlockCollisionRecipe(
        FluidIngredient.of(Fluids.LAVA),
        BlockCollisionRecipe.blocks(Blocks.GOLD_BLOCK),
        List.of(CollisionOutput.block(Blocks.GOLD_ORE)),
        1, false, false, false, null,
        BelowCondition.center(BlockCollisionRecipe.blocks(Blocks.GOLD_BLOCK))));
```

摆成同一高度的 `金块 — 空气 — 岩浆`，并且中间那格空气的正下方也是金块，那格空气里就会冒出金矿石（岩浆和金块都不动）；直接把岩浆倒在金块上方/旁边、且那格脚下是金块，同样会触发（这时中间格就是岩浆自己那格）。

### 示例：岩浆撞绿宝石块，中间下方要有绿宝石块

```java
CollisionRecipes.register("lava_emerald_block", new BlockCollisionRecipe(
        FluidIngredient.of(Fluids.LAVA),
        BlockCollisionRecipe.blocks(Blocks.EMERALD_BLOCK),
        List.of(CollisionOutput.block(Blocks.EMERALD_ORE)),
        1, false, false, false, null,
        BelowCondition.center(BlockCollisionRecipe.blocks(Blocks.EMERALD_BLOCK))));
```

摆成同一高度的 `绿宝石块 — 空气 — 岩浆`，并且中间那格空气的正下方也是绿宝石块，那格空气里就会冒出绿宝石矿石（岩浆和绿宝石块都不动）；直接把岩浆倒在绿宝石块上方/旁边、且那格脚下是绿宝石块，同样会触发。下面换成别的方块就不触发。

### 示例：熔岩撞冰

```java
CollisionRecipes.register("lava_ice", new BlockCollisionRecipe(
        FluidIngredient.of(Fluids.LAVA),
        BlockCollisionRecipe.tag(net.minecraft.tags.BlockTags.ICE),
        List.of(
                CollisionOutput.block(Blocks.OBSIDIAN, 1, 0.5F),
                CollisionOutput.item(net.minecraft.world.item.Items.CHARCOAL, 2, 0.3F)
        ),
        5, true, true, SoundEvents.GLASS_BREAK));
```

### 示例：模组自带的六条测试配方

模组在构造阶段调用了 `BuiltinCollisionRecipes.register()`，里面已经注册了六份直接用 Java 写的示例，进游戏就能试：

```java
// 熔岩碰到钻石矿石（含深板岩变种）：必定掉一颗钻石，并把矿石消耗掉
CollisionRecipes.register("lava_diamond_ore", new BlockCollisionRecipe(
        FluidIngredient.of(Fluids.LAVA),
        BlockCollisionRecipe.blocks(Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE),
        List.of(CollisionOutput.item(Items.DIAMOND, 1, 1.0F)),
        1, false, true, Optional.empty()));

// 熔岩碰到钻石块：原地再放一个钻石块
CollisionRecipes.register("lava_diamond_block", new BlockCollisionRecipe(
        FluidIngredient.of(Fluids.LAVA),
        BlockCollisionRecipe.blocks(Blocks.DIAMOND_BLOCK),
        List.of(CollisionOutput.block(Blocks.DIAMOND_BLOCK)),
        1, false, false, Optional.empty()));

// 熔岩碰到远古残骸：50% 还是远古残骸，25% 下界石英矿石，25% 下界岩
CollisionRecipes.register("lava_ancient_debris", new BlockCollisionRecipe(
        FluidIngredient.of(Fluids.LAVA),
        BlockCollisionRecipe.blocks(Blocks.ANCIENT_DEBRIS),
        List.of(
                CollisionOutput.block(Blocks.ANCIENT_DEBRIS, 1, 0.5F),
                CollisionOutput.block(Blocks.NETHER_QUARTZ_ORE, 1, 0.25F),
                CollisionOutput.block(Blocks.NETHERRACK, 1, 0.25F)),
        1, false, true, Optional.of(SoundEvents.LAVA_EXTINGUISH)));

// 熔岩碰到金块，中间那格空的下方也是金块时，才变成金矿石
CollisionRecipes.register("lava_gold_block", new BlockCollisionRecipe(
        FluidIngredient.of(Fluids.LAVA),
        BlockCollisionRecipe.blocks(Blocks.GOLD_BLOCK),
        List.of(CollisionOutput.block(Blocks.GOLD_ORE, 1, 1.0F)),
        1, false, false, false, null,
        BelowCondition.center(Blocks.GOLD_BLOCK)));

// 熔岩碰到绿宝石块，中间那格空的下方也是绿宝石块时，才变成绿宝石矿石
CollisionRecipes.register("lava_emerald_block", new BlockCollisionRecipe(
        FluidIngredient.of(Fluids.LAVA),
        BlockCollisionRecipe.blocks(Blocks.EMERALD_BLOCK),
        List.of(CollisionOutput.block(Blocks.EMERALD_ORE, 1, 1.0F)),
        1, false, false, false, null,
        BelowCondition.center(Blocks.EMERALD_BLOCK)));

// 熔岩碰到石头，且熔岩自己脚下是岩浆块时，才变成岩浆块
CollisionRecipes.register("lava_stone_on_magma", new BlockCollisionRecipe(
        FluidIngredient.of(Fluids.LAVA),
        BlockCollisionRecipe.blocks(Blocks.STONE),
        List.of(CollisionOutput.block(Blocks.MAGMA_BLOCK, 1, 1.0F)),
        1, false, false, false, null,
        BelowCondition.down(Blocks.MAGMA_BLOCK)));
```

不想保留这六条示例的话，把 `RedstoneChanLIB` 构造器里的 `BuiltinCollisionRecipes.register()` 删掉即可。

### fluid_below / down / center 怎么写

这两个条件在 Java 里是 `BelowCondition` 的工厂方法，直接写方块或标签即可，不用自己拼 `HolderSet`：

```java
// 只要求“中间那格空的下方”是绿宝石块
BelowCondition.center(Blocks.EMERALD_BLOCK);

// 只要求“流体自己脚下”是岩浆块
BelowCondition.fluid(Blocks.MAGMA_BLOCK);

// 只要求“对方（方块那一边）脚下”是铁块
BelowCondition.down(Blocks.IRON_BLOCK);

// 也可以写标签：中间那格下方是任意钻石矿石
BelowCondition.center(BlockTags.DIAMOND_ORES);

// 三处一起限制：流体脚下岩浆块、对方脚下铁块、中间格下方黑曜石
BelowCondition.all(
        BlockCollisionRecipe.blocks(Blocks.MAGMA_BLOCK),
        BlockCollisionRecipe.blocks(Blocks.IRON_BLOCK),
        BlockCollisionRecipe.blocks(Blocks.OBSIDIAN));
```

写进配方时把它当最后一个参数传进去就行：

```java
CollisionRecipes.register("lava_emerald_block", new BlockCollisionRecipe(
        FluidIngredient.of(Fluids.LAVA),
        BlockCollisionRecipe.blocks(Blocks.EMERALD_BLOCK),
        List.of(CollisionOutput.block(Blocks.EMERALD_ORE)),
        1, false, false, false, null,
        BelowCondition.center(Blocks.EMERALD_BLOCK)));
```

三个工厂分别是 `fluid(...)`、`down(...)`、`center(...)`，要同时限制多处就写 `all(fluid, down, center)`（只写流体与对方用 `both(fluid, down)`）；不限制下方方块时传 `BelowCondition.NONE`；同样的东西写在数据包里，就是配方 JSON 顶层的 `fluid_below`、`down`、`center` 三个字段。判断是否满足用 `testFluid(BlockState)`、`testDown(BlockState)`、`testCenter(BlockState)`，全部为空用 `isEmpty()` 判断。

### 可以直接调用的方法

- `CollisionRecipes.register(ResourceLocation id, FluidCollisionRecipe recipe)`：注册一份流体碰撞配方。
- `CollisionRecipes.register(String path, FluidCollisionRecipe recipe)`：同上，id 自动加 `redstone_chan_lib:` 前缀。
- `CollisionRecipes.register(ResourceLocation id, BlockCollisionRecipe recipe)`：注册一份方块碰撞配方。
- `CollisionRecipes.register(String path, BlockCollisionRecipe recipe)`：同上，id 自动加前缀。
- `CollisionRecipes.javaFluidRecipes()` / `javaBlockRecipes()`：取当前注册的 Java 配方表。
- `CollisionRecipes.onFluidPlaced(Level level, BlockPos pos, BlockState newState, BlockState oldState)`：手动喂一次“流体流入”，正常游戏里由注入自动调用。
- `CollisionOutput.block(Block)` / `block(Block, float chance)` / `block(Block, int count, float chance)`：造一条方块产出。
- `CollisionOutput.item(Item)` / `item(Item, int count, float chance)` / `item(ItemStack, float chance)`：造一条物品产出。
- `BlockCollisionRecipe.blocks(Block... blocks)`：把若干方块直接包成一个方块集合，免去手写 `HolderSet.direct(...)`。
- `BlockCollisionRecipe.tag(TagKey<Block> tag)`：把方块标签包成方块集合，标签不存在时会抛异常。
- `BelowCondition.NONE`：不做任何下方方块限制。
- `BelowCondition.center(Block...)` / `center(TagKey<Block>)` / `center(HolderSet<Block>)`：要求中间那格空的下方是这些方块。
- `BelowCondition.down(Block...)` / `down(TagKey<Block>)` / `down(HolderSet<Block>)`：要求对方（方块那一边）脚下是这些方块。
- `BelowCondition.fluid(Block...)` / `fluid(TagKey<Block>)` / `fluid(HolderSet<Block>)`：要求流体自己脚下是这些方块。
- `BelowCondition.both(HolderSet<Block> fluid, HolderSet<Block> down)`：流体脚下与对方脚下同时限制（任一参数可以传 `null`）。
- `BelowCondition.all(HolderSet<Block> fluid, HolderSet<Block> down, HolderSet<Block> center)`：三处一起限制（任一参数可以传 `null`）。
- `BelowCondition.testFluid(BlockState)` / `testDown(BlockState)` / `testCenter(BlockState)` / `isEmpty()`：单独判断某个下方方块、或判断是否完全没限制。
- 常量：`CollisionOutput.DEFAULT_COUNT`、`DEFAULT_CHANCE`、`FluidCollisionRecipe.DEFAULT_DELAY`、`TEST_AMOUNT`。
- `BuiltinCollisionRecipes.register()`：模组自带的六条示例配方（上一条示例里的那六份），在构造阶段注册。

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

## 流体与气体注册（Java 调用）

想加一种流体，不用自己写流体类、流体方块、桶物品，调一次注册器就够了。

### 快速开始

在模组构造器里创建一个注册器，之后想注册什么就 `register` 什么：

```java
FluidRegistrar registrar = FluidRegistrar.create("mymod", modEventBus);

FluidEntry mana = registrar.register(FluidDefinition.builder("liquid_mana")
        .textures(
                ResourceLocation.fromNamespaceAndPath("mymod", "block/liquid_mana_still"),
                ResourceLocation.fromNamespaceAndPath("mymod", "block/liquid_mana_flow"))
        .tintColor(0xFF3080FF)
        .fogColor(0x3080FF)
        .fogDistance(24.0F)
        .density(1200)
        .viscosity(800)
        .temperature(280)
        .canExtinguish(true)
        .build());
```

这一下注册出来的东西是：流体类型（`mymod:liquid_mana`）、源流体（`mymod:liquid_mana`）、流动流体（`mymod:flowing_liquid_mana`）、流体方块（`mymod:liquid_mana`）和桶（`mymod:liquid_mana_bucket`）。

### 气体示例

把种类改成 `GAS`，密度给负数，它就会往上飘，并且过一段时间自己散掉：

```java
FluidEntry gas = registrar.register(FluidDefinition.builder("rising_gas")
        .kind(FluidKind.GAS)
                .density(-1000)
                .tintColor(0x60FFFFFF)
                .fogColor(0xC9E4F7)
                .fogDistance(2.0F)
                .tickRate(10)
                .gasDissipateTicks(100)
                .build());
```

气体的方块会给实体一个上半格的碰撞面，所以能站在气体表面；`gasDissipateTicks` 是它消失前能活多少刻（20 刻是一秒），上面还被堵着的话会每二十刻再等一次。

气体的高度与形状也是上下颠倒的：下面还是同一团气体时按满格算，形状从方块顶部往下长（满格时直接占满整格），往四周铺开时找的是上方还有没有出口，而不是原版那种往下找坑。

画面上的画法也跟着翻了：原版液体渲染器永远把流体画在方块下半部分，气体这块用 `client/fluid/GasFluidRenderer` 接过来自己画，贴着方块顶部往下长，看得见的那一面朝下，所以气泡是从天花板一路往下压、下面边缘是毛的。这一块是靠 `mixin/client/LiquidBlockRendererMixin` 在 `tesselate` 的 HEAD 处拦下来做的，只对 `RCLGasFluid` 生效，别的流体还是原版那套。

### 气体桶的两种倒法

默认的气体桶和普通桶一样，右键会把气体方块放出来。想让气体“一倒就散”，加 `gasBurst(true)`：

```java
FluidEntry vapor = registrar.register(FluidDefinition.builder("hot_vapor")
        .kind(FluidKind.GAS)
        .density(-400)
        .gasBurst(true)
        .build());
```

开了之后右键不会放下方块，只会在视线落点放一团云粒子、播一声熄灭的音效，桶变回空桶，看起来就像气体当场散掉了。旁观者、生存创造都一样处理，创造模式不会消耗桶。不写这个开关就是默认的“能倒出气体方块”。

### 一次注册多个

```java
registrar.register(
        FluidDefinition.builder("deep_water").translucent(true).blockLightLevel(3).build(),
        FluidDefinition.builder("hot_air").kind(FluidKind.GAS).density(-800).createBucket(false).build());
```

### 能设哪些参数

外观与客户端表现：

- `textures(still, flowing)`：静止贴图与流动贴图，默认就是原版水的 `block/water_still` 与 `block/water_flow`，所以完全可以不准备贴图，靠 `tintColor` 染色就行；想自己画再调这个
- `stillTexture`、`flowingTexture`：单独设其中一张
- `tintColor(argb)`：流体着色，带透明度，默认 `0xFFFFFFFF` 不变色
- `fogColor(rgb)`：进入流体时的雾气颜色，不带透明度
- `fogDistance(float)`：雾气最远距离，越小越糊，默认 96
- `fogEnabled(boolean)`：关掉就没有雾
- `translucent(boolean)`：用半透明还是实心渲染层，默认半透明
- `lightLevel(int)`、`blockLightLevel(int)`：流体类型与流体方块的发光等级

物理与行为：

- `density(int)`：密度，负数往上浮，默认 1000
- `viscosity(int)`：粘稠度，越大流得越慢
- `temperature(int)`：温度
- `fallDistanceModifier(float)`：摔落距离修正
- `motionScale(double)`：实体在里面的运动速度缩放
- `canHydrate`、`canConvertToSource`、`supportsBoating`、`canExtinguish`、`canPushEntity`、`canDrown`、`canSwim`：对应原版流体类型的各项开关

流动与爆炸：

- `slopeFindDistance(int)`：找斜坡的距离，默认 4
- `levelDecreasePerBlock(int)`：每格等级衰减，默认 1
- `tickRate(int)`：更新刻率，越小流得越快，默认 5
- `explosionResistance(float)`：爆炸抗性，默认 100

气体专用：

- `gasDissipateTicks(int)`：消散前存在的刻数，默认 200
- `gasBurst(boolean)`：气体桶右键时直接散成粒子、不放出气体方块，默认 false（照常倒出方块）

其它：

- `createBucket(boolean)`：要不要同时注册桶，默认要
- `translationKey(String)`：翻译键，默认是 `block.命名空间.注册名`

### 拿到手之后

`register` 返回一个 `FluidEntry`，需要什么从里面取：

- `id()`：完整注册名
- `definition()`：当初传进去的描述
- `type()`、`fluidType()`：流体类型
- `source()`、`sourceFluid()`：源流体
- `flowing()`、`flowingFluid()`：流动流体
- `block()`、`fluidBlock()`：流体方块
- `bucket()`、`bucketItem()`：桶物品，没注册桶时 `bucket()` 是 null

注册器本身还有 `entries()` 和 `namespace()`。

### 资源文件：两种路子

贴图有两条路，挑一条就行。

**第一种，什么都不准备**：不写 `textures(...)`，流体就用原版水的静止与流动贴图，颜色由 `tintColor` 乘出来（水贴图本身是灰度的，染成什么色就是什么色）。雾气和发光照样生效。示例里的液态红石走的就是这条路。

**第二种，自己画贴图**：把两张灰度图放到 `assets/命名空间/textures/block/` 下面，再用 `textures(静止图, 流动图)` 指过去，静态图一般 16×16，流动图按原版习惯是 16×16 竖向多帧。示例里的红石气体走的就是这条路。

不管走哪条，下面这些还是要补：

- 流体方块不需要 blockstate 文件，原版靠流体渲染器画
- 桶物品需要一个模型，父模型二选一：想让它像原版水桶那样把流体装在桶里，用 `neoforge:item/bucket`；想让桶沿挂着流体，用 `neoforge:item/bucket_drip`（液体类的桶一般用这个），再让流体着色模型接管颜色
- 模型里的 `flip_gas` 是“桶要不要倒扣着拿”的开关：只有流体比空气轻（密度小于 0）并且写了 `true`，NeoForge 才会把整个模型转 180 度；液体桶写 `false`，气体桶写 `true`，看起来就是气体把桶往上顶、只能倒着拎
- 语言文件里补 `block.命名空间.注册名` 和 `item.命名空间.注册名_bucket`

用数据生成的话，桶的模型可以用现成的小工具：

```java
FluidModels.bucket(provider, item);         // 流体装在桶里
FluidModels.dripBucket(provider, item);     // 桶沿挂着流体，液体类的桶用这个
FluidModels.floatingBucket(provider, item); // 倒扣着往外飘，只给比空气轻的流体
```

### 关于访问转换器

气体要复用原版流体的扩散逻辑，而原版把几个方法写成了私有，所以库里带了一份 `META-INF/accesstransformer.cfg`，把那几个方法放宽到 `protected`。这份文件会跟着模组一起加载，对别的模组没有影响，只是让它们也能重写这几个方法。

### 自带示例

库本身注册了两份示例流体，装进游戏就能直接看效果：

- `redstone_chan_lib:liquid_redstone`（液态红石）：没带自己的贴图，直接用原版水的贴图染红，发光的红色稠液体，流动慢、流得近，泡在里面会有暗红的雾气。这只桶用的是滴落模型，桶沿挂着流体。
- `redstone_chan_lib:redstone_gas`（红石气体）：自带两张灰度贴图，通过 `textures(...)` 指过去，半透明，密度是负的，会一路往天上飘，飘一会儿自己散掉，贴进去画面会被淡红的雾包住。这只桶是普通气体桶，右键会倒出气体方块。
- `redstone_chan_lib:redstone_vapor`（红石蒸汽）：和气体共用贴图，区别是开了 `gasBurst(true)`，右键只放一团云粒子、播一声熄灭的音效，不会留下方块。

两只桶都在创造模式物品栏的“流体示例”分区里。想改配色或手感，直接看 `init/RCLDemoFluids.java`，那里就是一份完整用法，改完重新构建即可。
