# Usage & Examples

This document supplements [README_EN.md](README_EN.md) and covers "how to actually write it": the datapack JSON fields and examples for portals, Java registration and callable methods for portals, and how to invoke entity outlines and block outlines.

A portal can be defined in two ways with exactly the same result, and the two can be mixed: in a datapack (good for modpacks and for players who want to tweak things themselves), or in Java (good for addon mods). A datapack definition overrides the Java definition with the same id.

## Portals (datapack)

### Where the file goes

Create a JSON file under `data/<namespace>/redstone_chan_lib/portal/`; the file name without `.json` is the definition id. For example, `data/example/redstone_chan_lib/portal/moon.json` corresponds to `example:moon`.

Run `/reload` after editing and it takes effect, with no game restart needed. A datapack definition with the same id overrides the built-in Java definition, so to change how the test portal behaves, just edit the corresponding JSON.

### Field reference

- `frame_blocks`: required, an array of frame blocks. You can list block ids (such as `minecraft:diamond_block`) or block tags (starting with `#`, such as `#minecraft:stone_bricks`)
- `catalyst`: optional, the activation method. You can write a single string or an array of strings (matching any one of them lights the portal). String forms:
    - `item:minecraft:diamond`: holding the specified item
    - `item:#minecraft:coals` or just `#minecraft:coals`: holding any item in the tag
    - `fluid:minecraft:water`: holding a container filled with the specified fluid (a water bucket, a lava bucket, any tank with a fluid capability will do)
    - `fluid:#minecraft:water`: holding a container filled with any fluid in the tag
    - when the `item:` prefix is omitted, it is treated as an item
    - an empty string means right-clicking with an empty hand lights it; omitting the whole field is also equivalent to an empty hand
- `dimension`: optional, the target dimension id, defaulting to `minecraft:the_nether`
- `orientation`: optional, the placement type. `any` (default, both vertical and flat portals can be activated), `vertical` (only vertical portals are recognized), `horizontal` (only portals lying flat on the ground are recognized)
- `color`: optional, the color. Supports `#RRGGBB` and `#AARRGGBB`; when omitted, the mod's default purple `#730DFF` is used. Tinting is multiplicative and the texture itself is grayscale, so whatever color you write is the color you get (only R, G, B are used; the A channel does not take part in rendering)
- `name`: optional, the name of the portal block (plain text)
- `translate`: optional, the translation key for the portal block's name; takes priority over `name`
- `min_width`, `min_height`: optional, the minimum interior width and height, defaulting to 2 and 3
- `max_width`, `max_height`: optional, the maximum interior width and height, defaulting to 21
- `wait_ticks`: optional, how many ticks to wait inside the portal before teleporting, defaulting to 80
- `ritual`: optional, the ritual activation configuration. Written as an object containing:
    - `fluid`: the fluid that must fill the empty area, such as `minecraft:water`
    - `fluid_tag`: specify the empty area's fluid with a fluid tag, such as `#minecraft:water`
    - `width`, `depth`: the size of the empty area, defaulting to 2 and 2
    - `item`: the item that triggers it only when thrown into the fluid; the syntax is the same as `catalyst` (the `item:` prefix, `#` tags, and string arrays all work); omitting it means any item
    - `require_all_frame_types`: when `true`, every frame block (or tag) in the definition must appear at least once on the frame; default `false`

`min_width` and `min_height` refer to the **interior** size (the ring of frame blocks is not counted), so the defaults of 2 and 3 are exactly the size of a vanilla Nether portal; `max_width` and `max_height` are likewise interior sizes.

### Example: diamond block + diamond (the mod's bundled `test.json`)

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

### Example: a colored custom-dimension portal

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

### Example: a horizontal portal lying flat on the ground (the mod's bundled `test_flat.json`)

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

### Example: ritual portal (the mod's bundled `test_ritual.json`)

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

Layout: dig a 2×2 pit one block deep and fill it with water, plant flowers on the top faces of the ring of blocks around it, and throw a diamond into the water; once the diamond sinks, lightning strikes and the water surface turns into a horizontal portal.

## Portals (Java API)

- The public API is split across two packages. Portal-related classes live in `com.chinaex123.redstone_chan_lib.api.portal`, twelve classes in total: `PortalDefinition` (definition), `PortalCatalyst` (activation method), `PortalRitual` (ritual configuration), `PortalDefinitions` (registry), `PortalShape` (shape detection), `PortalOrientation` (placement type), `PortalStructureHelper` (placement and removal), `PortalPlacement` (cross-dimension positioning), `PortalTeleport` (teleport execution), `PortalActivation` (activation flow), `PortalRitualActivation` (ritual activation flow), `PortalEffects` (effects and sounds); the message utility `com.chinaex123.redstone_chan_lib.util.PortalTexts` is also within the public surface. Entity outlines live in `com.chinaex123.redstone_chan_lib.api.outline`, with two classes: `EntityOutline` (entity outlines) and `BlockOutline` (block outlines). See the `package-info.java` in each directory for the package documentation.
- This is not a separately written "for external use" implementation: the mod's own right-click lighting, breaking to dismantle a portal, teleport countdown, and occlusion mask all call the same methods in these very classes. So what an external mod gets from calling `PortalActivation.tryActivate(...)` is exactly the same as when a player lights it by right-click.
- Addon mods or modpack scripts can register built-in definitions during the mod construction stage. The imports used by the examples below:

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

Register a complete definition:

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

The frame can also be configured by tag:

```java
PortalDefinition.builder(ResourceLocation.fromNamespaceAndPath("example", "bricks"))
        .frameTag(BlockTags.STONE_BRICKS)
        .catalyst(Items.FLINT_AND_STEEL)
        .build();
```

Activation methods can be stacked, for example "diamond or water both work":

```java
PortalDefinition.builder(ResourceLocation.fromNamespaceAndPath("example", "diamond"))
        .frameBlock(Blocks.DIAMOND_BLOCK)
        .catalyst(Items.DIAMOND)    // item
        .itemTagCatalyst(ItemTags.COALS)    // item tag
        .fluidCatalyst(Fluids.WATER)    // fluid
        .fluidTagCatalyst(FluidTags.LAVA)   // fluid tag
        .build();
```

When no `catalyst(...)` method is called, it lights by right-clicking with an empty hand.

To restrict the placement type, for example to accept only portals lying flat on the ground (like the End portal):

```java
PortalDefinition.builder(ResourceLocation.fromNamespaceAndPath("example", "flat"))
        .frameBlock(Blocks.GOLD_BLOCK)
        .catalyst(Items.DIAMOND)
        .orientation(PortalOrientation.HORIZONTAL)
        .build();
```

The three values of `PortalOrientation`: `ANY` (default, both vertical and flat are fine), `VERTICAL` (only vertical portals), `HORIZONTAL` (only flat portals).

For ritual activation (Twilight Forest style), attach a `PortalRitual` to the definition:

```java
PortalDefinitions.register(
        PortalDefinition.builder(ResourceLocation.fromNamespaceAndPath("example", "ritual"))
                .frameTag(BlockTags.FLOWERS)    // flowers planted on the top faces of the ring of blocks around the pool
                .dimension(Level.NETHER)
                .orientation(PortalOrientation.HORIZONTAL)
                .ritual(PortalRitual.builder()
                        .fluid(Fluids.WATER)    // the fluid in the empty area
                        .size(2, 2) // size of the empty area
                        .item(Items.DIAMOND)    // the item thrown in
                        .build())
                .build()
);
```

The ritual portal's frame blocks sit on top of the blocks around the pool, which is why the example uses the flower tag: a 2×2 pool, flowers planted on top of the ring of blocks around it, and throwing a diamond in strikes lightning and lights it.

`PortalRitual` can also use `fluidTag(FluidTags.WATER)` to specify a fluid tag, `itemTag(ItemTags.COALS)` to specify an item tag, and `requireAllFrameTypes(true)` to require the frame to use every block type listed in the definition. When `item(...)` is not called, any item can trigger the ritual.

A registered definition can be queried with `PortalDefinitions.get(id)`, and `PortalDefinitions.all()` lists all currently valid definitions.

### Methods you can call directly

Below are the public method signatures grouped by purpose; in the parameters, `def` is always a `PortalDefinition` and `level` is a `Level` (pass `ServerLevel` on the server side).

Registry `PortalDefinitions`:

- `register(PortalDefinition def)`: registers or replaces a built-in definition.
- `get(ResourceLocation id)`: gets a definition by id; returns `null` if there is none.
- `all()`: all currently valid definitions (a datapack definition overrides a built-in one with the same id).
- `matchingFrames(BlockState state)`: which definitions this block can serve as the frame for.
- `isAnyFrame(BlockState state)`: whether this block is the frame of any definition.
- `hasRituals()`: whether any definition currently configures ritual activation; dropped-item detection uses it for a fast skip.
- `defaultDefinition()`: the default definition, which is what the block name display uses.

Definition `PortalDefinition`:

- `builder(ResourceLocation id)`: creates a new definition builder.
- Methods available on `Builder`: `frameBlock(Block)`, `frameTag(TagKey<Block>)`, `catalyst(Item)`, `catalyst(PortalCatalyst)`, `catalysts(Collection<PortalCatalyst>)`, `itemTagCatalyst(TagKey<Item>)`, `fluidCatalyst(Fluid)`, `fluidTagCatalyst(TagKey<Fluid>)`, `color(int)`, `name(Component)`, `dimension(ResourceKey<Level>)`, `size(int minWidth, int minHeight, int maxWidth, int maxHeight)`, `waitTicks(int)`, `orientation(PortalOrientation)`, `ritual(PortalRitual)`, `build()`.
- Reading a definition: `id()`, `frameBlocks()`, `frameTags()`, `catalysts()`, `color()`, `name()`, `dimension()`, `minWidth()`, `minHeight()`, `maxWidth()`, `maxHeight()`, `waitTicks()`, `orientation()`, `ritual()`.
- Checks: `isFrame(BlockState state)`, `isCatalyst(ItemStack stack)`, `matchingCatalyst(ItemStack stack)` (returns the matched activation method, or `null` if nothing matches), `allowsAxis(Direction.Axis axis)`, `isSizeAllowed(int width, int height)`, `getBuildFrameBlock()` (the block used to lay the frame when building a portal automatically).

Shape detection `PortalShape`:

- `find(Level level, BlockPos framePos, PortalDefinition def)`: starting from any one frame block, outlines the entire portal; returns `null` if the shape is invalid. The four corners may be left empty.
- `compose(BlockPos min, Direction.Axis axis, int width, int height)`: constructs the shape directly from a known frame bounds and interior size without doing a flood fill (this is what lighting a ritual portal uses).
- `connectedPortalBlocks(BlockGetter level, BlockPos start, Block block)`: collects connected portal blocks into a list.
- `isFramePosition(BlockGetter level, BlockPos pos, BlockPos portalPos, Direction.Axis axis, Block block)`: whether a coordinate is part of this portal's actual outer-ring frame (only the four sides are considered; the four corners do not count).
- `min()`, `max()`, `axis()`, `width()`, `height()`, `isHorizontal()`, `interiorCenter()`, `interiorMin()`, `interiorPositions()`, `isInside(BlockPos pos)`: accessors once you have the shape. `axis()` being `X` or `Z` means a vertical portal, and `Y` means a flat horizontal portal.

Placement and removal `PortalStructureHelper`:

- `fillPortal(Level level, PortalShape shape, PortalDefinition def)`: fills the interior of an already valid frame with portal blocks.
- `buildPortal(Level level, BlockPos min, Direction.Axis axis, int width, int height, PortalDefinition def)`: builds a new portal according to the definition; `min` is the frame's minimum corner.
- `destroyConnectedPortal(Level level, BlockPos start)`: destroys the entire portal connected to `start`, keeping the outer frame, with a break sound for every block.
- `isAreaSuitable(Level level, BlockPos min, Direction.Axis axis, int width, int height)`: whether this area can hold a portal.

Cross-dimension positioning `PortalPlacement`:

- `findOrCreatePortal(ServerLevel target, ServerLevel source, PortalDefinition def, BlockPos sourcePos)`: finds the same kind of portal in the target dimension, builds one on the spot if there is none, and returns the interior coordinate where the player should stand.
- `findOrCreatePortal(ServerLevel target, ServerLevel source, PortalDefinition def, BlockPos sourcePos, Direction.Axis axis)`: same as above, and specifies which orientation the newly built portal uses (`Y` for a horizontal portal, `X`/`Z` for a vertical one, `null` to choose automatically according to the definition).
- `createPortal(ServerLevel level, BlockPos base, PortalDefinition def)` and the overload with `Direction.Axis axis`: builds a portal directly at the given position.

Teleport execution `PortalTeleport`:

- `resolve(ServerPlayer player, PortalDefinition def)`: computes the destination (a `Destination` record holding the dimension and coordinates); returns `null` if it cannot be computed.
- `resolve(ServerPlayer player, PortalDefinition def, Direction.Axis axis)`: same as above, and makes a newly built target portal follow the given orientation.
- `move(ServerPlayer player, Destination destination)`: moves the player across, along with the arrival effects.
- `teleport(ServerPlayer player, PortalDefinition def)`: equivalent to `resolve` + `move`; returns `true` on success.

Activation and effects `PortalActivation`, `PortalRitualActivation`, `PortalEffects`:

- `PortalActivation.tryActivate(Player player, BlockPos framePos)`: attempts to light it with the main-hand item; returns whether it succeeded.
- `PortalActivation.tryActivate(Level level, BlockPos framePos, Player player, InteractionHand hand, ItemStack stack)`: the version with an explicit hand and item; returns the definition that was successfully lit, or `null`.
- `PortalRitualActivation.tryActivate(ServerLevel level, ItemEntity itemEntity)`: determines whether this dropped item has landed in the fluid pool of some ritual portal; on a hit it lights it and returns `true`. The mod's dropped-item detection calls it every tick.
- `PortalEffects.playActivationEffects(Level level, BlockPos pos)`: plays the sound and particles for lighting.
- `PortalEffects.playArrivalEffects(ServerPlayer player, BlockPos targetPos)`: plays the sound on arrival.

Message texts `PortalTexts`:

- `PortalTexts.text(String key, Object... args)`: gets a plain string localized to the language, used for exception messages and logs; the key carries no prefix, for example `PortalTexts.text("unknown_fluid", value)`.
- `PortalTexts.component(String key, Object... args)`: gets a text component, convenient for putting into a UI or a tooltip.
- Keys uniformly start with `message.redstone_chan_lib.` (`PortalTexts.PREFIX`) and are written in the mod's own language files; other mods or modpacks can also override keys with the same name to rewrite the messages.

## Mob Outlines (Java API)

An outline only affects rendering; it does not change any entity attributes. However, the fact that "this entity has an outline" is recorded in the server-side save data, so **calling on the server is recommended**:
A server-side call writes to the save file and syncs the setting to every player who can see that mob, so the outline is still there after a player logs out and back in, or after the chunk is reloaded.
Calling on the client only lasts for the current session and is gone the next time you join; that is fine for purely local visual effects.

Imports used in the examples below:

```java
import com.chinaex123.redstone_chan_lib.api.outline.EntityOutline;
import net.minecraft.world.entity.Entity;
```

Add an outline to a mob:

```java
// Glow outline, white by default
EntityOutline.apply(entity, EntityOutline.Mode.GLOW);
// Glow outline with a custom color (solid)
EntityOutline.apply(entity, EntityOutline.Mode.GLOW, 0xFF7CF9FF);
// Glow outline, multicolored (the hue is spread along the contour, so several colors are visible at once)
EntityOutline.apply(entity, EntityOutline.Mode.GLOW, 0xFFFFFFFF, EntityOutline.Style.RAINBOW);
// Glow outline with an explicit thickness (in blocks; 0.08 is the default, larger values are thicker)
EntityOutline.apply(entity, EntityOutline.Mode.GLOW, 0xFF7CF9FF, EntityOutline.Style.SOLID, 0.3);
// Glow outline, thickness 0.3, and not drawn when blocked by blocks (the last parameter is "through walls")
EntityOutline.apply(entity, EntityOutline.Mode.GLOW, 0xFF7CF9FF, EntityOutline.Style.SOLID, 0.3, false);
// Black outline
EntityOutline.apply(entity, EntityOutline.Mode.BLACK);
// Highlight effect
EntityOutline.apply(entity, EntityOutline.Mode.HIGHLIGHT);
// Remove
EntityOutline.clear(entity);
```

### Methods you can call directly

- `apply(Entity entity, Mode mode)`: adds an outline by mode; glow and highlight use the default white, black mode uses black.
- `apply(Entity entity, Mode mode, int rgb)`: same as above, with an explicit color; only the low 24 bits are used.
- `apply(Entity entity, Mode mode, int rgb, Style style)`: same as above, with a solid `Style.SOLID` or multicolored `Style.RAINBOW` style; thickness uses the default `EntityOutline.DEFAULT_THICKNESS` (0.08 blocks).
- `apply(Entity entity, Mode mode, int rgb, Style style, double thickness)`: also sets the thickness, in blocks, meaning how far the outline may bulge outward at most; larger is thicker. Black mode automatically narrows to eighty percent so it looks more like an outline than an outer glow.
- `apply(Entity entity, Mode mode, int rgb, Style style, double thickness, boolean throughWalls)`: adds "through walls". With `true` the outline is visible even when the mob hides behind a wall (the default is `EntityOutline.DEFAULT_THROUGH_WALLS`); with `false`, the outline of that mob is not drawn when the line from the player's eyes to the mob is blocked by blocks.
- `clear(Entity entity)`: removes the outline from one mob.
- `clearAll()`: removes all outlines.
- `isActive(Entity entity)`: whether this mob currently has an outline.
- `get(Entity entity)`: gets the current outline settings (an `Outline` record with mode, color, solid/multicolored, thickness, and through-walls), or `null` if absent.
- `activeEntities()`: all mobs that currently have an outline.
- `defaultColor(Mode mode)`: the default color of a mode.
- The modes are the three values of `EntityOutline.Mode`: `GLOW` (glow), `BLACK` (black), and `HIGHLIGHT` (highlight); solid/multicolored are `SOLID` and `RAINBOW` of `EntityOutline.Style`. Multicolored does not recolor the whole mob at once; instead the hue is spread by screen position, so at any instant the halo contains red, yellow, green and blue, and it slowly flows over time.

## Block Outlines (Java API)

Outlining a block uses the same API as outlining a mob; the only difference is that the target is a "dimension + position" instead of an entity. Here too, **calling on the server is recommended**: it writes to that dimension's save data and syncs to nearby players.

Imports used in the examples below:

```java
import com.chinaex123.redstone_chan_lib.api.outline.BlockOutline;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
```

Add an outline to a block:

```java
// Glow outline, white by default
BlockOutline.apply(level, pos, BlockOutline.Mode.GLOW);
// Glow outline with a custom color (solid)
BlockOutline.apply(level, pos, BlockOutline.Mode.GLOW, 0xFF7CF9FF);
// Black outline
BlockOutline.apply(level, pos, BlockOutline.Mode.BLACK);
// Highlight effect
BlockOutline.apply(level, pos, BlockOutline.Mode.HIGHLIGHT);
// Create style: appears thin, grows to the thickest, holds a moment, then thins out and disappears
BlockOutline.apply(level, pos, BlockOutline.Mode.CREATE, 0xFF65CAFC);
// Create style, shown permanently (does not fade away on its own)
BlockOutline.apply(level, pos, BlockOutline.Mode.CREATE, 0xFF65CAFC, true, 1.5);
// Create style, shown for 3 seconds and then thinning out and fading
BlockOutline.apply(level, pos, BlockOutline.Mode.CREATE, 0xFF65CAFC, false, 3.0);
// Multicolored glow outline
BlockOutline.apply(level, pos, BlockOutline.Mode.GLOW, 0xFFFFFFFF, BlockOutline.Style.RAINBOW);
// Explicit thickness and "not drawn when occluded"
BlockOutline.apply(level, pos, BlockOutline.Mode.GLOW, 0xFF7CF9FF, 0.1, false);
// Wireframe: the thinnest possible line, good for marking many blocks at once
BlockOutline.apply(level, pos, BlockOutline.Mode.WIRE, 0xFF7CF9FF);
// Remove
BlockOutline.clear(level, pos);
```

### Methods you can call directly

- `apply(Level level, BlockPos pos, Mode mode)`: adds an outline to a block by mode; glow and highlight use the default white, black mode uses black, Create style uses Create's blue `0xFF65CAFC`, and wireframe uses white.
- `apply(Level level, BlockPos pos, Mode mode, int rgb)`: same as above, with an explicit color; only the low 24 bits are used.
- `apply(Level level, BlockPos pos, Mode mode, int rgb, double thickness, boolean throughWalls)`: also sets the thickness and "through walls". Thickness is in blocks, converted to screen pixels as "one block = 100 pixels": `GLOW`/`BLACK`/`HIGHLIGHT` default to 0.045 blocks (about 4.5 pixels, clamped to 1.5~16 pixels), and `WIRE` defaults to 0.01 blocks (1 pixel, clamped to 1~4 pixels).
- `apply(Level level, BlockPos pos, Mode mode, int rgb, Style style)`: sets the color style; `SOLID` is a single color and `RAINBOW` is multicolored. As with mobs, multicolored spreads the hue by position, so a single box shows several colors at once and they slowly flow.
- `apply(Level level, BlockPos pos, Mode mode, int rgb, boolean keepVisible, double holdSeconds)`: for Create style; controls "shown permanently" and "how many seconds it stays visible".
- `apply(Level level, BlockPos pos, Mode mode, int rgb, Style style, double thickness, boolean throughWalls, boolean keepVisible, double holdSeconds)`: every parameter specified explicitly.
- `clear(Level level, BlockPos pos)`: removes the outline from a block.
- `isActive(Level level, BlockPos pos)`: whether this block currently has an outline.
- `get(Level level, BlockPos pos)`: gets the current outline settings (an `Outline` record with mode, color, style, thickness, through-walls, shown-permanently, and hold seconds), or `null` if absent.
- `activePositions()`: the positions of all blocks with an outline on the current client.
- `defaultColor(Mode mode)`: the default color of a mode.
- `defaultThickness(Mode mode)`: the default thickness of a mode; Create style is `CREATE_THICKNESS` (0.09 blocks), wireframe is `WIRE_THICKNESS` (0.01 blocks), and the other modes are 0.045 blocks.
- The modes are the five values of `BlockOutline.Mode`: `GLOW` (glow), `BLACK` (black), `HIGHLIGHT` (highlight), `CREATE` (Create style), and `WIRE` (wireframe).

## Block Scan Highlight (Java API)

`BlockOutline` means "put an outline on this specific position", while `BlockScanOutline` means "give it a recipe, scan the blocks around the player that match, and highlight them all" — the effect used by ore-finding mods: once you have it, the surrounding ores light up automatically.

The scan is centered on the player and the radius is up to you; highlighting reuses the modes above and **defaults to `WIRE` wireframe with through-walls enabled** — the whole point of prospecting is to see ores through walls.

Imports used in the examples below:

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

Use block tags as the target to highlight all surrounding ores:

```java
BlockScanOutline.Target ores = BlockScanOutline.target(
        BlockTags.COAL_ORES, BlockTags.IRON_ORES, BlockTags.GOLD_ORES, BlockTags.DIAMOND_ORES);
BlockScanOutline.start(level, ores, BlockOutline.Mode.GLOW, 0xFF7CF9FF, 16);
```

Use blocks as the target to mark only two kinds of blocks:

```java
BlockScanOutline.Target machines = BlockScanOutline.target(Blocks.FURNACE, Blocks.CRAFTING_TABLE);
BlockScanOutline.start(level, machines, 24);
```

Full configuration: mode, color style, thickness, radius, through-walls, per-block colors, and per-tag colors:

```java
BlockScanOutline.Scan scan = new BlockScanOutline.Scan(
        BlockOutline.Mode.WIRE,       // highlight mode; any of the five works
        0xFF7CF9FF,                   // fallback color (used by blocks that match neither color map)
        BlockOutline.Style.RAINBOW,   // solid or multicolored
        0.05D,                        // thickness (blocks)
        32,                           // scan radius
        true,                         // through walls (prospecting usually wants this on)
        Map.of(),                     // per-block colors
        Map.of());                    // per-tag colors
BlockScanOutline.start(level, ores, scan);
```

Prefer tags for colors; tags automatically pick up each mod's equivalent ores through the ore dictionary:

```java
// The built-in set: c:ores/coal, c:ores/iron ... c:ores/netherite_scrap
BlockScanOutline.Scan colorful = scan.withTagColors(BlockScanOutline.oreTagColors());

// Then give one specific tag its own color (redstone ore here as an example)
BlockScanOutline.Scan tuned = colorful.withTagColor(BlockTags.REDSTONE_ORES, 0xFFFF4040);
BlockScanOutline.start(level, ores, tuned);
```

If a mod's ores are not in any ore dictionary tag, you can fall back to the block id; per-block colors take priority over per-tag colors:

```java
BlockScanOutline.Scan exact = tuned.withColor(myModOre, 0xFF00FFE0);
```

Blocks that match neither map fall back to `rgb`, so "fallback color", "colors by tag" and "colors by block" can all be used at the same time.

The easier route, though, is the builder: list the colors one by one at the call site instead of assembling a `Map` yourself:

```java
BlockScanOutline.Scan scan = BlockScanOutline.scan(BlockOutline.Mode.WIRE, 0xFF7CF9FF)
        .radius(32)                                  // scan radius
        .thickness(0.04D)                            // thickness (blocks)
        .throughWalls(true)                          // through walls; turn on for prospecting
        .style(BlockOutline.Style.SOLID)             // solid or multicolored
        .oreDefaults()                               // lay down the built-in ore dictionary colors first
        .color(BlockTags.REDSTONE_ORES, 0xFFFF4040)  // colors by tag; add as many as you like
        .color(Blocks.DEEPSLATE_DIAMOND_ORE, 0xFF9CFFF2) // colors by block, higher priority
        .build();
BlockScanOutline.start(level, ores, scan);
```

If the colors already live in a list (read from config, for example), you can also pass them in all at once:

```java
List<BlockScanOutline.ColorRule> rules = List.of(
        BlockScanOutline.ColorRule.of(BlockTags.IRON_ORES, 0xFFD8AF93),
        BlockScanOutline.ColorRule.of(BlockTags.GOLD_ORES, 0xFFFCEE4B),
        BlockScanOutline.ColorRule.of(myModOre, 0xFF00FFE0));

BlockScanOutline.Scan scan = BlockScanOutline.scan(BlockOutline.Mode.WIRE, 0xFF7CF9FF)
        .colors(rules)
        .build();
```

Rules added later override rules added earlier; `oreDefaults()` only fills in tags that do not have a color yet, so the colors you specify yourself are not overwritten by it.

Stop the scan:

```java
BlockScanOutline.stop(level);
```

### Methods you can call directly

- `target(List<Block> blocks, List<TagKey<Block>> tags)`: scan target; blocks and tags can be mixed.
- `target(Block... blocks)`: blocks only.
- `target(TagKey<Block>... tags)`: tags only.
- `start(Level level, Target target, int radius)`: wireframe mode, default color, through walls, radius only.
- `start(Level level, Target target, Mode mode, int rgb, int radius)`: explicit mode and color.
- `start(Level level, Target target, Scan scan)`: every parameter specified explicitly.
- `stop(Level level)`: stops the scan in this dimension.
- `get(Level level)`: gets the current scan configuration (a `Scan` record), or `null` if absent.
- `Target.matches(BlockState state)`: whether a given block state matches the target.
- `Scan.clamped()`: clamps radius and thickness to their legal ranges (radius 1~48 blocks).
- `Scan.withColors(Map<Block, Integer> colors)`: replaces the entire per-block color map.
- `Scan.withColor(Block block, int rgb)`: gives one kind of block its own color (appended to the existing map).
- `Scan.withTagColors(Map<TagKey<Block>, Integer> tagColors)`: replaces the entire per-tag color map.
- `Scan.withTagColor(TagKey<Block> tag, int rgb)`: gives a tag a color; every block in that tag uses it.
- `colorOf(Scan scan, BlockState state)`: computes the effective color by the priority "per-block colors -> per-tag colors -> fallback color".
- `oreTagColors()`: a built-in set of ore tag colors, using NeoForge's `c:ores/xxx` tags (coal, iron, copper, gold, redstone, lapis, diamond, emerald, quartz, ancient debris).
- `scan(Mode mode, int rgb)`: starts building a scan configuration and returns a `Builder`, for listing colors right at the call site.
- `Builder.radius(int)` / `thickness(double)` / `throughWalls(boolean)` / `style(Style)`: set the individual parameters.
- `Builder.color(TagKey<Block> tag, int rgb)`: colors by tag; can be chained many times.
- `Builder.color(Block block, int rgb)`: colors by block, with higher priority than tags.
- `Builder.colors(List<ColorRule> rules)`: passes a whole list of rules at once.
- `Builder.oreDefaults()`: lays down `oreTagColors()` while keeping existing colors.
- `Builder.build()`: produces the `Scan`.
- `ColorRule.of(TagKey<Block> tag, int rgb)` / `ColorRule.of(Block block, int rgb)`: one rule in the list form.
- Constants: `DEFAULT_RADIUS` (16 blocks), `MAX_RADIUS` (48 blocks).

### How do I get other mods' ores to have colors too

Three approaches, from easiest to most precise:

- Just use `oreTagColors()`: as long as that mod added its ores to `c:ores/xxx` (the NeoForge ore dictionary; almost every mod does), they get a matching color automatically, with no code changes
- It uses its own tag (such as `mymod:ores/tin`): `scan.withTagColor(TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("mymod", "ores/tin")), 0xFFC0C0C0)`
- It did not even add tags: `scan.withColor(BuiltInRegistries.BLOCK.get(...), 0xFFC0C0C0)`, specifying directly by block id

All of these maps are saved and synced to the client along with the scan configuration, so a server-side call shows everyone the same colors.

## Entity Scan Highlight (Java API)

`EntityScanOutline` is the counterpart of block scanning: block scanning finds blocks, this one finds entities — it scans the entities around the player that match (entity type or entity tag) and draws each entity's bounding box as a wireframe. Same idea as before, "once you have it, the nearby targets light up automatically", and likewise it only scans while there are targets.

Usage is almost identical to block scanning, except the targets are `EntityType` and `EntityType` tags:

```java
import com.chinaex123.redstone_chan_lib.api.outline.BlockOutline;
import com.chinaex123.redstone_chan_lib.api.outline.EntityScanOutline;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
```

Use entity types as the target:

```java
EntityScanOutline.Target zombies = EntityScanOutline.target(EntityType.ZOMBIE, EntityType.SKELETON);
EntityScanOutline.start(level, zombies, 24);
```

Use entity tags as the target (arthropods here as an example):

```java
EntityScanOutline.Target arthropods = EntityScanOutline.target(EntityTypeTags.ARTHROPOD);
EntityScanOutline.start(level, arthropods, 0xFFFFC966, 16);
```

Full configuration:

```java
EntityScanOutline.Scan scan = new EntityScanOutline.Scan(
        0xFFFFC966,                   // wireframe color
        BlockOutline.Style.SOLID,     // solid or multicolored
        0.01D,                        // line width (blocks, converted to 1~4 pixels)
        32,                           // scan radius
        true);                        // through walls
EntityScanOutline.start(level, zombies, scan);
```

Stop the scan:

```java
EntityScanOutline.stop(level);
```

### Methods you can call directly

- `target(List<EntityType<?>> types, List<TagKey<EntityType<?>>> tags)`: scan target; types and tags can be mixed.
- `target(EntityType<?>... types)`: entity types only.
- `target(TagKey<EntityType<?>>... tags)`: entity tags only.
- `start(Level level, Target target, int radius)`: white wireframe, through walls, radius only.
- `start(Level level, Target target, int rgb, int radius)`: explicit color and radius.
- `start(Level level, Target target, Scan scan)`: every parameter specified explicitly.
- `stop(Level level)`: stops the entity scan in this dimension.
- `get(Level level)`: gets the current scan configuration, or `null` if absent.
- `Target.matches(Entity entity)`: whether an entity matches the target (dead entities are skipped automatically).
- `Scan.clamped()`: clamps radius and line width to their legal ranges (radius 1~64 blocks).
- Constants: `DEFAULT_RADIUS` (16 blocks), `MAX_RADIUS` (64 blocks).

## Fluid Collision Recipes (Data Pack)

### Where the Files Go

Place them in the data pack at `data/<namespace>/recipe/<file name>.json`, and set `type` to `redstone_chan_lib:fluid_collision` (fluid hitting fluid) or `redstone_chan_lib:block_collision` (fluid hitting a block); after making changes, `/reload` applies them.

### Field Reference

- `type`: `redstone_chan_lib:fluid_collision` or `redstone_chan_lib:block_collision`.
- `fluid`: the flowing-in fluid, written as `"minecraft:lava"` or a tag with `#` such as `"#minecraft:water"`; you can also use vanilla's `{"tag": "..."}` or a list of fluids.
- `other_fluid`: the other fluid that gets hit (only present on `fluid_collision`), written the same way as above.
- `block`: the block that gets hit (only present on `block_collision`), written as `"minecraft:ice"`, `"#minecraft:logs"`, a list of block ids, or vanilla's `{"tag": "..."}`.
- `results`: the output list, at least one entry. Each entry specifies either `block` or `item` (pick one), and may carry `count` (default 1, 1~64) and `chance` (default 1.0, 0~1); for `item` you can write the item id directly, or use `{"id": "...", "count": ...}`.
  The probability means **one collision produces only one output**: all entries are laid out in order, a single 0~1 dice roll is made, and whichever entry's cumulative probability interval covers the roll is the only one produced, so 50% + 25% + 25% means a one-of-three pick, and they will not all appear at once; when the total falls short of 100%, the remaining probability is "nothing comes out".
- `delay`: how many ticks to wait after triggering before generating, default 1.
- `consume_fluid`: whether to remove the flowing-in fluid after triggering, default `false`.
- `consume_other`: exclusive to `fluid_collision`, whether to remove the other fluid too after triggering, default `false`.
- `consume_block`: exclusive to `block_collision`, whether to remove the block that got hit after triggering, default `false`.
- `consume_source`: whether to also drain the **source** of this flow of fluid after triggering, default `false`; this suits situations like "pouring lava into a water pool keeps producing stone", where turning it off only handles the colliding cell and leaves the source in place.
- `sound`: the sound effect id played on trigger, optional; when omitted it defaults to "water meets lava and extinguishes" (`minecraft:block.lava.extinguish`).
- `fluid_below`: optional, requires that directly **under the flowing-in fluid itself** there is a certain block, written as `"minecraft:magma_block"` or a tag; the field cannot be named `fluid` because `fluid` is already used for the colliding fluid ingredient. In JEI this condition slot is drawn below the fluid column on the left.
- `down`: optional, requires that directly below the **other side** taking part in the collision (the block side) there is a certain block, written as `"minecraft:gold_block"` or a tag such as `"#minecraft:base_stone_nether"`. In JEI this condition slot is drawn below the block column on the right.
- `center`: optional, requires that directly below "the cell that is left empty" there is a certain block. Both layouts count as a collision: the fluid flows directly next to the gold block (or even directly on top of the gold block), in which case the empty cell is the fluid's own cell and `center` looks under the fluid's feet; or the layout is three cells `[Gold Block][Empty][Lava]`, in which case `center` looks under the middle empty cell. In JEI this condition slot is drawn below the middle column.
  However many of the three conditions you write, that many must be satisfied at the same time; writing none means no restriction on the blocks below; it applies equally to fluid hitting fluid (`down` then looks under the other fluid's cell).

### Example: Lava Hitting Water Turns It into Stone

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

The constructor's parameter order is "fluid, other ingredient, outputs, delay, whether to consume the flowing-in fluid, whether to consume the other side, whether to consume the fluid source, sound"; the last two can be omitted, and when the sound is omitted it defaults to water meeting lava and extinguishing.

### Example: Lava Hitting a Gold Block, Where the Block Below the Middle Empty Cell Must Also Be a Gold Block

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

Lay out three cells at the same height: `Gold Block — Air — Lava`, and the cell directly below the middle air cell must also be a gold block; you can also simply pour the lava on top of or next to the gold block, as long as the cell under that position is a gold block.
The fluid in the lava cell then collides with the gold block on the left across the empty cell, producing gold ore; filling in the middle empty cell, or changing the block below the empty cell to stone, will not trigger it.

### Example: Lava Hitting Ice

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

## Fluid Collision Recipes (Java API)

The recipe classes themselves are public records, so you can assemble one directly with the constructor or a static factory and then register it into `CollisionRecipes`; registered recipes take part in matching alongside data pack recipes, and they also show up in JEI.

The imports used by the examples below:

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

### Example: Lava Hitting Water

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

The constructor's parameter order is "fluid, other ingredient, outputs, delay, whether to consume the flowing-in fluid, whether to consume the other side, whether to consume the fluid source, sound"; the last two can be omitted, and when the sound is omitted it defaults to water meeting lava and extinguishing.

### Example: Lava Hitting a Gold Block, Where the Block Below the Middle Empty Cell Must Also Be a Gold Block

```java
CollisionRecipes.register("lava_gold_block", new BlockCollisionRecipe(
        FluidIngredient.of(Fluids.LAVA),
        BlockCollisionRecipe.blocks(Blocks.GOLD_BLOCK),
        List.of(CollisionOutput.block(Blocks.GOLD_ORE)),
        1, false, false, false, null,
        BelowCondition.center(BlockCollisionRecipe.blocks(Blocks.GOLD_BLOCK))));
```

Lay out `Gold Block — Air — Lava` at the same height, and the cell directly below the middle air cell is also a gold block, then gold ore appears in that air cell (neither the lava nor the gold block moves); pouring the lava directly on top of or next to the gold block, with a gold block under that cell, triggers it as well (in that case the middle cell is the lava's own cell).

### Example: Lava Hitting an Emerald Block, Where the Middle Cell's Block Below Must Be an Emerald Block

```java
CollisionRecipes.register("lava_emerald_block", new BlockCollisionRecipe(
        FluidIngredient.of(Fluids.LAVA),
        BlockCollisionRecipe.blocks(Blocks.EMERALD_BLOCK),
        List.of(CollisionOutput.block(Blocks.EMERALD_ORE)),
        1, false, false, false, null,
        BelowCondition.center(BlockCollisionRecipe.blocks(Blocks.EMERALD_BLOCK))));
```

Lay out `Emerald Block — Air — Lava` at the same height, and the cell directly below the middle air cell is also an emerald block, then emerald ore appears in that air cell (neither the lava nor the emerald block moves); pouring the lava directly on top of or next to the emerald block, with an emerald block under that cell, triggers it as well. Swapping in any other block below will not trigger it.

### Example: Lava Hitting Ice

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

### Example: The Six Test Recipes Bundled with the Mod

During the setup phase the mod calls `BuiltinCollisionRecipes.register()`, which already registers six examples written directly in Java, so you can try them as soon as you get in game:

```java
// Lava touching diamond ore (including the deepslate variant): always drops one diamond and consumes the ore
CollisionRecipes.register("lava_diamond_ore", new BlockCollisionRecipe(
        FluidIngredient.of(Fluids.LAVA),
        BlockCollisionRecipe.blocks(Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE),
        List.of(CollisionOutput.item(Items.DIAMOND, 1, 1.0F)),
        1, false, true, Optional.empty()));

// Lava touching a diamond block: places another diamond block in place
CollisionRecipes.register("lava_diamond_block", new BlockCollisionRecipe(
        FluidIngredient.of(Fluids.LAVA),
        BlockCollisionRecipe.blocks(Blocks.DIAMOND_BLOCK),
        List.of(CollisionOutput.block(Blocks.DIAMOND_BLOCK)),
        1, false, false, Optional.empty()));

// Lava touching ancient debris: 50% ancient debris again, 25% nether quartz ore, 25% netherrack
CollisionRecipes.register("lava_ancient_debris", new BlockCollisionRecipe(
        FluidIngredient.of(Fluids.LAVA),
        BlockCollisionRecipe.blocks(Blocks.ANCIENT_DEBRIS),
        List.of(
                CollisionOutput.block(Blocks.ANCIENT_DEBRIS, 1, 0.5F),
                CollisionOutput.block(Blocks.NETHER_QUARTZ_ORE, 1, 0.25F),
                CollisionOutput.block(Blocks.NETHERRACK, 1, 0.25F)),
        1, false, true, Optional.of(SoundEvents.LAVA_EXTINGUISH)));

// Lava touching a gold block only becomes gold ore when the block below the middle empty cell is also a gold block
CollisionRecipes.register("lava_gold_block", new BlockCollisionRecipe(
        FluidIngredient.of(Fluids.LAVA),
        BlockCollisionRecipe.blocks(Blocks.GOLD_BLOCK),
        List.of(CollisionOutput.block(Blocks.GOLD_ORE, 1, 1.0F)),
        1, false, false, false, null,
        BelowCondition.center(Blocks.GOLD_BLOCK)));

// Lava touching an emerald block only becomes emerald ore when the block below the middle empty cell is also an emerald block
CollisionRecipes.register("lava_emerald_block", new BlockCollisionRecipe(
        FluidIngredient.of(Fluids.LAVA),
        BlockCollisionRecipe.blocks(Blocks.EMERALD_BLOCK),
        List.of(CollisionOutput.block(Blocks.EMERALD_ORE, 1, 1.0F)),
        1, false, false, false, null,
        BelowCondition.center(Blocks.EMERALD_BLOCK)));

// Lava touching stone only becomes a magma block when the block under the lava itself is a magma block
CollisionRecipes.register("lava_stone_on_magma", new BlockCollisionRecipe(
        FluidIngredient.of(Fluids.LAVA),
        BlockCollisionRecipe.blocks(Blocks.STONE),
        List.of(CollisionOutput.block(Blocks.MAGMA_BLOCK, 1, 1.0F)),
        1, false, false, false, null,
        BelowCondition.down(Blocks.MAGMA_BLOCK)));
```

If you do not want to keep these six examples, just delete `BuiltinCollisionRecipes.register()` from the `RedstoneChanLIB` constructor.

### How to Write fluid_below / down / center

These conditions are `BelowCondition` factory methods in Java; write the block or tag directly, with no need to assemble a `HolderSet` yourself:

```java
// Only requires that "the block below the middle empty cell" is an emerald block
BelowCondition.center(Blocks.EMERALD_BLOCK);

// Only requires that "the block under the fluid itself" is a magma block
BelowCondition.fluid(Blocks.MAGMA_BLOCK);

// Only requires that "the block under the other side (the block side)" is an iron block
BelowCondition.down(Blocks.IRON_BLOCK);

// Tags work too: the block below the middle cell is any diamond ore
BelowCondition.center(BlockTags.DIAMOND_ORES);

// All three restricted together: magma block under the fluid, iron block under the other side, obsidian below the middle cell
BelowCondition.all(
        BlockCollisionRecipe.blocks(Blocks.MAGMA_BLOCK),
        BlockCollisionRecipe.blocks(Blocks.IRON_BLOCK),
        BlockCollisionRecipe.blocks(Blocks.OBSIDIAN));
```

When writing it into a recipe, just pass it as the last argument:

```java
CollisionRecipes.register("lava_emerald_block", new BlockCollisionRecipe(
        FluidIngredient.of(Fluids.LAVA),
        BlockCollisionRecipe.blocks(Blocks.EMERALD_BLOCK),
        List.of(CollisionOutput.block(Blocks.EMERALD_ORE)),
        1, false, false, false, null,
        BelowCondition.center(Blocks.EMERALD_BLOCK)));
```

The three factories are `fluid(...)`, `down(...)`, and `center(...)`; to restrict several places at once write `all(fluid, down, center)` (for only the fluid and the other side use `both(fluid, down)`); when no block-below restriction is needed pass `BelowCondition.NONE`; the same thing written in a data pack is the three top-level recipe JSON fields `fluid_below`, `down`, and `center`. To check whether a condition holds use `testFluid(BlockState)`, `testDown(BlockState)`, and `testCenter(BlockState)`, and use `isEmpty()` to check whether everything is unrestricted.

### Methods You Can Call Directly

- `CollisionRecipes.register(ResourceLocation id, FluidCollisionRecipe recipe)`: registers a fluid collision recipe.
- `CollisionRecipes.register(String path, FluidCollisionRecipe recipe)`: same as above, with `redstone_chan_lib:` automatically prefixed to the id.
- `CollisionRecipes.register(ResourceLocation id, BlockCollisionRecipe recipe)`: registers a block collision recipe.
- `CollisionRecipes.register(String path, BlockCollisionRecipe recipe)`: same as above, with the prefix added automatically.
- `CollisionRecipes.javaFluidRecipes()` / `javaBlockRecipes()`: get the currently registered Java recipe tables.
- `CollisionRecipes.onFluidPlaced(Level level, BlockPos pos, BlockState newState, BlockState oldState)`: manually feed one "fluid flows in" event; in normal gameplay this is called automatically by the injection.
- `CollisionOutput.block(Block)` / `block(Block, float chance)` / `block(Block, int count, float chance)`: build one block output.
- `CollisionOutput.item(Item)` / `item(Item, int count, float chance)` / `item(ItemStack, float chance)`: build one item output.
- `BlockCollisionRecipe.blocks(Block... blocks)`: wraps several blocks directly into a block set, saving you from writing `HolderSet.direct(...)` by hand.
- `BlockCollisionRecipe.tag(TagKey<Block> tag)`: wraps a block tag into a block set; it throws an exception if the tag does not exist.
- `BelowCondition.NONE`: no restriction on the block below at all.
- `BelowCondition.center(Block...)` / `center(TagKey<Block>)` / `center(HolderSet<Block>)`: requires that the block below the middle empty cell is one of these blocks.
- `BelowCondition.down(Block...)` / `down(TagKey<Block>)` / `down(HolderSet<Block>)`: requires that the block under the other side (the block side) is one of these blocks.
- `BelowCondition.fluid(Block...)` / `fluid(TagKey<Block>)` / `fluid(HolderSet<Block>)`: requires that the block under the fluid itself is one of these blocks.
- `BelowCondition.both(HolderSet<Block> fluid, HolderSet<Block> down)`: restricts the block under the fluid and under the other side at the same time (either argument may be passed as `null`).
- `BelowCondition.all(HolderSet<Block> fluid, HolderSet<Block> down, HolderSet<Block> center)`: restricts all three places at once (any argument may be passed as `null`).
- `BelowCondition.testFluid(BlockState)` / `testDown(BlockState)` / `testCenter(BlockState)` / `isEmpty()`: test one block-below position individually, or check whether there is no restriction at all.
- Constants: `CollisionOutput.DEFAULT_COUNT`, `DEFAULT_CHANCE`, `FluidCollisionRecipe.DEFAULT_DELAY`, `TEST_AMOUNT`.
- `BuiltinCollisionRecipes.register()`: the mod's six bundled example recipes (the six from the example above), registered during the setup phase.

## Creative Mode Tab Sections (Java API)

`SectionedCreativeTab` splits a creative tab into sections: once in game, a row of buttons appears on the left side of the screen in section order. Clicking one shows only that section, and holding Shift while clicking lets you select several at once. Sections that are not selected do not appear in the list, but searching can still find them.

Imports used in the example below:

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

Minimal usage: three sections, all appearance values left at their defaults.

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

// Registration (hand the generated tab to your own DeferredRegister)
public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "mymod");
public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN =
        TABS.register("main_tab", () -> TAB.tab());
```

Custom appearance: background textures (unselected, selected, hovered), button size, item rendering toggle and size, and tooltip text.

```java
CreativeSectionStyle style = CreativeSectionStyle.defaults()
        .withTexture(                       // background textures for the unselected / selected state
                ResourceLocation.fromNamespaceAndPath("mymod", "textures/gui/tab_button.png"),
                ResourceLocation.fromNamespaceAndPath("mymod", "textures/gui/tab_button_selected.png"))
        .withHoveredTexture(                // the texture layered over the background while hovering; null means no hover highlight
                ResourceLocation.fromNamespaceAndPath("mymod", "textures/gui/tab_button_hovered.png"))
        .withSize(30, 30)                   // button size
        .withTextureSize(30, 30)            // actual pixel size of the background texture (omit when it matches the button)
        .withIcon(true)                     // whether to draw the item
        .withIconScale(0.75F)               // item render size; 1.0 is 16×16
        .withIconOffset(9, 9)               // item offset relative to the button's top-left corner
        .withTooltip(Component.literal("材料都在这里"));  // tooltip text; null means the default "name + hold Shift to multi-select"
```

All three background textures can also be replaced in one go: `withTexture(unselected, selected, hovered)`.

A style can be set as the default for the whole tab, or set individually for a single section:

```java
SectionedCreativeTab.builder(id)
        .title(Component.literal("我的创造栏"))
        .icon(() -> new ItemStack(Items.DIAMOND))
        .style(CreativeSectionStyle.defaults())
        .section(CreativeSection.of(
                Component.literal("特殊分区"),
                () -> new ItemStack(Items.NETHER_STAR),
                output -> output.accept(Items.NETHER_STAR),
                CreativeSectionStyle.defaults().withIcon(false)))  // this button shows only the background texture
        .build();
```

### Methods You Can Call Directly

- `SectionedCreativeTab.builder(ResourceLocation id)`: starts describing a tab and returns the builder.
- `Builder.title(Component)`: tab title (required).
- `Builder.icon(Supplier<ItemStack>)`: tab icon.
- `Builder.style(CreativeSectionStyle)`: default button style for all sections.
- `Builder.section(Component name, Supplier<ItemStack> icon, Consumer<CreativeModeTab.Output> content)`: adds a section using the default style.
- `Builder.section(CreativeSection section)`: adds a section, optionally with its own style.
- `Builder.build()`: builds and registers it, returning a `SectionedCreativeTab`.
- `SectionedCreativeTab.tab()`: the built `CreativeModeTab`, to be handed to your own `DeferredRegister`.
- `SectionedCreativeTab.id()` / `sections()` / `style()`: retrieve the id, the section list and the default style.
- `CreativeSection.of(name, icon, content)` / `of(name, icon, content, style)`: construct a section; you can also use `new CreativeSection(...)` directly.
- `CreativeSectionStyle.defaults()`: the default style (24×24, built-in background texture, item at original size centered slightly above the middle, default tooltip).
- `CreativeSectionStyle.withTexture(unselected, selected, hovered)` / `withTexture(unselected, selected)` / `withTexture(theSameOne)`: replace the background textures.
- `withHoveredTexture(ResourceLocation)`: replaces only the hover background texture; `null` means no extra highlight on hover.
- `withSize(int width, int height)` / `withTextureSize(int w, int h)`: button size and background texture pixel size.
- `withIcon(boolean)` / `withIconScale(float)` / `withIconOffset(int x, int y)`: the toggle, size and offset of item rendering.
- `withTooltip(Component)`: hover tooltip; `null` means the default "section name + hold Shift to multi-select".
- `CreativeSections.byTab(CreativeModeTab)` / `byId(ResourceLocation)`: look up registration info (`Entry` holds the id, tab, sections and default style).
- `CreativeSections.isActive(tab, index)` / `selectSection(tab, index)` / `toggleSection(tab, index)`: read or change the selection state (you can also switch sections from code).
- `CreativeSections.sections(tab)` / `styleOf(entry, index)` / `addItems(tab, output)` / `indices(tab)`: get the sections, get the effective style, fill in items, get the indices.

## Fluid and Gas Registration (Java API)

To add a fluid you do not need to write your own fluid class, fluid block or bucket item — a single call to the registrar is enough.

### Quick Start

Create a registrar in your mod constructor, then `register` whatever you want:

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

What that single call registers is: the fluid type (`mymod:liquid_mana`), the source fluid (`mymod:liquid_mana`), the flowing fluid (`mymod:flowing_liquid_mana`), the fluid block (`mymod:liquid_mana`) and the bucket (`mymod:liquid_mana_bucket`).

### Gas Example

Change the kind to `GAS` and give the density a negative value, and it will float upward and dissipate by itself after a while:

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

A gas block gives entities a collision surface in the upper half, so you can stand on the surface of the gas; `gasDissipateTicks` is how many ticks it survives before disappearing (20 ticks is one second); if it is still blocked above, it waits another twenty ticks each time.

A gas's height and shape are also upside down: while the volume below is the same gas it counts as full, and the shape grows from the top of the block downward (when full it fills the whole block); when spreading outward it checks whether there is an opening above rather than looking for a pit below the way vanilla does.

The rendering flips along with it: the vanilla liquid renderer always draws fluid in the lower half of the block, so for gas, `client/fluid/GasFluidRenderer` takes over and draws it itself, growing down from the top of the block with the visible face pointing down, which is why the bubbles press down from the ceiling and the lower edge is fuzzy. This is done by `mixin/client/LiquidBlockRendererMixin` intercepting at the HEAD of `tesselate`, and it only applies to `RCLGasFluid`; other fluids still use the vanilla approach.

### Two Ways to Pour a Gas Bucket

The default gas bucket is the same as a normal bucket: right-clicking pours out the gas block. To make the gas "scatter the moment it is poured", add `gasBurst(true)`:

```java
FluidEntry vapor = registrar.register(FluidDefinition.builder("hot_vapor")
        .kind(FluidKind.GAS)
        .density(-400)
        .gasBurst(true)
        .build());
```

With that enabled, right-clicking does not place a block; it only spawns a puff of cloud particles at the point you are looking at and plays an extinguish sound, and the bucket turns back into an empty bucket, as if the gas dissipated on the spot. Spectators and survival/creative are all handled the same way, and creative mode does not consume the bucket. Without this switch, the default is "can pour out a gas block".

### Registering Several at Once

```java
registrar.register(
        FluidDefinition.builder("deep_water").translucent(true).blockLightLevel(3).build(),
        FluidDefinition.builder("hot_air").kind(FluidKind.GAS).density(-800).createBucket(false).build());
```

### Parameters You Can Set

Appearance and client-side behavior:

- `textures(still, flowing)`: still and flowing textures; the defaults are vanilla water's `block/water_still` and `block/water_flow`, so you can skip textures entirely and just tint with `tintColor`; call this only when you want to draw your own
- `stillTexture`, `flowingTexture`: set either one individually
- `tintColor(argb)`: fluid tint, including alpha; the default `0xFFFFFFFF` applies no tint
- `fogColor(rgb)`: fog color while inside the fluid, without alpha
- `fogDistance(float)`: maximum fog distance; the smaller it is, the murkier it looks; default 96
- `fogEnabled(boolean)`: turn it off and there is no fog
- `translucent(boolean)`: use the translucent or the solid render layer; default translucent
- `lightLevel(int)`, `blockLightLevel(int)`: light emission level of the fluid type and of the fluid block

Physics and behavior:

- `density(int)`: density; negative values float upward; default 1000
- `viscosity(int)`: viscosity; the higher it is, the slower it flows
- `temperature(int)`: temperature
- `fallDistanceModifier(float)`: fall distance modifier
- `motionScale(double)`: scaling of entity motion speed inside it
- `canHydrate`, `canConvertToSource`, `supportsBoating`, `canExtinguish`, `canPushEntity`, `canDrown`, `canSwim`: the corresponding toggles of the vanilla fluid type

Flow and explosion:

- `slopeFindDistance(int)`: distance to search for a slope; default 4
- `levelDecreasePerBlock(int)`: level decrease per block; default 1
- `tickRate(int)`: update tick rate; the smaller it is, the faster it flows; default 5
- `explosionResistance(float)`: explosion resistance; default 100

Gas-specific:

- `gasDissipateTicks(int)`: ticks it exists before dissipating; default 200
- `gasBurst(boolean)`: right-clicking the gas bucket scatters straight into particles without placing a gas block; default false (pours the block as usual)

Other:

- `createBucket(boolean)`: whether to register a bucket at the same time; default yes
- `translationKey(String)`: translation key; the default is `block.<namespace>.<name>`

### After You Get It Back

`register` returns a `FluidEntry`; take whatever you need from it:

- `id()`: the full registry name
- `definition()`: the definition you passed in
- `type()`, `fluidType()`: the fluid type
- `source()`, `sourceFluid()`: the source fluid
- `flowing()`, `flowingFluid()`: the flowing fluid
- `block()`, `fluidBlock()`: the fluid block
- `bucket()`, `bucketItem()`: the bucket item; `bucket()` is null when no bucket was registered

The registrar itself also has `entries()` and `namespace()`.

### Resource Files: Two Approaches

There are two paths for textures; just pick one.

**First, prepare nothing**: do not write `textures(...)`, and the fluid uses vanilla water's still and flowing textures, with the color multiplied in from `tintColor` (the water texture itself is grayscale, so whatever color you tint it is the color you get). Fog and light emission still work as usual. The liquid redstone in the example takes this path.

**Second, draw your own textures**: put the two grayscale images under `assets/<namespace>/textures/block/`, then point at them with `textures(<still>, <flowing>)`. The still image is usually 16×16, and by vanilla convention the flowing image is 16×16 with multiple vertical frames. The redstone gas in the example takes this path.

Whichever path you take, the following still needs to be added:

- Fluid blocks do not need a blockstate file; vanilla draws them with the fluid renderer
- The bucket item needs a model, and the parent model is one of two choices: to have the fluid sit inside the bucket like a vanilla water bucket, use `neoforge:item/bucket`; to have the fluid hang off the rim of the bucket, use `neoforge:item/bucket_drip` (liquid-type buckets generally use this one), and let the fluid-tinted model take over the color
- `flip_gas` in the model is the switch for "should the bucket be held upside down": only when the fluid is lighter than air (density less than 0) and `true` is written will NeoForge rotate the whole model 180 degrees; write `false` for liquid buckets and `true` for gas buckets, so it looks like the gas is pushing the bucket up and it can only be carried upside down
- In the language file, add `block.<namespace>.<name>` and `item.<namespace>.<name>_bucket`

If you use data generation, there are ready-made helpers for the bucket model:

```java
FluidModels.bucket(provider, item);         // fluid sits inside the bucket
FluidModels.dripBucket(provider, item);     // fluid hangs off the rim; use this for liquid-type buckets
FluidModels.floatingBucket(provider, item); // upside down and drifting outward; only for fluids lighter than air
```

### About the Access Transformer

Gas needs to reuse the spreading logic of vanilla fluids, and vanilla declares several of those methods private, so the library ships a `META-INF/accesstransformer.cfg` that widens those methods to `protected`. This file is loaded together with the mod and has no effect on other mods; it simply lets them override these methods too.

### Built-in Examples

The library itself registers two example fluids, which you can see in action as soon as you install it:

- `redstone_chan_lib:liquid_redstone` (liquid redstone): it has no textures of its own and directly uses vanilla water's texture tinted red; a glowing, thick red liquid that flows slowly and only a short distance, with dark red fog while you are submerged in it. Its bucket uses the drip model, with the fluid hanging off the rim.
- `redstone_chan_lib:redstone_gas` (redstone gas): it ships its own two grayscale textures pointed at through `textures(...)`; translucent, with negative density, it floats all the way up into the sky and dissipates by itself after a while, and standing in it wraps the screen in pale red fog. Its bucket is a normal gas bucket, and right-clicking pours out a gas block.
- `redstone_chan_lib:redstone_vapor` (redstone vapor): it shares textures with the gas; the difference is that `gasBurst(true)` is enabled, so right-clicking only emits a puff of cloud particles and plays an extinguish sound, leaving no block behind.

Both buckets are in the "Fluid Examples" section of the creative tab. To change the colors or the feel, look at `init/RCLDemoFluids.java`; that is a complete usage example, so just edit it and rebuild.
