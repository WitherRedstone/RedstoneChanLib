# Redstone Chan Lib

A small library of code that is convenient to use.

## What it provides

### Custom portals

The frame blocks, the activating item or fluid, the portal block's color and name, and the destination dimension are all defined in a data pack, and Java can register a portal directly as well. Vertical portals (like the Nether portal), flat portals lying on the ground (like the End portal), and Twilight Forest style ritual activation (a fluid pool, an outer ring of frames, throwing in an item, lightning lighting it up) are all supported.

### Entity outlines

Puts a rim of light around an entity. There are three styles — glow, black and highlight — each available in a solid color or rainbow, with adjustable thickness and a choice of whether it shows through walls. It is applied on the server and saved with the world.

### Block outlines

The same three styles, plus a Create style box and a thin wireframe, with fade in and fade out animations.

### Block and entity scan highlighting

Scans blocks or entities within a radius around you and only outlines the ones that match, with per-block and per-tag colors.

### Sectioned creative inventory tabs

A creative tab can be split into sections, each with its own icon, tooltip and selected state.

### Fluid and gas registration

One call registers a fluid type, the source fluid, the flowing fluid, the fluid block and the bucket item. Density, temperature, viscosity, light level, tint, fog color and fog distance are all configurable. Gases drift upward and dissipate on their own after a while, and a gas bucket can either place a gas block or vanish into particles.

### Fluid collision recipes

A fluid hitting another fluid or a block can produce other blocks or items. You can require specific blocks below the collision spot, and configure whether the ingredients and the fluid source are consumed. Both data packs and Java are supported, with JEI display included.

## Detailed usage

Data pack fields, Java examples and the full method list are in [README_USAGE_EN.md](README_USAGE_EN.md).
