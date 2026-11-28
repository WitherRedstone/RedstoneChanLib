package com.chinaex123.redstone_chan_lib.client.fluid;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.textures.FluidSpriteCache;

/**
 * 气体流体渲染器。
 * <p>
 * 自定义气体流体方块的渲染：按照与周围流体的高度关系计算四角高度，
 * 绘制底部液面、顶面与四个侧面，并处理流向贴图、光照与着色；
 * 与相邻同类流体高度一致时不重复绘制侧面。
 */
public final class GasFluidRenderer {

    /** 气体液面高度上限。 */
    private static final float MAX_GAS_LAYER = 0.8888889F;

    private GasFluidRenderer() {
    }

    /**
     * 渲染气体流体方块。
     * <p>
     * 获取流体贴图与着色后，判断各面是否需要绘制；
     * 需要绘制底部液面时按流向或静止贴图绘制，需要时绘制顶面与四个侧面。
     *
     * @param level      方块与着色获取器
     * @param pos        方块位置
     * @param buffer     顶点消费者
     * @param blockState 方块状态
     * @param fluidState 流体状态
     */
    public static void render(BlockAndTintGetter level, BlockPos pos, VertexConsumer buffer,
                              BlockState blockState, FluidState fluidState) {
        TextureAtlasSprite[] sprites = FluidSpriteCache.getFluidSprites(level, pos, fluidState);
        int tint = IClientFluidTypeExtensions.of(fluidState).getTintColor(fluidState, level, pos);
        float alpha = (tint >> 24 & 0xFF) / 255.0F;
        float red = (tint >> 16 & 0xFF) / 255.0F;
        float green = (tint >> 8 & 0xFF) / 255.0F;
        float blue = (tint & 0xFF) / 255.0F;

        BlockState upState = level.getBlockState(pos.above());
        FluidState upFluid = upState.getFluidState();
        BlockState downState = level.getBlockState(pos.below());
        FluidState downFluid = downState.getFluidState();
        BlockState northState = level.getBlockState(pos.north());
        FluidState northFluid = northState.getFluidState();
        BlockState southState = level.getBlockState(pos.south());
        FluidState southFluid = southState.getFluidState();
        BlockState westState = level.getBlockState(pos.west());
        FluidState westFluid = westState.getFluidState();
        BlockState eastState = level.getBlockState(pos.east());
        FluidState eastFluid = eastState.getFluidState();

        boolean sameGasUp = isNeighborSameFluid(fluidState, upFluid);
        boolean sameGasDown = isNeighborSameFluid(fluidState, downFluid);

        boolean renderSurface = !sameGasDown && !isNeighborStateHidingOverlay(fluidState, downState, Direction.UP);
        boolean renderTop = !sameGasUp
                && shouldRenderFace(level, pos, fluidState, blockState, Direction.UP, upState)
                && !isFaceOccludedByNeighbor(level, pos, Direction.UP, MAX_GAS_LAYER, upState);
        boolean renderNorth = shouldRenderFace(level, pos, fluidState, blockState, Direction.NORTH, northState);
        boolean renderSouth = shouldRenderFace(level, pos, fluidState, blockState, Direction.SOUTH, southState);
        boolean renderWest = shouldRenderFace(level, pos, fluidState, blockState, Direction.WEST, westState);
        boolean renderEast = shouldRenderFace(level, pos, fluidState, blockState, Direction.EAST, eastState);
        if (!renderSurface && !renderTop && !renderNorth && !renderSouth && !renderWest && !renderEast) {
            return;
        }

        float shadeDown = level.getShade(Direction.DOWN, true);
        float shadeUp = level.getShade(Direction.UP, true);
        float shadeNorth = level.getShade(Direction.NORTH, true);
        float shadeWest = level.getShade(Direction.WEST, true);
        Fluid fluid = fluidState.getType();
        float centerHeight = height(level, fluid, pos, blockState, fluidState);
        float heightNorthEast;
        float heightNorthWest;
        float heightSouthEast;
        float heightSouthWest;
        if (centerHeight >= 1.0F) {
            heightNorthEast = 1.0F;
            heightNorthWest = 1.0F;
            heightSouthEast = 1.0F;
            heightSouthWest = 1.0F;
        } else {
            float northHeight = height(level, fluid, pos.north(), northState, northFluid);
            float southHeight = height(level, fluid, pos.south(), southState, southFluid);
            float eastHeight = height(level, fluid, pos.east(), eastState, eastFluid);
            float westHeight = height(level, fluid, pos.west(), westState, westFluid);
            heightNorthWest = averageHeight(level, fluid, centerHeight, northHeight, westHeight, pos.north().west());
            heightNorthEast = averageHeight(level, fluid, centerHeight, northHeight, eastHeight, pos.north().east());
            heightSouthWest = averageHeight(level, fluid, centerHeight, southHeight, westHeight, pos.south().west());
            heightSouthEast = averageHeight(level, fluid, centerHeight, southHeight, eastHeight, pos.south().east());
        }

        float x = pos.getX() & 15;
        float y = pos.getY() & 15;
        float z = pos.getZ() & 15;
        float topOffset = renderTop ? 0.001F : 0.0F;

        if (renderSurface) {
            float hNorthWest = heightNorthWest - 0.001F;
            float hSouthWest = heightSouthWest - 0.001F;
            float hSouthEast = heightSouthEast - 0.001F;
            float hNorthEast = heightNorthEast - 0.001F;
            Vec3 flow = fluidState.getFlow(level, pos);
            float u0;
            float u1;
            float u2;
            float u3;
            float v0;
            float v1;
            float v2;
            float v3;
            if (flow.x == 0.0 && flow.z == 0.0) {
                TextureAtlasSprite still = sprites[0];
                u0 = still.getU(0.0F);
                v0 = still.getV(0.0F);
                u1 = u0;
                v1 = still.getV(1.0F);
                u2 = still.getU(1.0F);
                v2 = v1;
                u3 = u2;
                v3 = v0;
            } else {
                TextureAtlasSprite flowSprite = sprites[1];
                float angle = (float) Mth.atan2(flow.z, flow.x) - (float) (Math.PI / 2);
                float sin = Mth.sin(angle) * 0.25F;
                float cos = Mth.cos(angle) * 0.25F;
                u0 = flowSprite.getU(0.5F + (-cos - sin));
                v0 = flowSprite.getV(0.5F + -cos + sin);
                u1 = flowSprite.getU(0.5F + -cos + sin);
                v1 = flowSprite.getV(0.5F + cos + sin);
                u2 = flowSprite.getU(0.5F + cos + sin);
                v2 = flowSprite.getV(0.5F + (cos - sin));
                u3 = flowSprite.getU(0.5F + (cos - sin));
                v3 = flowSprite.getV(0.5F + (-cos - sin));
            }
            float centerU = (u0 + u1 + u2 + u3) / 4.0F;
            float centerV = (v0 + v1 + v2 + v3) / 4.0F;
            float shrink = sprites[0].uvShrinkRatio();
            u0 = Mth.lerp(shrink, u0, centerU);
            u1 = Mth.lerp(shrink, u1, centerU);
            u2 = Mth.lerp(shrink, u2, centerU);
            u3 = Mth.lerp(shrink, u3, centerU);
            v0 = Mth.lerp(shrink, v0, centerV);
            v1 = Mth.lerp(shrink, v1, centerV);
            v2 = Mth.lerp(shrink, v2, centerV);
            v3 = Mth.lerp(shrink, v3, centerV);
            int light = getLightColor(level, pos);
            float r = shadeDown * red;
            float g = shadeDown * green;
            float b = shadeDown * blue;
            vertex(buffer, x + 1.0F, y + 1.0F - hNorthEast, z, r, g, b, alpha, u3, v3, light, Direction.DOWN);
            vertex(buffer, x + 1.0F, y + 1.0F - hSouthEast, z + 1.0F, r, g, b, alpha, u2, v2, light, Direction.DOWN);
            vertex(buffer, x, y + 1.0F - hSouthWest, z + 1.0F, r, g, b, alpha, u1, v1, light, Direction.DOWN);
            vertex(buffer, x, y + 1.0F - hNorthWest, z, r, g, b, alpha, u0, v0, light, Direction.DOWN);
        }

        if (renderTop) {
            TextureAtlasSprite still = sprites[0];
            float u0 = still.getU0();
            float u1 = still.getU1();
            float v0 = still.getV0();
            float v1 = still.getV1();
            int light = getLightColor(level, pos.above());
            float r = shadeUp * red;
            float g = shadeUp * green;
            float b = shadeUp * blue;
            float top = y + 1.0F - topOffset;
            vertex(buffer, x + 1.0F, top, z + 1.0F, r, g, b, alpha, u1, v1, light, Direction.UP);
            vertex(buffer, x + 1.0F, top, z, r, g, b, alpha, u1, v0, light, Direction.UP);
            vertex(buffer, x, top, z, r, g, b, alpha, u0, v0, light, Direction.UP);
            vertex(buffer, x, top, z + 1.0F, r, g, b, alpha, u0, v1, light, Direction.UP);
        }

        int light = getLightColor(level, pos);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            float nearHeight;
            float farHeight;
            float x0;
            float x1;
            float z0;
            float z1;
            boolean shouldRender;
            switch (direction) {
                case NORTH -> {
                    nearHeight = heightSouthWest;
                    farHeight = heightSouthEast;
                    x0 = x;
                    x1 = x + 1.0F;
                    z0 = z + 0.001F;
                    z1 = z + 0.001F;
                    shouldRender = renderNorth;
                }
                case SOUTH -> {
                    nearHeight = heightNorthWest;
                    farHeight = heightNorthEast;
                    x0 = x + 1.0F;
                    x1 = x;
                    z0 = z + 1.0F - 0.001F;
                    z1 = z + 1.0F - 0.001F;
                    shouldRender = renderSouth;
                }
                case WEST -> {
                    nearHeight = heightNorthWest;
                    farHeight = heightSouthWest;
                    x0 = x + 0.001F;
                    x1 = x + 0.001F;
                    z0 = z + 1.0F;
                    z1 = z;
                    shouldRender = renderWest;
                }
                default -> {
                    nearHeight = heightSouthEast;
                    farHeight = heightNorthEast;
                    x0 = x + 1.0F - 0.001F;
                    x1 = x + 1.0F - 0.001F;
                    z0 = z;
                    z1 = z + 1.0F;
                    shouldRender = renderEast;
                }
            }
            if (!shouldRender || isFaceOccludedByNeighbor(level, pos, direction,
                    Math.max(nearHeight, farHeight), level.getBlockState(pos.relative(direction)))) {
                continue;
            }
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);
            FluidState neighborFluid = neighborState.getFluidState();
            if (isNeighborSameFluid(fluidState, neighborFluid)
                    && Math.abs(height(level, fluid, neighborPos, neighborState, neighborFluid) - centerHeight) < 1.0E-4F) {
                continue;
            }
            TextureAtlasSprite sideSprite = sprites[1];
            if (sprites[2] != null && neighborState.shouldDisplayFluidOverlay(level, neighborPos, fluidState)) {
                sideSprite = sprites[2];
            }
            float uLeft = sideSprite.getU(0.0F);
            float uRight = sideSprite.getU(0.5F);
            float vTop = sideSprite.getV(0.5F);
            float vBottomNear = sideSprite.getV((1.0F - nearHeight) * 0.5F);
            float vBottomFar = sideSprite.getV((1.0F - farHeight) * 0.5F);
            float shade = direction.getAxis() == Direction.Axis.Z ? shadeNorth : shadeWest;
            float r = shadeUp * shade * red;
            float g = shadeUp * shade * green;
            float b = shadeUp * shade * blue;
            float topY = y + 1.0F - topOffset;
            vertex(buffer, x0, topY, z0, r, g, b, alpha, uLeft, vTop, light, direction);
            vertex(buffer, x1, topY, z1, r, g, b, alpha, uRight, vTop, light, direction);
            vertex(buffer, x1, y + 1.0F - farHeight, z1, r, g, b, alpha, uRight, vBottomFar, light, direction);
            vertex(buffer, x0, y + 1.0F - nearHeight, z0, r, g, b, alpha, uLeft, vBottomNear, light, direction);
            if (sideSprite != sprites[2]) {
                vertex(buffer, x0, y + 1.0F - nearHeight, z0, r, g, b, alpha, uLeft, vBottomNear, light, direction);
                vertex(buffer, x1, y + 1.0F - farHeight, z1, r, g, b, alpha, uRight, vBottomFar, light, direction);
                vertex(buffer, x1, topY, z1, r, g, b, alpha, uRight, vTop, light, direction);
                vertex(buffer, x0, topY, z0, r, g, b, alpha, uLeft, vTop, light, direction);
            }
        }
    }

    /**
     * 计算指定位置流体的高度。
     * <p>
     * 同类源流体或下方为同类流体时高度为 1，否则取自身高度；
     * 非同类流体时，下方为固体则返回 0，否则返回 -1。
     *
     * @param level      方块与着色获取器
     * @param fluid      流体
     * @param pos        位置
     * @param blockState 方块状态
     * @param fluidState 流体状态
     * @return 流体高度
     */
    private static float height(BlockAndTintGetter level, Fluid fluid, BlockPos pos, BlockState blockState, FluidState fluidState) {
        if (fluid.isSame(fluidState.getType())) {
            if (fluidState.isSource()) {
                return 1.0F;
            }
            BlockState below = level.getBlockState(pos.below());
            return fluid.isSame(below.getFluidState().getType()) ? 1.0F : fluidState.getOwnHeight();
        }
        return !blockState.isSolid() ? 0.0F : -1.0F;
    }

    /**
     * 计算某角的加权平均高度。
     * <p>
     * 对角上相邻的三个高度做加权平均；任一角高度达到 1 时直接返回 1，
     * 权重偏向接近满高度的值，使液面过渡更平滑。
     *
     * @param level        方块与着色获取器
     * @param fluid        流体
     * @param currentHeight 当前高度
     * @param height1      相邻高度一
     * @param height2      相邻高度二
     * @param pos          角位置
     * @return 加权平均高度
     */
    private static float averageHeight(BlockAndTintGetter level, Fluid fluid, float currentHeight, float height1,
                                       float height2, BlockPos pos) {
        if (height2 >= 1.0F || height1 >= 1.0F) {
            return 1.0F;
        }
        float[] sum = new float[2];
        if (height2 > 0.0F || height1 > 0.0F) {
            BlockState neighborState = level.getBlockState(pos);
            float neighborHeight = height(level, fluid, pos, neighborState, neighborState.getFluidState());
            if (neighborHeight >= 1.0F) {
                return 1.0F;
            }
            addWeightedHeight(sum, neighborHeight);
        }
        addWeightedHeight(sum, currentHeight);
        addWeightedHeight(sum, height2);
        addWeightedHeight(sum, height1);
        return sum[0] / sum[1];
    }

    /**
     * 累加加权高度。
     * <p>
     * 高度达到 0.8 以上按 10 倍权重累加，非负高度按 1 倍权重累加，负值忽略。
     *
     * @param output 累加结果数组，索引 0 为加权和、索引 1 为权重和
     * @param value  高度值
     */
    private static void addWeightedHeight(float[] output, float value) {
        if (value >= 0.8F) {
            output[0] += value * 10.0F;
            output[1] += 10.0F;
        } else if (value >= 0.0F) {
            output[0] += value;
            output[1]++;
        }
    }

    /**
     * 判断两个流体状态是否为同类流体。
     *
     * @param first  第一个流体状态
     * @param second 第二个流体状态
     * @return 同类返回 true
     */
    private static boolean isNeighborSameFluid(FluidState first, FluidState second) {
        return second.getType().isSame(first.getType());
    }

    /**
     * 判断相邻方块状态是否隐藏流体贴面。
     *
     * @param selfState   自身流体状态
     * @param otherState  相邻方块状态
     * @param neighborFace 相邻面
     * @return 隐藏返回 true
     */
    private static boolean isNeighborStateHidingOverlay(FluidState selfState, BlockState otherState, Direction neighborFace) {
        return otherState.shouldHideAdjacentFluidFace(neighborFace, selfState);
    }

    /**
     * 判断指定方块状态是否在给定面上遮挡高度为 faceHeight 的盒体。
     *
     * @param level      方块获取器
     * @param face       面
     * @param faceHeight 盒体高度
     * @param pos        方块位置
     * @param state      方块状态
     * @return 遮挡返回 true
     */
    private static boolean isFaceOccludedByState(BlockGetter level, Direction face, float faceHeight, BlockPos pos, BlockState state) {
        if (!state.canOcclude()) {
            return false;
        }
        VoxelShape box = Shapes.box(0.0, 0.0, 0.0, 1.0, faceHeight, 1.0);
        VoxelShape occlusion = state.getOcclusionShape(level, pos);
        return Shapes.blockOccudes(box, occlusion, face);
    }

    /**
     * 判断相邻方块是否在给定面上遮挡流体面。
     *
     * @param level        方块获取器
     * @param pos          方块位置
     * @param side         面
     * @param faceHeight   面高度
     * @param neighborState 相邻方块状态
     * @return 遮挡返回 true
     */
    private static boolean isFaceOccludedByNeighbor(BlockGetter level, BlockPos pos, Direction side, float faceHeight,
                                                    BlockState neighborState) {
        return isFaceOccludedByState(level, side, faceHeight, pos.relative(side), neighborState);
    }

    /**
     * 判断自身方块是否在给定面上遮挡流体面。
     *
     * @param level 方块获取器
     * @param pos   方块位置
     * @param state 方块状态
     * @param face  面
     * @return 遮挡返回 true
     */
    private static boolean isFaceOccludedBySelf(BlockGetter level, BlockPos pos, BlockState state, Direction face) {
        return isFaceOccludedByState(level, face.getOpposite(), 1.0F, pos, state);
    }

    /**
     * 判断流体面是否应渲染。
     * <p>
     * 自身未遮挡该面且相邻方块未隐藏该面的贴面时渲染。
     *
     * @param level      方块与着色获取器
     * @param pos        方块位置
     * @param fluidState 流体状态
     * @param selfState  自身方块状态
     * @param direction  面
     * @param otherState 相邻方块状态
     * @return 应渲染返回 true
     */
    private static boolean shouldRenderFace(BlockAndTintGetter level, BlockPos pos, FluidState fluidState,
                                            BlockState selfState, Direction direction, BlockState otherState) {
        return !isFaceOccludedBySelf(level, pos, selfState, direction)
                && !isNeighborStateHidingOverlay(fluidState, otherState, direction.getOpposite());
    }

    /**
     * 获取流体的光照颜色。
     * <p>
     * 取当前位置与下方位置光照的较大值，分别处理方块光与天光。
     *
     * @param level 方块与着色获取器
     * @param pos   方块位置
     * @return 打包光照颜色
     */
    private static int getLightColor(BlockAndTintGetter level, BlockPos pos) {
        int here = LevelRenderer.getLightColor(level, pos);
        int below = LevelRenderer.getLightColor(level, pos.below());
        int blockLight = here & 0xFF;
        int blockLightBelow = below & 0xFF;
        int skyLight = here >> 16 & 0xFF;
        int skyLightBelow = below >> 16 & 0xFF;
        return (Math.max(blockLight, blockLightBelow)) | Math.max(skyLight, skyLightBelow) << 16;
    }

    /**
     * 写入单个顶点。
     *
     * @param buffer 顶点消费者
     * @param x      X 坐标
     * @param y      Y 坐标
     * @param z      Z 坐标
     * @param red    红色分量
     * @param green  绿色分量
     * @param blue   蓝色分量
     * @param alpha  透明度
     * @param u      U 坐标
     * @param v      V 坐标
     * @param light  打包光照
     * @param normal 法线方向
     */
    private static void vertex(VertexConsumer buffer, float x, float y, float z, float red, float green, float blue,
                               float alpha, float u, float v, int light, Direction normal) {
        buffer.addVertex(x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setLight(light)
                .setNormal(normal.getStepX(), normal.getStepY(), normal.getStepZ());
    }
}
