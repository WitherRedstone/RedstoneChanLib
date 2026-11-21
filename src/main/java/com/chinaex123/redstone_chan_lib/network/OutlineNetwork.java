package com.chinaex123.redstone_chan_lib.network;

import com.chinaex123.redstone_chan_lib.RedstoneChanLIB;
import com.chinaex123.redstone_chan_lib.api.outline.BlockOutline;
import com.chinaex123.redstone_chan_lib.api.outline.BlockScanOutline;
import com.chinaex123.redstone_chan_lib.api.outline.EntityOutline;
import com.chinaex123.redstone_chan_lib.api.outline.EntityScanOutline;
import com.chinaex123.redstone_chan_lib.client.outline.ClientOutlineApplier;
import com.chinaex123.redstone_chan_lib.util.PortalTexts;
import com.chinaex123.redstone_chan_lib.world.BlockOutlineSavedData;
import com.chinaex123.redstone_chan_lib.world.BlockScanSavedData;
import com.chinaex123.redstone_chan_lib.world.EntityScanSavedData;
import com.chinaex123.redstone_chan_lib.world.OutlineSavedData;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

/**
 * 实体描边网络处理。
 * <p>
 * 注册描边同步与切换请求两类数据包：同步包将服务端存档中的描边下发到客户端，
 * 切换包响应客户端测试入口的请求，按状态循环切换玩家注视生物的描边。
 * 同时在玩家开始追踪实体时补发描边数据，使新进入视野的实体正确显示描边。
 */
@EventBusSubscriber(modid = RedstoneChanLIB.MODID)
public final class OutlineNetwork {

    /** 测试入口的状态循环：发光青色 → 发光彩色 → 黑色 → 高光 → 不穿透 → 取消 */
    private static final int CYCLE_STATES = 6;
    /** 测试入口的选目标距离 */
    private static final double PICK_DISTANCE = 24.0D;

    private OutlineNetwork() {
    }

    /**
     * 注册数据包。
     *
     * @param event 数据包注册事件
     */
    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(OutlineSyncPayload.TYPE, OutlineSyncPayload.STREAM_CODEC, OutlineNetwork::handleSync);
        registrar.playToClient(BlockOutlineSyncPayload.TYPE, BlockOutlineSyncPayload.STREAM_CODEC, OutlineNetwork::handleBlockSync);
        registrar.playToClient(BlockScanSyncPayload.TYPE, BlockScanSyncPayload.STREAM_CODEC, OutlineNetwork::handleScanSync);
        registrar.playToClient(EntityScanSyncPayload.TYPE, EntityScanSyncPayload.STREAM_CODEC, OutlineNetwork::handleEntityScanSync);
        registrar.playToServer(OutlineCyclePayload.TYPE, OutlineCyclePayload.STREAM_CODEC, OutlineNetwork::handleCycle);
    }

    /**
     * 玩家登录时把当前维度里所有方块描边补发一遍。
     *
     * @param event 玩家登录事件
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        for (Map.Entry<BlockPos, BlockOutline.Outline> entry : BlockOutlineSavedData.get(level).all().entrySet()) {
            PacketDistributor.sendToPlayer(player, BlockOutlineSyncPayload.of(entry.getKey(), entry.getValue()));
        }
        BlockScanSavedData scanData = BlockScanSavedData.get(level);
        if (scanData.scan() != null) {
            PacketDistributor.sendToPlayer(player, BlockScanSyncPayload.of(scanData.target(), scanData.scan()));
        }
        EntityScanSavedData entityScanData = EntityScanSavedData.get(level);
        if (entityScanData.scan() != null) {
            PacketDistributor.sendToPlayer(player,
                    EntityScanSyncPayload.of(entityScanData.target(), entityScanData.scan()));
        }
    }

    /**
     * 玩家切换维度时，把新维度里的方块描边补发一遍。
     *
     * @param event 玩家切换维度事件
     */
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        for (Map.Entry<BlockPos, BlockOutline.Outline> entry : BlockOutlineSavedData.get(level).all().entrySet()) {
            PacketDistributor.sendToPlayer(player, BlockOutlineSyncPayload.of(entry.getKey(), entry.getValue()));
        }
        BlockScanSavedData scanData = BlockScanSavedData.get(level);
        if (scanData.scan() != null) {
            PacketDistributor.sendToPlayer(player, BlockScanSyncPayload.of(scanData.target(), scanData.scan()));
        }
        EntityScanSavedData entityScanData = EntityScanSavedData.get(level);
        if (entityScanData.scan() != null) {
            PacketDistributor.sendToPlayer(player,
                    EntityScanSyncPayload.of(entityScanData.target(), entityScanData.scan()));
        }
    }

    /**
     * 把扫描配置广播给该维度里的所有玩家。
     *
     * @param level  维度
     * @param target 扫描目标，null 表示停止
     * @param scan   扫描配置，null 表示停止
     */
    public static void broadcastScan(ServerLevel level, @Nullable BlockScanOutline.Target target,
                                     @Nullable BlockScanOutline.Scan scan) {
        PacketDistributor.sendToPlayersInDimension(level, BlockScanSyncPayload.of(target, scan));
    }

    /**
     * 把实体扫描配置广播给该维度里的所有玩家。
     *
     * @param level  维度
     * @param target 扫描目标，null 表示停止
     * @param scan   扫描配置，null 表示停止
     */
    public static void broadcastEntityScan(ServerLevel level, @Nullable EntityScanOutline.Target target,
                                           @Nullable EntityScanOutline.Scan scan) {
        PacketDistributor.sendToPlayersInDimension(level, EntityScanSyncPayload.of(target, scan));
    }

    /**
     * 处理实体扫描高亮同步包。
     *
     * @param payload 同步数据包
     * @param context 数据包上下文
     */
    private static void handleEntityScanSync(EntityScanSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientHandler.acceptEntityScan(payload);
            }
        });
    }

    /**
     * 处理扫描高亮同步包。
     *
     * @param payload 同步数据包
     * @param context 数据包上下文
     */
    private static void handleScanSync(BlockScanSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientHandler.acceptScan(payload);
            }
        });
    }

    /**
     * 向维度内附近的玩家广播某个方块的描边。
     *
     * @param level   维度
     * @param pos     方块位置
     * @param outline 描边配置，null 表示清除
     */
    public static void broadcastBlock(ServerLevel level, BlockPos pos, @Nullable BlockOutline.Outline outline) {
        PacketDistributor.sendToPlayersNear(level, null,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 96.0D,
                BlockOutlineSyncPayload.of(pos, outline));
    }

    /**
     * 处理方块描边同步包。
     *
     * @param payload 同步数据包
     * @param context 数据包上下文
     */
    private static void handleBlockSync(BlockOutlineSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientHandler.acceptBlock(payload);
            }
        });
    }

    /**
     * 玩家开始追踪实体时补发描边。
     * <p>
     * 若被追踪实体在存档中具有描边，则单独向该玩家发送同步包。
     *
     * @param event 玩家开始追踪实体事件
     */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Entity target = event.getTarget();
        MinecraftServer server = player.server;
        EntityOutline.Outline outline = OutlineSavedData.get(server).get(target.getUUID());
        if (outline != null) {
            PacketDistributor.sendToPlayer(player, OutlineSyncPayload.of(target.getUUID(), target.getId(), outline));
        }
    }

    /**
     * 向追踪该实体的所有玩家广播描边。
     *
     * @param entity  目标实体
     * @param outline 描边配置，null 表示清除
     */
    public static void broadcast(Entity entity, EntityOutline.Outline outline) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, OutlineSyncPayload.of(entity.getUUID(), entity.getId(), outline));
    }

    /**
     * 处理描边同步包。
     * <p>
     * 在主线程执行，仅在客户端分发到描边应用器。
     *
     * @param payload 同步数据包
     * @param context 数据包上下文
     */
    private static void handleSync(OutlineSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientHandler.accept(payload);
            }
        });
    }

    /**
     * 处理描边切换包。
     * <p>
     * 在主线程执行，选取玩家注视的活体实体并切换其描边；
     * 未选中目标时向玩家显示提示。
     *
     * @param payload 切换数据包
     * @param context 数据包上下文
     */
    private static void handleCycle(OutlineCyclePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            Entity target = pickTarget(player);
            if (target instanceof LivingEntity) {
                cycle(player, target);
                return;
            }
            if (cycleBlock(player)) {
                return;
            }
            player.displayClientMessage(PortalTexts.component("outline_no_target"), true);
            RedstoneChanLIB.LOGGER.info(PortalTexts.text("outline_no_target"));
        });
    }

    /**
     * 切换玩家注视方块的描边，用来验证方块描边的各种模式。
     *
     * @param player 玩家
     * @return 是否命中方块
     */
    private static boolean cycleBlock(ServerPlayer player) {
        HitResult hit = player.pick(PICK_DISTANCE, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        BlockPos pos = blockHit.getBlockPos();
        BlockOutline.Outline current = BlockOutline.get(level, pos);
        BlockOutline.Mode next;
        BlockOutline.Style style = BlockOutline.Style.SOLID;
        boolean keepVisible = false;
        if (current == null) {
            next = BlockOutline.Mode.GLOW;
        } else if (current.mode() == BlockOutline.Mode.GLOW && current.style() == BlockOutline.Style.SOLID) {
            next = BlockOutline.Mode.GLOW;
            style = BlockOutline.Style.RAINBOW;
        } else if (current.mode() == BlockOutline.Mode.CREATE && !current.keepVisible()) {
            next = BlockOutline.Mode.CREATE;
            keepVisible = true;
        } else {
            next = switch (current.mode()) {
                case GLOW -> BlockOutline.Mode.BLACK;
                case BLACK -> BlockOutline.Mode.HIGHLIGHT;
                case HIGHLIGHT -> BlockOutline.Mode.CREATE;
                case CREATE -> BlockOutline.Mode.WIRE;
                case WIRE -> null;
            };
        }
        if (next == null) {
            BlockOutline.clear(level, pos);
            notifyState(player, "block_outline_cleared");
            return true;
        }
        boolean rainbow = style == BlockOutline.Style.RAINBOW;
        int rgb = next == BlockOutline.Mode.GLOW ? 0xFF7CF9FF : BlockOutline.defaultColor(next);
        String key = switch (next) {
            case GLOW -> rainbow ? "block_outline_glow_rainbow" : "block_outline_glow";
            case BLACK -> "block_outline_black";
            case HIGHLIGHT -> "block_outline_highlight";
            case CREATE -> keepVisible ? "block_outline_create_keep" : "block_outline_create";
            case WIRE -> rainbow ? "block_outline_wire_rainbow" : "block_outline_wire";
        };
        BlockOutline.apply(level, pos, next, rgb, style, BlockOutline.defaultThickness(next),
                BlockOutline.DEFAULT_THROUGH_WALLS, keepVisible, BlockOutline.DEFAULT_HOLD_SECONDS);
        notifyState(player, key);
        return true;
    }

    /**
     * 选取玩家注视的实体。
     * <p>
     * 沿视线方向在限定距离内做实体射线检测，仅接受可拾取、非旁观者的活体实体。
     *
     * @param player 玩家
     * @return 注视的实体，未命中时返回 null
     */
    @Nullable
    private static Entity pickTarget(ServerPlayer player) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 view = player.getViewVector(1.0F);
        Vec3 end = eye.add(view.scale(PICK_DISTANCE));
        AABB searchBox = player.getBoundingBox()
                .expandTowards(view.scale(PICK_DISTANCE))
                .inflate(1.0D, 1.0D, 1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player, eye, end, searchBox,
                entity -> entity instanceof LivingEntity && entity.isPickable() && !entity.isSpectator(),
                PICK_DISTANCE * PICK_DISTANCE);
        return hit == null ? null : hit.getEntity();
    }

    /**
     * 切换目标实体的描边状态。
     * <p>
     * 依据当前描边推算下一个状态并应用：发光青色、发光彩色、黑色、高光、
     * 不穿透方块的发光青色，最后清除描边。
     *
     * @param player 玩家
     * @param target 目标实体
     */
    private static void cycle(ServerPlayer player, Entity target) {
        MinecraftServer server = player.server;
        OutlineSavedData data = OutlineSavedData.get(server);
        UUID id = target.getUUID();
        EntityOutline.Outline current = data.get(id);
        int state = current == null ? 0 : nextState(current);
        switch (state) {
            case 0 -> apply(player, data, target, new EntityOutline.Outline(EntityOutline.Mode.GLOW, 0xFF7CF9FF,
                    EntityOutline.Style.SOLID, EntityOutline.DEFAULT_THICKNESS, true), "outline_glow");
            case 1 -> apply(player, data, target, new EntityOutline.Outline(EntityOutline.Mode.GLOW,
                    EntityOutline.defaultColor(EntityOutline.Mode.GLOW), EntityOutline.Style.RAINBOW,
                    EntityOutline.DEFAULT_THICKNESS, true), "outline_glow_rainbow");
            case 2 -> apply(player, data, target, new EntityOutline.Outline(EntityOutline.Mode.BLACK,
                    EntityOutline.defaultColor(EntityOutline.Mode.BLACK), EntityOutline.Style.SOLID,
                    EntityOutline.DEFAULT_THICKNESS, true), "outline_black");
            case 3 -> apply(player, data, target, new EntityOutline.Outline(EntityOutline.Mode.HIGHLIGHT,
                    EntityOutline.defaultColor(EntityOutline.Mode.HIGHLIGHT), EntityOutline.Style.SOLID,
                    EntityOutline.DEFAULT_THICKNESS, true), "outline_highlight");
            case 4 -> apply(player, data, target, new EntityOutline.Outline(EntityOutline.Mode.GLOW, 0xFF7CF9FF,
                    EntityOutline.Style.SOLID, EntityOutline.DEFAULT_THICKNESS, false), "outline_glow_blocked");
            default -> {
                data.remove(id);
                broadcast(target, null);
                notifyState(player, "outline_cleared");
            }
        }
    }

    /**
     * 由当前描边推算下一个状态编号。
     * <p>
     * 发光描边区分是否穿透与是否彩色，分别推进到对应状态；
     * 黑色推进到高光，高光推进到不穿透，其余回到清除。
     *
     * @param current 当前描边
     * @return 下一个状态编号
     */
    private static int nextState(EntityOutline.Outline current) {
        return switch (current.mode()) {
            case GLOW -> !current.throughWalls() ? CYCLE_STATES - 1
                    : current.style() == EntityOutline.Style.RAINBOW ? 2 : 1;
            case BLACK -> 3;
            case HIGHLIGHT -> 4;
        };
    }

    /**
     * 应用描边并广播。
     * <p>
     * 写入存档、向追踪玩家广播，并向操作玩家显示状态提示。
     *
     * @param player  玩家
     * @param data    描边存档数据
     * @param target  目标实体
     * @param outline 描边配置
     * @param key     提示文案键
     */
    private static void apply(ServerPlayer player, OutlineSavedData data, Entity target,
                              EntityOutline.Outline outline, String key) {
        data.put(target.getUUID(), outline);
        broadcast(target, outline);
        notifyState(player, key);
    }

    /**
     * 输出状态提示。
     * <p>
     * 记录日志，并在玩家存在时向其显示动作栏消息。
     *
     * @param player 玩家，可为 null
     * @param key    提示文案键
     */
    private static void notifyState(@Nullable ServerPlayer player, String key) {
        RedstoneChanLIB.LOGGER.info(PortalTexts.text(key));
        if (player != null) {
            player.displayClientMessage(PortalTexts.component(key), true);
        }
    }

    /**
     * 客户端处理入口。
     * <p>
     * 与网络层解耦，避免在专用服务端加载客户端类。
     */
    private static final class ClientHandler {

        private ClientHandler() {
        }

        /**
         * 将同步数据交给客户端描边应用器。
         *
         * @param payload 同步数据包
         */
        private static void accept(OutlineSyncPayload payload) {
            ClientOutlineApplier.accept(payload);
        }

        /**
         * 把方块描边同步包写进客户端的本地表。
         *
         * @param payload 方块描边同步包
         */
        private static void acceptBlock(BlockOutlineSyncPayload payload) {
            BlockOutline.putLocal(payload.pos(), payload.toOutline());
        }

        /**
         * 把扫描高亮同步包写进客户端的本地配置。
         *
         * @param payload 扫描高亮同步包
         */
        private static void acceptScan(BlockScanSyncPayload payload) {
            BlockScanOutline.putLocal(payload.toTarget(), payload.toScan());
        }

        /**
         * 接收实体扫描同步包。
         *
         * @param payload 实体扫描同步包
         */
        private static void acceptEntityScan(EntityScanSyncPayload payload) {
            EntityScanOutline.putLocal(payload.toTarget(), payload.toScan());
        }
    }
}